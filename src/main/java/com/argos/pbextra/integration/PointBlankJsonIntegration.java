package com.argos.pbextra.integration;

import com.argos.pbextra.data.WeaponExtraData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
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

    private PointBlankJsonIntegration() {
    }


    public static void scanExtensions(Path extensionsPath, Map<String, WeaponExtraData> out) {
        if (extensionsPath == null || !Files.isDirectory(extensionsPath)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath)) {
            for (Path entry : stream) {
                try {
                    if (Files.isDirectory(entry)) {
                        // Point Blank extension layout: <extension>/assets/pointblank/items/*.json
                        scanItemsDirectory(entry.resolve(EXTENSION_ITEMS_DIR), out);
                        // Legacy layout accepted by earlier versions of this addon.
                        scanItemsDirectory(entry.resolve(ITEMS_DIR), out);
                    } else {
                        String fileName = entry.getFileName().toString().toLowerCase();
                        if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
                            scanItemsZip(entry, out);
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
    }

    /**
     * Scans Point Blank's built-in weapon JSONs (namespace {@code pointblank},
     * path {@code items/*.json}) through the resource manager.
     */
    public static void scanBuiltIn(ResourceManager resourceManager, Map<String, WeaponExtraData> out) {
        if (resourceManager == null) {
            return;
        }

        Map<ResourceLocation, Resource> resources;
        try {
            resources = resourceManager.listResources(
                    ITEMS_DIR,
                    loc -> loc.getPath().startsWith(ITEMS_DIR + "/")
                            && loc.getPath().endsWith(".json")
            );
        } catch (Exception e) {
            LOGGER.warn("Could not list Point Blank built-in item resources: {}", e.getMessage());
            return;
        }

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation loc = entry.getKey();
            try (InputStream is = entry.getValue().open()) {
                String fallbackName = stripItemsPrefixAndExtension(loc.getPath());
                parseWeapon(new InputStreamReader(is, StandardCharsets.UTF_8), fallbackName, out);
            } catch (Exception e) {
                LOGGER.warn("Could not read Point Blank item resource '{}': {}", loc, e.getMessage());
            }
        }
    }

    private static void scanItemsDirectory(Path itemsDir, Map<String, WeaponExtraData> out) {
        if (!Files.isDirectory(itemsDir)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(itemsDir, "*.json")) {
            for (Path path : stream) {
                String fallbackName = stripItemsPrefixAndExtension(path.getFileName().toString());
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    parseWeapon(reader, fallbackName, out);
                } catch (Exception e) {
                    LOGGER.warn("Could not read Point Blank item JSON '{}': {}", path, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not list Point Blank items directory '{}': {}", itemsDir, e.getMessage());
        }
    }

    private static void scanItemsZip(Path zipPath, Map<String, WeaponExtraData> out) {
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
                    parseWeapon(reader, fallbackName, out);
                } catch (Exception e) {
                    LOGGER.warn("Could not read Point Blank item JSON '{}' in '{}': {}",
                            name, zipPath, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not open Point Blank extension archive '{}': {}",
                    zipPath, e.getMessage());
        }
    }

    private static void parseWeapon(Reader reader, String fallbackName,
                                    Map<String, WeaponExtraData> out) {
        JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
        String name = fallbackName;
        if (obj.has("name") && obj.get("name").isJsonPrimitive()) {
            name = obj.get("name").getAsString();
        }
        out.put(name, WeaponExtraData.fromJson(obj));
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

