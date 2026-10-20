package com.arg.pbextra.bullethole;

import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class BulletHoleSoundConfigTest {

    @Test
    void exampleSoundObjectParsesAndIntegratesWithBulletHoleConfig() {
        BulletHoleConfig config = BulletHoleConfig.resolve(
                BulletHoleConfig.Overrides.fromJson(JsonParser.parseString("""
                        {
                          "type": "bulletHoles",
                          "bullet_holes": {
                            "enabled": true,
                            "soundsPlayOnperBlockHit": {
                              "enabled": true,
                              "default": "minecraft:block.stone.hit",
                              "surface_sounds": {
                                "minecraft:oak_planks": "minecraft:block.wood.hit",
                                "minecraft:stone": "minecraft:block.stone.hit",
                                "minecraft:iron_block": "minecraft:block.metal.hit",
                                "minecraft:glass": "minecraft:block.glass.hit",
                                "minecraft:dirt": "minecraft:block.gravel.hit",
                                "minecraft:sand": "minecraft:block.sand.hit"
                              },
                              "tag_sounds": {
                                "minecraft:logs": "minecraft:block.wood.hit",
                                "minecraft:planks": "minecraft:block.wood.hit"
                              }
                            }
                          }
                        }
                        """).getAsJsonObject()),
                BulletHoleConfig.BUILT_IN_DEFAULTS);

        BulletHoleSoundConfig sounds = config.getSounds();
        assertTrue(sounds.isEnabled());
        assertEquals(parse("minecraft:block.stone.hit"),
                sounds.getDefaultSound().sound());
        assertEquals(parse("minecraft:block.wood.hit"),
                sounds.selectSound(parse("minecraft:oak_planks"), tag -> false).sound());
        assertEquals(parse("minecraft:block.metal.hit"),
                sounds.selectSound(parse("minecraft:iron_block"), tag -> false).sound());
        assertEquals(parse("minecraft:block.glass.hit"),
                sounds.selectSound(parse("minecraft:glass"), tag -> false).sound());
        assertEquals(parse("minecraft:block.gravel.hit"),
                sounds.selectSound(parse("minecraft:dirt"), tag -> false).sound());
        assertEquals(parse("minecraft:block.sand.hit"),
                sounds.selectSound(parse("minecraft:sand"), tag -> false).sound());
    }

    @Test
    void exactSoundBeatsTagAndTagBeatsDefault() {
        BulletHoleSoundConfig sounds = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        {
                          "default": "minecraft:block.stone.hit",
                          "surface_sounds": {
                            "minecraft:oak_planks": "mypack:exact"
                          },
                          "tag_sounds": {
                            "minecraft:planks": "minecraft:block.wood.hit"
                          }
                        }
                        """).getAsJsonObject()),
                BulletHoleSoundConfig.BUILT_IN_DEFAULTS);

        assertEquals(parse("mypack:exact"),
                sounds.selectSound(parse("minecraft:oak_planks"),
                        tag -> tag.equals(parse("minecraft:planks"))).sound());
        assertEquals(parse("minecraft:block.wood.hit"),
                sounds.selectSound(parse("minecraft:birch_planks"),
                        tag -> tag.equals(parse("minecraft:planks"))).sound());
        assertEquals(parse("minecraft:block.stone.hit"),
                sounds.selectSound(parse("minecraft:diamond_block"), tag -> false).sound());
    }

    @Test
    void multipleMatchingTagsUseLexicographicPriority() {
        BulletHoleSoundConfig sounds = new BulletHoleSoundConfig(true,
                new BulletHoleSoundConfig.SoundRef(parse("minecraft:block.stone.hit")),
                java.util.Map.of(),
                java.util.Map.of(
                        parse("minecraft:planks"),
                        new BulletHoleSoundConfig.SoundRef(parse("minecraft:block.stone.hit")),
                        parse("minecraft:logs"),
                        new BulletHoleSoundConfig.SoundRef(parse("minecraft:block.wood.hit"))));

        assertEquals(parse("minecraft:block.wood.hit"),
                sounds.selectSound(parse("minecraft:oak_log"), tag -> true).sound());
    }

    @Test
    void disabledSoundEntriesDoNotFallThrough() {
        BulletHoleSoundConfig sounds = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        {
                          "enabled": true,
                          "default": "none",
                          "surface_sounds": {
                            "minecraft:stone": "none"
                          },
                          "tag_sounds": {
                            "minecraft:planks": "disabled"
                          }
                        }
                        """).getAsJsonObject()),
                BulletHoleSoundConfig.BUILT_IN_DEFAULTS);

        assertFalse(sounds.selectSound(parse("minecraft:stone"), tag -> false).isEnabled());
        assertFalse(sounds.selectSound(parse("minecraft:birch_planks"),
                tag -> tag.equals(parse("minecraft:planks"))).isEnabled());
        assertFalse(sounds.selectSound(parse("minecraft:diamond_block"), tag -> false)
                .isEnabled());
    }

    @Test
    void disabledFeatureProducesNoSound() {
        BulletHoleSoundConfig sounds = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        { "enabled": false, "default": "minecraft:block.stone.hit" }
                        """).getAsJsonObject()),
                BulletHoleSoundConfig.BUILT_IN_DEFAULTS);

        assertFalse(sounds.isEnabled());
        assertNull(sounds.selectSound(parse("minecraft:stone"), tag -> true));
    }


    @Test
    void gunOverridesOneEntryWithoutDroppingPackDefaults() {
        BulletHoleSoundConfig pack = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        {
                          "default": "minecraft:block.wood.hit",
                          "surface_sounds": {
                            "minecraft:stone": "minecraft:block.stone.hit",
                            "minecraft:iron_block": "minecraft:block.metal.hit"
                          },
                          "tag_sounds": {
                            "minecraft:planks": "minecraft:block.wood.hit"
                          }
                        }
                        """).getAsJsonObject()),
                BulletHoleSoundConfig.BUILT_IN_DEFAULTS);

        BulletHoleSoundConfig gun = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        {
                          "surface_sounds": {
                            "minecraft:iron_block": "mypack:custom_metal"
                          }
                        }
                        """).getAsJsonObject()),
                pack);

        assertEquals(parse("mypack:custom_metal"),
                gun.selectSound(parse("minecraft:iron_block"), tag -> false).sound());
        assertEquals(parse("minecraft:block.stone.hit"),
                gun.selectSound(parse("minecraft:stone"), tag -> false).sound());
        assertEquals(parse("minecraft:block.wood.hit"),
                gun.selectSound(parse("minecraft:birch_planks"),
                        tag -> tag.equals(parse("minecraft:planks"))).sound());
        assertEquals(parse("minecraft:block.wood.hit"),
                gun.selectSound(parse("minecraft:diamond_block"), tag -> false).sound());
    }

    @Test
    void explicitEnableOverridesInheritedDisable() {
        BulletHoleSoundConfig disabledPack = new BulletHoleSoundConfig(false,
                new BulletHoleSoundConfig.SoundRef(parse("minecraft:block.stone.hit")),
                java.util.Map.of(), java.util.Map.of());
        BulletHoleSoundConfig gun = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        { "enabled": true, "default": "minecraft:block.glass.hit" }
                        """).getAsJsonObject()),
                disabledPack);

        assertTrue(gun.isEnabled());
        assertEquals(parse("minecraft:block.glass.hit"),
                gun.selectSound(parse("minecraft:diamond_block"), tag -> false).sound());
    }

    @Test
    void invalidSoundIdsAreIgnoredWithoutCrashingOrDisablingTheFeature() {
        BulletHoleSoundConfig sounds = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        {
                          "default": "../not/a/sound",
                          "surface_sounds": {
                            "minecraft:stone": "bad id",
                            "minecraft:iron_block": "mypack:not_yet_registered"
                          }
                        }
                        """).getAsJsonObject()),
                BulletHoleSoundConfig.BUILT_IN_DEFAULTS);

        assertTrue(sounds.isEnabled());
        // Invalid default and map value inherit/are skipped.
        assertEquals(BulletHoleDefaults.DEFAULT_SOUND,
                sounds.getDefaultSound().sound());
        assertEquals(parse("minecraft:block.stone.hit"),
                sounds.selectSound(parse("minecraft:stone"), tag -> false).sound());
        // Syntactically valid custom ids are retained for future registration.
        assertEquals(parse("mypack:not_yet_registered"),
                sounds.selectSound(parse("minecraft:iron_block"), tag -> false).sound());
    }

    @Test
    void unavailableSelectedSoundFallsBackToRegisteredDefault() {
        BulletHoleSoundConfig.SoundRef selected = new BulletHoleSoundConfig.SoundRef(
                parse("mypack:missing"));
        BulletHoleSoundConfig.SoundRef fallback = new BulletHoleSoundConfig.SoundRef(
                parse("minecraft:block.stone.hit"));

        assertEquals(fallback, BulletHoleSoundConfig.resolveAvailable(selected,
                fallback, id -> id.equals(fallback.sound())));
        assertEquals(BulletHoleSoundConfig.SoundRef.DISABLED,
                BulletHoleSoundConfig.resolveAvailable(selected, null, id -> false));
        assertEquals(BulletHoleSoundConfig.SoundRef.DISABLED,
                BulletHoleSoundConfig.resolveAvailable(
                        BulletHoleSoundConfig.SoundRef.DISABLED, fallback, id -> true));
        assertEquals(selected, BulletHoleSoundConfig.resolveAvailable(selected,
                fallback, id -> true));
    }

    @Test
    void impactVolumeDefaultsToTheConfiguredDefaultAndParsesWithoutUpperClamp() {
        BulletHoleSoundConfig defaults = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("{}")
                        .getAsJsonObject()),
                BulletHoleSoundConfig.BUILT_IN_DEFAULTS);
        assertEquals(BulletHoleDefaults.IMPACT_VOLUME, defaults.getImpactVolume(), 0.0001F);

        BulletHoleSoundConfig parsed = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        { "impact_volume": 0.5 }
                        """).getAsJsonObject()),
                BulletHoleSoundConfig.BUILT_IN_DEFAULTS);
        assertEquals(0.5F, parsed.getImpactVolume(), 0.0001F);

        BulletHoleSoundConfig loud = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        { "impact_volume": 2.5 }
                        """).getAsJsonObject()),
                BulletHoleSoundConfig.BUILT_IN_DEFAULTS);
        assertEquals(2.5F, loud.getImpactVolume(), 0.0001F);

        BulletHoleSoundConfig clipped = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        { "impact_volume": -0.25 }
                        """).getAsJsonObject()),
                BulletHoleSoundConfig.BUILT_IN_DEFAULTS);
        assertEquals(0.0F, clipped.getImpactVolume(), 0.0001F);
    }

    @Test
    void engineVolumeNeverExtendsTheAttenuationDistance() {
        // Below 1 the configured volume still controls loudness.
        assertEquals(0.0F, BulletHoleSoundConfig.engineVolume(-1.0F), 0.0001F);
        assertEquals(0.35F, BulletHoleSoundConfig.engineVolume(0.35F), 0.0001F);
        assertEquals(1.0F, BulletHoleSoundConfig.engineVolume(1.0F), 0.0001F);
        // Above 1 SoundEngine clamps gain anyway; the instance volume must not
        // inflate max(volume, 1) * attenuationDistance either.
        assertEquals(1.0F, BulletHoleSoundConfig.engineVolume(1.5F), 0.0001F);
        assertEquals(1.0F, BulletHoleSoundConfig.engineVolume(1.9F), 0.0001F);
        assertEquals(1.0F, BulletHoleSoundConfig.engineVolume(100.0F), 0.0001F);
        assertEquals(0.0F, BulletHoleSoundConfig.engineVolume(Float.NaN), 0.0001F);
        assertEquals(0.0F, BulletHoleSoundConfig.engineVolume(Float.POSITIVE_INFINITY),
                0.0001F);
    }

    @Test
    void impactVolumeInheritsAndMergesIndependently() {
        BulletHoleSoundConfig pack = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        { "impact_volume": 0.35 }
                        """).getAsJsonObject()),
                BulletHoleSoundConfig.BUILT_IN_DEFAULTS);
        assertEquals(0.35F, pack.getImpactVolume(), 0.0001F);

        BulletHoleSoundConfig inherited = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.NONE, pack);
        assertEquals(0.35F, inherited.getImpactVolume(), 0.0001F);

        BulletHoleSoundConfig overridden = BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(JsonParser.parseString("""
                        { "impact_volume": 0.75 }
                        """).getAsJsonObject()),
                pack);
        assertEquals(0.75F, overridden.getImpactVolume(), 0.0001F);
        assertTrue(overridden.isEnabled());
    }

    @Test
    void physicalOggLocationsResolveToResourceAndSoundIds() {
        Identifier configured = parse("pointblankextra:sounds/bullet_holes/wood.ogg");
        assertTrue(BulletHoleSoundConfig.isPhysicalOgg(configured));
        assertEquals(parse("pointblankextra:sounds/bullet_holes/wood.ogg"),
                BulletHoleSoundConfig.physicalResourceId(configured));
        assertEquals(parse("pointblankextra:bullet_holes/wood"),
                BulletHoleSoundConfig.physicalSoundLocation(configured));

        Identifier shortForm = parse("mypack:bullet_holes/wood.ogg");
        assertEquals(parse("mypack:sounds/bullet_holes/wood.ogg"),
                BulletHoleSoundConfig.physicalResourceId(shortForm));
        assertEquals(parse("mypack:bullet_holes/wood"),
                BulletHoleSoundConfig.physicalSoundLocation(shortForm));

        assertFalse(BulletHoleSoundConfig.isPhysicalOgg(
                parse("minecraft:block.wood.hit")));
        assertNull(BulletHoleSoundConfig.physicalResourceId(
                parse("minecraft:block.wood.hit")));
        assertNull(BulletHoleSoundConfig.physicalSoundLocation(
                parse("minecraft:block.wood.hit")));
    }

    @Test
    void soundIdsAreRegistryIdsNotFilesystemPaths() {
        assertEquals(parse("minecraft:block.wood.hit"),
                BulletHoleSoundConfig.parseSoundId("block.wood.hit"));
        assertEquals(parse("mypack:custom/impact"),
                BulletHoleSoundConfig.parseSoundId("mypack:custom/impact"));
        assertNotNull(BulletHoleSoundConfig.parseSoundId("minecraft:block.glass.hit"));
        assertNull(BulletHoleSoundConfig.parseSoundId("mypack:../bad"));
    }

    @Test
    void maxDistanceDefaultsAndIsIndependentOfImpactVolume() {
        BulletHoleSoundConfig defaults = BulletHoleSoundConfig.BUILT_IN_DEFAULTS;
        assertEquals(BulletHoleDefaults.MAX_SOUND_DISTANCE,
                defaults.getMaxDistance(), 0.0001F);

        BulletHoleSoundConfig loud = resolveConfig(
                "{ \"impact_volume\": 50.0 }", defaults);
        assertEquals(50.0F, loud.getImpactVolume(), 0.0001F);
        assertEquals(defaults.getMaxDistance(), loud.getMaxDistance(), 0.0001F);
    }

    @Test
    void maxDistanceParsesAliasesMergesAndClamps() {
        BulletHoleSoundConfig pack = resolveConfig(
                "{ \"max_distance\": 48 }", BulletHoleSoundConfig.BUILT_IN_DEFAULTS);
        assertEquals(48.0F, pack.getMaxDistance(), 0.0001F);

        assertEquals(48.0F, resolveConfig("{}", pack).getMaxDistance(), 0.0001F);
        assertEquals(8.0F, resolveConfig("{ \"maxDistance\": 8 }", pack)
                .getMaxDistance(), 0.0001F);
        assertEquals(8.0F, resolveConfig(
                "{ \"max_distance\": 8, \"impact_volume\": 1.9 }", pack)
                .getMaxDistance(), 0.0001F);

        assertEquals(BulletHoleSoundConfig.MIN_MAX_DISTANCE,
                resolveConfig("{ \"max_distance\": -10 }", pack)
                        .getMaxDistance(), 0.0001F);
        assertEquals(BulletHoleSoundConfig.MAX_MAX_DISTANCE,
                resolveConfig("{ \"max_distance\": 10000 }", pack)
                        .getMaxDistance(), 0.0001F);
        assertEquals(BulletHoleDefaults.MAX_SOUND_DISTANCE,
                resolveConfig("{ \"max_distance\": \"nope\" }",
                        BulletHoleSoundConfig.BUILT_IN_DEFAULTS)
                        .getMaxDistance(), 0.0001F);
        assertEquals(BulletHoleSoundConfig.DEFAULT_MAX_DISTANCE,
                BulletHoleSoundConfig.clampMaxDistance(Float.NaN), 0.0001F);
    }

    @Test
    void distanceGainFadesToZeroAtTheConfiguredLimit() {
        assertEquals(1.0F, BulletHoleSoundConfig.distanceGain(0.0D, 16.0F), 0.0001F);
        assertEquals(0.5F, BulletHoleSoundConfig.distanceGain(8.0D, 16.0F), 0.0001F);
        assertEquals(0.25F, BulletHoleSoundConfig.distanceGain(12.0D, 16.0F), 0.0001F);
        assertEquals(0.0F, BulletHoleSoundConfig.distanceGain(16.0D, 16.0F), 0.0001F);
        assertEquals(0.0F, BulletHoleSoundConfig.distanceGain(100.0D, 16.0F), 0.0001F);
        // The distance gain has no volume input, so impact_volume cannot
        // change this curve or the audible radius.
        assertEquals(0.5F, BulletHoleSoundConfig.distanceGain(8.0D, 16.0F), 0.0001F);
    }

    private static BulletHoleSoundConfig resolveConfig(String json,
                                                       BulletHoleSoundConfig base) {
        return BulletHoleSoundConfig.resolve(
                BulletHoleSoundConfig.Overrides.fromJson(
                        JsonParser.parseString(json).getAsJsonObject()),
                base);
    }

    private static Identifier parse(String value) {
        return Identifier.parse(value);
    }
}

