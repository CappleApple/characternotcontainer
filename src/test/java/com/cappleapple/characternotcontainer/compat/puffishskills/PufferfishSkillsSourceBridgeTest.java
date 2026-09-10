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
        ResourceLocation attribute = new ResourceLocation("generic.max_health");
        var first = PufferfishSkillsSourceBridge.sourceForTitle(Component.literal("Vitality I"));
        var fifth = PufferfishSkillsSourceBridge.sourceForTitle(Component.literal("Vitality V"));
        var entries = PufferfishSkillsSourceBridge.groupedEntries(List.of(
                new PufferfishSkillsSourceBridge.ActiveReward(attribute,
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), first,
                        PufferfishSkillsSourceBridge.levelledTitle(first.fallback()).orElseThrow()),
                new PufferfishSkillsSourceBridge.ActiveReward(attribute,
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000005"), fifth,
                        PufferfishSkillsSourceBridge.levelledTitle(fifth.fallback()).orElseThrow())));

        assertEquals("Vitality V", entries.get(0).sources().get(0).fallback());
        assertEquals("Vitality V", entries.get(1).sources().get(0).fallback());
        assertEquals(entries.get(0).sources().get(0).aggregationKey(),
                entries.get(1).sources().get(0).aggregationKey());
        assertEquals("puffish_skills:vitality", entries.get(0).sources().get(0).aggregationKey());
    }
}
