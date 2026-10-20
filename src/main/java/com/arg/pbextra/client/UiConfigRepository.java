package com.arg.pbextra.client;

import com.arg.pbextra.PointBlankExtraFeatures;
import com.arg.pbextra.uieditor.GeneratedUiStore;
import com.arg.pbextra.uieditor.UiDefinitionKind;
import com.arg.pbextra.uieditor.UiEditorModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vicmatskiv.pointblank.registry.AmmoUiDefinition;
import com.vicmatskiv.pointblank.registry.AmmoUiRegistry;
import com.vicmatskiv.pointblank.registry.HeatUiDefinition;
import com.vicmatskiv.pointblank.registry.HeatUiRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public final class UiConfigRepository {


    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_.\\-]+");

    private UiConfigRepository() {
    }

    public static boolean isValidId(String id) {
        return id != null && ID_PATTERN.matcher(id.trim()).matches();
    }

    public static String defaultId(UiDefinitionKind kind) {
        List<String> ids = ids(kind);
        if (ids.contains("default")) {
            return "default";
        }
        return ids.isEmpty() ? "default" : ids.get(0);
    }


    public static List<String> ids(UiDefinitionKind kind) {
        Set<String> ids = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try {
            if (kind == UiDefinitionKind.AMMO) {
                for (AmmoUiDefinition definition : AmmoUiRegistry.values()) {
                    ids.add(definition.id());
                }
            } else {
                for (HeatUiDefinition definition : HeatUiRegistry.values()) {
                    ids.add(definition.id());
                }
            }
        } catch (RuntimeException exception) {
            PointBlankExtraFeatures.LOGGER.error("Failed to list registered {} definitions",
                    kind.displayName(), exception);
        }
        ids.addAll(GeneratedUiStore.overrideIds(kind));
        return new ArrayList<>(ids);
    }

    public static boolean hasOverride(UiDefinitionKind kind, String id) {
        return GeneratedUiStore.hasOverride(kind, id);
    }

    public static Path pathOf(UiDefinitionKind kind, String id) {
        return GeneratedUiStore.fileFor(kind, id);
    }


    public static UiEditorModel open(UiDefinitionKind kind, String id) {
        String resolved = id == null || id.isBlank() ? defaultId(kind) : id.trim();
        Path file = GeneratedUiStore.fileFor(kind, resolved);
        JsonObject json = null;
        try {
            json = GeneratedUiStore.readJson(file);
        } catch (IOException | RuntimeException exception) {
            PointBlankExtraFeatures.LOGGER.error(
                    "Failed to read generated UI override {}", file, exception);
        }
        if (json == null) {
            json = readContentPackJson(kind, resolved);
        }
        if (json != null) {
            return UiEditorModel.fromJson(kind, resolved, json, file);
        }
        UiEditorModel fromRegistry = fromRegistry(kind, resolved);
        if (fromRegistry != null) {
            return fromRegistry;
        }
        return UiEditorModel.createDefault(kind, resolved);
    }


    private static JsonObject readContentPackJson(UiDefinitionKind kind, String id) {
        Identifier location = contentPackLocation(kind, id);
        if (location == null) {
            return null;
        }
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) {
                return null;
            }
            try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException | RuntimeException exception) {
            // Not fatal: the registry copy below is still usable, it just loses the
            // original texture path spelling and unknown keys.
            return null;
        }
    }

    private static Identifier contentPackLocation(UiDefinitionKind kind, String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String path = (kind == UiDefinitionKind.HEAT ? "ui/heat/" : "ui/") + id + ".json";
        try {
            return Identifier.tryParse("pointblank:" + path);
        } catch (RuntimeException exception) {
            return null;
        }
    }


    public static String save(UiEditorModel model) {
        JsonObject json = model.toJson();
        String error = GeneratedUiStore.apply(model.kind(), model.id(), json);
        if (error != null) {
            return error;
        }
        try {
            Path file = GeneratedUiStore.write(model.kind(), model.id(), json);
            model.markSaved(file);
            PointBlankExtraFeatures.LOGGER.info("Saved {} override '{}' to {}",
                    model.kind().displayName(), model.id(), file);
            return null;
        } catch (IOException exception) {
            PointBlankExtraFeatures.LOGGER.error("Failed to save UI override {}", model.id(), exception);
            return "could not write the file: " + exception.getMessage();
        }
    }

    /**
     * @return the names of the Point Blank content packs in the game directory that
     *         use the normal {@code assets/pointblank} layout
     */
    public static List<String> contentPackNames() {
        Path root = contentPackRoot();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(root)) {
            return stream.filter(Files::isDirectory)
                    .filter(path -> Files.isDirectory(path.resolve("assets/pointblank")))
                    .map(path -> path.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException exception) {
            PointBlankExtraFeatures.LOGGER.error(
                    "Failed to list Point Blank content packs in {}", root, exception);
            return List.of();
        }
    }

    public static Path gameDirectory() {
        return FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
    }

    /** The directory Point Blank scans for content packs. */
    public static Path pointBlankRoot() {
        return contentPackRoot();
    }

    /** The canonical generated override directory for one UI kind. */
    public static Path generatedDirectory(UiDefinitionKind kind) {
        return kind.outputDirectory(GeneratedUiStore.rootDirectory()).toAbsolutePath().normalize();
    }

    /**
     * The canonical UI directory of an existing content pack, matching the
     * location Point Blank loads definitions from.
     */
    public static Path contentPackDirectory(UiDefinitionKind kind, String packName) {
        if (!isSafePackName(packName)) {
            return null;
        }
        Path root = contentPackRoot();
        Path pack = root.resolve(packName).normalize();
        if (!pack.startsWith(root) || pack.equals(root) || !Files.isDirectory(pack)) {
            return null;
        }
        Path base = pack.resolve("assets/pointblank");
        if (!Files.isDirectory(base)) {
            return null;
        }
        return kind.outputDirectory(base.resolve("ui")).toAbsolutePath().normalize();
    }

    /** Human readable path, relative to the game directory when possible. */
    public static String displayPath(Path path) {
        if (path == null) {
            return "(none)";
        }
        Path absolute = path.toAbsolutePath().normalize();
        Path game = gameDirectory();
        String text;
        if (absolute.startsWith(game)) {
            Path relative = game.relativize(absolute);
            text = relative.toString();
            if (text.isEmpty()) {
                text = ".";
            }
        } else {
            text = absolute.toString();
        }
        return text.replace('\\', '/');
    }

    public static Path contentPackFile(UiDefinitionKind kind, String packName, String id) {
        if (!isValidId(id)) {
            return null;
        }
        Path directory = contentPackDirectory(kind, packName);
        return directory == null ? null : directory.resolve(id.trim() + ".json");
    }

    /**
     * Accepts the generated root or its per-kind directory and returns the
     * canonical output directory, or {@code null} for any other folder.
     */
    public static Path normalizeGeneratedDestination(UiDefinitionKind kind, Path selected) {
        if (selected == null) {
            return null;
        }
        Path root = GeneratedUiStore.rootDirectory().toAbsolutePath().normalize();
        Path expected = generatedDirectory(kind);
        Path path = selected.toAbsolutePath().normalize();
        if (path.equals(expected)) {
            return expected;
        }
        if (path.equals(root) && kind == UiDefinitionKind.HEAT) {
            return expected;
        }
        return null;
    }

    /**
     * Accepts a pack root, {@code assets/pointblank}, or the canonical UI
     * directory of an existing pack and returns the canonical output directory.
     * Anything deeper or outside {@code <gameDir>/pointblank} is rejected.
     */
    public static Path normalizeContentPackDestination(UiDefinitionKind kind, Path selected) {
        if (selected == null) {
            return null;
        }
        Path root = contentPackRoot();
        Path path = selected.toAbsolutePath().normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            return null;
        }
        Path relative = root.relativize(path);
        if (relative.getNameCount() < 1) {
            return null;
        }
        String packName = relative.getName(0).toString();
        Path directory = contentPackDirectory(kind, packName);
        if (directory == null) {
            return null;
        }
        Path pack = root.resolve(packName).normalize();
        Path ui = pack.resolve("assets/pointblank/ui").toAbsolutePath().normalize();
        Path base = pack.resolve("assets/pointblank").toAbsolutePath().normalize();
        if (path.equals(directory) || path.equals(ui) || path.equals(base) || path.equals(pack)) {
            return directory;
        }
        return null;
    }

    /** @return the content pack name containing a canonical UI directory */
    public static String contentPackNameOf(Path destination) {
        if (destination == null) {
            return null;
        }
        Path root = contentPackRoot();
        Path path = destination.toAbsolutePath().normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            return null;
        }
        Path relative = root.relativize(path);
        return relative.getNameCount() == 0 ? null : relative.getName(0).toString();
    }

    /** Writes a validated definition into a selected generated directory. */
    public static String saveToGeneratedDirectory(UiEditorModel model, Path directory) {
        Path normalized = normalizeGeneratedDestination(model.kind(), directory);
        if (normalized == null) {
            return "the selected folder is not a valid generated UI destination";
        }
        return writeDefinition(model, normalized.resolve(model.id() + ".json"));
    }

    /** Writes a validated definition into a selected content pack directory. */
    public static String saveToContentPackDirectory(UiEditorModel model, Path directory) {
        Path normalized = normalizeContentPackDestination(model.kind(), directory);
        if (normalized == null) {
            return "the selected folder is not a valid Point Blank content pack destination";
        }
        return writeDefinition(model, normalized.resolve(model.id() + ".json"));
    }

    private static String writeDefinition(UiEditorModel model, Path target) {
        if (!isValidId(model.id())) {
            return "the definition id is not a valid file name";
        }
        JsonObject json = model.toJson();
        String error = GeneratedUiStore.apply(model.kind(), model.id(), json);
        if (error != null) {
            return error;
        }
        try {
            GeneratedUiStore.writeJson(target, json);
            model.markSaved(target);
            PointBlankExtraFeatures.LOGGER.info("Saved {} definition '{}' to {}",
                    model.kind().displayName(), model.id(), target);
            return null;
        } catch (IOException exception) {
            PointBlankExtraFeatures.LOGGER.error(
                    "Failed to save UI definition '{}' to {}", model.id(), target, exception);
            return "could not write the file: " + exception.getMessage();
        }
    }

    /**
     * Serializes the model, validates it with Point Blank's own parser (which also
     * refreshes the live HUD) and writes it into the selected content pack.
     */
    public static String saveToContentPack(UiEditorModel model, String packName, String id) {
        Path target = contentPackFile(model.kind(), packName, id);
        if (target == null) {
            return "the selected content pack is missing or does not use the Point Blank layout";
        }
        JsonObject json = model.toJson();
        String error = GeneratedUiStore.apply(model.kind(), id, json);
        if (error != null) {
            return error;
        }
        try {
            GeneratedUiStore.writeJson(target, json);
            model.markSaved(target);
            PointBlankExtraFeatures.LOGGER.info("Saved {} definition '{}' to content pack '{}' ({})",
                    model.kind().displayName(), id, packName, target);
            return null;
        } catch (IOException exception) {
            PointBlankExtraFeatures.LOGGER.error(
                    "Failed to save UI definition '{}' to content pack '{}'", id, packName, exception);
            return "could not write the file: " + exception.getMessage();
        }
    }

    /**
     * Converts a PNG selected on disk into the resource path stored in a Point
     * Blank UI JSON. The file must live under {@code assets/<namespace>/textures};
     * Point Blank resolves such paths relative to {@code assets/pointblank} (or
     * uses the explicit namespace prefix).
     *
     * @return the resource path, or {@code null} when the file is not inside a
     *         Minecraft resource directory
     */
    public static String textureResourcePath(Path textureFile) {
        if (textureFile == null) {
            return null;
        }
        Path file = textureFile.toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)
                || !file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
            return null;
        }
        Path current = file.getParent();
        while (current != null) {
            Path name = current.getFileName();
            if (name != null && name.toString().equals("assets")) {
                Path relative = current.relativize(file);
                if (relative.getNameCount() < 3
                        || !relative.getName(1).toString().equals("textures")) {
                    return null;
                }
                String namespace = relative.getName(0).toString();
                String path = relative.subpath(1, relative.getNameCount())
                        .toString().replace('\\', '/');
                Identifier identifier = Identifier.tryBuild(namespace, path);
                if (identifier == null) {
                    return null;
                }
                return namespace.equals("pointblank") ? path : namespace + ":" + path;
            }
            current = current.getParent();
        }
        return null;
    }

    private static Path contentPackRoot() {
        return FMLPaths.GAMEDIR.get().resolve("pointblank").normalize();
    }

    private static boolean isSafePackName(String packName) {
        return packName != null && !packName.isBlank()
                && !packName.equals(".") && !packName.equals("..")
                && packName.indexOf('/') < 0 && packName.indexOf('\\') < 0
                && packName.indexOf(':') < 0 && packName.indexOf('\0') < 0;
    }

    private static UiEditorModel fromRegistry(UiDefinitionKind kind, String id) {
        try {
            if (kind == UiDefinitionKind.AMMO) {
                for (AmmoUiDefinition definition : AmmoUiRegistry.values()) {
                    if (definition.id().equals(id)) {
                        return UiEditorModel.fromAmmoDefinition(definition);
                    }
                }
            } else {
                for (HeatUiDefinition definition : HeatUiRegistry.values()) {
                    if (definition.id().equals(id)) {
                        return UiEditorModel.fromHeatDefinition(definition);
                    }
                }
            }
        } catch (RuntimeException exception) {
            PointBlankExtraFeatures.LOGGER.error(
                    "Failed to copy {} definition '{}' from the registry", kind.displayName(), id, exception);
        }
        return null;
    }
}
