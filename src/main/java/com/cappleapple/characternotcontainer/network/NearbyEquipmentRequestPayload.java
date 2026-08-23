package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NearbyEquipmentRequestPayload(int searchId, EquipmentChangePayload.TargetSystem system,
                                            String slotId, int slotIndex, boolean cosmetic)
        implements CustomPacketPayload {
    public static final Type<NearbyEquipmentRequestPayload> TYPE =
            new Type<>(CharacterNotContainer.id("nearby_equipment_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyEquipmentRequestPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public NearbyEquipmentRequestPayload decode(RegistryFriendlyByteBuf buffer) {
            return new NearbyEquipmentRequestPayload(buffer.readVarInt(),
                    buffer.readEnum(EquipmentChangePayload.TargetSystem.class), buffer.readUtf(64),
                    buffer.readVarInt(), buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, NearbyEquipmentRequestPayload payload) {
            buffer.writeVarInt(payload.searchId);
            buffer.writeEnum(payload.system);
            buffer.writeUtf(payload.slotId, 64);
            buffer.writeVarInt(payload.slotIndex);
            buffer.writeBoolean(payload.cosmetic);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
