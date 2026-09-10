package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.network.FriendlyByteBuf;

public record ModifierSourcesRequestPayload()  {
    public static final ModifierSourcesRequestPayload INSTANCE = new ModifierSourcesRequestPayload();
    public static final net.minecraft.resources.ResourceLocation TYPE = CharacterNotContainer.id("modifier_sources_request");
    public static final PacketCodec<ModifierSourcesRequestPayload> STREAM_CODEC = new PacketCodec<>() {
        @Override public ModifierSourcesRequestPayload decode(FriendlyByteBuf buffer) { return INSTANCE; }
        @Override public void encode(FriendlyByteBuf buffer, ModifierSourcesRequestPayload payload) {}
    };
}
