package com.cappleapple.characternotcontainer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class ClientKeyMappings {
    public static final KeyMapping OPEN_CHARACTER = new KeyMapping(
            "key.characternotcontainer.open_character",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.characternotcontainer");

    private ClientKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CHARACTER);
    }
}
