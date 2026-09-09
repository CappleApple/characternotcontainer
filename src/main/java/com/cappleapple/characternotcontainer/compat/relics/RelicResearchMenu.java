package com.cappleapple.characternotcontainer.compat.relics;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RelicResearchMenu extends AbstractContainerMenu {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CharacterNotContainer.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<RelicResearchMenu>> TYPE = MENUS.register("relic_research",
            () -> new MenuType<>(RelicResearchMenu::new, FeatureFlags.DEFAULT_FLAGS));
    private final RelicResearchSource source;
    private final ItemStack original;
    private ItemStack lastSaved;

    public RelicResearchMenu(int id, Inventory inventory) {
        this(id, (RelicResearchSource)null);
    }

    public RelicResearchMenu(int id, RelicResearchSource source) {
        super(TYPE.get(), id);
        this.source = source;
        original = source == null ? ItemStack.EMPTY : source.stack().get();
        lastSaved = original.copy();
        addSlot(new Slot(new SimpleContainer(1), 0, 0, 0) {
            @Override public ItemStack getItem() {
                return source == null ? super.getItem() : sourceValid() ? original : ItemStack.EMPTY;
            }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });
    }

    private boolean sourceValid() {
        return source.valid().getAsBoolean() && !original.isEmpty() && source.stack().get() == original;
    }

    @Override public boolean stillValid(Player player) {
        return player.isAlive() && (source == null || sourceValid());
    }

    @Override public void broadcastChanges() {
        // Relics edits the live stack, then broadcasts the menu. Notify its owner
        // as well so equipment modifiers and persistent storage update.
        if (source != null && sourceValid() && !ItemStack.matches(lastSaved, original)) {
            source.changed().run();
            lastSaved = original.copy();
        }
        super.broadcastChanges();
    }

    @Override public void removed(Player player) {
        broadcastChanges();
        super.removed(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {}
}
