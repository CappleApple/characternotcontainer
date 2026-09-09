package com.cappleapple.characternotcontainer.equipment;

import com.cappleapple.characternotcontainer.gametest.GameTestPlayers;
import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import com.cappleapple.characternotcontainer.network.EquipmentChangePayload;
import com.cappleapple.characternotcontainer.network.NearbyEquipmentRequestPayload;
import com.cappleapple.characternotcontainer.network.NearbyEquipmentResponsePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

@GameTestHolder(CharacterNotContainer.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal")

public final class NearbyArmorSourceGameTests {
    private static final String EMPTY_TEMPLATE = "bastion/mobs/empty";
    private static final List<ArmorCase> ARMOR_CASES = List.of(
            new ArmorCase(EquipmentSlot.HEAD, Items.IRON_HELMET, Items.DIAMOND_HELMET),
            new ArmorCase(EquipmentSlot.CHEST, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE),
            new ArmorCase(EquipmentSlot.LEGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS),
            new ArmorCase(EquipmentSlot.FEET, Items.IRON_BOOTS, Items.DIAMOND_BOOTS));

    private NearbyArmorSourceGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void equipsVanillaArmorFromNearbyContainer(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.create(helper);
        BlockPos chestPos = player.blockPosition().offset(1, 0, 0);
        helper.getLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        Container chest = (Container)helper.getLevel().getBlockEntity(chestPos);
        for (int index = 0; index < ARMOR_CASES.size(); index++) {
            ArmorCase armor = ARMOR_CASES.get(index);
            player.setItemSlot(armor.slot(), new ItemStack(armor.previous()));
            chest.setItem(index, new ItemStack(armor.replacement()));
        }

        withNearbySourcesEnabled(player, () -> {
            for (int index = 0; index < ARMOR_CASES.size(); index++) {
                ArmorCase armor = ARMOR_CASES.get(index);
                equipNearbyArmor(helper, player, armor, index + 1);
                helper.assertTrue(chest.getItem(index).isEmpty(),
                        "Nearby container kept the equipped " + armor.slot().getName() + " armor");
            }
        });
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void equipsVanillaArmorFromNearbyArmorStand(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.create(helper);
        ArmorStand stand = new ArmorStand(helper.getLevel(), player.getX() + 1.0D, player.getY(), player.getZ());
        for (ArmorCase armor : ARMOR_CASES) {
            player.setItemSlot(armor.slot(), new ItemStack(armor.previous()));
            stand.setItemSlot(armor.slot(), new ItemStack(armor.replacement()));
        }
        helper.getLevel().addFreshEntity(stand);

        withNearbySourcesEnabled(player, () -> {
            for (int index = 0; index < ARMOR_CASES.size(); index++) {
                ArmorCase armor = ARMOR_CASES.get(index);
                equipNearbyArmor(helper, player, armor, index + 10);
                helper.assertTrue(stand.getItemBySlot(armor.slot()).isEmpty(),
                        "Nearby armor stand kept the equipped " + armor.slot().getName() + " armor");
            }
        });
        helper.succeed();
    }

    private static void equipNearbyArmor(GameTestHelper helper, ServerPlayer player, ArmorCase armor, int searchId) {
        EquipmentChangePayload targetPayload = new EquipmentChangePayload(EquipmentChangePayload.TargetSystem.VANILLA,
                armor.slot().getName(), 0, false, EquipmentChangePayload.SourceKind.UNEQUIP, -1, searchId);
        EquipmentTargetAccess target = VanillaEquipmentTarget.resolve(player, targetPayload)
                .orElseGet(() -> {
                    helper.fail("Vanilla " + armor.slot().getName() + " armor target could not be resolved");
                    throw new AssertionError();
                });
        NearbyEquipmentResponsePayload response = NearbyEquipmentSources.search(player,
                new NearbyEquipmentRequestPayload(searchId, EquipmentChangePayload.TargetSystem.VANILLA,
                        armor.slot().getName(), 0, false), target);
        NearbyEquipmentResponsePayload.Entry candidate = response.entries().stream()
                .filter(entry -> entry.stack().is(armor.replacement()))
                .findFirst()
                .orElseGet(() -> {
                    helper.fail("Nearby search did not return " + armor.replacement());
                    throw new AssertionError();
                });
        boolean changed = NearbyEquipmentSources.change(player,
                new EquipmentChangePayload(EquipmentChangePayload.TargetSystem.VANILLA,
                        armor.slot().getName(), 0, false, EquipmentChangePayload.SourceKind.NEARBY,
                        candidate.token(), searchId), target);

        helper.assertTrue(changed, "Nearby vanilla armor transaction was refused");
        helper.assertTrue(player.getItemBySlot(armor.slot()).is(armor.replacement()),
                "Player did not equip the nearby " + armor.slot().getName() + " armor");
        IItemHandler inventory = PlayerInventoryAccess.handler(player);
        boolean returnedPreviousArmor = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            returnedPreviousArmor |= inventory.getStackInSlot(slot).is(armor.previous());
        }
        helper.assertTrue(returnedPreviousArmor,
                "Previously equipped " + armor.slot().getName() + " armor was not returned to player inventory");
    }

    private static void withNearbySourcesEnabled(ServerPlayer player, Runnable action) {
        boolean previousEnabled = CharacterConfigManager.general().enableNearbyEquipmentSources;
        double previousRadius = CharacterConfigManager.general().nearbyEquipmentSearchRadius;
        CharacterConfigManager.general().enableNearbyEquipmentSources = true;
        CharacterConfigManager.general().nearbyEquipmentSearchRadius = 8.0D;
        try {
            action.run();
        } finally {
            NearbyEquipmentSources.clear(player);
            CharacterConfigManager.general().enableNearbyEquipmentSources = previousEnabled;
            CharacterConfigManager.general().nearbyEquipmentSearchRadius = previousRadius;
        }
    }

    private record ArmorCase(EquipmentSlot slot, Item previous, Item replacement) {}
}
