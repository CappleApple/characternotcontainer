package com.cappleapple.characternotcontainer.command;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CharacterCommands {
    private CharacterCommands() {}

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("characterui")
                .then(Commands.literal("attributes").executes(context -> dumpAttributes(context.getSource())))
                .then(Commands.literal("reload").requires(source -> source.hasPermission(2)).executes(context -> reload(context.getSource()))));
    }

    private static int dumpAttributes(CommandSourceStack source) {
        var ids = BuiltInRegistries.ATTRIBUTE.keySet().stream().sorted().toList();
        CharacterNotContainer.LOGGER.info("Registered attributes ({}):", ids.size());
        ids.forEach(id -> CharacterNotContainer.LOGGER.info("  {}", id));
        source.sendSuccess(() -> Component.literal("Logged " + ids.size() + " registered attribute IDs."), false);
        return ids.size();
    }

    private static int reload(CommandSourceStack source) {
        CharacterConfigManager.load();
        source.sendSuccess(() -> Component.literal("Reloaded Character Not Container configuration."), false);
        return 1;
    }
}
