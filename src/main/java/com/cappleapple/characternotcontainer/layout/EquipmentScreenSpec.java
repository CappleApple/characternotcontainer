package com.cappleapple.characternotcontainer.layout;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Maps Curios slot-type IDs to the screen's fixed, built-in body anchors. */
public final class EquipmentScreenSpec {
    public Map<String, CurioAnchor> slots = defaultSlots();

    public static EquipmentScreenSpec defaults() {
        return new EquipmentScreenSpec();
    }

    public CurioAnchor anchorFor(String slotType) {
        if (slotType == null || slotType.isBlank()) return CurioAnchor.OTHER;
        String normalized = slotType.toLowerCase(Locale.ROOT);
        CurioAnchor direct = slots.get(normalized);
        if (direct != null) return direct;
        int separator = normalized.indexOf(':');
        return separator >= 0
                ? slots.getOrDefault(normalized.substring(separator + 1), CurioAnchor.OTHER)
                : CurioAnchor.OTHER;
    }

    public void validate() {
        if (slots == null || slots.size() > 256) {
            throw new IllegalArgumentException("Curios slot bindings must contain at most 256 entries");
        }
        Map<String, CurioAnchor> normalized = new LinkedHashMap<>();
        slots.forEach((slotType, anchor) -> {
            if (slotType == null || slotType.isBlank() || slotType.length() > 128 || anchor == null) {
                throw new IllegalArgumentException("Every Curios slot binding needs a slot ID and anchor");
            }
            normalized.put(slotType.toLowerCase(Locale.ROOT), anchor);
        });
        slots = normalized;
    }

    private static Map<String, CurioAnchor> defaultSlots() {
        Map<String, CurioAnchor> result = new LinkedHashMap<>();
        bind(result, CurioAnchor.HEAD, "head", "hat", "face", "mask");
        bind(result, CurioAnchor.NECK, "neck", "necklace", "amulet");
        bind(result, CurioAnchor.BACK, "back", "cape", "wings");
        bind(result, CurioAnchor.BELT, "belt", "spellbook", "waist", "sheath", "body");
        bind(result, CurioAnchor.HANDS, "hands", "hand", "ring", "bracelet");
        bind(result, CurioAnchor.FEET, "feet", "shoes", "anklet");
        bind(result, CurioAnchor.OTHER, "charm", "curio");
        return result;
    }

    private static void bind(Map<String, CurioAnchor> destination, CurioAnchor anchor, String... slotTypes) {
        for (String slotType : slotTypes) destination.put(slotType, anchor);
    }

    public enum CurioAnchor {
        @SerializedName("head") HEAD,
        @SerializedName("neck") NECK,
        @SerializedName("back") BACK,
        @SerializedName("belt") BELT,
        @SerializedName("hands") HANDS,
        @SerializedName("left_hand") LEFT_HAND,
        @SerializedName("right_hand") RIGHT_HAND,
        @SerializedName("feet") FEET,
        @SerializedName("other") OTHER
    }
}
