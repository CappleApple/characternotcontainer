package com.cappleapple.characternotcontainer.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuriosClientIntegrationTest {
    @Test
    void cosmeticViewRetainsFunctionalSlotsWithoutCosmeticStorage() {
        assertTrue(CuriosClientIntegration.useCosmeticStorage(true, true));
        assertFalse(CuriosClientIntegration.useCosmeticStorage(true, false));
        assertFalse(CuriosClientIntegration.useCosmeticStorage(false, true));
    }
}
