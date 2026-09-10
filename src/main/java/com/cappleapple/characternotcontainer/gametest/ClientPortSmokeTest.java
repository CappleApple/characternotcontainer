package com.cappleapple.characternotcontainer.gametest;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.client.CharacterEquipmentScreen;
import com.cappleapple.characternotcontainer.network.EquipmentChangePayload;
import com.cappleapple.characternotcontainer.network.EquipmentNetwork;
import com.cappleapple.characternotcontainer.network.RelicResearchPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Opt-in, development-only rendered-client gate. This entire package is excluded from release JARs. */
@Mod.EventBusSubscriber(modid = CharacterNotContainer.MOD_ID, value = Dist.CLIENT)
public final class ClientPortSmokeTest {
    private static int stage;
    private static int ticks;
    private static CharacterEquipmentScreen character;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("characternotcontainer.clientSmoke") || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (++ticks > 20 * 180) throw new IllegalStateException("Forge client smoke test timed out at stage " + stage);
        if (stage == 0 && mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) {
            mc.setScreen(new TitleScreen());
        }
        if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            stage = 1;
            mc.createWorldOpenFlows().createFreshLevel("forge-port-smoke-" + System.currentTimeMillis(),
                    new LevelSettings("Forge port smoke", GameType.SURVIVAL, false, Difficulty.PEACEFUL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(1L, false, false), WorldPresets::createNormalWorldDimensions);
        } else if (stage == 1 && mc.player != null && mc.screen == null && mc.hasSingleplayerServer()) {
            stage = 2; ticks = 0;
            var id = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_BOOTS));
                player.inventoryMenu.broadcastChanges();
            });
        } else if (stage == 2 && ticks > 60) {
            if (!EquipmentNetwork.CHANNEL.isRemotePresent(mc.getConnection().getConnection())) {
                throw new IllegalStateException("Forge equipment channel was not negotiated");
            }
            character = new CharacterEquipmentScreen(mc.player);
            mc.setScreen(character);
            EquipmentNetwork.sendToServer(new EquipmentChangePayload(EquipmentChangePayload.TargetSystem.VANILLA,
                    "feet", 0, false, EquipmentChangePayload.SourceKind.PLAYER_INVENTORY, 0, 0));
            stage = 3; ticks = 0;
        } else if (stage == 3 && ticks > 80) {
            if (!mc.player.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS)) {
                throw new IllegalStateException("Forge equipment packet did not equip the inventory item");
            }
            Screenshot.grab(mc.gameDirectory, "forge-character.png", mc.getMainRenderTarget(), message -> {});
            CharacterNotContainer.LOGGER.info("CLIENT_SMOKE: rendered character and verified server-authoritative equipment packet");
            if (ModList.get().isLoaded("relics")) {
                var id = mc.player.getUUID();
                mc.getSingleplayerServer().execute(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    player.setItemSlot(EquipmentSlot.FEET,
                            new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("relics:roller_skates"))));
                    player.inventoryMenu.broadcastChanges();
                });
                stage = 4; ticks = 0;
            } else { stage = 7; ticks = 0; }
        } else if (stage == 4 && ticks > 40) {
            EquipmentNetwork.sendToServer(new RelicResearchPayload(new EquipmentChangePayload(
                    EquipmentChangePayload.TargetSystem.VANILLA, "feet", 0, false,
                    EquipmentChangePayload.SourceKind.UNEQUIP, -1, 0), mc.player.getItemBySlot(EquipmentSlot.FEET)));
            stage = 5; ticks = 0;
        } else if (stage == 5 && ticks > 80) {
            if (mc.screen == null || !mc.screen.getClass().getName().endsWith("RelicDescriptionScreen")) {
                throw new IllegalStateException("Relics research screen did not open: " + mc.screen);
            }
            Screenshot.grab(mc.gameDirectory, "forge-relic-research.png", mc.getMainRenderTarget(), message -> {});
            mc.screen.onClose();
            stage = 6; ticks = 0;
        } else if (stage == 6 && ticks > 40) {
            if (mc.screen != character || mc.player.containerMenu != mc.player.inventoryMenu) {
                throw new IllegalStateException("Relics did not return to the character screen and close its server menu");
            }
            CharacterNotContainer.LOGGER.info("CLIENT_SMOKE: rendered native Relics research and verified return/menu cleanup");
            stage = 7; ticks = 0;
        } else if (stage == 7 && ticks > 40) {
            CharacterNotContainer.LOGGER.info("CLIENT_SMOKE: PASS");
            stage = 8;
            mc.stop();
        }
    }
}
