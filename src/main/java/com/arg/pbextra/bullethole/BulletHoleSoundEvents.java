package com.arg.pbextra.bullethole;

import com.arg.pbextra.PointBlankExtraFeatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


/**
 * Registers the mod's existing bullet-hole impact sounds as real sound events.
 *
 * <p>The ids are exactly the keys already used by
 * {@code assets/pointblankextra/sounds.json}; only the missing Java
 * registration is added, so no new ids or duplicate registrations are
 * introduced. Registering them lets the ordinary
 * {@link net.minecraft.core.registries.BuiltInRegistries#SOUND_EVENT} lookup
 * used by the impact playback code resolve {@link BulletHoleDefaults#DEFAULT_SOUND}
 * (and the other built-in material sounds) instead of treating them as missing.</p>
 */
public final class BulletHoleSoundEvents {

    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, PointBlankExtraFeatures.MOD_ID);


    // Also register the defaults here too
    public static final DeferredHolder<SoundEvent, SoundEvent> DEFAULT =
            register(BulletHoleDefaults.DEFAULT_SOUND_PATH);
    public static final DeferredHolder<SoundEvent, SoundEvent> WOOD =
            register(BulletHoleDefaults.WOOD_SOUND_PATH);
    public static final DeferredHolder<SoundEvent, SoundEvent> GLASS =
            register(BulletHoleDefaults.GLASS_SOUND_PATH);
    public static final DeferredHolder<SoundEvent, SoundEvent> DIRT =
            register(BulletHoleDefaults.DIRT_SOUND_PATH);

    public static final DeferredHolder<SoundEvent, SoundEvent> METAL =
            register(BulletHoleDefaults.METAL_SOUND_PATH);

    private BulletHoleSoundEvents() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        return SOUND_EVENTS.register(path,
                () -> SoundEvent.createVariableRangeEvent(
                        Identifier.fromNamespaceAndPath(
                                PointBlankExtraFeatures.MOD_ID, path)));
    }

    /** Called from the mod constructor with the NeoForge mod event bus. */
    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
