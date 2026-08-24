package com.cappleapple.characternotcontainer.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterGuiSpritesTest {
    @Test
    void exposesStableCharacterScreenResourceLocations() {
        assertEquals("characternotcontainer:character_screen/screen/background",
                CharacterGuiSprites.SCREEN_BACKGROUND.toString());
        assertEquals("characternotcontainer:textures/gui/sprites/character_screen/screen/background.png",
                CharacterGuiSprites.textureFile(CharacterGuiSprites.SCREEN_BACKGROUND).toString());
    }

    @Test
    void everyPublicSpriteMapsToAUniqueGuiSpritePng() throws IllegalAccessException {
        List<ResourceLocation> sprites = Arrays.stream(CharacterGuiSprites.class.getFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == ResourceLocation.class)
                .map(this::value)
                .toList();

        assertEquals(sprites.size(), sprites.stream().distinct().count());
        for (ResourceLocation sprite : sprites) {
            assertEquals("characternotcontainer", sprite.getNamespace());
            ResourceLocation texture = CharacterGuiSprites.textureFile(sprite);
            assertEquals(sprite.getNamespace(), texture.getNamespace());
            assertTrue(texture.getPath().startsWith("textures/gui/sprites/character_screen/"));
            assertTrue(texture.getPath().endsWith(".png"));
        }
    }

    private ResourceLocation value(Field field) {
        try {
            return (ResourceLocation)field.get(null);
        } catch (IllegalAccessException exception) {
            throw new AssertionError(exception);
        }
    }
}
