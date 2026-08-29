package com.cappleapple.characternotcontainer.compat.puffishskills;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PufferfishSkillsSourceBridgeTest {
    @Test
    void preservesLiteralSkillTitles() {
        var source = PufferfishSkillsSourceBridge.sourceForTitle(Component.literal("Weapon Power III"));

        assertEquals("", source.translationKey());
        assertEquals("Weapon Power III", source.fallback());
    }

    @Test
    void preservesTranslatableSkillTitlesForClientLocalization() {
        var source = PufferfishSkillsSourceBridge.sourceForTitle(
                Component.translatableWithFallback("skill.within.weapon_power", "Weapon Power"));

        assertEquals("skill.within.weapon_power", source.translationKey());
        assertEquals("Weapon Power", source.fallback());
    }

    @Test
    void fallsBackToTheModNameForAnEmptyTitle() {
        var source = PufferfishSkillsSourceBridge.sourceForTitle(Component.empty());

        assertEquals("gui.characternotcontainer.puffish_skills", source.translationKey());
        assertEquals("Pufferfish Skills", source.fallback());
    }

    @Test
    void recognizesRomanAndNumericTrailingSkillLevels() {
        var roman = PufferfishSkillsSourceBridge.levelledTitle("Vitality IV").orElseThrow();
        var numeric = PufferfishSkillsSourceBridge.levelledTitle("Vitality 12").orElseThrow();

        assertEquals("Vitality", roman.base());
        assertEquals(4, roman.level());
        assertEquals("Vitality", numeric.base());
        assertEquals(12, numeric.level());
        assertTrue(PufferfishSkillsSourceBridge.levelledTitle("Vitality").isEmpty());
    }

    @Test
    void everyTierUsesTheHighestActiveTitleAndOneAggregationKey() {
        ResourceLocation attribute = ResourceLocation.withDefaultNamespace("generic.max_health");
        var first = PufferfishSkillsSourceBridge.sourceForTitle(Component.literal("Vitality I"));
        var fifth = PufferfishSkillsSourceBridge.sourceForTitle(Component.literal("Vitality V"));
        var entries = PufferfishSkillsSourceBridge.groupedEntries(List.of(
                new PufferfishSkillsSourceBridge.ActiveReward(attribute,
                        ResourceLocation.fromNamespaceAndPath("test", "vitality_1"), first,
                        PufferfishSkillsSourceBridge.levelledTitle(first.fallback()).orElseThrow()),
                new PufferfishSkillsSourceBridge.ActiveReward(attribute,
                        ResourceLocation.fromNamespaceAndPath("test", "vitality_5"), fifth,
                        PufferfishSkillsSourceBridge.levelledTitle(fifth.fallback()).orElseThrow())));

        assertEquals("Vitality V", entries.get(0).sources().getFirst().fallback());
        assertEquals("Vitality V", entries.get(1).sources().getFirst().fallback());
        assertEquals(entries.get(0).sources().getFirst().aggregationKey(),
                entries.get(1).sources().getFirst().aggregationKey());
        assertEquals("puffish_skills:vitality", entries.get(0).sources().getFirst().aggregationKey());
    }
}
