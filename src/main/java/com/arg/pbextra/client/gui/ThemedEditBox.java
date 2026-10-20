package com.arg.pbextra.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * EditBox with an editor-theme background. The vanilla sprite is disabled and
 * replaced with a themed border/fill; the text is shifted inward so the input
 * keeps the same padding as a normal bordered EditBox.
 */
public final class ThemedEditBox extends EditBox {

    private boolean renderingText;

    public ThemedEditBox(Font font, int x, int y, int width, int height, Component narration) {
        super(font, x, y, width, height, narration);
    }

    /**
     * Keep EditBox's normal bordered text placement/click mapping, but hide its
     * vanilla sprite while the themed background is being drawn.
     */
    @Override
    public boolean isBordered() {
        return !renderingText;
    }

    @Override
    public int getInnerWidth() {
        return Math.max(1, getWidth() - 8);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (UiEditorTheme.active() == UiEditorTheme.DEFAULT) {
            setTextColor(EditBox.DEFAULT_TEXT_COLOR);
            setTextColorUneditable(0xFF707070);
            super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        UiEditorTheme.Palette palette = UiEditorTheme.active().palette();
        int border = isFocused() ? palette.selected() : palette.buttonBorder();
        int left = getX();
        int top = getY();
        int right = getX() + getWidth();
        int bottom = getY() + getHeight();
        graphics.fill(left, top, right, bottom, border);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, palette.fieldBackground());

        setTextColor(palette.fieldText());
        setTextColorUneditable(palette.hint());

        renderingText = true;
        try {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        } finally {
            renderingText = false;
        }
    }
}
