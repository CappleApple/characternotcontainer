package com.cappleapple.characternotcontainer.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

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
}
