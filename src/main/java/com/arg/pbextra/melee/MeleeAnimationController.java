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

    /**
     * Releases this controller's pose completely.
     * <p>
     * GeckoLib's {@code stopTriggeredAnimation()} is <em>not</em> a release: it
     * keeps the controller's {@code animationPoint} and {@code timeline} alive and
     * pins them to the last frame. Because this controller is registered after
     * Point Blank's own controllers, such a pinned pose keeps overriding
     * {@code main}, {@code leftarm}, {@code rightarm} and {@code _camera_} forever
     * (no walking bob, no recoil/view shake, wrong dual-wield pose).
     * {@link #reset()} clears the animation point, timeline and raw animation, so
     * the controller stops contributing bone snapshots entirely.
     */
    public void release() {
        this.reset();
    }
}
