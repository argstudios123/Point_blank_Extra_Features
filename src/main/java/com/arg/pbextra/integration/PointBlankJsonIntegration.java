package com.arg.pbextra.integration;

import com.arg.pbextra.bullethole.BulletHoleConfig;
import com.arg.pbextra.data.WeaponExtraData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public final class PointBlankJsonIntegration {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ITEMS_DIR = "items";


    private static final String EXTENSION_ITEMS_DIR = "assets/pointblank/items";

    /** Optional pack-wide bullet-hole configuration, next to {@code ext.json}. */
    public static final String PACK_BULLET_HOLE_CONFIG = "bullet_holes.json";

    private PointBlankJsonIntegration() {
    }


    /**
     * Indexes every weapon JSON found in the {@code <gameDir>/pointblank} extension
     * directories and archives into {@code out}.
     *
     * @param failures receives one entry per source that could not be read, keyed by
     *                 the weapon name (or the source path when the name is unknown)
     * @return {@code true} when the extensions directory itself could be read (even
     *         if it does not exist or contains no weapons); {@code false} when the
     *         directory listing failed, in which case the caller must not treat the
     *         (possibly empty) result as authoritative
     */
    public static boolean scanExtensions(Path extensionsPath, Map<String, WeaponExtraData> out,
                                         Map<String, String> failures) {
        if (extensionsPath == null || !Files.isDirectory(extensionsPath)) {
            // No extensions directory is not an error: the pack simply contributes nothing.
            return true;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath)) {
            for (Path entry : stream) {
                try {
                    if (Files.isDirectory(entry)) {
                        BulletHoleConfig packDefaults = readPackConfig(entry);
                        // Point Blank extension layout: <extension>/assets/pointblank/items/*.json
                        scanItemsDirectory(entry.resolve(EXTENSION_ITEMS_DIR), out, failures,
                                packDefaults);
                        // Legacy layout accepted by earlier versions of this addon.
                        scanItemsDirectory(entry.resolve(ITEMS_DIR), out, failures,
                                packDefaults);
                    } else {
                        String fileName = entry.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
                            scanItemsZip(entry, out, failures, readPackConfig(entry));
                        }
                    }
                } catch (Exception e) {
                    String name = entry.getFileName().toString();
                    failures.put(name, "extension '" + name + "': " + describe(e));
                    LOGGER.warn("Could not scan Point Blank extension '{}': {}",
                            entry.getFileName(), e.getMessage());
                }
            }
            return true;
        } catch (IOException e) {
            failures.put(extensionsPath.toString(),
                    "extensions directory '" + extensionsPath + "': " + describe(e));
            LOGGER.warn("Could not list Point Blank extensions directory '{}': {}",
                    extensionsPath, e.getMessage());
            return false;
        }
    }

    /**
     * Indexes Point Blank's built-in weapon JSONs (namespace {@code pointblank},
     * path {@code items/*.json}) through the resource manager.
     *
     * @param failures receives one entry per source that could not be read, keyed by
     *                 the weapon name (or the source path when the name is unknown)
     * @return {@code true} when the lookup completed (even when it found nothing),
     *         {@code false} when the resource manager could not be queried
     */
    public static boolean scanBuiltIn(ResourceManager resourceManager, Map<String, WeaponExtraData> out,
                                      Map<String, String> failures) {
        if (resourceManager == null) {
            // No resource manager is not an error on its own.
            return true;
        }

        Map<Identifier, Resource> resources;
        try {
            resources = resourceManager.listResources(
                    ITEMS_DIR,
                    loc -> loc.getPath().startsWith(ITEMS_DIR + "/")
                            && loc.getPath().endsWith(".json")
            );
        } catch (Exception e) {
            failures.put(ITEMS_DIR, "built-in items: " + describe(e));
            LOGGER.warn("Could not list Point Blank built-in item resources: {}", e.getMessage());
            return false;
        }

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier loc = entry.getKey();
            String fallbackName = stripItemsPrefixAndExtension(loc.getPath());
            try (InputStream is = entry.getValue().open()) {
                parseWeapon(new InputStreamReader(is, StandardCharsets.UTF_8), fallbackName, out,
                        BulletHoleConfig.BUILT_IN_DEFAULTS, false);
            } catch (Exception e) {
                failures.put(fallbackName, loc + ": " + describe(e));
                LOGGER.warn("Could not read Point Blank item resource '{}': {}", loc, e.getMessage());
            }
        }
        return true;
    }

    private static void scanItemsDirectory(Path itemsDir, Map<String, WeaponExtraData> out,
                                           Map<String, String> failures,
                                           BulletHoleConfig packDefaults) {
        if (!Files.isDirectory(itemsDir)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(itemsDir, "*.json")) {
            for (Path path : stream) {
                String fallbackName = stripItemsPrefixAndExtension(path.getFileName().toString());
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    parseWeapon(reader, fallbackName, out, packDefaults, true);
                } catch (Exception e) {
                    failures.put(fallbackName, path + ": " + describe(e));
                    LOGGER.warn("Could not read Point Blank item JSON '{}': {}", path, e.getMessage());
                }
            }
        } catch (IOException e) {
            failures.put(itemsDir.toString(), "items directory '" + itemsDir + "': " + describe(e));
            LOGGER.warn("Could not list Point Blank items directory '{}': {}", itemsDir, e.getMessage());
        }
    }

    private static void scanItemsZip(Path zipPath, Map<String, WeaponExtraData> out,
                                     Map<String, String> failures,
                                     BulletHoleConfig packDefaults) {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                boolean insideItems = name.startsWith(EXTENSION_ITEMS_DIR + "/")
                        || name.startsWith(ITEMS_DIR + "/");
                if (entry.isDirectory() || !insideItems || !name.endsWith(".json")) {
                    continue;
                }
                String fallbackName = stripItemsPrefixAndExtension(name);
                try (Reader reader = new InputStreamReader(
                        zipFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                    parseWeapon(reader, fallbackName, out, packDefaults, true);
                } catch (Exception e) {
                    failures.put(fallbackName, name + " in '" + zipPath + "': " + describe(e));
                    LOGGER.warn("Could not read Point Blank item JSON '{}' in '{}': {}",
                            name, zipPath, e.getMessage());
                }
            }
        } catch (IOException e) {
            failures.put(zipPath.toString(), "extension archive '" + zipPath + "': " + describe(e));
            LOGGER.warn("Could not open Point Blank extension archive '{}': {}",
                    zipPath, e.getMessage());
        }
    }

    private static void parseWeapon(Reader reader, String fallbackName,
                                    Map<String, WeaponExtraData> out,
                                    BulletHoleConfig packDefaults,
                                    boolean overwrite) {
        JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
        String name = fallbackName;
        if (obj.has("name") && obj.get("name").isJsonPrimitive()) {
            name = obj.get("name").getAsString();
        }
        WeaponExtraData data = WeaponExtraData.fromJson(obj, packDefaults);
        if (overwrite) {
            out.put(name, data);
        } else {
            // Resource-manager entries can include extension-pack items that were
            // already scanned from disk with their pack-wide defaults. Never
            // overwrite those with built-in defaults.
            out.putIfAbsent(name, data);
        }
    }

    /**
     * Reads the optional pack-wide bullet-hole settings. Both layouts are
     * accepted, with the root file next to {@code ext.json} taking precedence:
     * <pre>
     * &lt;pack&gt;/bullet_holes.json
     * &lt;pack&gt;/assets/pointblank/bullet_holes.json
     * </pre>
     * Existing packs without such a file simply inherit the built-in defaults.
     */
    private static BulletHoleConfig readPackConfig(Path pack) {
        if (pack == null) {
            return BulletHoleConfig.BUILT_IN_DEFAULTS;
        }
        try {
            if (Files.isDirectory(pack)) {
                Path file = pack.resolve(PACK_BULLET_HOLE_CONFIG);
                if (!Files.isRegularFile(file)) {
                    file = pack.resolve("assets/pointblank/" + PACK_BULLET_HOLE_CONFIG);
                }
                if (!Files.isRegularFile(file)) {
                    return BulletHoleConfig.BUILT_IN_DEFAULTS;
                }
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    return BulletHoleConfig.fromJson(
                            JsonParser.parseReader(reader).getAsJsonObject(),
                            BulletHoleConfig.BUILT_IN_DEFAULTS);
                }
            }

            String fileName = pack.getFileName() == null
                    ? "" : pack.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!fileName.endsWith(".zip") && !fileName.endsWith(".jar")) {
                return BulletHoleConfig.BUILT_IN_DEFAULTS;
            }

            try (ZipFile zipFile = new ZipFile(pack.toFile())) {
                ZipEntry entry = zipFile.getEntry(PACK_BULLET_HOLE_CONFIG);
                if (entry == null) {
                    entry = zipFile.getEntry("assets/pointblank/" + PACK_BULLET_HOLE_CONFIG);
                }
                if (entry == null) {
                    return BulletHoleConfig.BUILT_IN_DEFAULTS;
                }
                try (Reader reader = new InputStreamReader(
                        zipFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                    return BulletHoleConfig.fromJson(
                            JsonParser.parseReader(reader).getAsJsonObject(),
                            BulletHoleConfig.BUILT_IN_DEFAULTS);
                }
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not read pack-wide bullet-hole configuration from '{}': {}",
                    pack, describe(exception));
            return BulletHoleConfig.BUILT_IN_DEFAULTS;
        }
    }


    private static String describe(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return cause.getClass().getSimpleName() + ": " + message;
    }

    private static String stripItemsPrefixAndExtension(String path) {
        String name = path;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (name.endsWith(".json")) {
            name = name.substring(0, name.length() - ".json".length());
        }
        return name;
    }

    /* ------------------------------------------------------------------ */
    /* Generic JSON resources of the extension layout                      */
    /* ------------------------------------------------------------------ */


    public record PackJson(String source, String relativePath, JsonObject content) {
    }


    public static List<PackJson> scanJsonResources(Path extensionsPath, String relativeDirectory) {
        List<PackJson> found = new ArrayList<>();
        if (extensionsPath == null || !Files.isDirectory(extensionsPath)) {
            return found;
        }
        String prefix = relativeDirectory.endsWith("/") ? relativeDirectory : relativeDirectory + "/";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath)) {
            for (Path entry : stream) {
                try {
                    if (Files.isDirectory(entry)) {
                        scanJsonDirectory(entry, prefix, entry.getFileName().toString(), found);
                    } else {
                        String fileName = entry.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
                            scanJsonArchive(entry, prefix, found);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Could not scan Point Blank extension '{}': {}",
                            entry.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not list Point Blank extensions directory '{}': {}",
                    extensionsPath, e.getMessage());
        }
        return found;
    }

    private static void scanJsonDirectory(Path packDirectory, String prefix, String source,
                                          List<PackJson> found) {
        Path directory = packDirectory.resolve(prefix);
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory, 3)) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
                    continue;
                }
                String relativePath = directory.relativize(file).toString().replace('\\', '/');
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    found.add(new PackJson(source, relativePath, JsonParser.parseReader(reader).getAsJsonObject()));
                } catch (Exception e) {
                    LOGGER.warn("Could not read Point Blank JSON '{}' in '{}': {}",
                            relativePath, packDirectory, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not walk Point Blank JSON directory '{}': {}", directory, e.getMessage());
        }
    }

    private static void scanJsonArchive(Path archive, String prefix, List<PackJson> found) {
        String source = archive.getFileName().toString();
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()
                        || !name.startsWith(prefix)
                        || !name.toLowerCase(Locale.ROOT).endsWith(".json")) {
                    continue;
                }
                String relativePath = name.substring(prefix.length());
                try (Reader reader = new InputStreamReader(
                        zipFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                    found.add(new PackJson(source, relativePath, JsonParser.parseReader(reader).getAsJsonObject()));
                } catch (Exception e) {
                    LOGGER.warn("Could not read Point Blank JSON '{}' in '{}': {}",
                            name, source, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not open Point Blank extension archive '{}': {}", archive, e.getMessage());
        }
    }
}

