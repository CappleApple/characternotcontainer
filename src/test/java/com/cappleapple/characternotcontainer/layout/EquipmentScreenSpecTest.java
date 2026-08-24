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
    void bindsCommonCuriosSlotTypesToNamedAnchors() {
        EquipmentScreenSpec spec = EquipmentScreenSpec.defaults();
        assertEquals("neck", spec.anchorFor("necklace"));
        assertEquals("belt", spec.anchorFor("spellbook"));
        assertEquals("belt", spec.anchorFor("example:sheath"));
        assertEquals("hands", spec.anchorFor("example:ring"));
        assertEquals("other", spec.anchorFor("example:unknown_slot"));
        spec.validate();
    }

    @Test
    void acceptsCustomAnchorNames() {
        EquipmentScreenSpec spec = EquipmentScreenSpec.defaults();
        spec.slots.put("example:shoulder", "left_shoulder");
        spec.validate();
        assertEquals("left_shoulder", spec.anchorFor("example:shoulder"));
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
        assertEquals("neck", EquipmentLayoutLoader.decode(uiBuilder).anchorFor("necklace"));
        assertEquals("belt", EquipmentLayoutLoader.decode(movableAnchors).anchorFor("belt"));
    }

    @Test
    void bundledAndInCodeDefaultsUseThePublishedSlotMap() throws Exception {
        Map<String, String> expected = expectedSlots();
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

    private static Map<String, String> expectedSlots() {
        Map<String, String> expected = new LinkedHashMap<>();
        put(expected, "head", "head", "hat", "face", "mask");
        put(expected, "neck", "neck", "necklace", "amulet");
        put(expected, "back", "back", "cape", "wings");
        put(expected, "belt", "belt", "spellbook", "waist", "sheath", "body");
        put(expected, "hands", "hands", "hand", "ring", "bracelet");
        put(expected, "feet", "feet", "shoes", "anklet");
        put(expected, "other", "charm", "curio");
        return expected;
    }

    private static void put(Map<String, String> destination, String anchor, String... slotTypes) {
        for (String slotType : slotTypes) destination.put(slotType, anchor);
    }
}
