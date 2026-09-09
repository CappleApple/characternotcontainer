package com.cappleapple.characternotcontainer.client;

import java.util.Objects;

/** Requires a continuous one-second hold on the same equipment source. */
final class ResearchHold<T> {
    static final int REQUIRED_TICKS = 20;
    private T target;
    private int previousTicks;
    private int ticks;
    private boolean sent;

    boolean tick(T hovered, boolean held) {
        previousTicks = ticks;
        if (hovered == null || !held || !Objects.equals(target, hovered)) {
            target = hovered;
            previousTicks = 0;
            ticks = 0;
            sent = false;
        }
        if (hovered == null || !held || sent) return false;
        if (++ticks < REQUIRED_TICKS) return false;
        sent = true;
        return true;
    }

    Progress progress(T hovered) {
        return hovered != null && Objects.equals(target, hovered)
                ? new Progress(previousTicks, ticks) : Progress.NONE;
    }

    record Progress(int previousTicks, int ticks) {
        static final Progress NONE = new Progress(0, 0);
    }
}
