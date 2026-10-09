package com.argos.pbextra.client;

import com.argos.pbextra.data.AdsReloadZoomEasing;


public final class AdsReloadZoomTransition {

    private static final long NANOS_PER_MILLI = 1_000_000L;

  // This is responsible for ADS Reload Zoom transition
    private static final long TRANSITION_MILLIS = 200L;

    private static long reloadStartNanos;
    private static boolean wasReloading;

    private AdsReloadZoomTransition() {
    }

    public static float progress(boolean reloading, String zoomType) {
        if (!reloading) {
            // Reload over (or never started): restore, and re-arm for the next reload.
            wasReloading = false;
            reloadStartNanos = 0L;
            return 0.0F;
        }

        long now = System.nanoTime();
        if (!wasReloading) {
            // A new reload just began: the transition is relative to this very moment.
            reloadStartNanos = now;
            wasReloading = true;
        }

        long elapsedMillis = (now - reloadStartNanos) / NANOS_PER_MILLI;
        return AdsReloadZoomEasing.apply(zoomType, transitionProgress(elapsedMillis));
    }

    static float transitionProgress(long elapsedMillis) {
        if (elapsedMillis <= 0L) {
            return 0.0F;
        }
        if (elapsedMillis >= TRANSITION_MILLIS) {
            return 1.0F;
        }
        return (float) elapsedMillis / (float) TRANSITION_MILLIS;
    }
}
