package com.argos.pbextra.client.gui;

import com.argos.pbextra.uieditor.UiColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;


public final class ColorSwatchWidget extends FlatButton {

    private final Supplier<Integer> getter;

    public ColorSwatchWidget(Font font, int width, int height, Supplier<Integer> getter, Runnable action) {
        super(font, width, height, "", action);
        this.getter = getter;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int color = getter.get() == null ? 0xFFFFFFFF : getter.get();
        int border = active && isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF505860;
        int left = getX() + 1;
        int top = getY() + 1;
        int right = getX() + getWidth() - 1;
        int bottom = getY() + getHeight() - 1;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), border);
        // Checkered backing so alpha values stay readable, then the color on top.
        for (int x = left; x < right; x += 4) {
            for (int y = top; y < bottom; y += 4) {
                int checker = ((x - left) / 4 + (y - top) / 4) % 2 == 0 ? 0xFF606060 : 0xFFA0A0A0;
                graphics.fill(x, y, Math.min(x + 4, right), Math.min(y + 4, bottom), checker);
            }
        }
        graphics.fill(left, top, right, bottom, color);
    }

    public String describe() {
        return UiColors.format(getter.get() == null ? 0xFFFFFFFF : getter.get());
    }
}
