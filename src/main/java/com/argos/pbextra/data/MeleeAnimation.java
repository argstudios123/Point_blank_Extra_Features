package com.argos.pbextra.data;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

// the melee animation handler kind of
public final class MeleeAnimation {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final String name;
    private final int durationMillis;
    private final int preMeleeMillis;

    public MeleeAnimation(String name, int durationMillis, int preMeleeMillis) {
        this.name = name;
        this.durationMillis = Math.max(0, durationMillis);
        this.preMeleeMillis = Math.max(0, preMeleeMillis);
    }

    public String getName() {
        return name;
    }
    public int getDurationMillis() {
        return durationMillis;
    }
    public int getPreMeleeMillis() {
        return preMeleeMillis;
    }


    public static MeleeAnimation fromJson(JsonObject obj) {
        if (obj == null || !obj.has("name") || !obj.get("name").isJsonPrimitive()
                || !obj.get("name").getAsJsonPrimitive().isString()) {
            return null;
        }

        String name = obj.get("name").getAsString();
        if (name.isBlank()) {
            return null;
        }

        long durationMillis = getLong(obj, "duration", -1L);
        int duration = durationMillis <= 0L
                ? 0
                : (int) Math.min(Integer.MAX_VALUE, durationMillis);

        long preMelee = getLong(obj, "preMelee", 0L);
        int preMeleeMillis = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, preMelee));

        if (duration > 0 && preMeleeMillis > duration) {
            LOGGER.warn("Melee animation '{}' has preMelee={} ms beyond its duration={} ms, "
                    + "so the hit would never be reached", name, preMeleeMillis, duration);
        }

        return new MeleeAnimation(name, duration, preMeleeMillis);
    }

    private static long getLong(JsonObject obj, String key, long def) {
        if (obj.has(key) && obj.get(key).isJsonPrimitive()
                && obj.get(key).getAsJsonPrimitive().isNumber()) {
            return obj.get(key).getAsLong();
        }
        return def;
    }

    @Override
    public String toString() {
        return name + "[duration=" + durationMillis + "ms, preMelee=" + preMeleeMillis + "ms]";
    }
}
