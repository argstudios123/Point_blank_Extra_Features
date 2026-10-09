package com.argos.pbextra.melee;

import com.vicmatskiv.pointblank.item.GunItem;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;

import java.util.function.ObjDoubleConsumer;


public class MeleeAnimationController extends AnimationController<GunItem> {

    public static ObjDoubleConsumer<MeleeAnimationController> positionListener;
    public static AnimationController.SoundKeyframeHandler<GunItem> soundKeyframeListener;

    public MeleeAnimationController(GunItem gun, String name) {
        // The state handler always returns STOP: the controller is idle unless a
        // melee animation is triggered, and GeckoLib bypasses the state handler
        // entirely while a triggered animation is playing.
        super(gun, name, 0, state -> PlayState.STOP);

        this.setSoundKeyframeHandler(event -> {
            AnimationController.SoundKeyframeHandler<GunItem> listener = soundKeyframeListener;
            if (listener != null) {
                listener.handle(event);
            }
        });
    }

    public void restartAnimationTime() {
        this.shouldResetTick = true;
    }

    @Override
    protected double adjustTick(double tick) {
        double position = super.adjustTick(tick);
        ObjDoubleConsumer<MeleeAnimationController> listener = positionListener;
        if (listener != null) {
            listener.accept(this, position);
        }
        return position;
    }
}
