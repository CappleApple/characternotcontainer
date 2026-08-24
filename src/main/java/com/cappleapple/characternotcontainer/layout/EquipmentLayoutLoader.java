package com.cappleapple.characternotcontainer.layout;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EquipmentLayoutLoader {
    public static final String RESOURCE_PATH = "assets/characternotcontainer/ui/equipment_screen.json";
    public static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(CharacterNotContainer.MOD_ID).resolve("equipment_screen.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private EquipmentLayoutLoader() {}

    public static EquipmentScreenSpec load() {
        EquipmentScreenSpec spec;
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            spec = Files.notExists(CONFIG_PATH) ? bundled() : readConfigured();
            spec.validate();
        } catch (IOException | RuntimeException exception) {
            CharacterNotContainer.LOGGER.error("Could not load equipment_screen.json; restoring the bundled Curios slot bindings", exception);
            spec = bundledUnchecked();
        }

        // Always normalize the file. This migrates the old UI-builder document
        // and removes any deprecated or unknown screen fields.
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            GSON.toJson(spec, writer);
        } catch (IOException exception) {
            CharacterNotContainer.LOGGER.error("Could not save normalized equipment_screen.json", exception);
        }
        return spec;
    }

    private static EquipmentScreenSpec readConfigured() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return decode(root);
        }
    }

    static EquipmentScreenSpec decode(JsonObject root) {
        if (root == null || root.has("widgets") || !root.has("slots")) return EquipmentScreenSpec.defaults();
        EquipmentScreenSpec spec = GSON.fromJson(root, EquipmentScreenSpec.class);
        return spec == null ? EquipmentScreenSpec.defaults() : spec;
    }

    private static EquipmentScreenSpec bundled() throws IOException {
        try (InputStream input = bundledStream(); Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            EquipmentScreenSpec spec = GSON.fromJson(reader, EquipmentScreenSpec.class);
            if (spec == null) throw new IOException("Bundled layout is empty");
            spec.validate();
            return spec;
        }
    }

    private static EquipmentScreenSpec bundledUnchecked() {
        try {
            return bundled();
        } catch (IOException | RuntimeException fallbackFailure) {
            CharacterNotContainer.LOGGER.error("Bundled character-screen Curios bindings are unavailable", fallbackFailure);
            return EquipmentScreenSpec.defaults();
        }
    }

    private static InputStream bundledStream() throws IOException {
        InputStream input = EquipmentLayoutLoader.class.getClassLoader().getResourceAsStream(RESOURCE_PATH);
        if (input == null) throw new IOException("Missing " + RESOURCE_PATH);
        return input;
    }
}
