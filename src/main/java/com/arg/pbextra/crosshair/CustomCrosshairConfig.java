package com.arg.pbextra.crosshair;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


/**
 * Parsed {@code custom_crosshair} feature settings.
 *
 * <p>The feature is optional per weapon. When present, {@code enabled}
 * defaults to {@code true}; {@code texture} is the resource path and
 * {@code size} controls the static complete-image size. {@code layout} selects
 * between the native Point Blank 39x39 atlas (which reuses VPB's renderer and
 * therefore its exact expansion, smoothing, positioning and visibility) and
 * the backwards-compatible complete-image fallback, which is rendered statically.
 * A missing or invalid texture is represented by {@link #hasTexture()} returning
 * {@code false}, so the renderer can leave Point Blank's normal crosshair in
 * place instead of suppressing it.</p>
 */
public final class CustomCrosshairConfig {

    public static final String FEATURE_TYPE = "custom_crosshair";
    public static final String SETTINGS_KEY = "custom_crosshair";
    public static final String SETTINGS_KEY_CAMEL = "customCrosshair";

    public static final int DEFAULT_SIZE = 16;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 512;

    public static final CustomCrosshairConfig DISABLED =
            new CustomCrosshairConfig(false, null, DEFAULT_SIZE,
                    CustomCrosshairLayout.COMPLETE);

    private final boolean enabled;
    private final String texture;
    private final int size;
    private final CustomCrosshairLayout layout;

    /** Backwards-compatible constructor: complete static layout. */
    public CustomCrosshairConfig(boolean enabled, String texture, int size) {
        this(enabled, texture, size, CustomCrosshairLayout.COMPLETE);
    }

    public CustomCrosshairConfig(boolean enabled, String texture, int size,
                                 CustomCrosshairLayout layout) {
        this.enabled = enabled;
        this.texture = normalise(texture);
        this.size = clampSize(size);
        this.layout = layout == null ? CustomCrosshairLayout.COMPLETE : layout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Raw configured texture path, or {@code null} when absent. */
    public String getTexture() {
        return texture;
    }

    public boolean hasTexture() {
        return texture != null;
    }

    /**
     * Largest rendered dimension for the static complete-image layout. Native
     * Point Blank atlases deliberately ignore this value because VPB owns their
     * native geometry.
     */
    public int getSize() {
        return size;
    }

    public CustomCrosshairLayout getLayout() {
        return layout;
    }

    public static CustomCrosshairConfig fromJson(JsonObject feature) {
        if (feature == null) {
            return DISABLED;
        }

        JsonObject settings = feature;
        JsonObject nested = firstObject(feature, SETTINGS_KEY, SETTINGS_KEY_CAMEL);
        if (nested != null) {
            settings = nested;
        }

        Boolean enabled = readBoolean(settings, "enabled");
        String texture = readString(settings, "texture");
        Integer size = readInt(settings, "size");
        CustomCrosshairLayout layout = CustomCrosshairLayout.fromJson(
                readString(settings, "layout"));

        return new CustomCrosshairConfig(enabled == null || enabled,
                texture, size == null ? DEFAULT_SIZE : size, layout);
    }


    private static String normalise(String texture) {
        if (texture == null) {
            return null;
        }
        String value = texture.trim();
        return value.isEmpty() ? null : value;
    }

    private static int clampSize(int value) {
        if (value <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(MAX_SIZE, value);
    }

    private static JsonObject firstObject(JsonObject obj, String... keys) {
        for (String key : keys) {
            JsonElement element = obj.get(key);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        }
        return null;
    }

    private static Boolean readBoolean(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (!primitive.isString()) {
            return null;
        }
        String value = primitive.getAsString();
        if ("true".equalsIgnoreCase(value) || "enabled".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value) || "disabled".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static String readString(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isString()) {
            return null;
        }
        return element.getAsString();
    }

    private static Integer readInt(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        try {
            return primitive.isNumber()
                    ? primitive.getAsInt() : Integer.parseInt(primitive.getAsString().trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "CustomCrosshairConfig[enabled=" + enabled
                + ", texture=" + texture + ", size=" + size
                + ", layout=" + layout + "]";
    }
}
