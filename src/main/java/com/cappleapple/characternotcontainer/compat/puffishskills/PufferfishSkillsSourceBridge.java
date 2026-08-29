package com.cappleapple.characternotcontainer.compat.puffishskills;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.network.ModifierSourcesResponsePayload;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Exact, optional integration with Pufferfish Skills' loaded attribute rewards. */
public final class PufferfishSkillsSourceBridge {
    private static final String MOD_ID = "puffish_skills";
    private static final String GENERIC_TRANSLATION = "gui.characternotcontainer.puffish_skills";
    private static final String GENERIC_FALLBACK = "Pufferfish Skills";
    private static final Pattern LEVEL_SUFFIX = Pattern.compile(
            "^(.*\\S)\\s+([1-9][0-9]*|(?i:[IVXLCDM]+))$");
    private static volatile Bridge bridge;
    private static volatile boolean failed;

    private PufferfishSkillsSourceBridge() {}

    public static void serverStarted(ServerStartedEvent event) {
        if (!ModList.get().isLoaded(MOD_ID)) return;
        loadBridge();
    }

    public static List<ModifierSourcesResponsePayload.Entry> sources(ServerPlayer player) {
        if (!ModList.get().isLoaded(MOD_ID) || failed) return List.of();
        Bridge loaded = loadBridge();
        if (loaded == null) return List.of();
        try {
            return loaded.sources(player);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            CharacterNotContainer.LOGGER.error("Could not read Pufferfish Skills attribute sources", exception);
            return List.of();
        }
    }

    private static Bridge loadBridge() {
        if (failed) return null;
        Bridge loaded = bridge;
        if (loaded != null) return loaded;
        try {
            loaded = new Bridge();
            bridge = loaded;
            CharacterNotContainer.LOGGER.info("Enabled direct Pufferfish Skills attribute-source support");
            return loaded;
        } catch (ReflectiveOperationException | LinkageError exception) {
            failed = true;
            CharacterNotContainer.LOGGER.error("Pufferfish Skills source integration is unavailable", exception);
            return null;
        }
    }

    static ModifierSourcesResponsePayload.Source sourceForTitle(Component title) {
        if (title == null) return genericSource();
        String fallback = limited(title.getString());
        if (fallback.isBlank()) return genericSource();
        if (title.getContents() instanceof TranslatableContents translatable
                && translatable.getArgs().length == 0 && title.getSiblings().isEmpty()) {
            return new ModifierSourcesResponsePayload.Source(ItemStack.EMPTY,
                    limited(translatable.getKey()), fallback);
        }
        return new ModifierSourcesResponsePayload.Source(ItemStack.EMPTY, "", fallback);
    }

    private static ModifierSourcesResponsePayload.Source genericSource() {
        return new ModifierSourcesResponsePayload.Source(ItemStack.EMPTY, GENERIC_TRANSLATION, GENERIC_FALLBACK);
    }

    static Optional<LevelledTitle> levelledTitle(String title) {
        if (title == null) return Optional.empty();
        Matcher matcher = LEVEL_SUFFIX.matcher(title.strip());
        if (!matcher.matches()) return Optional.empty();
        String base = matcher.group(1).strip();
        String suffix = matcher.group(2);
        if (base.isEmpty()) return Optional.empty();
        try {
            int level = Character.isDigit(suffix.charAt(0))
                    ? Integer.parseInt(suffix)
                    : romanLevel(suffix.toUpperCase(Locale.ROOT));
            return level > 0 ? Optional.of(new LevelledTitle(base, level)) : Optional.empty();
        } catch (ArithmeticException | NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static int romanLevel(String value) {
        int total = 0;
        int previous = 0;
        for (int index = value.length() - 1; index >= 0; index--) {
            int current = switch (value.charAt(index)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                case 'L' -> 50;
                case 'C' -> 100;
                case 'D' -> 500;
                case 'M' -> 1000;
                default -> 0;
            };
            total = Math.addExact(total, current < previous ? -current : current);
            previous = current;
        }
        return total;
    }

    record LevelledTitle(String base, int level) {
        String normalizedBase() {
            return base.toLowerCase(Locale.ROOT);
        }
    }

    private static String limited(String value) {
        if (value == null) return "";
        return value.length() <= 256 ? value : value.substring(0, 256);
    }

    private static Collection<?> collection(Object value) {
        return value instanceof Collection<?> collection ? collection : List.of();
    }

    private static final class Bridge {
        private final Method skillsModInstance;
        private final Method allCategories;
        private final Method categoryDefinitions;
        private final Method allDefinitions;
        private final Method definitionTitle;
        private final Method definitionRewards;
        private final Method rewardInstance;
        private final Class<?> attributeRewardType;
        private final Field attributeRewardIds;
        private final Field attributeRewardAttribute;

        private Bridge() throws ReflectiveOperationException {
            Class<?> skillsModType = Class.forName("net.puffish.skillsmod.SkillsMod");
            skillsModInstance = skillsModType.getMethod("getInstance");
            allCategories = skillsModType.getDeclaredMethod("getAllCategories");
            allCategories.setAccessible(true);

            Class<?> categoryType = Class.forName("net.puffish.skillsmod.config.CategoryConfig");
            categoryDefinitions = categoryType.getMethod("definitions");
            Class<?> definitionsType = Class.forName(
                    "net.puffish.skillsmod.config.skill.SkillDefinitionsConfig");
            allDefinitions = definitionsType.getMethod("getAll");
            Class<?> definitionType = Class.forName(
                    "net.puffish.skillsmod.config.skill.SkillDefinitionConfig");
            definitionTitle = definitionType.getMethod("title");
            definitionRewards = definitionType.getMethod("rewards");
            Class<?> rewardConfigType = Class.forName(
                    "net.puffish.skillsmod.config.skill.SkillRewardConfig");
            rewardInstance = rewardConfigType.getMethod("instance");

            attributeRewardType = Class.forName("net.puffish.skillsmod.reward.builtin.AttributeReward");
            attributeRewardIds = attributeRewardType.getDeclaredField("ids");
            attributeRewardIds.setAccessible(true);
            attributeRewardAttribute = attributeRewardType.getDeclaredField("attribute");
            attributeRewardAttribute.setAccessible(true);
        }

        private List<ModifierSourcesResponsePayload.Entry> sources(ServerPlayer player)
                throws ReflectiveOperationException {
            List<ActiveReward> activeRewards = new ArrayList<>();
            Object skillsMod = skillsModInstance.invoke(null);
            for (Object category : collection(allCategories.invoke(skillsMod))) {
                Object definitions = categoryDefinitions.invoke(category);
                for (Object definition : collection(allDefinitions.invoke(definitions))) {
                    Component title = definitionTitle.invoke(definition) instanceof Component component
                            ? component : null;
                    ModifierSourcesResponsePayload.Source source = sourceForTitle(title);
                    LevelledTitle levelledTitle = levelledTitle(source.fallback()).orElse(null);
                    for (Object rewardConfig : collection(definitionRewards.invoke(definition))) {
                        Object reward = rewardInstance.invoke(rewardConfig);
                        if (!attributeRewardType.isInstance(reward)) continue;
                        addRewardSources(activeRewards, player, reward, source, levelledTitle);
                    }
                }
            }
            return groupedEntries(activeRewards);
        }

        @SuppressWarnings("unchecked")
        private void addRewardSources(List<ActiveReward> result, ServerPlayer player,
                                      Object reward, ModifierSourcesResponsePayload.Source source,
                                      LevelledTitle levelledTitle)
                throws IllegalAccessException {
            Object rawHolder = attributeRewardAttribute.get(reward);
            if (!(rawHolder instanceof Holder<?> holder) || !(holder.value() instanceof Attribute attribute)) return;
            ResourceLocation attributeId = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
            if (attributeId == null) return;
            AttributeInstance instance = player.getAttribute((Holder<Attribute>)holder);
            if (instance == null) return;
            for (Object rawId : collection(attributeRewardIds.get(reward))) {
                if (rawId instanceof ResourceLocation modifierId && instance.getModifier(modifierId) != null) {
                    result.add(new ActiveReward(attributeId, modifierId, source, levelledTitle));
                }
            }
        }

    }

    static List<ModifierSourcesResponsePayload.Entry> groupedEntries(List<ActiveReward> activeRewards) {
        Map<GroupKey, ActiveReward> highest = new LinkedHashMap<>();
        for (ActiveReward reward : activeRewards) {
            if (reward.levelledTitle == null) continue;
            GroupKey key = new GroupKey(reward.attributeId, reward.levelledTitle.normalizedBase());
            highest.merge(key, reward, (current, candidate) ->
                    candidate.levelledTitle.level > current.levelledTitle.level ? candidate : current);
        }

        List<ModifierSourcesResponsePayload.Entry> result = new ArrayList<>(activeRewards.size());
        for (ActiveReward reward : activeRewards) {
            ModifierSourcesResponsePayload.Source source = reward.source;
            if (reward.levelledTitle != null) {
                GroupKey key = new GroupKey(reward.attributeId, reward.levelledTitle.normalizedBase());
                ActiveReward selected = highest.get(key);
                source = new ModifierSourcesResponsePayload.Source(
                        selected.source.stack(), selected.source.translationKey(), selected.source.fallback(),
                        limited("puffish_skills:" + key.normalizedBase));
            }
            result.add(new ModifierSourcesResponsePayload.Entry(
                    reward.attributeId, reward.modifierId, List.of(source)));
        }
        return List.copyOf(result);
    }

    record ActiveReward(ResourceLocation attributeId, ResourceLocation modifierId,
                        ModifierSourcesResponsePayload.Source source, LevelledTitle levelledTitle) {}

    private record GroupKey(ResourceLocation attributeId, String normalizedBase) {}
}
