package com.arg.pbextra.crosshair;


/**
 * Geometry for the one segment missing from Vic's Point Blank's native
 * crosshair renderer.
 *
 * <p>Point Blank 2.2.0's {@code CrosshairRenderer.renderCrosshairOverlay3}
 * draws the center patch, left, right and bottom segments. It never draws the
 * top atlas region. These constants and the mirrored top quad are copied from
 * the decompiled native method so the missing segment uses the same UV region,
 * arm span, hair width and native expansion offset as the bottom segment.</p>
 *
 * <p>Expansion is the value already computed and smoothed by VPB. This class
 * does not calculate movement, firing, sprinting, jumping, recovery or any
 * other crosshair state.</p>
 */
public final class NativeCrosshairTopSegment {

    /** Native central-patch UV ratio from CrosshairRenderer. */
    public static final float NATIVE_RATIO = 0.17948718F;
    /** Native top-region V coordinate; identical to the native uvOffset. */
    public static final float NATIVE_UV_OFFSET = (1.0F - NATIVE_RATIO) * 0.5F;
    /** Native arm span. */
    public static final float NATIVE_ARM_SPAN = 30.0F;
    /** Native hair width. */
    public static final float NATIVE_HAIR_WIDTH = 0.5F;
    /** Native vertical offset per unit of expansion: 8.0 * 0.4. */
    public static final float NATIVE_OFFSET_PER_EXPANSION = 3.2F;

    /** Top atlas region: full width, top 41% (the region VPB never draws). */
    public static final float TOP_U0 = 0.0F;
    public static final float TOP_U1 = 1.0F;
    public static final float TOP_V0 = 0.0F;
    public static final float TOP_V1 = NATIVE_UV_OFFSET;

    private NativeCrosshairTopSegment() {
    }

    /**
     * Mirrors the native bottom quad around the crosshair center.
     *
     * <p>Native bottom: {@code y0 = centerY + halfInnerSide - hairWidth + offset},
     * {@code y1 = y0 + hairLength - hairWidth}. The top is the mirror image:
     * {@code y1 = centerY - halfInnerSide + hairWidth - offset},
     * {@code y0 = y1 - (hairLength - hairWidth)}. Horizontal bounds are the
     * native {@code centerX +/- halfSq} used by the bottom segment.</p>
     */
    public static Segment topSegment(float centerX, float centerY, float expansion) {
        float innerSide = NATIVE_ARM_SPAN * NATIVE_RATIO;
        float halfInnerSide = innerSide * 0.5F;
        float halfSq = NATIVE_ARM_SPAN * 0.5F;
        float hairLength = (NATIVE_ARM_SPAN - innerSide) * 0.5F;
        float offset = NATIVE_OFFSET_PER_EXPANSION * Math.max(0.0F, expansion);

        float y1 = centerY - halfInnerSide + NATIVE_HAIR_WIDTH - offset;
        float y0 = y1 - (hairLength - NATIVE_HAIR_WIDTH);
        return new Segment(centerX - halfSq, y0, centerX + halfSq, y1);
    }

    public record Segment(float x0, float y0, float x1, float y1) {
    }
}
