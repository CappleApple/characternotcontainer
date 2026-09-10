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
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) added += modifier.getAmount();
        }
        double multiplied = added;
        for (AttributeModifier modifier : modifiers) {
            if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) multiplied += added * modifier.getAmount();
        }
        for (AttributeModifier modifier : modifiers) {
            if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) multiplied *= 1.0D + modifier.getAmount();
        }
        return instance.getAttribute().sanitizeValue(multiplied);
    }

    static double standaloneAddValue(AttributeInstance instance, AttributeModifier modifier) {
        if (modifier.getOperation() != AttributeModifier.Operation.ADDITION) {
            throw new IllegalArgumentException("Standalone ADDITION calculation requires an ADDITION modifier");
        }
        double withoutSource = calculate(instance, List.of());
        double withSource = calculate(instance, List.of(modifier));
        return withSource - withoutSource;
    }
}
