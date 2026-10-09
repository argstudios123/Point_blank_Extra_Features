package com.argos.pbextra.data;

import com.argos.pbextra.constraint.AdsConstraintConfig;
import com.argos.pbextra.constraint.AdsConstraintConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


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
        this.toggleAiming = toggleAiming;
        this.melee = melee == null ? MeleeConfig.DISABLED : melee;
        this.adsConstraint = adsConstraint == null ? AdsConstraintConfig.DISABLED : adsConstraint;
        this.noADSDuringInspect = noADSDuringInspect;
        this.disableAdsDuringReload = disableAdsDuringReload;
        this.adsReloadZoomControl = adsReloadZoomControl == null
                ? AdsReloadZoomControlConfig.DISABLED : adsReloadZoomControl;
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


    public static WeaponExtraData fromJson(JsonObject obj) {
        if (obj == null) {
            return EMPTY;
        }

        boolean toggleAiming = parseToggleAiming(obj);
        MeleeConfig melee = parseMelee(obj);
        AdsConstraintConfig adsConstraint = parseAdsConstraint(obj);
        boolean noADSDuringInspect = parseNoADSDuringInspect(obj);
        boolean disableAdsDuringReload = parseDisableAdsDuringReload(obj);
        AdsReloadZoomControlConfig adsReloadZoomControl = parseAdsReloadZoomControl(obj);

        return new WeaponExtraData(toggleAiming, melee, adsConstraint,
                noADSDuringInspect, disableAdsDuringReload, adsReloadZoomControl);
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
