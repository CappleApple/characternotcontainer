package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.compat.relics.RelicResearchMenu;
import com.cappleapple.characternotcontainer.compat.relics.RelicResearchSource;
import com.cappleapple.characternotcontainer.compat.relics.RelicsIntegration;
import net.minecraft.world.SimpleMenuProvider;
import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.compat.armordamagescaling.ArmorDamageScalingBridge;
import com.cappleapple.characternotcontainer.compat.needsnotnecessities.NeedsNotNecessitiesSourceBridge;
import com.cappleapple.characternotcontainer.compat.puffishskills.PufferfishSkillsSourceBridge;
import com.cappleapple.characternotcontainer.equipment.EquipmentTargetAccess;
import com.cappleapple.characternotcontainer.equipment.EquipmentTransactions;
import com.cappleapple.characternotcontainer.equipment.NearbyEquipmentSources;
import com.cappleapple.characternotcontainer.equipment.PlayerInventoryAccess;
import com.cappleapple.characternotcontainer.equipment.VanillaEquipmentTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Optional;

public final class EquipmentNetwork {
    private EquipmentNetwork() {}

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CharacterNotContainer.id("equipment"), () -> PROTOCOL,
            EquipmentNetwork::acceptVersion, EquipmentNetwork::acceptVersion);

    private static boolean acceptVersion(String version) {
        return PROTOCOL.equals(version) || NetworkRegistry.ABSENT.equals(version)
                || NetworkRegistry.ACCEPTVANILLA.equals(version);
    }

    public static void register() {
        register(0, EquipmentChangePayload.class, EquipmentChangePayload.STREAM_CODEC,
                NetworkDirection.PLAY_TO_SERVER, EquipmentNetwork::handleChange);
        register(1, NearbyEquipmentRequestPayload.class, NearbyEquipmentRequestPayload.STREAM_CODEC,
                NetworkDirection.PLAY_TO_SERVER, EquipmentNetwork::handleNearbyRequest);
        register(2, NearbyEquipmentResponsePayload.class, NearbyEquipmentResponsePayload.STREAM_CODEC,
                NetworkDirection.PLAY_TO_CLIENT, EquipmentNetwork::handleNearbyResponse);
        register(3, ModifierSourcesRequestPayload.class, ModifierSourcesRequestPayload.STREAM_CODEC,
                NetworkDirection.PLAY_TO_SERVER, EquipmentNetwork::handleModifierSourcesRequest);
        register(4, ModifierSourcesResponsePayload.class, ModifierSourcesResponsePayload.STREAM_CODEC,
                NetworkDirection.PLAY_TO_CLIENT, EquipmentNetwork::handleModifierSourcesResponse);
        register(5, RelicResearchPayload.class, RelicResearchPayload.STREAM_CODEC,
                NetworkDirection.PLAY_TO_SERVER, EquipmentNetwork::handleRelicResearch);
    }

    private static <T> void register(int id, Class<T> type, PacketCodec<T> codec, NetworkDirection direction,
                                     java.util.function.BiConsumer<T, NetworkEvent.Context> handler) {
        CHANNEL.registerMessage(id, type, (packet, buffer) -> codec.encode(buffer, packet), codec::decode,
                (packet, supplied) -> {
                    NetworkEvent.Context context = supplied.get();
                    handler.accept(packet, context);
                    context.setPacketHandled(true);
                }, Optional.of(direction));
    }

    public static void sendToServer(Object packet) { CHANNEL.sendToServer(packet); }

    private static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static void handleRelicResearch(RelicResearchPayload payload, NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if ((player == null) || player.containerMenu != player.inventoryMenu
                    || !player.isAlive() || !RelicsIntegration.isRelic(payload.expected())) return;
            EquipmentChangePayload target = payload.target();
            var equipment = resolveTarget(player, target);
            if (equipment.isEmpty()) return;
            Optional<RelicResearchSource> source = switch (target.sourceKind()) {
                case UNEQUIP -> equipment.get().researchSource();
                case PLAYER_INVENTORY -> Optional.of(
                        RelicResearchSource.handler(
                                () -> PlayerInventoryAccess.handler(player), target.sourceIndex(), player::isAlive,
                                () -> player.getInventory().setChanged()));
                case NEARBY -> NearbyEquipmentSources.researchSource(player, target);
            };
            source.filter(value -> value.valid().getAsBoolean()
                    && ItemStack.isSameItem(value.stack().get(), payload.expected())).ifPresent(value ->
                    player.openMenu(new SimpleMenuProvider(
                            (id, inventory, owner) -> new RelicResearchMenu(id, value),
                            Component.translatable("gui.characternotcontainer.character"))));
        });
    }

    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) NearbyEquipmentSources.clear(player);
    }

    private static void handleChange(EquipmentChangePayload payload, NetworkEvent.Context context) {
        context.enqueueWork(() -> handleChangeOnMainThread(payload, context));
    }

    private static void handleChangeOnMainThread(EquipmentChangePayload payload, NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if ((player == null) || player.containerMenu != player.inventoryMenu) return;
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

    private static void handleNearbyRequest(NearbyEquipmentRequestPayload request, NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if ((player == null) || player.containerMenu != player.inventoryMenu) return;
            EquipmentChangePayload targetPayload = new EquipmentChangePayload(request.system(), request.slotId(),
                    request.slotIndex(), request.cosmetic(), EquipmentChangePayload.SourceKind.UNEQUIP, -1, request.searchId());
            NearbyEquipmentResponsePayload response = resolveTarget(player, targetPayload)
                    .map(target -> NearbyEquipmentSources.search(player, request, target))
                    .orElseGet(() -> new NearbyEquipmentResponsePayload(request.searchId(), false, java.util.List.of()));
            sendToPlayer(player, response);
        });
    }

    private static void handleNearbyResponse(NearbyEquipmentResponsePayload response, NetworkEvent.Context context) {
        context.enqueueWork(() -> ClientAccess.accept(response));
    }

    private static void handleModifierSourcesRequest(ModifierSourcesRequestPayload request, NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if ((player == null)) return;
            var entries = new ArrayList<>(NeedsNotNecessitiesSourceBridge.sources(player));
            entries.addAll(PufferfishSkillsSourceBridge.sources(player));
            sendToPlayer(player, new ModifierSourcesResponsePayload(
                    entries, ArmorDamageScalingBridge.values(player)));
        });
    }

    private static void handleModifierSourcesResponse(ModifierSourcesResponsePayload response, NetworkEvent.Context context) {
        context.enqueueWork(() -> ClientAccess.accept(response));
    }

    private static Optional<EquipmentTargetAccess> resolveTarget(ServerPlayer player, EquipmentChangePayload payload) {
        return switch (payload.system()) {
            case VANILLA -> VanillaEquipmentTarget.resolve(player, payload);
            case CURIOS -> ModList.get().isLoaded("curios") ? CuriosServerAccess.resolve(player, payload) : Optional.empty();
        };
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
