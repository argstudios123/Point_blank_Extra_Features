package com.argos.pbextra.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PointBlankExtraConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_MOD;

    public static final ForgeConfigSpec.BooleanValue ENABLE_WATER_LAVA_IMPACT_PARTICLES;

    public static final ForgeConfigSpec.BooleanValue ENABLE_RESERVE_AMMO_COUNTER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

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
