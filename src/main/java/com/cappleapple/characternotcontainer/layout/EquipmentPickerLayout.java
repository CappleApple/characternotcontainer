package com.cappleapple.characternotcontainer.layout;

public record EquipmentPickerLayout(ScreenRect bounds, int columns, int visibleRows, int totalRows) {
    private static final int CELL_SIZE = 20;
    private static final int HORIZONTAL_PADDING = 10;
    private static final int VERTICAL_CHROME = 10;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int ANCHOR_GAP = 4;
    private static final int SCREEN_MARGIN = 2;

    public static EquipmentPickerLayout calculate(ScreenRect anchor, int screenWidth, int screenHeight, int entries,
                                                   int configuredColumns, int configuredVisibleRows, boolean opensLeft) {
        int columns = Math.max(1, Math.min(Math.max(1, configuredColumns), Math.max(1, entries)));
        int totalRows = Math.max(1, (Math.max(1, entries) + columns - 1) / columns);
        int visibleRows = Math.max(1, Math.min(Math.max(1, configuredVisibleRows), totalRows));
        int panelWidth = columns * CELL_SIZE + HORIZONTAL_PADDING
                + (totalRows > visibleRows ? SCROLLBAR_WIDTH : 0);
        int panelHeight = visibleRows * CELL_SIZE + VERTICAL_CHROME;
        int desiredX = opensLeft ? anchor.x() - panelWidth - ANCHOR_GAP
                : anchor.x() + anchor.width() + ANCHOR_GAP;
        int x = clamp(desiredX, SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenWidth - panelWidth - SCREEN_MARGIN));
        int y = clamp(anchor.y(), SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenHeight - panelHeight - SCREEN_MARGIN));
        return new EquipmentPickerLayout(new ScreenRect(x, y, panelWidth, panelHeight),
                columns, visibleRows, totalRows);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
