package com.cappleapple.characternotcontainer.compat.curios;

import com.cappleapple.characternotcontainer.equipment.EquipmentTargetAccess;
import com.cappleapple.characternotcontainer.network.EquipmentChangePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

public final class CuriosEquipmentMutator {
    private CuriosEquipmentMutator() {}

    public static Optional<EquipmentTargetAccess> resolve(ServerPlayer player, EquipmentChangePayload payload) {
        return CuriosApi.getCuriosInventory(player).flatMap(handler -> handler.getStacksHandler(payload.slotId()))
                .map(handler -> {
                    if (payload.slotIndex() < 0 || payload.slotIndex() >= handler.getSlots()) return null;
                    var stacks = payload.cosmetic() ? handler.getCosmeticStacks() : handler.getStacks();
                    return new EquipmentTargetAccess() {
                        @Override
                        public ItemStack equipped() {
                            return stacks.getStackInSlot(payload.slotIndex()).copy();
                        }

                        @Override
                        public boolean accepts(ItemStack stack) {
                            return !stack.isEmpty() && stacks.isItemValid(payload.slotIndex(), stack);
                        }

                        @Override
                        public boolean canRemove() {
                            ItemStack equipped = stacks.getStackInSlot(payload.slotIndex());
                            if (equipped.isEmpty()) return true;
                            ItemStack extractable = stacks.extractItem(payload.slotIndex(), equipped.getCount(), true);
                            return extractable.getCount() == equipped.getCount()
                                    && ItemStack.isSameItemSameComponents(extractable, equipped);
                        }

                        @Override
                        public void set(ItemStack stack) {
                            stacks.setStackInSlot(payload.slotIndex(), stack.copy());
                        }
                    };
                });
    }
}
