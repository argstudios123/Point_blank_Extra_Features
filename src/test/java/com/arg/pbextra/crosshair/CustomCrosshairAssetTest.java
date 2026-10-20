package com.arg.pbextra.crosshair;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CustomCrosshairAssetTest {

    private static final String COMPLETE =
            "assets/pointblankextra/textures/custom_crosshair/custom_crosshair.png";
    private static final String NATIVE =
            "assets/pointblankextra/textures/custom_crosshair/custom_crosshair_native.png";

    @Test
    void completePlaceholderIsA64x64TransparentPng() throws Exception {
        BufferedImage image = load(COMPLETE);
        assertEquals(64, image.getWidth());
        assertEquals(64, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(0, alphaAt(image, 0, 0));
    }

    @Test
    void nativeTemplateIsA39x39TransparentPng() throws Exception {
        BufferedImage image = load(NATIVE);
        assertEquals(CustomCrosshairLayout.POINTBLANK_TEXTURE_SIZE, image.getWidth());
        assertEquals(CustomCrosshairLayout.POINTBLANK_TEXTURE_SIZE, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(0, alphaAt(image, 0, 0));
    }

    @Test
    void nativeTemplateHasTopAndBottomSegments() throws Exception {
        BufferedImage image = load(NATIVE);
        assertTrue(hasOpaquePixel(image, 16, 1, 23, 15),
                "native template needs top-segment artwork");
        assertTrue(hasOpaquePixel(image, 16, 24, 23, 39),
                "native template needs bottom-segment artwork");
    }

    private static boolean hasOpaquePixel(BufferedImage image,
                                          int x0, int y0, int x1, int y1) {
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (alphaAt(image, x, y) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static BufferedImage load(String path) throws Exception {
        try (InputStream stream = CustomCrosshairAssetTest.class.getClassLoader()
                .getResourceAsStream(path)) {
            assertNotNull(stream, path);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, path);
            return image;
        }
    }

    private static int alphaAt(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) & 0xFF;
    }
}
