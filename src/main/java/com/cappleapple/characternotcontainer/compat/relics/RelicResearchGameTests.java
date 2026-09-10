package com.cappleapple.characternotcontainer.compat.relics;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.gametest.GameTestPlayers;
import com.cappleapple.characternotcontainer.compat.curios.CuriosEquipmentMutator;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import com.cappleapple.characternotcontainer.equipment.NearbyEquipmentSources;
import com.cappleapple.characternotcontainer.equipment.VanillaEquipmentTarget;
import com.cappleapple.characternotcontainer.network.EquipmentChangePayload;
import com.cappleapple.characternotcontainer.network.NearbyEquipmentRequestPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(CharacterNotContainer.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal")
public final class RelicResearchGameTests {
    private static final String TEMPLATE = "empty";
    private RelicResearchGameTests() {}

    @GameTest(templateNamespace = CharacterNotContainer.MOD_ID, template = TEMPLATE)
    public static void liveInventorySlotCannotBeTakenOrReplaced(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.create(helper);
        ItemStackHandler inventory = new ItemStackHandler(1);
        ItemStack original = new ItemStack(Items.IRON_BOOTS);
        inventory.setStackInSlot(0, original);
        AtomicInteger saves = new AtomicInteger();
        RelicResearchMenu menu = new RelicResearchMenu(1,
                RelicResearchSource.handler(() -> inventory, 0, () -> true, saves::incrementAndGet));
        helper.assertTrue(menu.getSlot(0).getItem() == original, "Research used a copy instead of the live inventory item");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        menu.quickMoveStack(player, 0);
        helper.assertTrue(inventory.getStackInSlot(0) == original && menu.getCarried().isEmpty(), "Research moved an item");
        menu.getSlot(0).getItem().setHoverName(Component.literal("Researched"));
        menu.broadcastChanges();
        helper.assertTrue(saves.get() == 1 && original.hasCustomHoverName(), "Research changes were not saved");
        inventory.setStackInSlot(0, new ItemStack(Items.IRON_BOOTS));
        helper.assertTrue(!menu.stillValid(player) && menu.getSlot(0).getItem().isEmpty(), "Research followed a replacement item");
        helper.succeed();
    }

    @GameTest(templateNamespace = CharacterNotContainer.MOD_ID, template = TEMPLATE)
    public static void equippedRelicKeepsNativeDataChanges(GameTestHelper helper) throws Exception {
        ServerPlayer player = GameTestPlayers.create(helper);
        ItemStack stack = new ItemStack(Items.IRON_BOOTS);
        if (ModList.get().isLoaded("relics")) {
            stack = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("relics:roller_skates")));
            helper.assertTrue(RelicsIntegration.isRelic(stack), "Installed Relics item was not recognized");
        }
        player.setItemSlot(EquipmentSlot.FEET, stack);
        var target = VanillaEquipmentTarget.resolve(player, target(EquipmentChangePayload.SourceKind.UNEQUIP, -1, 0)).orElseThrow();
        RelicResearchMenu menu = new RelicResearchMenu(1, target.researchSource().orElseThrow());
        helper.assertTrue(menu.getSlot(0).getItem() == player.getItemBySlot(EquipmentSlot.FEET), "Equipped slot used a copy");
        if (ModList.get().isLoaded("relics")) {
            Class<?> api = Class.forName("it.hurts.sskirillss.relics.items.relics.base.IRelicItem");
            api.getMethod("setExperience", ItemStack.class, int.class).invoke(stack.getItem(), menu.getSlot(0).getItem(), 7);
            menu.broadcastChanges();
            helper.assertTrue((int)api.getMethod("getExperience", ItemStack.class).invoke(stack.getItem(),
                    player.getItemBySlot(EquipmentSlot.FEET)) == 7, "Native Relics data did not reach the equipped item");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = CharacterNotContainer.MOD_ID, template = TEMPLATE)
    public static void equippedCuriosUseTheirOriginalSlot(GameTestHelper helper) {
        if (ModList.get().isLoaded("curios")) CuriosTestAccess.verifyOriginalSlot(helper);
        helper.succeed();
    }

    @GameTest(templateNamespace = CharacterNotContainer.MOD_ID, template = TEMPLATE)
    public static void nearbyResearchRequiresCurrentSearchAndRange(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.create(helper);
        BlockPos pos = player.blockPosition().offset(1, 0, 0);
        helper.getLevel().setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        Container chest = (Container)helper.getLevel().getBlockEntity(pos);
        ItemStack boots = new ItemStack(Items.IRON_BOOTS);
        chest.setItem(0, boots);
        ArmorStand stand = new ArmorStand(helper.getLevel(), player.getX() + 2, player.getY(), player.getZ());
        stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        helper.getLevel().addFreshEntity(stand);
        boolean previous = CharacterConfigManager.general().enableNearbyEquipmentSources;
        CharacterConfigManager.general().enableNearbyEquipmentSources = true;
        try {
            var equipment = VanillaEquipmentTarget.resolve(player, target(EquipmentChangePayload.SourceKind.UNEQUIP, -1, 0)).orElseThrow();
            var response = NearbyEquipmentSources.search(player, new NearbyEquipmentRequestPayload(17,
                    EquipmentChangePayload.TargetSystem.VANILLA, "feet", 0, false), equipment);
            for (var candidate : response.entries()) {
                var source = NearbyEquipmentSources.researchSource(player,
                        target(EquipmentChangePayload.SourceKind.NEARBY, candidate.token(), 17)).orElseThrow();
                RelicResearchMenu menu = new RelicResearchMenu(1, source);
                ItemStack live = candidate.stack().is(Items.IRON_BOOTS) ? chest.getItem(0) : stand.getItemBySlot(EquipmentSlot.FEET);
                helper.assertTrue(menu.getSlot(0).getItem() == live, "Nearby source returned a copy");
                menu.getSlot(0).getItem().setHoverName(Component.literal("Researched"));
                menu.broadcastChanges();
                helper.assertTrue(live.hasCustomHoverName(), "Nearby changes were lost");
                double x = player.getX();
                player.setPos(x + 100, player.getY(), player.getZ());
                helper.assertTrue(!menu.stillValid(player) && menu.getSlot(0).getItem().isEmpty(), "Out-of-range research stayed accessible");
                player.setPos(x, player.getY(), player.getZ());
            }
            helper.assertTrue(response.entries().size() >= 2, "Chest and armor stand were not both tested");
            helper.assertTrue(NearbyEquipmentSources.researchSource(player,
                    target(EquipmentChangePayload.SourceKind.NEARBY, 0, 18)).isEmpty(), "Stale search token was accepted");
        } finally {
            CharacterConfigManager.general().enableNearbyEquipmentSources = previous;
            NearbyEquipmentSources.clear(player);
        }
        helper.succeed();
    }

    private static EquipmentChangePayload target(EquipmentChangePayload.SourceKind kind, int index, int searchId) {
        return new EquipmentChangePayload(EquipmentChangePayload.TargetSystem.VANILLA, "feet", 0, false, kind, index, searchId);
    }

    // Keep Curios-typed lambda signatures out of the class Forge scans for test methods.
    private static final class CuriosTestAccess {
        private static void verifyOriginalSlot(GameTestHelper helper) {
            ServerPlayer player = GameTestPlayers.create(helper);
            var curios = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            if (curios.getCurios().values().stream().noneMatch(handler -> handler.getSlots() > 0)) {
                var slots = new java.util.LinkedHashMap<>(curios.getCurios());
                slots.put("curio", new top.theillusivec4.curios.common.inventory.CurioStacksHandler(
                        curios, "curio", 1, true, true, true,
                        top.theillusivec4.curios.api.type.capability.ICurio.DropRule.DEFAULT));
                curios.setCurios(slots);
            }
            var entry = curios.getCurios().entrySet().stream().filter(value -> value.getValue().getSlots() > 0).findFirst().orElseThrow();
            ItemStack stack = new ItemStack(Items.DIAMOND);
            entry.getValue().getStacks().setStackInSlot(0, stack);
            var request = new EquipmentChangePayload(EquipmentChangePayload.TargetSystem.CURIOS, entry.getKey(), 0,
                    false, EquipmentChangePayload.SourceKind.UNEQUIP, -1, 0);
            RelicResearchMenu menu = new RelicResearchMenu(1, CuriosEquipmentMutator.resolve(player, request)
                    .orElseThrow().researchSource().orElseThrow());
            helper.assertTrue(menu.getSlot(0).getItem() == stack, "Curios research used a presentation copy");
            menu.getSlot(0).getItem().setHoverName(Component.literal("Researched"));
            menu.broadcastChanges();
            helper.assertTrue(entry.getValue().getStacks().getStackInSlot(0).hasCustomHoverName(), "Curios changes were lost");
            helper.assertTrue(menu.stillValid(player), "Curios save invalidated its own research slot");
        }
    }
}
