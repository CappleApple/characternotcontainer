package com.cappleapple.characternotcontainer.compat.relics;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** A live slot; no item is extracted or moved into the research menu. */
public record RelicResearchSource(Supplier<ItemStack> stack, BooleanSupplier valid, Runnable changed) {
    public static RelicResearchSource handler(Supplier<IItemHandler> access, int index,
                                               BooleanSupplier valid, Runnable changed) {
        Supplier<ItemStack> stack = () -> {
            IItemHandler handler = access.get();
            return handler == null || index < 0 || index >= handler.getSlots()
                    ? ItemStack.EMPTY : handler.getStackInSlot(index);
        };
        return new RelicResearchSource(stack, valid, () -> {
            IItemHandler handler = access.get();
            if (handler instanceof IItemHandlerModifiable modifiable && index >= 0 && index < handler.getSlots()) {
                modifiable.setStackInSlot(index, handler.getStackInSlot(index));
            }
            changed.run();
        });
    }
}
