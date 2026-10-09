package com.argos.pbextra.data;

import com.argos.pbextra.constraint.AdsConstraintConfig;
import com.argos.pbextra.integration.PointBlankJsonIntegration;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class WeaponExtraDataManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, WeaponExtraData> DATA = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    private WeaponExtraDataManager() {
    }


    public static void reload(ResourceManager resourceManager) {
        Map<String, WeaponExtraData> next = new HashMap<>();
        PointBlankJsonIntegration.scanExtensions(
                FMLPaths.GAMEDIR.get().resolve("pointblank"), next);
        PointBlankJsonIntegration.scanBuiltIn(resourceManager, next);

        synchronized (DATA) {
            DATA.clear();
            DATA.putAll(next);
            loaded = true;
        }
        logIndexed(next);
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
}
