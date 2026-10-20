package com.arg.pbextra.bullethole;

import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class BulletHoleConfigTest {

    @Test
    void missingWeaponEntryInheritsEnabledBuiltInDefaults() {
        BulletHoleConfig config = BulletHoleConfig.resolve(
                BulletHoleConfig.Overrides.NONE, BulletHoleConfig.BUILT_IN_DEFAULTS);

        assertTrue(config.isEnabled());
        assertEquals(BulletHoleConfig.DEFAULT_TEXTURE, config.getDefaultTexture());
        assertEquals(BulletHoleConfig.DEFAULT_SIZE, config.getSize());
        assertEquals(BulletHoleConfig.DEFAULT_LIFETIME_TICKS, config.getLifetimeTicks());
        assertEquals(BulletHoleConfig.DEFAULT_MAX_PER_CHUNK, config.getMaxPerChunk());
    }

    @Test
    void packDisableIsInheritedUnlessTheGunExplicitlyEnablesIt() {
        BulletHoleConfig pack = BulletHoleConfig.resolve(
                BulletHoleConfig.Overrides.fromJson(JsonParser.parseString("""
                        { "enabled": false, "size": 0.2 }
                        """).getAsJsonObject()),
                BulletHoleConfig.BUILT_IN_DEFAULTS);

        assertFalse(BulletHoleConfig.resolve(BulletHoleConfig.Overrides.NONE, pack).isEnabled());

        BulletHoleConfig gunEnabled = BulletHoleConfig.resolve(
                BulletHoleConfig.Overrides.fromJson(JsonParser.parseString("""
                        { "type": "bulletHoles", "bullet_holes": { "enabled": true } }
                        """).getAsJsonObject()),
                pack);
        assertTrue(gunEnabled.isEnabled());
        // Unspecified values still come from the pack.
        assertEquals(0.2F, gunEnabled.getSize(), 0.0001F);
    }

    @Test
    void gunOverridesOnlyTheValuesItSuppliesAndMergesTextureMaps() {
        BulletHoleConfig pack = BulletHoleConfig.resolve(
                BulletHoleConfig.Overrides.fromJson(JsonParser.parseString("""
                        {
                          "default_texture": "mypack:textures/bullet_holes/stone.png",
                          "surface_textures": { "minecraft:stone": "mypack:textures/bullet_holes/stone.png" },
                          "tag_textures": { "minecraft:logs": "mypack:textures/bullet_holes/wood.png" }
                        }
                        """).getAsJsonObject()),
                BulletHoleConfig.BUILT_IN_DEFAULTS);

        BulletHoleConfig gun = BulletHoleConfig.resolve(
                BulletHoleConfig.Overrides.fromJson(JsonParser.parseString("""
                        {
                          "type": "bulletHoles",
                          "bullet_holes": {
                            "size": 0.25,
                            "surface_textures": { "minecraft:stone": "mypack:textures/bullet_holes/metal.png" }
                          }
                        }
                        """).getAsJsonObject()),
                pack);

        assertEquals(0.25F, gun.getSize(), 0.0001F);
        assertEquals(parse("mypack:textures/bullet_holes/stone.png"), gun.getDefaultTexture());
        assertEquals(parse("mypack:textures/bullet_holes/metal.png"),
                gun.getSurfaceTextures().get(parse("minecraft:stone")));
        // The pack's tag map is inherited and overrides the built-in entry.
        Identifier logsTag = parse("minecraft:logs");
        assertEquals(parse("mypack:textures/bullet_holes/wood.png"),
                gun.selectTexture(parse("minecraft:birch_log"), logsTag::equals));
    }

    @Test
    void tagSelectionIsDeterministicAndLexicographic() {
        BulletHoleConfig config = new BulletHoleConfig(true,
                BulletHoleConfig.DEFAULT_TEXTURE, 0.12F, 1200, 128,
                Map.of(),
                Map.of(parse("minecraft:planks"), parse("mypack:textures/bullet_holes/planks.png"),
                        parse("minecraft:logs"), parse("mypack:textures/bullet_holes/logs.png")));

        List<BulletHoleConfig.TagTexture> tags = config.getTagTextures();
        assertEquals(parse("minecraft:logs"), tags.get(0).tag());
        assertEquals(parse("minecraft:planks"), tags.get(1).tag());

        // Both tags match; the lexicographically first tag wins.
        assertEquals(parse("mypack:textures/bullet_holes/logs.png"),
                config.selectTexture(parse("minecraft:oak_log"), tag -> true));
    }
    @Test
    void exactSurfaceTextureBeatsTags() {
        BulletHoleConfig config = new BulletHoleConfig(true,
                BulletHoleConfig.DEFAULT_TEXTURE, 0.12F, 1200, 128,
                Map.of(parse("minecraft:oak_planks"), parse("mypack:textures/bullet_holes/exact.png")),
                Map.of(parse("minecraft:planks"), parse("mypack:textures/bullet_holes/tag.png")));

        assertEquals(parse("mypack:textures/bullet_holes/exact.png"),
                config.selectTexture(parse("minecraft:oak_planks"), tag -> true));
        assertEquals(parse("mypack:textures/bullet_holes/tag.png"),
                config.selectTexture(parse("minecraft:birch_planks"), tag -> true));
        assertEquals(BulletHoleConfig.DEFAULT_TEXTURE,
                config.selectTexture(parse("minecraft:dirt"), tag -> false));
    }

    @Test
    void builtInMaterialsUseDedicatedTextures() {
        BulletHoleConfig config = BulletHoleConfig.BUILT_IN_DEFAULTS;
        Identifier impermeable = parse("minecraft:impermeable");
        Identifier glassBlocks = parse("c:glass_blocks");
        Identifier glassPanes = parse("c:glass_panes");
        Identifier dirtTag = parse("minecraft:dirt");
        Identifier sandTag = parse("minecraft:sand");

        assertEquals(BulletHoleConfig.GLASS_TEXTURE,
                config.selectTexture(parse("minecraft:glass"), glassBlocks::equals));
        assertEquals(BulletHoleConfig.GLASS_TEXTURE,
                config.selectTexture(parse("minecraft:white_stained_glass"),
                        glassBlocks::equals));
        assertEquals(BulletHoleConfig.GLASS_TEXTURE,
                config.selectTexture(parse("minecraft:glass_pane"), glassPanes::equals));

        assertEquals(BulletHoleConfig.DIRT_TEXTURE,
                config.selectTexture(parse("minecraft:dirt"), dirtTag::equals));
        assertEquals(BulletHoleConfig.DIRT_TEXTURE,
                config.selectTexture(parse("minecraft:coarse_dirt"), dirtTag::equals));
        assertEquals(BulletHoleConfig.DEFAULT_TEXTURE,
                config.selectTexture(parse("minecraft:farmland"), tag -> false));

        assertEquals(BulletHoleConfig.SAND_TEXTURE,
                config.selectTexture(parse("minecraft:sand"), sandTag::equals));
        assertEquals(BulletHoleConfig.SAND_TEXTURE,
                config.selectTexture(parse("minecraft:red_sand"), sandTag::equals));
        assertEquals(BulletHoleConfig.SAND_TEXTURE,
                config.selectTexture(parse("minecraft:suspicious_sand"), sandTag::equals));

        assertEquals(BulletHoleConfig.WOOD_TEXTURE,
                config.selectTexture(parse("minecraft:oak_log"),
                        parse("minecraft:logs")::equals));
        assertEquals(BulletHoleConfig.STONE_TEXTURE,
                config.selectTexture(parse("minecraft:stone_bricks"),
                        parse("minecraft:stone_bricks")::equals));
        assertEquals(BulletHoleConfig.METAL_TEXTURE,
                config.selectTexture(parse("minecraft:iron_block"), tag -> false));
        assertEquals(BulletHoleConfig.DEFAULT_TEXTURE,
                config.selectTexture(parse("minecraft:diamond_block"), tag -> false));

        // A glass-like block outside the common glass tags still falls back to
        // the effective default through the built-in fallback tag.
        assertEquals(BulletHoleConfig.DEFAULT_TEXTURE,
                config.selectTexture(parse("minecraft:glass"), impermeable::equals));
    }

    @Test
    void materialMappingsSurviveInheritanceAndCanBeOverridden() {
        Identifier glassTag = parse("c:glass_blocks");
        Identifier dirtTag = parse("minecraft:dirt");
        Identifier customGlass = parse("mypack:textures/bullet_holes/custom_glass.png");

        BulletHoleConfig pack = BulletHoleConfig.resolve(
                new BulletHoleConfig.Overrides(null,
                        parse("mypack:textures/bullet_holes/pack_default.png"),
                        null, null, null, Map.of(), Map.of()),
                BulletHoleConfig.BUILT_IN_DEFAULTS);
        // Built-in material tags are inherited even when the pack changes the
        // default texture.
        assertEquals(BulletHoleConfig.GLASS_TEXTURE,
                pack.selectTexture(parse("minecraft:glass"), glassTag::equals));
        assertEquals(BulletHoleConfig.DIRT_TEXTURE,
                pack.selectTexture(parse("minecraft:dirt"), dirtTag::equals));

        BulletHoleConfig gun = BulletHoleConfig.resolve(
                new BulletHoleConfig.Overrides(null, null, null, null, null,
                        Map.of(),
                        Map.of(glassTag, customGlass)),
                pack);
        assertEquals(customGlass,
                gun.selectTexture(parse("minecraft:glass"), glassTag::equals));
        assertEquals(BulletHoleConfig.DIRT_TEXTURE,
                gun.selectTexture(parse("minecraft:dirt"), dirtTag::equals));
    }

    @Test
    void exactAndTagOverridesBeatBuiltInMaterialMappings() {
        Identifier glassTag = parse("c:glass_blocks");
        Identifier customTag = parse("mypack:textures/bullet_holes/tag_glass.png");
        Identifier customExact = parse("mypack:textures/bullet_holes/exact_glass.png");

        BulletHoleConfig tagOverride = BulletHoleConfig.resolve(
                new BulletHoleConfig.Overrides(null, null, null, null, null,
                        Map.of(),
                        Map.of(glassTag, customTag)),
                BulletHoleConfig.BUILT_IN_DEFAULTS);
        assertEquals(customTag,
                tagOverride.selectTexture(parse("minecraft:glass"), glassTag::equals));

        BulletHoleConfig exactOverride = BulletHoleConfig.resolve(
                new BulletHoleConfig.Overrides(null, null, null, null, null,
                        Map.of(parse("minecraft:glass"), customExact),
                        Map.of()),
                tagOverride);
        assertEquals(customExact,
                exactOverride.selectTexture(parse("minecraft:glass"), glassTag::equals));
        // The same exact override leaves other glass blocks on the tag texture.
        assertEquals(customTag,
                exactOverride.selectTexture(parse("minecraft:white_stained_glass"),
                        glassTag::equals));
    }

    @Test
    void textureIdentifiersAreNormalisedAndValidated() {
        assertEquals(parse("pointblankextra:textures/bullet_holes/default.png"),
                BulletHoleConfig.parseTexture("textures/bullet_holes/default.png"));
        assertEquals(parse("pointblankextra:textures/bullet_holes/wood.png"),
                BulletHoleConfig.parseTexture("bullet_holes/wood"));
        assertEquals(parse("mypack:textures/custom/hole.png"),
                BulletHoleConfig.parseTexture("mypack:custom/hole"));
        assertNull(BulletHoleConfig.parseTexture("../escape"));
        assertNull(BulletHoleConfig.parseTexture("textures/../escape.png"));
        assertNull(BulletHoleConfig.parseTexture("Upper_Case"));
        assertNull(BulletHoleConfig.parseTexture(null));
    }

    @Test
    void pathologicalValuesAreClamped() {
        BulletHoleConfig config = BulletHoleConfig.resolve(
                new BulletHoleConfig.Overrides(null, null, 50.0F, 1, 100_000,
                        Map.of(), Map.of()),
                BulletHoleConfig.BUILT_IN_DEFAULTS);

        assertEquals(BulletHoleConfig.MAX_SIZE, config.getSize(), 0.0001F);
        assertEquals(BulletHoleConfig.MIN_LIFETIME_TICKS, config.getLifetimeTicks());
        assertEquals(BulletHoleConfig.MAX_MAX_PER_CHUNK, config.getMaxPerChunk());
    }

    @Test
    void exampleFeatureSchemaParsesEveryField() {
        BulletHoleConfig config = BulletHoleConfig.resolve(
                BulletHoleConfig.Overrides.fromJson(JsonParser.parseString("""
                        {
                          "type": "bulletHoles",
                          "bullet_holes": {
                            "enabled": true,
                            "default_texture": "pointblankextra:textures/bullet_holes/default.png",
                            "size": 0.12,
                            "lifetime": 1200,
                            "max_per_chunk": 128,
                            "surface_textures": {
                              "minecraft:oak_planks": "pointblankextra:textures/bullet_holes/wood.png"
                            },
                            "tag_textures": {
                              "minecraft:logs": "pointblankextra:textures/bullet_holes/wood.png"
                            }
                          }
                        }
                        """).getAsJsonObject()),
                BulletHoleConfig.BUILT_IN_DEFAULTS);

        assertTrue(config.isEnabled());
        assertEquals(0.12F, config.getSize(), 0.0001F);
        assertEquals(1200, config.getLifetimeTicks());
        assertEquals(128, config.getMaxPerChunk());
        assertEquals(parse("pointblankextra:textures/bullet_holes/wood.png"),
                config.getSurfaceTextures().get(parse("minecraft:oak_planks")));
    }

    @Test
    void maxBlockParticlesInheritsAndZeroDisablesParticles() {
        assertEquals(BulletHoleDefaults.MAX_BLOCK_PARTICLES,
                resolveParticles("{}").getMaxBlockParticles());
        assertEquals(0, resolveParticles("{ \"max_block_particles\": 0 }")
                .getMaxBlockParticles());
        assertEquals(4, resolveParticles("{ \"max_block_particles\": 4 }")
                .getMaxBlockParticles());
        assertEquals(7, resolveParticles("{ \"maxBlockParticles\": 7 }")
                .getMaxBlockParticles());
    }

    @Test
    void pathologicalBlockParticleValuesAreClamped() {
        assertEquals(BulletHoleConfig.MIN_MAX_BLOCK_PARTICLES,
                resolveParticles("{ \"max_block_particles\": -12 }")
                        .getMaxBlockParticles());
        assertEquals(BulletHoleConfig.MAX_MAX_BLOCK_PARTICLES,
                resolveParticles("{ \"max_block_particles\": 100000 }")
                        .getMaxBlockParticles());
        // An unreadable value is treated as absent and inherits the default.
        assertEquals(BulletHoleDefaults.MAX_BLOCK_PARTICLES,
                resolveParticles("{ \"max_block_particles\": \"not a number\" }")
                        .getMaxBlockParticles());
    }

    private static BulletHoleConfig resolveParticles(String json) {
        return BulletHoleConfig.resolve(
                BulletHoleConfig.Overrides.fromJson(
                        JsonParser.parseString(json).getAsJsonObject()),
                BulletHoleConfig.BUILT_IN_DEFAULTS);
    }

    private static Identifier parse(String value) {
        return Identifier.parse(value);
    }
}

