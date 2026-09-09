package com.cappleapple.characternotcontainer.equipment;

import com.cappleapple.characternotcontainer.compat.relics.RelicResearchSource;
import com.cappleapple.characternotcontainer.network.EquipmentChangePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class VanillaEquipmentTarget {
    private VanillaEquipmentTarget() {}

    public static Optional<EquipmentTargetAccess> resolve(ServerPlayer player, EquipmentChangePayload payload) {
        if (payload.cosmetic() || payload.slotIndex() != 0) return Optional.empty();
        EquipmentSlot slot = switch (payload.slotId()) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
        return slot == null ? Optional.empty() : Optional.of(forSlot(player, slot));
    }

    static EquipmentTargetAccess forSlot(ServerPlayer player, EquipmentSlot slot) {
        return new EquipmentTargetAccess() {
            @Override
            public ItemStack equipped() {
                return player.getItemBySlot(slot).copy();
            }

            @Override
            public Optional<RelicResearchSource> researchSource() {
                return Optional.of(new RelicResearchSource(
                        () -> player.getItemBySlot(slot), player::isAlive, () -> player.getInventory().setChanged()));
            }

            @Override
            public boolean accepts(ItemStack stack) {
                return !stack.isEmpty() && player.getEquipmentSlotForItem(stack) == slot && stack.canEquip(slot, player);
            }

            @Override
            public boolean canRemove() {
                return true;
            }

            @Override
            public void set(ItemStack stack) {
                player.setItemSlot(slot, stack.copy());
            }
        };
    }
}
