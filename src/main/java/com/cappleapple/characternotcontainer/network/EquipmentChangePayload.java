package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.network.FriendlyByteBuf;

public record EquipmentChangePayload(TargetSystem system, String slotId, int slotIndex, boolean cosmetic,
                                     SourceKind sourceKind, int sourceIndex, int searchId)
         {
    public static final net.minecraft.resources.ResourceLocation TYPE = CharacterNotContainer.id("equipment_change");
    public static final PacketCodec<EquipmentChangePayload> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public EquipmentChangePayload decode(FriendlyByteBuf buffer) {
            return new EquipmentChangePayload(buffer.readEnum(TargetSystem.class), buffer.readUtf(64),
                    buffer.readVarInt(), buffer.readBoolean(), buffer.readEnum(SourceKind.class),
                    buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void encode(FriendlyByteBuf buffer, EquipmentChangePayload payload) {
            buffer.writeEnum(payload.system);
            buffer.writeUtf(payload.slotId, 64);
            buffer.writeVarInt(payload.slotIndex);
            buffer.writeBoolean(payload.cosmetic);
            buffer.writeEnum(payload.sourceKind);
            buffer.writeVarInt(payload.sourceIndex);
            buffer.writeVarInt(payload.searchId);
        }
    };

    public enum TargetSystem { VANILLA, CURIOS }

    public enum SourceKind { UNEQUIP, PLAYER_INVENTORY, NEARBY }
}
