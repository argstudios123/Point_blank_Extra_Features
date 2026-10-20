package com.arg.pbextra.bullethole;

import com.arg.pbextra.data.WeaponExtraData;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * End-to-end checks for the centralized defaults and for the merge rules that
 * apply them to a gun through its normal weapon JSON.
 */
class BulletHoleDefaultsTest {

    @Test
    void centralizedDefaultsAreBuiltFromTheDefaultsClass() {
        BulletHoleConfig config = BulletHoleConfig.BUILT_IN_DEFAULTS;
        assertTrue(config.isEnabled());
        assertEquals(BulletHoleDefaults.DEFAULT_TEXTURE, config.getDefaultTexture());
        assertEquals(BulletHoleDefaults.SIZE, config.getSize(), 0.0001F);
        assertEquals(BulletHoleDefaults.LIFETIME_TICKS, config.getLifetimeTicks());
        assertEquals(BulletHoleDefaults.MAX_PER_CHUNK, config.getMaxPerChunk());
        assertEquals(BulletHoleDefaults.MAX_BLOCK_PARTICLES,
                config.getMaxBlockParticles());

        assertEquals(BulletHoleDefaults.surfaceTextures(), config.getSurfaceTextures());
        assertEquals(BulletHoleDefaults.WOOD_TEXTURE,
                config.selectTexture(parse("minecraft:oak_log"),
                        parse("minecraft:logs")::equals));
        assertEquals(BulletHoleDefaults.WOOD_TEXTURE,
                config.selectTexture(parse("minecraft:birch_planks"),
                        parse("minecraft:planks")::equals));

        BulletHoleSoundConfig sounds = config.getSounds();
        assertTrue(sounds.isEnabled());
        assertEquals(BulletHoleDefaults.DEFAULT_SOUND, sounds.getDefaultSound().sound());
        assertEquals(BulletHoleDefaults.IMPACT_VOLUME, sounds.getImpactVolume(), 0.0001F);

        // Every built-in exact mapping is present with the centralized sound.
        for (Map.Entry<Identifier, Identifier> entry
                : BulletHoleDefaults.surfaceSounds().entrySet()) {
            assertEquals(entry.getValue(),
                    sounds.getSurfaceSounds().get(entry.getKey()).sound());
        }
        // Every built-in tag mapping is present with the centralized sound.
        for (Map.Entry<Identifier, Identifier> entry
                : BulletHoleDefaults.tagSounds().entrySet()) {
            BulletHoleSoundConfig.TagSound configured = sounds.getTagSounds().stream()
                    .filter(tag -> tag.tag().equals(entry.getKey()))
                    .findFirst().orElseThrow();
            assertEquals(entry.getValue(), configured.sound().sound());
        }
    }

    @Test
    void weaponWithoutABulletHolesEntryInheritsEveryDefault() {
        BulletHoleConfig config = weapon("""
                { "name": "gun_without_entry",
                  "features": [ { "type": "aiming", "enabled": true } ] }
                """);

        assertTrue(config.isEnabled());
        assertEquals(BulletHoleConfig.DEFAULT_TEXTURE, config.getDefaultTexture());
        assertEquals(BulletHoleConfig.DEFAULT_SIZE, config.getSize(), 0.0001F);
        assertEquals(BulletHoleConfig.DEFAULT_LIFETIME_TICKS, config.getLifetimeTicks());
        assertEquals(BulletHoleConfig.DEFAULT_MAX_PER_CHUNK, config.getMaxPerChunk());
        assertEquals(BulletHoleConfig.DEFAULT_MAX_BLOCK_PARTICLES,
                config.getMaxBlockParticles());
        assertEquals(BulletHoleConfig.WOOD_TEXTURE,
                config.getSurfaceTextures().get(parse("minecraft:oak_planks")));

        assertTrue(config.getSounds().isEnabled());
        assertEquals(BulletHoleDefaults.DEFAULT_SOUND,
                config.getSounds().getDefaultSound().sound());
        assertEquals(BulletHoleDefaults.IMPACT_VOLUME,
                config.getSounds().getImpactVolume(), 0.0001F);
    }


    @Test
    void partialWeaponConfigurationInheritsOmittedDefaults() {
        BulletHoleConfig config = weapon("""
                {
                  "name": "partial_gun",
                  "features": [
                    {
                      "type": "bulletHoles",
                      "bullet_holes": {
                        "size": 0.3,
                        "max_block_particles": 7,
                        "surface_textures": {
                          "minecraft:oak_planks": "mypack:textures/bullet_holes/custom_wood.png"
                        },
                        "soundsPlayOnperBlockHit": {
                          "surface_sounds": {
                            "minecraft:stone": "mypack:custom_stone"
                          }
                        }
                      }
                    }
                  ]
                }
                """);

        assertEquals(0.3F, config.getSize(), 0.0001F);
        assertEquals(7, config.getMaxBlockParticles());
        // Omitted scalar properties still come from the defaults.
        assertEquals(BulletHoleConfig.DEFAULT_LIFETIME_TICKS, config.getLifetimeTicks());
        assertEquals(BulletHoleConfig.DEFAULT_MAX_PER_CHUNK, config.getMaxPerChunk());
        assertEquals(BulletHoleConfig.DEFAULT_TEXTURE, config.getDefaultTexture());

        // Overriding one texture keeps every other default texture mapping.
        assertEquals(parse("mypack:textures/bullet_holes/custom_wood.png"),
                config.getSurfaceTextures().get(parse("minecraft:oak_planks")));
        assertEquals(BulletHoleConfig.STONE_TEXTURE,
                config.getSurfaceTextures().get(parse("minecraft:stone")));
        assertEquals(BulletHoleConfig.METAL_TEXTURE,
                config.getSurfaceTextures().get(parse("minecraft:iron_block")));

        // Overriding one sound keeps the default impact sound, volume and every
        // other material sound mapping.
        BulletHoleSoundConfig sounds = config.getSounds();
        assertTrue(sounds.isEnabled());
        assertEquals(BulletHoleDefaults.DEFAULT_SOUND,
                sounds.getDefaultSound().sound());
        assertEquals(BulletHoleDefaults.IMPACT_VOLUME, sounds.getImpactVolume(), 0.0001F);
        assertSound(sounds, "minecraft:stone", "mypack:custom_stone");
        for (Map.Entry<Identifier, Identifier> entry
                : BulletHoleDefaults.surfaceSounds().entrySet()) {
            if (entry.getKey().equals(parse("minecraft:stone"))) {
                continue;
            }
            assertEquals(entry.getValue(),
                    sounds.getSurfaceSounds().get(entry.getKey()).sound());
        }
    }

    @Test
    void customDefaultSoundReplacesOnlyTheFallback() {
        BulletHoleConfig config = weapon("""
                {
                  "name": "custom_sound_gun",
                  "features": [
                    {
                      "type": "bulletHoles",
                      "bullet_holes": {
                        "soundsPlayOnperBlockHit": {
                          "default": "mypack:custom_impact",
                          "impact_volume": 0.7,
                          "tag_sounds": {
                            "minecraft:logs": "mypack:custom_wood"
                          }
                        }
                      }
                    }
                  ]
                }
                """);

        BulletHoleSoundConfig sounds = config.getSounds();
        assertEquals(parse("mypack:custom_impact"), sounds.getDefaultSound().sound());
        assertEquals(0.7F, sounds.getImpactVolume(), 0.0001F);
        // A non-overridden default surface sound and tag survives the custom
        // default and the custom logs tag.
        for (Map.Entry<Identifier, Identifier> entry
                : BulletHoleDefaults.surfaceSounds().entrySet()) {
            assertEquals(entry.getValue(),
                    sounds.getSurfaceSounds().get(entry.getKey()).sound());
        }
        assertEquals(BulletHoleDefaults.tagSounds().get(parse("minecraft:planks")),
                sounds.selectSound(parse("minecraft:birch_planks"),
                        parse("minecraft:planks")::equals).sound());
        assertEquals(parse("mypack:custom_wood"),
                sounds.selectSound(parse("minecraft:oak_log"),
                        parse("minecraft:logs")::equals).sound());
    }

    @Test
    void disablingImpactSoundsKeepsBulletHolesEnabled() {
        BulletHoleConfig config = weapon("""
                {
                  "name": "silent_gun",
                  "features": [
                    {
                      "type": "bulletHoles",
                      "bullet_holes": {
                        "enabled": true,
                        "soundsPlayOnperBlockHit": { "enabled": false }
                      }
                    }
                  ]
                }
                """);

        assertTrue(config.isEnabled());
        assertFalse(config.getSounds().isEnabled());
        assertNull(config.getSounds().selectSound(parse("minecraft:stone"), tag -> true));
    }

    @Test
    void explicitlyDisablingBulletHolesIsRespected() {
        BulletHoleConfig config = weapon("""
                {
                  "name": "no_holes_gun",
                  "features": [
                    { "type": "bulletHoles", "bullet_holes": { "enabled": false } }
                  ]
                }
                """);

        assertFalse(config.isEnabled());
        // The visual switch does not implicitly disable the sound object.
        assertTrue(config.getSounds().isEnabled());
    }

    private static BulletHoleConfig weapon(String json) {
        return WeaponExtraData.fromJson(
                        JsonParser.parseString(json).getAsJsonObject())
                .getBulletHoles();
    }

    private static void assertSound(BulletHoleSoundConfig sounds, String blockId,
                                    String soundId) {
        assertEquals(parse(soundId),
                sounds.getSurfaceSounds().get(parse(blockId)).sound());
    }

    private static Identifier parse(String value) {
        return Identifier.parse(value);
    }
}

