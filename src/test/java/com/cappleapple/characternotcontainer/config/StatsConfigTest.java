package com.cappleapple.characternotcontainer.config;

import org.junit.jupiter.api.Test;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsConfigTest {
    @Test
    void bundledStatsUseLocalizedNamesByDefault() {
        assertTrue(StatsConfig.defaults().attributes.stream()
                .allMatch(stat -> stat.name == null || stat.name.isBlank()));
    }

    @Test
    void bundledStatsAreAFlatAttributeCatalog() {
        assertTrue(StatsConfig.defaults().attributes.size() >= 14);
        assertTrue(StatsConfig.defaults().attributes.stream()
                .allMatch(stat -> stat.attribute != null && !stat.attribute.isBlank()));
        assertTrue(StatsConfig.defaults().attributes.stream().allMatch(stat -> stat.visible));
    }

    @Test
    void visibilityDefaultsOnAndSurvivesConfigNormalization() {
        Gson gson = new Gson();
        StatDefinition implicit = gson.fromJson("{\"attribute\":\"example:implicit\"}", StatDefinition.class);
        var hiddenJson = JsonParser.parseString(
                "{\"attribute\":\"example:hidden\",\"visible\":false,\"deltaFormat\":\"legacy\"}")
                .getAsJsonObject();
        StatDefinition hidden = gson.fromJson(CharacterConfigManager.normalizeStatJson(hiddenJson), StatDefinition.class);
        assertTrue(implicit.visible);
        assertFalse(hidden.visible);
    }
}
