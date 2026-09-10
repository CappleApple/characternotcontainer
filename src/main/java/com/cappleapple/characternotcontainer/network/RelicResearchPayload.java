package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record RelicResearchPayload(EquipmentChangePayload target, ItemStack expected)  {
    public static final net.minecraft.resources.ResourceLocation TYPE = CharacterNotContainer.id("relic_research");
    public static final PacketCodec<RelicResearchPayload> STREAM_CODEC = new PacketCodec<>() {
        @Override public RelicResearchPayload decode(FriendlyByteBuf buffer) {
            return new RelicResearchPayload(EquipmentChangePayload.STREAM_CODEC.decode(buffer), buffer.readItem());
        }
        @Override public void encode(FriendlyByteBuf buffer, RelicResearchPayload payload) {
            EquipmentChangePayload.STREAM_CODEC.encode(buffer, payload.target());
            buffer.writeItem(payload.expected());
        }
    };
}
