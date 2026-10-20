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



public final class BulletHoleSoundConfig {

    public static final String KEY = "soundsPlayOnperBlockHit";

    // Aliases of the centralized values in BulletHoleDefaults. Change the
    // defaults there, never here.
    public static final String DEFAULT_SOUND = BulletHoleDefaults.DEFAULT_SOUND.toString();
    public static final float DEFAULT_IMPACT_VOLUME = BulletHoleDefaults.IMPACT_VOLUME;
    public static final float DEFAULT_MAX_DISTANCE = BulletHoleDefaults.MAX_SOUND_DISTANCE;
    public static final float MIN_MAX_DISTANCE = 1.0F;
    public static final float MAX_MAX_DISTANCE = 128.0F;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private final boolean enabled;
    private final SoundRef defaultSound;
    private final Map<Identifier, SoundRef> surfaceSounds;
    private final List<TagSound> tagSounds;
    private final float impactVolume;
    private final float maxDistance;

    public BulletHoleSoundConfig(boolean enabled, SoundRef defaultSound,
                                 Map<Identifier, SoundRef> surfaceSounds,
                                 Map<Identifier, SoundRef> tagSounds) {
        this(enabled, defaultSound, surfaceSounds, tagSounds, DEFAULT_IMPACT_VOLUME);
    }

    public BulletHoleSoundConfig(boolean enabled, SoundRef defaultSound,
                                 Map<Identifier, SoundRef> surfaceSounds,
                                 Map<Identifier, SoundRef> tagSounds,
                                 float impactVolume) {
        this(enabled, defaultSound, surfaceSounds, tagSounds, impactVolume,
                DEFAULT_MAX_DISTANCE);
    }

    public BulletHoleSoundConfig(boolean enabled, SoundRef defaultSound,
                                 Map<Identifier, SoundRef> surfaceSounds,
                                 Map<Identifier, SoundRef> tagSounds,
                                 float impactVolume, float maxDistance) {
        this.enabled = enabled;
        this.defaultSound = defaultSound;
        this.surfaceSounds = Map.copyOf(validEntries(surfaceSounds));
        this.impactVolume = clampImpactVolume(impactVolume);
        this.maxDistance = clampMaxDistance(maxDistance);

        Map<Identifier, SoundRef> validTags = validEntries(tagSounds);
        List<TagSound> tags = new ArrayList<>(validTags.size());
        for (Map.Entry<Identifier, SoundRef> entry : validTags.entrySet()) {
            tags.add(new TagSound(entry.getKey(), entry.getValue()));
        }
        // Same deterministic rule as texture tags: lexicographic tag id,
        // first matching tag wins.
        tags.sort(Comparator.comparing(tag -> tag.tag().toString()));
        this.tagSounds = List.copyOf(tags);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public SoundRef getDefaultSound() {
        return defaultSound;
    }

    public Map<Identifier, SoundRef> getSurfaceSounds() {
        return surfaceSounds;
    }

    public List<TagSound> getTagSounds() {
        return tagSounds;
    }

    /** Effective impact volume, clamped to at least zero; values above 1 are allowed. */
    public float getImpactVolume() {
        return impactVolume;
    }

    /**
     * Maximum distance in blocks at which this impact sound can be heard.
     * Independent of {@link #getImpactVolume()}.
     */
    public float getMaxDistance() {
        return maxDistance;
    }

    /** Negative and non-finite values become zero; values above 1 are preserved. */
    public static float clampImpactVolume(float value) {
        if (!Float.isFinite(value) || value < 0.0F) {
            return 0.0F;
        }
        return value;
    }

    /**
     * Linear distance gain for an impact sound. Returns 1 at the listener and
     * 0 at or beyond {@code maxDistance}. This calculation has no volume input,
     * so {@code impact_volume} cannot extend the audible radius.
     */
    public static float distanceGain(double distanceBlocks, float maxDistance) {
        float limit = clampMaxDistance(maxDistance);
        if (!(distanceBlocks > 0.0D)) {
            return 1.0F;
        }
        if (distanceBlocks >= limit) {
            return 0.0F;
        }
        return (float) (1.0D - distanceBlocks / limit);
    }

    /**
     * Keeps the audible radius valid and bounded. Invalid values fall back to
     * the default; valid values are clamped to {@code [1, 128]} blocks.
     */
    public static float clampMaxDistance(float value) {
        if (!Float.isFinite(value)) {
            return DEFAULT_MAX_DISTANCE;
        }
        return Math.max(MIN_MAX_DISTANCE, Math.min(MAX_MAX_DISTANCE, value));
    }

    /**
     * Volume handed to a positional {@code SoundInstance}.
     *
     * <p>Minecraft's {@code SoundEngine} clamps the channel gain to the range
     * 0..1, but it derives the linear attenuation distance as
     * {@code max(instanceVolume, 1) * sound.attenuationDistance()}. Passing a
     * configured volume above 1 therefore does <em>not</em> make the impact
     * louder; it only enlarges the radius in which the impact is audible.
     * Capping the instance volume at 1 keeps {@code impact_volume} as a gain
     * control for quieter impacts without letting it extend the vanilla
     * attenuation distance.</p>
     */
    public static float engineVolume(float configuredVolume) {
        return Math.min(clampImpactVolume(configuredVolume), 1.0F);
    }

    /**
     * Resolves a sound using exact block, tag (lexicographic), then default
     * priority. A disabled {@link SoundRef} is returned as-is so playback can
     * distinguish it from an absent override.
     */
    public SoundRef selectSound(Identifier blockId, Predicate<Identifier> tagMatcher) {
        if (!enabled) {
            return null;
        }
        if (blockId != null) {
            SoundRef exact = surfaceSounds.get(blockId);
            if (exact != null) {
                return exact;
            }
        }
        if (tagMatcher != null) {
            for (TagSound tagSound : tagSounds) {
                if (tagMatcher.test(tagSound.tag())) {
                    return tagSound.sound();
                }
            }
        }
        return defaultSound;
    }

    /**
     * Applies registry availability to a selected sound. Explicitly disabled
     * entries stay disabled; an unknown selected id falls back to the default
     * only when that default is different and registered.
     */
    public static SoundRef resolveAvailable(SoundRef selected, SoundRef fallback,
                                            Predicate<Identifier> registered) {
        if (selected == null || !selected.isEnabled()) {
            return selected;
        }
        if (registered.test(selected.sound())) {
            return selected;
        }
        if (fallback != null && fallback.isEnabled()
                && !fallback.sound().equals(selected.sound())
                && registered.test(fallback.sound())) {
            return fallback;
        }
        return SoundRef.DISABLED;
    }

    /** Resolves a partial sound layer over an inherited configuration. */
    public static BulletHoleSoundConfig resolve(Overrides overrides,
                                                BulletHoleSoundConfig inherited) {
        Overrides values = overrides == null ? Overrides.NONE : overrides;
        BulletHoleSoundConfig base = inherited == null ? DISABLED : inherited;

        boolean enabled = values.enabled() != null
                ? values.enabled() : base.enabled;
        SoundRef defaultSound = values.defaultSound() != null
                ? values.defaultSound() : base.defaultSound;
        float impactVolume = values.impactVolume() != null
                ? values.impactVolume() : base.impactVolume;
        float maxDistance = values.maxDistance() != null
                ? values.maxDistance() : base.maxDistance;

        Map<Identifier, SoundRef> surfaces = new LinkedHashMap<>(base.surfaceSounds);
        surfaces.putAll(values.surfaceSounds());
        Map<Identifier, SoundRef> tags = new LinkedHashMap<>(base.tagSoundMap());
        tags.putAll(values.tagSounds());

        return new BulletHoleSoundConfig(enabled, defaultSound, surfaces, tags,
                impactVolume, maxDistance);
    }

    private Map<Identifier, SoundRef> tagSoundMap() {
        Map<Identifier, SoundRef> result = new LinkedHashMap<>();
        for (TagSound tagSound : tagSounds) {
            result.put(tagSound.tag(), tagSound.sound());
        }
        return result;
    }

    private static Map<Identifier, SoundRef> validEntries(
            Map<Identifier, SoundRef> source) {
        Map<Identifier, SoundRef> result = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<Identifier, SoundRef> entry : source.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    /** One sound id, or a disabled entry when {@code sound} is null. */
    public record SoundRef(Identifier sound) {
        public static final SoundRef DISABLED = new SoundRef(null);

        public boolean isEnabled() {
            return sound != null;
        }
    }

    /** One tag-to-sound mapping, ordered lexicographically. */
    public record TagSound(Identifier tag, SoundRef sound) {
    }

    /**
     * A partial sound layer. Null values mean "inherit"; a
     * {@link SoundRef#DISABLED} value means "do not play for this entry".
     */
    public record Overrides(Boolean enabled,
                            SoundRef defaultSound,
                            Map<Identifier, SoundRef> surfaceSounds,
                            Map<Identifier, SoundRef> tagSounds,
                            Float impactVolume,
                            Float maxDistance) {

        public static final Overrides NONE =
                new Overrides(null, null, Map.of(), Map.of(), null, null);

        /** Backward-compatible constructor for callers that do not change volume. */
        public Overrides(Boolean enabled, SoundRef defaultSound,
                         Map<Identifier, SoundRef> surfaceSounds,
                         Map<Identifier, SoundRef> tagSounds) {
            this(enabled, defaultSound, surfaceSounds, tagSounds, null, null);
        }

        /** Backward-compatible constructor without a distance override. */
        public Overrides(Boolean enabled, SoundRef defaultSound,
                         Map<Identifier, SoundRef> surfaceSounds,
                         Map<Identifier, SoundRef> tagSounds,
                         Float impactVolume) {
            this(enabled, defaultSound, surfaceSounds, tagSounds, impactVolume, null);
        }

        public Overrides {
            surfaceSounds = surfaceSounds == null ? Map.of() : Map.copyOf(surfaceSounds);
            tagSounds = tagSounds == null ? Map.of() : Map.copyOf(tagSounds);
        }

        public static Overrides fromJson(JsonObject root) {
            if (root == null) {
                return NONE;
            }

            Boolean enabled = readBoolean(root, "enabled");
            SoundRef defaultSound = readSound(root, "default");
            Map<Identifier, SoundRef> surfaces = readSoundMap(root, "surface_sounds");
            Map<Identifier, SoundRef> tags = readSoundMap(root, "tag_sounds");
            Float impactVolume = readFloat(root, "impact_volume");
            Float maxDistance = readFloat(root, "max_distance", "maxDistance");

            return new Overrides(enabled, defaultSound, surfaces, tags, impactVolume,
                    maxDistance);
        }

        private static Float readFloat(JsonObject root, String... keys) {
            for (String key : keys) {
                JsonElement element = root.get(key);
                if (element == null || !element.isJsonPrimitive()) {
                    continue;
                }
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                try {
                    float value = primitive.isNumber()
                            ? primitive.getAsFloat()
                            : Float.parseFloat(primitive.getAsString());
                    if (Float.isFinite(value)) {
                        return value;
                    }
                } catch (RuntimeException exception) {
                    // Try the next alias / treat as absent.
                }
            }
            return null;
        }

        private static Boolean readBoolean(JsonObject root, String key) {
            JsonElement element = root.get(key);
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

        private static SoundRef readSound(JsonObject root, String key) {
            JsonElement element = root.get(key);
            if (element == null || !element.isJsonPrimitive()) {
                return null;
            }
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (!primitive.isString()) {
                return null;
            }
            return parseSoundRef(primitive.getAsString(), key);
        }

        private static Map<Identifier, SoundRef> readSoundMap(JsonObject root,
                                                              String key) {
            JsonElement element = root.get(key);
            if (element == null || !element.isJsonObject()) {
                return Map.of();
            }
            Map<Identifier, SoundRef> result = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry
                    : element.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if (value == null || !value.isJsonPrimitive()
                        || !value.getAsJsonPrimitive().isString()) {
                    warnOnce("Ignoring non-string bullet-hole sound map entry '"
                            + entry.getKey() + "'");
                    continue;
                }
                Identifier keyId = parseRegistryKey(entry.getKey());
                SoundRef sound = parseSoundRef(value.getAsString(), entry.getKey());
                if (keyId == null || sound == null) {
                    warnOnce("Ignoring invalid bullet-hole sound mapping '"
                            + entry.getKey() + "' -> '" + value.getAsString() + "'");
                    continue;
                }
                result.put(keyId, sound);
            }
            return result;
        }
    }


    private static SoundRef parseSoundRef(String value, String where) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()
                || "none".equalsIgnoreCase(trimmed)
                || "disabled".equalsIgnoreCase(trimmed)
                || "off".equalsIgnoreCase(trimmed)
                || "false".equalsIgnoreCase(trimmed)
                || "silent".equalsIgnoreCase(trimmed)) {
            return SoundRef.DISABLED;
        }
        Identifier sound = parseSoundId(trimmed);
        if (sound == null) {
            warnOnce("Ignoring invalid bullet-hole sound event id '" + value
                    + "' for '" + where + "'");
            return null;
        }
        return new SoundRef(sound);
    }

    /** True for a config value that names a physical .ogg resource. */
    public static boolean isPhysicalOgg(Identifier soundId) {
        return soundId != null && soundId.getPath().endsWith(".ogg");
    }

    /**
     * Converts a configured physical location to the resource-manager path.
     * {@code ns:sounds/bullet_holes/wood.ogg} stays as-is;
     * {@code ns:bullet_holes/wood.ogg} is interpreted under {@code sounds/}.
     */
    public static Identifier physicalResourceId(Identifier soundId) {
        if (!isPhysicalOgg(soundId)) {
            return null;
        }
        String path = soundId.getPath();
        if (!path.startsWith("sounds/")) {
            path = "sounds/" + path;
        }
        return Identifier.tryParse(soundId.getNamespace() + ":" + path);
    }

    /**
     * Converts a configured physical location to the id expected by
     * {@link net.minecraft.client.resources.sounds.Sound}. Minecraft's
     * {@code Sound.SOUND_LISTER} adds {@code sounds/} and {@code .ogg} back.
     */
    public static Identifier physicalSoundLocation(Identifier soundId) {
        if (!isPhysicalOgg(soundId)) {
            return null;
        }
        String path = soundId.getPath();
        if (path.startsWith("sounds/")) {
            path = path.substring("sounds/".length());
        }
        path = path.substring(0, path.length() - ".ogg".length());
        if (path.isEmpty()) {
            return null;
        }
        return Identifier.tryParse(soundId.getNamespace() + ":" + path);
    }


    /**
     * Parses a sound-event registry id. This is deliberately not the texture
     * parser: sound ids are registry names such as
     * {@code minecraft:block.wood.hit}, not filesystem paths. A slash is a
     * valid resource-location path separator for custom registrations, so it
     * must not be rejected here.
     */
    public static Identifier parseSoundId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.indexOf('\\') >= 0
                || trimmed.contains("..")) {
            return null;
        }
        if (trimmed.indexOf(':') < 0) {
            trimmed = "minecraft:" + trimmed;
        }
        return Identifier.tryParse(trimmed);
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

    @Override
    public String toString() {
        return "BulletHoleSoundConfig[enabled=" + enabled
                + ", default=" + (defaultSound == null || !defaultSound.isEnabled()
                ? "disabled" : defaultSound.sound())
                + ", impactVolume=" + impactVolume
                + ", maxDistance=" + maxDistance
                + ", surfaces=" + surfaceSounds.size()
                + ", tags=" + tagSounds.size() + "]";
    }

    private static Map<Identifier, SoundRef> builtInSounds(
            Map<Identifier, Identifier> source) {
        Map<Identifier, SoundRef> sounds = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Identifier> entry : source.entrySet()) {
            sounds.put(entry.getKey(), new SoundRef(entry.getValue()));
        }
        return sounds;
    }

    public static final BulletHoleSoundConfig DISABLED =
            new BulletHoleSoundConfig(false, SoundRef.DISABLED, Map.of(), Map.of());

    /**
     * Defaults for every gun, built from {@link BulletHoleDefaults}: impact
     * sounds on, the mod's registered default impact sound and the built-in
     * material mappings. A weapon JSON that omits the sound object inherits
     * this unchanged; a partial object is merged over it.
     */
    public static final BulletHoleSoundConfig BUILT_IN_DEFAULTS =
            new BulletHoleSoundConfig(BulletHoleDefaults.SOUNDS_ENABLED,
                    new SoundRef(BulletHoleDefaults.DEFAULT_SOUND),
                    builtInSounds(BulletHoleDefaults.surfaceSounds()),
                    builtInSounds(BulletHoleDefaults.tagSounds()),
                    BulletHoleDefaults.IMPACT_VOLUME,
                    BulletHoleDefaults.MAX_SOUND_DISTANCE);
}

