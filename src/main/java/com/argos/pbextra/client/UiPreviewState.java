package com.argos.pbextra.client;

import net.minecraft.util.Mth;

// This is respossible for preview state
public final class UiPreviewState {

    public static final String DEFAULT_MESSAGE = "NO AMMO";
    public static final String DEFAULT_FIRE_MODE = "AUTO";

    private int currentAmmo = 24;
    private int maxAmmo = 30;
    private boolean infiniteAmmo;
    private boolean reloading;
    private boolean showMessage;
    private String messageText = DEFAULT_MESSAGE;
    private String fireModeName = DEFAULT_FIRE_MODE;
    private float heatRatio = 0.62f;
    private boolean mirrorHorizontally;

    public int currentAmmo() {
        return currentAmmo;
    }

    public void setCurrentAmmo(int currentAmmo) {
        this.currentAmmo = Math.max(0, currentAmmo);
    }

    public int maxAmmo() {
        return maxAmmo;
    }

    public void setMaxAmmo(int maxAmmo) {
        this.maxAmmo = Math.max(0, maxAmmo);
    }


    public int effectiveMaxAmmo() {
        return infiniteAmmo ? Integer.MAX_VALUE : maxAmmo;
    }

    public boolean infiniteAmmo() {
        return infiniteAmmo;
    }

    public void setInfiniteAmmo(boolean infiniteAmmo) {
        this.infiniteAmmo = infiniteAmmo;
    }

    public boolean reloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    public boolean showMessage() {
        return showMessage;
    }

    public void setShowMessage(boolean showMessage) {
        this.showMessage = showMessage;
    }

    public String messageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText == null ? "" : messageText;
    }

    public String fireModeName() {
        return fireModeName;
    }

    public void setFireModeName(String fireModeName) {
        this.fireModeName = fireModeName == null ? "" : fireModeName;
    }

    public float heatRatio() {
        return heatRatio;
    }

    public void setHeatRatio(float heatRatio) {
        this.heatRatio = Mth.clamp(heatRatio, 0.0f, 1.0f);
    }

    public boolean mirrorHorizontally() {
        return mirrorHorizontally;
    }

    public void setMirrorHorizontally(boolean mirrorHorizontally) {
        this.mirrorHorizontally = mirrorHorizontally;
    }


    public boolean ammoEmpty() {
        return currentAmmo <= 0 && !reloading && !showMessage;
    }

    public void reset() {
        this.currentAmmo = 24;
        this.maxAmmo = 30;
        this.infiniteAmmo = false;
        this.reloading = false;
        this.showMessage = false;
        this.messageText = DEFAULT_MESSAGE;
        this.fireModeName = DEFAULT_FIRE_MODE;
        this.heatRatio = 0.62f;
        this.mirrorHorizontally = false;
    }
}
