package com.cappleapple.characternotcontainer.equipment;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;

import org.jetbrains.annotations.Nullable;

public final class PlayerInventoryAccess {
    private PlayerInventoryAccess() {}

    public static IItemHandler handler(Player player) {
        IItemHandler automation = player.getCapability(ForgeCapabilities.ITEM_HANDLER, net.minecraft.core.Direction.UP).orElse(null);
        IItemHandler entity = player.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        return preferredHandler(automation, entity, new PlayerInvWrapper(player.getInventory()));
    }

    static IItemHandler preferredHandler(@Nullable IItemHandler automation, @Nullable IItemHandler entity,
                                         IItemHandler fallback) {
        if (automation != null) return automation;
        return entity != null ? entity : fallback;
    }
}
