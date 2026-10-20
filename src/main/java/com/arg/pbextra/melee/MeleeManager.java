package com.arg.pbextra.melee;

import com.arg.pbextra.data.MeleeAnimation;
import com.arg.pbextra.data.MeleeConfig;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.mojang.logging.LogUtils;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Server-authoritative melee attacks.
 * <p>
 * The client only reports that the melee key is down (subject to its own local
 * cooldown). The server decides whether the swing is legal, picks the animation,
 * schedules the damage using the animation's {@code preMelee} time and performs
 * all target selection itself. A swing is therefore never dependent on a
 * client-side animation callback, and a client cannot choose targets or damage.
 * <p>
 * Each player has at most one unexecuted impact at a time. That guarantees that
 * one swing can never deal damage twice and that two swings cannot overlap,
 * while still allowing the next swing as soon as the cooldown permits.
 */
public final class MeleeManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();

    /** The vanilla melee impulse is 0.4 blocks per tick. */
    private static final double IMPULSE_PER_KNOCKBACK_UNIT = 0.1;

    private static final Map<UUID, PlayerMeleeState> STATES = new ConcurrentHashMap<>();

    private MeleeManager() {
    }


    /**
     * Handles one melee request from a client. Requests may arrive repeatedly
     * while the key is held; all of the gating happens here on the server.
     */
    public static void handleRequest(ServerPlayer player, boolean offhand) {
        if (!isUsable(player) || player.isSprinting()) {
            return;
        }

        ItemStack stack = getHeldGun(player, offhand);
        if (stack.isEmpty()) {
            return;
        }

        MeleeConfig config = WeaponExtraDataManager.getMeleeConfig(stack);
        if (!config.isEnabled()) {
            return;
        }

        Identifier weaponId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (weaponId == null) {
            return;
        }

        long now = serverTick(player);
        PlayerMeleeState state = STATES.computeIfAbsent(player.getUUID(),
                key -> new PlayerMeleeState());

        // An accepted swing stays in flight until its impact executes. Ignoring
        // requests in the meantime prevents overlapping attacks and accidental
        // cancellation of a hit that has not landed yet.
        if (state.hasPendingImpact()) {
            return;
        }

        if (!state.isCooldownExpired(weaponId, now)) {
            return;
        }

        // The cooldown starts when the swing is accepted, not when the damage
        // lands, exactly as before.
        state.recordCooldown(weaponId, now, config.getCooldownTicks());

        MeleeAnimation animation = selectAnimation(config);
        if (animation == null) {
            // No animation configured: there is nothing to synchronise with, so
            // the hit is applied immediately, exactly as it was before.
            performHit(player, offhand, config);
            return;
        }

        int impactDelayTicks = MeleeGeometry.millisToTicks(animation.getPreMeleeMillis());
        state.beginPendingImpact(weaponId, offhand, now + impactDelayTicks);

        PacketDistributor.sendToPlayer(player,
                new MeleeResponsePacket(offhand, animation.getName(),
                        animation.getDurationMillis()));

        if (impactDelayTicks == 0) {
            executePendingImpact(player, state);
        }
    }


    /**
     * Advances every player's pending impact. Because the delay is owned by the
     * server, an interrupted or unrendered client animation can never leave an
     * attack permanently stuck or silently drop its damage.
     */
    public static void tick(MinecraftServer server) {
        if (STATES.isEmpty()) {
            return;
        }

        long now = server.getTickCount();
        Iterator<Map.Entry<UUID, PlayerMeleeState>> iterator = STATES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerMeleeState> entry = iterator.next();
            PlayerMeleeState state = entry.getValue();

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                // The player left the server; never keep their attack state.
                iterator.remove();
                continue;
            }

            if (!state.hasPendingImpact()) {
                state.pruneExpiredCooldowns(now);
                if (state.isIdle()) {
                    iterator.remove();
                }
                continue;
            }

            if (!isUsable(player)) {
                state.clearPendingImpact();
                continue;
            }

            ItemStack stack = getHeldGun(player, state.pendingOffhand);
            if (stack.isEmpty()) {
                state.clearPendingImpact();
                continue;
            }

            Identifier heldWeaponId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (heldWeaponId == null || !heldWeaponId.equals(state.pendingWeaponId)) {
                // The weapon was swapped away while the swing was playing.
                state.clearPendingImpact();
                continue;
            }

            MeleeConfig config = WeaponExtraDataManager.getMeleeConfig(stack);
            if (!config.isEnabled()) {
                state.clearPendingImpact();
                continue;
            }

            if (now >= state.pendingImpactTick) {
                executePendingImpact(player, state);
            }
        }
    }


    /**
     * Executes the one pending impact of a player, clearing it first so that it
     * can never be executed twice.
     */
    private static void executePendingImpact(ServerPlayer player, PlayerMeleeState state) {
        boolean offhand = state.pendingOffhand;
        state.clearPendingImpact();

        if (!isUsable(player)) {
            return;
        }

        ItemStack stack = getHeldGun(player, offhand);
        if (stack.isEmpty()) {
            return;
        }

        MeleeConfig config = WeaponExtraDataManager.getMeleeConfig(stack);
        if (!config.isEnabled()) {
            return;
        }

        performHit(player, offhand, config);
    }


    private static void performHit(ServerPlayer player, boolean offhand, MeleeConfig config) {
        List<LivingEntity> targets = findTargets(player, config);
        if (targets.isEmpty()) {
            LOGGER.debug("Melee performed with no target in reach and inside the cone");
            return;
        }

        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        player.swing(hand);

        boolean damageCatsAndOcelots = config.isDamageCatsAndOcelots();
        float damage = (float) config.getDamage();
        ServerLevel level = player.level();
        DamageSource damageSource = level.damageSources().playerAttack(player);
        int damagedTargets = 0;

        for (LivingEntity target : targets) {
            if (!damageCatsAndOcelots && isCatOrOcelot(target)) {
                LOGGER.debug("Melee attack on cat/ocelot target {} dealt no damage: "
                                + "\"{}\" is false for this weapon",
                        target.getId(), MeleeConfig.DAMAGE_CATS_AND_OCELOTS_KEY);
                continue;
            }

            boolean hurt = target.hurtServer(level, damageSource, damage);
            if (!hurt) {
                continue;
            }

            // Knockback is applied exactly once per damaged target.
            applyKnockback(player, target, config.getKnockback());
            damagedTargets++;

            LOGGER.debug("Melee hit target {} for {} damage", target.getId(), damage);
        }

        if (damagedTargets == 0) {
            LOGGER.debug("Melee swing hit no damageable targets");
        }
    }



    /**
     * Collects every eligible target inside the configured frontal cone.
     * <p>
     * Candidates come from one bounding-box query around the attacker, never a
     * world-wide scan. The reach is measured from the eyes to the nearest point
     * of the target's box (so large entities are handled sensibly), while the
     * angle uses the horizontal direction toward the target's box centre.
     */
    private static List<LivingEntity> findTargets(ServerPlayer player, MeleeConfig config) {
        double reach = config.getReach();
        if (reach <= 0.0) {
            return Collections.emptyList();
        }

        AABB searchBox = player.getBoundingBox().inflate(reach);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> entity != player
                        && entity.isAlive()
                        && !entity.isSpectator()
                        && player.canAttack(entity));

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Vec3 eye = player.getEyePosition();
        double yawRadians = Math.toRadians(player.getYRot());
        double facingX = -Math.sin(yawRadians);
        double facingZ = Math.cos(yawRadians);
        double reachSquared = reach * reach;
        double angleRange = config.getAngleRange();

        List<LivingEntity> targets = new ArrayList<>(candidates.size());
        for (LivingEntity candidate : candidates) {
            AABB box = candidate.getBoundingBox();
            double distanceSquared = MeleeGeometry.distanceSquaredToBox(
                    eye.x, eye.y, eye.z,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            if (distanceSquared > reachSquared) {
                continue;
            }

            Vec3 center = box.getCenter();
            if (!MeleeGeometry.isWithinCone(center.x - player.getX(),
                    center.z - player.getZ(), facingX, facingZ, angleRange)) {
                continue;
            }

            targets.add(candidate);
        }

        return targets;
    }


    private static void applyKnockback(ServerPlayer player, LivingEntity target,
                                       double configuredKnockback) {
        double impulse = configuredKnockback * IMPULSE_PER_KNOCKBACK_UNIT;
        if (impulse <= 0.0) {
            return;
        }

        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length < 1.0E-4) {
            // The target is exactly on top of the attacker. Push it along the
            // attacker's facing direction rather than skipping the knockback.
            double yawRadians = Math.toRadians(player.getYRot());
            dx = -Math.sin(yawRadians);
            dz = Math.cos(yawRadians);
        } else {
            dx /= length;
            dz /= length;
        }

        target.knockback(impulse, dx, dz);
    }


    private static boolean isCatOrOcelot(LivingEntity entity) {
        if (entity instanceof Cat || entity instanceof Ocelot) {
            return true;
        }
        EntityType<?> type = entity.getType();
        return type == EntityType.CAT || type == EntityType.OCELOT;
    }



    /**
     * Clears all attack and cooldown state for a player. Used on death,
     * respawn and logout so an interrupted swing cannot be reported afterwards
     * and a stale cooldown cannot block the next session.
     */
    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) {
            STATES.remove(playerId);
        }
    }


    private static boolean isUsable(ServerPlayer player) {
        return player != null
                && !player.isRemoved()
                && !player.hasDisconnected()
                && !player.isDeadOrDying()
                && !player.isSpectator();
    }


    private static long serverTick(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server == null ? player.level().getGameTime() : server.getTickCount();
    }


    private static ItemStack getHeldGun(ServerPlayer player, boolean offhand) {
        ItemStack stack = offhand ? player.getOffhandItem() : player.getMainHandItem();
        return stack.getItem() instanceof GunItem ? stack : ItemStack.EMPTY;
    }


    /**
     * Picks one of the weapon's configured melee animations at random.
     * Returns {@code null} when the weapon has no melee animations configured, in
     * which case the swing is still performed but nothing is played.
     */
    private static MeleeAnimation selectAnimation(MeleeConfig config) {
        List<MeleeAnimation> animations = config.getAnimations();
        if (animations.isEmpty()) {
            return null;
        }
        return animations.get(RANDOM.nextInt(animations.size()));
    }


    /** Per-player attack state. All access happens on the server thread. */
    private static final class PlayerMeleeState {

        /** Remaining cooldown per weapon item, so switching weapons behaves as before. */
        private final Map<Identifier, Long> cooldownEndTicks = new HashMap<>();

        private boolean pendingImpact;
        private long pendingImpactTick;
        private Identifier pendingWeaponId;
        private boolean pendingOffhand;

        private boolean hasPendingImpact() {
            return pendingImpact;
        }

        private void beginPendingImpact(Identifier weaponId, boolean offhand, long impactTick) {
            this.pendingWeaponId = weaponId;
            this.pendingOffhand = offhand;
            this.pendingImpactTick = Math.max(0L, impactTick);
            this.pendingImpact = true;
        }

        private void clearPendingImpact() {
            this.pendingImpact = false;
            this.pendingImpactTick = 0L;
            this.pendingWeaponId = null;
            this.pendingOffhand = false;
        }

        private boolean isCooldownExpired(Identifier weaponId, long now) {
            Long cooldownEnd = cooldownEndTicks.get(weaponId);
            return cooldownEnd == null || now >= cooldownEnd;
        }

        private void recordCooldown(Identifier weaponId, long now, int cooldownTicks) {
            cooldownEndTicks.put(weaponId, now + Math.max(0, cooldownTicks));
        }

        private void pruneExpiredCooldowns(long now) {
            cooldownEndTicks.values().removeIf(cooldownEnd -> now >= cooldownEnd);
        }

        private boolean isIdle() {
            return !hasPendingImpact() && cooldownEndTicks.isEmpty();
        }
    }
}
