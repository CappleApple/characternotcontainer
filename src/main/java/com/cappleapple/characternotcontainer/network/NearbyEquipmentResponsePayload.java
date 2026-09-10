package com.cappleapple.characternotcontainer.network;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record NearbyEquipmentResponsePayload(int searchId, boolean enabled, List<Entry> entries)
         {
    public static final int MAX_ENTRIES = 256;
    public static final net.minecraft.resources.ResourceLocation TYPE = CharacterNotContainer.id("nearby_equipment_response");
    public static final PacketCodec<NearbyEquipmentResponsePayload> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public NearbyEquipmentResponsePayload decode(FriendlyByteBuf buffer) {
            int searchId = buffer.readVarInt();
            boolean enabled = buffer.readBoolean();
            int size = buffer.readVarInt();
            if (size < 0 || size > MAX_ENTRIES) throw new IllegalArgumentException("Invalid nearby equipment entry count: " + size);
            List<Entry> entries = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                entries.add(new Entry(buffer.readVarInt(), buffer.readItem()));
            }
            return new NearbyEquipmentResponsePayload(searchId, enabled, List.copyOf(entries));
        }

        @Override
        public void encode(FriendlyByteBuf buffer, NearbyEquipmentResponsePayload payload) {
            buffer.writeVarInt(payload.searchId);
            buffer.writeBoolean(payload.enabled);
            int size = Math.min(MAX_ENTRIES, payload.entries.size());
            buffer.writeVarInt(size);
            for (int index = 0; index < size; index++) {
                Entry entry = payload.entries.get(index);
                buffer.writeVarInt(entry.token);
                buffer.writeItem(entry.stack);
            }
        }
    };

    public NearbyEquipmentResponsePayload {
        entries = List.copyOf(entries);
    }

    public record Entry(int token, ItemStack stack) {
        public Entry {
            stack = stack.copy();
        }
    }
}
