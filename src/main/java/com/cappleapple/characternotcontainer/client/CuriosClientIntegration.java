package com.cappleapple.characternotcontainer.client;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface CuriosClientIntegration {
    List<CurioSlotView> slots(Player player, boolean cosmetic);
    boolean isValid(Player player, CurioSlotView slot, ItemStack stack);
    List<EquipmentContribution> contributions(Player player, Holder<Attribute> attribute);

    static CuriosClientIntegration load() {
        try {
            return (CuriosClientIntegration) Class.forName(
                    "com.cappleapple.characternotcontainer.compat.curios.CuriosClientIntegrationImpl")
                    .getConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Curios is present but its character UI integration could not load", exception);
        }
    }

    record CurioSlotView(String type, int index, ResourceLocation icon, ItemStack stack, boolean cosmetic) {}
    record EquipmentContribution(ItemStack stack, AttributeModifier modifier) {}
}
