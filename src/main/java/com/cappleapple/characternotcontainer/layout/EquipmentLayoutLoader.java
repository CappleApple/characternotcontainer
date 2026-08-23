package com.cappleapple.characternotcontainer.layout;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public final class EquipmentLayoutLoader {
    public static final String RESOURCE_PATH = "assets/characternotcontainer/ui/equipment_screen.json";
    public static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(CharacterNotContainer.MOD_ID).resolve("equipment_screen.json");
    private static final Gson GSON = new GsonBuilder().create();

    private EquipmentLayoutLoader() {}

    public static EquipmentScreenSpec load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.notExists(CONFIG_PATH)) {
                try (InputStream input = bundledStream()) {
                    Files.copy(input, CONFIG_PATH);
                }
            }
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                EquipmentScreenSpec spec = GSON.fromJson(reader, EquipmentScreenSpec.class);
                validate(spec);
                return spec;
            }
        } catch (IOException | RuntimeException exception) {
            CharacterNotContainer.LOGGER.error("Could not load equipment_screen.json; using the bundled layout", exception);
            try (InputStream input = bundledStream(); Reader reader = new java.io.InputStreamReader(input, StandardCharsets.UTF_8)) {
                EquipmentScreenSpec spec = GSON.fromJson(reader, EquipmentScreenSpec.class);
                validate(spec);
                return spec;
            } catch (IOException fallbackFailure) {
                throw new IllegalStateException("Bundled character screen layout is unavailable", fallbackFailure);
            }
        }
    }

    private static InputStream bundledStream() throws IOException {
        InputStream input = EquipmentLayoutLoader.class.getClassLoader().getResourceAsStream(RESOURCE_PATH);
        if (input == null) throw new IOException("Missing " + RESOURCE_PATH);
        return input;
    }

    private static void validate(EquipmentScreenSpec spec) {
        if (spec == null || spec.width <= 0 || spec.height <= 0) throw new IllegalArgumentException("Invalid screen dimensions");
        spec.widget("screen_background");
        spec.widget("stats_list");
        spec.widget("player_preview_reference");
        spec.widget("equipment_picker_overlay_reference");
        for (String id : java.util.List.of("equip_hitbox_head", "equip_hitbox_chest", "equip_hitbox_legs", "equip_hitbox_feet")) {
            spec.widget(id);
        }
    }
}
