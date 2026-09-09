package com.cappleapple.characternotcontainer.compat.relics;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Keeps Relics optional, including on dedicated servers. */
public final class RelicsIntegration {
    private static final Class<?> RELIC_ITEM = findRelicItem();
    private RelicsIntegration() {}

    public static boolean isRelic(ItemStack stack) {
        return RELIC_ITEM != null && !stack.isEmpty() && RELIC_ITEM.isInstance(stack.getItem());
    }

    private static Class<?> findRelicItem() {
        if (!ModList.get().isLoaded("relics")) return null;
        try {
            return Class.forName("it.hurts.sskirillss.relics.items.relics.base.IRelicItem");
        } catch (ReflectiveOperationException | LinkageError exception) {
            CharacterNotContainer.LOGGER.warn("Relics research integration is unavailable", exception);
            return null;
        }
    }
}
