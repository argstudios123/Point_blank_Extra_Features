package com.arg.pbextra.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;


public class FlatButton extends AbstractButton {

    public static final int DEFAULT_TEXT_COLOR = 0xFFE0E0E0;

    private final Font font;
    private final Runnable action;
    private int textColor;
    private int leftPadding = 3;

    public FlatButton(Font font, int width, int height, String label, Runnable action) {
        super(0, 0, width, height, Component.literal(label == null ? "" : label));
        this.font = font;
        this.action = action;
        this.textColor = UiEditorTheme.active().palette().buttonText();
    }

    public FlatButton textColor(int color) {
        this.textColor = color;
        return this;
    }

    public FlatButton leftPadding(int padding) {
        this.leftPadding = padding;
        return this;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (action != null) {
            action.run();
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        UiEditorTheme.Palette palette = UiEditorTheme.active().palette();
        boolean hovered = active && isHoveredOrFocused();
        int border = !active ? palette.buttonBorderDisabled()
                : hovered ? palette.buttonBorderHovered() : palette.buttonBorder();
        int fill = !active ? palette.buttonFillDisabled()
                : hovered ? palette.buttonFillHovered() : palette.buttonFill();
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), border);
        graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, fill);
        if (getMessage() != null) {
            graphics.text(font, getMessage(), getX() + leftPadding,
                    getY() + (getHeight() - 8) / 2 + 1,
                    active ? textColor : palette.buttonTextDisabled(), false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
