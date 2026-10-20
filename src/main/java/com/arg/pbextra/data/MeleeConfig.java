package com.arg.pbextra.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class MeleeConfig {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String DAMAGE_CATS_AND_OCELOTS_KEY = "damageCatsandOcalotsEntities";

    public static final double DEFAULT_DAMAGE = 6.0;
    public static final int DEFAULT_COOLDOWN_TICKS = 20;
    public static final double DEFAULT_REACH = 3.0;
    /**
     * The knockback value is expressed in tenths of the vanilla melee impulse, so
     * the default 4.0 reproduces the 0.4 blocks/tick impulse this addon has always
     * used for melee hits.
     */
    public static final double DEFAULT_KNOCKBACK = 4.0;
    /** Full horizontal width of the melee cone, in degrees. */
    public static final double DEFAULT_ANGLE_RANGE = 40.0;
    public static final double MAX_ANGLE_RANGE = 360.0;

    public static final MeleeConfig DISABLED =
            new MeleeConfig(false, 0.0, 0, 0.0, DEFAULT_KNOCKBACK, DEFAULT_ANGLE_RANGE,
                    false, Collections.emptyList());

    private final boolean enabled;
    private final double damage;
    private final int cooldownTicks;
    private final double reach;
    private final double knockback;
    private final double angleRange;
    private final boolean damageCatsAndOcelots;
    private final List<MeleeAnimation> animations;

    public MeleeConfig(boolean enabled, double damage, int cooldownTicks, double reach,
                       double knockback, double angleRange, boolean damageCatsAndOcelots,
                       List<MeleeAnimation> animations) {
        this.enabled = enabled;
        this.damage = Math.max(0.0, damage);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.reach = Math.max(0.0, reach);
        this.knockback = Double.isFinite(knockback)
                ? Math.max(0.0, knockback) : DEFAULT_KNOCKBACK;
        this.angleRange = Double.isFinite(angleRange)
                ? Math.min(MAX_ANGLE_RANGE, Math.max(0.0, angleRange))
                : DEFAULT_ANGLE_RANGE;
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

    /**
     * The configured knockback strength, in tenths of the vanilla melee impulse.
     * A value of 4.0 therefore produces the 0.4 blocks/tick impulse used by
     * vanilla sword attacks (and by this addon before the value was honoured).
     */
    public double getKnockback() {
        return knockback;
    }

    /**
     * The full horizontal width of the frontal attack cone, in degrees. A value
     * of 40 means 20 degrees to either side of the player's facing direction.
     */
    public double getAngleRange() {
        return angleRange;
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

        double damage = getDouble(obj, "damage", DEFAULT_DAMAGE);
        if (damage < 0.0) {
            LOGGER.warn("Melee damage must be non-negative ({}), using 0.0", damage);
            damage = 0.0;
        }

        int cooldown = getInt(obj, "cooldown", DEFAULT_COOLDOWN_TICKS);
        if (cooldown < 0) {
            LOGGER.warn("Melee cooldown must be non-negative ({}), using 0", cooldown);
            cooldown = 0;
        }

        double reach = getDouble(obj, "reach", DEFAULT_REACH);
        if (reach < 0.0) {
            LOGGER.warn("Melee reach must be non-negative ({}), using 0.0", reach);
            reach = 0.0;
        }

        // Knockback is expressed in tenths of the vanilla melee impulse. A JSON
        // without the key keeps the historical 0.4 blocks/tick force.
        double knockback = getDouble(obj, "knockback", DEFAULT_KNOCKBACK);
        if (knockback < 0.0) {
            LOGGER.warn("Melee knockback must be non-negative ({}), using 0.0", knockback);
            knockback = 0.0;
        }

        // angleRange is the FULL horizontal width of the frontal cone, so 40
        // leaves 20 degrees of leeway on either side of the facing direction.
        double angleRange = getDouble(obj, "angleRange", DEFAULT_ANGLE_RANGE);
        if (angleRange < 0.0) {
            LOGGER.warn("Melee angleRange must be non-negative ({}), using 0.0", angleRange);
            angleRange = 0.0;
        } else if (angleRange > MAX_ANGLE_RANGE) {
            LOGGER.warn("Melee angleRange must be at most {} degrees ({}), using {}",
                    MAX_ANGLE_RANGE, angleRange, MAX_ANGLE_RANGE);
            angleRange = MAX_ANGLE_RANGE;
        }

        // Absent key => false, which is also the behaviour of a weapon JSON written
        // before this option existed: cats and ocelots take no melee damage then.
        boolean damageCatsAndOcelots = getBoolean(obj, DAMAGE_CATS_AND_OCELOTS_KEY, false);

        List<MeleeAnimation> animations = parseAnimations(obj);

        return new MeleeConfig(enabled, damage, cooldown, reach, knockback, angleRange,
                damageCatsAndOcelots, animations);
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
            double value = obj.get(key).getAsDouble();
            if (Double.isFinite(value)) {
                return value;
            }
            LOGGER.warn("Melee '{}' must be a finite number ({}), using {}", key, value, def);
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
