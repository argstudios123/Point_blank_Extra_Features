package com.argos.pbextra.impact;

import com.argos.pbextra.PointBlankExtraFeatures;
import com.argos.pbextra.config.PointBlankExtraConfig;
import com.vicmatskiv.pointblank.event.BlockHitEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


public final class WaterLavaImpactParticles {

    private static final int SPLASH_PARTICLES = 30;
    private static final int BUBBLE_PARTICLES = 6;

    private static final int LAVA_PARTICLES = 12;
    private static final int LAVA_SMOKE_PARTICLES = 5;


    private static final double SPREAD = 0.35D;
    private static final double SPLASH_SPREAD = 0.30D;
    private static final double SPLASH_SPEED = 0.35D;

    private static final double BUBBLE_SPEED = 0.05D;
    private static final double LAVA_SPEED = 0.10D;
    private static final double LAVA_SMOKE_SPEED = 0.05D;

    private WaterLavaImpactParticles() {
    }

    @Mod.EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {

        @SubscribeEvent
        public static void onBlockHit(BlockHitEvent event) {
            if (!isEnabled()) {
                return;
            }

            // Another mod consumed the impact; leave that decision alone.
            if (event.isCanceled()) {
                return;
            }

            BlockHitResult hitResult = event.getBlockHitResult();
            if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
                return;
            }

            Entity projectile = event.getProjectile();
            Entity source = projectile != null ? projectile : event.getPlayer();
            if (source == null) {
                return;
            }

            Level level = source.level();
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            BlockPos fluidPos = fluidPosOf(level,
                    hitResult.getBlockPos(),
                    hitResult.getDirection());
            if (fluidPos == null) {
                return;
            }

            FluidState fluid = level.getFluidState(fluidPos);
            if (fluid.is(FluidTags.WATER)) {
                Vec3 impact = hitResult.getLocation();
                Vec3 surface = waterSurfaceCrossing(serverLevel, source, projectile, event.getPlayer(), impact);
                spawnWater(serverLevel, impact, surface == null ? impact : surface);
            } else if (fluid.is(FluidTags.LAVA)) {
                spawnLava(serverLevel, hitResult.getLocation());
            }
        }


        private static void spawnWater(ServerLevel level, Vec3 impact, Vec3 splashLocation) {

            level.sendParticles(ParticleTypes.SPLASH,
                    splashLocation.x, splashLocation.y, splashLocation.z,
                    SPLASH_PARTICLES, SPLASH_SPREAD, SPLASH_SPREAD, SPLASH_SPREAD, SPLASH_SPEED);


            level.sendParticles(ParticleTypes.BUBBLE,
                    impact.x, impact.y, impact.z,
                    BUBBLE_PARTICLES, SPREAD, SPREAD, SPREAD, BUBBLE_SPEED);
        }



        private static Vec3 waterSurfaceCrossing(ServerLevel level, Entity source, Entity projectile,
                                                 LivingEntity shooter, Vec3 impact) {
            // For hitscan shots the ray starts at the shooter's eye, exactly like Point Blank's
            // own hitscan. For projectiles the entity is still at the start of the segment it just
            // travelled (Point Blank moves it after the impact is handled), so its position is the
            // real start of the shot's path.
            Vec3 origin = projectile != null
                    ? projectile.position()
                    : (shooter != null ? shooter.getEyePosition() : null);
            if (origin == null) {
                return null;
            }

            if (isWater(level, origin)) {
                return null;
            }

            BlockHitResult crossing = level.clip(new ClipContext(origin, impact,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, source));

            if (crossing.getType() != HitResult.Type.BLOCK
                    || !isWater(level, crossing.getBlockPos())
                    || crossing.getLocation().distanceToSqr(impact) < 1.0E-4D) {
                return null;
            }

            return crossing.getLocation();
        }


        private static boolean isWater(Level level, Vec3 position) {
            return isWater(level, BlockPos.containing(position.x, position.y, position.z));
        }


        private static boolean isWater(Level level, BlockPos pos) {
            return level.getFluidState(pos).is(FluidTags.WATER);
        }

        private static void spawnLava(ServerLevel level, Vec3 location) {
            level.sendParticles(ParticleTypes.LAVA,
                    location.x, location.y, location.z,
                    LAVA_PARTICLES, SPREAD, SPREAD, SPREAD, LAVA_SPEED);
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    location.x, location.y, location.z,
                    LAVA_SMOKE_PARTICLES, SPREAD, SPREAD, SPREAD, LAVA_SMOKE_SPEED);
        }
    }


    private static BlockPos fluidPosOf(Level level, BlockPos hitPos, Direction face) {
        if (!level.getFluidState(hitPos).isEmpty()) {
            return hitPos;
        }

        if (face != null) {
            BlockPos facePos = hitPos.relative(face);
            if (!level.getFluidState(facePos).isEmpty()) {
                return facePos;
            }
        }

        return null;
    }


    private static boolean isEnabled() {
        if (!PointBlankExtraConfig.SPEC.isLoaded()) {
            return PointBlankExtraConfig.ENABLE_WATER_LAVA_IMPACT_PARTICLES.getDefault();
        }

        return PointBlankExtraConfig.ENABLE_MOD.get()
                && PointBlankExtraConfig.ENABLE_WATER_LAVA_IMPACT_PARTICLES.get();
    }
}
