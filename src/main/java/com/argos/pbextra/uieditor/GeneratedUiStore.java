package com.argos.pbextra.uieditor;

import com.argos.pbextra.PointBlankExtraFeatures;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vicmatskiv.pointblank.registry.AmmoUiDefinition;
import com.vicmatskiv.pointblank.registry.AmmoUiRegistry;
import com.vicmatskiv.pointblank.registry.HeatUiDefinition;
import com.vicmatskiv.pointblank.registry.HeatUiRegistry;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

// AUthur : ARG Studios
public final class GeneratedUiStore {
    public static final String DIRECTORY_NAME = "generated_ui_jsons";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private GeneratedUiStore() {
    }

    public static Path rootDirectory() {
        return FMLPaths.GAMEDIR.get().resolve(DIRECTORY_NAME);
    }


    public static Path write(UiDefinitionKind kind, String id, JsonObject json) throws IOException {
        Path base = rootDirectory();
        Path directory = kind.outputDirectory(base);
        Files.createDirectories(directory);
        Path target = kind.outputPath(base, id);
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
        return target;
    }


    public static String apply(UiDefinitionKind kind, String id, String json) {
        try {
            if (kind == UiDefinitionKind.AMMO) {
                AmmoUiRegistry.register(AmmoUiDefinition.fromReader(new StringReader(json), id));
            } else {
                HeatUiRegistry.register(HeatUiDefinition.fromReader(new StringReader(json), id));
            }
            return null;
        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            return message != null ? message : exception.toString();
        }
    }

    public static String apply(UiDefinitionKind kind, String id, JsonObject json) {
        return apply(kind, id, GSON.toJson(json));
    }

// Loads the jsons
    public static int loadOverrides() {
        int loaded = 0;
        Path base = rootDirectory();
        for (UiDefinitionKind kind : UiDefinitionKind.values()) {
            Path directory = kind.outputDirectory(base);
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String fileName = file.getFileName().toString();
                    if (!fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
                        continue;
                    }
                    String id = fileName.substring(0, fileName.length() - ".json".length());
                    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        String error = apply(kind, id, readAll(reader));
                        if (error == null) {
                            loaded++;
                            PointBlankExtraFeatures.LOGGER.info(
                                    "Loaded generated {} override '{}' from {}", kind.displayName(), id, file);
                        } else {
                            PointBlankExtraFeatures.LOGGER.error(
                                    "Failed to load generated {} override '{}': {}", kind.displayName(), id, error);
                        }
                    } catch (IOException exception) {
                        PointBlankExtraFeatures.LOGGER.error("Failed to read generated UI override {}", file, exception);
                    }
                }
            } catch (IOException exception) {
                PointBlankExtraFeatures.LOGGER.error(
                        "Failed to list generated UI overrides in {}", directory, exception);
            }
        }
        return loaded;
    }

    // he file a definition id would be saved to, whether it exists or not
    public static Path fileFor(UiDefinitionKind kind, String id) {
        return kind.outputPath(rootDirectory(), id);
    }


    public static JsonObject readJson(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    public static boolean hasOverride(UiDefinitionKind kind, String id) {
        return Files.isRegularFile(fileFor(kind, id));
    }

    public static List<String> overrideIds(UiDefinitionKind kind) {
        Path directory = kind.outputDirectory(rootDirectory());
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.toLowerCase(Locale.ROOT).endsWith(".json"))
                    .map(fileName -> fileName.substring(0, fileName.length() - ".json".length()))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException exception) {
            PointBlankExtraFeatures.LOGGER.error("Failed to list generated UI overrides in {}", directory, exception);
            return List.of();
        }
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
            builder.append(buffer, 0, read);
        }
        return builder.toString();
    }
}
