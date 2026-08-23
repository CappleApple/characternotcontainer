package com.cappleapple.characternotcontainer.config;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CharacterConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path DIRECTORY = FMLPaths.CONFIGDIR.get().resolve(CharacterNotContainer.MOD_ID);
    private static volatile GeneralConfig general = new GeneralConfig();
    private static volatile StatsConfig stats = StatsConfig.defaults();
    private static volatile long revision;

    private CharacterConfigManager() {}

    public static synchronized void load() {
        try {
            Files.createDirectories(DIRECTORY);
            general = loadOrCreate(DIRECTORY.resolve("general.json"), GeneralConfig.class, new GeneralConfig());
            stats = loadOrCreate(DIRECTORY.resolve("stats.json"), StatsConfig.class, StatsConfig.defaults());
            sanitize();
            revision++;
        } catch (IOException | RuntimeException exception) {
            CharacterNotContainer.LOGGER.error("Could not load character UI configuration; using safe defaults", exception);
            general = new GeneralConfig();
            stats = StatsConfig.defaults();
            revision++;
        }
    }

    private static <T> T loadOrCreate(Path path, Class<T> type, T defaults) throws IOException {
        if (Files.notExists(path)) {
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(defaults, writer);
            }
            return defaults;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            T loaded = GSON.fromJson(reader, type);
            return loaded == null ? defaults : loaded;
        }
    }

    private static void sanitize() {
        general.nearbyEquipmentSearchRadius = Math.max(0.0D, Math.min(32.0D, general.nearbyEquipmentSearchRadius));
        general.equipmentPickerColumns = Math.max(1, Math.min(12, general.equipmentPickerColumns));
        general.equipmentPickerVisibleRows = Math.max(1, Math.min(12, general.equipmentPickerVisibleRows));
        if (stats.categories == null) stats.categories = StatsConfig.defaults().categories;
        stats.categories.removeIf(category -> category == null || category.stats == null);
        stats.categories.forEach(category -> category.stats.removeIf(stat -> stat == null || stat.attribute == null || stat.attribute.isBlank()));
        stats.categories.sort(java.util.Comparator.comparingInt(category -> category.order));
    }

    public static GeneralConfig general() { return general; }
    public static StatsConfig stats() { return stats; }
    public static Path directory() { return DIRECTORY; }
    public static long revision() { return revision; }
}
