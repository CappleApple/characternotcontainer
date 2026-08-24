package com.cappleapple.characternotcontainer.compat.needsnotnecessities;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.network.ModifierSourcesResponsePayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Exact, optional integration with Needs Not Necessities' public runtime records. */
public final class NeedsNotNecessitiesSourceBridge {
    private static final String MOD_ID = "needs_not_necessities";
    private static volatile Bridge bridge;
    private static volatile boolean failed;

    private NeedsNotNecessitiesSourceBridge() {}

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
            CharacterNotContainer.LOGGER.error("Could not read Needs Not Necessities attribute sources", exception);
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
            CharacterNotContainer.LOGGER.info("Enabled direct Needs Not Necessities attribute-source support");
            return loaded;
        } catch (ReflectiveOperationException | LinkageError exception) {
            failed = true;
            CharacterNotContainer.LOGGER.error("Needs Not Necessities source integration is unavailable", exception);
            return null;
        }
    }

    private static final class Bridge {
        private final List<Method> stateGetters;
        private final Method stateDisplayName;
        private final Method stateModifiers;
        private final Method activeMealGetter;
        private final Method mealSourceItem;
        private final Method mealDisplayName;
        private final Method mealModifiers;
        private final Method comfortGetter;
        private final Object comfortManager;
        private final Method comfortModifiersAt;
        private final Method modifierId;
        private final Method modifierTarget;

        private Bridge() throws ReflectiveOperationException {
            Class<?> api = Class.forName("com.cappleapple.needsnotnecessities.api.NeedsNotNecessitiesApi");
            Method hunger = api.getMethod("getHungerState", ServerPlayer.class);
            Method thirst = api.getMethod("getThirstState", ServerPlayer.class);
            Method rest = api.getMethod("getRestState", ServerPlayer.class);
            stateGetters = List.of(hunger, thirst, rest);
            Class<?> stateType = hunger.getReturnType();
            stateDisplayName = stateType.getMethod("displayName");
            stateModifiers = stateType.getMethod("modifiers");

            activeMealGetter = api.getMethod("getActiveMeal", ServerPlayer.class);
            Class<?> mealType = Class.forName("com.cappleapple.needsnotnecessities.data.ActiveMealData");
            mealSourceItem = mealType.getMethod("sourceItem");
            mealDisplayName = mealType.getMethod("displayName");
            mealModifiers = mealType.getMethod("modifiers");

            comfortGetter = api.getMethod("getComfort", ServerPlayer.class);
            Class<?> comfortManagerType = Class.forName(
                    "com.cappleapple.needsnotnecessities.survival.comfort.ComfortEffectManager");
            Field instance = comfortManagerType.getField("INSTANCE");
            comfortManager = instance.get(null);
            comfortModifiersAt = comfortManagerType.getMethod("modifiersAt", double.class);

            Class<?> modifierType = Class.forName("com.cappleapple.needsnotnecessities.modifier.SurvivalModifier");
            modifierId = modifierType.getMethod("id");
            modifierTarget = modifierType.getMethod("target");
        }

        private List<ModifierSourcesResponsePayload.Entry> sources(ServerPlayer player)
                throws ReflectiveOperationException {
            Map<Key, ModifierSourcesResponsePayload.Source> known = new LinkedHashMap<>();
            for (Method getter : stateGetters) {
                Object state = getter.invoke(null, player);
                String name = (String)stateDisplayName.invoke(state);
                addAll(known, collection(stateModifiers.invoke(state)), ItemStack.EMPTY, "", name);
            }

            Optional<?> meal = (Optional<?>)activeMealGetter.invoke(null, player);
            if (meal.isPresent()) {
                Object value = meal.get();
                ResourceLocation itemId = (ResourceLocation)mealSourceItem.invoke(value);
                ItemStack stack = BuiltInRegistries.ITEM.getOptional(itemId)
                        .map(ItemStack::new)
                        .orElse(ItemStack.EMPTY);
                addAll(known, collection(mealModifiers.invoke(value)), stack, "",
                        (String)mealDisplayName.invoke(value));
            }

            double comfort = (double)comfortGetter.invoke(null, player);
            addAll(known, collection(comfortModifiersAt.invoke(comfortManager, comfort)), ItemStack.EMPTY,
                    "gui.characternotcontainer.nnn.comfort", "Comfort");

            List<ModifierSourcesResponsePayload.Entry> result = new ArrayList<>(known.size());
            known.forEach((key, source) -> result.add(new ModifierSourcesResponsePayload.Entry(
                    key.attributeId, key.modifierId, List.of(source))));
            return List.copyOf(result);
        }

        private void addAll(Map<Key, ModifierSourcesResponsePayload.Source> destination, Collection<?> modifiers,
                            ItemStack stack, String translationKey, String fallback)
                throws ReflectiveOperationException {
            for (Object modifier : modifiers) {
                ResourceLocation id = (ResourceLocation)modifierId.invoke(modifier);
                ResourceLocation target = (ResourceLocation)modifierTarget.invoke(modifier);
                destination.putIfAbsent(new Key(target, id),
                        new ModifierSourcesResponsePayload.Source(stack, translationKey, fallback));
            }
        }

        private static Collection<?> collection(Object value) {
            return value instanceof Collection<?> collection ? collection : List.of();
        }
    }

    private record Key(ResourceLocation attributeId, ResourceLocation modifierId) {}
}
