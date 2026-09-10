package com.cappleapple.characternotcontainer.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PacketCodecTest {
    @Test void equipmentAndNearbyRequestsRetainTheirTargetAndSearchToken() {
        var target = new EquipmentChangePayload(EquipmentChangePayload.TargetSystem.CURIOS, "ring", 2, true,
                EquipmentChangePayload.SourceKind.NEARBY, 17, 42);
        assertEquals(target, roundTrip(EquipmentChangePayload.STREAM_CODEC, target));
        var request = new NearbyEquipmentRequestPayload(42, target.system(), target.slotId(), target.slotIndex(), true);
        assertEquals(request, roundTrip(NearbyEquipmentRequestPayload.STREAM_CODEC, request));
    }

    @Test void relicAndNearbyStacksRetainNbt() {
        ItemStack stack = new ItemStack(Items.DIAMOND_BOOTS);
        stack.setHoverName(Component.literal("Test boots"));
        stack.setDamageValue(7);
        var target = new EquipmentChangePayload(EquipmentChangePayload.TargetSystem.VANILLA, "feet", 0, false,
                EquipmentChangePayload.SourceKind.PLAYER_INVENTORY, 3, 0);
        var relic = roundTrip(RelicResearchPayload.STREAM_CODEC, new RelicResearchPayload(target, stack));
        assertEquals(target, relic.target());
        assertTrue(ItemStack.matches(stack, relic.expected()));
        var nearby = roundTrip(NearbyEquipmentResponsePayload.STREAM_CODEC,
                new NearbyEquipmentResponsePayload(42, true, List.of(new NearbyEquipmentResponsePayload.Entry(9, stack))));
        assertEquals(9, nearby.entries().get(0).token());
        assertTrue(ItemStack.matches(stack, nearby.entries().get(0).stack()));
    }

    @Test void sourceResponseRetainsUuidAttributionAndResistance() {
        UUID id = UUID.fromString("cc000000-0000-0000-0000-000000000042");
        var source = new ModifierSourcesResponsePayload.Source(ItemStack.EMPTY, "skill.test", "Strength III", "strength");
        var value = new ModifierSourcesResponsePayload(List.of(new ModifierSourcesResponsePayload.Entry(
                new ResourceLocation("minecraft:generic.attack_damage"), id, List.of(source))),
                Optional.of(new ModifierSourcesResponsePayload.ArmorDamageScalingValues(0.25, 0.5)));
        var decoded = roundTrip(ModifierSourcesResponsePayload.STREAM_CODEC, value);
        assertEquals(id, decoded.entries().get(0).modifierId());
        assertEquals("strength", decoded.entries().get(0).sources().get(0).aggregationKey());
        assertEquals(value.armorDamageScaling(), decoded.armorDamageScaling());
    }

    @Test void oversizedAndNegativeResponseListsAreRejected() {
        for (int size : new int[]{-1, 257}) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeVarInt(size);
                assertThrows(IllegalArgumentException.class, () -> ModifierSourcesResponsePayload.STREAM_CODEC.decode(buffer));
                buffer.clear(); buffer.writeVarInt(1); buffer.writeBoolean(true); buffer.writeVarInt(size);
                assertThrows(IllegalArgumentException.class, () -> NearbyEquipmentResponsePayload.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
    }

    @Test void nonFiniteResistanceIsRejectedOnDecode() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(0); buffer.writeBoolean(true); buffer.writeDouble(Double.NaN); buffer.writeDouble(0.5);
            assertThrows(IllegalArgumentException.class, () -> ModifierSourcesResponsePayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }

    private static <T> T roundTrip(PacketCodec<T> codec, T packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(buffer, packet);
            T decoded = codec.decode(buffer);
            assertEquals(0, buffer.readableBytes());
            return decoded;
        } finally { buffer.release(); }
    }
}
