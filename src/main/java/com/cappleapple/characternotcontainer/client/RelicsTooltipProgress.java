package com.cappleapple.characternotcontainer.client;

import java.lang.reflect.Field;

/** Supplies the character hold timer to Relics' own tooltip renderer, only while it runs. */
final class RelicsTooltipProgress {
    private final Field previousTicks;
    private final Field ticks;

    RelicsTooltipProgress(Class<?> handler) throws ReflectiveOperationException {
        Field previous;
        try { previous = counter(handler, "ticksCountOld"); }
        catch (NoSuchFieldException ignored) { previous = null; }
        previousTicks = previous;
        ticks = counter(handler, "ticksCount");
    }

    private static Field counter(Class<?> handler, String name) throws ReflectiveOperationException {
        Field field = handler.getDeclaredField(name);
        if (field.getType() != int.class || !java.lang.reflect.Modifier.isStatic(field.getModifiers())
                || java.lang.reflect.Modifier.isFinal(field.getModifiers()) || !field.trySetAccessible()) {
            throw new IllegalAccessException("Unsupported Relics research counter: " + name);
        }
        return field;
    }

    void render(ResearchHold.Progress progress, Runnable renderTooltip) {
        try {
            int savedPrevious = previousTicks == null ? 0 : previousTicks.getInt(null);
            int savedTicks = ticks.getInt(null);
            try {
                if (previousTicks != null) previousTicks.setInt(null, progress.previousTicks());
                ticks.setInt(null, progress.ticks());
                renderTooltip.run();
            } finally {
                if (previousTicks != null) previousTicks.setInt(null, savedPrevious);
                ticks.setInt(null, savedTicks);
            }
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not access validated Relics research counters", exception);
        }
    }
}
