package com.argos.pbextra.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public final class MeleeConfig {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String DAMAGE_CATS_AND_OCELOTS_KEY = "damageCatsandOcalotsEntities";

    public static final MeleeConfig DISABLED =
            new MeleeConfig(false, 0.0, 0, 0.0, false, Collections.emptyList());

    private final boolean enabled;
    private final double damage;
    private final int cooldownTicks;
    private final double reach;
    private final boolean damageCatsAndOcelots;
    private final List<MeleeAnimation> animations;

    public MeleeConfig(boolean enabled, double damage, int cooldownTicks, double reach,
                       boolean damageCatsAndOcelots, List<MeleeAnimation> animations) {
        this.enabled = enabled;
        this.damage = Math.max(0.0, damage);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.reach = Math.max(0.0, reach);
        this.damageCatsAndOcelots = damageCatsAndOcelots;
        this.animations = Collections.unmodifiableList(new ArrayList<>(animations));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getDamage() {
        return damage;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public double getReach() {
        return reach;
    }


    public boolean isDamageCatsAndOcelots() {
        return damageCatsAndOcelots;
    }

    public List<MeleeAnimation> getAnimations() {
        return animations;
    }

    /** The GeckoLib animation names to choose from. May be empty. */
    public List<String> getAnimationNames() {
        List<String> names = new ArrayList<>(animations.size());
        for (MeleeAnimation animation : animations) {
            names.add(animation.getName());
        }
        return Collections.unmodifiableList(names);
    }

    /**
     * Parses a {@code "melee"} JSON object.
     * Missing or invalid fields fall back to sensible defaults rather than throwing.
     */
    public static MeleeConfig fromJson(JsonObject obj) {
        if (obj == null) {
            return DISABLED;
        }

        boolean enabled = getBoolean(obj, "enabled", false);

        double damage = getDouble(obj, "damage", 6.0);
        if (damage < 0.0) {
            LOGGER.warn("Melee damage must be non-negative ({}), using 0.0", damage);
            damage = 0.0;
        }

        int cooldown = getInt(obj, "cooldown", 20);
        if (cooldown < 0) {
            LOGGER.warn("Melee cooldown must be non-negative ({}), using 0", cooldown);
            cooldown = 0;
        }

        double reach = getDouble(obj, "reach", 3.0);
        if (reach < 0.0) {
            LOGGER.warn("Melee reach must be non-negative ({}), using 0.0", reach);
            reach = 0.0;
        }

        // Absent key => false, which is also the behaviour of a weapon JSON written
        // before this option existed: cats and ocelots take no melee damage then.
        boolean damageCatsAndOcelots = getBoolean(obj, DAMAGE_CATS_AND_OCELOTS_KEY, false);

        List<MeleeAnimation> animations = parseAnimations(obj);

        return new MeleeConfig(enabled, damage, cooldown, reach, damageCatsAndOcelots,
                animations);
    }


    private static List<MeleeAnimation> parseAnimations(JsonObject obj) {
        List<MeleeAnimation> animations = new ArrayList<>();

        if (obj.has("meleeAnimations") && obj.get("meleeAnimations").isJsonArray()) {
            for (JsonElement element : obj.getAsJsonArray("meleeAnimations")) {
                if (!element.isJsonObject()) {
                    LOGGER.warn("Ignoring non-object entry in melee 'meleeAnimations' array");
                    continue;
                }
                MeleeAnimation animation = MeleeAnimation.fromJson(element.getAsJsonObject());
                if (animation != null) {
                    animations.add(animation);
                } else {
                    LOGGER.warn("Ignoring malformed entry in melee 'meleeAnimations' array "
                            + "(expected a non-empty \"name\")");
                }
            }
        }

        if (animations.isEmpty() && obj.has("animations") && obj.get("animations").isJsonArray()) {
            for (JsonElement element : obj.getAsJsonArray("animations")) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    String name = element.getAsString();
                    if (!name.isBlank()) {
                        animations.add(new MeleeAnimation(name, 0, 0));
                    }
                } else {
                    LOGGER.warn("Ignoring non-string entry in legacy melee 'animations' array");
                }
            }
        }

        return animations;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean def) {
        if (obj.has(key) && obj.get(key).isJsonPrimitive()
                && obj.get(key).getAsJsonPrimitive().isBoolean()) {
            return obj.get(key).getAsBoolean();
        }
        return def;
    }

    private static double getDouble(JsonObject obj, String key, double def) {
        if (obj.has(key) && obj.get(key).isJsonPrimitive()
                && obj.get(key).getAsJsonPrimitive().isNumber()) {
            return obj.get(key).getAsDouble();
        }
        return def;
    }

    private static int getInt(JsonObject obj, String key, int def) {
        if (obj.has(key) && obj.get(key).isJsonPrimitive()
                && obj.get(key).getAsJsonPrimitive().isNumber()) {
            return obj.get(key).getAsInt();
        }
        return def;
    }
}
