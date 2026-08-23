package com.cappleapple.characternotcontainer.config;

import java.math.RoundingMode;
import java.util.Locale;

public enum StatFormat {
    RAW,
    INTEGER,
    DECIMAL,
    PERCENT,
    PERCENT_DELTA,
    MULTIPLIER,
    TIME,
    CUSTOM;

    public String format(double value, StatDefinition definition, boolean delta) {
        double transformed = transform(value, definition, delta);
        return render(transformed, definition, delta);
    }

    public String formatDifference(double base, double current, StatDefinition definition) {
        return render(displayDifference(base, current, definition), definition, true);
    }

    public double displayDifference(double base, double current, StatDefinition definition) {
        return transform(current, definition, false) - transform(base, definition, false);
    }

    private String render(double transformed, StatDefinition definition, boolean delta) {
        int places = this == INTEGER || this == TIME ? 0 : Math.max(0, definition.decimalPlaces);
        String text;
        if (this == TIME) {
            long seconds = Math.round(transformed / 20.0D);
            text = String.format(Locale.ROOT, "%d:%02d", seconds / 60, Math.abs(seconds % 60));
        } else {
            text = java.math.BigDecimal.valueOf(transformed).setScale(places, RoundingMode.HALF_UP).toPlainString();
        }
        boolean signed = delta || this == PERCENT_DELTA;
        if (signed && transformed > 0.0D) text = "+" + text;
        return safe(definition.prefix) + text + defaultSuffix(definition) + safe(definition.suffix);
    }

    private double transform(double value, StatDefinition definition, boolean delta) {
        double transformed = definition.invert && value != 0.0D ? 1.0D / value : value;
        return switch (this) {
            case INTEGER, DECIMAL, RAW, MULTIPLIER, TIME -> transformed;
            case PERCENT -> transformed * 100.0D;
            case PERCENT_DELTA -> (delta ? transformed : transformed - 1.0D) * 100.0D;
            case CUSTOM -> transformed * definition.scale + (delta ? 0.0D : definition.offset);
        };
    }

    private String defaultSuffix(StatDefinition definition) {
        if (definition.suffix != null && !definition.suffix.isEmpty()) return "";
        return switch (this) {
            case PERCENT, PERCENT_DELTA -> "%";
            case MULTIPLIER -> "x";
            default -> "";
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
