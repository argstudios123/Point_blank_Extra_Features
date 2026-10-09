package com.argos.pbextra.constraint;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Locale;


public final class AdsConstraintConfig {

    public static final String ENABLED_KEY = "enabled";

    public static final String CAMERA_KEY = "camera";

    public static final String CAMERA_CONSTRAINT_KEY = "cameraConstraint";

    private static final float UNSPECIFIED = Float.NaN;

    public static final AdsConstraintConfig DISABLED =
            new AdsConstraintConfig(false, true, UNSPECIFIED);

    private final boolean enabled;
    private final boolean camera;
    private final float cameraConstraint;

    /** Backwards-compatible constructor; no explicit camera constraint is configured. */
    public AdsConstraintConfig(boolean enabled, boolean camera) {
        this(enabled, camera, UNSPECIFIED);
    }

    public AdsConstraintConfig(boolean enabled, boolean camera, float cameraConstraint) {
        this.enabled = enabled;
        this.camera = camera;
        this.cameraConstraint = cameraConstraint;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isCamera() {
        return camera;
    }


    public boolean hasCameraConstraint() {
        return Float.isFinite(cameraConstraint);
    }

    public float getCameraConstraint() {
        return cameraConstraint;
    }


    public static AdsConstraintConfig fromJson(JsonObject obj) {
        if (obj == null) {
            return DISABLED;
        }

        boolean enabled = readBoolean(obj, ENABLED_KEY, false);
        boolean camera = readBoolean(obj, CAMERA_KEY, true);
        float cameraConstraint = readCameraConstraint(obj);

        return new AdsConstraintConfig(enabled, camera, cameraConstraint);
    }

    private static float readCameraConstraint(JsonObject obj) {
        if (!obj.has(CAMERA_CONSTRAINT_KEY) || !obj.get(CAMERA_CONSTRAINT_KEY).isJsonPrimitive()) {
            return UNSPECIFIED;
        }
        JsonPrimitive primitive = obj.getAsJsonPrimitive(CAMERA_CONSTRAINT_KEY);
        if (!primitive.isNumber()) {
            return UNSPECIFIED;
        }
        float value = primitive.getAsFloat();
        if (!Float.isFinite(value) || value <= 0.0F) {
            return 0.0F;
        }
        return Math.min(value, 1.0F);
    }

    private static boolean readBoolean(JsonObject obj, String key, boolean def) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return def;
        }

        JsonPrimitive primitive = obj.getAsJsonPrimitive(key);
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (!primitive.isString()) {
            return def;
        }

        String value = primitive.getAsString();
        if ("true".equalsIgnoreCase(value) || "enabled".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "disabled".equalsIgnoreCase(value)) {
            return false;
        }
        return def;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "AdsConstraint[enabled=%s, camera=%s, cameraConstraint=%s]",
                enabled, camera, hasCameraConstraint() ? Float.toString(cameraConstraint) : "default");
    }
}
