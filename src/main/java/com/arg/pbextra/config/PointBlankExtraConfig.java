package com.arg.pbextra.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PointBlankExtraConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_MOD;

    public static final ModConfigSpec.BooleanValue ENABLE_WATER_LAVA_IMPACT_PARTICLES;

    public static final ModConfigSpec.BooleanValue ENABLE_RESERVE_AMMO_COUNTER;

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
                        "Replaces the 2nd number of ammo counter which shows mag capaity to an actual reserve ammo"
                )
                .define("reserveAmmoCounter", true);

        builder.pop();

        SPEC = builder.build();
    }

    private PointBlankExtraConfig() {
    }
}
