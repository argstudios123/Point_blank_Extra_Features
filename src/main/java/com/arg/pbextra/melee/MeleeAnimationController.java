package com.arg.pbextra.melee;

import com.vicmatskiv.pointblank.item.GunItem;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.cache.animation.keyframeevent.SoundKeyframeData;


public class MeleeAnimationController extends AnimationController<GunItem> {

    public static AnimationController.KeyframeEventHandler<GunItem, SoundKeyframeData> soundKeyframeListener;

    public MeleeAnimationController(GunItem gun, String name) {
        // The state handler always returns STOP: the controller is idle unless a
        // melee animation is triggered, and GeckoLib takes the triggered-animation
        // path while a triggered animation is playing.
        super(name, 0, state -> PlayState.STOP);

        this.setSoundKeyframeHandler(event -> {
            AnimationController.KeyframeEventHandler<GunItem, SoundKeyframeData> listener = soundKeyframeListener;
            if (listener != null) {
                listener.handle(event);
            }
        });
    }

    public void release() {
        this.reset();
    }
}
