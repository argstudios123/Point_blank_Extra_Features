package com.arg.pbextra.client.bulletholes;

import com.arg.pbextra.bullethole.BulletHoleConfig;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/** Verifies the built-in PNG resources are present, valid and material-specific. */
class BulletHoleTextureResourceTest {

    private static final String ROOT = "/assets/pointblankextra/textures/bullet_holes/";
    private static final String[] TEXTURES = {
            "default.png", "wood.png", "stone.png", "metal.png",
            "glass.png", "dirt.png", "sand.png"
    };

    @Test
    void allBuiltInTexturesArePackagedAsValidRgbaPngs() throws IOException {
        for (String name : TEXTURES) {
            BufferedImage image = read(name);
            assertEquals(16, image.getWidth(), name + " width");
            assertEquals(16, image.getHeight(), name + " height");
            assertTrue(image.getColorModel().hasAlpha(), name + " must be RGBA");
            int transparent = 0;
            int visible = 0;
            Set<Integer> colors = new HashSet<>();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int alpha = argb >>> 24;
                    if (alpha == 0) {
                        transparent++;
                    } else {
                        visible++;
                        colors.add(argb & 0x00FFFFFF);
                    }
                }
            }
            assertTrue(transparent > 0, name + " should have transparent border pixels");
            assertTrue(visible > 0, name + " should have visible pixels");
            assertTrue(colors.size() >= 5, name + " should use a pixel-art palette, not a flat fill");
        }
    }

    @Test
    void glassDirtAndSandAreVisuallyDistinctFromTheOldMaterials() throws IOException {
        int glass = hash("glass.png");
        int dirt = hash("dirt.png");
        int sand = hash("sand.png");
        int stone = hash("stone.png");
        int metal = hash("metal.png");
        int wood = hash("wood.png");

        Set<Integer> distinct = Set.of(glass, dirt, sand, stone, metal, wood);
        assertEquals(6, distinct.size(), "each material texture must be distinct");

        assertTrue(averageBlue("glass.png") > averageRed("glass.png"),
                "glass should have a cool/blue impact palette");
        assertTrue(averageRed("dirt.png") > averageGreen("dirt.png")
                        && averageGreen("dirt.png") > averageBlue("dirt.png"),
                "dirt should use an earthy brown palette");
        assertTrue(averageRed("sand.png") > averageGreen("sand.png")
                        && averageGreen("sand.png") > averageBlue("sand.png"),
                "sand should use a warm tan palette");
        assertTrue(averageBrightness("sand.png") > averageBrightness("dirt.png"),
                "sand should be visibly lighter than dirt");
        assertTrue(averageBrightness("metal.png") > averageBrightness("dirt.png"),
                "metal should remain distinct and lighter than dirt");
    }

    @Test
    void textureIdentifiersPointAtThePackagedFiles() throws IOException {
        assertEquals("textures/bullet_holes/glass.png", BulletHoleConfig.GLASS_TEXTURE.getPath());
        assertEquals("textures/bullet_holes/dirt.png", BulletHoleConfig.DIRT_TEXTURE.getPath());
        assertEquals("textures/bullet_holes/sand.png", BulletHoleConfig.SAND_TEXTURE.getPath());
        for (String name : new String[]{"glass.png", "dirt.png", "sand.png"}) {
            assertNotNull(read(name), "texture should resolve: " + name);
        }
    }

    private static BufferedImage read(String name) throws IOException {
        try (InputStream stream = BulletHoleTextureResourceTest.class
                .getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, "missing texture resource: " + ROOT + name);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, "unreadable PNG: " + name);
            return image;
        }
    }

    private static int hash(String name) throws IOException {
        BufferedImage image = read(name);
        int hash = 1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                hash = 31 * hash + image.getRGB(x, y);
            }
        }
        return hash;
    }

    private static double averageRed(String name) throws IOException {
        return channelAverage(name, 16);
    }

    private static double averageGreen(String name) throws IOException {
        return channelAverage(name, 8);
    }

    private static double averageBlue(String name) throws IOException {
        return channelAverage(name, 0);
    }

    private static double channelAverage(String name, int shift) throws IOException {
        BufferedImage image = read(name);
        long total = 0;
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                total += (argb >> shift) & 0xFF;
                count++;
            }
        }
        return count == 0 ? 0.0D : (double) total / count;
    }

    private static double averageBrightness(String name) throws IOException {
        return (averageRed(name) + averageGreen(name) + averageBlue(name)) / 3.0D;
    }
}