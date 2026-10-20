package com.arg.pbextra.crosshair;

import net.minecraft.resources.Identifier;

import java.util.Locale;



public final class CustomCrosshairPaths {

    public static final String DEFAULT_NAMESPACE = "pointblank";

    private CustomCrosshairPaths() {
    }

    /** Returns a validated texture id, or {@code null} for an invalid path. */
    public static Identifier parse(String raw) {
        if (raw == null) {
            return null;
        }

        String value = raw.trim().replace('\\', '/');
        if (value.isEmpty() || value.startsWith("/") || value.contains("..")) {
            return null;
        }

        String namespace = DEFAULT_NAMESPACE;
        String path = value;
        int colon = value.indexOf(':');
        if (colon >= 0) {
            if (value.indexOf(':', colon + 1) >= 0) {
                return null;
            }
            namespace = value.substring(0, colon).trim();
            path = value.substring(colon + 1).trim();
        }

        if (namespace.isEmpty() || path.isEmpty()) {
            return null;
        }
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.toLowerCase(Locale.ROOT).endsWith(".png")) {
            path = path + ".png";
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return null;
            }
        }

        return Identifier.tryParse(namespace + ":" + path);
    }

    /** File name of a resolved texture, or an empty string. */
    public static String fileName(Identifier texture) {
        if (texture == null) {
            return "";
        }
        String path = texture.getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
