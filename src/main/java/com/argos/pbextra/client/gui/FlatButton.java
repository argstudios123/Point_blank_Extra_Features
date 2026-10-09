package com.argos.pbextra.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;


public class FlatButton extends AbstractButton {

    private static final int BORDER_COLOR = 0xFF505860;
    private static final int BORDER_COLOR_HOVERED = 0xFFB0C0D0;
    private static final int BORDER_COLOR_DISABLED = 0xFF383838;
    private static final int FILL_COLOR = 0xFF262B30;
    private static final int FILL_COLOR_HOVERED = 0xFF3A4249;
    private static final int FILL_COLOR_DISABLED = 0xFF1A1D20;
    public static final int DEFAULT_TEXT_COLOR = 0xFFE0E0E0;

    private final Font font;
    private final Runnable action;
    private int textColor = DEFAULT_TEXT_COLOR;
    private int leftPadding = 3;

    public FlatButton(Font font, int width, int height, String label, Runnable action) {
        super(0, 0, width, height, Component.literal(label == null ? "" : label));
        this.font = font;
        this.action = action;
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
    public void onPress() {
        if (action != null) {
            action.run();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = active && isHoveredOrFocused();
        int border = !active ? BORDER_COLOR_DISABLED : hovered ? BORDER_COLOR_HOVERED : BORDER_COLOR;
        int fill = !active ? FILL_COLOR_DISABLED : hovered ? FILL_COLOR_HOVERED : FILL_COLOR;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), border);
        graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, fill);
        if (getMessage() != null) {
            graphics.drawString(font, getMessage(), getX() + leftPadding,
                    getY() + (getHeight() - 8) / 2 + 1, active ? textColor : 0xFF808080, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
