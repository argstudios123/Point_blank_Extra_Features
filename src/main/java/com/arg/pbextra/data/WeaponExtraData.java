package com.arg.pbextra.data;

import com.arg.pbextra.animation.AnimationLibraryPaths;
import com.arg.pbextra.bullethole.BulletHoleConfig;
import com.arg.pbextra.constraint.AdsConstraintConfig;
import com.arg.pbextra.constraint.AdsConstraintConstants;
import com.arg.pbextra.crosshair.CustomCrosshairConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Collections;
import java.util.List;


public final class WeaponExtraData {

    public static final WeaponExtraData EMPTY =
            new WeaponExtraData(false, MeleeConfig.DISABLED, AdsConstraintConfig.DISABLED,
                    false, false, AdsReloadZoomControlConfig.DISABLED);

    private final boolean toggleAiming;
    private final MeleeConfig melee;
    private final AdsConstraintConfig adsConstraint;
    private final boolean noADSDuringInspect;
    private final boolean disableAdsDuringReload;
    private final AdsReloadZoomControlConfig adsReloadZoomControl;
    private final BulletHoleConfig bulletHoles;
    private final CustomCrosshairConfig customCrosshair;
    private final List<String> additionalFirstPersonFallbacks;
    private final List<String> declaredFirstPersonFallbacks;

    /** Backwards-compatible constructor; the ADS features default to disabled. */
    public WeaponExtraData(boolean toggleAiming, MeleeConfig melee) {
        this(toggleAiming, melee, AdsConstraintConfig.DISABLED, false, false,
                AdsReloadZoomControlConfig.DISABLED);
    }

    public WeaponExtraData(boolean toggleAiming, MeleeConfig melee,
                           AdsConstraintConfig adsConstraint) {
        this(toggleAiming, melee, adsConstraint, false, false,
                AdsReloadZoomControlConfig.DISABLED);
    }

    /** Backwards-compatible constructor; the reload ADS switch defaults to disabled. */
    public WeaponExtraData(boolean toggleAiming, MeleeConfig melee,
                           AdsConstraintConfig adsConstraint,
                           boolean noADSDuringInspect,
                           AdsReloadZoomControlConfig adsReloadZoomControl) {
        this(toggleAiming, melee, adsConstraint, noADSDuringInspect, false,
                adsReloadZoomControl);
    }

    public WeaponExtraData(boolean toggleAiming, MeleeConfig melee,
                           AdsConstraintConfig adsConstraint,
                           boolean noADSDuringInspect,
                           boolean disableAdsDuringReload,
                           AdsReloadZoomControlConfig adsReloadZoomControl) {
        this(toggleAiming, melee, adsConstraint, noADSDuringInspect, disableAdsDuringReload,
                adsReloadZoomControl, Collections.emptyList(), Collections.emptyList());
    }

    public WeaponExtraData(boolean toggleAiming, MeleeConfig melee,
                           AdsConstraintConfig adsConstraint,
                           boolean noADSDuringInspect,
                           boolean disableAdsDuringReload,
                           AdsReloadZoomControlConfig adsReloadZoomControl,
                           List<String> additionalFirstPersonFallbacks,
                           List<String> declaredFirstPersonFallbacks) {
        this(toggleAiming, melee, adsConstraint, noADSDuringInspect, disableAdsDuringReload,
                adsReloadZoomControl, additionalFirstPersonFallbacks,
                declaredFirstPersonFallbacks, BulletHoleConfig.BUILT_IN_DEFAULTS);
    }

    public WeaponExtraData(boolean toggleAiming, MeleeConfig melee,
                           AdsConstraintConfig adsConstraint,
                           boolean noADSDuringInspect,
                           boolean disableAdsDuringReload,
                           AdsReloadZoomControlConfig adsReloadZoomControl,
                           List<String> additionalFirstPersonFallbacks,
                           List<String> declaredFirstPersonFallbacks,
                           BulletHoleConfig bulletHoles) {
        this(toggleAiming, melee, adsConstraint, noADSDuringInspect, disableAdsDuringReload,
                adsReloadZoomControl, additionalFirstPersonFallbacks,
                declaredFirstPersonFallbacks, bulletHoles, CustomCrosshairConfig.DISABLED);
    }

    public WeaponExtraData(boolean toggleAiming, MeleeConfig melee,
                           AdsConstraintConfig adsConstraint,
                           boolean noADSDuringInspect,
                           boolean disableAdsDuringReload,
                           AdsReloadZoomControlConfig adsReloadZoomControl,
                           List<String> additionalFirstPersonFallbacks,
                           List<String> declaredFirstPersonFallbacks,
                           BulletHoleConfig bulletHoles,
                           CustomCrosshairConfig customCrosshair) {
        this.toggleAiming = toggleAiming;
        this.melee = melee == null ? MeleeConfig.DISABLED : melee;
        this.adsConstraint = adsConstraint == null ? AdsConstraintConfig.DISABLED : adsConstraint;
        this.noADSDuringInspect = noADSDuringInspect;
        this.disableAdsDuringReload = disableAdsDuringReload;
        this.adsReloadZoomControl = adsReloadZoomControl == null
                ? AdsReloadZoomControlConfig.DISABLED : adsReloadZoomControl;
        this.bulletHoles = bulletHoles == null
                ? BulletHoleConfig.BUILT_IN_DEFAULTS : bulletHoles;
        this.customCrosshair = customCrosshair == null
                ? CustomCrosshairConfig.DISABLED : customCrosshair;
        this.additionalFirstPersonFallbacks = additionalFirstPersonFallbacks == null
                ? List.of() : List.copyOf(additionalFirstPersonFallbacks);
        this.declaredFirstPersonFallbacks = declaredFirstPersonFallbacks == null
                ? List.of() : List.copyOf(declaredFirstPersonFallbacks);
    }

    public boolean isToggleAiming() {
        return toggleAiming;
    }

    public MeleeConfig getMelee() {
        return melee;
    }

    public AdsConstraintConfig getAdsConstraint() {
        return adsConstraint;
    }

    public boolean isNoADSDuringInspect() {
        return noADSDuringInspect;
    }

    public boolean isDisableAdsDuringReload() {
        return disableAdsDuringReload;
    }

    public AdsReloadZoomControlConfig getAdsReloadZoomControl() {
        return adsReloadZoomControl;
    }

    /** Effective bullet-hole configuration for this weapon (never null). */
    public BulletHoleConfig getBulletHoles() {
        return bulletHoles;
    }

    /** Effective custom crosshair configuration for this weapon (never null). */
    public CustomCrosshairConfig getCustomCrosshair() {
        return customCrosshair;
    }

    /** Additive array entries from {@code firstPersonFallbackAnimations}. */
    public List<String> getAdditionalFirstPersonFallbacks() {
        return additionalFirstPersonFallbacks;
    }

    /** All declared fallback values, including the native string form. */
    public List<String> getDeclaredFirstPersonFallbacks() {
        return declaredFirstPersonFallbacks;
    }


    public static WeaponExtraData fromJson(JsonObject obj) {
        return fromJson(obj, BulletHoleConfig.BUILT_IN_DEFAULTS);
    }

    /**
     * Parses one weapon definition, resolving bullet holes over the supplied
     * content-pack defaults (or the built-in defaults for Point Blank's own guns).
     */
    public static WeaponExtraData fromJson(JsonObject obj, BulletHoleConfig packDefaults) {
        if (obj == null) {
            return EMPTY;
        }

        boolean toggleAiming = parseToggleAiming(obj);
        MeleeConfig melee = parseMelee(obj);
        AdsConstraintConfig adsConstraint = parseAdsConstraint(obj);
        boolean noADSDuringInspect = parseNoADSDuringInspect(obj);
        boolean disableAdsDuringReload = parseDisableAdsDuringReload(obj);
        AdsReloadZoomControlConfig adsReloadZoomControl = parseAdsReloadZoomControl(obj);
        BulletHoleConfig bulletHoles = parseBulletHoles(obj, packDefaults);
        CustomCrosshairConfig customCrosshair = parseCustomCrosshair(obj);
        JsonElement fallbackAnimations = obj.get(AnimationLibraryPaths.KEY);
        List<String> additionalFirstPersonFallbacks =
                AnimationLibraryPaths.parseAdditional(fallbackAnimations);
        List<String> declaredFirstPersonFallbacks =
                AnimationLibraryPaths.parseDeclared(fallbackAnimations);

        return new WeaponExtraData(toggleAiming, melee, adsConstraint,
                noADSDuringInspect, disableAdsDuringReload, adsReloadZoomControl,
                additionalFirstPersonFallbacks, declaredFirstPersonFallbacks,
                bulletHoles, customCrosshair);
    }


    /**
     * Reads the optional {@code bulletHoles} feature. A missing entry inherits
     * the pack defaults (bullet holes stay enabled), which is what makes every
     * gun work without editing its JSON.
     */
    private static BulletHoleConfig parseBulletHoles(JsonObject obj, BulletHoleConfig packDefaults) {
        if (obj.has("features") && obj.get("features").isJsonArray()) {
            JsonArray features = obj.getAsJsonArray("features");
            for (JsonElement feature : features) {
                if (feature.isJsonObject()
                        && isFeatureOfType(feature.getAsJsonObject(),
                                BulletHoleConfig.FEATURE_TYPE)) {
                    return BulletHoleConfig.resolve(
                            BulletHoleConfig.Overrides.fromJson(feature.getAsJsonObject()),
                            packDefaults);
                }
            }
        }

        JsonElement root = obj.get(BulletHoleConfig.SETTINGS_KEY_CAMEL);
        if (root == null) {
            root = obj.get(BulletHoleConfig.SETTINGS_KEY);
        }
        if (root != null && root.isJsonObject()) {
            return BulletHoleConfig.resolve(
                    BulletHoleConfig.Overrides.fromJson(root.getAsJsonObject()),
                    packDefaults);
        }

        return BulletHoleConfig.resolve(BulletHoleConfig.Overrides.NONE, packDefaults);
    }



    /**
     * Reads the optional {@code custom_crosshair} feature. A missing entry
     * leaves Point Blank's normal crosshair untouched. A present entry with a
     * missing texture is retained so the renderer can fall back safely.
     */
    private static CustomCrosshairConfig parseCustomCrosshair(JsonObject obj) {
        if (obj.has("features") && obj.get("features").isJsonArray()) {
            JsonArray features = obj.getAsJsonArray("features");
            for (JsonElement feature : features) {
                if (feature.isJsonObject()
                        && isFeatureOfType(feature.getAsJsonObject(),
                                CustomCrosshairConfig.FEATURE_TYPE)) {
                    return CustomCrosshairConfig.fromJson(feature.getAsJsonObject());
                }
            }
        }

        JsonElement root = obj.get(CustomCrosshairConfig.SETTINGS_KEY_CAMEL);
        if (root == null) {
            root = obj.get(CustomCrosshairConfig.SETTINGS_KEY);
        }
        if (root != null && root.isJsonObject()) {
            return CustomCrosshairConfig.fromJson(root.getAsJsonObject());
        }

        return CustomCrosshairConfig.DISABLED;
    }


    private static boolean parseToggleAiming(JsonObject obj) {
        if (!obj.has("toggle_aiming") || !obj.get("toggle_aiming").isJsonPrimitive()) {
            return false;
        }

        JsonPrimitive primitive = obj.getAsJsonPrimitive("toggle_aiming");
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }

        String value = primitive.getAsString();
        return "true".equalsIgnoreCase(value) || "enabled".equalsIgnoreCase(value);
    }


    private static MeleeConfig parseMelee(JsonObject obj) {
        if (obj.has("features") && obj.get("features").isJsonArray()) {
            JsonArray features = obj.getAsJsonArray("features");
            for (JsonElement feature : features) {
                if (feature.isJsonObject() && isMeleeFeature(feature.getAsJsonObject())) {
                    return MeleeConfig.fromJson(feature.getAsJsonObject());
                }
            }
        }

        if (obj.has("melee") && obj.get("melee").isJsonObject()) {
            return MeleeConfig.fromJson(obj.getAsJsonObject("melee"));
        }

        return MeleeConfig.DISABLED;
    }

    private static boolean isMeleeFeature(JsonObject feature) {
        if (!feature.has("type") || !feature.get("type").isJsonPrimitive()) {
            return false;
        }
        return "melee".equalsIgnoreCase(feature.get("type").getAsString());
    }


    private static AdsConstraintConfig parseAdsConstraint(JsonObject obj) {
        if (obj.has("features") && obj.get("features").isJsonArray()) {
            JsonArray features = obj.getAsJsonArray("features");
            for (JsonElement feature : features) {
                if (feature.isJsonObject()
                        && isFeatureOfType(feature.getAsJsonObject(),
                                AdsConstraintConstants.FEATURE_TYPE)) {
                    return AdsConstraintConfig.fromJson(feature.getAsJsonObject());
                }
            }
        }

        if (obj.has("adsConstraint") && obj.get("adsConstraint").isJsonObject()) {
            return AdsConstraintConfig.fromJson(obj.getAsJsonObject("adsConstraint"));
        }

        return AdsConstraintConfig.DISABLED;
    }


    private static boolean parseNoADSDuringInspect(JsonObject obj) {
        if (!obj.has("noADSDuringInspect") || !obj.get("noADSDuringInspect").isJsonPrimitive()) {
            return false;
        }

        JsonPrimitive primitive = obj.getAsJsonPrimitive("noADSDuringInspect");
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (!primitive.isString()) {
            return false;
        }

        String value = primitive.getAsString();
        return "true".equalsIgnoreCase(value) || "enabled".equalsIgnoreCase(value);
    }


    private static boolean parseDisableAdsDuringReload(JsonObject obj) {
        if (!obj.has("disableAdsDuringreload") || !obj.get("disableAdsDuringreload").isJsonPrimitive()) {
            return false;
        }

        JsonPrimitive primitive = obj.getAsJsonPrimitive("disableAdsDuringreload");
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (!primitive.isString()) {
            return false;
        }

        String value = primitive.getAsString();
        return "true".equalsIgnoreCase(value) || "enabled".equalsIgnoreCase(value);
    }


    private static AdsReloadZoomControlConfig parseAdsReloadZoomControl(JsonObject obj) {
        if (obj.has("features") && obj.get("features").isJsonArray()) {
            JsonArray features = obj.getAsJsonArray("features");
            for (JsonElement feature : features) {
                if (feature.isJsonObject()
                        && isFeatureOfType(feature.getAsJsonObject(),
                                AdsReloadZoomControlConfig.FEATURE_TYPE)) {
                    return AdsReloadZoomControlConfig.fromJson(feature.getAsJsonObject());
                }
            }
        }
        return AdsReloadZoomControlConfig.DISABLED;
    }

    private static boolean isFeatureOfType(JsonObject feature, String type) {
        if (!feature.has("type") || !feature.get("type").isJsonPrimitive()) {
            return false;
        }
        return type.equalsIgnoreCase(feature.get("type").getAsString());
    }
}
