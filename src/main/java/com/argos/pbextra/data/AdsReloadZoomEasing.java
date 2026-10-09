package com.argos.pbextra.data;

import java.util.Locale;


public final class AdsReloadZoomEasing {

    public static final String DEFAULT_TYPE = "smooth";

    private AdsReloadZoomEasing() {
    }


    public static String normalize(String type) {
        if (type == null) {
            return DEFAULT_TYPE;
        }
        String value = type.trim().toLowerCase(Locale.ROOT);
        switch (value) {
            case "linear":
            case "smooth":
            case "ease_in":
            case "ease_out":
            case "ease_in_out":
            case "sine_in":
            case "sine_out":
            case "sine_in_out":
                return value;
            default:
                return DEFAULT_TYPE;
        }
    }


    public static float apply(String type, float progress) {
        float t = clamp01(progress);
        switch (normalize(type)) {
            case "linear":
                return t;
            case "ease_in":
                // Quadratic ease-in: slow start, fast finish.
                return t * t;
            case "ease_out":
                // Quadratic ease-out: fast start, slow finish.
                return 1.0F - (1.0F - t) * (1.0F - t);
            case "ease_in_out":
                // Quadratic ease-in-out: slow start and finish.
                return t < 0.5F
                        ? 2.0F * t * t
                        : 1.0F - 2.0F * (1.0F - t) * (1.0F - t);
            case "sine_in":
                return (float) (1.0 - Math.cos(t * Math.PI / 2.0));
            case "sine_out":
                return (float) Math.sin(t * Math.PI / 2.0);
            case "sine_in_out":
                return (float) (-(Math.cos(Math.PI * t) - 1.0) / 2.0);
            case "smooth":
            default:
                // Smoothstep: gentle, non-robotic accelerate/decelerate.
                return t * t * (3.0F - 2.0F * t);
        }
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            return 0.0F;
        }
        return Math.min(value, 1.0F);
    }
}
