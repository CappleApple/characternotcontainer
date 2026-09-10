package com.cappleapple.characternotcontainer.testing;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Path;
import java.util.List;

/** Initializes the standalone unit-test JVM; full loader behavior is covered by GameTests. */
public final class MinecraftTestBootstrap implements BeforeAllCallback {
    private static boolean initialized;

    @Override public void beforeAll(ExtensionContext context) throws Exception {
        if (initialized) return;
        FMLPaths.loadAbsolutePaths(Path.of("build/unit-test-game").toAbsolutePath());
        ModList mods = ModList.of(List.of(), List.of());
        var setLoadedMods = ModList.class.getDeclaredMethod("setLoadedMods", List.class);
        setLoadedMods.setAccessible(true);
        setLoadedMods.invoke(mods, List.of());
        // ModLauncher normally supplies event constructors. Precompute this base
        // listener list so vanilla bootstrap can initialize Forge's network constants.
        var listenerList = net.minecraftforge.eventbus.api.EventListenerHelper.class
                .getDeclaredMethod("getListenerListInternal", Class.class, boolean.class);
        listenerList.setAccessible(true);
        listenerList.invoke(null, net.minecraftforge.network.NetworkEvent.class, true);
        for (Class<?> type : net.minecraftforge.network.NetworkEvent.class.getDeclaredClasses()) {
            if (net.minecraftforge.eventbus.api.Event.class.isAssignableFrom(type)) seedListenerList(listenerList, type);
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        initialized = true;
    }
    private static void seedListenerList(java.lang.reflect.Method method, Class<?> type) throws Exception {
        if (type == net.minecraftforge.eventbus.api.Event.class) return;
        seedListenerList(method, type.getSuperclass());
        method.invoke(null, type, true);
    }
}
