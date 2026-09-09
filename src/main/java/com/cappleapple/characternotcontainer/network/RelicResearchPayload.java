package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record RelicResearchPayload(EquipmentChangePayload target, ItemStack expected) implements CustomPacketPayload {
    public static final Type<RelicResearchPayload> TYPE = new Type<>(CharacterNotContainer.id("relic_research"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RelicResearchPayload> STREAM_CODEC = StreamCodec.composite(
            EquipmentChangePayload.STREAM_CODEC, RelicResearchPayload::target,
            ItemStack.STREAM_CODEC, RelicResearchPayload::expected, RelicResearchPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
