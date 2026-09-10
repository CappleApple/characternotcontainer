package com.cappleapple.characternotcontainer.compat.armordamagescaling;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.network.ModifierSourcesResponsePayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.fml.ModList;

import java.util.Optional;

/**
 * Obtains Armor Damage Scaling's live results through the vanilla methods that its mixins replace.
 * This keeps configured formulas and configured protection-enchantment weights authoritative.
 */
public final class ArmorDamageScalingBridge {
    static final String MOD_ID = "armordamagescale";
    private static boolean failureLogged;

    private ArmorDamageScalingBridge() {}

    public static Optional<ModifierSourcesResponsePayload.ArmorDamageScalingValues> values(ServerPlayer player) {
        if (!ModList.get().isLoaded(MOD_ID)) return Optional.empty();

        float incomingDamage = player.getMaxHealth();
        if (!Float.isFinite(incomingDamage) || incomingDamage <= 0.0F) return Optional.empty();

        try {
            DamageSource source = new DamageSource(player.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.MOB_ATTACK));
            float armor = player.getArmorValue();
            float toughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            float protection = currentProtection(player, source);

            float afterArmor = CombatRules.getDamageAfterAbsorb(incomingDamage, armor, 0.0F);
            float afterArmorAndProtection = CombatRules.getDamageAfterMagicAbsorb(afterArmor, protection);
            float afterHeavyHit = CombatRules.getDamageAfterAbsorb(incomingDamage, armor, toughness);
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

    /** Forge 1.20.1 exposes the live protection calculation directly, including mod mixins. */
    static float currentProtection(ServerPlayer player, DamageSource source) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
    }
}
