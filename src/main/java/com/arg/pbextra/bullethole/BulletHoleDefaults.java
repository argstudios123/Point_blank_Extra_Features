package com.arg.pbextra.bullethole;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// this si like the java Class defaults where ic an change for all the guns all by default
public final class BulletHoleDefaults {

    /** Namespace used by every built-in texture, sound event and default id. */
    public static final String MOD_NAMESPACE = "pointblankextra";

    /* ------------------------------------------------------------------ */
    /* Visual feature defaults                                             */
    /* ------------------------------------------------------------------ */

    /** Whether bullet holes are created for a gun that declares no setting. */
    public static final boolean ENABLED = true;

    public static final Identifier DEFAULT_TEXTURE =
            id("textures/bullet_holes/default.png");
    public static final Identifier WOOD_TEXTURE =
            id("textures/bullet_holes/wood.png");
    public static final Identifier STONE_TEXTURE =
            id("textures/bullet_holes/stone.png");
    public static final Identifier METAL_TEXTURE =
            id("textures/bullet_holes/metal.png");
    public static final Identifier GLASS_TEXTURE =
            id("textures/bullet_holes/glass.png");
    public static final Identifier DIRT_TEXTURE =
            id("textures/bullet_holes/dirt.png");
    public static final Identifier SAND_TEXTURE =
            id("textures/bullet_holes/sand.png");

    /** Decal size in blocks. */
    public static final float SIZE = 0.12F;

    /** Decal lifetime in client ticks (20 ticks = 1 second). */
    public static final int LIFETIME_TICKS = 1_200;

    /** Upper decal bound per chunk. */
    public static final int MAX_PER_CHUNK = 256;

    /** Maximum block-debris particles spawned for one bullet impact (0 disables them). */
    public static final int MAX_BLOCK_PARTICLES = 5;

    /**
     * Maximum distance in blocks at which an impact sound can be heard.
     * Matches vanilla's default {@code attenuation_distance}. This is applied
     * independently of {@code impact_volume}, which only controls loudness.
     */
    public static final float MAX_SOUND_DISTANCE = 15.0F;

    /* ------------------------------------------------------------------ */
    /* Impact sound defaults  this si where it starts                     */
    /* ------------------------------------------------------------------ */

    /** Whether material impact sounds play for a gun that declares no setting. */
    public static final boolean SOUNDS_ENABLED = true;

    /**
     * Registry id of the mod's default impact sound. It is backed by
     * {@code assets/pointblankextra/sounds.json} and the OGG files under
     * {@code assets/pointblankextra/sounds/bullet_holes/}; the matching
     * SoundEvent is registered by {@link BulletHoleSoundEvents}.
     */
    public static final String DEFAULT_SOUND_PATH = "bullet_holes.default";
    public static final Identifier DEFAULT_SOUND = id(DEFAULT_SOUND_PATH);

    /** Registry ids of the other built-in material sounds content packs may use. */
    public static final String WOOD_SOUND_PATH = "bullet_holes.wood";
    public static final String GLASS_SOUND_PATH = "bullet_holes.glass";
    public static final String DIRT_SOUND_PATH = "bullet_holes.dirt";
    public static final String METAL_SOUND_PATH = "bullet_holes.metal";

    /** Volume passed to the playback call; values above 1 extend the hearing range. */
    public static final float IMPACT_VOLUME = 1.5F;

    private BulletHoleDefaults() {
    }

    /* ------------------------------------------------------------------ */
    /* Built-in material mappings                                          */
    /* ------------------------------------------------------------------ */

    private static final Map<Identifier, Identifier> SURFACE_TEXTURES = buildSurfaceTextures();
    private static final Map<Identifier, Identifier> TAG_TEXTURES = buildTagTextures();
    private static final Map<Identifier, Identifier> SURFACE_SOUNDS = buildSurfaceSounds();
    private static final Map<Identifier, Identifier> TAG_SOUNDS = buildTagSounds();

    /** Exact block id -> texture. Explicit entries beat tag entries. */
    public static Map<Identifier, Identifier> surfaceTextures() {
        return SURFACE_TEXTURES;
    }

    /** Block tag id -> texture; the lexicographically first matching tag wins. */
    public static Map<Identifier, Identifier> tagTextures() {
        return TAG_TEXTURES;
    }

    /** Exact block id -> sound event id. Explicit entries beat tag entries. */
    public static Map<Identifier, Identifier> surfaceSounds() {
        return SURFACE_SOUNDS;
    }

    /** Block tag id -> sound event id; the lexicographically first matching tag wins. */
    public static Map<Identifier, Identifier> tagSounds() {
        return TAG_SOUNDS;
    }

    private static Map<Identifier, Identifier> buildSurfaceTextures() {
        Map<Identifier, Identifier> textures = new LinkedHashMap<>();
        texture(textures, "minecraft:oak_planks", WOOD_TEXTURE);
        texture(textures, "minecraft:iron_block", METAL_TEXTURE);
        texture(textures, "minecraft:gold_block", METAL_TEXTURE);
        texture(textures, "minecraft:copper_block", METAL_TEXTURE);
        texture(textures, "minecraft:netherite_block", METAL_TEXTURE);
        texture(textures, "minecraft:iron_bars", METAL_TEXTURE);
        texture(textures, "minecraft:stone", STONE_TEXTURE);
        texture(textures, "minecraft:cobblestone", STONE_TEXTURE);
        texture(textures, "minecraft:deepslate", STONE_TEXTURE);
        texture(textures, "minecraft:stone_bricks", STONE_TEXTURE);
        // Glass, dirt and sand are deliberately tag-based so a pack/gun tag
        // override applies to every member of the tag. Unmatched blocks such as
        // farmland remain on the effective default texture.
        return Collections.unmodifiableMap(textures);
    }

    private static Map<Identifier, Identifier> buildTagTextures() {
        Map<Identifier, Identifier> textures = new LinkedHashMap<>();
        texture(textures, "minecraft:logs", WOOD_TEXTURE);
        texture(textures, "minecraft:planks", WOOD_TEXTURE);
        texture(textures, "minecraft:bamboo_blocks", WOOD_TEXTURE);
        texture(textures, "minecraft:stone_bricks", STONE_TEXTURE);
        texture(textures, "minecraft:base_stone_overworld", STONE_TEXTURE);
        // NeoForge's common glass tags cover ordinary, stained, tinted and
        // glass-pane variants, including modded blocks tagged by their authors.
        texture(textures, "c:glass_blocks", GLASS_TEXTURE);
        texture(textures, "c:glass_panes", GLASS_TEXTURE);
        texture(textures, "minecraft:dirt", DIRT_TEXTURE);
        texture(textures, "minecraft:sand", SAND_TEXTURE);
        return Collections.unmodifiableMap(textures);
    }


    // This is where you add , remove or replace the sounds id withspecific metal blocktags or chains
    private static Map<Identifier, Identifier> buildSurfaceSounds() {
        Map<Identifier, Identifier> sounds = new LinkedHashMap<>();
        sound(sounds, "minecraft:stone", "minecraft:block.stone.hit");
        sound(sounds, "minecraft:cobblestone", "minecraft:block.stone.hit");
        sound(sounds, "minecraft:deepslate", "minecraft:block.stone.hit");
        sound(sounds, "minecraft:stone_bricks", "minecraft:block.stone.hit");
        sound(sounds, "minecraft:iron_block", "pointblankextra:bullet_holes.metal");
        sound(sounds, "minecraft:gold_block", "pointblankextra:bullet_holes.metal");
        sound(sounds, "minecraft:copper_block", "pointblankextra:bullet_holes.metal");
        sound(sounds, "minecraft:netherite_block", "pointblankextra:bullet_holes.metal");
        sound(sounds, "minecraft:iron_bars", "pointblankextra:bullet_holes.metal");
        sound(sounds, "minecraft:sand", "minecraft:block.sand.hit");
        sound(sounds, "minecraft:iron_door", "pointblankextra:bullet_holes.metal");
        sound(sounds, "minecraft:iron_trapdoor", "pointblankextra:bullet_holes.metal");
        sound(sounds, "minecraft:anvil", "pointblankextra:bullet_holes.metal");
        sound(sounds, "minecraft:chain", "pointblankextra:bullet_holes.metal");
        sound(sounds, "minecraft:beacon", "pointblankextra:bullet_holes.glass");

        return Collections.unmodifiableMap(sounds);
    }

    private static Map<Identifier, Identifier> buildTagSounds() {
        Map<Identifier, Identifier> sounds = new LinkedHashMap<>();
        sound(sounds, "minecraft:logs", "pointblankextra:bullet_holes.wood");
        sound(sounds, "minecraft:planks", "pointblankextra:bullet_holes.wood");
        sound(sounds, "minecraft:bamboo_blocks", "pointblankextra:bullet_holes.wood");
        sound(sounds, "minecraft:stone_bricks", "minecraft:block.stone.hit");
        sound(sounds, "minecraft:base_stone_overworld", "minecraft:block.stone.hit");
        sound(sounds, "c:glass_blocks", "pointblankextra:bullet_holes.glass");
        sound(sounds, "c:glass_panes", "pointblankextra:bullet_holes.glass");
        sound(sounds, "minecraft:dirt", "pointblankextra:bullet_holes.dirt");
        sound(sounds, "minecraft:sand", "minecraft:block.sand.hit");
        sound(sounds, "c:metal_blocks", "pointblankextra:bullet_holes.metal");


        return Collections.unmodifiableMap(sounds);
    }

    private static void texture(Map<Identifier, Identifier> target, String blockId,
                                Identifier texture) {
        Identifier block = Identifier.tryParse(blockId);
        if (block != null) {
            target.put(block, texture);
        }
    }

    private static void sound(Map<Identifier, Identifier> target, String blockId,
                              String soundId) {
        Identifier block = Identifier.tryParse(blockId);
        Identifier sound = Identifier.tryParse(soundId);
        if (block != null && sound != null) {
            target.put(block, sound);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_NAMESPACE, path);
    }
}

