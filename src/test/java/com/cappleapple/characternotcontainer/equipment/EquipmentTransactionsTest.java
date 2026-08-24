package com.cappleapple.characternotcontainer.equipment;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentTransactionsTest {
    @Test
    void swapsThroughAnItemHandlerInsteadOfVanillaInventoryLists() {
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND_BOOTS));
        AtomicReference<ItemStack> equipped = new AtomicReference<>(new ItemStack(Items.IRON_BOOTS));

        assertTrue(EquipmentTransactions.swapFromInventory(inventory, 0, equipped::get, equipped::set));
        assertTrue(equipped.get().is(Items.DIAMOND_BOOTS));
        assertTrue(inventory.getStackInSlot(0).is(Items.IRON_BOOTS));
    }

    @Test
    void extractedSlotCreatesCapacityForThePreviouslyEquippedItem() {
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND_HELMET));
        AtomicReference<ItemStack> equipped = new AtomicReference<>(new ItemStack(Items.IRON_HELMET));

        assertTrue(EquipmentTransactions.swapFromInventory(inventory, 0, equipped::get, equipped::set));
        assertTrue(equipped.get().is(Items.DIAMOND_HELMET));
        assertTrue(inventory.getStackInSlot(0).is(Items.IRON_HELMET));
    }

    @Test
    void refusesAStackedSourceWhenThereIsNoRoomForCurrentEquipment() {
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 2));
        AtomicReference<ItemStack> equipped = new AtomicReference<>(new ItemStack(Items.IRON_INGOT));

        assertFalse(EquipmentTransactions.swapFromInventory(inventory, 0, equipped::get, equipped::set));
        assertEquals(2, inventory.getStackInSlot(0).getCount());
        assertTrue(equipped.get().is(Items.IRON_INGOT));
    }

    @Test
    void supportsHandlersWhoseIndicesCompactAfterExtraction() {
        CompactingHandler inventory = new CompactingHandler(
                new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.GOLD_INGOT));
        AtomicReference<ItemStack> equipped = new AtomicReference<>(new ItemStack(Items.IRON_CHESTPLATE));

        assertTrue(EquipmentTransactions.swapFromInventory(inventory, 0, equipped::get, equipped::set));
        assertTrue(equipped.get().is(Items.DIAMOND_CHESTPLATE));
        assertTrue(inventory.stacks.stream().anyMatch(stack -> stack.is(Items.IRON_CHESTPLATE)));
        assertTrue(inventory.stacks.stream().anyMatch(stack -> stack.is(Items.GOLD_INGOT)));
    }

    @Test
    void doesNotDuplicateWhenTheHandlerSourceIsTheEquipmentTarget() {
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND_CHESTPLATE));

        assertTrue(EquipmentTransactions.swapFromInventory(inventory, 0,
                () -> inventory.getStackInSlot(0), stack -> inventory.setStackInSlot(0, stack)));
        assertTrue(inventory.getStackInSlot(0).is(Items.DIAMOND_CHESTPLATE));
        assertTrue(inventory.getStackInSlot(1).isEmpty());
    }

    @Test
    void unequipDoesNotInsertBackIntoAStackableAliasedTarget() {
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 2));

        assertTrue(EquipmentTransactions.unequip(inventory,
                () -> inventory.getStackInSlot(0), stack -> inventory.setStackInSlot(0, stack)));
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertEquals(2, inventory.getStackInSlot(1).getCount());
    }

    private static final class CompactingHandler implements IItemHandler {
        private final List<ItemStack> stacks = new ArrayList<>();

        private CompactingHandler(ItemStack... initial) {
            for (ItemStack stack : initial) stacks.add(stack.copy());
        }

        @Override
        public int getSlots() {
            return stacks.size() + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == stacks.size() ? ItemStack.EMPTY : stacks.get(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < stacks.size()) return stack;
            if (!simulate) stacks.add(stack.copy());
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= stacks.size() || amount <= 0) return ItemStack.EMPTY;
            ItemStack extracted = stacks.get(slot).copy();
            extracted.setCount(1);
            if (!simulate) stacks.remove(slot);
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty();
        }
    }
}
