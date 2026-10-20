package com.arg.pbextra.crosshair;


/**
 * Pure sizing helpers shared by the renderer and its tests. These return
 * individual dimensions so the per-frame render path does not allocate.
 */
public final class CustomCrosshairSizing {

    private CustomCrosshairSizing() {
    }

    /**
     * Width that fits the texture inside a square box without distorting its
     * aspect ratio. Always at least 1.
     */
    public static int fitWidth(int textureWidth, int textureHeight, int boxSize) {
        if (textureWidth <= 0 || textureHeight <= 0 || boxSize <= 0) {
            return 1;
        }
        return Math.max(1,
                (int) Math.round(textureWidth * scale(textureWidth, textureHeight, boxSize)));
    }

    /**
     * Height that fits the texture inside a square box without distorting its
     * aspect ratio. Always at least 1.
     */
    public static int fitHeight(int textureWidth, int textureHeight, int boxSize) {
        if (textureWidth <= 0 || textureHeight <= 0 || boxSize <= 0) {
            return 1;
        }
        return Math.max(1,
                (int) Math.round(textureHeight * scale(textureWidth, textureHeight, boxSize)));
    }

    private static double scale(int textureWidth, int textureHeight, int boxSize) {
        return boxSize / (double) Math.max(textureWidth, textureHeight);
    }
}
