package com.arg.pbextra.animation;

import com.geckolib.cache.BakedAnimationCache;
import com.geckolib.cache.GeckoLibResources;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;



public final class AnimationLibraryRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, WeaponLibraries> LIBRARIES = new ConcurrentHashMap<>();
    private static final AtomicLong GENERATION = new AtomicLong();

    private AnimationLibraryRegistry() {
    }


    /** Libraries declared by one weapon: additive array entries and all declared values. */
    public record WeaponLibraries(List<String> additional, List<String> declared) {

        public WeaponLibraries {
            additional = additional == null ? List.of() : List.copyOf(additional);
            declared = declared == null ? List.of() : List.copyOf(declared);
        }
    }



    public static void captureBuilderJson(JsonObject json) {
        if (json == null) {
            return;
        }

        try {
            String name = readName(json);
            if (name == null) {
                return;
            }

            JsonElement element = json.get(AnimationLibraryPaths.KEY);
            if (element == null || element.isJsonNull()) {
                return;
            }

            if (element.isJsonArray()) {
                captureArray(json, name, element);
                return;
            }

            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String value = element.getAsString().trim();
                if (value.isEmpty()) {
                    return;
                }
                // The native string remains Point Blank's responsibility. Record it
                // for reload-time diagnostics only.
                put(name, new WeaponLibraries(List.of(), List.of(value)));
                if (value.indexOf(':') >= 0) {
                    LOGGER.warn("Weapon '{}' uses a namespaced firstPersonFallbackAnimations '{}'; "
                                    + "Point Blank's string form always resolves in the 'pointblank' namespace. "
                                    + "Use an array entry for a custom namespace.",
                            name, value);
                }
                return;
            }

            LOGGER.warn("Weapon '{}' has an invalid '{}'; expected a string or an array of strings",
                    name, AnimationLibraryPaths.KEY);
            remove(name);
            json.remove(AnimationLibraryPaths.KEY);
        } catch (Exception e) {
            LOGGER.warn("Could not inspect '{}' for reusable animation libraries: {}",
                    AnimationLibraryPaths.KEY, e.getMessage());
        }
    }


    /** Replaces (or merges) the runtime index after a weapon JSON scan. */
    public static void commit(Map<String, WeaponLibraries> libraries, boolean replace) {
        if (replace) {
            LIBRARIES.clear();
        }
        if (libraries != null) {
            libraries.forEach((name, value) -> {
                if (name != null && value != null
                        && (!value.additional().isEmpty() || !value.declared().isEmpty())) {
                    LIBRARIES.put(name, value);
                }
            });
        }
        GENERATION.incrementAndGet();
    }


    /** Additive array libraries for a weapon, in declaration order. */
    public static List<String> getAdditionalLibraries(String weaponName) {
        if (weaponName == null || weaponName.isBlank()) {
            return List.of();
        }
        WeaponLibraries libraries = LIBRARIES.get(weaponName);
        return libraries == null ? List.of() : libraries.additional();
    }


    /** Increments whenever the index changes so renderer-side caches can refresh. */
    public static long getGeneration() {
        return GENERATION.get();
    }



    public static void validate(ResourceManager resources) {
        if (resources == null) {
            LOGGER.debug("Skipping reusable animation library validation: no resource manager");
            return;
        }

        BakedAnimationCache baked = GeckoLibResources.getBakedAnimations();
        if (baked == null || baked.cache() == null || baked.cache().isEmpty()) {
            LOGGER.debug("Skipping reusable animation library validation: "
                    + "GeckoLib's baked animation cache is not ready");
            return;
        }

        Map<String, String> firstWeaponByLibrary = new LinkedHashMap<>();
        for (Map.Entry<String, WeaponLibraries> entry : LIBRARIES.entrySet()) {
            // Native string values are resolved by Point Blank itself. Only
            // the additive array form is loaded through this addon.
            for (String value : entry.getValue().additional()) {
                firstWeaponByLibrary.putIfAbsent(value, entry.getKey());
            }
        }

        for (Map.Entry<String, String> entry : firstWeaponByLibrary.entrySet()) {
            String value = entry.getKey();
            String weaponName = entry.getValue();
            String normalized = AnimationLibraryPaths.normalizeIdentifier(value);
            if (normalized == null) {
                LOGGER.warn("Animation library '{}' referenced by weapon '{}' is not a valid "
                        + "resource identifier", value, weaponName);
                continue;
            }

            List<String> cacheKeys = AnimationLibraryPaths.candidateAnimationCacheKeys(normalized);
            boolean found = false;
            String matchedKey = null;
            for (String cacheKey : cacheKeys) {
                Identifier identifier = Identifier.tryParse(cacheKey);
                if (identifier == null) {
                    continue;
                }
                try {
                    if (baked.cache().containsKey(identifier)) {
                        found = true;
                        matchedKey = cacheKey;
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not check animation cache key '{}': {}",
                            cacheKey, e.getMessage());
                }
            }

            if (found) {
                LOGGER.debug("Reusable animation library '{}' for weapon '{}' is available "
                        + "under GeckoLib cache key '{}'", value, weaponName, matchedKey);
            } else {
                LOGGER.warn("Animation library '{}' referenced by weapon '{}' was not found "
                                + "in GeckoLib's baked animation cache; expected one of {}",
                        value, weaponName, cacheKeys);
            }
        }
    }


    private static void captureArray(JsonObject json, String name, JsonElement element) {
        List<String> declared = AnimationLibraryPaths.parseDeclared(element);
        List<String> additional = AnimationLibraryPaths.parseAdditional(element);

        int rawCount = element.getAsJsonArray().size();
        if (additional.size() < rawCount) {
            LOGGER.warn("Weapon '{}' has non-string or duplicate entries in '{}'; "
                            + "only unique string paths are used",
                    name, AnimationLibraryPaths.KEY);
        }

        if (additional.isEmpty()) {
            remove(name);
        } else {
            put(name, new WeaponLibraries(additional, declared));
        }

        // Point Blank's JsonUtil.getJsonString cannot read an array. Removing the
        // additive form makes PB leave its native single-fallback field null; the
        // mixin appends these libraries instead.
        json.remove(AnimationLibraryPaths.KEY);
    }


    private static void put(String name, WeaponLibraries libraries) {
        LIBRARIES.put(name, libraries);
        GENERATION.incrementAndGet();
    }


    private static void remove(String name) {
        if (LIBRARIES.remove(name) != null) {
            GENERATION.incrementAndGet();
        }
    }


    private static String readName(JsonObject json) {
        JsonElement name = json.get("name");
        if (name == null || !name.isJsonPrimitive()
                || !name.getAsJsonPrimitive().isString()) {
            return null;
        }
        String value = name.getAsString();
        return value == null || value.isBlank() ? null : value;
    }
}

