package com.arg.pbextra.client.bulletholes;

import com.arg.pbextra.bullethole.BulletHoleConfig;
import com.arg.pbextra.config.PointBlankExtraConfig;
import com.arg.pbextra.data.WeaponExtraDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


/**
 * Client-side block-debris particles for one bullet impact.
 *
 * <p>Particles are derived from the actual {@link BlockState} at the impact
 * position through {@link ParticleTypes#BLOCK}. That provider resolves the
 * block's own particle sprite (including model-based and modded blocks), so no
 * block ids are hardcoded and unusual or unknown states cannot crash here. The
 * server already sends the impact position in {@link
 * com.arg.pbextra.network.BulletHoleImpactPacket}, so no extra packet is
 * needed.</p>
 *
 * <p>This is independent of the persistent decal renderer: decals are gated by
 * the client's bullet-hole master switch, while debris is gated by the addon
 * master switch, the per-gun {@code enabled} flag and {@code
 * max_block_particles}.</p>
 */
public final class BulletHoleParticles {

    /** Same sanity limit the decal path applies to stale or absurd packets. */
    private static final double MAX_PACKET_DISTANCE_SQR = 256.0D * 256.0D;

    /** Small outward offset so debris does not start inside the hit block. */
    private static final double SURFACE_OFFSET = 0.02D;

    private BulletHoleParticles() {
    }

    public static void spawn(Identifier weaponId, BlockPos pos, Direction face,
                             Vec3 location) {
        if (pos == null || face == null || location == null
                || !PointBlankExtraConfig.isModEnabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        if (Vec3.atCenterOf(pos).distanceToSqr(minecraft.player.position())
                > MAX_PACKET_DISTANCE_SQR) {
            return;
        }

        BulletHoleConfig config = weaponId == null
                ? BulletHoleConfig.BUILT_IN_DEFAULTS
                : WeaponExtraDataManager.getData(weaponId).getBulletHoles();
        if (!config.isEnabled()) {
            return;
        }
        int count = config.getMaxBlockParticles();
        if (count <= 0) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        // The optional position lets NeoForge select the model-based particle
        // sprite for blocks whose texture depends on their location. The
        // provider returns no particle for states that do not support terrain
        // particles, so this stays safe for unusual modded states.
        BlockParticleOption particle =
                new BlockParticleOption(ParticleTypes.BLOCK, state, pos);

        RandomSource random = level.getRandom();
        double normalX = face.getStepX();
        double normalY = face.getStepY();
        double normalZ = face.getStepZ();

        for (int i = 0; i < count; i++) {
            double x = location.x + (random.nextDouble() - 0.5D) * 0.2D
                    + normalX * SURFACE_OFFSET;
            double y = location.y + (random.nextDouble() - 0.5D) * 0.2D
                    + normalY * SURFACE_OFFSET;
            double z = location.z + (random.nextDouble() - 0.5D) * 0.2D
                    + normalZ * SURFACE_OFFSET;

            double speed = 0.05D + random.nextDouble() * 0.10D;
            double velocityX = normalX * speed
                    + (random.nextDouble() - 0.5D) * 0.10D;
            double velocityY = normalY * speed
                    + (random.nextDouble() - 0.5D) * 0.10D + 0.02D;
            double velocityZ = normalZ * speed
                    + (random.nextDouble() - 0.5D) * 0.10D;

            level.addParticle(particle, x, y, z,
                    velocityX, velocityY, velocityZ);
        }
    }
}
