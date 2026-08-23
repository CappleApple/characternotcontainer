package com.cappleapple.characternotcontainer.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class AttributeDisplayNameTest {
    @Test
    void explicitJsonNameTakesPriority() {
        assertEquals("Custom Speed", AttributeDisplayName.resolve("Custom Speed", "attribute.name.generic.movement_speed",
                "minecraft:generic.movement_speed", key -> fail("Localization must not run for an explicit override")));
    }

    @Test
    void usesLocalizedAttributeDescriptionByDefault() {
        assertEquals("Localized Movement Speed", AttributeDisplayName.resolve("", "attribute.name.generic.movement_speed",
                "minecraft:generic.movement_speed", key -> "Localized Movement Speed"));
    }

    @Test
    void parsesNamespacedAndCamelCaseFallbacks() {
        assertEquals("Movement Speed", AttributeDisplayName.resolve("", "missing.translation",
                "minecraft:generic.movement_speed", key -> key));
        assertEquals("Movement Speed", AttributeDisplayName.fallback("movementSpeed"));
    }
}
