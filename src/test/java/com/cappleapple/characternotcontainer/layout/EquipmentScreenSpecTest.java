package com.cappleapple.characternotcontainer.layout;

import org.junit.jupiter.api.Test;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EquipmentScreenSpecTest {
    @Test
    void bindsCommonCuriosSlotTypesToFixedAnchors() {
        EquipmentScreenSpec spec = EquipmentScreenSpec.defaults();
        assertEquals(EquipmentScreenSpec.CurioAnchor.NECK, spec.anchorFor("necklace"));
        assertEquals(EquipmentScreenSpec.CurioAnchor.BELT, spec.anchorFor("spellbook"));
        assertEquals(EquipmentScreenSpec.CurioAnchor.BELT, spec.anchorFor("example:sheath"));
        assertEquals(EquipmentScreenSpec.CurioAnchor.HANDS, spec.anchorFor("example:ring"));
        assertEquals(EquipmentScreenSpec.CurioAnchor.OTHER, spec.anchorFor("example:unknown_slot"));
        spec.validate();
    }

    @Test
    void rejectsMissingBindingTargets() {
        EquipmentScreenSpec spec = EquipmentScreenSpec.defaults();
        spec.slots.put("broken", null);
        assertThrows(IllegalArgumentException.class, spec::validate);
    }

    @Test
    void migratesDeprecatedAnchorAndUiBuilderDocumentsToBindingDefaults() {
        var uiBuilder = JsonParser.parseString("{\"width\":400,\"widgets\":[]}").getAsJsonObject();
        var movableAnchors = JsonParser.parseString("{\"head\":{\"anchor\":\"head\",\"offset\":{\"x\":1,\"y\":2}}}").getAsJsonObject();
        assertEquals(EquipmentScreenSpec.CurioAnchor.NECK,
                EquipmentLayoutLoader.decode(uiBuilder).anchorFor("necklace"));
        assertEquals(EquipmentScreenSpec.CurioAnchor.BELT,
                EquipmentLayoutLoader.decode(movableAnchors).anchorFor("belt"));
    }

    @Test
    void bundledAndInCodeDefaultsUseThePublishedSlotMap() throws Exception {
        Map<String, EquipmentScreenSpec.CurioAnchor> expected = expectedSlots();
        assertEquals(expected, EquipmentScreenSpec.defaults().slots);

        try (var input = EquipmentScreenSpecTest.class.getClassLoader()
                .getResourceAsStream(EquipmentLayoutLoader.RESOURCE_PATH)) {
            if (input == null) throw new AssertionError("Missing bundled equipment-screen defaults");
            try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                EquipmentScreenSpec bundled = EquipmentLayoutLoader.decode(JsonParser.parseReader(reader).getAsJsonObject());
                assertEquals(expected, bundled.slots);
            }
        }
    }

    private static Map<String, EquipmentScreenSpec.CurioAnchor> expectedSlots() {
        Map<String, EquipmentScreenSpec.CurioAnchor> expected = new LinkedHashMap<>();
        put(expected, EquipmentScreenSpec.CurioAnchor.HEAD, "head", "hat", "face", "mask");
        put(expected, EquipmentScreenSpec.CurioAnchor.NECK, "neck", "necklace", "amulet");
        put(expected, EquipmentScreenSpec.CurioAnchor.BACK, "back", "cape", "wings");
        put(expected, EquipmentScreenSpec.CurioAnchor.BELT, "belt", "spellbook", "waist", "sheath", "body");
        put(expected, EquipmentScreenSpec.CurioAnchor.HANDS, "hands", "hand", "ring", "bracelet");
        put(expected, EquipmentScreenSpec.CurioAnchor.FEET, "feet", "shoes", "anklet");
        put(expected, EquipmentScreenSpec.CurioAnchor.OTHER, "charm", "curio");
        return expected;
    }

    private static void put(Map<String, EquipmentScreenSpec.CurioAnchor> destination,
                            EquipmentScreenSpec.CurioAnchor anchor, String... slotTypes) {
        for (String slotType : slotTypes) destination.put(slotType, anchor);
    }
}
