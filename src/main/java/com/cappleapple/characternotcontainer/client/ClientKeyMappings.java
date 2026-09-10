package com.cappleapple.characternotcontainer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "characternotcontainer", value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class ClientKeyMappings {
    public static final KeyMapping OPEN_CHARACTER = new KeyMapping(
            "key.characternotcontainer.open_character",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.characternotcontainer");

    private ClientKeyMappings() {}

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CHARACTER);
    }
}
