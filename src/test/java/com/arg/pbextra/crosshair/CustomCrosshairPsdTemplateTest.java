package com.arg.pbextra.crosshair;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CustomCrosshairPsdTemplateTest {

    private static final String BASE =
            "assets/pointblankextra/templates/custom_crosshair/";

    @Test
    void native39TemplateIsAValidLayeredPsd() {
        assertPsd("custom_crosshair_native_39.psd", 39, 39,
                "Top Segment", "Bottom Segment", "Left Segment",
                "Right Segment", "Center", "Guides / Safe Areas");
    }

    @Test
    void nativeWorkbookTemplateIsAValidLayeredPsd() {
        assertPsd("custom_crosshair_native_workbook_312.psd", 312, 312,
                "Top Segment", "Bottom Segment", "Left Segment",
                "Right Segment", "Center", "Guides / Safe Areas");
    }

    @Test
    void completeTemplateIsAValidLayeredPsd() {
        assertPsd("custom_crosshair_complete_512.psd", 512, 512,
                "Artwork", "Guides");
    }

    private static void assertPsd(String fileName, int width, int height,
                                  String... layerNames) {
        byte[] data = read(BASE + fileName);
        assertNotNull(data, fileName);
        assertEquals('8', data[0]);
        assertEquals('B', data[1]);
        assertEquals('P', data[2]);
        assertEquals('S', data[3]);

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        assertEquals(1, buffer.getShort(4));
        assertEquals(4, buffer.getShort(12));
        assertEquals(height, buffer.getInt(14));
        assertEquals(width, buffer.getInt(18));
        assertEquals(8, buffer.getShort(22));
        assertEquals(3, buffer.getShort(24));

        for (String name : layerNames) {
            assertTrue(indexOf(data, name.getBytes(StandardCharsets.US_ASCII)) >= 0,
                    fileName + " is missing layer " + name);
        }
    }

    private static byte[] read(String path) {
        try (var stream = CustomCrosshairPsdTemplateTest.class.getClassLoader()
                .getResourceAsStream(path)) {
            return stream == null ? null : stream.readAllBytes();
        } catch (Exception exception) {
            throw new AssertionError("Could not read " + path, exception);
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
