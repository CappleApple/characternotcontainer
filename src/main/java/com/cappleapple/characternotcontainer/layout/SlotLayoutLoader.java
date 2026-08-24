package com.cappleapple.characternotcontainer.layout;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SlotLayoutLoader {
    public static final String RESOURCE_PATH = "assets/characternotcontainer/ui/slots.json";
    public static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(CharacterNotContainer.MOD_ID).resolve("slots.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private SlotLayoutLoader() {}

    public static SlotLayoutSpec load() {
        SlotLayoutSpec spec;
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            spec = Files.notExists(CONFIG_PATH) ? bundled() : configured();
            spec.validate();
        } catch (IOException | RuntimeException exception) {
            CharacterNotContainer.LOGGER.error("Could not load slots.json; restoring the bundled slot anchors", exception);
            spec = bundledUnchecked();
        }

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            GSON.toJson(spec, writer);
        } catch (IOException exception) {
            CharacterNotContainer.LOGGER.error("Could not save normalized slots.json", exception);
        }
        return spec;
    }

    private static SlotLayoutSpec configured() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            SlotLayoutSpec spec = GSON.fromJson(reader, SlotLayoutSpec.class);
            return spec == null ? SlotLayoutSpec.defaults() : spec;
        }
    }

    static SlotLayoutSpec bundled() throws IOException {
        try (InputStream input = bundledStream(); Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            SlotLayoutSpec spec = GSON.fromJson(reader, SlotLayoutSpec.class);
            if (spec == null) throw new IOException("Bundled slot layout is empty");
            spec.validate();
            return spec;
        }
    }

    private static SlotLayoutSpec bundledUnchecked() {
        try {
            return bundled();
        } catch (IOException | RuntimeException fallbackFailure) {
            CharacterNotContainer.LOGGER.error("Bundled character-screen slot anchors are unavailable", fallbackFailure);
            return SlotLayoutSpec.defaults();
        }
    }

    private static InputStream bundledStream() throws IOException {
        InputStream input = SlotLayoutLoader.class.getClassLoader().getResourceAsStream(RESOURCE_PATH);
        if (input == null) throw new IOException("Missing " + RESOURCE_PATH);
        return input;
    }
}
