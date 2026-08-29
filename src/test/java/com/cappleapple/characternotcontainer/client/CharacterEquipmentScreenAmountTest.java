package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.config.StatDefinition;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CharacterEquipmentScreenAmountTest {
    @Test
    void baseMultiplierDisplaysOnlyItsPercentage() {
        Component amount = CharacterEquipmentScreen.percentageModifierAmount(
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 1.5D);

        assertEquals("+150%", amount.getString());
    }

    @Test
    void totalMultiplierIsExplicitlyLabeledAsStacking() {
        Component amount = CharacterEquipmentScreen.percentageModifierAmount(
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 1.5D);
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, amount.getContents());

        assertEquals("gui.characternotcontainer.stacking", contents.getKey());
        assertArrayEquals(new Object[]{"+150%"}, contents.getArgs());
    }

    @Test
    void resistancePercentageHasNoSourceModifierSign() {
        assertEquals("87.5%", CharacterEquipmentScreen.resistancePercentage(0.875D));
        assertEquals("-12.5%", CharacterEquipmentScreen.resistancePercentage(-0.125D));
    }

    @Test
    void groupedStackingModifiersDisplayTheirCombinedEffect() {
        Component amount = CharacterEquipmentScreen.groupedModifierAmount(List.of(
                        modifier("one", 0.1D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        modifier("two", 0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)),
                new StatDefinition("test:attribute"), instance());
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, amount.getContents());

        assertEquals("gui.characternotcontainer.stacking", contents.getKey());
        assertArrayEquals(new Object[]{"+32%"}, contents.getArgs());
    }

    private static AttributeModifier modifier(String path, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath("test", path), amount, operation);
    }

    private static AttributeInstance instance() {
        return new AttributeInstance(Holder.direct(new TestAttribute()), ignored -> {});
    }

    private static final class TestAttribute extends Attribute {
        private TestAttribute() {
            super("attribute.test.grouped", 0.0D);
        }
    }
}
