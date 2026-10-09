package com.argos.pbextra.client.gui;

import com.argos.pbextra.uieditor.UiColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

// Ui Color picker for easily targetting
public final class UiColorPicker {

    private static final int WIDTH = 172;
    private static final int HEIGHT = 98;
    private static final int CHANNELS = 4;
    private static final int BAR_HEIGHT = 10;
    private static final int BAR_SPACING = 4;
    private static final int BAR_LEFT = 24;
    private static final int BAR_RIGHT = 152;
    private static final int BARS_TOP = 20;
    private static final int BACKGROUND_COLOR = 0xF0181C20;
    private static final int BORDER_COLOR = 0xFFB0C0D0;
    private static final String[] CHANNEL_NAMES = {"A", "R", "G", "B"};

    private boolean open;
    private IntSupplier getter;
    private Consumer<Integer> setter;
    private String title = "Color";
    private int left;
    private int top;
    private int draggedChannel = -1;

    public boolean isOpen() {
        return open;
    }

    public void open(int screenWidth, int screenHeight, String title,
                      IntSupplier getter, Consumer<Integer> setter) {
        this.open = true;
        this.title = title == null ? "Color" : title;
        this.getter = getter;
        this.setter = setter;
        this.left = Math.max(4, (screenWidth - WIDTH) / 2);
        this.top = Math.max(4, (screenHeight - HEIGHT) / 2);
        this.draggedChannel = -1;
    }

    public void close() {
        this.open = false;
        this.draggedChannel = -1;
        this.getter = null;
        this.setter = null;
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    private int color() {
        return getter == null ? 0xFFFFFFFF : getter.getAsInt();
    }

    private void setColor(int argb) {
        if (setter != null) {
            setter.accept(argb);
        }
    }

    private int channel(int index) {
        return color() >>> (24 - index * 8) & 0xFF;
    }

    private int withChannel(int index, int value) {
        int shift = 24 - index * 8;
        return color() & ~(0xFF << shift) | value << shift;
    }


   // true when the click was consumed
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!open) {
            return false;
        }
        if (!isInside(mouseX, mouseY)) {
            close();
            return true;
        }
        int channel = channelAt(mouseY);
        if (channel >= 0) {
            draggedChannel = channel;
            applyChannel(channel, mouseX);
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!open || draggedChannel < 0) {
            return false;
        }
        applyChannel(draggedChannel, mouseX);
        return true;
    }

    public boolean mouseReleased() {
        if (draggedChannel < 0) {
            return false;
        }
        draggedChannel = -1;
        return true;
    }

    private void applyChannel(int channel, double mouseX) {
        double ratio = Mth.clamp((mouseX - (left + BAR_LEFT)) / (double) (BAR_RIGHT - BAR_LEFT), 0.0D, 1.0D);
        setColor(withChannel(channel, (int) Math.round(ratio * 255.0D)));
    }

    private boolean isInside(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < left + WIDTH && mouseY >= top && mouseY < top + HEIGHT;
    }

    private int channelAt(double mouseY) {
        for (int index = 0; index < CHANNELS; index++) {
            int barTop = top + BARS_TOP + index * (BAR_HEIGHT + BAR_SPACING);
            if (mouseY >= barTop - 2 && mouseY < barTop + BAR_HEIGHT + 2) {
                return index;
            }
        }
        return -1;
    }


    public void render(GuiGraphics graphics, Font font) {
        if (!open) {
            return;
        }
        int right = left + WIDTH;
        int bottom = top + HEIGHT;
        graphics.fill(left, top, right, bottom, BACKGROUND_COLOR);
        graphics.renderOutline(left, top, WIDTH, HEIGHT, BORDER_COLOR);
        graphics.drawString(font, title, left + 6, top + 6, 0xFFFFD24D, false);

        for (int index = 0; index < CHANNELS; index++) {
            int barTop = top + BARS_TOP + index * (BAR_HEIGHT + BAR_SPACING);
            renderBar(graphics, font, index, barTop);
        }

        int previewTop = top + BARS_TOP + CHANNELS * (BAR_HEIGHT + BAR_SPACING) + 2;
        int previewLeft = left + BAR_LEFT;
        int previewRight = previewLeft + 40;
        int previewBottom = previewTop + 12;
        for (int x = previewLeft + 1; x < previewRight - 1; x += 4) {
            for (int y = previewTop + 1; y < previewBottom - 1; y += 4) {
                int checker = ((x - previewLeft) / 4 + (y - previewTop) / 4) % 2 == 0 ? 0xFF606060 : 0xFFA0A0A0;
                graphics.fill(x, y, Math.min(x + 4, previewRight - 1), Math.min(y + 4, previewBottom - 1), checker);
            }
        }
        graphics.fill(previewLeft + 1, previewTop + 1, previewRight - 1, previewBottom - 1, color());
        graphics.renderOutline(previewLeft, previewTop, 40, 12, 0xFF303030);
        graphics.drawString(font, UiColors.format(color()), previewLeft + 46, previewTop + 2, 0xFFFFFFFF, false);
        graphics.drawString(font, "Esc or click outside to close", left + 6, previewTop + 16, 0xFF909090, false);
    }

    private void renderBar(GuiGraphics graphics, Font font, int index, int barTop) {
        int barLeft = left + BAR_LEFT;
        int barRight = left + BAR_RIGHT;
        graphics.drawString(font, CHANNEL_NAMES[index], left + 10, barTop + 1, 0xFFE0E0E0, false);

        int slices = barRight - barLeft;
        for (int slice = 0; slice < slices; slice++) {
            int value = (int) Math.round(slice / (double) (slices - 1) * 255.0D);
            graphics.fill(barLeft + slice, barTop, barLeft + slice + 1, barTop + BAR_HEIGHT, withChannel(index, value));
        }
        graphics.renderOutline(barLeft - 1, barTop - 1, barRight - barLeft + 2, BAR_HEIGHT + 2, 0xFF303030);
        int knob = barLeft + Math.round(channel(index) / 255.0f * (barRight - barLeft - 1));
        graphics.fill(knob, barTop - 1, knob + 1, barTop + BAR_HEIGHT + 1, 0xFFFFFFFF);
        graphics.drawString(font, Integer.toString(channel(index)), barRight + 4, barTop + 1, 0xFFE0E0E0, false);
    }
}
