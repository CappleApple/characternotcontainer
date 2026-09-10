package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.network.FriendlyByteBuf;

public record NearbyEquipmentRequestPayload(int searchId, EquipmentChangePayload.TargetSystem system,
                                            String slotId, int slotIndex, boolean cosmetic)
         {
    public static final net.minecraft.resources.ResourceLocation TYPE = CharacterNotContainer.id("nearby_equipment_request");
    public static final PacketCodec<NearbyEquipmentRequestPayload> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public NearbyEquipmentRequestPayload decode(FriendlyByteBuf buffer) {
            return new NearbyEquipmentRequestPayload(buffer.readVarInt(),
                    buffer.readEnum(EquipmentChangePayload.TargetSystem.class), buffer.readUtf(64),
                    buffer.readVarInt(), buffer.readBoolean());
        }

        @Override
        public void encode(FriendlyByteBuf buffer, NearbyEquipmentRequestPayload payload) {
            buffer.writeVarInt(payload.searchId);
            buffer.writeEnum(payload.system);
            buffer.writeUtf(payload.slotId, 64);
            buffer.writeVarInt(payload.slotIndex);
            buffer.writeBoolean(payload.cosmetic);
        }
    };
}
