package com.arg.pbextra.bullethole;

import com.arg.pbextra.data.WeaponExtraData;
import com.arg.pbextra.integration.PointBlankJsonIntegration;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/** End-to-end discovery test for content-pack bullet-hole defaults. */
class BulletHolePackIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void packConfigAppliesToGunsWithoutTheirOwnEntry() throws IOException {
        Path pack = newPack("applied_pack");
        write(pack.resolve("bullet_holes.json"), """
                {
                  "enabled": true,
                  "size": 0.3,
                  "default_texture": "mypack:textures/bullet_holes/stone.png",
                  "tag_textures": {
                    "minecraft:logs": "mypack:textures/bullet_holes/wood.png",
                    "c:glass_blocks": "mypack:textures/bullet_holes/glass.png",
                    "minecraft:dirt": "mypack:textures/bullet_holes/dirt.png",
                    "minecraft:sand": "mypack:textures/bullet_holes/sand.png"
                  }
                }
                """);
        write(pack.resolve("assets/pointblank/items/gun_default.json"),
                "{ \"name\": \"gun_default\" }");

        Map<String, WeaponExtraData> out = scan();

        BulletHoleConfig config = out.get("gun_default").getBulletHoles();
        assertTrue(config.isEnabled());
        assertEquals(0.3F, config.getSize(), 0.0001F);
        assertEquals(parse("mypack:textures/bullet_holes/stone.png"),
                config.getDefaultTexture());
        assertEquals(parse("mypack:textures/bullet_holes/wood.png"),
                config.selectTexture(parse("minecraft:oak_log"),
                        parse("minecraft:logs")::equals));
        assertEquals(parse("mypack:textures/bullet_holes/glass.png"),
                config.selectTexture(parse("minecraft:glass"),
                        parse("c:glass_blocks")::equals));
        assertEquals(parse("mypack:textures/bullet_holes/dirt.png"),
                config.selectTexture(parse("minecraft:dirt"),
                        parse("minecraft:dirt")::equals));
        assertEquals(parse("mypack:textures/bullet_holes/sand.png"),
                config.selectTexture(parse("minecraft:red_sand"),
                        parse("minecraft:sand")::equals));
    }

    @Test
    void gunEntryOverridesOnlyExplicitValues() throws IOException {
        Path pack = newPack("override_pack");
        write(pack.resolve("bullet_holes.json"), """
                {
                  "size": 0.2,
                  "lifetime": 2400,
                  "surface_textures": {
                    "minecraft:stone": "mypack:textures/bullet_holes/stone.png"
                  },
                  "tag_textures": {
                    "c:glass_blocks": "mypack:textures/bullet_holes/pack_glass.png",
                    "minecraft:dirt": "mypack:textures/bullet_holes/pack_dirt.png"
                  }
                }
                """);
        write(pack.resolve("assets/pointblank/items/gun_override.json"), """
                {
                  "name": "gun_override",
                  "features": [
                    {
                      "type": "bulletHoles",
                      "bullet_holes": {
                        "size": 0.4,
                        "surface_textures": {
                          "minecraft:stone": "mypack:textures/bullet_holes/metal.png"
                        },
                        "tag_textures": {
                          "c:glass_blocks": "mypack:textures/bullet_holes/gun_glass.png"
                        }
                      }
                    }
                  ]
                }
                """);

        BulletHoleConfig config = scan().get("gun_override").getBulletHoles();

        assertEquals(0.4F, config.getSize(), 0.0001F);
        assertEquals(2400, config.getLifetimeTicks());
        assertEquals(parse("mypack:textures/bullet_holes/metal.png"),
                config.getSurfaceTextures().get(parse("minecraft:stone")));
        assertEquals(parse("mypack:textures/bullet_holes/gun_glass.png"),
                config.selectTexture(parse("minecraft:glass"),
                        parse("c:glass_blocks")::equals));
        // The gun did not override dirt, so the pack-wide dirt mapping survives.
        assertEquals(parse("mypack:textures/bullet_holes/pack_dirt.png"),
                config.selectTexture(parse("minecraft:dirt"),
                        parse("minecraft:dirt")::equals));
    }

    @Test
    void packDisableWinsUnlessTheGunExplicitlyEnablesBulletHoles() throws IOException {
        Path pack = newPack("disabled_pack");
        write(pack.resolve("bullet_holes.json"), "{ \"enabled\": false, \"size\": 0.2 }");
        write(pack.resolve("assets/pointblank/items/gun_disabled.json"),
                "{ \"name\": \"gun_disabled\" }");
        write(pack.resolve("assets/pointblank/items/gun_reenabled.json"), """
                {
                  "name": "gun_reenabled",
                  "features": [
                    { "type": "bulletHoles", "bullet_holes": { "enabled": true } }
                  ]
                }
                """);

        Map<String, WeaponExtraData> out = scan();

        assertFalse(out.get("gun_disabled").getBulletHoles().isEnabled());
        BulletHoleConfig reenabled = out.get("gun_reenabled").getBulletHoles();
        assertTrue(reenabled.isEnabled());
        assertEquals(0.2F, reenabled.getSize(), 0.0001F);
    }

    @Test
    void packWithoutAConfigKeepsBuiltInDefaults() throws IOException {
        Path pack = newPack("plain_pack");
        write(pack.resolve("assets/pointblank/items/plain_gun.json"),
                "{ \"name\": \"plain_gun\" }");

        BulletHoleConfig config = scan().get("plain_gun").getBulletHoles();

        assertTrue(config.isEnabled());
        assertEquals(BulletHoleConfig.DEFAULT_TEXTURE, config.getDefaultTexture());
        assertEquals(BulletHoleConfig.DEFAULT_SIZE, config.getSize(), 0.0001F);
        // The built-in material tags are part of the effective configuration,
        // not just a declaration on the built-in object.
        assertEquals(BulletHoleConfig.GLASS_TEXTURE,
                config.selectTexture(parse("minecraft:glass"),
                        parse("c:glass_blocks")::equals));
        assertEquals(BulletHoleConfig.DIRT_TEXTURE,
                config.selectTexture(parse("minecraft:coarse_dirt"),
                        parse("minecraft:dirt")::equals));
        assertEquals(BulletHoleConfig.SAND_TEXTURE,
                config.selectTexture(parse("minecraft:sand"),
                        parse("minecraft:sand")::equals));
    }

    @Test
    void packSoundDefaultsAreInheritedAndGunSoundOverridesMerge() throws IOException {
        Path pack = newPack("sound_pack");
        write(pack.resolve("bullet_holes.json"), """
                {
                  "soundsPlayOnperBlockHit": {
                    "enabled": true,
                    "default": "minecraft:block.stone.hit",
                    "impact_volume": 0.4,
                    "surface_sounds": {
                      "minecraft:stone": "minecraft:block.stone.hit",
                      "minecraft:iron_block": "minecraft:block.metal.hit"
                    },
                    "tag_sounds": {
                      "minecraft:logs": "minecraft:block.wood.hit"
                    }
                  }
                }
                """);
        write(pack.resolve("assets/pointblank/items/sound_gun.json"), """
                {
                  "name": "sound_gun",
                  "features": [
                    {
                      "type": "bulletHoles",
                      "bullet_holes": {
                        "soundsPlayOnperBlockHit": {
                          "surface_sounds": {
                            "minecraft:iron_block": "mypack:custom_metal"
                          }
                        }
                      }
                    }
                  ]
                }
                """);

        BulletHoleSoundConfig sounds = scan().get("sound_gun")
                .getBulletHoles().getSounds();

        assertTrue(sounds.isEnabled());
        assertEquals(0.4F, sounds.getImpactVolume(), 0.0001F);
        assertEquals(parse("mypack:custom_metal"),
                sounds.selectSound(parse("minecraft:iron_block"), tag -> false).sound());
        assertEquals(parse("minecraft:block.stone.hit"),
                sounds.selectSound(parse("minecraft:stone"), tag -> false).sound());
        assertEquals(parse("minecraft:block.wood.hit"),
                sounds.selectSound(parse("minecraft:oak_log"),
                        tag -> tag.equals(parse("minecraft:logs"))).sound());
        assertEquals(parse("minecraft:block.stone.hit"),
                sounds.selectSound(parse("minecraft:diamond_block"),
                        tag -> false).sound());
    }

    @Test
    void packAndGunBlockParticleLimitsMergeAndInherit() throws IOException {
        Path pack = newPack("particle_pack");
        write(pack.resolve("bullet_holes.json"),
                "{ \"max_block_particles\": 9 }");
        write(pack.resolve("assets/pointblank/items/gun_inherits.json"),
                "{ \"name\": \"gun_inherits\" }");
        write(pack.resolve("assets/pointblank/items/gun_overrides.json"), """
                {
                  "name": "gun_overrides",
                  "features": [
                    {
                      "type": "bulletHoles",
                      "bullet_holes": { "max_block_particles": 0 }
                    }
                  ]
                }
                """);

        Map<String, WeaponExtraData> out = scan();

        // A gun without the field inherits the pack-wide value.
        assertEquals(9, out.get("gun_inherits").getBulletHoles().getMaxBlockParticles());
        // An explicit zero disables particles for that gun.
        assertEquals(0, out.get("gun_overrides").getBulletHoles().getMaxBlockParticles());
    }

    @Test
    void packAndGunMaxSoundDistanceMergeAndInherit() throws IOException {
        Path pack = newPack("distance_pack");
        write(pack.resolve("bullet_holes.json"), """
                {
                  "soundsPlayOnperBlockHit": { "max_distance": 40 }
                }
                """);
        write(pack.resolve("assets/pointblank/items/gun_distance_inherits.json"),
                "{ \"name\": \"gun_distance_inherits\" }");
        write(pack.resolve("assets/pointblank/items/gun_distance_overrides.json"), """
                {
                  "name": "gun_distance_overrides",
                  "features": [
                    {
                      "type": "bulletHoles",
                      "bullet_holes": {
                        "soundsPlayOnperBlockHit": { "max_distance": 12 }
                      }
                    }
                  ]
                }
                """);

        Map<String, WeaponExtraData> out = scan();

        assertEquals(40.0F, out.get("gun_distance_inherits").getBulletHoles()
                .getSounds().getMaxDistance(), 0.0001F);
        assertEquals(12.0F, out.get("gun_distance_overrides").getBulletHoles()
                .getSounds().getMaxDistance(), 0.0001F);
    }

    private Map<String, WeaponExtraData> scan() {
        Map<String, WeaponExtraData> out = new HashMap<>();
        Map<String, String> failures = new HashMap<>();
        assertTrue(PointBlankJsonIntegration.scanExtensions(tempDir, out, failures));
        assertTrue(failures.isEmpty(), () -> failures.toString());
        return out;
    }

    private Path newPack(String name) throws IOException {
        Path pack = tempDir.resolve(name);
        Files.createDirectories(pack.resolve("assets/pointblank/items"));
        return pack;
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private static Identifier parse(String value) {
        return Identifier.parse(value);
    }
}
