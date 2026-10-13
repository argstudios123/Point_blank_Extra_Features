package com.arg.pbextra.config;

import com.arg.pbextra.PointBlankExtraFeatures;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The addon's configuration.
 *
 * <p>The Forge and NeoForge builds declare this with NeoForge's {@code ModConfigSpec}
 * as a {@code CLIENT} config, which produces {@code config/pointblankextra-client.toml}
 * with a {@code [general]} table. Fabric has no built-in configuration system, so this
 * class keeps the <em>exact same file, table and keys</em> and reads them itself: a
 * configuration file written by the Forge, NeoForge or Fabric build keeps working
 * unchanged, and a missing file is created with the same layout and the same defaults.</p>
 *
 * <p>{@link #ENABLE_MOD}, {@link #ENABLE_WATER_LAVA_IMPACT_PARTICLES} and
 * {@link #ENABLE_RESERVE_AMMO_COUNTER} expose the same {@code get()} /
 * {@code getDefault()} surface the NeoForge values did, so no other class had to
 * change. The file is re-read (at most twice a second) after it has been modified, so
 * editing a value takes effect without restarting the game, just like NeoForge's
 * config watcher did; {@link #setChangeListener(Runnable)} lets the client drop its
 * configuration-dependent caches at that moment.</p>
 */
public final class PointBlankExtraConfig {

    private static final String FILE_NAME = PointBlankExtraFeatures.MOD_ID + "-client.toml";
    private static final String TABLE = "general";

    /** How often the file is checked for external edits. */
    private static final long POLL_NANOS = 500_000_000L;

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    private static final Object LOCK = new Object();
    private static final Map<String, BooleanValue> VALUES = new LinkedHashMap<>();

    public static final Spec SPEC = new Spec();

    public static final BooleanValue ENABLE_MOD = new BooleanValue(
            "enableMod", true,
            "Enables or disables all Point Blank Extra Features functionality.",
            "When false, this mod leaves Point Blank's behavior unchanged.");

    public static final BooleanValue ENABLE_WATER_LAVA_IMPACT_PARTICLES = new BooleanValue(
            "enableWaterLavaImpactParticles", true,
            "Spawns water and lava Particles wherever the hitscan or projecile hits the water or lava",
            "Surfaces");

    public static final BooleanValue ENABLE_RESERVE_AMMO_COUNTER = new BooleanValue(
            "reserveAmmoCounter", true,
            "Replaces the 2nd number of ammo counter which shows mag capaity to an actual reserve ammo");

    private static boolean loaded;
    private static long lastModified = Long.MIN_VALUE;
    private static long lastCheckedNanos;
    private static Runnable changeListener;

    private PointBlankExtraConfig() {
    }

    /** Loads the configuration (creating it when missing) and arms the file watcher. */
    public static void initialize() {
        synchronized (LOCK) {
            if (loaded) {
                return;
            }
            loaded = true;
        }
        reloadFromDisk(true);
        logCurrentValues();
    }

    /**
     * Registers a callback that runs whenever a value actually changes, which is how
     * the client invalidates its configuration-dependent caches.
     */
    public static void setChangeListener(Runnable listener) {
        changeListener = listener;
    }

    private static void refresh() {
        if (!loaded) {
            // A value was queried before the initialiser ran; make sure the file has
            // actually been read instead of reporting stale defaults.
            initialize();
            return;
        }
        long now = System.nanoTime();
        if (now - lastCheckedNanos < POLL_NANOS) {
            return;
        }
        lastCheckedNanos = now;
        reloadFromDisk(false);
    }

    private static void reloadFromDisk(boolean createIfMissing) {
        boolean changed = false;
        synchronized (LOCK) {
            try {
                if (!Files.isRegularFile(FILE)) {
                    if (createIfMissing) {
                        writeDefaults();
                        lastModified = lastModifiedMillis();
                    }
                    return;
                }

                long modified = lastModifiedMillis();
                if (modified == lastModified) {
                    return;
                }
                lastModified = modified;
                changed = apply(parse(Files.readAllLines(FILE, StandardCharsets.UTF_8)));
            } catch (IOException | RuntimeException exception) {
                PointBlankExtraFeatures.LOGGER.warn(
                        "Could not read {}; keeping the current settings ({})",
                        FILE, exception.toString());
            }
        }

        if (changed) {
            logCurrentValues();
            Runnable listener = changeListener;
            if (listener != null) {
                listener.run();
            }
        }
    }

    /**
     * Applies the parsed values. A key missing from the file falls back to its default,
     * which is what the NeoForge spec did when it rewrote an incomplete file.
     */
    private static boolean apply(Map<String, Boolean> parsed) {
        boolean changed = false;
        for (BooleanValue value : VALUES.values()) {
            Boolean parsedValue = parsed.get(value.key);
            boolean next = parsedValue == null ? value.defaultValue : parsedValue;
            if (next != value.current) {
                value.current = next;
                changed = true;
            }
        }
        return changed;
    }

    /**
     * A deliberately small TOML reader: the addon's file only ever contains the
     * {@code [general]} table with boolean entries, so unknown tables, unknown keys and
     * malformed lines are skipped instead of failing the load.
     */
    private static Map<String, Boolean> parse(List<String> lines) {
        Map<String, Boolean> parsed = new LinkedHashMap<>();
        String section = "";

        for (String rawLine : lines) {
            String line = stripComment(rawLine);
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }

            if (!section.isEmpty() && !TABLE.equalsIgnoreCase(section)) {
                // Not our table; Point Blank Extra Features only reads [general].
                continue;
            }

            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            String key = line.substring(0, separator).trim();
            BooleanValue known = VALUES.get(key);
            if (known == null) {
                continue;
            }

            String rawValue = line.substring(separator + 1).trim();
            Boolean parsedValue = parseBoolean(rawValue);
            if (parsedValue == null) {
                PointBlankExtraFeatures.LOGGER.warn(
                        "Ignoring the invalid value '{}' for '{}' in {}", rawValue, key, FILE);
                // Keep whatever is currently in effect rather than resetting the key.
                parsed.put(key, known.current);
                continue;
            }
            parsed.put(key, parsedValue);
        }
        return parsed;
    }

    private static String stripComment(String rawLine) {
        int comment = rawLine.indexOf('#');
        String line = comment < 0 ? rawLine : rawLine.substring(0, comment);
        return line.trim();
    }

    private static Boolean parseBoolean(String text) {
        String value = text.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    /** Writes the file with the same layout and defaults the NeoForge build produced. */
    private static void writeDefaults() {
        try {
            Path parent = FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            StringBuilder text = new StringBuilder();
            text.append('\n').append('[').append(TABLE).append("]\n");
            for (BooleanValue value : VALUES.values()) {
                for (String comment : value.comments) {
                    text.append('\t').append('#').append(comment).append('\n');
                }
                text.append('\t').append(value.key).append(" = ")
                        .append(value.defaultValue).append('\n');
            }

            Files.writeString(FILE, text.toString(), StandardCharsets.UTF_8);
            PointBlankExtraFeatures.LOGGER.info(
                    "Wrote the default Point Blank Extra Features configuration to {}", FILE);
        } catch (IOException exception) {
            PointBlankExtraFeatures.LOGGER.warn(
                    "Could not write the default configuration to {} ({})",
                    FILE, exception.toString());
        }
    }

    private static long lastModifiedMillis() {
        try {
            return Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException exception) {
            return Long.MIN_VALUE;
        }
    }

    private static void logCurrentValues() {
        PointBlankExtraFeatures.LOGGER.debug(
                "Point Blank Extra Features configuration ({}): enableMod={}, "
                        + "enableWaterLavaImpactParticles={}, reserveAmmoCounter={}",
                FILE, ENABLE_MOD.current, ENABLE_WATER_LAVA_IMPACT_PARTICLES.current,
                ENABLE_RESERVE_AMMO_COUNTER.current);
    }

    /**
     * Mirrors NeoForge's {@code ModConfigSpec}: {@code isLoaded()} reports whether the
     * file has been read, so the defensive check in the water/lava impact feature keeps
     * behaving the same way.
     */
    public static final class Spec {

        private Spec() {
        }

        public boolean isLoaded() {
            return loaded;
        }
    }

    /** Mirrors NeoForge's {@code ModConfigSpec.BooleanValue}. */
    public static final class BooleanValue {

        private final String key;
        private final boolean defaultValue;
        private final String[] comments;
        private volatile boolean current;

        private BooleanValue(String key, boolean defaultValue, String... comments) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.comments = comments;
            this.current = defaultValue;
            VALUES.put(key, this);
        }

        /** The value in effect, re-reading the file first when it changed on disk. */
        public boolean get() {
            refresh();
            return current;
        }

        public boolean getDefault() {
            return defaultValue;
        }
    }
}
