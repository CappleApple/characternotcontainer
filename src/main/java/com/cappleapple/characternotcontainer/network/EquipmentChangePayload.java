package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EquipmentChangePayload(TargetSystem system, String slotId, int slotIndex, boolean cosmetic,
                                     SourceKind sourceKind, int sourceIndex, int searchId)
        implements CustomPacketPayload {
    public static final Type<EquipmentChangePayload> TYPE = new Type<>(CharacterNotContainer.id("equipment_change"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentChangePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EquipmentChangePayload decode(RegistryFriendlyByteBuf buffer) {
            return new EquipmentChangePayload(buffer.readEnum(TargetSystem.class), buffer.readUtf(64),
                    buffer.readVarInt(), buffer.readBoolean(), buffer.readEnum(SourceKind.class),
                    buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, EquipmentChangePayload payload) {
            buffer.writeEnum(payload.system);
            buffer.writeUtf(payload.slotId, 64);
            buffer.writeVarInt(payload.slotIndex);
            buffer.writeBoolean(payload.cosmetic);
            buffer.writeEnum(payload.sourceKind);
            buffer.writeVarInt(payload.sourceIndex);
            buffer.writeVarInt(payload.searchId);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public enum TargetSystem { VANILLA, CURIOS }

    public enum SourceKind { UNEQUIP, PLAYER_INVENTORY, NEARBY }
}
