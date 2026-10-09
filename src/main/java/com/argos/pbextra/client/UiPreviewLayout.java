package com.argos.pbextra.client;

import com.argos.pbextra.uieditor.TextField;
import com.argos.pbextra.uieditor.UiDefinitionKind;
import com.argos.pbextra.uieditor.UiEditorModel;
import com.argos.pbextra.uieditor.UiTextBlock;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public final class UiPreviewLayout {

    /** Line height used for hit boxes and for the block outlines. */
    public static final int LINE_HEIGHT = 9;
    private static final String INFINITY = "\u221e";

    /** One block of text, positioned the way Point Blank would position it. */
    public record Placed(String key, UiTextBlock block, String text, Component styled,
                         int color, float x, float y, float width, float height,
                         boolean drawn) {

        public float size() {
            return block.size();
        }

        public boolean contains(float previewX, float previewY) {
            float pad = 1.0f;
            return previewX >= x - pad && previewX <= x + width + pad
                    && previewY >= y - pad && previewY <= y + height + pad;
        }
    }

    private final UiEditorModel model;
    private final UiPreviewState state;
    private final int screenWidth;
    private final int screenHeight;
    private final float inertiaX;
    private final float inertiaY;

    public UiPreviewLayout(UiEditorModel model, UiPreviewState state,
                           int screenWidth, int screenHeight,
                           float inertiaX, float inertiaY) {
        this.model = model;
        this.state = state;
        this.screenWidth = Math.max(1, screenWidth);
        this.screenHeight = Math.max(1, screenHeight);
        this.inertiaX = inertiaX;
        this.inertiaY = inertiaY;
    }

    public UiEditorModel model() {
        return model;
    }

    public UiPreviewState state() {
        return state;
    }

    public int screenWidth() {
        return screenWidth;
    }

    public int screenHeight() {
        return screenHeight;
    }

    public String location() {
        String raw = model.location();
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    public float uiWidth() {
        return model.width();
    }

    public float uiHeight() {
        return model.height();
    }


    public boolean mirrored() {
        if (model.kind() == UiDefinitionKind.AMMO) {
            return state.mirrorHorizontally();
        }
        String location = location();
        return location.equals("BOTTOM_LEFT") || location.equals("TOP_LEFT")
                || location.equals("LEFT_OF_HOTBAR");
    }

    public float[] topLeft() {
        float x;
        float y;
        if (model.kind() == UiDefinitionKind.AMMO) {
            boolean mirrored = mirrored();
            x = screenWidth - uiWidth() - model.paddingX();
            y = screenHeight - uiHeight() - model.paddingY();
            switch (location()) {
                case "BOTTOM_LEFT" -> x = model.paddingX();
                case "TOP_RIGHT" -> y = model.paddingY();
                case "TOP_LEFT" -> {
                    x = model.paddingX();
                    y = model.paddingY();
                }
                default -> {
                }
            }
            if (mirrored) {
                x = screenWidth - x - uiWidth();
            }
            x += mirrored ? -inertiaX : inertiaX;
        } else {
            boolean mirrored = mirrored();
            x = screenWidth - uiWidth() - model.paddingX();
            y = screenHeight - uiHeight() - model.paddingY();
            switch (location()) {
                case "BOTTOM_LEFT" -> x = model.paddingX();
                case "TOP_RIGHT" -> y = model.paddingY();
                case "TOP_LEFT" -> {
                    x = model.paddingX();
                    y = model.paddingY();
                }
                case "LEFT_OF_HOTBAR" -> x = (screenWidth >> 1) - 97 - uiWidth() - model.paddingX();
                case "RIGHT_OF_HOTBAR" -> x = (screenWidth >> 1) + 97 + model.paddingX();
                default -> {
                }
            }
            x += inertiaX;
        }
        y += inertiaY;
        return new float[]{x, y};
    }


    public List<Placed> placed(Font font) {
        float[] origin = topLeft();
        boolean mirrored = mirrored();
        List<Placed> result = new ArrayList<>(4);
        if (model.kind() == UiDefinitionKind.AMMO) {
            UiTextBlock ammoText = model.block(UiEditorModel.AMMO_TEXT);
            place(result, font, UiEditorModel.AMMO_TEXT, ammoText, ammoString(), ammoColor(),
                    origin, mirrored, true);

            UiTextBlock capacityText = model.block(UiEditorModel.CAPACITY_TEXT);
            boolean capacityDrawn = capacityText.present()
                    && !(state.reloading() && capacityText.boolValue(TextField.HIDE_ON_RELOAD));
            place(result, font, UiEditorModel.CAPACITY_TEXT, capacityText,
                    formatCapacityText(state.effectiveMaxAmmo(), capacityText.boolValue(TextField.SHOW_SLASH)),
                    capacityText.color(), origin, mirrored, capacityDrawn);

            UiTextBlock fireModeText = model.block(UiEditorModel.FIRE_MODE_TEXT);
            place(result, font, UiEditorModel.FIRE_MODE_TEXT, fireModeText, state.fireModeName(),
                    fireModeText.color(), origin, mirrored, fireModeText.boolValue(TextField.ENABLED));
        } else {
            UiTextBlock percentageText = model.block(UiEditorModel.PERCENTAGE_TEXT);
            String text = Math.round(state.heatRatio() * 100.0f) + "%";
            place(result, font, UiEditorModel.PERCENTAGE_TEXT, percentageText, text,
                    heatColor(state.heatRatio(), percentageText.emptyColor(), percentageText.fullColor()),
                    origin, mirrored, percentageText.boolValue(TextField.ENABLED));
        }
        return result;
    }

    public Placed hitTest(Font font, float previewX, float previewY) {
        List<Placed> placed = placed(font);
        for (int index = placed.size() - 1; index >= 0; index--) {
            Placed candidate = placed.get(index);
            if (candidate.contains(previewX, previewY)) {
                return candidate;
            }
        }
        return null;
    }

    public Placed find(Font font, String key) {
        if (key == null) {
            return null;
        }
        for (Placed candidate : placed(font)) {
            if (candidate.key().equals(key)) {
                return candidate;
            }
        }
        return null;
    }

    public String ammoString() {
        if (state.reloading()) {
            return model.reloadText() == null ? "" : model.reloadText();
        }
        if (state.showMessage()) {
            return state.messageText();
        }
        UiTextBlock ammoText = model.block(UiEditorModel.AMMO_TEXT);
        return formatAmmoText(state.currentAmmo(), state.effectiveMaxAmmo(),
                ammoText.boolValue(TextField.DIGITAL_STYLE));
    }

    private int ammoColor() {
        UiTextBlock ammoText = model.block(UiEditorModel.AMMO_TEXT);
        return state.ammoEmpty() ? ammoText.emptyColor() : ammoText.color();
    }

    private void place(List<Placed> out, Font font, String key, UiTextBlock block, String text,
                       int color, float[] origin, boolean mirrored, boolean drawn) {
        if (block == null) {
            return;
        }
        Component styled = styled(text, block.boolValue(TextField.ITALIC), block.boolValue(TextField.BOLD));
        float width = font.width(styled) * block.size();
        float textX = block.x();
        if (mirrored) {
            textX = uiWidth() - textX - width;
        }
        out.add(new Placed(key, block, text, styled, color,
                origin[0] + textX, origin[1] + block.y(), width, LINE_HEIGHT * block.size(), drawn));
    }

    public static String formatAmmoText(int currentAmmo, int maxAmmo, boolean digitalStyle) {
        if (maxAmmo == Integer.MAX_VALUE) {
            return INFINITY;
        }
        if (!digitalStyle) {
            return String.valueOf(Math.max(currentAmmo, 0));
        }
        int width = Math.max(1, String.valueOf(Math.max(maxAmmo, 0)).length());
        return String.format("%0" + width + "d", Math.max(currentAmmo, 0));
    }

    public static String formatCapacityText(int maxAmmo, boolean showSlash) {
        if (maxAmmo == Integer.MAX_VALUE) {
            return showSlash ? "/" + INFINITY : INFINITY;
        }
        String text = String.valueOf(Math.max(maxAmmo, 0));
        return showSlash ? "/" + text : text;
    }

    public static Component styled(String text, boolean italic, boolean bold) {
        return Component.literal(text == null ? "" : text)
                .withStyle(style -> style.withItalic(italic).withBold(bold));
    }

    public static int heatColor(float heatRatio, int emptyColor, int fullColor) {
        int alpha = Math.round(Mth.lerp(heatRatio, (float) (emptyColor >>> 24), (float) (fullColor >>> 24)));
        int red = Math.round(Mth.lerp(heatRatio, (float) (emptyColor >> 16 & 0xFF),
                (float) (fullColor >> 16 & 0xFF)));
        int green = Math.round(Mth.lerp(heatRatio, (float) (emptyColor >> 8 & 0xFF),
                (float) (fullColor >> 8 & 0xFF)));
        int blue = Math.round(Mth.lerp(heatRatio, (float) (emptyColor & 0xFF), (float) (fullColor & 0xFF)));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static ResourceLocation texture(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.contains(":")) {
            return ResourceLocation.tryParse(value);
        }
        return ResourceLocation.tryParse("pointblank:" + value);
    }
}
