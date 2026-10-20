package com.arg.pbextra.client.crosshair;

import com.arg.pbextra.crosshair.CustomCrosshairConfig;
import com.arg.pbextra.crosshair.CustomCrosshairLayout;
import com.arg.pbextra.crosshair.CustomCrosshairSizing;
import com.arg.pbextra.crosshair.NativeCrosshairTopSegment;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.mojang.logging.LogUtils;
import com.vicmatskiv.pointblank.client.render.CrosshairRenderer;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;



public final class CustomCrosshairRenderer {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile boolean warnedAboutFailure;

    private CustomCrosshairRenderer() {
    }


    public static boolean renderNativeBranch(GuiGraphicsExtractor graphics,
                                             float partialTick,
                                             float expansion,
                                             float posX,
                                             float posY,
                                             int renderWidth,
                                             int renderHeight) {
        try {
            return renderUnchecked(graphics, partialTick, expansion,
                    posX, posY, renderWidth, renderHeight);
        } catch (RuntimeException exception) {
            if (!warnedAboutFailure) {
                warnedAboutFailure = true;
                LOGGER.warn("Custom crosshair rendering failed; using the native "
                        + "Point Blank crosshair", exception);
            }
            return false;
        }
    }

    private static boolean renderUnchecked(GuiGraphicsExtractor graphics,
                                           float partialTick,
                                           float expansion,
                                           float posX,
                                           float posY,
                                           int renderWidth,
                                           int renderHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        ItemStack stack = GunItem.getMainHeldGunItemStack(minecraft.player);
        if (stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof GunItem)) {
            return false;
        }

        CustomCrosshairConfig config = WeaponExtraDataManager.getCustomCrosshair(stack);
        if (!config.isEnabled() || !config.hasTexture()) {
            return false;
        }

        Identifier texture = CustomCrosshairTextures.resolve(config.getTexture());
        if (texture == null) {
            return false;
        }
        CustomCrosshairTextures.TextureInfo info = CustomCrosshairTextures.info(texture);
        if (info == null) {
            return false;
        }

        if (config.getLayout() == CustomCrosshairLayout.POINTBLANK) {
            if (!CustomCrosshairTextures.validateNativeLayout(texture, info)) {
                return false;
            }
            // Exact native path: native texture, native expansion, native
            // positioning, native smoothing and native segment geometry.
            CrosshairRenderer.renderCrosshairOverlay3(graphics, partialTick, texture,
                    expansion, posX, posY, renderWidth, renderHeight);
            // Point Blank 2.2.0 never draws the top atlas region. Add the
            // mirrored top quad with the same native expansion value.
            renderTopSegment(graphics, texture, expansion, posX, posY,
                    renderWidth, renderHeight);
            return true;
        }

        renderStaticComplete(graphics, texture, info, config.getSize());
        return true;
    }


    private static void renderTopSegment(GuiGraphicsExtractor graphics,
                                         Identifier texture,
                                         float expansion,
                                         float posX,
                                         float posY,
                                         int renderWidth,
                                         int renderHeight) {
        float centerX = posX + renderWidth * 0.5F;
        float centerY = posY + renderHeight * 0.5F;
        NativeCrosshairTopSegment.Segment segment =
                NativeCrosshairTopSegment.topSegment(centerX, centerY, expansion);
        float width = segment.x1() - segment.x0();
        float height = segment.y1() - segment.y0();
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        int textureSize = CustomCrosshairLayout.POINTBLANK_TEXTURE_SIZE;
        int uPixels = Math.round(NativeCrosshairTopSegment.TOP_U0 * textureSize);
        int vPixels = Math.round(NativeCrosshairTopSegment.TOP_V0 * textureSize);
        int uWidthPixels = Math.max(1, Math.round(
                (NativeCrosshairTopSegment.TOP_U1 - NativeCrosshairTopSegment.TOP_U0)
                        * textureSize));
        int vHeightPixels = Math.max(1, Math.round(
                (NativeCrosshairTopSegment.TOP_V1 - NativeCrosshairTopSegment.TOP_V0)
                        * textureSize));

        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(segment.x0(), segment.y0());
            graphics.pose().scale(width, height);
            graphics.blit(RenderPipelines.CROSSHAIR, texture, 0, 0,
                    uPixels, vPixels, 1, 1,
                    uWidthPixels, vHeightPixels, textureSize, textureSize, -1);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    /**
     * Static fallback for arbitrary complete PNGs. Visibility is still decided
     * by VPB's native branch; this method only draws the image and does not
     * apply any spread.
     */
    private static void renderStaticComplete(GuiGraphicsExtractor graphics,
                                             Identifier texture,
                                             CustomCrosshairTextures.TextureInfo info,
                                             int size) {
        int drawWidth = CustomCrosshairSizing.fitWidth(info.width(), info.height(), size);
        int drawHeight = CustomCrosshairSizing.fitHeight(info.width(), info.height(), size);
        if (drawWidth <= 0 || drawHeight <= 0) {
            return;
        }

        float x = (graphics.guiWidth() - drawWidth) / 2.0F;
        float y = (graphics.guiHeight() - drawHeight) / 2.0F;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(drawWidth, drawHeight);
            graphics.blit(texture, 0, 0, 1, 1, 0.0F, 1.0F, 0.0F, 1.0F);
        } finally {
            graphics.pose().popMatrix();
        }
    }
}
