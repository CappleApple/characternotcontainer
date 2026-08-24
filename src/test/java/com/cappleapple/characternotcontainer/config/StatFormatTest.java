package com.cappleapple.characternotcontainer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatFormatTest {
    @Test
    void formatsCurrentValuesWithoutDeltaDecoration() {
        StatDefinition stat = new StatDefinition();
        stat.decimalPlaces = 1;
        assertEquals("18", StatFormat.INTEGER.format(17.6, stat));
        assertEquals("12.5", StatFormat.DECIMAL.format(12.5, stat));
        assertEquals("115.0%", StatFormat.PERCENT.format(1.15, stat));
    }

    @Test
    void scaleAndSuffixRemainAvailableForDisplayUnits() {
        StatDefinition stat = new StatDefinition();
        stat.scale = 1000.0D;
        stat.decimalPlaces = 0;
        stat.suffix = "%";
        assertEquals("100%", StatFormat.DECIMAL.format(0.1D, stat));
    }
}
