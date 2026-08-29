package com.cappleapple.characternotcontainer.compat.armordamagescaling;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.network.ModifierSourcesResponsePayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Obtains Armor Damage Scaling's live results through the vanilla methods that its mixins replace.
 * This keeps configured formulas and configured protection-enchantment weights authoritative.
 */
public final class ArmorDamageScalingBridge {
    static final String MOD_ID = "armordamagescale";
    private static boolean failureLogged;
    private static volatile ProtectionConfigAccess protectionConfigAccess;

    private ArmorDamageScalingBridge() {}

    public static Optional<ModifierSourcesResponsePayload.ArmorDamageScalingValues> values(ServerPlayer player) {
        if (!ModList.get().isLoaded(MOD_ID)) return Optional.empty();

        float incomingDamage = player.getMaxHealth();
        if (!Float.isFinite(incomingDamage) || incomingDamage <= 0.0F) return Optional.empty();

        try {
            DamageSource source = new DamageSource(player.registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.MOB_ATTACK));
            float armor = player.getArmorValue();
            float toughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            float protection = currentProtection(player, source);

            float afterArmor = CombatRules.getDamageAfterAbsorb(player, incomingDamage, source, armor, 0.0F);
            float afterArmorAndProtection = CombatRules.getDamageAfterMagicAbsorb(afterArmor, protection);
            float afterHeavyHit = CombatRules.getDamageAfterAbsorb(player, incomingDamage, source, armor, toughness);
            float afterHeavyHitAndProtection = CombatRules.getDamageAfterMagicAbsorb(afterHeavyHit, protection);

            Optional<Double> damageResistance = resistance(incomingDamage, afterArmorAndProtection);
            Optional<Double> heavyHitResistance = resistance(incomingDamage, afterHeavyHitAndProtection);
            if (damageResistance.isEmpty() || heavyHitResistance.isEmpty()) {
                throw new IllegalStateException("Armor Damage Scaling returned a non-finite damage result");
            }

            failureLogged = false;
            return Optional.of(new ModifierSourcesResponsePayload.ArmorDamageScalingValues(
                    damageResistance.get(), heavyHitResistance.get()));
        } catch (Exception | LinkageError exception) {
            if (!failureLogged) {
                CharacterNotContainer.LOGGER.error(
                        "Could not calculate Armor Damage Scaling attribute resistance values", exception);
                failureLogged = true;
            }
            return Optional.empty();
        }
    }

    static Optional<Double> resistance(double incomingDamage, double remainingDamage) {
        if (!Double.isFinite(incomingDamage) || incomingDamage <= 0.0D || !Double.isFinite(remainingDamage)) {
            return Optional.empty();
        }
        double result = 1.0D - remainingDamage / incomingDamage;
        return Double.isFinite(result) ? Optional.of(result) : Optional.empty();
    }

    /** Mirrors Armor Damage Scaling's protection-level mixin without relying on Cupboard's stale registry helper. */
    static float currentProtection(ServerPlayer player, DamageSource source) throws ReflectiveOperationException {
        Map<?, ?> configuredLevels = protectionConfigAccess().protectionLevels();
        float protection = 0.0F;
        var enchantmentRegistry = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).asLookup();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            var enchantments = stack.getAllEnchantments(enchantmentRegistry);
            for (var entry : enchantments.entrySet()) {
                var enchantment = entry.getKey();
                int level = entry.getIntValue();
                if (!enchantment.value().matchingSlot(slot)) continue;

                float before = protection;
                LootContext context = Enchantment.damageContext(player.serverLevel(), level, player, source);
                for (ConditionalEffect<EnchantmentValueEffect> effect
                        : enchantment.value().getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION)) {
                    if (effect.matches(context)) {
                        protection = effect.effect().process(level, player.getRandom(), protection);
                    }
                }

                if (Float.compare(before, protection) != 0) {
                    ResourceLocation id = enchantment.unwrapKey().map(key -> key.location()).orElse(null);
                    Object configured = id == null ? null : configuredLevels.get(id.toString());
                    if (configured instanceof Number number && number.doubleValue() != -770.0D) {
                        protection = before + number.floatValue() * level;
                    }
                }
            }
        }
        return protection;
    }

    private static ProtectionConfigAccess protectionConfigAccess() throws ReflectiveOperationException {
        ProtectionConfigAccess loaded = protectionConfigAccess;
        if (loaded != null) return loaded;
        loaded = new ProtectionConfigAccess();
        protectionConfigAccess = loaded;
        return loaded;
    }

    private static final class ProtectionConfigAccess {
        private final Field config;
        private final Method commonConfig;
        private final Field protectionLevels;

        private ProtectionConfigAccess() throws ReflectiveOperationException {
            Class<?> modType = Class.forName("com.armordamagescale.ArmorDamage");
            config = modType.getField("config");
            Object configValue = config.get(null);
            commonConfig = configValue.getClass().getMethod("getCommonConfig");
            Object commonValue = commonConfig.invoke(configValue);
            protectionLevels = commonValue.getClass().getField("protectionLevels");
        }

        private Map<?, ?> protectionLevels() throws ReflectiveOperationException {
            Object configValue = config.get(null);
            Object commonValue = commonConfig.invoke(configValue);
            Object levels = protectionLevels.get(commonValue);
            if (levels instanceof Map<?, ?> map) return map;
            throw new IllegalStateException("Armor Damage Scaling protectionLevels is not a map");
        }
    }
}
