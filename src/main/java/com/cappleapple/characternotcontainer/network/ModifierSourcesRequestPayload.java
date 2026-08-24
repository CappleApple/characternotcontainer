package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ModifierSourcesRequestPayload() implements CustomPacketPayload {
    public static final ModifierSourcesRequestPayload INSTANCE = new ModifierSourcesRequestPayload();
    public static final Type<ModifierSourcesRequestPayload> TYPE = new Type<>(CharacterNotContainer.id("modifier_sources_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ModifierSourcesRequestPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ModifierSourcesRequestPayload decode(RegistryFriendlyByteBuf buffer) { return INSTANCE; }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ModifierSourcesRequestPayload payload) {}
    };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
