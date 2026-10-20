package com.arg.pbextra.client.bulletholes;

import com.arg.pbextra.bullethole.BulletHoleConfig;
import com.arg.pbextra.bullethole.BulletHoleSoundConfig;
import com.arg.pbextra.config.PointBlankExtraConfig;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Client-side playback for material impacts.
 *
 * <p>The server selects the sound-event id and impact volume from the effective
 * per-gun/pack configuration and sends them in the existing bullet-hole impact
 * payload. The client resolves {@code max_distance} from the same configuration.
 * Each nearby client then plays it through Minecraft's normal sound engine.
 * This keeps dedicated servers free of client-audio code and avoids an
 * additional per-impact sound packet.</p>
 *
 * <p>Every sound is played with a {@link ImpactSoundInstance} positioned at the
 * exact world-space impact point with {@code relative = false}. The instance
 * itself applies a linear distance fade and stops once the configured
 * {@code max_distance} is reached, so the limit is independent of
 * {@code impact_volume}. The bundled impact OGGs are mono, which lets OpenAL
 * spatialize (pan) the source; stereo sources would otherwise be treated as
 * non-positional by OpenAL Soft.</p>
 */
public final class BulletHoleSounds {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Identifier> WARNED = ConcurrentHashMap.newKeySet();
    private static final Map<Identifier, Sound> PHYSICAL_SOUNDS = new ConcurrentHashMap<>();

    private static final float PITCH = 1.0F;
    private static final int PHYSICAL_ATTENUATION_DISTANCE = 16;

    private BulletHoleSounds() {
    }

    public static void play(Identifier weaponId, Identifier soundId, Vec3 location,
                            float impactVolume) {
        if (soundId == null || location == null
                || !PointBlankExtraConfig.isModEnabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        // SoundEngine clamps the channel gain to 1. The instance volume is
        // capped so impact_volume controls loudness only; audible distance is
        // enforced separately by ImpactSoundInstance.
        float volume = BulletHoleSoundConfig.engineVolume(impactVolume);

        BulletHoleConfig config = weaponId == null
                ? BulletHoleConfig.BUILT_IN_DEFAULTS
                : WeaponExtraDataManager.getData(weaponId).getBulletHoles();
        float maxDistance = config.getSounds().getMaxDistance();

        // Registered sound events resolve their sounds.json entry through the
        // normal SoundManager path; the instance stays positioned in the world.
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId)
                .map(reference -> reference.value()).orElse(null);
        if (sound != null) {
            minecraft.getSoundManager().play(new ImpactSoundInstance(
                    sound.location(), location, volume, maxDistance, null));
            return;
        }

        // A physical .ogg location is resolved through the client resource
        // manager and played with the same positional instance.
        if (!BulletHoleSoundConfig.isPhysicalOgg(soundId)) {
            warnMissing(soundId);
            return;
        }

        Identifier resource = BulletHoleSoundConfig.physicalResourceId(soundId);
        Identifier soundLocation = BulletHoleSoundConfig.physicalSoundLocation(soundId);
        if (resource == null || soundLocation == null
                || minecraft.getResourceManager() == null
                || minecraft.getResourceManager().getResource(resource).isEmpty()) {
            warnMissing(soundId);
            return;
        }

        Sound physicalSound = PHYSICAL_SOUNDS.computeIfAbsent(soundLocation,
                key -> new Sound(key, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F),
                        1, Sound.Type.FILE, false, false,
                        PHYSICAL_ATTENUATION_DISTANCE));
        minecraft.getSoundManager().play(new ImpactSoundInstance(
                soundLocation, location, volume, maxDistance, physicalSound));
    }

    private static void warnMissing(Identifier soundId) {
        if (WARNED.add(soundId)) {
            LOGGER.warn("Bullet-hole sound '{}' is neither a registered sound event "
                    + "nor an existing .ogg resource; impact sound skipped", soundId);
        }
    }

    /**
     * Positional impact sound shared by registered sound events and physical
     * OGGs.
     *
     * <p>The source is placed at the exact impact coordinates with
     * {@code relative = false}. Minecraft/OpenAL handles directional panning;
     * this instance applies its own linear distance fade and stops once the
     * configured maximum distance is reached. That makes the audible radius
     * independent of {@code impact_volume}, which only scales the base gain.
     * Ticking is O(1) per active impact and lasts only while the short sound
     * plays; nothing is polled globally.</p>
     */
    private static final class ImpactSoundInstance extends AbstractSoundInstance
            implements TickableSoundInstance {

        private final Sound physicalSound;
        private final WeighedSoundEvents physicalEvent;
        private final float baseVolume;
        private final double maxDistance;
        private final double maxDistanceSqr;
        private float fade = 1.0F;
        private boolean stopped;

        private ImpactSoundInstance(Identifier identifier, Vec3 location, float volume,
                                    float maxDistance, Sound physicalSound) {
            super(identifier, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.physicalSound = physicalSound;
            this.physicalEvent = physicalSound == null ? null
                    : new WeighedSoundEvents(identifier, null);
            this.baseVolume = volume;
            this.maxDistance = maxDistance;
            this.maxDistanceSqr = maxDistance * maxDistance;
            this.pitch = PITCH;
            this.x = location.x;
            this.y = location.y;
            this.z = location.z;
            // Distance gain is applied by tick(); OpenAL still pans the source.
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = false;
            updateFade();
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager soundManager) {
            if (physicalSound != null) {
                this.sound = physicalSound;
                return physicalEvent;
            }
            return super.resolve(soundManager);
        }

        @Override
        public Sound getSound() {
            return physicalSound != null ? physicalSound : super.getSound();
        }

        @Override
        public float getVolume() {
            float soundVolume = this.sound == null ? 1.0F
                    : this.sound.getVolume().sample(this.random);
            return baseVolume * fade * soundVolume;
        }

        @Override
        public boolean isLooping() {
            return false;
        }

        @Override
        public boolean isStopped() {
            return stopped;
        }

        @Override
        public void tick() {
            updateFade();
        }

        private void updateFade() {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level == null || minecraft.player == null) {
                fade = 0.0F;
                stopped = true;
                return;
            }

            Camera camera = minecraft.gameRenderer.getMainCamera();
            Vec3 listener = camera.isInitialized()
                    ? camera.position() : minecraft.player.getEyePosition();
            double distanceSqr = listener.distanceToSqr(this.x, this.y, this.z);
            if (distanceSqr >= maxDistanceSqr) {
                fade = 0.0F;
                stopped = true;
                return;
            }
            this.fade = BulletHoleSoundConfig.distanceGain(Math.sqrt(distanceSqr),
                    (float) maxDistance);
        }
    }
}