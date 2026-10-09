package com.argos.pbextra.client;

import com.argos.pbextra.PointBlankExtraFeatures;
import com.argos.pbextra.uieditor.GeneratedUiStore;
import com.argos.pbextra.uieditor.UiDefinitionKind;
import com.argos.pbextra.uieditor.UiEditorModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vicmatskiv.pointblank.registry.AmmoUiDefinition;
import com.vicmatskiv.pointblank.registry.AmmoUiRegistry;
import com.vicmatskiv.pointblank.registry.HeatUiDefinition;
import com.vicmatskiv.pointblank.registry.HeatUiRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;


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
        ResourceLocation location = contentPackLocation(kind, id);
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

    private static ResourceLocation contentPackLocation(UiDefinitionKind kind, String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String path = (kind == UiDefinitionKind.HEAT ? "ui/heat/" : "ui/") + id + ".json";
        try {
            return ResourceLocation.tryParse("pointblank:" + path);
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
