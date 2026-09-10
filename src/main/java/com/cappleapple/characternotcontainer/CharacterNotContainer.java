package com.cappleapple.characternotcontainer;

import com.cappleapple.characternotcontainer.compat.relics.RelicResearchMenu;
import com.cappleapple.characternotcontainer.command.CharacterCommands;
import com.cappleapple.characternotcontainer.compat.needsnotnecessities.NeedsNotNecessitiesSourceBridge;
import com.cappleapple.characternotcontainer.compat.puffishskills.PufferfishSkillsSourceBridge;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import com.cappleapple.characternotcontainer.network.EquipmentNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

@Mod(CharacterNotContainer.MOD_ID)

public final class CharacterNotContainer {
    public static final String MOD_ID = "characternotcontainer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CharacterNotContainer() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CharacterConfigManager.load();
        RelicResearchMenu.MENUS.register(modBus);
        EquipmentNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(CharacterCommands::register);
        MinecraftForge.EVENT_BUS.addListener(EquipmentNetwork::playerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(NeedsNotNecessitiesSourceBridge::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(PufferfishSkillsSourceBridge::serverStarted);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
