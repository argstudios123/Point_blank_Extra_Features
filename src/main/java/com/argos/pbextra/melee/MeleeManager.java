package com.argos.pbextra.melee;

import com.argos.pbextra.data.MeleeAnimation;
import com.argos.pbextra.data.MeleeConfig;
import com.argos.pbextra.data.WeaponExtraDataManager;
import com.argos.pbextra.network.ModNetwork;
import com.mojang.logging.LogUtils;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class MeleeManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();

    private static final Map<UUID, Map<ResourceLocation, Long>> LAST_MELEE_TICK =
            new ConcurrentHashMap<>();

    private static final Map<UUID, PendingImpact> PENDING_IMPACTS = new ConcurrentHashMap<>();

    private MeleeManager() {
    }


    public static void handleRequest(ServerPlayer player, boolean offhand) {
        if (player == null) {
            return;
        }

        ItemStack stack = getHeldGun(player, offhand);
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem)) {
            return;
        }

        MeleeConfig config = WeaponExtraDataManager.getMeleeConfig(stack);
        if (!config.isEnabled()) {
            return;
        }

        long now = player.level().getGameTime();
        if (!isCooldownExpired(player, stack, config, now)) {
            return;
        }

        MeleeAnimation animation = selectAnimation(config);

        // The cooldown starts when the player swings, not when the damage lands,
        // so a second press during the swing is already rejected.
        recordCooldown(player, stack, now);

        // Send the animation first: it must start immediately, it is only the hit
        // that waits for preMelee. Both times are passed on in milliseconds of
        // animation time, exactly as configured - never as Minecraft ticks.
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new MeleeResponsePacket(offhand,
                        animation == null ? "" : animation.getName(),
                        animation == null ? 0 : animation.getDurationMillis(),
                        animation == null ? 0 : animation.getPreMeleeMillis())
        );

        if (animation == null) {
            // No animation configured: there is nothing to synchronise with, so the
            // hit is applied right away, exactly as it was before.
            PENDING_IMPACTS.remove(player.getUUID());
            performHit(player, offhand);
            return;
        }

        // The swing is now in flight until the client reports that the animation it
        // was granted has reached preMelee.
        PENDING_IMPACTS.put(player.getUUID(), new PendingImpact(animation.getName(), offhand));
    }


    public static void handleImpact(ServerPlayer player, String animationName) {
        if (player == null) {
            return;
        }

        PendingImpact pending = PENDING_IMPACTS.get(player.getUUID());
        if (pending == null || !pending.animationName.equals(animationName)) {
            // Nothing in flight, or an announcement that belongs to another swing.
            return;
        }

        PENDING_IMPACTS.remove(player.getUUID());
        performHit(player, pending.offhand);
    }


    private static void performHit(ServerPlayer player, boolean offhand) {
        if (player.isRemoved() || player.hasDisconnected() || player.isDeadOrDying()) {
            return;
        }

        ItemStack stack = getHeldGun(player, offhand);
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem)) {
            // The weapon was swapped away or dropped while the swing was playing.
            return;
        }

        MeleeConfig config = WeaponExtraDataManager.getMeleeConfig(stack);
        if (!config.isEnabled()) {
            return;
        }

        LivingEntity target = findTarget(player, config.getReach());
        boolean damageExcluded = target != null
                && isCatOrOcelot(target)
                && !config.isDamageCatsAndOcelots();

        int targetId = -1;
        boolean hit = false;
        if (target != null && !damageExcluded) {
            hit = target.hurt(player.level().damageSources().playerAttack(player),
                    (float) config.getDamage());
            if (hit) {
                targetId = target.getId();
                applyKnockback(player, target);
            }
        }

        if (target != null && (hit || damageExcluded)) {
            // The attack still happens normally - the arm swing is kept when the
            // damage was skipped for a cat or ocelot.
            player.swing(offhand ? net.minecraft.world.InteractionHand.OFF_HAND
                    : net.minecraft.world.InteractionHand.MAIN_HAND);
        }

        if (hit) {
            LOGGER.debug("Melee hit target {} for {} damage", targetId, config.getDamage());
        } else if (damageExcluded) {
            LOGGER.debug("Melee attack on cat/ocelot target {} dealt no damage: "
                            + "\"{}\" is false for this weapon",
                    target.getId(), MeleeConfig.DAMAGE_CATS_AND_OCELOTS_KEY);
        } else {
            LOGGER.debug("Melee performed with no target hit");
        }
    }


    private static boolean isCatOrOcelot(LivingEntity entity) {
        if (entity instanceof Cat || entity instanceof Ocelot) {
            return true;
        }
        EntityType<?> type = entity.getType();
        return type == EntityType.CAT || type == EntityType.OCELOT;
    }

    /**
     * Clears cooldown and in-flight swing state for a player (used on death and
     * respawn, so an interrupted swing cannot be reported afterwards).
     */
    public static void clearCooldown(UUID playerId) {
        LAST_MELEE_TICK.remove(playerId);
        PENDING_IMPACTS.remove(playerId);
    }

    private static boolean isCooldownExpired(ServerPlayer player, ItemStack stack,
                                             MeleeConfig config, long now) {
        Map<ResourceLocation, Long> cooldowns = LAST_MELEE_TICK.get(player.getUUID());
        if (cooldowns == null) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        Long last = id == null ? null : cooldowns.get(id);
        return last == null || (now - last) >= config.getCooldownTicks();
    }

    private static void recordCooldown(ServerPlayer player, ItemStack stack, long now) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return;
        }
        LAST_MELEE_TICK
                .computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
                .put(id, now);
    }

    private static ItemStack getHeldGun(ServerPlayer player, boolean offhand) {
        ItemStack stack = offhand ? player.getOffhandItem() : player.getMainHandItem();
        if (stack.getItem() instanceof GunItem) {
            return stack;
        }
        return ItemStack.EMPTY;
    }

    private static LivingEntity findTarget(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.x * reach, look.y * reach, look.z * reach);
        AABB box = player.getBoundingBox()
                .expandTowards(look.scale(reach))
                .inflate(1.0);

        EntityHitResult result = net.minecraft.world.entity.projectile.ProjectileUtil
                .getEntityHitResult(
                        player.level(),
                        player,
                        eye,
                        end,
                        box,
                        e -> e != null && e instanceof LivingEntity && e != player
                );

        if (result != null && result.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static void applyKnockback(ServerPlayer player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.0001) {
            return;
        }
        target.knockback(0.4, dx / length, dz / length);
    }

    /**
     * Picks one of the weapon's configured melee animations at random.
     * Returns {@code null} when the weapon has no melee animations configured, in
     * which case the swing is still performed but nothing is played.
     */
    private static MeleeAnimation selectAnimation(MeleeConfig config) {
        List<MeleeAnimation> animations = config.getAnimations();
        if (animations.isEmpty()) {
            LOGGER.debug("No melee animations configured for this weapon; skipping melee animation");
            return null;
        }
        return animations.get(RANDOM.nextInt(animations.size()));
    }

    /** An accepted swing whose impact frame the client has not reported yet. */
    private static final class PendingImpact {

        private final String animationName;
        private final boolean offhand;

        private PendingImpact(String animationName, boolean offhand) {
            this.animationName = animationName;
            this.offhand = offhand;
        }
    }
}
