package com.cappleapple.characternotcontainer.compat.armordamagescaling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorDamageScalingBridgeTest {
    @Test
    void resistanceUsesRemainingDamageRelativeToIncomingDamage() {
        assertEquals(0.75D, ArmorDamageScalingBridge.resistance(20.0D, 5.0D).orElseThrow());
        assertEquals(-0.25D, ArmorDamageScalingBridge.resistance(20.0D, 25.0D).orElseThrow());
    }

    @Test
    void resistanceRejectsInvalidDamageValues() {
        assertTrue(ArmorDamageScalingBridge.resistance(0.0D, 0.0D).isEmpty());
        assertTrue(ArmorDamageScalingBridge.resistance(Double.NaN, 1.0D).isEmpty());
        assertTrue(ArmorDamageScalingBridge.resistance(20.0D, Double.POSITIVE_INFINITY).isEmpty());
    }
}
