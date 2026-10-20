package com.arg.pbextra.client.bulletholes;

import com.arg.pbextra.PointBlankExtraFeatures;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;


/**
 * Render pipeline shared by every bullet-hole texture.
 *
 * <p>It is a copy of {@link RenderPipelines#ENTITY_TRANSLUCENT} except that
 * depth writes are disabled. Bullet holes are translucent and deliberately
 * coplanar with their supporting face. If the translucent entity pipeline
 * writes depth, two overlapping decals have exactly the same depth: the later
 * upload can pass {@code LEQUAL} and overwrite the first, and the upload sort
 * order is unstable when distances are equal. That is the observed flicker.
 * Testing depth against the world while not writing decal depth removes the
 * decal-vs-decal depth conflict without moving the decal farther off the wall.</p>
 */
public final class BulletHolePipelines {

    public static final RenderPipeline BULLET_HOLE = RenderPipeline
            .builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(
                    PointBlankExtraFeatures.MOD_ID, "pipeline/bullet_hole"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withSampler("Sampler1")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(
                    CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

    private BulletHolePipelines() {
    }
}