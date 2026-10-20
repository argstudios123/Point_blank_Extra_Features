package com.arg.pbextra.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PointBlankExtraConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_MOD;

    public static final ModConfigSpec.BooleanValue ENABLE_WATER_LAVA_IMPACT_PARTICLES;

    public static final ModConfigSpec.BooleanValue ENABLE_RESERVE_AMMO_COUNTER;

    public static final ModConfigSpec.BooleanValue ENABLE_BULLET_HOLES;

    public static final ModConfigSpec.ConfigValue<String> UI_EDITOR_THEME;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");

        ENABLE_MOD = builder
                .comment(
                        "Enables or disables all Point Blank Extra Features functionality.",
                        "When false, this mod leaves Point Blank's behavior unchanged."
                )
                .define("enableMod", true);

        ENABLE_WATER_LAVA_IMPACT_PARTICLES = builder
                .comment(
                        "Spawns water and lava Particles wherever the hitscan or projecile hits the water or lava" ,
                        "Surfaces"
                )
                .define("enableWaterLavaImpactParticles", true);

        ENABLE_RESERVE_AMMO_COUNTER = builder
                .comment(
                        "Replaces the 2nd number of ammo counter which shows mag capacity to an actual reserve ammo"
                )
                .define("reserveAmmoCounter", true);

        ENABLE_BULLET_HOLES = builder
                .comment(
                        "Master switch for bullet-hole decals on block impacts.",
                        "When false, no new bullet holes are created and existing ones are removed.",
                        "Weapon JSON and content-pack settings cannot override this switch."
                )
                .define("enableBulletHoles", true);

        UI_EDITOR_THEME = builder
                .comment(
                        "Editor-only color theme: DEFAULT, DARK, LIGHT, MID_DARK or MID_LIGHT.",
                        "This only affects the UI editor interface, never the HUD being edited."
                )
                .define("uiEditorTheme", "DEFAULT");

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Global addon switch. Sound playback uses this rather than the visual
     * bullet-hole switch, so turning decals off does not mute impact sounds.
     */
    public static boolean isModEnabled() {
        if (!SPEC.isLoaded()) {
            return ENABLE_MOD.getDefault();
        }
        return ENABLE_MOD.get();
    }

    /**
     * Effective bullet-hole switch. The master key wins over everything a
     * weapon JSON or content pack can declare; the existing global mod switch
     * also disables the feature, like every other addon feature.
     */
    public static boolean areBulletHolesEnabled() {
        if (!SPEC.isLoaded()) {
            return ENABLE_BULLET_HOLES.getDefault();
        }
        return ENABLE_MOD.get() && ENABLE_BULLET_HOLES.get();
    }

    private PointBlankExtraConfig() {
    }
}
