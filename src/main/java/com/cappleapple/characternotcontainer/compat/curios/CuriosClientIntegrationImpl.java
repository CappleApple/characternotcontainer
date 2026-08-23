package com.cappleapple.characternotcontainer.compat.curios;

import com.cappleapple.characternotcontainer.client.CuriosClientIntegration;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.ArrayList;
import java.util.List;

public final class CuriosClientIntegrationImpl implements CuriosClientIntegration {
    public CuriosClientIntegrationImpl() {}

    @Override
    public List<CurioSlotView> slots(Player player, boolean cosmetic) {
        List<CurioSlotView> result = new ArrayList<>();
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> inventory.getCurios().forEach((type, handler) -> {
            if (!handler.isVisible() || cosmetic && !handler.hasCosmetic()) return;
            var stacks = cosmetic ? handler.getCosmeticStacks() : handler.getStacks();
            for (int index = 0; index < handler.getSlots(); index++) {
                if (inventory.isSlotActive(type, index)) {
                    result.add(new CurioSlotView(type, index, CuriosApi.getSlotIcon(type), stacks.getStackInSlot(index).copy(), cosmetic));
                }
            }
        }));
        return List.copyOf(result);
    }

    @Override
    public boolean isValid(Player player, CurioSlotView slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return CuriosApi.getCuriosInventory(player).flatMap(inventory -> inventory.getStacksHandler(slot.type()))
                .map(handler -> {
                    if (slot.index() < 0 || slot.index() >= handler.getSlots()) return false;
                    var stacks = slot.cosmetic() ? handler.getCosmeticStacks() : handler.getStacks();
                    return stacks.isItemValid(slot.index(), stack);
                }).orElse(false);
    }

    @Override
    public List<EquipmentContribution> contributions(Player player, Holder<Attribute> attribute) {
        List<EquipmentContribution> result = new ArrayList<>();
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> inventory.getCurios().forEach((type, handler) -> {
            for (int index = 0; index < handler.getSlots(); index++) {
                if (!inventory.isSlotActive(type, index)) continue;
                ItemStack stack = handler.getStacks().getStackInSlot(index);
                if (stack.isEmpty()) continue;
                SlotContext context = new SlotContext(type, player, index, false, true);
                CuriosApi.getAttributeModifiers(context, CuriosApi.getSlotId(context), stack).get(attribute)
                        .forEach(modifier -> result.add(new EquipmentContribution(stack.copy(), modifier)));
            }
        }));
        return List.copyOf(result);
    }
}
