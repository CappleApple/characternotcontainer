package com.cappleapple.characternotcontainer.equipment;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;

import org.jetbrains.annotations.Nullable;

public final class PlayerInventoryAccess {
    private PlayerInventoryAccess() {}

    public static IItemHandler handler(Player player) {
        IItemHandler automation = player.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, null);
        IItemHandler entity = player.getCapability(Capabilities.ItemHandler.ENTITY);
        return preferredHandler(automation, entity, new PlayerInvWrapper(player.getInventory()));
    }

    static IItemHandler preferredHandler(@Nullable IItemHandler automation, @Nullable IItemHandler entity,
                                         IItemHandler fallback) {
        if (automation != null) return automation;
        return entity != null ? entity : fallback;
    }
}
