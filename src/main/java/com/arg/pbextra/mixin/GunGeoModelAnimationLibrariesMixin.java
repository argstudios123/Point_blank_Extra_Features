package com.arg.pbextra.mixin;

import com.arg.pbextra.animation.AnimationLibraryPaths;
import com.arg.pbextra.animation.AnimationLibraryRegistry;
import com.geckolib.cache.BakedAnimationCache;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.model.GeoModel;
import com.mojang.logging.LogUtils;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Appends reusable animation libraries to the fallback chain Point Blank already
 * uses for item animations.
 * <p>
 * The chain becomes:
 * <pre>
 * weapon's own animation file
 *   -&gt; reusable libraries (array order; traditional root first, then
 *      default_animations)
 *   -&gt; Point Blank's AnimationType defaults (pistol/common)
 * </pre>
 * Weapon-specific animations still win because GeckoLib searches the primary
 * resource before any fallback. Dual-wield suffixes are untouched: Point Blank's
 * existing resolver calls {@code getBakedAnimation} with {@code _dwleft} or
 * {@code _dwright}, so suffixed names in these libraries are found automatically.
 */
@Mixin(targets = "com.vicmatskiv.pointblank.client.model.GunGeoModel", remap = false)
public abstract class GunGeoModelAnimationLibrariesMixin {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, CachedFallbacks> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private static volatile long cacheGeneration = Long.MIN_VALUE;

    @Inject(
            method = "getAnimationResourceFallbacks(Lcom/vicmatskiv/pointblank/item/GunItem;)"
                    + "[Lnet/minecraft/resources/Identifier;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void pbextra$appendAnimationLibraries(GunItem gun,
                                                  CallbackInfoReturnable<Identifier[]> cir) {
        try {
            long generation = AnimationLibraryRegistry.getGeneration();
            if (generation != cacheGeneration) {
                CACHE.clear();
                cacheGeneration = generation;
            }

            if (gun == null) {
                return;
            }

            String weaponName = gun.getItemName();
            if (weaponName == null || weaponName.isBlank()) {
                return;
            }

            List<String> libraries = AnimationLibraryRegistry.getAdditionalLibraries(weaponName);
            if (libraries.isEmpty()) {
                return;
            }

            Identifier[] original = cir.getReturnValue();
            CachedFallbacks cached = CACHE.get(weaponName);
            if (cached != null && Arrays.equals(cached.original(), original)) {
                cir.setReturnValue(cached.combined());
                return;
            }

            LinkedHashSet<Identifier> combined = new LinkedHashSet<>();

            for (String value : libraries) {
                Identifier identifier = parseLibrary(weaponName, value);
                if (identifier != null) {
                    // Existing content packs keep the traditional
                    // animations/item/<path> lookup. The optional custom root
                    // is appended after it for each configured entry so the
                    // entry order is still preserved. Entries that already
                    // target default_animations/ have no traditional form.
                    String traditional =
                            AnimationLibraryPaths.traditionalLibraryIdentifier(value);
                    if (traditional != null) {
                        Identifier traditionalIdentifier = Identifier.tryParse(traditional);
                        if (traditionalIdentifier != null) {
                            combined.add(traditionalIdentifier);
                        }
                    }

                    String custom = AnimationLibraryPaths.customLibraryIdentifier(value);
                    if (custom != null) {
                        Identifier customIdentifier = Identifier.tryParse(custom);
                        if (customIdentifier != null) {
                            combined.add(customIdentifier);
                        }
                    }
                }
            }

            if (combined.isEmpty()) {
                return;
            }

            if (original != null) {
                for (Identifier identifier : original) {
                    if (identifier != null) {
                        combined.add(identifier);
                    }
                }
            }

            Identifier[] result = combined.toArray(new Identifier[0]);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Reusable animation fallback chain for '{}': {}",
                        weaponName, combined);
            }
            CACHE.put(weaponName, new CachedFallbacks(original, result));
            cir.setReturnValue(result);
        } catch (Exception e) {
            if (WARNED.add("error|" + gun)) {
                LOGGER.warn("Could not append reusable animation libraries for weapon '{}': {}",
                        gun == null ? "<unknown>" : gun.getItemName(), e.getMessage());
            }
        }
    }


    /**
     * Point Blank's {@code BakedAnimationCacheMixin} scans each resource for
     * the exact animation name and then, if it is absent, immediately returns
     * the stripped base name from that resource. That means a primary file
     * containing {@code animation.model.idle} wins over a later library
     * containing {@code animation.model.idle_dwright}.
     * <p>
     * Resolve an exact dual-wield name across the complete primary + fallback
     * chain before Point Blank's base-name fallback runs. If no exact variant
     * exists anywhere, do nothing and let Point Blank's existing fallback
     * resolve the unsuffixed name.
     */
    @Inject(
            method = "getBakedAnimation(Lcom/vicmatskiv/pointblank/item/GunItem;Ljava/lang/String;)"
                    + "Lcom/geckolib/cache/animation/Animation;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    @SuppressWarnings("unchecked")
    private void pbextra$preferExactDualWield(GunItem gun,
                                              String animationName,
                                              CallbackInfoReturnable<Animation> cir) {
        if (gun == null || !AnimationLibraryPaths.isDualWieldAnimationName(animationName)) {
            return;
        }

        try {
            BakedAnimationCache baked = GeckoLibResources.getBakedAnimations();
            if (baked == null || baked.cache() == null || baked.cache().isEmpty()) {
                return;
            }

            GeoModel<GunItem> model = (GeoModel<GunItem>) (Object) this;
            ExactAnimation exact = findExactDualWieldAnimation(
                    baked,
                    model.getAnimationResource(gun),
                    model.getAnimationResourceFallbacks(gun),
                    animationName);

            if (exact == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[dual-wield exact] gun={} requested={} no exact definition; "
                                    + "deferring to Point Blank's base-name fallback",
                            gun.getItemName(), animationName);
                }
                return;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[dual-wield exact] gun={} requested={} source={} resolved={}",
                        gun.getItemName(), animationName, exact.source(),
                        exact.animation().name());
            }
            cir.setReturnValue(exact.animation());
        } catch (Exception e) {
            LOGGER.debug("[dual-wield exact] Could not resolve '{}' for '{}': {}",
                    animationName, gun.getItemName(), e.getMessage());
        }
    }


    private static ExactAnimation findExactDualWieldAnimation(
            BakedAnimationCache baked,
            Identifier primary,
            Identifier[] fallbacks,
            String animationName) {
        ExactAnimation exact = findInResource(baked, primary, animationName);
        if (exact != null || fallbacks == null) {
            return exact;
        }

        for (Identifier fallback : fallbacks) {
            exact = findInResource(baked, fallback, animationName);
            if (exact != null) {
                return exact;
            }
        }
        return null;
    }


    private static ExactAnimation findInResource(
            BakedAnimationCache baked,
            Identifier resource,
            String animationName) {
        if (resource == null) {
            return null;
        }
        BakedAnimations animations = baked.cache().get(resource);
        if (animations == null) {
            return null;
        }
        Animation animation = animations.getAnimation(animationName);
        if (animation == null || isSkippableEmptyLocomotion(animationName, animation)) {
            return null;
        }
        return new ExactAnimation(animation, resource);
    }


    /**
     * Mirrors Point Blank's handling of an explicitly empty off-ground
     * fallback: keep searching for an exact definition rather than replacing
     * the base animation with an empty one.
     */
    private static boolean isSkippableEmptyLocomotion(String animationName,
                                                       Animation animation) {
        if (animation.boneAnimations().length != 0) {
            return false;
        }
        String baseName = AnimationLibraryPaths.stripDualWieldSuffix(animationName);
        return "animation.model.off_ground".equals(baseName)
                || "animation.model.off_ground_sprinting".equals(baseName);
    }


    private static Identifier parseLibrary(String weaponName, String value) {
        String normalized = AnimationLibraryPaths.normalizeIdentifier(value);
        if (normalized == null) {
            warnOnce(weaponName, value, "invalid resource identifier");
            return null;
        }

        Identifier identifier = Identifier.tryParse(normalized);
        if (identifier == null) {
            warnOnce(weaponName, value, "invalid Minecraft identifier");
            return null;
        }
        return identifier;
    }


    private static void warnOnce(String weaponName, String value, String reason) {
        if (WARNED.add(weaponName + "|" + value)) {
            LOGGER.warn("Ignoring animation library '{}' for weapon '{}': {}",
                    value, weaponName, reason);
        }
    }


    private record CachedFallbacks(Identifier[] original, Identifier[] combined) {
    }


    private record ExactAnimation(Animation animation, Identifier source) {
    }
}
