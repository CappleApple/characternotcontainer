package com.cappleapple.characternotcontainer;

import com.cappleapple.characternotcontainer.command.CharacterCommands;
import com.cappleapple.characternotcontainer.compat.needsnotnecessities.NeedsNotNecessitiesSourceBridge;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import com.cappleapple.characternotcontainer.network.EquipmentNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(CharacterNotContainer.MOD_ID)
public final class CharacterNotContainer {
    public static final String MOD_ID = "characternotcontainer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CharacterNotContainer(IEventBus modBus, ModContainer container) {
        CharacterConfigManager.load();
        modBus.addListener(EquipmentNetwork::register);
        NeoForge.EVENT_BUS.addListener(CharacterCommands::register);
        NeoForge.EVENT_BUS.addListener(EquipmentNetwork::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(NeedsNotNecessitiesSourceBridge::serverStarted);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
