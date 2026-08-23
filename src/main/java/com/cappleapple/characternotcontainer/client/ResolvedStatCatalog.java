package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.api.CharacterUiApi;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import com.cappleapple.characternotcontainer.config.StatCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ResolvedStatCatalog {
    private static final Set<String> WARNED_MISSING = new HashSet<>();
    private static List<ResolvedCategory> cached;
    private static long cachedRevision = -1;
    private static long cachedApiRevision = -1;

    private ResolvedStatCatalog() {}

    static List<ResolvedCategory> categories() {
        long revision = CharacterConfigManager.revision();
        long apiRevision = CharacterUiApi.revision();
        if (cached == null || cachedRevision != revision || cachedApiRevision != apiRevision) {
            cached = resolve();
            cachedRevision = revision;
            cachedApiRevision = apiRevision;
        }
        return cached;
    }

    static void invalidate() {
        cached = null;
        cachedRevision = -1;
        cachedApiRevision = -1;
    }

    private static List<ResolvedCategory> resolve() {
        List<StatCategory> definitions = new ArrayList<>();
        CharacterConfigManager.stats().categories.forEach(category -> definitions.add(copy(category)));
        CharacterUiApi.registeredCategories().forEach(category -> definitions.add(copy(category)));
        var byId = new HashMap<String, StatCategory>();
        definitions.forEach(category -> byId.put(category.id, category));
        CharacterUiApi.registeredStats().forEach(entry -> {
            StatCategory category = byId.get(entry.categoryId());
            if (category == null) {
                category = new StatCategory(entry.categoryId(), entry.categoryId(), Integer.MAX_VALUE);
                definitions.add(category);
                byId.put(entry.categoryId(), category);
            }
            category.stats.add(entry.stat());
        });
        definitions.sort(java.util.Comparator.comparingInt(category -> category.order));

        List<ResolvedCategory> result = new ArrayList<>();
        for (StatCategory category : definitions) {
            List<ResolvedStat> stats = new ArrayList<>();
            for (var definition : category.stats) {
                ResourceLocation id = ResourceLocation.tryParse(definition.attribute);
                var holder = id == null ? java.util.Optional.<net.minecraft.core.Holder.Reference<net.minecraft.world.entity.ai.attributes.Attribute>>empty()
                        : BuiltInRegistries.ATTRIBUTE.getHolder(id);
                if (holder.isPresent()) {
                    stats.add(new ResolvedStat(definition, holder.get()));
                } else if (WARNED_MISSING.add(definition.attribute)) {
                    CharacterNotContainer.LOGGER.warn("Skipping unknown configured attribute {}", definition.attribute);
                }
            }
            if (!stats.isEmpty()) result.add(new ResolvedCategory(category, List.copyOf(stats)));
        }
        return List.copyOf(result);
    }

    private static StatCategory copy(StatCategory source) {
        StatCategory copy = new StatCategory(source.id, source.name, source.order);
        copy.initiallyCollapsed = source.initiallyCollapsed;
        copy.icon = source.icon;
        copy.stats.addAll(source.stats);
        return copy;
    }
}
