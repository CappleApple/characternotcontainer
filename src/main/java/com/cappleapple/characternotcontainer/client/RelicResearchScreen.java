package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.compat.relics.RelicResearchMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Keeps the character view visible until the research slot arrives from the server. */
final class RelicResearchScreen extends Screen implements MenuAccess<RelicResearchMenu> {
    private final RelicResearchMenu menu;
    private final Screen parent;
    private int waitingTicks;

    RelicResearchScreen(RelicResearchMenu menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
        parent = Minecraft.getInstance().screen;
    }

    @Override public RelicResearchMenu getMenu() { return menu; }

    @Override public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null || !minecraft.player.isAlive()
                || minecraft.player.containerMenu != menu || !(parent instanceof CharacterEquipmentScreen)
                || ++waitingTicks > 100) {
            onClose();
        } else if (!menu.getSlot(0).getItem().isEmpty()) {
            // Relics returns straight to the character screen, never to this handoff.
            if (!RelicsResearchClient.open(menu.getSlot(0).getItem(), parent)) onClose();
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (parent != null) parent.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void removed() {
        // Forwarded character rendering can recreate its offscreen player buffer.
        if (parent instanceof CharacterEquipmentScreen) parent.removed();
        super.removed();
    }

    @Override public void onClose() {
        if (minecraft == null) return;
        if (minecraft.player != null && minecraft.player.containerMenu == menu) closeResearchMenu(minecraft.player);
        minecraft.setScreen(minecraft.player != null && minecraft.player.isAlive() ? parent : null);
    }

    static void closeMenuOnReturn(ScreenEvent.Opening event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getNewScreen() instanceof CharacterEquipmentScreen && minecraft.player != null
                && minecraft.player.containerMenu instanceof RelicResearchMenu) {
            // Close the server-backed slot in the same screen change, before the
            // character UI resumes accepting equipment changes.
            closeResearchMenu(minecraft.player);
        }
    }

    private static void closeResearchMenu(LocalPlayer player) {
        // LocalPlayer.closeContainer() also calls setScreen(null). Perform its
        // packet and menu-reset steps here while retaining the direct screen change.
        player.connection.send(new ServerboundContainerClosePacket(player.containerMenu.containerId));
        player.containerMenu = player.inventoryMenu;
    }

    @Override public boolean isPauseScreen() { return false; }
}