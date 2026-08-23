package com.cappleapple.characternotcontainer.config;

public final class StatDefinition {
    public String attribute = "minecraft:generic.max_health";
    public String name = "";
    public String description = "";
    public String icon = "";
    public StatFormat format = StatFormat.DECIMAL;
    public boolean showDelta = true;
    public boolean higherIsBetter = true;
    public int decimalPlaces = 1;
    public double scale = 1.0D;
    public double offset = 0.0D;
    public boolean invert = false;
    public String prefix = "";
    public String suffix = "";

    public StatDefinition() {}

    public StatDefinition(String attribute, String name, StatFormat format, int decimalPlaces) {
        this.attribute = attribute;
        this.name = name;
        this.format = format;
        this.decimalPlaces = decimalPlaces;
    }
}
