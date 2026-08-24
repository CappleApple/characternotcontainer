package com.cappleapple.characternotcontainer.client;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;

/** Applies an attribute's own sanitizer to preview and per-source values. */
final class AttributeValueCalculator {
    private AttributeValueCalculator() {}

    static double calculate(AttributeInstance instance, Iterable<AttributeModifier> modifiers) {
        double added = instance.getBaseValue();
        for (AttributeModifier modifier : modifiers) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) added += modifier.amount();
        }
        double multiplied = added;
        for (AttributeModifier modifier : modifiers) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) multiplied += added * modifier.amount();
        }
        for (AttributeModifier modifier : modifiers) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) multiplied *= 1.0D + modifier.amount();
        }
        return instance.getAttribute().value().sanitizeValue(multiplied);
    }

    static double standaloneAddValue(AttributeInstance instance, AttributeModifier modifier) {
        if (modifier.operation() != AttributeModifier.Operation.ADD_VALUE) {
            throw new IllegalArgumentException("Standalone ADD_VALUE calculation requires an ADD_VALUE modifier");
        }
        double withoutSource = calculate(instance, List.of());
        double withSource = calculate(instance, List.of(modifier));
        return withSource - withoutSource;
    }
}
