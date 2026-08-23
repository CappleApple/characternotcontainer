package com.cappleapple.characternotcontainer.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EquipmentPickerLayoutTest {
    private static final ScreenRect ANCHOR = new ScreenRect(100, 60, 18, 18);

    @Test
    void twoOptionsUseExactlyTwoColumnsAndOneRow() {
        EquipmentPickerLayout layout = EquipmentPickerLayout.calculate(ANCHOR, 400, 300, 2, 5, 3, false);

        assertEquals(new ScreenRect(122, 60, 50, 30), layout.bounds());
        assertEquals(2, layout.columns());
        assertEquals(1, layout.visibleRows());
        assertEquals(1, layout.totalRows());
    }

    @Test
    void defaultLimitsWrapAfterFiveColumnsAndScrollAfterThreeRows() {
        EquipmentPickerLayout layout = EquipmentPickerLayout.calculate(ANCHOR, 400, 300, 20, 5, 3, false);

        assertEquals(new ScreenRect(122, 60, 115, 70), layout.bounds());
        assertEquals(5, layout.columns());
        assertEquals(3, layout.visibleRows());
        assertEquals(4, layout.totalRows());
    }

    @Test
    void leftConfigurationAnchorsBesideTheSlot() {
        EquipmentPickerLayout layout = EquipmentPickerLayout.calculate(ANCHOR, 400, 300, 2, 5, 3, true);

        assertEquals(46, layout.bounds().x());
        assertEquals(ANCHOR.y(), layout.bounds().y());
    }

    @Test
    void panelStaysInsideTheScreenAtEdges() {
        EquipmentPickerLayout layout = EquipmentPickerLayout.calculate(
                new ScreenRect(385, 290, 18, 18), 400, 300, 20, 5, 3, false);

        assertEquals(283, layout.bounds().x());
        assertEquals(228, layout.bounds().y());
    }
}
