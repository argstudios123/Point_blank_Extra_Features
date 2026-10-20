package com.arg.pbextra.bullethole;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;



public final class BulletHoleConfig {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Weapon JSON feature type handled by this feature. */
    public static final String FEATURE_TYPE = "bulletHoles";

    /** Settings keys accepted inside a feature entry and at the weapon root. */
    public static final String SETTINGS_KEY = "bullet_holes";
    public static final String SETTINGS_KEY_CAMEL = "bulletHoles";

    public static final String MOD_NAMESPACE = BulletHoleDefaults.MOD_NAMESPACE;

    // Aliases of the centralized values in BulletHoleDefaults. Change the
    // defaults there, never here.
    public static final Identifier DEFAULT_TEXTURE = BulletHoleDefaults.DEFAULT_TEXTURE;
    public static final Identifier WOOD_TEXTURE = BulletHoleDefaults.WOOD_TEXTURE;
    public static final Identifier STONE_TEXTURE = BulletHoleDefaults.STONE_TEXTURE;
    public static final Identifier METAL_TEXTURE = BulletHoleDefaults.METAL_TEXTURE;
    public static final Identifier GLASS_TEXTURE = BulletHoleDefaults.GLASS_TEXTURE;
    public static final Identifier DIRT_TEXTURE = BulletHoleDefaults.DIRT_TEXTURE;
    public static final Identifier SAND_TEXTURE = BulletHoleDefaults.SAND_TEXTURE;

    public static final float DEFAULT_SIZE = BulletHoleDefaults.SIZE;
    public static final int DEFAULT_LIFETIME_TICKS = BulletHoleDefaults.LIFETIME_TICKS;
    public static final int DEFAULT_MAX_PER_CHUNK = BulletHoleDefaults.MAX_PER_CHUNK;
    public static final int DEFAULT_MAX_BLOCK_PARTICLES =
            BulletHoleDefaults.MAX_BLOCK_PARTICLES;

    public static final float MIN_SIZE = 0.01F;
    public static final float MAX_SIZE = 1.0F;
    public static final int MIN_LIFETIME_TICKS = 20;
    public static final int MAX_LIFETIME_TICKS = 72_000;
    public static final int MIN_MAX_PER_CHUNK = 1;
    public static final int MAX_MAX_PER_CHUNK = 512;
    /** Zero is valid and disables impact particles. */
    public static final int MIN_MAX_BLOCK_PARTICLES = 0;
    /** Hard safety bound so a bad JSON value cannot spawn unbounded particles. */
    public static final int MAX_MAX_BLOCK_PARTICLES = 128;

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private final boolean enabled;
    private final Identifier defaultTexture;
    private final float size;
    private final int lifetimeTicks;
    private final int maxPerChunk;
    private final int maxBlockParticles;
    private final Map<Identifier, Identifier> surfaceTextures;
    private final List<TagTexture> tagTextures;
    private final BulletHoleSoundConfig sounds;

    public BulletHoleConfig(boolean enabled, Identifier defaultTexture, float size,
                            int lifetimeTicks, int maxPerChunk,
                            Map<Identifier, Identifier> surfaceTextures,
                            Map<Identifier, Identifier> tagTextures) {
        this(enabled, defaultTexture, size, lifetimeTicks, maxPerChunk,
                DEFAULT_MAX_BLOCK_PARTICLES, surfaceTextures, tagTextures,
                BulletHoleSoundConfig.DISABLED);
    }

    public BulletHoleConfig(boolean enabled, Identifier defaultTexture, float size,
                            int lifetimeTicks, int maxPerChunk,
                            Map<Identifier, Identifier> surfaceTextures,
                            Map<Identifier, Identifier> tagTextures,
                            BulletHoleSoundConfig sounds) {
        this(enabled, defaultTexture, size, lifetimeTicks, maxPerChunk,
                DEFAULT_MAX_BLOCK_PARTICLES, surfaceTextures, tagTextures, sounds);
    }

    /**
     * Full constructor. {@code maxBlockParticles} is the per-impact block-debris
     * cap; it is clamped to {@code [0, MAX_MAX_BLOCK_PARTICLES]}.
     */
    public BulletHoleConfig(boolean enabled, Identifier defaultTexture, float size,
                            int lifetimeTicks, int maxPerChunk, int maxBlockParticles,
                            Map<Identifier, Identifier> surfaceTextures,
                            Map<Identifier, Identifier> tagTextures,
                            BulletHoleSoundConfig sounds) {
        this.enabled = enabled;
        this.defaultTexture = defaultTexture == null ? DEFAULT_TEXTURE : defaultTexture;
        this.size = clampSize(size);
        this.lifetimeTicks = clampLifetime(lifetimeTicks);
        this.maxPerChunk = clampMaxPerChunk(maxPerChunk);
        this.maxBlockParticles = clampMaxBlockParticles(maxBlockParticles);
        this.surfaceTextures = Map.copyOf(validEntries(surfaceTextures));
        this.sounds = sounds == null ? BulletHoleSoundConfig.DISABLED : sounds;

        List<TagTexture> tags = new ArrayList<>();
        for (Map.Entry<Identifier, Identifier> entry : validEntries(tagTextures).entrySet()) {
            tags.add(new TagTexture(entry.getKey(), entry.getValue()));
        }
        // Deterministic rule for multiple matching tags: lexicographic tag id
        // order, first match wins. Stable across reloads and platforms.
        tags.sort(Comparator.comparing(tag -> tag.tag().toString()));
        this.tagTextures = List.copyOf(tags);
    }

    private static Map<Identifier, Identifier> validEntries(Map<Identifier, Identifier> source) {
        Map<Identifier, Identifier> result = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<Identifier, Identifier> entry : source.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Identifier getDefaultTexture() {
        return defaultTexture;
    }

    public float getSize() {
        return size;
    }

    public int getLifetimeTicks() {
        return lifetimeTicks;
    }

    public int getMaxPerChunk() {
        return maxPerChunk;
    }

    /** Maximum block-debris particles for one impact; zero disables them. */
    public int getMaxBlockParticles() {
        return maxBlockParticles;
    }

    public Map<Identifier, Identifier> getSurfaceTextures() {
        return surfaceTextures;
    }

    public List<TagTexture> getTagTextures() {
        return tagTextures;
    }

    /** Effective material sound configuration (never null). */
    public BulletHoleSoundConfig getSounds() {
        return sounds;
    }


    /**
     * Selects the decal texture: exact block override, then the first matching
     * configured tag (lexicographic order), then the effective default texture.
     */
    public Identifier selectTexture(Identifier blockId, Predicate<Identifier> tagMatcher) {
        if (blockId != null) {
            Identifier exact = surfaceTextures.get(blockId);
            if (exact != null) {
                return exact;
            }
        }
        if (tagMatcher != null) {
            for (TagTexture tagTexture : tagTextures) {
                if (tagMatcher.test(tagTexture.tag())) {
                    return tagTexture.texture();
                }
            }
        }
        return defaultTexture;
    }

    private static float clampSize(float value) {
        if (!Float.isFinite(value)) {
            return DEFAULT_SIZE;
        }
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, value));
    }

    private static int clampLifetime(int value) {
        return Math.max(MIN_LIFETIME_TICKS, Math.min(MAX_LIFETIME_TICKS, value));
    }

    private static int clampMaxPerChunk(int value) {
        return Math.max(MIN_MAX_PER_CHUNK, Math.min(MAX_MAX_PER_CHUNK, value));
    }

    /** Negative values disable particles; excessive values are bounded safely. */
    private static int clampMaxBlockParticles(int value) {
        return Math.max(MIN_MAX_BLOCK_PARTICLES,
                Math.min(MAX_MAX_BLOCK_PARTICLES, value));
    }

    /** One tag-to-texture mapping, ordered lexicographically for deterministic selection. */
    public record TagTexture(Identifier tag, Identifier texture) {
    }


    /**
     * A partially specified configuration. {@code null} values and absent map
     * entries mean "inherit", which lets a gun override only what it supplies.
     */
    public record Overrides(Boolean enabled,
                            Identifier defaultTexture,
                            Float size,
                            Integer lifetimeTicks,
                            Integer maxPerChunk,
                            Map<Identifier, Identifier> surfaceTextures,
                            Map<Identifier, Identifier> tagTextures,
                            BulletHoleSoundConfig.Overrides soundOverrides,
                            Integer maxBlockParticles) {

        public static final Overrides NONE =
                new Overrides(null, null, null, null, null,
                        Map.of(), Map.of(), BulletHoleSoundConfig.Overrides.NONE, null);

        /** Backward-compatible convenience constructor for callers without sounds. */
        public Overrides(Boolean enabled, Identifier defaultTexture, Float size,
                         Integer lifetimeTicks, Integer maxPerChunk,
                         Map<Identifier, Identifier> surfaceTextures,
                         Map<Identifier, Identifier> tagTextures) {
            this(enabled, defaultTexture, size, lifetimeTicks, maxPerChunk,
                    surfaceTextures, tagTextures,
                    BulletHoleSoundConfig.Overrides.NONE, null);
        }

        /** Backward-compatible convenience constructor without particle overrides. */
        public Overrides(Boolean enabled, Identifier defaultTexture, Float size,
                         Integer lifetimeTicks, Integer maxPerChunk,
                         Map<Identifier, Identifier> surfaceTextures,
                         Map<Identifier, Identifier> tagTextures,
                         BulletHoleSoundConfig.Overrides soundOverrides) {
            this(enabled, defaultTexture, size, lifetimeTicks, maxPerChunk,
                    surfaceTextures, tagTextures, soundOverrides, null);
        }

        public Overrides {
            surfaceTextures = surfaceTextures == null ? Map.of() : Map.copyOf(surfaceTextures);
            tagTextures = tagTextures == null ? Map.of() : Map.copyOf(tagTextures);
            soundOverrides = soundOverrides == null
                    ? BulletHoleSoundConfig.Overrides.NONE : soundOverrides;
        }

        /**
         * Reads a feature entry, a pack {@code bullet_holes.json} or a weapon-root
         * {@code bulletHoles}/{@code bullet_holes} object. A wrapper object with
         * one of those names is unwrapped when present.
         */
        public static Overrides fromJson(JsonObject root) {
            JsonObject settings = selectSettingsObject(root);
            if (settings == null) {
                return NONE;
            }

            Boolean enabled = readBoolean(settings, "enabled");
            Identifier defaultTexture = readTexture(settings,
                    "default_texture", "defaultTexture");
            Float size = readFloat(settings, "size");
            Integer lifetime = readInt(settings, "lifetime");
            Integer maxPerChunk = readInt(settings, "max_per_chunk", "maxPerChunk");
            Integer maxBlockParticles = readInt(settings,
                    "max_block_particles", "maxBlockParticles");
            Map<Identifier, Identifier> surfaces = readTextureMap(settings,
                    "surface_textures", "surfaceTextures");
            Map<Identifier, Identifier> tags = readTextureMap(settings,
                    "tag_textures", "tagTextures");
            JsonObject soundObject = firstObject(settings, BulletHoleSoundConfig.KEY);
            BulletHoleSoundConfig.Overrides sounds = soundObject == null
                    ? BulletHoleSoundConfig.Overrides.NONE
                    : BulletHoleSoundConfig.Overrides.fromJson(soundObject);

            return new Overrides(enabled, defaultTexture, size, lifetime, maxPerChunk,
                    surfaces, tags, sounds, maxBlockParticles);
        }

        private static JsonObject selectSettingsObject(JsonObject root) {
            if (root == null) {
                return null;
            }
            JsonObject nested = firstObject(root, SETTINGS_KEY, SETTINGS_KEY_CAMEL);
            return nested != null ? nested : root;
        }
    }

    private static JsonObject firstObject(JsonObject obj, String... keys) {
        for (String key : keys) {
            JsonElement element = obj.get(key);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        }
        return null;
    }

    private static Boolean readBoolean(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (!primitive.isString()) {
            return null;
        }
        String value = primitive.getAsString();
        if ("true".equalsIgnoreCase(value) || "enabled".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value) || "disabled".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static Float readFloat(JsonObject obj, String... keys) {
        JsonElement element = firstPrimitive(obj, keys);
        if (element == null) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        try {
            float value = primitive.isNumber()
                    ? primitive.getAsFloat() : Float.parseFloat(primitive.getAsString());
            return Float.isFinite(value) ? value : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Integer readInt(JsonObject obj, String... keys) {
        JsonElement element = firstPrimitive(obj, keys);
        if (element == null) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        try {
            return primitive.isNumber()
                    ? primitive.getAsInt() : Integer.parseInt(primitive.getAsString().trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static JsonElement firstPrimitive(JsonObject obj, String... keys) {
        for (String key : keys) {
            JsonElement element = obj.get(key);
            if (element != null && element.isJsonPrimitive()) {
                return element;
            }
        }
        return null;
    }

    private static Identifier readTexture(JsonObject obj, String... keys) {
        JsonElement element = firstPrimitive(obj, keys);
        if (element == null || !element.getAsJsonPrimitive().isString()) {
            return null;
        }
        return parseTexture(element.getAsString());
    }

    private static Map<Identifier, Identifier> readTextureMap(JsonObject obj, String... keys) {
        JsonObject map = firstObject(obj, keys);
        if (map == null) {
            return Map.of();
        }
        Map<Identifier, Identifier> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
            if (entry.getValue() == null || !entry.getValue().isJsonPrimitive()
                    || !entry.getValue().getAsJsonPrimitive().isString()) {
                warnOnce("Ignoring non-string bullet-hole texture map entry '" + entry.getKey() + "'");
                continue;
            }
            Identifier key = parseRegistryKey(entry.getKey());
            Identifier texture = parseTexture(entry.getValue().getAsString());
            if (key == null || texture == null) {
                warnOnce("Ignoring invalid bullet-hole texture mapping '"
                        + entry.getKey() + "' -> '" + entry.getValue().getAsString() + "'");
                continue;
            }
            result.put(key, texture);
        }
        return result;
    }

    /**
     * Normalises a configured texture. Accepted forms include
     * {@code namespace:textures/bullet_holes/x.png},
     * {@code namespace:bullet_holes/x} and {@code bullet_holes/x}; the last two
     * get the {@code textures/} prefix and {@code .png} suffix.
     */
    public static Identifier parseTexture(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        int colon = trimmed.indexOf(':');
        String namespace;
        String path;
        if (colon >= 0) {
            if (trimmed.indexOf(':', colon + 1) >= 0) {
                return null;
            }
            namespace = trimmed.substring(0, colon);
            path = trimmed.substring(colon + 1);
        } else {
            namespace = MOD_NAMESPACE;
            path = trimmed;
        }

        if (path.indexOf('/') < 0) {
            path = "bullet_holes/" + path;
        }
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return null;
            }
        }
        return Identifier.tryParse(namespace + ":" + path);
    }

    private static Identifier parseRegistryKey(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.indexOf(':') < 0) {
            trimmed = "minecraft:" + trimmed;
        }
        return Identifier.tryParse(trimmed);
    }

    private static void warnOnce(String message) {
        if (WARNED.add(message)) {
            LOGGER.warn(message);
        }
    }


    /** Resolves a partial configuration over an inherited (pack or built-in) one. */
    public static BulletHoleConfig resolve(Overrides overrides, BulletHoleConfig inherited) {
        Overrides values = overrides == null ? Overrides.NONE : overrides;
        BulletHoleConfig base = inherited == null ? BUILT_IN_DEFAULTS : inherited;

        boolean enabled = values.enabled() != null ? values.enabled() : base.enabled;
        Identifier texture = values.defaultTexture() != null
                ? values.defaultTexture() : base.defaultTexture;
        float size = values.size() != null ? values.size() : base.size;
        int lifetime = values.lifetimeTicks() != null
                ? values.lifetimeTicks() : base.lifetimeTicks;
        int maxPerChunk = values.maxPerChunk() != null
                ? values.maxPerChunk() : base.maxPerChunk;
        int maxBlockParticles = values.maxBlockParticles() != null
                ? values.maxBlockParticles() : base.maxBlockParticles;

        Map<Identifier, Identifier> surfaces = new LinkedHashMap<>(base.surfaceTextures);
        surfaces.putAll(values.surfaceTextures());
        Map<Identifier, Identifier> tags = new LinkedHashMap<>(base.tagTextureMap());
        tags.putAll(values.tagTextures());

        BulletHoleSoundConfig sounds = BulletHoleSoundConfig.resolve(
                values.soundOverrides(), base.sounds);

        return new BulletHoleConfig(enabled, texture, size, lifetime, maxPerChunk,
                maxBlockParticles, surfaces, tags, sounds);
    }

    /** Convenience for pack-wide configuration files. */
    public static BulletHoleConfig fromJson(JsonObject root, BulletHoleConfig inherited) {
        return resolve(Overrides.fromJson(root), inherited);
    }

    private Map<Identifier, Identifier> tagTextureMap() {
        Map<Identifier, Identifier> result = new LinkedHashMap<>();
        for (TagTexture tagTexture : tagTextures) {
            result.put(tagTexture.tag(), tagTexture.texture());
        }
        return result;
    }

    @Override
    public String toString() {
        return "BulletHoleConfig[enabled=" + enabled
                + ", texture=" + defaultTexture
                + ", size=" + size
                + ", lifetime=" + lifetimeTicks
                + ", maxPerChunk=" + maxPerChunk
                + ", maxBlockParticles=" + maxBlockParticles
                + ", surfaces=" + surfaceTextures.size()
                + ", tags=" + tagTextures.size()
                + ", sounds=" + sounds + "]";
    }


    /**
     * Defaults for every gun, built from {@link BulletHoleDefaults}: bullet
     * holes on, built-in texture, the configured limits and the built-in
     * material mappings. A weapon JSON without a bullet-hole entry inherits
     * this object unchanged; a partial entry is merged over it.
     */
    public static final BulletHoleConfig BUILT_IN_DEFAULTS = new BulletHoleConfig(
            BulletHoleDefaults.ENABLED,
            DEFAULT_TEXTURE,
            DEFAULT_SIZE,
            DEFAULT_LIFETIME_TICKS,
            DEFAULT_MAX_PER_CHUNK,
            DEFAULT_MAX_BLOCK_PARTICLES,
            BulletHoleDefaults.surfaceTextures(),
            BulletHoleDefaults.tagTextures(),
            BulletHoleSoundConfig.BUILT_IN_DEFAULTS);
}

