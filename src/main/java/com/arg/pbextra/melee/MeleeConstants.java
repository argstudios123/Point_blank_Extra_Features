package com.arg.pbextra.melee;


public final class MeleeConstants {

    public static final String MELEE_CONTROLLER = "extra_melee";
    public static final String MELEE_TRIGGER = "melee";

    /**
     * GeckoLib 5 exposes a controller's animation time in seconds, so the millisecond
     * conversion used by the melee pre-hit timing is 1000 ms per second.
     */
    public static final double ANIMATION_MILLIS_PER_SECOND = 1000.0;

    private MeleeConstants() {
    }
}
