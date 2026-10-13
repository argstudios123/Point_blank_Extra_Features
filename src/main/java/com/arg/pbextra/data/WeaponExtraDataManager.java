package com.arg.pbextra.data;

import com.arg.pbextra.ammo.ReserveAmmoCounter;
import com.arg.pbextra.constraint.AdsConstraintConfig;
import com.arg.pbextra.integration.PointBlankJsonIntegration;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class WeaponExtraDataManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, WeaponExtraData> DATA = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    private WeaponExtraDataManager() {
    }


    /**
     * Rebuilds the weapon index from the latest available sources and reports what
     * happened. Extension directories are always scanned from disk; the resource
     * manager (when supplied) contributes Point Blank's built-in weapon resources.
     * <p>
     * The previous index is replaced atomically and only after a successful scan, so
     * a failed reload never leaves the cache partially updated or empty.
     */
    public static Result reloadTracked(ResourceManager resourceManager) {
        Map<String, WeaponExtraData> next = new HashMap<>();
        Map<String, String> failuresById = new LinkedHashMap<>();

        boolean extensionsScanned = PointBlankJsonIntegration.scanExtensions(
                FMLPaths.GAMEDIR.get().resolve("pointblank"), next, failuresById);
        boolean builtInScanned = PointBlankJsonIntegration.scanBuiltIn(
                resourceManager, next, failuresById);

        Map<String, WeaponExtraData> previous;
        synchronized (DATA) {
            previous = new HashMap<>(DATA);
        }

        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, String> failure : failuresById.entrySet()) {
            WeaponExtraData previousData = previous.get(failure.getKey());
            if (previousData != null) {
                // A file that failed to parse must not drop the weapon it belongs to:
                // keep the definition that is already indexed and report the problem.
                next.putIfAbsent(failure.getKey(), previousData);
                failures.add(failure.getValue() + " (kept the previously indexed definition)");
            } else {
                failures.add(failure.getValue());
            }
        }

        if (!extensionsScanned && !builtInScanned && !previous.isEmpty()) {
            // Nothing could be scanned at all: never trade a working index for an
            // empty one just because a source was not readable.
            failures.add("no weapon definitions could be scanned; kept the previously indexed data");
            LOGGER.error("Point Blank Extra Features could not rebuild the weapon index "
                            + "({} problem(s)); keeping the previous {} definition(s)",
                    failuresById.size(), previous.size());
            return new Result(previous.size(), failures, false);
        }

        synchronized (DATA) {
            DATA.clear();
            DATA.putAll(next);
            loaded = true;
        }
        invalidateClientCaches();
        logIndexed(next);
        if (!failures.isEmpty()) {
            LOGGER.warn("Point Blank Extra Features indexed {} weapon definition(s) with {} problem(s)",
                    next.size(), failures.size());
        }
        return new Result(next.size(), failures, true);
    }

    /**
     * Resource-reload-listener entry point. Kept {@code void} so it can be used as a
     * {@link net.minecraft.server.packs.resources.ResourceManagerReloadListener}
     * (see {@code ClientEvents} and {@code ServerEvents}).
     */
    public static void reload(ResourceManager resourceManager) {
        reloadTracked(resourceManager);
    }

    /**
     * The reserve ammo counter keeps a short lived cache of the last computed value
     * keyed on the ItemStack identity, which survives a resource reload, so drop it
     * whenever the weapon index changes. The client-only class is never touched on a
     * dedicated server.
     */
    private static void invalidateClientCaches() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ReserveAmmoCounter.invalidate());
    }

    private static void logIndexed(Map<String, WeaponExtraData> data) {
        LOGGER.debug("Point Blank Extra Features indexed {} weapon definition(s)", data.size());
        data.forEach((name, extra) -> {
            if (extra.getAdsConstraint().isEnabled()) {
                LOGGER.info("Point Blank Extra Features: adsConstraint enabled for '{}' (camera={})",
                        name, extra.getAdsConstraint().isCamera());
            }
            if (!extra.isToggleAiming() && !extra.getMelee().isEnabled()
                    && !extra.isNoADSDuringInspect() && !extra.isDisableAdsDuringReload()) {
                return;
            }
            LOGGER.info("Point Blank Extra Features: loaded '{}' (toggleAiming={}, melee={}, "
                            + "noADSDuringInspect={}, disableAdsDuringreload={})",
                    name, extra.isToggleAiming(), describeMelee(extra.getMelee()),
                    extra.isNoADSDuringInspect(), extra.isDisableAdsDuringReload());
        });
    }

    private static String describeMelee(MeleeConfig melee) {
        if (!melee.isEnabled()) {
            return "disabled";
        }
        return String.format(Locale.ROOT,
                "enabled[damage=%.1f, cooldown=%d, reach=%.1f, %s=%s, animations=%s]",
                melee.getDamage(), melee.getCooldownTicks(), melee.getReach(),
                MeleeConfig.DAMAGE_CATS_AND_OCELOTS_KEY, melee.isDamageCatsAndOcelots(),
                melee.getAnimations());
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (DATA) {
            if (loaded) {
                return;
            }
            reload(null);
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static WeaponExtraData getData(ResourceLocation itemId) {
        if (itemId == null) {
            return WeaponExtraData.EMPTY;
        }
        ensureLoaded();
        return DATA.getOrDefault(itemId.getPath(), WeaponExtraData.EMPTY);
    }

    public static WeaponExtraData getData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return WeaponExtraData.EMPTY;
        }
        return getData(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    /** Whether the given item has toggle aiming enabled via its weapon JSON. */
    public static boolean isToggleAiming(ItemStack stack) {
        return getData(stack).isToggleAiming();
    }

    /** The melee configuration for the given item (never null). */
    public static MeleeConfig getMeleeConfig(ItemStack stack) {
        return getData(stack).getMelee();
    }

    /** The ADS constraint configuration for the given item (never null). */
    public static AdsConstraintConfig getAdsConstraintConfig(ItemStack stack) {
        return getData(stack).getAdsConstraint();
    }

    /** Whether the given item has "no ADS during inspect" enabled via its weapon JSON. */
    public static boolean isNoADSDuringInspect(ResourceLocation itemId) {
        return getData(itemId).isNoADSDuringInspect();
    }

    /** Whether the given item has "no ADS during inspect" enabled via its weapon JSON. */
    public static boolean isNoADSDuringInspect(ItemStack stack) {
        return getData(stack).isNoADSDuringInspect();
    }

    /** Whether the given item has "ADS disabled during reload" enabled via its weapon JSON. */
    public static boolean isDisableAdsDuringReload(ResourceLocation itemId) {
        return getData(itemId).isDisableAdsDuringReload();
    }

    /** Whether the given item has "ADS disabled during reload" enabled via its weapon JSON. */
    public static boolean isDisableAdsDuringReload(ItemStack stack) {
        return getData(stack).isDisableAdsDuringReload();
    }

    /** The ADS reload zoom relaxation configuration for the given item (never null). */
    public static AdsReloadZoomControlConfig getAdsReloadZoomControlConfig(ItemStack stack) {
        return getData(stack).getAdsReloadZoomControl();
    }

    /** Clears the index, forcing it to be rebuilt on next access. */
    public static void clear() {
        synchronized (DATA) {
            DATA.clear();
            loaded = false;
        }
    }

    /**
     * Outcome of one {@link #reloadTracked(ResourceManager)}.
     *
     * @param loaded    number of weapon definitions in the index after the reload
     * @param failures  human readable problems, already including the source path
     * @param committed {@code false} when the reload failed as a whole and the
     *                  previously indexed data was kept instead
     */
    public record Result(int loaded, List<String> failures, boolean committed) {
    }
}
