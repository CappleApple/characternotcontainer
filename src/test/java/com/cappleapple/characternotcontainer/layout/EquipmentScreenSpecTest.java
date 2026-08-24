package com.cappleapple.characternotcontainer.layout;

import org.junit.jupiter.api.Test;
import com.google.gson.JsonParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EquipmentScreenSpecTest {
    @Test
    void bindsCommonCuriosSlotTypesToFixedAnchors() {
        EquipmentScreenSpec spec = EquipmentScreenSpec.defaults();
        assertEquals(EquipmentScreenSpec.CurioAnchor.NECK, spec.anchorFor("necklace"));
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
}
