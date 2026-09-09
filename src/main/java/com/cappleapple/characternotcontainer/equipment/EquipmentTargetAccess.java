package com.cappleapple.characternotcontainer.equipment;

import com.cappleapple.characternotcontainer.compat.relics.RelicResearchSource;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public interface EquipmentTargetAccess {
    ItemStack equipped();

    default Optional<RelicResearchSource> researchSource() {
        return Optional.empty();
    }

    boolean accepts(ItemStack stack);

    boolean canRemove();

    void set(ItemStack stack);
}
