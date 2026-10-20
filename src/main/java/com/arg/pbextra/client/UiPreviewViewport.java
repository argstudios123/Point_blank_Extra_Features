package com.arg.pbextra.client;

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

    /**
     * Zooms while keeping the preview point under the mouse cursor stationary.
     * The existing transform (origin + zoom) stays the only one used by the
     * editor; only the origin is adjusted around the cursor.
     */
    public void zoomAt(double amount, double mouseX, double mouseY,
                       float canvasX, float canvasY, float canvasWidth, float canvasHeight) {
        if (amount == 0.0D || canvasWidth <= 0.0f || canvasHeight <= 0.0f) {
            return;
        }
        float previewX = toPreviewX(mouseX);
        float previewY = toPreviewY(mouseY);
        float fitZoom = Math.min(canvasWidth / screenWidth, canvasHeight / screenHeight);
        zoomBy(amount);

        float applied = zoomFactor <= MIN_ZOOM_FACTOR
                ? Math.max(MIN_ZOOM, fitZoom)
                : Math.max(MIN_ZOOM, fitZoom * zoomFactor);

        float nextOriginX = (float) mouseX - previewX * applied;
        float nextOriginY = (float) mouseY - previewY * applied;
        this.zoom = applied;
        this.originX = clampOrigin(nextOriginX, canvasX, canvasWidth, screenWidth * applied);
        this.originY = clampOrigin(nextOriginY, canvasY, canvasHeight, screenHeight * applied);
    }

    private static float clampOrigin(float origin, float canvasStart, float canvasSize, float contentSize) {
        if (contentSize <= canvasSize) {
            // Keep the smaller axis inside the canvas without forcing a recenter;
            // this preserves as much of the cursor anchor as possible.
            return Mth.clamp(origin, canvasStart, canvasStart + canvasSize - contentSize);
        }
        return Mth.clamp(origin, canvasStart + canvasSize - contentSize, canvasStart);
    }

    /** Moves the preview without changing its zoom, clamped to the viewport. */
    public void panBy(double deltaX, double deltaY,
                      float canvasX, float canvasY, float canvasWidth, float canvasHeight) {
        if ((deltaX == 0.0D && deltaY == 0.0D) || canvasWidth <= 0.0f || canvasHeight <= 0.0f) {
            return;
        }
        this.originX = clampOrigin(originX + (float) deltaX,
                canvasX, canvasWidth, screenWidth * zoom);
        this.originY = clampOrigin(originY + (float) deltaY,
                canvasY, canvasHeight, screenHeight * zoom);
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
