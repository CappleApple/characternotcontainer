package com.cappleapple.characternotcontainer.equipment;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import com.cappleapple.characternotcontainer.network.EquipmentChangePayload;
import com.cappleapple.characternotcontainer.network.NearbyEquipmentRequestPayload;
import com.cappleapple.characternotcontainer.network.NearbyEquipmentResponsePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NearbyEquipmentSources {
    private static final int SESSION_LIFETIME_TICKS = 20 * 60 * 5;
    private static final List<EquipmentSlot> ARMOR_STAND_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);
    private static final Map<UUID, SearchSession> SESSIONS = new HashMap<>();

    private NearbyEquipmentSources() {}

    public static NearbyEquipmentResponsePayload search(ServerPlayer player, NearbyEquipmentRequestPayload request,
                                                          EquipmentTargetAccess target) {
        if (!CharacterConfigManager.general().enableNearbyEquipmentSources
                || CharacterConfigManager.general().nearbyEquipmentSearchRadius <= 0.0D) {
            SESSIONS.remove(player.getUUID());
            return new NearbyEquipmentResponsePayload(request.searchId(), false, List.of());
        }

        ServerLevel level = player.serverLevel();
        double radius = CharacterConfigManager.general().nearbyEquipmentSearchRadius;
        double radiusSquared = radius * radius;
        List<SourceCandidate> candidates = new ArrayList<>();
        Set<IItemHandler> visitedHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<CandidateIdentity> seenCandidates = new HashSet<>();
        scanBlocks(player, target, level, radius, radiusSquared, candidates, visitedHandlers, seenCandidates);
        scanEntities(player, target, level, radius, radiusSquared, candidates, visitedHandlers, seenCandidates);

        TargetKey targetKey = TargetKey.of(request.system(), request.slotId(), request.slotIndex(), request.cosmetic());
        SearchSession session = new SearchSession(request.searchId(), targetKey, level.dimension(),
                level.getGameTime() + SESSION_LIFETIME_TICKS, List.copyOf(candidates));
        SESSIONS.put(player.getUUID(), session);

        List<NearbyEquipmentResponsePayload.Entry> entries = new ArrayList<>(candidates.size());
        for (int token = 0; token < candidates.size(); token++) {
            entries.add(new NearbyEquipmentResponsePayload.Entry(token, candidates.get(token).displayStack));
        }
        return new NearbyEquipmentResponsePayload(request.searchId(), true, entries);
    }

    public static boolean change(ServerPlayer player, EquipmentChangePayload payload, EquipmentTargetAccess target) {
        if (!CharacterConfigManager.general().enableNearbyEquipmentSources) return false;
        SearchSession session = SESSIONS.get(player.getUUID());
        if (session == null || session.searchId != payload.searchId()
                || !session.target.equals(TargetKey.of(payload.system(), payload.slotId(), payload.slotIndex(), payload.cosmetic()))
                || !session.dimension.equals(player.level().dimension())
                || player.level().getGameTime() > session.expiresAt
                || payload.sourceIndex() < 0 || payload.sourceIndex() >= session.candidates.size()) return false;

        SourceCandidate candidate = session.candidates.get(payload.sourceIndex());
        if (!candidate.source.inRange(player) || !target.canRemove()) return false;
        ItemStack simulated = candidate.source.extractOne(player, true);
        if (simulated.isEmpty() || !ItemStack.isSameItemSameComponents(simulated, candidate.displayStack)
                || !target.accepts(simulated)) return false;

        IItemHandler playerInventory = PlayerInventoryAccess.handler(player);
        ItemStack extracted = candidate.source.extractOne(player, false);
        if (extracted.isEmpty() || !ItemStack.isSameItemSameComponents(extracted, simulated)) {
            if (!extracted.isEmpty() && !candidate.source.restore(player, extracted)) {
                CharacterNotContainer.LOGGER.error("Could not restore an unexpected extraction from a nearby source for {}",
                        player.getGameProfile().getName());
            }
            CharacterNotContainer.LOGGER.warn("A nearby equipment source changed while {} was extracting from it",
                    player.getGameProfile().getName());
            return false;
        }
        if (!EquipmentTransactions.equipExternal(playerInventory, extracted, target::equipped, target::set)) {
            if (!candidate.source.restore(player, extracted)) {
                CharacterNotContainer.LOGGER.error("Could not roll back a nearby extraction for {}",
                        player.getGameProfile().getName());
            }
            CharacterNotContainer.LOGGER.error("Could not store previously validated equipment for {}", player.getGameProfile().getName());
            return false;
        }
        SESSIONS.remove(player.getUUID());
        return true;
    }

    public static void clear(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    private static void scanBlocks(ServerPlayer player, EquipmentTargetAccess target, ServerLevel level,
                                   double radius, double radiusSquared, List<SourceCandidate> result,
                                   Set<IItemHandler> visitedHandlers, Set<CandidateIdentity> seenCandidates) {
        BlockPos center = player.blockPosition();
        int blockRadius = (int)Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-blockRadius, -blockRadius, -blockRadius),
                center.offset(blockRadius, blockRadius, blockRadius))) {
            if (result.size() >= NearbyEquipmentResponsePayload.MAX_ENTRIES) return;
            if (Vec3.atCenterOf(pos).distanceToSqr(player.position()) > radiusSquared || !level.hasChunkAt(pos)) continue;
            scanBlockHandler(player, target, level, pos, null, result, visitedHandlers, seenCandidates);
            for (Direction side : Direction.values()) {
                if (result.size() >= NearbyEquipmentResponsePayload.MAX_ENTRIES) return;
                scanBlockHandler(player, target, level, pos, side, result, visitedHandlers, seenCandidates);
            }
        }
    }

    private static void scanBlockHandler(ServerPlayer player, EquipmentTargetAccess target, ServerLevel level,
                                         BlockPos pos, Direction side, List<SourceCandidate> result,
                                         Set<IItemHandler> visitedHandlers, Set<CandidateIdentity> seenCandidates) {
        IItemHandler handler;
        try {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        } catch (RuntimeException exception) {
            CharacterNotContainer.LOGGER.debug("Nearby item-handler lookup failed at {} from side {}", pos, side, exception);
            return;
        }
        if (handler == null || !visitedHandlers.add(handler)) return;
        collectHandler(target, new BlockHandlerSource(pos.immutable(), side), handler, result, seenCandidates);
    }

    private static void scanEntities(ServerPlayer player, EquipmentTargetAccess target, ServerLevel level,
                                     double radius, double radiusSquared, List<SourceCandidate> result,
                                     Set<IItemHandler> visitedHandlers, Set<CandidateIdentity> seenCandidates) {
        AABB area = player.getBoundingBox().inflate(radius);
        for (Entity entity : level.getEntities(player, area, entity -> entity.isAlive()
                && entity.distanceToSqr(player) <= radiusSquared)) {
            if (result.size() >= NearbyEquipmentResponsePayload.MAX_ENTRIES) return;
            if (entity instanceof ArmorStand stand) {
                scanArmorStand(target, stand, result);
                continue;
            }
            scanEntityHandler(target, entity, EntityHandlerKind.NORMAL, null, result, visitedHandlers, seenCandidates);
            scanEntityHandler(target, entity, EntityHandlerKind.AUTOMATION, null, result, visitedHandlers, seenCandidates);
            for (Direction side : Direction.values()) {
                if (result.size() >= NearbyEquipmentResponsePayload.MAX_ENTRIES) return;
                scanEntityHandler(target, entity, EntityHandlerKind.AUTOMATION, side, result, visitedHandlers, seenCandidates);
            }
        }
    }

    private static void scanEntityHandler(EquipmentTargetAccess target, Entity entity, EntityHandlerKind kind,
                                          Direction side, List<SourceCandidate> result,
                                          Set<IItemHandler> visitedHandlers, Set<CandidateIdentity> seenCandidates) {
        IItemHandler handler;
        try {
            handler = kind == EntityHandlerKind.NORMAL
                    ? entity.getCapability(Capabilities.ItemHandler.ENTITY, null)
                    : entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, side);
        } catch (RuntimeException exception) {
            CharacterNotContainer.LOGGER.debug("Nearby entity item-handler lookup failed for {}", entity, exception);
            return;
        }
        if (handler == null || !visitedHandlers.add(handler)) return;
        collectHandler(target, new EntityHandlerSource(entity.getUUID(), kind, side), handler, result, seenCandidates);
    }

    private static void collectHandler(EquipmentTargetAccess target, HandlerSource source, IItemHandler handler,
                                       List<SourceCandidate> result, Set<CandidateIdentity> seenCandidates) {
        for (int slot = 0; slot < handler.getSlots() && result.size() < NearbyEquipmentResponsePayload.MAX_ENTRIES; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            ItemStack extractable = handler.extractItem(slot, 1, true);
            if (extractable.isEmpty() || !target.accepts(extractable)) continue;
            CandidateIdentity identity = new CandidateIdentity(source.physicalSource(), slot,
                    ItemStack.hashItemAndComponents(stack), stack.getCount());
            if (!seenCandidates.add(identity)) continue;
            result.add(new SourceCandidate(source.atSlot(slot), stack.copy()));
        }
    }

    private static void scanArmorStand(EquipmentTargetAccess target, ArmorStand stand, List<SourceCandidate> result) {
        for (EquipmentSlot slot : ARMOR_STAND_SLOTS) {
            if (result.size() >= NearbyEquipmentResponsePayload.MAX_ENTRIES || !stand.canUseSlot(slot)) continue;
            ItemStack stack = stand.getItemBySlot(slot);
            if (!stack.isEmpty() && target.accepts(single(stack))) {
                result.add(new SourceCandidate(new ArmorStandSource(stand.getUUID(), slot), stack.copy()));
            }
        }
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack result = stack.copy();
        result.setCount(Math.min(1, result.getCount()));
        return result;
    }

    private record SearchSession(int searchId, TargetKey target, ResourceKey<Level> dimension, long expiresAt,
                                 List<SourceCandidate> candidates) {}

    private record TargetKey(EquipmentChangePayload.TargetSystem system, String slotId, int slotIndex, boolean cosmetic) {
        private static TargetKey of(EquipmentChangePayload.TargetSystem system, String slotId, int slotIndex, boolean cosmetic) {
            return new TargetKey(system, slotId, slotIndex, cosmetic);
        }
    }

    private record SourceCandidate(NearbySource source, ItemStack displayStack) {}

    private interface NearbySource {
        ItemStack extractOne(ServerPlayer player, boolean simulate);

        boolean restore(ServerPlayer player, ItemStack stack);

        boolean inRange(ServerPlayer player);
    }

    private interface HandlerSource extends NearbySource {
        Object physicalSource();

        NearbySource atSlot(int slot);
    }

    private record BlockHandlerSource(BlockPos pos, Direction side, int slot) implements HandlerSource {
        private BlockHandlerSource(BlockPos pos, Direction side) {
            this(pos, side, -1);
        }

        @Override
        public BlockHandlerSource atSlot(int slot) {
            return new BlockHandlerSource(pos, side, slot);
        }

        @Override
        public Object physicalSource() {
            return pos;
        }

        @Override
        public ItemStack extractOne(ServerPlayer player, boolean simulate) {
            if (!inRange(player)) return ItemStack.EMPTY;
            IItemHandler handler = player.serverLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            return handler == null || slot < 0 || slot >= handler.getSlots()
                    ? ItemStack.EMPTY : handler.extractItem(slot, 1, simulate);
        }

        @Override
        public boolean restore(ServerPlayer player, ItemStack stack) {
            if (!inRange(player)) return false;
            IItemHandler handler = player.serverLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            if (handler == null || slot < 0 || slot >= handler.getSlots()
                    || !handler.insertItem(slot, stack.copy(), true).isEmpty()) return false;
            return handler.insertItem(slot, stack.copy(), false).isEmpty();
        }

        @Override
        public boolean inRange(ServerPlayer player) {
            double radius = CharacterConfigManager.general().nearbyEquipmentSearchRadius;
            return player.serverLevel().hasChunkAt(pos)
                    && Vec3.atCenterOf(pos).distanceToSqr(player.position()) <= radius * radius;
        }
    }

    private record EntityHandlerSource(UUID entityId, EntityHandlerKind kind, Direction side, int slot)
            implements HandlerSource {
        private EntityHandlerSource(UUID entityId, EntityHandlerKind kind, Direction side) {
            this(entityId, kind, side, -1);
        }

        @Override
        public EntityHandlerSource atSlot(int slot) {
            return new EntityHandlerSource(entityId, kind, side, slot);
        }

        @Override
        public Object physicalSource() {
            return entityId;
        }

        @Override
        public ItemStack extractOne(ServerPlayer player, boolean simulate) {
            Entity entity = player.serverLevel().getEntity(entityId);
            if (entity == null || !inRange(player)) return ItemStack.EMPTY;
            IItemHandler handler = kind == EntityHandlerKind.NORMAL
                    ? entity.getCapability(Capabilities.ItemHandler.ENTITY, null)
                    : entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, side);
            return handler == null || slot < 0 || slot >= handler.getSlots()
                    ? ItemStack.EMPTY : handler.extractItem(slot, 1, simulate);
        }

        @Override
        public boolean restore(ServerPlayer player, ItemStack stack) {
            Entity entity = player.serverLevel().getEntity(entityId);
            if (entity == null || !inRange(player)) return false;
            IItemHandler handler = kind == EntityHandlerKind.NORMAL
                    ? entity.getCapability(Capabilities.ItemHandler.ENTITY, null)
                    : entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, side);
            if (handler == null || slot < 0 || slot >= handler.getSlots()
                    || !handler.insertItem(slot, stack.copy(), true).isEmpty()) return false;
            return handler.insertItem(slot, stack.copy(), false).isEmpty();
        }

        @Override
        public boolean inRange(ServerPlayer player) {
            Entity entity = player.serverLevel().getEntity(entityId);
            double radius = CharacterConfigManager.general().nearbyEquipmentSearchRadius;
            return entity != null && entity.isAlive() && entity.distanceToSqr(player) <= radius * radius;
        }
    }

    private record ArmorStandSource(UUID entityId, EquipmentSlot slot) implements NearbySource {
        @Override
        public ItemStack extractOne(ServerPlayer player, boolean simulate) {
            Entity entity = player.serverLevel().getEntity(entityId);
            if (!(entity instanceof ArmorStand stand) || !stand.canUseSlot(slot) || !inRange(player)) return ItemStack.EMPTY;
            ItemStack current = stand.getItemBySlot(slot);
            if (current.isEmpty()) return ItemStack.EMPTY;
            ItemStack extracted = single(current);
            if (!simulate) {
                ItemStack remaining = current.copy();
                remaining.shrink(1);
                stand.setItemSlot(slot, remaining);
            }
            return extracted;
        }

        @Override
        public boolean restore(ServerPlayer player, ItemStack stack) {
            Entity entity = player.serverLevel().getEntity(entityId);
            if (!(entity instanceof ArmorStand stand) || !stand.canUseSlot(slot) || !inRange(player)) return false;
            ItemStack current = stand.getItemBySlot(slot);
            if (current.isEmpty()) {
                stand.setItemSlot(slot, stack.copy());
                return true;
            }
            if (!ItemStack.isSameItemSameComponents(current, stack)
                    || current.getCount() + stack.getCount() > current.getMaxStackSize()) return false;
            ItemStack restored = current.copy();
            restored.grow(stack.getCount());
            stand.setItemSlot(slot, restored);
            return true;
        }

        @Override
        public boolean inRange(ServerPlayer player) {
            Entity entity = player.serverLevel().getEntity(entityId);
            double radius = CharacterConfigManager.general().nearbyEquipmentSearchRadius;
            return entity instanceof ArmorStand stand && stand.isAlive()
                    && stand.distanceToSqr(player) <= radius * radius;
        }
    }

    private enum EntityHandlerKind { NORMAL, AUTOMATION }

    private record CandidateIdentity(Object physicalSource, int slot, int itemHash, int count) {}
}
