package com.cappleapple.characternotcontainer.config;

import java.util.ArrayList;
import java.util.List;

public final class StatsConfig {
    public List<StatCategory> categories = new ArrayList<>();

    public static StatsConfig defaults() {
        StatsConfig config = new StatsConfig();
        StatCategory offense = new StatCategory("offense", "Offense", 0);
        offense.stats.add(new StatDefinition("minecraft:generic.attack_damage", "Attack Damage", StatFormat.DECIMAL, 1));
        offense.stats.add(new StatDefinition("minecraft:generic.attack_speed", "Attack Speed", StatFormat.DECIMAL, 2));
        offense.stats.add(new StatDefinition("minecraft:generic.attack_knockback", "Attack Knockback", StatFormat.DECIMAL, 1));

        StatCategory defense = new StatCategory("defense", "Defense", 10);
        defense.stats.add(new StatDefinition("minecraft:generic.armor", "Armor", StatFormat.INTEGER, 0));
        defense.stats.add(new StatDefinition("minecraft:generic.armor_toughness", "Armor Toughness", StatFormat.INTEGER, 0));
        defense.stats.add(new StatDefinition("minecraft:generic.knockback_resistance", "Knockback Resistance", StatFormat.PERCENT, 0));
        defense.stats.add(new StatDefinition("minecraft:generic.max_health", "Max Health", StatFormat.DECIMAL, 1));

        StatCategory mobility = new StatCategory("mobility", "Mobility", 20);
        StatDefinition movementSpeed = new StatDefinition("minecraft:generic.movement_speed", "Movement Speed", StatFormat.CUSTOM, 0);
        movementSpeed.scale = 1000.0D;
        movementSpeed.suffix = "%";
        mobility.stats.add(movementSpeed);
        StatDefinition flyingSpeed = new StatDefinition("minecraft:generic.flying_speed", "Flying Speed", StatFormat.CUSTOM, 0);
        flyingSpeed.scale = 1000.0D;
        flyingSpeed.suffix = "%";
        mobility.stats.add(flyingSpeed);
        mobility.stats.add(new StatDefinition("minecraft:generic.jump_strength", "Jump Strength", StatFormat.DECIMAL, 2));
        mobility.stats.add(new StatDefinition("minecraft:generic.step_height", "Step Height", StatFormat.DECIMAL, 1));

        StatCategory utility = new StatCategory("utility", "Utility", 30);
        utility.stats.add(new StatDefinition("minecraft:player.block_interaction_range", "Block Reach", StatFormat.DECIMAL, 1));
        utility.stats.add(new StatDefinition("minecraft:player.entity_interaction_range", "Entity Reach", StatFormat.DECIMAL, 1));
        utility.stats.add(new StatDefinition("minecraft:generic.luck", "Luck", StatFormat.DECIMAL, 1));

        config.categories.add(offense);
        config.categories.add(defense);
        config.categories.add(mobility);
        config.categories.add(utility);
        return config;
    }
}
