package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.compat.armordamagescaling.ArmorDamageScalingBridge;
import com.cappleapple.characternotcontainer.compat.needsnotnecessities.NeedsNotNecessitiesSourceBridge;
import com.cappleapple.characternotcontainer.compat.puffishskills.PufferfishSkillsSourceBridge;
import com.cappleapple.characternotcontainer.equipment.EquipmentTargetAccess;
import com.cappleapple.characternotcontainer.equipment.EquipmentTransactions;
import com.cappleapple.characternotcontainer.equipment.NearbyEquipmentSources;
import com.cappleapple.characternotcontainer.equipment.PlayerInventoryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Optional;

public final class EquipmentNetwork {
    private EquipmentNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("6").optional();
        registrar.playToServer(EquipmentChangePayload.TYPE, EquipmentChangePayload.STREAM_CODEC, EquipmentNetwork::handleChange);
        registrar.playToServer(NearbyEquipmentRequestPayload.TYPE, NearbyEquipmentRequestPayload.STREAM_CODEC,
                EquipmentNetwork::handleNearbyRequest);
        registrar.playToClient(NearbyEquipmentResponsePayload.TYPE, NearbyEquipmentResponsePayload.STREAM_CODEC,
                EquipmentNetwork::handleNearbyResponse);
        registrar.playToServer(ModifierSourcesRequestPayload.TYPE, ModifierSourcesRequestPayload.STREAM_CODEC,
                EquipmentNetwork::handleModifierSourcesRequest);
        registrar.playToClient(ModifierSourcesResponsePayload.TYPE, ModifierSourcesResponsePayload.STREAM_CODEC,
                EquipmentNetwork::handleModifierSourcesResponse);
    }

    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) NearbyEquipmentSources.clear(player);
    }

    private static void handleChange(EquipmentChangePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleChangeOnMainThread(payload, context));
    }

    private static void handleChangeOnMainThread(EquipmentChangePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.containerMenu != player.inventoryMenu) return;
        Optional<EquipmentTargetAccess> resolved = resolveTarget(player, payload);
        boolean changed = resolved.isPresent() && change(player, payload, resolved.get());
        if (changed) {
            player.inventoryMenu.broadcastChanges();
        } else {
            player.displayClientMessage(Component.translatable("message.characternotcontainer.equipment_change_refused"), true);
        }
    }

    private static boolean change(ServerPlayer player, EquipmentChangePayload payload, EquipmentTargetAccess target) {
        IItemHandler inventory = PlayerInventoryAccess.handler(player);
        return switch (payload.sourceKind()) {
            case UNEQUIP -> target.canRemove()
                    && EquipmentTransactions.unequip(inventory, target::equipped, target::set);
            case PLAYER_INVENTORY -> changeFromPlayerInventory(inventory, payload.sourceIndex(), target);
            case NEARBY -> NearbyEquipmentSources.change(player, payload, target);
        };
    }

    private static boolean changeFromPlayerInventory(IItemHandler inventory, int inventoryIndex,
                                                     EquipmentTargetAccess target) {
        if (inventoryIndex < 0 || inventoryIndex >= inventory.getSlots() || !target.canRemove()) return false;
        ItemStack candidate = inventory.getStackInSlot(inventoryIndex);
        if (candidate.isEmpty() || !target.accepts(candidate)) return false;
        return EquipmentTransactions.swapFromInventory(inventory, inventoryIndex, target::equipped, target::set);
    }

    private static void handleNearbyRequest(NearbyEquipmentRequestPayload request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.containerMenu != player.inventoryMenu) return;
            EquipmentChangePayload targetPayload = new EquipmentChangePayload(request.system(), request.slotId(),
                    request.slotIndex(), request.cosmetic(), EquipmentChangePayload.SourceKind.UNEQUIP, -1, request.searchId());
            NearbyEquipmentResponsePayload response = resolveTarget(player, targetPayload)
                    .map(target -> NearbyEquipmentSources.search(player, request, target))
                    .orElseGet(() -> new NearbyEquipmentResponsePayload(request.searchId(), false, java.util.List.of()));
            PacketDistributor.sendToPlayer(player, response);
        });
    }

    private static void handleNearbyResponse(NearbyEquipmentResponsePayload response, IPayloadContext context) {
        context.enqueueWork(() -> ClientAccess.accept(response));
    }

    private static void handleModifierSourcesRequest(ModifierSourcesRequestPayload request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var entries = new ArrayList<>(NeedsNotNecessitiesSourceBridge.sources(player));
            entries.addAll(PufferfishSkillsSourceBridge.sources(player));
            PacketDistributor.sendToPlayer(player, new ModifierSourcesResponsePayload(
                    entries, ArmorDamageScalingBridge.values(player)));
        });
    }

    private static void handleModifierSourcesResponse(ModifierSourcesResponsePayload response, IPayloadContext context) {
        context.enqueueWork(() -> ClientAccess.accept(response));
    }

    private static Optional<EquipmentTargetAccess> resolveTarget(ServerPlayer player, EquipmentChangePayload payload) {
        return switch (payload.system()) {
            case VANILLA -> resolveVanilla(player, payload);
            case CURIOS -> ModList.get().isLoaded("curios") ? CuriosServerAccess.resolve(player, payload) : Optional.empty();
        };
    }

    private static Optional<EquipmentTargetAccess> resolveVanilla(ServerPlayer player, EquipmentChangePayload payload) {
        if (payload.cosmetic()) return Optional.empty();
        EquipmentSlot slot = switch (payload.slotId()) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
        if (slot == null) return Optional.empty();
        return Optional.of(new EquipmentTargetAccess() {
            @Override
            public ItemStack equipped() {
                return player.getItemBySlot(slot).copy();
            }

            @Override
            public boolean accepts(ItemStack stack) {
                return !stack.isEmpty() && player.getEquipmentSlotForItem(stack) == slot && stack.canEquip(slot, player);
            }

            @Override
            public boolean canRemove() {
                return true;
            }

            @Override
            public void set(ItemStack stack) {
                player.setItemSlot(slot, stack.copy());
            }
        });
    }

    private static final class CuriosServerAccess {
        private static final java.lang.reflect.Method RESOLVE = find();

        private static java.lang.reflect.Method find() {
            try {
                return Class.forName("com.cappleapple.characternotcontainer.compat.curios.CuriosEquipmentMutator")
                        .getMethod("resolve", ServerPlayer.class, EquipmentChangePayload.class);
            } catch (ReflectiveOperationException | LinkageError exception) {
                CharacterNotContainer.LOGGER.error("Curios is loaded but its equipment target access is unavailable", exception);
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        private static Optional<EquipmentTargetAccess> resolve(ServerPlayer player, EquipmentChangePayload payload) {
            if (RESOLVE == null) return Optional.empty();
            try {
                return (Optional<EquipmentTargetAccess>) RESOLVE.invoke(null, player, payload);
            } catch (ReflectiveOperationException exception) {
                CharacterNotContainer.LOGGER.error("Curios equipment target lookup failed", exception);
                return Optional.empty();
            }
        }
    }

    private static final class ClientAccess {
        private static void accept(NearbyEquipmentResponsePayload response) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.screen instanceof com.cappleapple.characternotcontainer.client.CharacterEquipmentScreen screen) {
                screen.acceptNearbyEquipment(response);
            }
        }

        private static void accept(ModifierSourcesResponsePayload response) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.screen instanceof com.cappleapple.characternotcontainer.client.CharacterEquipmentScreen screen) {
                screen.acceptModifierSources(response);
            }
        }
    }
}
