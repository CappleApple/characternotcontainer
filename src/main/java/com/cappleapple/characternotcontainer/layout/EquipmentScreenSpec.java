package com.cappleapple.characternotcontainer.layout;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Maps Curios slot-type IDs to named anchors from slots.json. */
public final class EquipmentScreenSpec {
    public Map<String, String> slots = defaultSlots();

    public static EquipmentScreenSpec defaults() {
        return new EquipmentScreenSpec();
    }

    public String anchorFor(String slotType) {
        if (slotType == null || slotType.isBlank()) return "other";
        String normalized = slotType.toLowerCase(Locale.ROOT);
        String direct = slots.get(normalized);
        if (direct != null) return direct;
        int separator = normalized.indexOf(':');
        return separator >= 0
                ? slots.getOrDefault(normalized.substring(separator + 1), "other")
                : "other";
    }

    public void validate() {
        if (slots == null || slots.size() > 256) {
            throw new IllegalArgumentException("Curios slot bindings must contain at most 256 entries");
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        slots.forEach((slotType, anchor) -> {
            if (slotType == null || slotType.isBlank() || slotType.length() > 128
                    || anchor == null || anchor.isBlank() || anchor.length() > 128) {
                throw new IllegalArgumentException("Every Curios slot binding needs a slot ID and anchor");
            }
            normalized.put(slotType.toLowerCase(Locale.ROOT), anchor.toLowerCase(Locale.ROOT));
        });
        slots = normalized;
    }

    private static Map<String, String> defaultSlots() {
        Map<String, String> result = new LinkedHashMap<>();
        bind(result, "head", "head", "hat", "face", "mask");
        bind(result, "neck", "neck", "necklace", "amulet");
        bind(result, "back", "back", "cape", "wings");
        bind(result, "belt", "belt", "spellbook", "waist", "sheath", "body");
        bind(result, "hands", "hands", "hand", "ring", "bracelet");
        bind(result, "feet", "feet", "shoes", "anklet");
        bind(result, "other", "charm", "curio");
        return result;
    }

    private static void bind(Map<String, String> destination, String anchor, String... slotTypes) {
        for (String slotType : slotTypes) destination.put(slotType, anchor);
    }
}
