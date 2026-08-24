package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ModifierSourcesResponsePayload(List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 256;
    private static final int MAX_SOURCES = 4;
    public static final Type<ModifierSourcesResponsePayload> TYPE = new Type<>(CharacterNotContainer.id("modifier_sources_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ModifierSourcesResponsePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ModifierSourcesResponsePayload decode(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            if (size < 0 || size > MAX_ENTRIES) throw new IllegalArgumentException("Invalid modifier source count: " + size);
            List<Entry> entries = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                ResourceLocation attribute = buffer.readResourceLocation();
                ResourceLocation modifier = buffer.readResourceLocation();
                int sourceCount = buffer.readVarInt();
                if (sourceCount < 0 || sourceCount > MAX_SOURCES) throw new IllegalArgumentException("Invalid source candidate count: " + sourceCount);
                List<Source> sources = new ArrayList<>(sourceCount);
                for (int sourceIndex = 0; sourceIndex < sourceCount; sourceIndex++) {
                    ItemStack stack = buffer.readBoolean() ? ItemStack.STREAM_CODEC.decode(buffer) : ItemStack.EMPTY;
                    sources.add(new Source(stack, buffer.readUtf(256), buffer.readUtf(256)));
                }
                entries.add(new Entry(attribute, modifier, sources));
            }
            return new ModifierSourcesResponsePayload(entries);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ModifierSourcesResponsePayload payload) {
            int size = Math.min(MAX_ENTRIES, payload.entries.size());
            buffer.writeVarInt(size);
            for (int index = 0; index < size; index++) {
                Entry entry = payload.entries.get(index);
                buffer.writeResourceLocation(entry.attributeId);
                buffer.writeResourceLocation(entry.modifierId);
                int sourceCount = Math.min(MAX_SOURCES, entry.sources.size());
                buffer.writeVarInt(sourceCount);
                for (int sourceIndex = 0; sourceIndex < sourceCount; sourceIndex++) {
                    Source source = entry.sources.get(sourceIndex);
                    buffer.writeBoolean(!source.stack.isEmpty());
                    if (!source.stack.isEmpty()) ItemStack.STREAM_CODEC.encode(buffer, source.stack);
                    buffer.writeUtf(source.translationKey, 256);
                    buffer.writeUtf(source.fallback, 256);
                }
            }
        }
    };

    public ModifierSourcesResponsePayload { entries = List.copyOf(entries); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record Entry(ResourceLocation attributeId, ResourceLocation modifierId, List<Source> sources) {
        public Entry { sources = List.copyOf(sources); }
    }

    public record Source(ItemStack stack, String translationKey, String fallback) {
        public Source {
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
            translationKey = translationKey == null ? "" : translationKey;
            fallback = fallback == null ? "" : fallback;
        }
    }
}
