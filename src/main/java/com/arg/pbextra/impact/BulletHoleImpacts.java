package com.arg.pbextra.impact;

import com.arg.pbextra.PointBlankExtraFeatures;
import com.arg.pbextra.bullethole.BulletHoleConfig;
import com.arg.pbextra.bullethole.BulletHoleImpactContext;
import com.arg.pbextra.bullethole.BulletHoleSoundConfig;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.arg.pbextra.network.BulletHoleImpactPacket;
import com.vicmatskiv.pointblank.Platform;
import com.vicmatskiv.pointblank.entity.SlowProjectile;
import com.vicmatskiv.pointblank.event.BlockHitEvent;
import com.vicmatskiv.pointblank.event.SubscribeEvent2;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.item.HurtingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;



public final class BulletHoleImpacts {

    private static final double BROADCAST_RADIUS = 96.0D;

    private static final Map<Identifier, TagKey<Block>> TAG_CACHE =
            new ConcurrentHashMap<>();
    private static final Set<Identifier> WARNED_SOUNDS = ConcurrentHashMap.newKeySet();

    private BulletHoleImpacts() {
    }

    /** Runs after every mod has been constructed, so Point Blank's event bus exists. */
    @EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID)
    public static final class ModBus {

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(BulletHoleImpacts::install);
        }
    }

    private static void install() {
        try {
            Platform.getInstance().getEventBus().register(new BlockHitListener());
            PointBlankExtraFeatures.LOGGER.debug(
                    "Registered the bullet-hole impact listener on Point Blank's event bus");
        } catch (RuntimeException exception) {
            PointBlankExtraFeatures.LOGGER.warn(
                    "Could not register the bullet-hole impact listener on Point Blank's "
                            + "event bus; bullet holes are disabled ({})",
                    exception.toString());
        }
    }

    /** Point Blank event listener object expected by {@code Platform#getEventBus()}. */
    public static final class BlockHitListener {

        @SubscribeEvent2
        public void onBlockHit(BlockHitEvent event) {
            handleBlockHit(event);
        }
    }

    private static void handleBlockHit(BlockHitEvent event) {
        if (event.isCanceled()) {
            return;
        }

        BlockHitResult hitResult = event.getBlockHitResult();
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        LivingEntity player = event.getPlayer();
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        HurtingItem source = BulletHoleImpactContext.current();
        if (source == null && event.getProjectile() instanceof SlowProjectile projectile) {
            source = projectile.hurtingItem;
        }
        if (!(source instanceof GunItem gun)) {
            return;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(gun);
        if (itemId == null) {
            return;
        }

        BulletHoleConfig config = WeaponExtraDataManager.getData(itemId).getBulletHoles();

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!isEligibleSupport(state)) {
            return;
        }

        Identifier soundId = resolveSoundId(config.getSounds(), state);
        float impactVolume = config.getSounds().getImpactVolume();
        if (!config.isEnabled() && soundId == null) {
            return;
        }

        Vec3 location = sanitizeLocation(hitResult.getLocation(), pos, hitResult.getDirection());
        PacketDistributor.sendToPlayersNear(level, null,
                location.x, location.y, location.z, BROADCAST_RADIUS,
                new BulletHoleImpactPacket(itemId, pos, hitResult.getDirection(),
                        location, soundId, impactVolume, config.isEnabled()));
    }

    /**
     * Selects and validates the server-visible sound reference. An explicitly
     * disabled exact/tag entry stops resolution and suppresses the sound.
     * Registered events and physical .ogg locations are accepted; a
     * syntactically valid but unavailable registered event falls back to the
     * configured default. The client validates the physical resource itself
     * because client resource packs are not available to a dedicated server.
     */
    private static Identifier resolveSoundId(BulletHoleSoundConfig sounds,
                                             BlockState state) {
        if (sounds == null || !sounds.isEnabled()) {
            return null;
        }
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        BulletHoleSoundConfig.SoundRef selected = sounds.selectSound(blockId,
                tag -> state.is(tagKey(tag)));
        if (selected == null || !selected.isEnabled()) {
            return null;
        }

        BulletHoleSoundConfig.SoundRef available =
                BulletHoleSoundConfig.resolveAvailable(selected,
                        sounds.getDefaultSound(),
                        id -> isRegisteredSound(id)
                                || BulletHoleSoundConfig.isPhysicalOgg(id));
        if (available != null && available.isEnabled()) {
            if (!available.sound().equals(selected.sound())) {
                warnSoundOnce(selected.sound(),
                        "is not registered; using configured default "
                                + available.sound());
            }
            return available.sound();
        }

        warnSoundOnce(selected.sound(), "is not registered; impact sound skipped");
        return null;
    }

    private static boolean isRegisteredSound(Identifier soundId) {
        return soundId != null
                && BuiltInRegistries.SOUND_EVENT.get(soundId).isPresent();
    }

    private static TagKey<Block> tagKey(Identifier tag) {
        return TAG_CACHE.computeIfAbsent(tag,
                id -> TagKey.create(Registries.BLOCK, id));
    }

    private static void warnSoundOnce(Identifier soundId, String message) {
        if (WARNED_SOUNDS.add(soundId)) {
            PointBlankExtraFeatures.LOGGER.warn(
                    "Bullet-hole sound event '{}' {}", soundId, message);
        }
    }

    /**
     * Only air and fluids are rejected. Opaque-ness is intentionally irrelevant:
     * glass and stained glass are valid supports. Their texture is resolved by
     * {@link BulletHoleConfig#selectTexture}, including the built-in glass tags
     * mapped to glass.png.
     */
    private static boolean isEligibleSupport(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    private static Vec3 sanitizeLocation(Vec3 location, BlockPos pos,
                                         net.minecraft.core.Direction face) {
        if (location != null && Double.isFinite(location.x)
                && Double.isFinite(location.y) && Double.isFinite(location.z)) {
            return location;
        }
        return Vec3.atCenterOf(pos).relative(face, 0.5D);
    }
}
