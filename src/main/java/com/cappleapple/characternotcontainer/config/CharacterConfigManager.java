package com.cappleapple.characternotcontainer.config;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

public final class CharacterConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path DIRECTORY = FMLPaths.CONFIGDIR.get().resolve(CharacterNotContainer.MOD_ID);
    private static final Path GENERAL_PATH = DIRECTORY.resolve("general.json");
    private static final Path STATS_PATH = DIRECTORY.resolve("stats.json");
    private static volatile GeneralConfig general = new GeneralConfig();
    private static volatile StatsConfig stats = StatsConfig.defaults();
    private static volatile long revision;
    private static final Map<String, String> LEGACY_DEFAULT_NAMES = Map.ofEntries(
            Map.entry("minecraft:generic.attack_damage", "Attack Damage"),
            Map.entry("minecraft:generic.attack_speed", "Attack Speed"),
            Map.entry("minecraft:generic.attack_knockback", "Attack Knockback"),
            Map.entry("minecraft:generic.armor", "Armor"),
            Map.entry("minecraft:generic.armor_toughness", "Armor Toughness"),
            Map.entry("minecraft:generic.knockback_resistance", "Knockback Resistance"),
            Map.entry("minecraft:generic.max_health", "Max Health"),
            Map.entry("minecraft:generic.movement_speed", "Movement Speed"),
            Map.entry("minecraft:generic.flying_speed", "Flying Speed"),
            Map.entry("minecraft:generic.jump_strength", "Jump Strength"),
            Map.entry("minecraft:generic.step_height", "Step Height"),
            Map.entry("minecraft:player.block_interaction_range", "Block Reach"),
            Map.entry("minecraft:player.entity_interaction_range", "Entity Reach"),
            Map.entry("minecraft:generic.luck", "Luck"));

    private CharacterConfigManager() {}

    public static synchronized void load() {
        try {
            Files.createDirectories(DIRECTORY);
            general = loadOrCreate(GENERAL_PATH, GeneralConfig.class, new GeneralConfig());
            stats = loadStats();
            sanitizeGeneral();
            sanitizeStats(stats);
            write(GENERAL_PATH, general);
            write(STATS_PATH, stats); // Migrates and removes deprecated fields.
            revision++;
        } catch (IOException | RuntimeException exception) {
            CharacterNotContainer.LOGGER.error("Could not load character UI configuration; using safe defaults", exception);
            general = new GeneralConfig();
            stats = StatsConfig.defaults();
            revision++;
        }
    }

    public static synchronized boolean mergeDiscoveredAttributes(Collection<String> attributeIds) {
        Set<String> known = new HashSet<>();
        stats.attributes.forEach(stat -> known.add(stat.attribute));
        List<String> additions = attributeIds.stream()
                .filter(id -> id != null && !id.isBlank() && known.add(id))
                .sorted()
                .toList();
        if (additions.isEmpty()) return false;

        additions.stream().map(StatDefinition::new).forEach(stats.attributes::add);
        sanitizeStats(stats);
        try {
            write(STATS_PATH, stats);
            revision++;
            return true;
        } catch (IOException exception) {
            CharacterNotContainer.LOGGER.error("Could not save automatically discovered attributes", exception);
            return false;
        }
    }

    private static StatsConfig loadStats() throws IOException {
        if (Files.notExists(STATS_PATH)) return StatsConfig.defaults();
        try (Reader reader = Files.newBufferedReader(STATS_PATH)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) return StatsConfig.defaults();
            JsonObject root = parsed.getAsJsonObject();
            JsonArray flat = new JsonArray();
            boolean legacy = root.has("categories") && root.get("categories").isJsonArray();
            if (legacy) {
                for (JsonElement categoryElement : root.getAsJsonArray("categories")) {
                    if (!categoryElement.isJsonObject()) continue;
                    JsonElement oldStats = categoryElement.getAsJsonObject().get("stats");
                    if (oldStats != null && oldStats.isJsonArray()) oldStats.getAsJsonArray().forEach(flat::add);
                }
            } else if (root.has("attributes") && root.get("attributes").isJsonArray()) {
                root.getAsJsonArray("attributes").forEach(flat::add);
            }

            StatsConfig result = new StatsConfig();
            for (JsonElement element : flat) {
                if (!element.isJsonObject()) continue;
                StatDefinition definition = GSON.fromJson(normalizeStatJson(element.getAsJsonObject()), StatDefinition.class);
                if (definition != null) result.attributes.add(definition);
            }
            return result.attributes.isEmpty() ? StatsConfig.defaults() : result;
        }
    }

    static JsonObject normalizeStatJson(JsonObject old) {
        JsonObject normalized = new JsonObject();
        copy(old, normalized, "attribute");
        copy(old, normalized, "visible");
        copyNonBlank(old, normalized, "name");
        copyNonBlank(old, normalized, "icon");
        copy(old, normalized, "decimalPlaces");
        if (old.has("scale") && old.get("scale").isJsonPrimitive()
                && Math.abs(old.get("scale").getAsDouble() - 1.0D) > 0.0000001D) {
            copy(old, normalized, "scale");
        }
        copyNonBlank(old, normalized, "suffix");
        if (normalized.has("attribute") && normalized.has("name")) {
            String attribute = normalized.get("attribute").getAsString();
            if (normalized.get("name").getAsString().equals(LEGACY_DEFAULT_NAMES.get(attribute))) normalized.remove("name");
        }
        if (old.has("format") && old.get("format").isJsonPrimitive()) {
            String format = old.get("format").getAsString();
            format = switch (format) {
                case "INTEGER", "DECIMAL", "PERCENT" -> format;
                case "PERCENT_DELTA" -> "PERCENT";
                default -> "DECIMAL";
            };
            normalized.addProperty("format", format);
        }
        return normalized;
    }

    private static void copy(JsonObject from, JsonObject to, String name) {
        if (from.has(name) && !from.get(name).isJsonNull()) to.add(name, from.get(name).deepCopy());
    }

    private static void copyNonBlank(JsonObject from, JsonObject to, String name) {
        if (from.has(name) && from.get(name).isJsonPrimitive() && !from.get(name).getAsString().isBlank()) {
            to.addProperty(name, from.get(name).getAsString());
        }
    }

    private static <T> T loadOrCreate(Path path, Class<T> type, T defaults) throws IOException {
        if (Files.notExists(path)) {
            write(path, defaults);
            return defaults;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            T loaded = GSON.fromJson(reader, type);
            return loaded == null ? defaults : loaded;
        }
    }

    private static void write(Path path, Object value) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(value, writer);
        }
    }

    private static void sanitizeGeneral() {
        general.nearbyEquipmentSearchRadius = Math.max(0.0D, Math.min(32.0D, general.nearbyEquipmentSearchRadius));
        general.equipmentPickerColumns = Math.max(1, Math.min(12, general.equipmentPickerColumns));
        general.equipmentPickerVisibleRows = Math.max(1, Math.min(12, general.equipmentPickerVisibleRows));
    }

    private static void sanitizeStats(StatsConfig config) {
        if (config.attributes == null) config.attributes = StatsConfig.defaults().attributes;
        Set<String> seen = new HashSet<>();
        config.attributes.removeIf(stat -> stat == null || stat.attribute == null || stat.attribute.isBlank()
                || !seen.add(stat.attribute));
        config.attributes.forEach(stat -> {
            if (stat.name != null && stat.name.isBlank()) stat.name = null;
            if (stat.icon != null && stat.icon.isBlank()) stat.icon = null;
            if (stat.suffix != null && stat.suffix.isBlank()) stat.suffix = null;
            if (stat.decimalPlaces != null) stat.decimalPlaces = Math.max(0, Math.min(6, stat.decimalPlaces));
            if (stat.scale != null && !Double.isFinite(stat.scale)) stat.scale = null;
        });
    }

    public static GeneralConfig general() { return general; }
    public static StatsConfig stats() { return stats; }
    public static Path directory() { return DIRECTORY; }
    public static long revision() { return revision; }
}
