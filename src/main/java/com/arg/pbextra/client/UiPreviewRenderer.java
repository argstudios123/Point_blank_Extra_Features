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
    // Screen-space positioning handles (sizes are in GUI pixels, not preview pixels).
    private static final int HANDLE_SIZE = 6;
    private static final int HANDLE_OFFSET = 8;
    private static final int HANDLE_COLOR = 0xC0E0E0E0;
    private static final int HANDLE_BORDER = 0xFF101014;
    // HUD padding / resize handles (screen-space, outside the green rectangle).
    private static final int HUD_HANDLE_GAP = 8;
    private static final int HUD_HANDLE_HIT = 12;
    private static final int HUD_PADDING_COLOR = 0xFFFFB000;
    private static final int HUD_PADDING_BORDER = 0xFF3A2400;
    private static final int HUD_RESIZE_COLOR = 0xFF7CE07C;
    private static final int HUD_RESIZE_FILL = 0xE0101010;

    /** Screen-space rectangle of one HUD padding/resize handle. */
    public record HudHandle(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

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

        if (showGuides) {
            drawHandles(graphics, font, layout, viewport, selectedKey);
            drawHudHandles(graphics, font, layout, viewport);
        }
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
        if (!available(progress)) {
            return;
        }
        // Point Blank 26.1.2 removed its masked heat-progress blit helper together with the
        // legacy GUI pipeline, so the editor preview always draws the progress texture clipped
        // to the filled fraction of the bar (the fallback this method already used).
        // Fallback: the progress texture, clipped to the filled fraction of the bar.
        RenderUtil.blit(graphics, progress, x, x + width * ratio, y, y + height, 0.0F,
                mirrored ? 1.0F - ratio : 0.0F, mirrored ? 1.0F : ratio, 0.0F, 1.0F);
    }

    private static boolean available(Identifier texture) {
        if (texture == null) {
            return false;
        }
        try {
            return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
        } catch (RuntimeException exception) {
            // An incomplete texture path (for example one with an empty directory
            // while the user is typing) can make the pack resource lookup throw.
            // The preview treats it as missing and keeps drawing the placeholder.
            return false;
        }
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

    private static void drawHandles(GuiGraphicsExtractor graphics, Font font, UiPreviewLayout layout,
                                    UiPreviewViewport viewport, String selectedKey) {
        for (UiPreviewLayout.Placed placed : layout.placed(font)) {
            if (!isPositionable(placed)) {
                continue;
            }
            int x = handleX(placed, viewport);
            int y = handleY(placed, viewport);
            boolean selected = placed.key().equals(selectedKey);
            graphics.fill(x, y, x + HANDLE_SIZE, y + HANDLE_SIZE, HANDLE_BORDER);
            graphics.fill(x + 1, y + 1, x + HANDLE_SIZE - 1, y + HANDLE_SIZE - 1,
                    selected ? SELECTED_OUTLINE : HANDLE_COLOR);
        }
    }

    /** True when the screen-space positioning handle of {@code placed} contains the mouse. */
    public static boolean handleHit(UiPreviewLayout.Placed placed, UiPreviewViewport viewport,
                                    double mouseX, double mouseY) {
        int x = handleX(placed, viewport);
        int y = handleY(placed, viewport);
        return mouseX >= x && mouseX < x + HANDLE_SIZE && mouseY >= y && mouseY < y + HANDLE_SIZE;
    }

    private static boolean isPositionable(UiPreviewLayout.Placed placed) {
        UiTextBlock block = placed.block();
        return block != null && block.present()
                && block.supports(TextField.X) && block.supports(TextField.Y);
    }

    private static int handleX(UiPreviewLayout.Placed placed, UiPreviewViewport viewport) {
        return Math.round(viewport.toScreenX(placed.x())) - HANDLE_OFFSET;
    }

    private static int handleY(UiPreviewLayout.Placed placed, UiPreviewViewport viewport) {
        return Math.round(viewport.toScreenY(placed.y())) - HANDLE_OFFSET;
    }

    public static HudHandle paddingXHandle(UiPreviewLayout layout, UiPreviewViewport viewport) {
        float[] origin = layout.topLeft();
        boolean left = layout.paddingXSign() > 0;
        float edgeX = left ? origin[0] : origin[0] + layout.uiWidth();
        int centerX = clampHandleX(layout, viewport,
                Math.round(viewport.toScreenX(edgeX)) + (left ? -HUD_HANDLE_GAP : HUD_HANDLE_GAP));
        int centerY = clampHandleY(layout, viewport,
                Math.round(viewport.toScreenY(origin[1] + layout.uiHeight() / 2.0F)));
        return centeredHandle(centerX, centerY);
    }

    public static HudHandle paddingYHandle(UiPreviewLayout layout, UiPreviewViewport viewport) {
        float[] origin = layout.topLeft();
        boolean top = layout.paddingYSign() > 0;
        float edgeY = top ? origin[1] : origin[1] + layout.uiHeight();
        int centerX = clampHandleX(layout, viewport,
                Math.round(viewport.toScreenX(origin[0] + layout.uiWidth() / 2.0F)));
        int centerY = clampHandleY(layout, viewport,
                Math.round(viewport.toScreenY(edgeY)) + (top ? -HUD_HANDLE_GAP : HUD_HANDLE_GAP));
        return centeredHandle(centerX, centerY);
    }

    public static HudHandle widthHandle(UiPreviewLayout layout, UiPreviewViewport viewport) {
        float[] origin = layout.topLeft();
        boolean freeRight = layout.paddingXSign() > 0;
        float edgeX = freeRight ? origin[0] + layout.uiWidth() : origin[0];
        int centerX = clampHandleX(layout, viewport,
                Math.round(viewport.toScreenX(edgeX)) + (freeRight ? HUD_HANDLE_GAP : -HUD_HANDLE_GAP));
        int centerY = clampHandleY(layout, viewport,
                Math.round(viewport.toScreenY(origin[1] + layout.uiHeight() / 2.0F)));
        return centeredHandle(centerX, centerY);
    }

    public static HudHandle heightHandle(UiPreviewLayout layout, UiPreviewViewport viewport) {
        float[] origin = layout.topLeft();
        boolean freeBottom = layout.paddingYSign() > 0;
        float edgeY = freeBottom ? origin[1] + layout.uiHeight() : origin[1];
        int centerX = clampHandleX(layout, viewport,
                Math.round(viewport.toScreenX(origin[0] + layout.uiWidth() / 2.0F)));
        int centerY = clampHandleY(layout, viewport,
                Math.round(viewport.toScreenY(edgeY)) + (freeBottom ? HUD_HANDLE_GAP : -HUD_HANDLE_GAP));
        return centeredHandle(centerX, centerY);
    }

    private static int clampHandleX(UiPreviewLayout layout, UiPreviewViewport viewport, int centerX) {
        int min = Math.round(viewport.toScreenX(0)) + HUD_HANDLE_HIT / 2;
        int max = Math.round(viewport.toScreenX(layout.screenWidth())) - HUD_HANDLE_HIT / 2;
        return Math.max(min, Math.min(max, centerX));
    }

    private static int clampHandleY(UiPreviewLayout layout, UiPreviewViewport viewport, int centerY) {
        int min = Math.round(viewport.toScreenY(0)) + HUD_HANDLE_HIT / 2;
        int max = Math.round(viewport.toScreenY(layout.screenHeight())) - HUD_HANDLE_HIT / 2;
        return Math.max(min, Math.min(max, centerY));
    }

    private static HudHandle centeredHandle(int centerX, int centerY) {
        return new HudHandle(centerX - HUD_HANDLE_HIT / 2, centerY - HUD_HANDLE_HIT / 2,
                HUD_HANDLE_HIT, HUD_HANDLE_HIT);
    }

    private static void drawHudHandles(GuiGraphicsExtractor graphics, Font font,
                                       UiPreviewLayout layout, UiPreviewViewport viewport) {
        HudHandle paddingX = paddingXHandle(layout, viewport);
        graphics.fill(paddingX.x() + 4, paddingX.y(), paddingX.x() + 8,
                paddingX.y() + paddingX.height(), HUD_PADDING_BORDER);
        graphics.fill(paddingX.x() + 5, paddingX.y() + 1, paddingX.x() + 7,
                paddingX.y() + paddingX.height() - 1, HUD_PADDING_COLOR);

        HudHandle paddingY = paddingYHandle(layout, viewport);
        graphics.fill(paddingY.x(), paddingY.y() + 4, paddingY.x() + paddingY.width(),
                paddingY.y() + 8, HUD_PADDING_BORDER);
        graphics.fill(paddingY.x() + 1, paddingY.y() + 5, paddingY.x() + paddingY.width() - 1,
                paddingY.y() + 7, HUD_PADDING_COLOR);

        drawHudResizeHandle(graphics, font, widthHandle(layout, viewport),
                layout.paddingXSign() > 0 ? ">" : "<");
        drawHudResizeHandle(graphics, font, heightHandle(layout, viewport),
                layout.paddingYSign() > 0 ? "v" : "^");
    }

    private static void drawHudResizeHandle(GuiGraphicsExtractor graphics, Font font,
                                            HudHandle handle, String arrow) {
        graphics.fill(handle.x(), handle.y(), handle.x() + handle.width(),
                handle.y() + handle.height(), HUD_RESIZE_FILL);
        outline(graphics, handle.x(), handle.y(), handle.width(), handle.height(),
                HUD_RESIZE_COLOR, 1.0F);
        int textX = handle.x() + (handle.width() - font.width(arrow)) / 2;
        int textY = handle.y() + (handle.height() - 8) / 2 + 1;
        graphics.text(font, arrow, textX, textY, HUD_RESIZE_COLOR, false);
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
