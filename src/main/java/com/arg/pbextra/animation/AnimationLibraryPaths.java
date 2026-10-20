package com.arg.pbextra.animation;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Pure parsing and path helpers for reusable first-person animation libraries.
 * <p>
 * Nothing in this class touches Minecraft classes, so the JSON and path rules
 * can be unit-tested without a game runtime.
 */
public final class AnimationLibraryPaths {

    /** The Point Blank weapon JSON key handled by this feature. */
    public static final String KEY = "firstPersonFallbackAnimations";

    /** Point Blank hardcodes this namespace for the native string form. */
    public static final String DEFAULT_NAMESPACE = "pointblank";

    /** Optional content-pack root for reusable animation libraries. */
    public static final String CUSTOM_LIBRARY_ROOT = "default_animations";

    /** Point Blank's dual-wield animation-name suffixes. */
    public static final String DUAL_WIELD_LEFT_SUFFIX = "_dwleft";
    public static final String DUAL_WIELD_RIGHT_SUFFIX = "_dwright";

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH_PATTERN = Pattern.compile("[a-z0-9/._-]+");

    private AnimationLibraryPaths() {
    }


    /**
     * Reads every declared fallback for diagnostics. A string is treated as the
     * native single fallback; an array is treated as the reusable library form.
     */
    public static List<String> parseDeclared(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString().trim();
            return value.isEmpty() ? List.of() : List.of(value);
        }
        if (element.isJsonArray()) {
            return parseArray(element);
        }
        return List.of();
    }


    /** Reads only the array entries used by the additive reusable-library extension. */
    public static List<String> parseAdditional(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonArray()) {
            return List.of();
        }
        return parseArray(element);
    }


    /**
     * Normalises a JSON value to {@code namespace:path}.
     * Relative values get the {@code pointblank} namespace, exactly like the
     * native Point Blank string form. Returns {@code null} for invalid values.
     */
    public static String normalizeIdentifier(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        int colon = trimmed.indexOf(':');
        String namespace;
        String path;
        if (colon >= 0) {
            if (trimmed.indexOf(':', colon + 1) >= 0) {
                return null;
            }
            namespace = trimmed.substring(0, colon);
            path = trimmed.substring(colon + 1);
        } else {
            namespace = DEFAULT_NAMESPACE;
            path = trimmed;
        }

        if (!NAMESPACE_PATTERN.matcher(namespace).matches()
                || !PATH_PATTERN.matcher(path).matches()) {
            return null;
        }

        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return null;
            }
        }

        return namespace + ":" + path;
    }


    /** Returns true when the name is a Point Blank dual-wield variant. */
    public static boolean isDualWieldAnimationName(String name) {
        return name != null
                && (name.endsWith(DUAL_WIELD_LEFT_SUFFIX)
                || name.endsWith(DUAL_WIELD_RIGHT_SUFFIX));
    }


    /**
     * Returns the unsuffixed base name for a dual-wield animation, or
     * {@code null} when the name does not carry a dual-wield suffix.
     */
    public static String stripDualWieldSuffix(String name) {
        if (name == null) {
            return null;
        }
        if (name.endsWith(DUAL_WIELD_LEFT_SUFFIX)) {
            return name.substring(0, name.length() - DUAL_WIELD_LEFT_SUFFIX.length());
        }
        if (name.endsWith(DUAL_WIELD_RIGHT_SUFFIX)) {
            return name.substring(0, name.length() - DUAL_WIELD_RIGHT_SUFFIX.length());
        }
        return null;
    }


    /**
     * Converts a JSON entry to its identifier inside the optional
     * {@code default_animations/} root. Entries may either omit the root
     * (for example {@code walking_animations}) or include it (for example
     * {@code default_animations/pistol_anims/aw_pistol_dualwield}). The root
     * is never doubled.
     */
    public static String customLibraryPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return path.startsWith(CUSTOM_LIBRARY_ROOT + "/")
                ? path
                : CUSTOM_LIBRARY_ROOT + "/" + path;
    }


    /** Full {@code namespace:default_animations/path} identifier, or null. */
    public static String customLibraryIdentifier(String value) {
        String normalized = normalizeIdentifier(value);
        if (normalized == null) {
            return null;
        }

        int colon = normalized.indexOf(':');
        String namespace = normalized.substring(0, colon);
        String path = normalized.substring(colon + 1);
        return namespace + ":" + customLibraryPath(path);
    }


    /**
     * Traditional {@code namespace:item/path} animation resource identifier for
     * a configured library, or {@code null} when the entry already targets the
     * optional {@code default_animations/} root.
     * <p>
     * Point Blank's fallback list stores animation <em>resource</em> identifiers,
     * not file paths: {@code DefaultedItemGeoModel.buildFormattedAnimationPath}
     * prefixes the {@code item/} subtype, so {@code walking_animations} becomes
     * {@code pointblank:item/walking_animations}. An entry such as
     * {@code default_animations/pistol_anims/aw_pistol_dualwield_lefthand} can
     * never exist under {@code animations/item/}, so emitting
     * {@code pointblank:item/default_animations/...} would only add a resource
     * that is guaranteed to be missing from GeckoLib's baked cache.
     */
    public static String traditionalLibraryIdentifier(String value) {
        String normalized = normalizeIdentifier(value);
        if (normalized == null) {
            return null;
        }

        int colon = normalized.indexOf(':');
        String namespace = normalized.substring(0, colon);
        String path = normalized.substring(colon + 1);
        if (path.startsWith(CUSTOM_LIBRARY_ROOT + "/")) {
            return null;
        }
        return namespace + ":item/" + path;
    }


    /**
     * Synthetic GeckoLib resource identifier for a file discovered under
     * {@code default_animations/}. GeckoLib bakes
     * {@code geckolib/animations/default_animations/...} into the cache key
     * {@code namespace:default_animations/...}, which is exactly what
     * {@link #customLibraryIdentifier(String)} returns.
     */
    public static String customAnimationTargetPath(String sourcePath) {
        if (sourcePath == null || !sourcePath.endsWith(".animation.json")) {
            return null;
        }

        String relative = sourcePath.startsWith(CUSTOM_LIBRARY_ROOT + "/")
                ? sourcePath.substring(CUSTOM_LIBRARY_ROOT.length() + 1)
                : sourcePath;
        return "geckolib/animations/" + CUSTOM_LIBRARY_ROOT + "/" + relative;
    }


    /**
     * Returns the two resource paths GeckoLib/Point Blank resolve an item
     * animation through (the modern content-pack root and the legacy built-in
     * root), plus the optional {@code default_animations} root. The first two
     * preserve existing behaviour; the last one is loaded by the addon's
     * GeckoLib resource-manager wrapper.
     */
    public static List<String> candidateAnimationResources(String identifier) {
        String normalized = normalizeIdentifier(identifier);
        if (normalized == null) {
            return List.of();
        }

        int colon = normalized.indexOf(':');
        String namespace = normalized.substring(0, colon);
        String path = normalized.substring(colon + 1);

        return List.of(
                namespace + ":animations/item/" + path + ".animation.json",
                namespace + ":geckolib/animations/item/" + path + ".animation.json",
                namespace + ":" + customLibraryPath(path) + ".animation.json"
        );
    }


    /**
     * GeckoLib cache keys that can satisfy a configured library. The
     * traditional key is checked first to preserve existing content packs;
     * the custom-directory key is checked next. Entries that already target
     * {@code default_animations/} only have the custom-directory key.
     */
    public static List<String> candidateAnimationCacheKeys(String identifier) {
        String normalized = normalizeIdentifier(identifier);
        if (normalized == null) {
            return List.of();
        }

        int colon = normalized.indexOf(':');
        String namespace = normalized.substring(0, colon);
        String path = normalized.substring(colon + 1);

        List<String> keys = new ArrayList<>(2);
        String traditional = traditionalLibraryIdentifier(normalized);
        if (traditional != null) {
            keys.add(traditional);
        }
        keys.add(namespace + ":" + customLibraryPath(path));
        return List.copyOf(keys);
    }


    private static List<String> parseArray(JsonElement element) {
        Set<String> values = new LinkedHashSet<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry == null || !entry.isJsonPrimitive()
                    || !entry.getAsJsonPrimitive().isString()) {
                continue;
            }
            String value = entry.getAsString().trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }
}
