package com.arg.pbextra.client.gui;

import com.arg.pbextra.config.PointBlankExtraConfig;

/**
 * Editor-only color themes. The preview renderer is deliberately not themed:
 * these colors affect the editor chrome, not the Ammo/Heat UI being edited.
 */
public enum UiEditorTheme {

    DEFAULT("DEFAULT", new PaletteBuilder()
            .screenBackground(0x00000000)
            .panelBackground(0xE6101418)
            .panelBorder(0xFF2E3841)
            .label(0xFFC2CCD6)
            .heading(0xFF7FD8FF)
            .hint(0xFF90A0B0)
            .success(0xFF7CE07C)
            .error(0xFFFF8080)
            .title(0xFFFFFFFF)
            .selected(0xFF33D6FF)
            .disabledText(0xFF707070)
            .scrim(0x99000000)
            .dialogBackground(0xF0181C20)
            .dialogTitle(0xFFFFD24D)
            .fieldBackground(0xFF000000)
            .fieldText(0xFFE0E0E0)
            .buttonBorder(0xFF505860)
            .buttonBorderHovered(0xFFB0C0D0)
            .buttonBorderDisabled(0xFF383838)
            .buttonFill(0xFF262B30)
            .buttonFillHovered(0xFF3A4249)
            .buttonFillDisabled(0xFF1A1D20)
            .buttonText(0xFFE0E0E0)
            .buttonTextDisabled(0xFF808080)
            .choiceSelected(0xFF2E5C6E)
            .build()),
    DARK("DARK", new PaletteBuilder()
            .screenBackground(0xF00B0D10)
            .panelBackground(0xE815181C)
            .panelBorder(0xFF303840)
            .label(0xFFC8D2DC)
            .heading(0xFF7FD8FF)
            .hint(0xFF8E9AA6)
            .success(0xFF7CE07C)
            .error(0xFFFF9090)
            .title(0xFFF5F8FA)
            .selected(0xFF55D6FF)
            .disabledText(0xFF707070)
            .scrim(0xB0000000)
            .dialogBackground(0xF814181C)
            .dialogTitle(0xFFFFD24D)
            .fieldBackground(0xFF080A0D)
            .fieldText(0xFFE6EDF3)
            .buttonBorder(0xFF3A444E)
            .buttonBorderHovered(0xFFB8C8D8)
            .buttonBorderDisabled(0xFF262B30)
            .buttonFill(0xFF1E2328)
            .buttonFillHovered(0xFF2C343C)
            .buttonFillDisabled(0xFF121518)
            .buttonText(0xFFE0E6EC)
            .buttonTextDisabled(0xFF707070)
            .choiceSelected(0xFF204E5C)
            .build()),

    LIGHT("LIGHT", new PaletteBuilder()
            .screenBackground(0xF0E9ECEF)
            .panelBackground(0xF2F7F8FA)
            .panelBorder(0xFFB4BCC4)
            .label(0xFF2E353C)
            .heading(0xFF146C88)
            .hint(0xFF5A6570)
            .success(0xFF1E7A34)
            .error(0xFFB42318)
            .title(0xFF101418)
            .selected(0xFF006A8A)
            .disabledText(0xFF6B6B6B)
            .scrim(0x99000000)
            .dialogBackground(0xFFFAFBFC)
            .dialogTitle(0xFF805500)
            .fieldBackground(0xFFFFFFFF)
            .fieldText(0xFF101418)
            .buttonBorder(0xFF9AA4AE)
            .buttonBorderHovered(0xFF405060)
            .buttonBorderDisabled(0xFFC8CDD2)
            .buttonFill(0xFFE6EAEE)
            .buttonFillHovered(0xFFD5DCE3)
            .buttonFillDisabled(0xFFF0F1F2)
            .buttonText(0xFF1A2026)
            .buttonTextDisabled(0xFF6B6B6B)
            .choiceSelected(0xFFACD8E6)
            .build()),
    MID_DARK("MID DARK", new PaletteBuilder()
            .screenBackground(0xF03D4146)
            .panelBackground(0xE84B5056)
            .panelBorder(0xFF717982)
            .label(0xFFDDE3E8)
            .heading(0xFFA6DCFF)
            .hint(0xFFB4BEC8)
            .success(0xFF9BE89B)
            .error(0xFFFFA0A0)
            .title(0xFFFFFFFF)
            .selected(0xFF66D8FF)
            .disabledText(0xFF858585)
            .scrim(0xA0000000)
            .dialogBackground(0xF0545960)
            .dialogTitle(0xFFFFD24D)
            .fieldBackground(0xFF303438)
            .fieldText(0xFFF0F4F8)
            .buttonBorder(0xFF7C858E)
            .buttonBorderHovered(0xFFD2DAE2)
            .buttonBorderDisabled(0xFF54595F)
            .buttonFill(0xFF5A6067)
            .buttonFillHovered(0xFF6A727A)
            .buttonFillDisabled(0xFF44494F)
            .buttonText(0xFFF0F2F4)
            .buttonTextDisabled(0xFFA0A0A0)
            .choiceSelected(0xFF3E6C7C)
            .build()),

    MID_LIGHT("MID LIGHT", new PaletteBuilder()
            .screenBackground(0xF0C9CDD1)
            .panelBackground(0xF2E0E3E6)
            .panelBorder(0xFF8D959D)
            .label(0xFF2A3138)
            .heading(0xFF176B86)
            .hint(0xFF4F5B65)
            .success(0xFF1E7A34)
            .error(0xFFB42318)
            .title(0xFF101418)
            .selected(0xFF00607C)
            .disabledText(0xFF606060)
            .scrim(0x99000000)
            .dialogBackground(0xFFEEF0F2)
            .dialogTitle(0xFF7A5000)
            .fieldBackground(0xFFFFFFFF)
            .fieldText(0xFF101418)
            .buttonBorder(0xFF8A929A)
            .buttonBorderHovered(0xFF404850)
            .buttonBorderDisabled(0xFFB2B6BA)
            .buttonFill(0xFFD2D6DA)
            .buttonFillHovered(0xFFC1C7CD)
            .buttonFillDisabled(0xFFCFCFCF)
            .buttonText(0xFF1A2026)
            .buttonTextDisabled(0xFF606060)
            .choiceSelected(0xFFAFD7E3)
            .build());
    private static UiEditorTheme current;

    private final String displayName;
    private final Palette palette;

    UiEditorTheme(String displayName, Palette palette) {
        this.displayName = displayName;
        this.palette = palette;
    }

    public String displayName() {
        return displayName;
    }

    public Palette palette() {
        return palette;
    }

    public UiEditorTheme next() {
        UiEditorTheme[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public void select() {
        current = this;
        try {
            PointBlankExtraConfig.UI_EDITOR_THEME.set(name());
            PointBlankExtraConfig.SPEC.save();
        } catch (RuntimeException ignored) {
            // A read-only or not-yet-loaded config must never break the editor.
        }
    }

    public static UiEditorTheme active() {
        if (current == null) {
            UiEditorTheme loaded = load();
            if (loaded != null) {
                current = loaded;
            }
            return loaded == null ? DEFAULT : loaded;
        }
        return current;
    }

    private static UiEditorTheme load() {
        try {
            String configured = PointBlankExtraConfig.UI_EDITOR_THEME.get();
            for (UiEditorTheme theme : values()) {
                if (theme.name().equalsIgnoreCase(configured)
                        || theme.displayName.equalsIgnoreCase(configured)) {
                    return theme;
                }
            }
            return DEFAULT;
        } catch (RuntimeException ignored) {
            // Do not cache DEFAULT while the config is still unavailable; retry later.
            return null;
        }
    }

    public record Palette(
            int screenBackground,
            int panelBackground,
            int panelBorder,
            int label,
            int heading,
            int hint,
            int success,
            int error,
            int title,
            int selected,
            int disabledText,
            int scrim,
            int dialogBackground,
            int dialogTitle,
            int fieldBackground,
            int fieldText,
            int buttonBorder,
            int buttonBorderHovered,
            int buttonBorderDisabled,
            int buttonFill,
            int buttonFillHovered,
            int buttonFillDisabled,
            int buttonText,
            int buttonTextDisabled,
            int choiceSelected) {
    }

    private static final class PaletteBuilder {
        private int screenBackground;
        private int panelBackground;
        private int panelBorder;
        private int label;
        private int heading;
        private int hint;
        private int success;
        private int error;
        private int title;
        private int selected;
        private int disabledText;
        private int scrim;
        private int dialogBackground;
        private int dialogTitle;
        private int fieldBackground;
        private int fieldText;
        private int buttonBorder;
        private int buttonBorderHovered;
        private int buttonBorderDisabled;
        private int buttonFill;
        private int buttonFillHovered;
        private int buttonFillDisabled;
        private int buttonText;
        private int buttonTextDisabled;
        private int choiceSelected;

        private Palette build() {
            return new Palette(screenBackground, panelBackground, panelBorder, label, heading,
                    hint, success, error, title, selected, disabledText, scrim, dialogBackground,
                    dialogTitle, fieldBackground, fieldText, buttonBorder, buttonBorderHovered,
                    buttonBorderDisabled, buttonFill, buttonFillHovered, buttonFillDisabled,
                    buttonText, buttonTextDisabled, choiceSelected);
        }

        private PaletteBuilder screenBackground(int color) {
            this.screenBackground = color;
            return this;
        }

        private PaletteBuilder panelBackground(int color) {
            this.panelBackground = color;
            return this;
        }

        private PaletteBuilder panelBorder(int color) {
            this.panelBorder = color;
            return this;
        }

        private PaletteBuilder label(int color) {
            this.label = color;
            return this;
        }

        private PaletteBuilder heading(int color) {
            this.heading = color;
            return this;
        }

        private PaletteBuilder hint(int color) {
            this.hint = color;
            return this;
        }

        private PaletteBuilder success(int color) {
            this.success = color;
            return this;
        }

        private PaletteBuilder error(int color) {
            this.error = color;
            return this;
        }

        private PaletteBuilder title(int color) {
            this.title = color;
            return this;
        }

        private PaletteBuilder selected(int color) {
            this.selected = color;
            return this;
        }

        private PaletteBuilder disabledText(int color) {
            this.disabledText = color;
            return this;
        }

        private PaletteBuilder scrim(int color) {
            this.scrim = color;
            return this;
        }

        private PaletteBuilder dialogBackground(int color) {
            this.dialogBackground = color;
            return this;
        }

        private PaletteBuilder dialogTitle(int color) {
            this.dialogTitle = color;
            return this;
        }

        private PaletteBuilder fieldBackground(int color) {
            this.fieldBackground = color;
            return this;
        }

        private PaletteBuilder fieldText(int color) {
            this.fieldText = color;
            return this;
        }

        private PaletteBuilder buttonBorder(int color) {
            this.buttonBorder = color;
            return this;
        }

        private PaletteBuilder buttonBorderHovered(int color) {
            this.buttonBorderHovered = color;
            return this;
        }

        private PaletteBuilder buttonBorderDisabled(int color) {
            this.buttonBorderDisabled = color;
            return this;
        }

        private PaletteBuilder buttonFill(int color) {
            this.buttonFill = color;
            return this;
        }

        private PaletteBuilder buttonFillHovered(int color) {
            this.buttonFillHovered = color;
            return this;
        }

        private PaletteBuilder buttonFillDisabled(int color) {
            this.buttonFillDisabled = color;
            return this;
        }

        private PaletteBuilder buttonText(int color) {
            this.buttonText = color;
            return this;
        }

        private PaletteBuilder buttonTextDisabled(int color) {
            this.buttonTextDisabled = color;
            return this;
        }

        private PaletteBuilder choiceSelected(int color) {
            this.choiceSelected = color;
            return this;
        }
    }
}
