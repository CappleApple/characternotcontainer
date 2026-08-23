package com.cappleapple.characternotcontainer.equipment;

import net.minecraft.world.item.ItemStack;

public interface EquipmentTargetAccess {
    ItemStack equipped();

    boolean accepts(ItemStack stack);

    boolean canRemove();

    void set(ItemStack stack);
}
