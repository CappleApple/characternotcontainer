package com.cappleapple.characternotcontainer.config;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum StatFormat {
    INTEGER,
    DECIMAL,
    PERCENT;

    public String format(double value, StatDefinition definition) {
        double transformed = value * definition.effectiveScale();
        if (this == PERCENT) transformed *= 100.0D;
        int places = this == INTEGER ? 0 : definition.effectiveDecimalPlaces();
        String text = BigDecimal.valueOf(transformed).setScale(places, RoundingMode.HALF_UP).toPlainString();
        String suffix = definition.suffix == null ? "" : definition.suffix;
        if (this == PERCENT && suffix.isEmpty()) suffix = "%";
        return text + suffix;
    }
}
