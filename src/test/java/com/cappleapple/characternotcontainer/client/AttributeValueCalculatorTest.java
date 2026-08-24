package com.cappleapple.characternotcontainer.client;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributeValueCalculatorTest {
    @Test
    void reportsNonlinearStandaloneSanitizedAdditions() {
        AttributeInstance instance = instance(new NonlinearResistanceAttribute(30.0D));

        List<AttributeModifier> pieces = List.of(
                modifier("helmet", 5.0D), modifier("chestplate", 10.0D),
                modifier("leggings", 8.0D), modifier("boots", 5.0D));

        assertEquals(28.0D / 58.0D, AttributeValueCalculator.calculate(instance, pieces), 0.000001D);
        assertEquals(5.0D / 35.0D, AttributeValueCalculator.standaloneAddValue(instance, pieces.get(0)), 0.000001D);
        assertEquals(10.0D / 40.0D, AttributeValueCalculator.standaloneAddValue(instance, pieces.get(1)), 0.000001D);
        assertEquals(8.0D / 38.0D, AttributeValueCalculator.standaloneAddValue(instance, pieces.get(2)), 0.000001D);
        assertEquals(5.0D / 35.0D, AttributeValueCalculator.standaloneAddValue(instance, pieces.get(3)), 0.000001D);
    }

    @Test
    void reportsEachLinearSanitizedPieceInsteadOfTheCombinedValue() {
        AttributeInstance instance = instance(new QuarterScaleAttribute());
        List<AttributeModifier> pieces = List.of(
                modifier("one", 4.0D), modifier("two", 4.0D),
                modifier("three", 4.0D), modifier("four", 4.0D));

        assertEquals(4.0D, AttributeValueCalculator.calculate(instance, pieces), 0.000001D);
        pieces.forEach(piece -> assertEquals(
                1.0D, AttributeValueCalculator.standaloneAddValue(instance, piece), 0.000001D));
    }

    @Test
    void retainsTheInstanceBaseWhenSanitizingNegativeAdditions() {
        AttributeInstance instance = instance(new NonlinearResistanceAttribute(30.0D));
        instance.setBaseValue(10.0D);

        double expected = 5.0D / 35.0D - 10.0D / 40.0D;
        assertEquals(expected, standalone(instance, "negative", -5.0D), 0.000001D);
    }

    @Test
    void preservesRawAmountsForIdentityAttributes() {
        AttributeInstance instance = instance(new IdentityAttribute());
        instance.setBaseValue(2.0D);

        assertEquals(6.0D, standalone(instance, "ordinary", 6.0D), 0.000001D);
    }

    private static double standalone(AttributeInstance instance, String id, double amount) {
        return AttributeValueCalculator.standaloneAddValue(instance, modifier(id, amount));
    }

    private static AttributeModifier modifier(String path, double amount) {
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath("test", path), amount,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static AttributeInstance instance(Attribute attribute) {
        return new AttributeInstance(Holder.direct(attribute), ignored -> {});
    }

    private static final class IdentityAttribute extends Attribute {
        private IdentityAttribute() {
            super("attribute.test.identity", 0.0D);
        }
    }

    private static final class QuarterScaleAttribute extends Attribute {
        private QuarterScaleAttribute() {
            super("attribute.test.quarter_scale", 0.0D);
        }

        @Override
        public double sanitizeValue(double value) {
            return value * 0.25D;
        }
    }

    private static final class NonlinearResistanceAttribute extends Attribute {
        private final double scalingConstant;

        private NonlinearResistanceAttribute(double scalingConstant) {
            super("attribute.test.nonlinear_resistance", 0.0D);
            this.scalingConstant = scalingConstant;
        }

        @Override
        public double sanitizeValue(double value) {
            return value <= 0.0D ? 0.0D : value / (value + scalingConstant);
        }
    }
}
