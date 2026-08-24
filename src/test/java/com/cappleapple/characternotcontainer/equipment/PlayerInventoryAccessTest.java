package com.cappleapple.characternotcontainer.equipment;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class PlayerInventoryAccessTest {
    @Test
    void prefersAnAutomationHandlerExposedByAnInventoryOverhaul() {
        IItemHandler automation = new ItemStackHandler(80);
        IItemHandler vanillaEntity = new ItemStackHandler(41);
        IItemHandler fallback = new ItemStackHandler(41);

        assertSame(automation, PlayerInventoryAccess.preferredHandler(automation, vanillaEntity, fallback));
    }

    @Test
    void fallsBackToTheOrdinaryEntityHandler() {
        IItemHandler entity = new ItemStackHandler(41);
        IItemHandler fallback = new ItemStackHandler(41);

        assertSame(entity, PlayerInventoryAccess.preferredHandler(null, entity, fallback));
    }

    @Test
    void retainsTheVanillaFallbackWhenNoCapabilityExists() {
        IItemHandler fallback = new ItemStackHandler(41);

        assertSame(fallback, PlayerInventoryAccess.preferredHandler(null, null, fallback));
    }
}
