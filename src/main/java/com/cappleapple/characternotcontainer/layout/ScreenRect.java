package com.cappleapple.characternotcontainer.layout;

public record ScreenRect(int x, int y, int width, int height) {
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
