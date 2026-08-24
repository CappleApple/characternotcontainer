package com.cappleapple.characternotcontainer.api;

import com.cappleapple.characternotcontainer.config.StatDefinition;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CharacterUiApi {
    private static final CopyOnWriteArrayList<StatDefinition> STATS = new CopyOnWriteArrayList<>();
    private static final java.util.concurrent.atomic.AtomicLong REVISION = new java.util.concurrent.atomic.AtomicLong();

    private CharacterUiApi() {}

    public static void registerStat(StatDefinition stat) {
        if (stat == null || stat.attribute == null || stat.attribute.isBlank()) {
            throw new IllegalArgumentException("Attribute id is required");
        }
        STATS.add(stat);
        REVISION.incrementAndGet();
    }

    public static List<StatDefinition> registeredStats() { return List.copyOf(STATS); }
    public static long revision() { return REVISION.get(); }
}
