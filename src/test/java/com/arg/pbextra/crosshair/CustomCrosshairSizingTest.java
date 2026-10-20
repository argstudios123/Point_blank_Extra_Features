package com.arg.pbextra.crosshair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class CustomCrosshairSizingTest {

    @Test
    void squareTextureFillsTheConfiguredBox() {
        assertSize(16, 16, 64, 64, 16);
    }

    @Test
    void wideAndTallTexturesKeepTheirAspectRatio() {
        assertSize(16, 8, 128, 64, 16);
        assertSize(8, 16, 64, 128, 16);
        assertSize(24, 6, 256, 64, 24);
    }

    @Test
    void dimensionsNeverCollapseToZero() {
        assertSize(1, 1, 1024, 1, 1);
        assertSize(1, 1, 0, 64, 16);
        assertSize(1, 1, 64, -1, 16);
    }

    private static void assertSize(int expectedWidth, int expectedHeight,
                                   int textureWidth, int textureHeight, int boxSize) {
        assertEquals(expectedWidth,
                CustomCrosshairSizing.fitWidth(textureWidth, textureHeight, boxSize));
        assertEquals(expectedHeight,
                CustomCrosshairSizing.fitHeight(textureWidth, textureHeight, boxSize));
    }
}
