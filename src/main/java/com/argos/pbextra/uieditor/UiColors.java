package com.argos.pbextra.uieditor;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

/**
 * Color helpers that mirror Point Blank's
 * {@code AmmoUiDefinition#parseColor} / {@code HeatUiDefinition#parseColor}
 * behavior (numbers are accepted as-is, strings are read as hex and get an
 * opaque alpha channel when only 6 digits are given).
 */
public final class UiColors {

    private UiColors() {
    }

    /**
     * @return the parsed ARGB color, or {@code fallback} when the text is not a
     *         valid color
     */
    public static int parse(String text, int fallback) {
        if (text == null) {
            return fallback;
        }
        String value = text.trim();
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            if (!value.startsWith("#") && (value.startsWith("0x") || value.startsWith("0X"))) {
                value = value.substring(2);
            } else if (value.startsWith("#")) {
                value = value.substring(1);
            }
            long parsed = Long.parseLong(value, 16);
            if (value.length() <= 6) {
                parsed |= 0xFF000000L;
            }
            return (int) parsed;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static int parse(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            int value = element.getAsInt();
            return (value & 0xFF000000) == 0 ? value | 0xFF000000 : value;
        }
        return parse(element.getAsString(), fallback);
    }

    public static String format(int argb) {
        return String.format(Locale.ROOT, "#%08x", argb);
    }

    public static JsonPrimitive toJson(int argb) {
        return new JsonPrimitive(format(argb));
    }
}
