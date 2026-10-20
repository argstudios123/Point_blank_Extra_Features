package com.arg.pbextra.crosshair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class NativeCrosshairTopSegmentTest {

    @Test
    void usesTheTopAtlasRegion() {
        assertEquals(0.0F, NativeCrosshairTopSegment.TOP_U0, 0.0001F);
        assertEquals(1.0F, NativeCrosshairTopSegment.TOP_U1, 0.0001F);
        assertEquals(0.0F, NativeCrosshairTopSegment.TOP_V0, 0.0001F);
        assertEquals(0.41025642F, NativeCrosshairTopSegment.TOP_V1, 0.0001F);
        // The top region ends exactly where the native center patch begins.
        assertEquals(NativeCrosshairTopSegment.NATIVE_UV_OFFSET,
                NativeCrosshairTopSegment.TOP_V1, 0.0001F);
    }

    @Test
    void mirrorsTheNativeBottomSegmentAtRest() {
        float centerX = 100.0F;
        float centerY = 100.0F;
        NativeCrosshairTopSegment.Segment top =
                NativeCrosshairTopSegment.topSegment(centerX, centerY, 0.0F);
        float[] bottom = nativeBottom(centerX, centerY, 0.0F);

        assertEquals(85.0F, top.x0(), 0.0001F);
        assertEquals(115.0F, top.x1(), 0.0001F);
        assertEquals(86.0F, top.y0(), 0.0001F);
        assertEquals(97.80769F, top.y1(), 0.0001F);

        // Mirror around the crosshair center.
        assertEquals(centerY * 2.0F, top.y0() + bottom[1], 0.0001F);
        assertEquals(centerY * 2.0F, top.y1() + bottom[0], 0.0001F);
        assertEquals(centerX * 2.0F, top.x0() + top.x1(), 0.0001F);
    }

    @Test
    void movesOutwardWithTheNativeExpansionValue() {
        float centerX = 100.0F;
        float centerY = 100.0F;
        NativeCrosshairTopSegment.Segment atRest =
                NativeCrosshairTopSegment.topSegment(centerX, centerY, 0.0F);
        NativeCrosshairTopSegment.Segment expanded =
                NativeCrosshairTopSegment.topSegment(centerX, centerY, 2.0F);

        float expectedOffset = NativeCrosshairTopSegment.NATIVE_OFFSET_PER_EXPANSION * 2.0F;
        assertEquals(atRest.y0() - expectedOffset, expanded.y0(), 0.0001F);
        assertEquals(atRest.y1() - expectedOffset, expanded.y1(), 0.0001F);
        assertEquals(atRest.x0(), expanded.x0(), 0.0001F);
        assertEquals(atRest.x1(), expanded.x1(), 0.0001F);

        float[] bottom = nativeBottom(centerX, centerY, 2.0F);
        assertEquals(centerY * 2.0F, expanded.y0() + bottom[1], 0.0001F);
        assertEquals(centerY * 2.0F, expanded.y1() + bottom[0], 0.0001F);
    }

    @Test
    void negativeExpansionDoesNotPushTheSegmentInward() {
        NativeCrosshairTopSegment.Segment rest =
                NativeCrosshairTopSegment.topSegment(100.0F, 100.0F, 0.0F);
        NativeCrosshairTopSegment.Segment negative =
                NativeCrosshairTopSegment.topSegment(100.0F, 100.0F, -3.0F);

        assertEquals(rest.y0(), negative.y0(), 0.0001F);
        assertEquals(rest.y1(), negative.y1(), 0.0001F);
        assertTrue(negative.y1() < negative.y0() + 12.0F);
    }

    /** Exact native bottom geometry from CrosshairRenderer.renderCrosshairOverlay3. */
    private static float[] nativeBottom(float centerX, float centerY, float expansion) {
        float ratio = 0.17948718F;
        float innerSide = 30.0F * ratio;
        float halfInnerSide = innerSide * 0.5F;
        float hairLength = (30.0F - innerSide) * 0.5F;
        float hairWidth = 0.5F;
        float offset = 8.0F * 0.4F * Math.max(0.0F, expansion);
        float y0 = centerY + halfInnerSide - hairWidth + offset;
        float y1 = y0 + hairLength - hairWidth;
        return new float[]{y0, y1};
    }
}
