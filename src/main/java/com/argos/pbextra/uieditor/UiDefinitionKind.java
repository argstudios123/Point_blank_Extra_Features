package com.argos.pbextra.uieditor;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;


public enum UiDefinitionKind {

    AMMO("ammo", "Ammo UI", "", false, List.of(
            "bottom_right",
            "bottom_left",
            "top_right",
            "top_left"
    )),

    HEAT("heat", "Heat UI", "heat", true, List.of(
            "BOTTOM_RIGHT",
            "BOTTOM_LEFT",
            "TOP_RIGHT",
            "TOP_LEFT",
            "LEFT_OF_HOTBAR",
            "RIGHT_OF_HOTBAR"
    ));

    private final String id;
    private final String displayName;
    private final String outputSubDirectory;
    private final boolean uppercaseLocation;
    private final List<String> locations;

    UiDefinitionKind(String id, String displayName, String outputSubDirectory,
                     boolean uppercaseLocation, List<String> locations) {
        this.id = id;
        this.displayName = displayName;
        this.outputSubDirectory = outputSubDirectory;
        this.uppercaseLocation = uppercaseLocation;
        this.locations = locations;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean uppercaseLocation() {
        return uppercaseLocation;
    }

    /**
     * Locations accepted by Point Blank for this kind. Point Blank upper-cases
     * the JSON value before resolving the enum, so the casing written here is
     * cosmetic - but the addon keeps the casing the content packs use so the
     * generated files look like hand written ones.
     */
    public List<String> locations() {
        return locations;
    }

    public String normalizeLocation(String raw) {
        if (raw == null) {
            return locations.get(0);
        }
        String trimmed = raw.trim();
        for (String location : locations) {
            if (location.equalsIgnoreCase(trimmed)) {
                return location;
            }
        }
        return uppercaseLocation
                ? trimmed.toUpperCase(Locale.ROOT)
                : trimmed.toLowerCase(Locale.ROOT);
    }

    public String nextLocation(String current) {
        String normalized = normalizeLocation(current);
        int index = locations.indexOf(normalized);
        return locations.get((index + 1) % locations.size());
    }

    /**
     * @param base the {@code generated_ui_jsons} directory
     * @return the file this kind is written to for the given definition id
     */
    public Path outputPath(Path base, String definitionId) {
        Path directory = outputSubDirectory.isEmpty() ? base : base.resolve(outputSubDirectory);
        return directory.resolve(definitionId + ".json");
    }

    public Path outputDirectory(Path base) {
        return outputSubDirectory.isEmpty() ? base : base.resolve(outputSubDirectory);
    }

    public static UiDefinitionKind byId(String id) {
        if (id != null) {
            for (UiDefinitionKind kind : values()) {
                if (kind.id.equalsIgnoreCase(id)) {
                    return kind;
                }
            }
        }
        return AMMO;
    }
}
