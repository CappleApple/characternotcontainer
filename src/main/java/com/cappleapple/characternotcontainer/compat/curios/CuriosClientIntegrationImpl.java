package com.cappleapple.characternotcontainer.compat.curios;

import com.cappleapple.characternotcontainer.client.CuriosClientIntegration;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.common.network.client.CPacketToggleRender;

import java.util.ArrayList;
import java.util.List;

public final class CuriosClientIntegrationImpl implements CuriosClientIntegration {
    public CuriosClientIntegrationImpl() {}

    @Override
    public List<CurioSlotView> slots(Player player, boolean cosmetic) {
        List<CurioSlotView> result = new ArrayList<>();
        CuriosApi.getCuriosInventory(player).resolve().ifPresent(inventory -> inventory.getCurios().forEach((type, handler) -> {
            if (!handler.isVisible()) return;
            boolean useCosmeticStorage = CuriosClientIntegration.useCosmeticStorage(cosmetic, handler.hasCosmetic());
            var stacks = useCosmeticStorage ? handler.getCosmeticStacks() : handler.getStacks();
            var renderStates = handler.getRenders();
            boolean canToggleRendering = handler.canToggleRendering();
            for (int index = 0; index < handler.getSlots(); index++) {
                {
                    boolean rendering = !canToggleRendering
                            || renderStates.size() > index && renderStates.get(index);
                    result.add(new CurioSlotView(type, index, CuriosApi.getSlotIcon(type),
                            stacks.getStackInSlot(index).copy(), useCosmeticStorage, rendering, canToggleRendering));
                }
            }
        }));
        return List.copyOf(result);
    }

    @Override
    public boolean isValid(Player player, CurioSlotView slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return CuriosApi.getCuriosInventory(player).resolve().flatMap(inventory -> inventory.getStacksHandler(slot.type()))
                .map(handler -> {
                    if (slot.index() < 0 || slot.index() >= handler.getSlots()) return false;
                    var stacks = slot.cosmetic() ? handler.getCosmeticStacks() : handler.getStacks();
                    return stacks.isItemValid(slot.index(), stack);
                }).orElse(false);
    }

    @Override
    public void toggleRendering(Player player, CurioSlotView slot) {
        CuriosApi.getCuriosInventory(player).resolve().flatMap(inventory -> inventory.getStacksHandler(slot.type()))
                .ifPresent(handler -> {
                    if (!handler.canToggleRendering() || slot.index() < 0 || slot.index() >= handler.getSlots()
                            || slot.index() >= handler.getRenders().size()) return;
                    top.theillusivec4.curios.common.network.NetworkHandler.INSTANCE.sendToServer(new CPacketToggleRender(slot.type(), slot.index()));
                });
    }

    @Override
    public List<EquipmentContribution> contributions(Player player, Attribute attribute) {
        List<EquipmentContribution> result = new ArrayList<>();
        CuriosApi.getCuriosInventory(player).resolve().ifPresent(inventory -> inventory.getCurios().forEach((type, handler) -> {
            for (int index = 0; index < handler.getSlots(); index++) {
                ItemStack stack = handler.getStacks().getStackInSlot(index);
                if (stack.isEmpty()) continue;
                SlotContext context = new SlotContext(type, player, index, false, true);
                CuriosApi.getAttributeModifiers(context, CuriosApi.getSlotUuid(context), stack).get(attribute)
                        .forEach(modifier -> result.add(new EquipmentContribution(stack.copy(), modifier)));
            }
        }));
        return List.copyOf(result);
    }
}
