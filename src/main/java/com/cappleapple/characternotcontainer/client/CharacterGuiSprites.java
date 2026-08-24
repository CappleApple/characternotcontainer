package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.resources.ResourceLocation;

/**
 * Fixed GUI sprite IDs that resource packs may provide to skin the character
 * screen. CharacterEquipmentScreen retains its procedural drawing whenever a
 * corresponding sprite resource is absent.
 */
public final class CharacterGuiSprites {
    public static final ResourceLocation SCREEN_BACKGROUND = sprite("screen/background");
    public static final ResourceLocation PLAYER_PREVIEW_BACKGROUND = sprite("screen/player_preview_background");
    public static final ResourceLocation PLAYER_PREVIEW_FRAME = sprite("screen/player_preview_frame");

    public static final ResourceLocation STATS_PANEL = sprite("stats/panel");
    public static final ResourceLocation STATS_ROW_HOVERED = sprite("stats/row_hovered");
    public static final ResourceLocation STATS_SCROLLBAR_TRACK = sprite("stats/scrollbar_track");
    public static final ResourceLocation STATS_SCROLLBAR_THUMB = sprite("stats/scrollbar_thumb");
    public static final ResourceLocation STATS_FALLBACK_ICON = sprite("stats/fallback_icon");

    public static final ResourceLocation EQUIPMENT_SLOT = sprite("equipment/slot");
    public static final ResourceLocation EQUIPMENT_SLOT_HOVERED = sprite("equipment/slot_hovered");
    public static final ResourceLocation EQUIPMENT_SLOT_SELECTED = sprite("equipment/slot_selected");
    public static final ResourceLocation CURIO_SLOT = sprite("equipment/curio_slot");
    public static final ResourceLocation CURIO_SLOT_HOVERED = sprite("equipment/curio_slot_hovered");
    public static final ResourceLocation CURIO_SLOT_SELECTED = sprite("equipment/curio_slot_selected");
    public static final ResourceLocation COSMETIC_CURIO_SLOT = sprite("equipment/cosmetic_curio_slot");
    public static final ResourceLocation COSMETIC_CURIO_SLOT_HOVERED = sprite("equipment/cosmetic_curio_slot_hovered");
    public static final ResourceLocation COSMETIC_CURIO_SLOT_SELECTED = sprite("equipment/cosmetic_curio_slot_selected");
    public static final ResourceLocation SHOW_COSMETICS_BUTTON = sprite("equipment/show_cosmetics_button");
    public static final ResourceLocation SHOW_COSMETICS_BUTTON_HOVERED = sprite("equipment/show_cosmetics_button_hovered");
    public static final ResourceLocation SHOW_EQUIPMENT_BUTTON = sprite("equipment/show_equipment_button");
    public static final ResourceLocation SHOW_EQUIPMENT_BUTTON_HOVERED = sprite("equipment/show_equipment_button_hovered");

    public static final ResourceLocation PICKER_BACKGROUND = sprite("picker/background");
    public static final ResourceLocation PICKER_CELL = sprite("picker/cell");
    public static final ResourceLocation PICKER_CELL_HOVERED = sprite("picker/cell_hovered");
    public static final ResourceLocation PICKER_UNEQUIP = sprite("picker/unequip");
    public static final ResourceLocation PICKER_SCROLLBAR_TRACK = sprite("picker/scrollbar_track");
    public static final ResourceLocation PICKER_SCROLLBAR_THUMB = sprite("picker/scrollbar_thumb");

    public static final ResourceLocation SOURCE_TOOLTIP_BACKGROUND = sprite("tooltip/source_background");

    private CharacterGuiSprites() {}

    private static ResourceLocation sprite(String path) {
        return CharacterNotContainer.id("character_screen/" + path);
    }

    static ResourceLocation textureFile(ResourceLocation sprite) {
        return ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(),
                "textures/gui/sprites/" + sprite.getPath() + ".png");
    }
}
