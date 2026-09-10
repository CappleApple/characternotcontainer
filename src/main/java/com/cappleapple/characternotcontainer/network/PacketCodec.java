package com.cappleapple.characternotcontainer.network;

import net.minecraft.network.FriendlyByteBuf;

public interface PacketCodec<T> {
    T decode(FriendlyByteBuf buffer);
    void encode(FriendlyByteBuf buffer, T payload);
}
