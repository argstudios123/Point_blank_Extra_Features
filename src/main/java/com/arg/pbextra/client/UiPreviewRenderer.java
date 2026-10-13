package com.arg.pbextra.client;

import com.arg.pbextra.PointBlankExtraFeatures;
import com.arg.pbextra.uieditor.TextField;
import com.arg.pbextra.uieditor.UiDefinitionKind;
import com.arg.pbextra.uieditor.UiEditorModel;
import com.arg.pbextra.uieditor.UiTextBlock;
import com.vicmatskiv.pointblank.client.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;


public final class UiPreviewRenderer {

    private static final int CANVAS_BACKGROUND = 0xFF0C1014;
    private static final int CANVAS_BORDER = 0xFF3A444E;
    private static final int GRID_COLOR = 0x14FFFFFF;
    private static final int HORIZON_COLOR = 0x30FFFFFF;
    private static final int HOTBAR_COLOR = 0xC0101010;
    private static final int HOTBAR_BORDER = 0xFF303840;
    private static final int SLOT_COLOR = 0x60FFFFFF;
    private static final int SLOT_BORDER = 0x40FFFFFF;
    private static final int SLOT_HELD_BORDER = 0xFFE0E0E0;
    private static final int DEFINITION_OUTLINE = 0xFF6FD86F;
    private static final int SELECTED_OUTLINE = 0xFF33D6FF;
    private static final int BLOCK_OUTLINE = 0x66FFFFFF;
    private static final int GHOST_ALPHA = 0x55;
    private static final int GHOST_OUTLINE = 0x99FF9955;
    private static final int GUIDE_TEXT = 0xFFB8C4D0;
    private static final int LABEL_BACKGROUND = 0xA0000000;

    private static boolean maskedProgressUnavailable;

    private UiPreviewRenderer() {
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, UiPreviewLayout layout,
                              UiPreviewViewport viewport, int canvasLeft, int canvasTop,
                              int canvasRight, int canvasBottom, boolean showGuides,
                              String selectedKey) {
        graphics.enableScissor(canvasLeft, canvasTop, canvasRight, canvasBottom);
        graphics.fill(canvasLeft, canvasTop, canvasRight, canvasBottom, CANVAS_BACKGROUND);

        // Minecraft 26.1.2 replaced the old RenderSystem blend/depth toggles with the
        // GuiRenderState pipeline system: the extractor batches this preview into its
        // own scissor area and the pipeline handles blending, so no global
        // RenderSystem state is changed here any more.
        graphics.pose().pushMatrix();
        graphics.pose().translate(viewport.originX(), viewport.originY());
        graphics.pose().scale(viewport.zoom(), viewport.zoom());

        drawMockScreen(graphics, layout);
        drawHotbar(graphics, layout);
        if (layout.model().kind() == UiDefinitionKind.AMMO) {
            drawAmmoUi(graphics, layout);
        } else {
            drawHeatUi(graphics, layout);
        }
        drawTexts(graphics, font, layout);
        if (showGuides) {
            drawGuides(graphics, font, layout, selectedKey);
        }

        graphics.pose().popMatrix();

        graphics.outline(canvasLeft, canvasTop, canvasRight - canvasLeft,
                canvasBottom - canvasTop, CANVAS_BORDER);
        drawCornerLabels(graphics, font, layout, canvasLeft, canvasTop, canvasBottom, selectedKey);
        graphics.disableScissor();
    }


    private static void drawMockScreen(GuiGraphicsExtractor graphics, UiPreviewLayout layout) {
        int width = layout.screenWidth();
        int height = layout.screenHeight();
        graphics.fill(0, 0, width, height, 0x80202C38);
        graphics.fill(0, height / 2, width, height / 2 + 1, HORIZON_COLOR);
        for (int x = 0; x <= width; x += 32) {
            graphics.fill(x, 0, x + 1, height, GRID_COLOR);
        }
        for (int y = 0; y <= height; y += 32) {
            graphics.fill(0, y, width, y + 1, GRID_COLOR);
        }
    }

    private static void drawHotbar(GuiGraphicsExtractor graphics, UiPreviewLayout layout) {
        int width = layout.screenWidth();
        int height = layout.screenHeight();
        int left = width / 2 - 91;
        int top = height - 22;
        graphics.fill(left, top, left + 182, top + 22, HOTBAR_COLOR);
        outline(graphics, left, top, 182, 22, HOTBAR_BORDER, 1.0F);
        for (int slot = 0; slot < 9; slot++) {
            int slotLeft = left + 3 + slot * 20;
            int slotTop = top + 3;
            graphics.fill(slotLeft, slotTop, slotLeft + 16, slotTop + 16, SLOT_COLOR);
            outline(graphics, slotLeft, slotTop, 16, 16,
                    slot == 0 ? SLOT_HELD_BORDER : SLOT_BORDER, 1.0F);
        }
    }


    private static void drawAmmoUi(GuiGraphicsExtractor graphics, UiPreviewLayout layout) {
        UiEditorModel model = layout.model();
        float[] origin = layout.topLeft();
        drawTexture(graphics, UiPreviewLayout.texture(model.texture()),
                origin[0], origin[1], model.width(), model.height(), layout.mirrored());
    }

    private static void drawHeatUi(GuiGraphicsExtractor graphics, UiPreviewLayout layout) {
        UiEditorModel model = layout.model();
        float[] origin = layout.topLeft();
        boolean mirrored = layout.mirrored();
        Identifier background = UiPreviewLayout.texture(model.backgroundTexture());
        Identifier progress = UiPreviewLayout.texture(model.progressTexture());
        Identifier mask = UiPreviewLayout.texture(model.maskTexture());
        drawTexture(graphics, background, origin[0], origin[1], model.width(), model.height(), mirrored);
        if (progress != null) {
            drawMaskedProgress(graphics, progress, mask, layout.state().heatRatio(),
                    origin[0], origin[1], model.width(), model.height(), mirrored);
        }
    }

    private static void drawTexture(GuiGraphicsExtractor graphics, Identifier texture, float x, float y,
                                    int width, int height, boolean mirrored) {
        if (!available(texture)) {
            checkerboard(graphics, x, y, width, height);
            return;
        }
        RenderUtil.blit(graphics, texture, x, x + width, y, y + height, 0.0F,
                mirrored ? 1.0F : 0.0F, mirrored ? 0.0F : 1.0F, 0.0F, 1.0F);
    }

    private static void drawMaskedProgress(GuiGraphicsExtractor graphics, Identifier progress,
                                           Identifier mask, float ratio, float x, float y,
                                           int width, int height, boolean mirrored) {
        // Point Blank 26.1.2 removed its masked heat-progress blit helper together with the
        // legacy GUI pipeline, so the editor preview always draws the progress texture clipped
        // to the filled fraction of the bar (the fallback this method already used).
        // Fallback: the progress texture, clipped to the filled fraction of the bar.
        RenderUtil.blit(graphics, progress, x, x + width * ratio, y, y + height, 0.0F,
                mirrored ? 1.0F - ratio : 0.0F, mirrored ? 1.0F : ratio, 0.0F, 1.0F);
    }

    private static boolean available(Identifier texture) {
        return texture != null
                && Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
    }


    private static void drawTexts(GuiGraphicsExtractor graphics, Font font, UiPreviewLayout layout) {
        for (UiPreviewLayout.Placed placed : layout.placed(font)) {
            if (placed.text() == null || placed.text().isEmpty()) {
                continue;
            }
            drawStyledText(graphics, font, placed, placed.drawn());
        }
    }


    private static void drawStyledText(GuiGraphicsExtractor graphics, Font font,
                                       UiPreviewLayout.Placed placed, boolean drawn) {
        UiTextBlock block = placed.block();
        Component text = placed.styled();
        int color = drawn ? placed.color() : ghost(placed.color());
        float size = Math.max(0.1f, block.size());
        boolean shadow = block.boolValue(TextField.DROP_SHADOW);
        graphics.pose().pushMatrix();
        graphics.pose().translate(placed.x(), placed.y());
        graphics.pose().scale(size, size);
        if (block.boolValue(TextField.BORDER)) {
            int border = dim(color);
            graphics.text(font, text, 1, 0, border, shadow);
            graphics.text(font, text, -1, 0, border, shadow);
            graphics.text(font, text, 0, 1, border, shadow);
            graphics.text(font, text, 0, -1, border, shadow);
        }
        graphics.text(font, text, 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }

    private static void drawGuides(GuiGraphicsExtractor graphics, Font font, UiPreviewLayout layout,
                                   String selectedKey) {
        int screenWidth = layout.screenWidth();
        int screenHeight = layout.screenHeight();
        graphics.fill(screenWidth / 2, 0, screenWidth / 2 + 1, screenHeight, HORIZON_COLOR);
        graphics.fill(0, screenHeight / 2, screenWidth, screenHeight / 2 + 1, HORIZON_COLOR);

        float[] origin = layout.topLeft();
        float uiWidth = layout.uiWidth();
        float uiHeight = layout.uiHeight();
        outline(graphics, origin[0] - 1.0F, origin[1] - 1.0F, uiWidth + 2.0F, uiHeight + 2.0F,
                DEFINITION_OUTLINE, 1.0F);
        graphics.text(font, Math.round(uiWidth) + "x" + Math.round(uiHeight),
                Math.round(origin[0]), Math.round(origin[1]) - 9, DEFINITION_OUTLINE, false);

        for (UiPreviewLayout.Placed placed : layout.placed(font)) {
            boolean selected = placed.key().equals(selectedKey);
            int color = selected ? SELECTED_OUTLINE : placed.drawn() ? BLOCK_OUTLINE : GHOST_OUTLINE;
            outline(graphics, placed.x() - 1.0F, placed.y() - 1.0F,
                    Math.max(1.0F, placed.width()) + 2.0F,
                    Math.max(1.0F, placed.height()) + 2.0F, color, 1.0F);
        }
    }

    private static void checkerboard(GuiGraphicsExtractor graphics, float x, float y, int width, int height) {
        int cell = 4;
        int left = Math.round(x);
        int top = Math.round(y);
        for (int cellX = 0; cellX < width; cellX += cell) {
            for (int cellY = 0; cellY < height; cellY += cell) {
                int color = ((cellX / cell + cellY / cell) % 2 == 0) ? 0xFF101010 : 0xFFF010F0;
                graphics.fill(left + cellX, top + cellY,
                        left + Math.min(cellX + cell, width), top + Math.min(cellY + cell, height), color);
            }
        }
    }


    private static void drawCornerLabels(GuiGraphicsExtractor graphics, Font font, UiPreviewLayout layout,
                                         int canvasLeft, int canvasTop, int canvasBottom,
                                         String selectedKey) {
        label(graphics, font, layout.screenWidth() + "x" + layout.screenHeight()
                + "  " + layout.location(), canvasLeft + 3, canvasTop + 3, GUIDE_TEXT);

        float[] origin = layout.topLeft();
        String info = "origin " + Math.round(origin[0]) + "," + Math.round(origin[1])
                + (layout.mirrored() ? "  mirrored" : "")
                + (layout.model().isFromScratch() ? "  new file" : "");
        label(graphics, font, info, canvasLeft + 3, canvasBottom - 10, GUIDE_TEXT);
        if (selectedKey != null) {
            label(graphics, font, "selected " + selectedKey, canvasLeft + 3, canvasTop + 13,
                    SELECTED_OUTLINE);
        }
    }


    private static void outline(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                int color, float thickness) {
        int left = Math.round(x);
        int top = Math.round(y);
        int right = Math.round(x + width);
        int bottom = Math.round(y + height);
        int pixel = Math.max(1, Math.round(thickness));
        graphics.fill(left, top, right, top + pixel, color);
        graphics.fill(left, bottom - pixel, right, bottom, color);
        graphics.fill(left, top + pixel, left + pixel, bottom - pixel, color);
        graphics.fill(right - pixel, top + pixel, right, bottom - pixel, color);
    }

    private static void label(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
        graphics.fill(x - 1, y - 1, x + font.width(text) + 1, y + 9, LABEL_BACKGROUND);
        graphics.text(font, text, x, y, color, false);
    }

    private static int dim(int color) {
        int factor = 0x66;
        int red = (color >> 16 & 0xFF) * factor / 0xFF;
        int green = (color >> 8 & 0xFF) * factor / 0xFF;
        int blue = (color & 0xFF) * factor / 0xFF;
        return color & 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int ghost(int color) {
        int red = ((color >> 16 & 0xFF) * 3 + (CANVAS_BACKGROUND >> 16 & 0xFF) * 2) / 5;
        int green = ((color >> 8 & 0xFF) * 3 + (CANVAS_BACKGROUND >> 8 & 0xFF) * 2) / 5;
        int blue = ((color & 0xFF) * 3 + (CANVAS_BACKGROUND & 0xFF) * 2) / 5;
        return color & 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
