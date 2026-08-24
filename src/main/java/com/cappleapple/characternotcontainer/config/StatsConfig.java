package com.cappleapple.characternotcontainer.config;

import java.util.ArrayList;
import java.util.List;

public final class StatsConfig {
    public List<StatDefinition> attributes = new ArrayList<>();

    public static StatsConfig defaults() {
        StatsConfig config = new StatsConfig();
        config.attributes.add(new StatDefinition("minecraft:generic.attack_damage", StatFormat.DECIMAL, 1));
        config.attributes.add(new StatDefinition("minecraft:generic.attack_speed", StatFormat.DECIMAL, 2));
        config.attributes.add(new StatDefinition("minecraft:generic.attack_knockback", StatFormat.DECIMAL, 1));
        config.attributes.add(new StatDefinition("minecraft:generic.armor", StatFormat.INTEGER, 0));
        config.attributes.add(new StatDefinition("minecraft:generic.armor_toughness", StatFormat.INTEGER, 0));
        config.attributes.add(new StatDefinition("minecraft:generic.knockback_resistance", StatFormat.PERCENT, 0));
        config.attributes.add(new StatDefinition("minecraft:generic.max_health", StatFormat.DECIMAL, 1));

        StatDefinition movementSpeed = new StatDefinition("minecraft:generic.movement_speed", StatFormat.DECIMAL, 0);
        movementSpeed.scale = 1000.0D;
        movementSpeed.suffix = "%";
        config.attributes.add(movementSpeed);
        StatDefinition flyingSpeed = new StatDefinition("minecraft:generic.flying_speed", StatFormat.DECIMAL, 0);
        flyingSpeed.scale = 1000.0D;
        flyingSpeed.suffix = "%";
        config.attributes.add(flyingSpeed);

        config.attributes.add(new StatDefinition("minecraft:generic.jump_strength", StatFormat.DECIMAL, 2));
        config.attributes.add(new StatDefinition("minecraft:generic.step_height", StatFormat.DECIMAL, 1));
        config.attributes.add(new StatDefinition("minecraft:player.block_interaction_range", StatFormat.DECIMAL, 1));
        config.attributes.add(new StatDefinition("minecraft:player.entity_interaction_range", StatFormat.DECIMAL, 1));
        config.attributes.add(new StatDefinition("minecraft:generic.luck", StatFormat.DECIMAL, 1));
        return config;
    }
}
