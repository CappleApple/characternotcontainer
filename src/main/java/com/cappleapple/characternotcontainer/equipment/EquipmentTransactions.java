package com.cappleapple.characternotcontainer.equipment;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class EquipmentTransactions {
    private EquipmentTransactions() {}

    public static boolean swapFromInventory(IItemHandler inventory, int sourceIndex, Supplier<ItemStack> equipped,
                                            Consumer<ItemStack> equipAction) {
        if (sourceIndex < 0 || sourceIndex >= inventory.getSlots()) return false;
        ItemStack previouslyEquipped = equipped.get().copy();
        ItemStack simulated = inventory.extractItem(sourceIndex, 1, true);
        if (simulated.isEmpty()) return false;

        ItemStack extracted = inventory.extractItem(sourceIndex, 1, false);
        if (extracted.isEmpty() || !ItemStack.isSameItemSameComponents(extracted, simulated)) {
            if (!extracted.isEmpty()) insert(inventory, extracted);
            return false;
        }

        boolean sourceAliasesTarget = !ItemStack.matches(previouslyEquipped, equipped.get());
        if (sourceAliasesTarget) {
            equipAction.accept(extracted);
            return true;
        }
        if (replaceEquipped(inventory, extracted, equipped, equipAction)) return true;
        insert(inventory, extracted);
        return false;
    }

    public static boolean unequip(IItemHandler inventory, Supplier<ItemStack> equipped, Consumer<ItemStack> equipAction) {
        return replaceEquipped(inventory, ItemStack.EMPTY, equipped, equipAction);
    }

    public static boolean equipExternal(IItemHandler inventory, ItemStack newEquipment,
                                        Supplier<ItemStack> equipped, Consumer<ItemStack> equipAction) {
        return replaceEquipped(inventory, newEquipment, equipped, equipAction);
    }

    private static boolean replaceEquipped(IItemHandler inventory, ItemStack newEquipment,
                                           Supplier<ItemStack> equipped, Consumer<ItemStack> equipAction) {
        ItemStack previous = equipped.get().copy();
        if (previous.isEmpty()) {
            equipAction.accept(newEquipment);
            return true;
        }

        ItemStack blocker = previous.copy();
        blocker.setCount(previous.getMaxStackSize());
        equipAction.accept(blocker);
        boolean stored = canInsert(inventory, previous) && insert(inventory, previous);
        equipAction.accept(stored ? newEquipment : previous);
        return stored;
    }

    private static boolean canInsert(IItemHandler inventory, ItemStack stack) {
        return stack.isEmpty() || ItemHandlerHelper.insertItemStacked(inventory, stack.copy(), true).isEmpty();
    }

    private static boolean insert(IItemHandler inventory, ItemStack stack) {
        if (stack.isEmpty()) return true;
        return ItemHandlerHelper.insertItemStacked(inventory, stack.copy(), false).isEmpty();
    }
}
