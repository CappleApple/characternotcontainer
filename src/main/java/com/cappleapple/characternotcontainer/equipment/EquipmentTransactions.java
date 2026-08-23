package com.cappleapple.characternotcontainer.equipment;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public final class EquipmentTransactions {
    private EquipmentTransactions() {}

    public static boolean swapFromInventory(Inventory inventory, int sourceIndex, ItemStack equipped,
                                            Consumer<ItemStack> equipAction) {
        if (sourceIndex < 0 || sourceIndex >= inventory.items.size()) return false;
        ItemStack source = inventory.items.get(sourceIndex);
        if (source.isEmpty()) return false;
        ItemStack toEquip = source.copy();
        toEquip.setCount(1);

        if (source.getCount() == 1) {
            inventory.items.set(sourceIndex, equipped.copy());
        } else if (equipped.isEmpty()) {
            source.shrink(1);
        } else {
            int destination = findDestination(inventory, equipped, sourceIndex);
            if (destination < 0) return false;
            insertAt(inventory, destination, equipped);
            source.shrink(1);
        }
        equipAction.accept(toEquip);
        inventory.setChanged();
        return true;
    }

    public static boolean unequip(Inventory inventory, ItemStack equipped, Consumer<ItemStack> equipAction) {
        if (equipped.isEmpty()) return true;
        int destination = findDestination(inventory, equipped, -1);
        if (destination < 0) return false;
        insertAt(inventory, destination, equipped);
        equipAction.accept(ItemStack.EMPTY);
        inventory.setChanged();
        return true;
    }

    public static boolean canInsert(Inventory inventory, ItemStack stack) {
        return stack.isEmpty() || findDestination(inventory, stack, -1) >= 0;
    }

    public static boolean insert(Inventory inventory, ItemStack stack) {
        if (stack.isEmpty()) return true;
        int destination = findDestination(inventory, stack, -1);
        if (destination < 0) return false;
        insertAt(inventory, destination, stack);
        inventory.setChanged();
        return true;
    }

    private static int findDestination(Inventory inventory, ItemStack stack, int excluded) {
        for (int index = 0; index < inventory.items.size(); index++) {
            if (index == excluded) continue;
            ItemStack existing = inventory.items.get(index);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) return index;
        }
        for (int index = 0; index < inventory.items.size(); index++) {
            if (index != excluded && inventory.items.get(index).isEmpty()) return index;
        }
        return -1;
    }

    private static void insertAt(Inventory inventory, int index, ItemStack stack) {
        ItemStack existing = inventory.items.get(index);
        if (existing.isEmpty()) inventory.items.set(index, stack.copy());
        else existing.grow(stack.getCount());
    }
}
