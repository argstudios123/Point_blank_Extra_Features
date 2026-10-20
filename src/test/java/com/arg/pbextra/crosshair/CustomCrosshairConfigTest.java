package com.arg.pbextra.crosshair;

import com.arg.pbextra.data.WeaponExtraData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CustomCrosshairConfigTest {

    @Test
    void exampleFeatureParses() {
        CustomCrosshairConfig config = parseFeature("""
                {
                  "type": "custom_crosshair",
                  "enabled": true,
                  "texture": "textures/item/custom_crosshairs/custom_crosshair.png",
                  "size": 16
                }
                """);

        assertTrue(config.isEnabled());
        assertTrue(config.hasTexture());
        assertEquals("textures/item/custom_crosshairs/custom_crosshair.png",
                config.getTexture());
        assertEquals(16, config.getSize());
    }

    @Test
    void enabledDefaultsToTrueAndTextureMayBeMissing() {
        CustomCrosshairConfig config = parseFeature("""
                { "type": "custom_crosshair", "texture": "textures/x.png" }
                """);
        assertTrue(config.isEnabled());

        CustomCrosshairConfig missing = parseFeature("""
                { "type": "custom_crosshair" }
                """);
        assertTrue(missing.isEnabled());
        assertFalse(missing.hasTexture());
        assertNull(missing.getTexture());
    }

    @Test
    void disabledAndNestedFormsAreSupported() {
        assertFalse(parseFeature("""
                { "type": "custom_crosshair", "enabled": false,
                  "texture": "textures/x.png" }
                """).isEnabled());

        CustomCrosshairConfig nested = parseFeature("""
                {
                  "type": "custom_crosshair",
                  "custom_crosshair": {
                    "enabled": false,
                    "texture": "textures/nested.png",
                    "size": 24
                  }
                }
                """);
        assertFalse(nested.isEnabled());
        assertEquals("textures/nested.png", nested.getTexture());
        assertEquals(24, nested.getSize());
    }

    @Test
    void sizeDefaultsAndClampsSafely() {
        assertEquals(CustomCrosshairConfig.DEFAULT_SIZE,
                parseFeature("{ \"type\": \"custom_crosshair\" }").getSize());
        assertEquals(CustomCrosshairConfig.DEFAULT_SIZE,
                parseFeature("{ \"type\": \"custom_crosshair\", \"size\": 0 }").getSize());
        assertEquals(CustomCrosshairConfig.DEFAULT_SIZE,
                parseFeature("{ \"type\": \"custom_crosshair\", \"size\": -4 }").getSize());
        assertEquals(CustomCrosshairConfig.DEFAULT_SIZE,
                parseFeature("{ \"type\": \"custom_crosshair\", \"size\": \"oops\" }")
                        .getSize());
        assertEquals(32, parseFeature(
                "{ \"type\": \"custom_crosshair\", \"size\": \"32\" }").getSize());
        assertEquals(CustomCrosshairConfig.MAX_SIZE,
                parseFeature("{ \"type\": \"custom_crosshair\", \"size\": 100000 }")
                        .getSize());
    }

    @Test
    void weaponFeatureIsParsedThroughWeaponExtraData() {
        WeaponExtraData data = WeaponExtraData.fromJson(JsonParser.parseString("""
                {
                  "name": "crosshair_gun",
                  "features": [
                    {
                      "type": "custom_crosshair",
                      "enabled": true,
                      "texture": "textures/item/custom_crosshairs/custom_crosshair.png",
                      "size": 20
                    }
                  ]
                }
                """).getAsJsonObject());

        assertTrue(data.getCustomCrosshair().isEnabled());
        assertEquals(20, data.getCustomCrosshair().getSize());
        assertEquals("textures/item/custom_crosshairs/custom_crosshair.png",
                data.getCustomCrosshair().getTexture());

        WeaponExtraData plain = WeaponExtraData.fromJson(
                JsonParser.parseString("{ \"name\": \"plain\" }").getAsJsonObject());
        assertFalse(plain.getCustomCrosshair().isEnabled());
        assertFalse(plain.getCustomCrosshair().hasTexture());
    }

    @Test
    void texturePathsFollowPointBlankConventions() {
        assertEquals(Identifier.parse(
                        "pointblank:textures/item/custom_crosshairs/custom_crosshair.png"),
                CustomCrosshairPaths.parse(
                        "textures/item/custom_crosshairs/custom_crosshair.png"));
        assertEquals(Identifier.parse(
                        "pointblank:textures/item/crosshairs/m7_crosshair.png"),
                CustomCrosshairPaths.parse("item/crosshairs/m7_crosshair"));
        assertEquals(Identifier.parse("mypack:textures/gui/x.png"),
                CustomCrosshairPaths.parse("mypack:textures/gui/x.png"));
        assertEquals(Identifier.parse("pointblank:textures/custom_crosshair.png"),
                CustomCrosshairPaths.parse("custom_crosshair.png"));
    }

    @Test
    void invalidTexturePathsAreRejected() {
        assertNull(CustomCrosshairPaths.parse(null));
        assertNull(CustomCrosshairPaths.parse(""));
        assertNull(CustomCrosshairPaths.parse("   "));
        assertNull(CustomCrosshairPaths.parse("/absolute.png"));
        assertNull(CustomCrosshairPaths.parse("../outside.png"));
        assertNull(CustomCrosshairPaths.parse("textures/../../outside.png"));
        assertNull(CustomCrosshairPaths.parse("textures//empty.png"));
        assertNull(CustomCrosshairPaths.parse("textures/./dot.png"));
        assertNull(CustomCrosshairPaths.parse("a:b:c"));
    }

    @Test
    void fileNameReturnsTheLastSegment() {
        Identifier id = CustomCrosshairPaths.parse(
                "textures/item/custom_crosshairs/custom_crosshair.png");
        assertNotNull(id);
        assertEquals("custom_crosshair.png", CustomCrosshairPaths.fileName(id));
        assertEquals("", CustomCrosshairPaths.fileName(null));
    }

    @Test
    void layoutDefaultsToCompleteAndAliasesAreAccepted() {
        assertEquals(CustomCrosshairLayout.COMPLETE,
                parseFeature("{ \"type\": \"custom_crosshair\" }").getLayout());
        assertEquals(CustomCrosshairLayout.POINTBLANK,
                parseFeature("{ \"type\": \"custom_crosshair\", "
                        + "\"layout\": \"pointblank\" }").getLayout());
        assertEquals(CustomCrosshairLayout.POINTBLANK,
                parseFeature("{ \"type\": \"custom_crosshair\", "
                        + "\"layout\": \"native\" }").getLayout());
        assertEquals(CustomCrosshairLayout.COMPLETE,
                parseFeature("{ \"type\": \"custom_crosshair\", "
                        + "\"layout\": \"unknown\" }").getLayout());

        assertTrue(CustomCrosshairLayout.POINTBLANK.accepts(39, 39));
        assertFalse(CustomCrosshairLayout.POINTBLANK.accepts(64, 64));
        assertTrue(CustomCrosshairLayout.COMPLETE.accepts(512, 128));
    }

    @Test
    void dynamicKeysAreIgnoredAndDoNotCreateASecondSpreadSystem() {
        CustomCrosshairConfig config = parseFeature("""
                {
                  "type": "custom_crosshair",
                  "texture": "textures/item/custom_crosshairs/halo_reticle.png",
                  "size": 16,
                  "dynamic": {
                    "enabled": true,
                    "movement": { "expansion": 100.0 },
                    "firing": { "expansion": 100.0 },
                    "jumping": { "expansion": 100.0 }
                  }
                }
                """);

        assertTrue(config.isEnabled());
        assertEquals(CustomCrosshairLayout.COMPLETE, config.getLayout());
        assertEquals(16, config.getSize());
        // No dynamic state is exposed or applied by PBE.
        assertEquals("textures/item/custom_crosshairs/halo_reticle.png",
                config.getTexture());
    }

    private static CustomCrosshairConfig parseFeature(String json) {
        JsonObject feature = JsonParser.parseString(json).getAsJsonObject();
        return CustomCrosshairConfig.fromJson(feature);
    }
}

