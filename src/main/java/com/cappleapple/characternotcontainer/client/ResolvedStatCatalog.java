package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.api.CharacterUiApi;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import com.cappleapple.characternotcontainer.config.StatDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

final class ResolvedStatCatalog {
    private static final Set<String> WARNED_MISSING = new HashSet<>();
    private static List<ResolvedStat> cached;
    private static long cachedRevision = -1;
    private static long cachedApiRevision = -1;

    private ResolvedStatCatalog() {}

    static List<ResolvedStat> stats() {
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

    private static List<ResolvedStat> resolve() {
        LinkedHashMap<String, StatDefinition> definitions = new LinkedHashMap<>();
        CharacterConfigManager.stats().attributes.forEach(stat -> definitions.putIfAbsent(stat.attribute, stat));
        CharacterUiApi.registeredStats().forEach(stat -> definitions.put(stat.attribute, stat));

        List<ResolvedStat> result = new ArrayList<>();
        for (StatDefinition definition : definitions.values()) {
            ResourceLocation id = ResourceLocation.tryParse(definition.attribute);
            var holder = id == null ? java.util.Optional.<net.minecraft.core.Holder.Reference<Attribute>>empty()
                    : BuiltInRegistries.ATTRIBUTE.getHolder(id);
            if (holder.isPresent()) {
                result.add(new ResolvedStat(definition, holder.get()));
            } else if (WARNED_MISSING.add(definition.attribute)) {
                CharacterNotContainer.LOGGER.warn("Skipping unknown configured attribute {}", definition.attribute);
            }
        }
        return List.copyOf(result);
    }
}
