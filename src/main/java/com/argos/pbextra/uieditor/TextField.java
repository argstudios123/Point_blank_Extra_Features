package com.argos.pbextra.uieditor;

/**
 * One editable property of a text block, together with its JSON key and value
 * type. The editor derives both the UI rows and the serialized JSON from these
 * entries, so the schema only has to be described once.
 */
public enum TextField {

    X(Type.INT, "x"),
    Y(Type.INT, "y"),
    SIZE(Type.FLOAT, "size"),
    COLOR(Type.COLOR, "color"),
    EMPTY_COLOR(Type.COLOR, "emptyColor"),
    FULL_COLOR(Type.COLOR, "fullColor"),
    BORDER(Type.BOOL, "border"),
    DROP_SHADOW(Type.BOOL, "dropShadow"),
    ITALIC(Type.BOOL, "italic"),
    BOLD(Type.BOOL, "bold"),
    ENABLED(Type.BOOL, "enabled"),
    DIGITAL_STYLE(Type.BOOL, "digitalStyle"),
    HIDE_ON_RELOAD(Type.BOOL, "hideOnReloadText"),
    SHOW_SLASH(Type.BOOL, "showSlash");

    public enum Type {
        INT,
        FLOAT,
        COLOR,
        BOOL
    }

    private final Type type;
    private final String jsonKey;

    TextField(Type type, String jsonKey) {
        this.type = type;
        this.jsonKey = jsonKey;
    }

    public Type type() {
        return type;
    }

    public String jsonKey() {
        return jsonKey;
    }
}
