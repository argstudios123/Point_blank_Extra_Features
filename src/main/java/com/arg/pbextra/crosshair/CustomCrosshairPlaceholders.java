package com.arg.pbextra.crosshair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;



public final class CustomCrosshairPlaceholders {

    public static final String PLACEHOLDER_FILE_NAME = "custom_crosshair.png";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BUNDLED_PLACEHOLDER =
            "assets/pointblankextra/textures/custom_crosshair/custom_crosshair.png";
    private static final List<String> ITEM_DIRECTORIES = List.of(
            "assets/pointblank/items", "items");

    private CustomCrosshairPlaceholders() {
    }


    public static void installBundled() {
        try {
            byte[] placeholder = readBundledPlaceholder();
            if (placeholder == null) {
                return;
            }
            int created = install(FMLPaths.GAMEDIR.get().resolve("pointblank"), placeholder);
            if (created > 0) {
                LOGGER.info("Point Blank Extra Features created {} custom crosshair "
                        + "placeholder(s) in Point Blank extension packs", created);
            }
        } catch (RuntimeException exception) {
            // Placeholder installation is a convenience and must never prevent
            // the client from starting.
            LOGGER.warn("Could not install custom crosshair placeholders: {}",
                    exception.getMessage());
        }
    }


    public static int install(Path extensionsRoot, byte[] placeholderPng) {
        if (extensionsRoot == null || placeholderPng == null
                || !Files.isDirectory(extensionsRoot)) {
            return 0;
        }

        int created = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsRoot)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    created += installForPack(entry, placeholderPng);
                }
                // ZIP/JAR packs cannot be written safely; leave them untouched.
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not scan Point Blank extension packs for custom crosshair "
                    + "placeholders: {}", exception.getMessage());
        }
        return created;
    }

    private static int installForPack(Path pack, byte[] placeholderPng) {
        Set<Path> targets = new LinkedHashSet<>();
        for (String itemDirectory : ITEM_DIRECTORIES) {
            Path items = pack.resolve(itemDirectory);
            if (!Files.isDirectory(items)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(items, 2)) {
                for (Path json : walk.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString()
                                .toLowerCase(Locale.ROOT).endsWith(".json"))
                        .toList()) {
                    String texture = findPlaceholderTexture(json);
                    if (texture == null) {
                        continue;
                    }
                    Path target = resolveTarget(pack, texture);
                    if (target != null) {
                        targets.add(target);
                    }
                }
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Could not scan Point Blank item JSON in '{}': {}",
                        pack, exception.getMessage());
            }
        }

        int created = 0;
        for (Path target : targets) {
            if (Files.exists(target)) {
                continue;
            }
            try {
                Path parent = target.getParent();
                if (parent == null) {
                    continue;
                }
                Files.createDirectories(parent);
                Files.write(target, placeholderPng,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                LOGGER.info("Created custom crosshair placeholder '{}'", target);
                created++;
            } catch (FileAlreadyExistsException exception) {
                // A creator or another process created it first: never overwrite.
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Could not create custom crosshair placeholder '{}': {}",
                        target, exception.getMessage());
            }
        }
        return created;
    }

    /**
     * Resolves the exact file for a feature texture. Only paths whose file name
     * is {@code custom_crosshair.png} and that stay inside the pack's
     * {@code assets} directory are accepted.
     */
    static Path resolveTarget(Path pack, String texture) {
        Identifier id = CustomCrosshairPaths.parse(texture);
        if (id == null || !PLACEHOLDER_FILE_NAME.equals(CustomCrosshairPaths.fileName(id))) {
            return null;
        }

        // Point Blank extension packs expose only the "pointblank" namespace
        // (ExtensionRegistry$2#getNamespaces). Writing another namespace here
        // would create a file the extension pack cannot resolve.
        if (!CustomCrosshairPaths.DEFAULT_NAMESPACE.equals(id.getNamespace())) {
            return null;
        }

        Path assets = pack.resolve("assets").toAbsolutePath().normalize();
        Path target = assets.resolve(id.getNamespace()).resolve(id.getPath()).normalize();
        if (target.equals(assets) || !target.startsWith(assets)) {
            return null;
        }
        return target;
    }


    private static String findPlaceholderTexture(Path json) {
        try (Reader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return findPlaceholderTexture(root);
        } catch (Exception exception) {
            // Not a weapon definition or malformed JSON: leave it alone.
            return null;
        }
    }

    private static String findPlaceholderTexture(JsonObject root) {
        if (root == null) {
            return null;
        }

        if (root.has("features") && root.get("features").isJsonArray()) {
            JsonArray features = root.getAsJsonArray("features");
            for (JsonElement element : features) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject feature = element.getAsJsonObject();
                if (!isCustomCrosshair(feature)) {
                    continue;
                }
                CustomCrosshairConfig config = CustomCrosshairConfig.fromJson(feature);
                if (config.getLayout() == CustomCrosshairLayout.POINTBLANK) {
                    // The native atlas is 39x39; do not create a complete-layout
                    // 64x64 placeholder at that path. The native template is
                    // copied explicitly by the creator.
                    continue;
                }
                String texture = config.getTexture();
                if (isPlaceholderPath(texture)) {
                    return texture;
                }
            }
        }

        JsonElement rootFeature = root.get(CustomCrosshairConfig.SETTINGS_KEY_CAMEL);
        if (rootFeature == null) {
            rootFeature = root.get(CustomCrosshairConfig.SETTINGS_KEY);
        }
        if (rootFeature != null && rootFeature.isJsonObject()) {
            CustomCrosshairConfig config = CustomCrosshairConfig.fromJson(
                    rootFeature.getAsJsonObject());
            if (config.getLayout() == CustomCrosshairLayout.POINTBLANK) {
                return null;
            }
            String texture = config.getTexture();
            if (isPlaceholderPath(texture)) {
                return texture;
            }
        }
        return null;
    }

    private static boolean isCustomCrosshair(JsonObject feature) {
        if (!feature.has("type") || !feature.get("type").isJsonPrimitive()) {
            return false;
        }
        return CustomCrosshairConfig.FEATURE_TYPE.equalsIgnoreCase(
                feature.get("type").getAsString());
    }

    private static boolean isPlaceholderPath(String texture) {
        Identifier id = CustomCrosshairPaths.parse(texture);
        return id != null && PLACEHOLDER_FILE_NAME.equals(CustomCrosshairPaths.fileName(id));
    }

    /** The editable placeholder bundled in this mod, or {@code null} if absent. */
    public static byte[] readBundledPlaceholder() {
        try (InputStream stream = CustomCrosshairPlaceholders.class.getClassLoader()
                .getResourceAsStream(BUNDLED_PLACEHOLDER)) {
            if (stream == null) {
                LOGGER.warn("Bundled custom crosshair placeholder '{}' is missing",
                        BUNDLED_PLACEHOLDER);
                return null;
            }
            return stream.readAllBytes();
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not read the bundled custom crosshair placeholder: {}",
                    exception.getMessage());
            return null;
        }
    }
}

