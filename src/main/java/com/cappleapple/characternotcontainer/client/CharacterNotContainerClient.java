package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CharacterNotContainer.MOD_ID, dist = Dist.CLIENT)
public final class CharacterNotContainerClient {
    private static boolean discoverAttributes;
    private static int discoveryDelay;

    public CharacterNotContainerClient(net.neoforged.bus.api.IEventBus modBus, ModContainer container) {
        modBus.addListener(ClientKeyMappings::register);
        modBus.addListener(CharacterNotContainerClient::registerReloadListener);
        NeoForge.EVENT_BUS.addListener(CharacterNotContainerClient::clientTick);
        NeoForge.EVENT_BUS.addListener(CharacterNotContainerClient::redirectCuriosInventoryButton);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CharacterNotContainerClient::openCharacterFromInventory);
        NeoForge.EVENT_BUS.addListener(CharacterNotContainerClient::playerLoggedIn);
        NeoForge.EVENT_BUS.addListener(CharacterNotContainerClient::playerLoggedOut);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        if (discoverAttributes && discoveryDelay-- <= 0) discoverPlayerAttributes();
        if (CharacterConfigManager.general().enableSeparateKeybind) {
            while (ClientKeyMappings.OPEN_CHARACTER.consumeClick()) openCharacterScreen();
        }
    }

    private static void playerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        CharacterConfigManager.load();
        scheduleAttributeDiscovery();
    }

    private static void playerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        discoverAttributes = false;
        discoveryDelay = 0;
    }

    private static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener)resourceManager -> {
            CharacterConfigManager.load();
            scheduleAttributeDiscovery();
        });
    }

    private static void scheduleAttributeDiscovery() {
        discoverAttributes = true;
        discoveryDelay = 20;
    }

    private static void discoverPlayerAttributes() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        discoverAttributes = false;
        var ids = BuiltInRegistries.ATTRIBUTE.holders()
                .filter(holder -> holder.value().isClientSyncable())
                .filter(holder -> minecraft.player.getAttributes().hasAttribute(holder))
                .map(holder -> BuiltInRegistries.ATTRIBUTE.getKey(holder.value()))
                .filter(java.util.Objects::nonNull)
                .map(Object::toString)
                .toList();
        if (CharacterConfigManager.mergeDiscoveredAttributes(ids)) ResolvedStatCatalog.invalidate();
    }

    private static void openCharacterScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.setScreen(new CharacterEquipmentScreen(minecraft.player));
    }

    static void openInventoryScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        while (ClientKeyMappings.OPEN_CHARACTER.consumeClick()) {}
        if (minecraft.gameMode != null && minecraft.gameMode.isServerControlledInventory()) {
            minecraft.setScreen(null);
            minecraft.player.sendOpenInventory();
        } else {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
        }
    }

    private static void openCharacterFromInventory(ScreenEvent.KeyPressed.Post event) {
        if (!CharacterConfigManager.general().enableSeparateKeybind
                || !ClientKeyMappings.OPEN_CHARACTER.matches(event.getKeyCode(), event.getScanCode())
                || !isPlayerInventoryScreen(event.getScreen())
                || hasFocusedTextInput(event.getScreen())) return;
        event.setCanceled(true);
        Screen inventoryScreen = event.getScreen();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.screen == inventoryScreen) openCharacterScreen();
        });
    }

    private static boolean isPlayerInventoryScreen(Screen screen) {
        if (screen instanceof InventoryScreen) return true;
        if (screen instanceof CreativeModeInventoryScreen creative) return creative.isInventoryOpen();
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && screen instanceof AbstractContainerScreen<?> container
                && container.getMenu() == minecraft.player.inventoryMenu;
    }

    private static boolean hasFocusedTextInput(Screen screen) {
        return screen.getFocused() instanceof EditBox editBox && editBox.canConsumeInput();
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
