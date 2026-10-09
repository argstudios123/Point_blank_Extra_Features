package com.argos.pbextra.uieditor;

import java.util.EnumSet;
import java.util.Locale;


public final class UiTextBlock {

    public static final EnumSet<TextField> AMMO_TEXT = EnumSet.of(
            TextField.X, TextField.Y, TextField.SIZE, TextField.COLOR, TextField.EMPTY_COLOR,
            TextField.BORDER, TextField.DROP_SHADOW, TextField.ITALIC, TextField.BOLD,
            TextField.DIGITAL_STYLE);

    public static final EnumSet<TextField> CAPACITY_TEXT = EnumSet.of(
            TextField.X, TextField.Y, TextField.SIZE, TextField.COLOR,
            TextField.BORDER, TextField.DROP_SHADOW, TextField.ITALIC, TextField.BOLD,
            TextField.ENABLED, TextField.HIDE_ON_RELOAD, TextField.SHOW_SLASH);

    public static final EnumSet<TextField> FIRE_MODE_TEXT = EnumSet.of(
            TextField.X, TextField.Y, TextField.SIZE, TextField.COLOR,
            TextField.BORDER, TextField.DROP_SHADOW, TextField.ITALIC, TextField.BOLD,
            TextField.ENABLED);

    public static final EnumSet<TextField> PERCENTAGE_TEXT = EnumSet.of(
            TextField.X, TextField.Y, TextField.SIZE, TextField.EMPTY_COLOR, TextField.FULL_COLOR,
            TextField.BORDER, TextField.DROP_SHADOW, TextField.ITALIC, TextField.BOLD,
            TextField.ENABLED);

    private final EnumSet<TextField> fields;
    private boolean present = true;
    private int x;
    private int y;
    private float size = 1.0f;
    private int color = 0xFFFFFFFF;
    private int emptyColor = 0xFF0000FF;
    private int fullColor = 0xFFFF0000;
    private boolean border;
    private boolean dropShadow;
    private boolean italic;
    private boolean bold;
    private boolean enabled = true;
    private boolean digitalStyle;
    private boolean hideOnReloadText;
    private boolean showSlash;

    public UiTextBlock(EnumSet<TextField> fields) {
        this.fields = EnumSet.copyOf(fields);
    }

    public UiTextBlock(UiTextBlock other) {
        this.fields = EnumSet.copyOf(other.fields);
        copyFrom(other);
    }

    public void copyFrom(UiTextBlock other) {
        this.present = other.present;
        this.x = other.x;
        this.y = other.y;
        this.size = other.size;
        this.color = other.color;
        this.emptyColor = other.emptyColor;
        this.fullColor = other.fullColor;
        this.border = other.border;
        this.dropShadow = other.dropShadow;
        this.italic = other.italic;
        this.bold = other.bold;
        this.enabled = other.enabled;
        this.digitalStyle = other.digitalStyle;
        this.hideOnReloadText = other.hideOnReloadText;
        this.showSlash = other.showSlash;
    }

    public EnumSet<TextField> fields() {
        return fields;
    }

    public boolean supports(TextField field) {
        return fields.contains(field);
    }

    public boolean present() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public int x() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int y() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public float size() {
        return size;
    }

    public void setSize(float size) {
        this.size = Math.max(0.1f, size);
    }

    public int color() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int emptyColor() {
        return emptyColor;
    }

    public void setEmptyColor(int emptyColor) {
        this.emptyColor = emptyColor;
    }

    public int fullColor() {
        return fullColor;
    }

    public void setFullColor(int fullColor) {
        this.fullColor = fullColor;
    }

    public boolean boolValue(TextField field) {
        return switch (field) {
            case BORDER -> border;
            case DROP_SHADOW -> dropShadow;
            case ITALIC -> italic;
            case BOLD -> bold;
            case ENABLED -> enabled;
            case DIGITAL_STYLE -> digitalStyle;
            case HIDE_ON_RELOAD -> hideOnReloadText;
            case SHOW_SLASH -> showSlash;
            default -> false;
        };
    }

    public void setBoolValue(TextField field, boolean value) {
        switch (field) {
            case BORDER -> border = value;
            case DROP_SHADOW -> dropShadow = value;
            case ITALIC -> italic = value;
            case BOLD -> bold = value;
            case ENABLED -> enabled = value;
            case DIGITAL_STYLE -> digitalStyle = value;
            case HIDE_ON_RELOAD -> hideOnReloadText = value;
            case SHOW_SLASH -> showSlash = value;
            default -> {
            }
        }
    }

    public int colorValue(TextField field) {
        return switch (field) {
            case COLOR -> color;
            case EMPTY_COLOR -> emptyColor;
            case FULL_COLOR -> fullColor;
            default -> color;
        };
    }

    public void setColorValue(TextField field, int value) {
        switch (field) {
            case COLOR -> color = value;
            case EMPTY_COLOR -> emptyColor = value;
            case FULL_COLOR -> fullColor = value;
            default -> {
            }
        }
    }

    public double numberValue(TextField field) {
        return switch (field) {
            case X -> x;
            case Y -> y;
            case SIZE -> size;
            case COLOR, EMPTY_COLOR, FULL_COLOR -> colorValue(field);
            default -> 0.0D;
        };
    }

    public void setNumberValue(TextField field, double value) {
        switch (field) {
            case X -> x = (int) Math.round(value);
            case Y -> y = (int) Math.round(value);
            case SIZE -> setSize((float) value);
            default -> {
            }
        }
    }

    public String displayValue(TextField field) {
        return switch (field.type()) {
            case INT -> Integer.toString((int) numberValue(field));
            case FLOAT -> String.format(Locale.ROOT, "%.2f", (float) numberValue(field));
            case COLOR -> UiColors.format(colorValue(field));
            case BOOL -> boolValue(field) ? "ON" : "OFF";
        };
    }
}
