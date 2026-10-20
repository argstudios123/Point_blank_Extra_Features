package com.arg.pbextra.crosshair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CustomCrosshairPlaceholdersTest {

    @TempDir
    Path tempDir;

    @Test
    void bundledPlaceholderIsAValidPng() {
        byte[] png = CustomCrosshairPlaceholders.readBundledPlaceholder();
        assertNotNull(png, "bundled custom_crosshair.png");
        assertTrue(png.length > 8);
        assertEquals((byte) 0x89, png[0]);
        assertEquals('P', png[1]);
        assertEquals('N', png[2]);
        assertEquals('G', png[3]);
    }

    @Test
    void placeholderIsCreatedAtTheReferencedPathAndNeverOverwritten() throws IOException {
        byte[] png = CustomCrosshairPlaceholders.readBundledPlaceholder();
        assertNotNull(png);

        Path pack = newPack("Argos Weaponry");
        writeItem(pack, "gun", """
                {
                  // Point Blank content-pack files commonly contain comments.
                  "name": "gun",
                  "features": [
                    {
                      "type": "custom_crosshair",
                      "enabled": true,
                      "texture": "textures/item/custom_crosshairs/custom_crosshair.png",
                      "size": 16
                    }
                  ]
                }
                """);

        assertEquals(1, CustomCrosshairPlaceholders.install(tempDir, png));

        Path target = pack.resolve(
                "assets/pointblank/textures/item/custom_crosshairs/custom_crosshair.png");
        assertTrue(Files.exists(target));
        assertArrayEquals(png, Files.readAllBytes(target));

        // Simulate a creator editing the file; a second scan must not touch it.
        byte[] edited = "creator-owned".getBytes(StandardCharsets.UTF_8);
        Files.write(target, edited);
        assertEquals(0, CustomCrosshairPlaceholders.install(tempDir, png));
        assertArrayEquals(edited, Files.readAllBytes(target));
    }

    @Test
    void onlyThePlaceholderFileNameIsGenerated() throws IOException {
        byte[] png = CustomCrosshairPlaceholders.readBundledPlaceholder();
        assertNotNull(png);

        Path pack = newPack("custom_pack");
        writeItem(pack, "other", """
                {
                  "name": "other",
                  "features": [
                    {
                      "type": "custom_crosshair",
                      "texture": "textures/item/crosshairs/other.png"
                    }
                  ]
                }
                """);

        assertEquals(0, CustomCrosshairPlaceholders.install(tempDir, png));
        assertFalse(Files.exists(pack.resolve(
                "assets/pointblank/textures/item/crosshairs/other.png")));
    }

    @Test
    void traversalPathsAreRejected() throws IOException {
        byte[] png = CustomCrosshairPlaceholders.readBundledPlaceholder();
        assertNotNull(png);

        Path pack = newPack("unsafe_pack");
        writeItem(pack, "unsafe", """
                {
                  "name": "unsafe",
                  "features": [
                    {
                      "type": "custom_crosshair",
                      "texture": "../../../../custom_crosshair.png"
                    }
                  ]
                }
                """);

        assertEquals(0, CustomCrosshairPlaceholders.install(tempDir, png));
        assertFalse(Files.exists(tempDir.resolve("custom_crosshair.png")));
    }

    @Test
    void multiplePacksEachGetTheirOwnPlaceholder() throws IOException {
        byte[] png = CustomCrosshairPlaceholders.readBundledPlaceholder();
        assertNotNull(png);

        Path first = newPack("pack_one");
        Path second = newPack("pack_two");
        String json = """
                {
                  "name": "gun",
                  "features": [
                    {
                      "type": "custom_crosshair",
                      "texture": "textures/item/custom_crosshairs/custom_crosshair.png"
                    }
                  ]
                }
                """;
        writeItem(first, "gun", json);
        writeItem(second, "gun", json);

        assertEquals(2, CustomCrosshairPlaceholders.install(tempDir, png));
        assertTrue(Files.exists(first.resolve(
                "assets/pointblank/textures/item/custom_crosshairs/custom_crosshair.png")));
        assertTrue(Files.exists(second.resolve(
                "assets/pointblank/textures/item/custom_crosshairs/custom_crosshair.png")));
    }

    @Test
    void disabledFeatureStillGetsAnEditablePlaceholder() throws IOException {
        byte[] png = CustomCrosshairPlaceholders.readBundledPlaceholder();
        assertNotNull(png);

        Path pack = newPack("disabled_pack");
        writeItem(pack, "gun", """
                {
                  "name": "gun",
                  "features": [
                    {
                      "type": "custom_crosshair",
                      "enabled": false,
                      "texture": "textures/item/custom_crosshairs/custom_crosshair.png"
                    }
                  ]
                }
                """);

        assertEquals(1, CustomCrosshairPlaceholders.install(tempDir, png));
        assertTrue(Files.exists(pack.resolve(
                "assets/pointblank/textures/item/custom_crosshairs/custom_crosshair.png")));
    }

    @Test
    void explicitPointblankNamespaceUsesThePointblankAssetsRoot() throws IOException {
        byte[] png = CustomCrosshairPlaceholders.readBundledPlaceholder();
        assertNotNull(png);

        Path pack = newPack("namespace_pack");
        writeItem(pack, "gun", """
                {
                  "name": "gun",
                  "features": [
                    {
                      "type": "custom_crosshair",
                      "texture": "pointblank:textures/gui/custom_crosshair.png"
                    }
                  ]
                }
                """);

        assertEquals(1, CustomCrosshairPlaceholders.install(tempDir, png));
        assertTrue(Files.exists(pack.resolve(
                "assets/pointblank/textures/gui/custom_crosshair.png")));
    }

    @Test
    void nonPointblankNamespaceIsNotInstalledIntoAnExtensionPack() throws IOException {
        byte[] png = CustomCrosshairPlaceholders.readBundledPlaceholder();
        assertNotNull(png);

        Path pack = newPack("foreign_namespace_pack");
        writeItem(pack, "gun", """
                {
                  "name": "gun",
                  "features": [
                    {
                      "type": "custom_crosshair",
                      "texture": "mypack:textures/gui/custom_crosshair.png"
                    }
                  ]
                }
                """);

        assertEquals(0, CustomCrosshairPlaceholders.install(tempDir, png));
        assertFalse(Files.exists(pack.resolve(
                "assets/mypack/textures/gui/custom_crosshair.png")));
    }

    @Test
    void pointblankLayoutDoesNotGetACompletePlaceholder() throws IOException {
        byte[] png = CustomCrosshairPlaceholders.readBundledPlaceholder();
        assertNotNull(png);

        Path pack = newPack("native_layout_pack");
        writeItem(pack, "gun", """
                {
                  "name": "gun",
                  "features": [
                    {
                      "type": "custom_crosshair",
                      "layout": "pointblank",
                      "texture": "textures/item/custom_crosshairs/custom_crosshair.png"
                    }
                  ]
                }
                """);

        assertEquals(0, CustomCrosshairPlaceholders.install(tempDir, png));
        assertFalse(Files.exists(pack.resolve(
                "assets/pointblank/textures/item/custom_crosshairs/custom_crosshair.png")));
    }

    private Path newPack(String name) throws IOException {
        Path pack = tempDir.resolve(name);
        Files.createDirectories(pack.resolve("assets/pointblank/items"));
        return pack;
    }

    private static void writeItem(Path pack, String name, String json) throws IOException {
        Files.writeString(pack.resolve("assets/pointblank/items/" + name + ".json"),
                json, StandardCharsets.UTF_8);
    }
}

