package com.arg.pbextra.client.bulletholes;

import com.mojang.blaze3d.platform.CompareOp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * Guards the render-state part of the Z-fighting fix. The translucent entity
 * pipeline writes depth; coplanar overlapping decals then fight for the same
 * depth value. The decal pipeline must keep the depth test but not write depth.
 */
class BulletHolePipelinesTest {

    @Test
    void decalPipelineTestsDepthButDoesNotWriteIt() {
        var depth = BulletHolePipelines.BULLET_HOLE.getDepthStencilState();
        assertEquals(CompareOp.LESS_THAN_OR_EQUAL, depth.depthTest());
        assertFalse(depth.writeDepth(),
                "coplanar bullet-hole decals must not write depth");
    }
}