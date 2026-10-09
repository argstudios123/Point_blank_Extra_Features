package com.argos.pbextra.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;


public final class ToggleWidget extends FlatButton {

    private static final int ON_COLOR = 0xFF7CE07C;
    private static final int OFF_COLOR = 0xFF9A9A9A;
    private static final Component ON_TEXT = Component.literal("ON");
    private static final Component OFF_TEXT = Component.literal("OFF");

    private final Font font;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    public ToggleWidget(Font font, int width, int height, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(font, width, height, "", null);
        this.font = font;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void onPress() {
        boolean next = !Boolean.TRUE.equals(getter.get());
        setter.accept(next);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean value = Boolean.TRUE.equals(getter.get());
        Component message = value ? ON_TEXT : OFF_TEXT;
        setMessage(message);
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        // The base class already drew the label at the left; keep the state on
        // the right so long labels never collide with it.
        if (value) {
            graphics.drawString(font, message, getX() + getWidth() - 3 - font.width(message), getY() + 2, ON_COLOR, false);
        } else {
            graphics.drawString(font, message, getX() + getWidth() - 3 - font.width(message), getY() + 2, OFF_COLOR, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
