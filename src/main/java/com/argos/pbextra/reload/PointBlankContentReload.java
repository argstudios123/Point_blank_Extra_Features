package com.argos.pbextra.reload;

import com.argos.pbextra.PointBlankExtraFeatures;
import com.argos.pbextra.uieditor.GeneratedUiStore;
import com.vicmatskiv.pointblank.registry.AmmoUiDefinition;
import com.vicmatskiv.pointblank.registry.AmmoUiRegistry;
import com.vicmatskiv.pointblank.registry.HeatUiDefinition;
import com.vicmatskiv.pointblank.registry.HeatUiRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public final class PointBlankContentReload {

    private static final String NAMESPACE = "pointblank";
    /** Ammo UI definitions live in {@code assets/pointblank/ui}. */
    private static final String UI_DIRECTORY = "ui";
    /** Custom Heat UI definitions live in {@code assets/pointblank/ui/heat}. */
    private static final String HEAT_DIRECTORY = "ui/heat/";

    private static final int MAX_DETAIL_ENTRIES = 3;
    private static final int MAX_ENTRY_LENGTH = 160;

    private PointBlankContentReload() {
    }


    public static String rediscoverExtensionPacks() {
        try {
            Object mod = ModList.get().getModObjectById("pointblank").orElse(null);
            if (mod == null) {
                return "the pointblank mod instance is not available";
            }
            Field registryField = mod.getClass().getDeclaredField("extensionRegistry");
            registryField.setAccessible(true);
            Object registry = registryField.get(mod);
            if (registry == null) {
                return "the pointblank extension registry is not initialised";
            }
            // discoverExtensions(PackType) is public API and rebuilds the
            // "pointblank_resources" pack from <gameDir>/pointblank/; both pack
            // types are re-scanned because Point Blank shares a single pack between
            // them and does exactly the same at startup.
            Method discover = registry.getClass().getMethod("discoverExtensions", PackType.class);
            discover.invoke(registry, PackType.CLIENT_RESOURCES);
            discover.invoke(registry, PackType.SERVER_DATA);
            return null;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            return warnRediscovery(cause);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return warnRediscovery(exception);
        }
    }

    private static String warnRediscovery(Throwable throwable) {
        String message = "could not re-scan <gameDir>/pointblank (" + describe(throwable) + ")";
        PointBlankExtraFeatures.LOGGER.warn(message);
        return message;
    }

    /**
     * Re-registers every Point Blank UI definition the given (freshly reloaded)
     * resource manager can see.
     *
     * @param resources the resource manager of the side that owns the registries -
     *                  the client's, because the HUD is rendered there
     */
    public static Result reload(ResourceManager resources) {
        Map<ResourceLocation, Resource> found = resources.listResources(UI_DIRECTORY,
                PointBlankContentReload::isUiDefinition);

        Map<String, AmmoUiDefinition> ammo = new LinkedHashMap<>();
        Map<String, HeatUiDefinition> heat = new LinkedHashMap<>();
        Map<String, String> ammoFailures = new LinkedHashMap<>();
        Map<String, String> heatFailures = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();

        List<ResourceLocation> locations = new ArrayList<>(found.keySet());
        locations.sort(Comparator.comparing(ResourceLocation::toString));

        for (ResourceLocation location : locations) {
            String path = location.getPath();
            boolean heatEntry = path.startsWith(HEAT_DIRECTORY);
            String stem = path.substring((heatEntry ? HEAT_DIRECTORY : UI_DIRECTORY + "/").length());
            if (stem.indexOf('/') >= 0) {
                // Point Blank only globs "*.json" in those two levels.
                skipped.add(path + " (Point Blank does not scan nested folders)");
                continue;
            }
            String id = stem.substring(0, stem.length() - ".json".length());
            try (Reader reader = new InputStreamReader(found.get(location).open(), StandardCharsets.UTF_8)) {
                if (heatEntry) {
                    heat.put(id, HeatUiDefinition.fromReader(reader, id));
                } else {
                    ammo.put(id, AmmoUiDefinition.fromReader(reader, id));
                }
            } catch (IOException | RuntimeException exception) {
                (heatEntry ? heatFailures : ammoFailures).put(id, path + ": " + describe(exception));
            }
        }

        if (found.isEmpty()) {
            // Never wipe a working registry just because the resource manager did
            // not expose the pack (for example on a dedicated server, where the
            // extension pack only serves the "data" directory).
            PointBlankExtraFeatures.LOGGER.warn(
                    "No assets/pointblank/ui definitions were visible to the {} resource manager; "
                            + "Point Blank UI registries were left unchanged",
                    resources.getClass().getSimpleName());
            return new Result(0, 0, 0, List.of(), skipped, true);
        }

        List<String> failures = new ArrayList<>();
        mergeFailures(ammoFailures, AmmoUiRegistry.values(), ammo, failures, AmmoUiDefinition::id);
        mergeFailures(heatFailures, HeatUiRegistry.values(), heat, failures, HeatUiDefinition::id);

        // Commit, mirroring Point Blank's own registerItemsFromExtensions()
        // ordering: heat keeps its code built "default", and the editor's
        // generated overrides are applied last so they keep winning.
        AmmoUiRegistry.clear();
        for (AmmoUiDefinition definition : ammo.values()) {
            AmmoUiRegistry.register(definition);
        }
        HeatUiRegistry.reset();
        for (HeatUiDefinition definition : heat.values()) {
            HeatUiRegistry.register(definition);
        }
        int overrides;
        try {
            // The editor's own overrides are applied last so they keep winning over
            // content pack definitions, exactly like at startup.
            overrides = GeneratedUiStore.loadOverrides();
        } catch (RuntimeException exception) {
            // The content pack definitions above are already committed; a failure
            // here only means the generated overrides could not be re-applied.
            overrides = 0;
            failures.add("generated_ui_jsons/: " + describe(exception)
                    + " (content pack definitions were still reloaded)");
        }

        PointBlankExtraFeatures.LOGGER.info(
                "Point Blank content reload: {} ammo UI, {} heat UI definition(s), {} generated override(s), {} failure(s), {} skipped",
                ammo.size(), heat.size(), overrides, failures.size(), skipped.size());
        for (String failure : failures) {
            PointBlankExtraFeatures.LOGGER.error("Point Blank content reload failure: {}", failure);
        }
        return new Result(ammo.size(), heat.size(), overrides, failures, skipped, false);
    }

    /**
     * Files that failed to parse keep the definition that is already registered
     * (when there is one) instead of dropping it, and still show up as a failure.
     */
    private static <T> void mergeFailures(Map<String, String> failuresById,
                                          Collection<T> registered,
                                          Map<String, T> parsed,
                                          List<String> failures,
                                          Function<T, String> idOf) {
        Map<String, T> previous = new LinkedHashMap<>();
        for (T definition : registered) {
            previous.put(idOf.apply(definition), definition);
        }
        for (Map.Entry<String, String> failure : failuresById.entrySet()) {
            T previousDefinition = previous.get(failure.getKey());
            if (previousDefinition == null) {
                failures.add(failure.getValue());
            } else {
                parsed.put(failure.getKey(), previousDefinition);
                failures.add(failure.getValue() + " (kept the previously loaded definition)");
            }
        }
    }

    private static boolean isUiDefinition(ResourceLocation location) {
        String path = location.getPath();
        return NAMESPACE.equals(location.getNamespace())
                && path.startsWith(UI_DIRECTORY + "/")
                && path.endsWith(".json");
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

    /** Outcome of one reload, with enough detail to build command feedback. */
    public record Result(int ammoLoaded, int heatLoaded, int overrides,
                         List<String> failures, List<String> skipped,
                         boolean registriesUntouched) {

        public boolean hasProblems() {
            return !failures.isEmpty() || !skipped.isEmpty() || registriesUntouched;
        }

        /** One line summary, safe for chat. */
        public String summary() {
            StringBuilder text = new StringBuilder("Point Blank content reload: ");
            if (registriesUntouched) {
                text.append("no assets/pointblank/ui definitions were visible to the resource manager, ")
                        .append("so the UI registries were left unchanged");
            } else {
                text.append(ammoLoaded).append(" ammo UI and ").append(heatLoaded)
                        .append(" heat UI definition(s) registered");
                if (overrides > 0) {
                    text.append(", ").append(overrides).append(" generated override(s) re-applied");
                }
            }
            if (!failures.isEmpty()) {
                text.append("; ").append(failures.size()).append(" file(s) failed");
            }
            if (!skipped.isEmpty()) {
                text.append("; ").append(skipped.size()).append(" file(s) skipped");
            }
            return text.append('.').toString();
        }

        /** The first few problems, in detail. Empty when the reload was clean. */
        public String details() {
            List<String> lines = new ArrayList<>(failures);
            lines.addAll(skipped);
            if (lines.isEmpty()) {
                return "";
            }
            List<String> shown = new ArrayList<>();
            for (String line : lines.subList(0, Math.min(MAX_DETAIL_ENTRIES, lines.size()))) {
                shown.add(line.length() <= MAX_ENTRY_LENGTH
                        ? line : line.substring(0, MAX_ENTRY_LENGTH) + "...");
            }
            if (lines.size() > shown.size()) {
                shown.add("and " + (lines.size() - shown.size()) + " more (see the log)");
            }
            return String.join(" | ", shown);
        }
    }
}
