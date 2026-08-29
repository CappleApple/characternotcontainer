package com.cappleapple.characternotcontainer.compat.armordamagescaling;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CharacterNotContainer.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal")
public final class ArmorDamageScalingGameTests {
    private static final String EMPTY_TEMPLATE = "bastion/mobs/empty";

    private ArmorDamageScalingGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void resistanceSnapshotUsesLiveArmorAndProtectionPipeline(GameTestHelper helper)
            throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("armordamagescale")) {
            helper.succeed();
            return;
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chestplate.enchant(helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.PROTECTION), 4);
        player.setItemSlot(EquipmentSlot.CHEST, chestplate);
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));

        float incomingDamage = player.getMaxHealth();
        DamageSource source = new DamageSource(player.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.MOB_ATTACK));
        float protection = ArmorDamageScalingBridge.currentProtection(player, source);
        float armorOnlyDamage = CombatRules.getDamageAfterAbsorb(
                player, incomingDamage, source, player.getArmorValue(), 0.0F);
        armorOnlyDamage = CombatRules.getDamageAfterMagicAbsorb(armorOnlyDamage, protection);
        float heavyHitDamage = CombatRules.getDamageAfterAbsorb(player, incomingDamage, source,
                player.getArmorValue(), (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        heavyHitDamage = CombatRules.getDamageAfterMagicAbsorb(heavyHitDamage, protection);

        var values = ArmorDamageScalingBridge.values(player)
                .orElseGet(() -> {
                    helper.fail("Armor Damage Scaling did not produce a resistance snapshot");
                    throw new AssertionError();
                });
        assertClose(helper, 1.0D - armorOnlyDamage / incomingDamage, values.damageResistance(),
                "Damage Resistance did not use the live armor and protection formulas");
        assertClose(helper, 1.0D - heavyHitDamage / incomingDamage, values.heavyHitResistance(),
                "Heavy Hit Resistance did not use the live armor, toughness, and protection formulas");
        helper.succeed();
    }

    private static void assertClose(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.00001D,
                message + "; expected " + expected + ", got " + actual);
    }
}
