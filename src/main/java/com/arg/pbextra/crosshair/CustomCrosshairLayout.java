package com.arg.pbextra.crosshair;

import java.util.Locale;


/**
 * Texture layout used by a custom crosshair.
 *
 * <p>{@link #POINTBLANK} is Vic's Point Blank's native 39x39 crosshair atlas:
 * left/right/bottom regions plus a central patch. It is passed directly to the
 * native renderer, reproducing VPB's exact segment geometry and behavior. Point
 * Blank 2.2.0 omits the top region, so PBE adds a mirrored top quad using the
 * same native expansion value. {@link #COMPLETE} is the backwards-compatible
 * arbitrary PNG layout. It is rendered as a simple static image because a
 * single undivided texture cannot follow VPB's native segmented atlas.</p>
 */
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
