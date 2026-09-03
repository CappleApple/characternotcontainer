package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.layout.ScreenRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterEquipmentScreenCurioVisibilityTest {
    @Test
    void renderToggleMatchesTheCuriosStyleTopRightSlotOverlay() {
        assertEquals(new ScreenRect(112, 49, 8, 8),
                CharacterEquipmentScreen.curioVisibilityButtonBounds(new ScreenRect(100, 50, 18, 18)));
    }
}
