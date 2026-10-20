package com.arg.pbextra.crosshair;

import java.util.Locale;

public enum CustomCrosshairLayout {

    COMPLETE,
    POINTBLANK;

    /** Native crosshair atlas size; the native arm blits hardcode 39 px. */
    public static final int POINTBLANK_TEXTURE_SIZE = 39;

    public static CustomCrosshairLayout fromJson(String value) {
        if (value == null) {
            return COMPLETE;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "pointblank", "native", "segmented", "atlas" -> POINTBLANK;
            default -> COMPLETE;
        };
    }

    /** Whether the decoded texture can use this layout. */
    public boolean accepts(int textureWidth, int textureHeight) {
        return this != POINTBLANK
                || (textureWidth == POINTBLANK_TEXTURE_SIZE
                && textureHeight == POINTBLANK_TEXTURE_SIZE);
    }
}
