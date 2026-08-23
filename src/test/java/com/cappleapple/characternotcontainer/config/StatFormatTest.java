package com.cappleapple.characternotcontainer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatFormatTest {
    @Test
    void supportsRequiredBuiltInFormats() {
        StatDefinition stat = new StatDefinition();
        stat.decimalPlaces = 1;
        assertEquals("1.3", StatFormat.RAW.format(1.25, stat, false));
        assertEquals("18", StatFormat.INTEGER.format(17.6, stat, false));
        assertEquals("12.5", StatFormat.DECIMAL.format(12.5, stat, false));
        assertEquals("115.0%", StatFormat.PERCENT.format(1.15, stat, false));
        assertEquals("+15.0%", StatFormat.PERCENT_DELTA.format(1.15, stat, false));
        assertEquals("1.2x", StatFormat.MULTIPLIER.format(1.15, stat, false));
        assertEquals("1:30", StatFormat.TIME.format(1800, stat, false));
    }

    @Test
    void customFormatAppliesInversionScaleOffsetAndDecoration() {
        StatDefinition stat = new StatDefinition();
        stat.format = StatFormat.CUSTOM;
        stat.scale = 100;
        stat.offset = -100;
        stat.decimalPlaces = 0;
        stat.prefix = "[";
        stat.suffix = "%]";
        assertEquals("[15%]", StatFormat.CUSTOM.format(1.15, stat, false));
        assertEquals("[+15%]", StatFormat.CUSTOM.formatDifference(1.0, 1.15, stat));

        stat.invert = true;
        stat.scale = 1;
        stat.offset = 0;
        stat.prefix = "";
        stat.suffix = "";
        stat.decimalPlaces = 2;
        assertEquals("0.50", StatFormat.CUSTOM.format(2.0, stat, false));
        assertEquals("-0.50", StatFormat.CUSTOM.formatDifference(1.0, 2.0, stat));
    }

    @Test
    void deltasAreSignedAndPercentDeltaDoesNotSubtractBaselineTwice() {
        StatDefinition stat = new StatDefinition();
        stat.decimalPlaces = 0;
        assertEquals("+15%", StatFormat.PERCENT_DELTA.format(0.15, stat, true));
        assertEquals("-5%", StatFormat.PERCENT.format(-0.05, stat, true));
        assertEquals("0%", StatFormat.PERCENT.format(0, stat, true));
    }
}
