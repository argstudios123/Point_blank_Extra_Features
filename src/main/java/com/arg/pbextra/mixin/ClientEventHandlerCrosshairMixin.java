package com.arg.pbextra.mixin;

import com.arg.pbextra.client.crosshair.CustomCrosshairRenderer;
import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.render.CrosshairRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


/**
 * Redirects Vic's Point Blank's native crosshair draw call.
 *
 * <p>The redirect runs inside the native crosshair branch, after VPB has
 * already checked first-person state, aiming, firing/idle state, the global
 * crosshair configuration and computed the smoothed expansion value. Returning
 * custom artwork here therefore preserves native visibility and spread; when
 * no custom crosshair is active the original native draw is called unchanged.</p>
 *
 * <p>The target method name and descriptor are verified against Point Blank
 * 2.2.0 for NeoForge 26.1.2. The jar is not obfuscated, so {@code remap=false}
 * is used. Any Point Blank update must re-check that the call still exists.</p>
 */
@Mixin(value = ClientEventHandler.class, remap = false)
public abstract class ClientEventHandlerCrosshairMixin {

    @Redirect(
            method = "onRenderOverlay",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/vicmatskiv/pointblank/client/render/CrosshairRenderer;"
                            + "renderCrosshairOverlay3(Lnet/minecraft/client/gui/"
                            + "GuiGraphicsExtractor;FLnet/minecraft/resources/Identifier;FFFII)V"
            ),
            remap = false
    )
    private static void pointblankextra$renderCustomCrosshair(
            GuiGraphicsExtractor graphics,
            float partialTick,
            Identifier nativeTexture,
            float expansion,
            float posX,
            float posY,
            int renderWidth,
            int renderHeight) {
        if (!CustomCrosshairRenderer.renderNativeBranch(graphics, partialTick,
                expansion, posX, posY, renderWidth, renderHeight)) {
            CrosshairRenderer.renderCrosshairOverlay3(graphics, partialTick,
                    nativeTexture, expansion, posX, posY, renderWidth, renderHeight);
        }
    }
}
