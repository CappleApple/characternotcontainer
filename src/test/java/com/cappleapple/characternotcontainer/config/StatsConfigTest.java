package com.cappleapple.characternotcontainer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsConfigTest {
    @Test
    void bundledStatsUseLocalizedNamesByDefault() {
        assertTrue(StatsConfig.defaults().categories.stream()
                .flatMap(category -> category.stats.stream())
                .allMatch(stat -> stat.name == null || stat.name.isBlank()));
    }
}
