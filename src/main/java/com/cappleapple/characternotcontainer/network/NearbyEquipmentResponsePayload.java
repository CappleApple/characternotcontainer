package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record NearbyEquipmentResponsePayload(int searchId, boolean enabled, List<Entry> entries)
        implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 256;
    public static final Type<NearbyEquipmentResponsePayload> TYPE =
            new Type<>(CharacterNotContainer.id("nearby_equipment_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyEquipmentResponsePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public NearbyEquipmentResponsePayload decode(RegistryFriendlyByteBuf buffer) {
            int searchId = buffer.readVarInt();
            boolean enabled = buffer.readBoolean();
            int size = buffer.readVarInt();
            if (size < 0 || size > MAX_ENTRIES) throw new IllegalArgumentException("Invalid nearby equipment entry count: " + size);
            List<Entry> entries = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                entries.add(new Entry(buffer.readVarInt(), ItemStack.STREAM_CODEC.decode(buffer)));
            }
            return new NearbyEquipmentResponsePayload(searchId, enabled, List.copyOf(entries));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, NearbyEquipmentResponsePayload payload) {
            buffer.writeVarInt(payload.searchId);
            buffer.writeBoolean(payload.enabled);
            int size = Math.min(MAX_ENTRIES, payload.entries.size());
            buffer.writeVarInt(size);
            for (int index = 0; index < size; index++) {
                Entry entry = payload.entries.get(index);
                buffer.writeVarInt(entry.token);
                ItemStack.STREAM_CODEC.encode(buffer, entry.stack);
            }
        }
    };

    public NearbyEquipmentResponsePayload {
        entries = List.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(int token, ItemStack stack) {
        public Entry {
            stack = stack.copy();
        }
    }
}
