package com.argos.pbextra.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


public final class AdsReloadZoomControlConfig {

    public static final String FEATURE_TYPE = "AdsReloadZoomControl";
    public static final String ENABLED_KEY = "enabled";
    public static final String AMOUNT_KEY = "amount";
    public static final String ZOOM_TYPE_KEY = "zoom_type";

    public static final AdsReloadZoomControlConfig DISABLED =
            new AdsReloadZoomControlConfig(false, 0.0F, AdsReloadZoomEasing.DEFAULT_TYPE);

    private final boolean enabled;
    private final float amount;
    private final String zoomType;


    public AdsReloadZoomControlConfig(boolean enabled, float amount) {
        this(enabled, amount, AdsReloadZoomEasing.DEFAULT_TYPE);
    }

    public AdsReloadZoomControlConfig(boolean enabled, float amount, String zoomType) {
        this.enabled = enabled;
        this.amount = clamp(amount);
        this.zoomType = AdsReloadZoomEasing.normalize(zoomType);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float getAmount() {
        return amount;
    }
    public String getZoomType() {
        return zoomType;
    }


    public static AdsReloadZoomControlConfig fromJson(JsonObject obj) {
        if (obj == null) {
            return DISABLED;
        }

        boolean enabled = readBoolean(obj, ENABLED_KEY, false);
        float amount = readFloat(obj, AMOUNT_KEY, 0.0F);
        String zoomType = readString(obj, ZOOM_TYPE_KEY, AdsReloadZoomEasing.DEFAULT_TYPE);

        return new AdsReloadZoomControlConfig(enabled, amount, zoomType);
    }

    private static float clamp(float amount) {
        if (!Float.isFinite(amount) || amount <= 0.0F) {
            return 0.0F;
        }
        return Math.min(amount, 1.0F);
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

    private static float readFloat(JsonObject obj, String key, float def) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return def;
        }

        JsonPrimitive primitive = obj.getAsJsonPrimitive(key);
        if (primitive.isNumber()) {
            return primitive.getAsFloat();
        }
        return def;
    }

    private static String readString(JsonObject obj, String key, String def) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return def;
        }

        JsonPrimitive primitive = obj.getAsJsonPrimitive(key);
        if (primitive.isString()) {
            return primitive.getAsString();
        }
        return def;
    }

    @Override
    public String toString() {
        return "AdsReloadZoomControl[enabled=" + enabled + ", amount=" + amount
                + ", zoomType=" + zoomType + "]";
    }
}
