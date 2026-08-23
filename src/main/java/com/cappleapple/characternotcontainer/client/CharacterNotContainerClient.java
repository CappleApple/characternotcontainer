package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CharacterNotContainer.MOD_ID, dist = Dist.CLIENT)
public final class CharacterNotContainerClient {
    public CharacterNotContainerClient(net.neoforged.bus.api.IEventBus modBus, ModContainer container) {
        modBus.addListener(ClientKeyMappings::register);
        NeoForge.EVENT_BUS.addListener(CharacterNotContainerClient::clientTick);
        NeoForge.EVENT_BUS.addListener(CharacterNotContainerClient::redirectCuriosInventoryButton);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        if (!CharacterConfigManager.general().enableSeparateKeybind) return;
        while (ClientKeyMappings.OPEN_CHARACTER.consumeClick()) openCharacterScreen();
    }

    private static void openCharacterScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.setScreen(new CharacterEquipmentScreen(minecraft.player));
    }

    private static void redirectCuriosInventoryButton(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!CharacterConfigManager.general().redirectCuriosInventoryButton || event.getButton() != 0
                || !supportsCuriosButtonRedirect(event.getScreen())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.containerMenu.getCarried().isEmpty()) return;
        boolean clickedCurios = event.getScreen().children().stream()
                .filter(listener -> listener instanceof AbstractWidget)
                .map(listener -> (AbstractWidget)listener)
                .anyMatch(widget -> widget.active && widget.visible
                        && widget.getClass().getName().equals("top.theillusivec4.curios.client.gui.CuriosButton")
                        && widget.isMouseOver(event.getMouseX(), event.getMouseY()));
        if (!clickedCurios) return;
        event.setCanceled(true);
        Screen inventoryScreen = event.getScreen();
        minecraft.execute(() -> {
            if (minecraft.screen == inventoryScreen) openCharacterScreen();
        });
    }

    private static boolean supportsCuriosButtonRedirect(Screen screen) {
        if (screen instanceof InventoryScreen) return true;
        return screen instanceof CreativeModeInventoryScreen creative && creative.isInventoryOpen();
    }
}
