package com.arg.pbextra.bullethole;

import com.arg.pbextra.PointBlankExtraFeatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


// here this class will handle the  Sound events of bullet impacts on block surfaces
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
