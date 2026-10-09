package com.argos.pbextra.uieditor;

import com.argos.pbextra.client.UiConfigRepository;
import com.argos.pbextra.integration.PointBlankJsonIntegration;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public final class UiDefinitionIndex {

    /** Ammo UI definitions live in {@code assets/pointblank/ui}. */
    private static final String AMMO_DIRECTORY = "assets/pointblank/ui";
    /** Custom Heat UI definitions live in the {@code heat} sub directory. */
    private static final String HEAT_DIRECTORY = "assets/pointblank/ui/heat";


    public record Entry(UiDefinitionKind kind, String id, String source, JsonObject json) {
    }

    private UiDefinitionIndex() {
    }

    /**
     * Collects every definition of one kind. Intended to be called once when the
     * search dialog opens; filtering is then done in memory per keystroke.
     */
    public static List<Entry> load(UiDefinitionKind kind) {
        List<Entry> entries = new ArrayList<>();
        collectPackEntries(kind, entries);
        collectOverrideEntries(kind, entries);
        collectRegistryOnlyEntries(kind, entries);
        entries.sort(Comparator.comparing(Entry::id, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Entry::source, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    private static void collectPackEntries(UiDefinitionKind kind, List<Entry> entries) {
        // Each scan is rooted at the directory Point Blank itself scans for that
        // kind, so the relative path of a definition is simply "<id>.json" (a nested
        // path means the file is in a sub directory Point Blank does not glob).
        String directory = kind == UiDefinitionKind.HEAT ? HEAT_DIRECTORY : AMMO_DIRECTORY;
        for (PointBlankJsonIntegration.PackJson resource
                : PointBlankJsonIntegration.scanJsonResources(extensionsRoot(), directory)) {
            String id = stem(resource.relativePath());
            if (id == null || id.isEmpty()) {
                continue;
            }
            String source = resource.source();
            if (UiConfigRepository.hasOverride(kind, id)) {
                source = source + "  [generated override active]";
            }
            entries.add(new Entry(kind, id, source, resource.content()));
        }
    }

    private static void collectOverrideEntries(UiDefinitionKind kind, List<Entry> entries) {
        for (String id : GeneratedUiStore.overrideIds(kind)) {
            try {
                JsonObject json = GeneratedUiStore.readJson(GeneratedUiStore.fileFor(kind, id));
                if (json != null) {
                    entries.add(new Entry(kind, id, GeneratedUiStore.DIRECTORY_NAME, json));
                }
            } catch (Exception e) {
                // A malformed override is simply not searchable; opening the id
                // directly still reports the problem to the user.
            }
        }
    }

    private static void collectRegistryOnlyEntries(UiDefinitionKind kind, List<Entry> entries) {
        Set<String> known = new LinkedHashSet<>();
        for (Entry entry : entries) {
            known.add(entry.id());
        }
        for (String id : UiConfigRepository.ids(kind)) {
            if (!known.contains(id)) {
                entries.add(new Entry(kind, id, "registered definition", null));
            }
        }
    }

    /**
     * @param query matched case insensitively against the definition id and its
     *              {@code "name"}; a blank query returns everything
     */
    public static List<Entry> filter(List<Entry> entries, String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return entries;
        }
        List<Entry> matches = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.id().toLowerCase(Locale.ROOT).contains(needle) || nameOf(entry).contains(needle)) {
                matches.add(entry);
            }
        }
        return matches;
    }

    private static String nameOf(Entry entry) {
        JsonObject json = entry.json();
        if (json != null && json.has("name") && json.get("name").isJsonPrimitive()) {
            return json.get("name").getAsString().toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static Path extensionsRoot() {
        return FMLPaths.GAMEDIR.get().resolve("pointblank");
    }

    private static String stem(String relativePath) {
        if (relativePath.indexOf('/') >= 0
                || !relativePath.toLowerCase(Locale.ROOT).endsWith(".json")) {
            return null;
        }
        return relativePath.substring(0, relativePath.length() - ".json".length());
    }
}

