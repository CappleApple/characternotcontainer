package com.cappleapple.characternotcontainer.config;

/** A deliberately small, flat attribute display definition. */
public final class StatDefinition {
    public String attribute = "minecraft:generic.max_health";
    public boolean visible = true;
    public String name;
    public String icon;
    public StatFormat format;
    public Integer decimalPlaces;
    public Double scale;
    public String suffix;

    public StatDefinition() {}

    public StatDefinition(String attribute) {
        this.attribute = attribute;
    }

    public StatDefinition(String attribute, StatFormat format, int decimalPlaces) {
        this.attribute = attribute;
        this.format = format;
        this.decimalPlaces = decimalPlaces;
    }

    public StatFormat effectiveFormat() {
        return format == null ? StatFormat.DECIMAL : format;
    }

    public int effectiveDecimalPlaces() {
        return decimalPlaces == null ? 2 : Math.max(0, Math.min(6, decimalPlaces));
    }

    public double effectiveScale() {
        return scale == null || !Double.isFinite(scale) ? 1.0D : scale;
    }
}
