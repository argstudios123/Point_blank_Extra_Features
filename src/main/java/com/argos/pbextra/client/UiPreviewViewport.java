package com.argos.pbextra.client;

import net.minecraft.util.Mth;


public final class UiPreviewViewport {

    private static final float MIN_ZOOM_FACTOR = 1.0f;
    private static final float MAX_ZOOM_FACTOR = 12.0f;
    private static final float MIN_ZOOM = 0.05f;

    private final int screenWidth;
    private final int screenHeight;
    private float originX;
    private float originY;
    private float zoom = 1.0f;
    private float zoomFactor = 1.0f;

    public UiPreviewViewport(int screenWidth, int screenHeight) {
        this.screenWidth = Math.max(1, screenWidth);
        this.screenHeight = Math.max(1, screenHeight);
    }

    public int screenWidth() {
        return screenWidth;
    }

    public int screenHeight() {
        return screenHeight;
    }

    public float originX() {
        return originX;
    }

    public float originY() {
        return originY;
    }

    public float zoom() {
        return zoom;
    }

    public void fit(float canvasX, float canvasY, float canvasWidth, float canvasHeight) {
        float fitZoom = Math.min(canvasWidth / screenWidth, canvasHeight / screenHeight);
        float applied = Math.max(MIN_ZOOM, fitZoom * zoomFactor);
        // Never zoom out further than the plain fit, only in.
        if (zoomFactor <= MIN_ZOOM_FACTOR) {
            applied = Math.max(MIN_ZOOM, fitZoom);
        }
        this.zoom = applied;
        this.originX = canvasX + (canvasWidth - screenWidth * zoom) / 2.0f;
        this.originY = canvasY + (canvasHeight - screenHeight * zoom) / 2.0f;
    }

    public void zoomBy(double amount) {
        if (amount == 0.0D) {
            return;
        }
        zoomFactor = Mth.clamp(zoomFactor * (float) Math.pow(1.2D, amount),
                MIN_ZOOM_FACTOR, MAX_ZOOM_FACTOR);
    }

    public boolean isFitToArea() {
        return zoomFactor <= MIN_ZOOM_FACTOR;
    }

    public float toScreenX(double screenSpaceX) {
        return originX + (float) screenSpaceX * zoom;
    }

    public float toScreenY(double screenSpaceY) {
        return originY + (float) screenSpaceY * zoom;
    }

    public float toPreviewX(double guiX) {
        return (float) ((guiX - originX) / zoom);
    }

    public float toPreviewY(double guiY) {
        return (float) ((guiY - originY) / zoom);
    }

    public float scale(double length) {
        return (float) length * zoom;
    }
}
