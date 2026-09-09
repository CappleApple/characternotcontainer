package com.cappleapple.characternotcontainer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A tooltip-only container context for mods whose standard tooltip hooks require one.
 * Never opened or ticked: research, input and the server menu remain owned by the character screen.
 */
final class RelicsTooltipContext extends AbstractContainerScreen<InventoryMenu> {
    private ItemStack tooltipStack = ItemStack.EMPTY;
    private final SimpleContainer tooltipItem = new SimpleContainer(1) {
        @Override
        public ItemStack getItem(int index) { return tooltipStack; }
    };

    RelicsTooltipContext(Player player, Screen parent) {
        super(player.inventoryMenu, player.getInventory(), parent.getTitle());
        minecraft = Minecraft.getInstance();
        font = minecraft.font;
        width = parent.width;
        height = parent.height;
        imageWidth = width;
        imageHeight = height;
        hoveredSlot = new Slot(tooltipItem, 0, 0, 0);
    }

    void renderItemTooltip(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY,
                           ResearchHold.Progress progress) {
        Screen previousScreen = minecraft.screen;
        ItemStack previousStack = tooltipStack;
        tooltipStack = stack;
        try {
            // Do not call setScreen: no screen events, menu changes, init/removal or inventory frame.
            // Relics' ItemMixin checks Minecraft.screen while the default handler builds its lines.
            minecraft.screen = this;
            RelicsResearchClient.withTooltipProgress(progress, () -> renderTooltip(graphics, mouseX, mouseY));
        } finally {
            if (minecraft.screen == this) minecraft.screen = previousScreen;
            tooltipStack = previousStack;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}
}
