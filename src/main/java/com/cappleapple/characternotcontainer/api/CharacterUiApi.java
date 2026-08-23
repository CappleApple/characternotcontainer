package com.cappleapple.characternotcontainer.api;

import com.cappleapple.characternotcontainer.config.StatCategory;
import com.cappleapple.characternotcontainer.config.StatDefinition;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CharacterUiApi {
    private static final CopyOnWriteArrayList<StatCategory> CATEGORIES = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<RegisteredStat> STATS = new CopyOnWriteArrayList<>();
    private static final java.util.concurrent.atomic.AtomicLong REVISION = new java.util.concurrent.atomic.AtomicLong();

    private CharacterUiApi() {}

    public static void registerCategory(StatCategory category) {
        if (category == null || category.id == null || category.id.isBlank()) throw new IllegalArgumentException("Category id is required");
        CATEGORIES.add(category);
        REVISION.incrementAndGet();
    }

    public static void registerStat(String categoryId, StatDefinition stat) {
        if (categoryId == null || categoryId.isBlank() || stat == null) throw new IllegalArgumentException("Category id and stat are required");
        STATS.add(new RegisteredStat(categoryId, stat));
        REVISION.incrementAndGet();
    }

    public static List<StatCategory> registeredCategories() { return List.copyOf(CATEGORIES); }
    public static List<RegisteredStat> registeredStats() { return List.copyOf(STATS); }
    public static long revision() { return REVISION.get(); }

    public record RegisteredStat(String categoryId, StatDefinition stat) {}
}
