package com.argos.pbextra.uieditor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.registry.AmmoUiDefinition;
import com.vicmatskiv.pointblank.registry.HeatUiDefinition;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;


public final class UiEditorModel {

    public static final String AMMO_TEXT = "ammoText";
    public static final String CAPACITY_TEXT = "capacityText";
    public static final String FIRE_MODE_TEXT = "fireModeText";
    public static final String PERCENTAGE_TEXT = "percentageText";

    private final UiDefinitionKind kind;
    private String id;
    private String name;
    private String texture = "textures/gui/ammo.png";
    private String backgroundTexture = "textures/gui/heat.png";
    private String progressTexture = "textures/gui/heat_progress.png";
    private String maskTexture = "textures/gui/heat_progress_mask.png";
    private String reloadText = "RELOADING...";
    private float inertia;
    private int width;
    private int height;
    private int paddingX;
    private int paddingY;
    private String location;
    private final Map<String, UiTextBlock> blocks = new LinkedHashMap<>();
    private JsonObject raw;
    private Path sourcePath;
    private boolean fromScratch;

    private UiEditorModel(UiDefinitionKind kind, String id) {
        this.kind = kind;
        this.id = id;
        this.name = id;
        if (kind == UiDefinitionKind.AMMO) {
            this.width = 48;
            this.height = 32;
            this.location = kind.normalizeLocation("bottom_right");

            UiTextBlock ammoText = new UiTextBlock(UiTextBlock.AMMO_TEXT);
            ammoText.setColor(0xFFFFFFFF);
            ammoText.setEmptyColor(0xFFFF0000);
            blocks.put(AMMO_TEXT, ammoText);

            UiTextBlock capacityText = new UiTextBlock(UiTextBlock.CAPACITY_TEXT);
            capacityText.setPresent(false);
            blocks.put(CAPACITY_TEXT, capacityText);

            UiTextBlock fireModeText = new UiTextBlock(UiTextBlock.FIRE_MODE_TEXT);
            fireModeText.setY(10);
            fireModeText.setSize(0.75f);
            blocks.put(FIRE_MODE_TEXT, fireModeText);
        } else {
            this.width = 49;
            this.height = 49;
            this.paddingX = 10;
            this.paddingY = 10;
            this.location = kind.normalizeLocation("BOTTOM_RIGHT");

            UiTextBlock percentageText = new UiTextBlock(UiTextBlock.PERCENTAGE_TEXT);
            percentageText.setX(-10);
            percentageText.setY(28);
            percentageText.setSize(1.1f);
            percentageText.setEmptyColor(0xFF8787EB);
            percentageText.setFullColor(0xFFFF0000);
            percentageText.setBoolValue(TextField.BORDER, true);
            blocks.put(PERCENTAGE_TEXT, percentageText);
        }
        this.fromScratch = true;
    }

    public static UiEditorModel createDefault(UiDefinitionKind kind, String id) {
        return new UiEditorModel(kind, id);
    }

    public UiDefinitionKind kind() {
        return kind;
    }

    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String texture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public String backgroundTexture() {
        return backgroundTexture;
    }

    public void setBackgroundTexture(String backgroundTexture) {
        this.backgroundTexture = backgroundTexture;
    }

    public String progressTexture() {
        return progressTexture;
    }

    public void setProgressTexture(String progressTexture) {
        this.progressTexture = progressTexture;
    }

    public String maskTexture() {
        return maskTexture;
    }

    public void setMaskTexture(String maskTexture) {
        this.maskTexture = maskTexture;
    }

    public String reloadText() {
        return reloadText;
    }

    public void setReloadText(String reloadText) {
        this.reloadText = reloadText;
    }

    public float inertia() {
        return inertia;
    }

    public void setInertia(float inertia) {
        this.inertia = Math.max(0.0f, inertia);
    }

    public int width() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int height() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int paddingX() {
        return paddingX;
    }

    public void setPaddingX(int paddingX) {
        this.paddingX = paddingX;
    }

    public int paddingY() {
        return paddingY;
    }

    public void setPaddingY(int paddingY) {
        this.paddingY = paddingY;
    }

    public String location() {
        return location;
    }

    public void setLocation(String location) {
        this.location = kind.normalizeLocation(location);
    }

    public UiTextBlock block(String key) {
        return blocks.get(key);
    }

    public Map<String, UiTextBlock> blocks() {
        return blocks;
    }

    public JsonObject raw() {
        return raw;
    }

    public Path sourcePath() {
        return sourcePath;
    }

    public boolean isFromScratch() {
        return fromScratch;
    }

    public String title() {
        return name + " (" + id + ")";
    }

    /* ------------------------------------------------------------------ */
    /* Factories                                                          */
    /* ------------------------------------------------------------------ */

    public static UiEditorModel fromJson(UiDefinitionKind kind, String id, JsonObject object, Path sourcePath) {
        UiEditorModel model = new UiEditorModel(kind, id);
        model.raw = object.deepCopy();
        model.sourcePath = sourcePath;
        model.fromScratch = false;
        model.name = getString(object, "name", id);
        model.readCommon(object);
        if (kind == UiDefinitionKind.AMMO) {
            model.readAmmo(object);
        } else {
            model.readHeat(object);
        }
        return model;
    }

    public static UiEditorModel fromAmmoDefinition(AmmoUiDefinition definition) {
        UiEditorModel model = new UiEditorModel(UiDefinitionKind.AMMO, definition.id());
        model.fromScratch = false;
        model.name = definition.name();
        model.texture = definition.texture().toString();
        model.reloadText = definition.reloadText();
        model.inertia = definition.inertia();
        model.width = definition.width();
        model.height = definition.height();
        model.paddingX = definition.paddingX();
        model.paddingY = definition.paddingY();
        model.location = model.kind().normalizeLocation(definition.screenLocation().name());
        model.applyAmmoText(model.block(AMMO_TEXT), definition.ammoText());
        if (definition.capacityText() != null) {
            model.block(CAPACITY_TEXT).setPresent(true);
            model.applyAmmoText(model.block(CAPACITY_TEXT), definition.capacityText());
        } else {
            model.block(CAPACITY_TEXT).setPresent(false);
        }
        if (definition.fireModeText() != null) {
            model.block(FIRE_MODE_TEXT).setPresent(true);
            model.applyAmmoText(model.block(FIRE_MODE_TEXT), definition.fireModeText());
        } else {
            model.block(FIRE_MODE_TEXT).setPresent(false);
        }
        return model;
    }

    public static UiEditorModel fromHeatDefinition(HeatUiDefinition definition) {
        UiEditorModel model = new UiEditorModel(UiDefinitionKind.HEAT, definition.id());
        model.fromScratch = false;
        model.name = definition.name();
        model.backgroundTexture = definition.backgroundTexture().toString();
        model.progressTexture = definition.progressTexture().toString();
        model.maskTexture = definition.maskTexture().toString();
        model.inertia = definition.inertia();
        model.width = definition.width();
        model.height = definition.height();
        model.paddingX = definition.paddingX();
        model.paddingY = definition.paddingY();
        model.location = model.kind().normalizeLocation(definition.screenLocation().name());
        HeatUiDefinition.PercentageTextDefinition percentage = definition.percentageText();
        if (percentage != null) {
            UiTextBlock block = model.block(PERCENTAGE_TEXT);
            block.setPresent(true);
            block.setX(percentage.x());
            block.setY(percentage.y());
            block.setSize(percentage.size());
            block.setEmptyColor(percentage.emptyColor());
            block.setFullColor(percentage.fullColor());
            block.setBoolValue(TextField.BORDER, percentage.border());
            block.setBoolValue(TextField.DROP_SHADOW, percentage.dropShadow());
            block.setBoolValue(TextField.ITALIC, percentage.italic());
            block.setBoolValue(TextField.BOLD, percentage.bold());
            block.setBoolValue(TextField.ENABLED, percentage.enabled());
        } else {
            UiTextBlock block = model.block(PERCENTAGE_TEXT);
            block.setPresent(false);
            block.setBoolValue(TextField.ENABLED, false);
        }
        return model;
    }

    /* ------------------------------------------------------------------ */
    /* Reading                                                            */
    /* ------------------------------------------------------------------ */

    private void readCommon(JsonObject object) {
        JsonObject size = getObject(object, "size");
        if (size != null) {
            this.width = Math.max(1, getInt(size, "width", this.width));
            this.height = Math.max(1, getInt(size, "height", this.height));
        }
        JsonObject padding = getObject(object, "padding");
        if (padding != null) {
            this.paddingX = getInt(padding, "x", this.paddingX);
            this.paddingY = getInt(padding, "y", this.paddingY);
        }
        this.inertia = Math.max(0.0f, getFloat(object, "inertia", this.inertia));
        this.location = kind.normalizeLocation(getString(object, "location", this.location));
    }

    private void readAmmo(JsonObject object) {
        this.texture = getString(object, "texture", this.texture);
        this.reloadText = getString(object, "reloadText", this.reloadText);

        JsonObject ammoText = getObject(object, "ammoText");
        if (ammoText != null) {
            readBlock(block(AMMO_TEXT), ammoText);
        }

        JsonObject capacityText = getObject(object, "capacityText");
        UiTextBlock capacity = block(CAPACITY_TEXT);
        if (capacityText != null) {
            capacity.setPresent(true);
            readBlock(capacity, capacityText);
        } else {
            capacity.setPresent(false);
        }

        JsonObject fireModeText = getObject(object, "fireModeText");
        UiTextBlock fireMode = block(FIRE_MODE_TEXT);
        if (fireModeText != null) {
            fireMode.setPresent(true);
            readBlock(fireMode, fireModeText);
        } else {
            // Point Blank derives the fire mode text from the ammo text when the
            // object is absent - the editor mirrors that so the preview matches.
            fireMode.setPresent(false);
            fireMode.setX(block(AMMO_TEXT).x());
            fireMode.setY(block(AMMO_TEXT).y() + 10);
            fireMode.setSize(Math.max(0.1f, block(AMMO_TEXT).size() * 0.75f));
            fireMode.setColor(0xFFFFFFFF);
            fireMode.setBoolValue(TextField.ENABLED, true);
        }
    }

    private void readHeat(JsonObject object) {
        JsonObject textures = getObject(object, "textures");
        if (textures != null) {
            this.backgroundTexture = getString(textures, "background", this.backgroundTexture);
            this.progressTexture = getString(textures, "progress", this.progressTexture);
            this.maskTexture = getString(textures, "mask", this.maskTexture);
        }
        JsonObject percentageText = getObject(object, "percentageText");
        if (percentageText != null) {
            block(PERCENTAGE_TEXT).setPresent(true);
            readBlock(block(PERCENTAGE_TEXT), percentageText);
        } else {
            // Point Blank tolerates a heat UI without a percentage text. Keep it
            // absent (and disabled) so round-tripping such a file never starts
            // showing a text that was not there before.
            UiTextBlock block = block(PERCENTAGE_TEXT);
            block.setPresent(false);
            block.setBoolValue(TextField.ENABLED, false);
        }
    }

    private void readBlock(UiTextBlock block, JsonObject object) {
        for (TextField field : block.fields()) {
            switch (field.type()) {
                case INT -> block.setNumberValue(field,
                        getInt(object, field.jsonKey(), (int) block.numberValue(field)));
                case FLOAT -> block.setNumberValue(field,
                        getFloat(object, field.jsonKey(), (float) block.numberValue(field)));
                case COLOR -> block.setColorValue(field,
                        UiColors.parse(object.get(field.jsonKey()), block.colorValue(field)));
                case BOOL -> block.setBoolValue(field,
                        getBoolean(object, field.jsonKey(), block.boolValue(field)));
            }
        }
    }

    private void applyAmmoText(UiTextBlock block, AmmoUiDefinition.TextDefinition definition) {
        block.setX(definition.x());
        block.setY(definition.y());
        block.setSize(definition.size());
        block.setColor(definition.color());
        block.setEmptyColor(definition.emptyColor());
        block.setBoolValue(TextField.BORDER, definition.border());
        block.setBoolValue(TextField.DROP_SHADOW, definition.dropShadow());
        block.setBoolValue(TextField.ITALIC, definition.italic());
        block.setBoolValue(TextField.BOLD, definition.bold());
        block.setBoolValue(TextField.ENABLED, definition.enabled());
        block.setBoolValue(TextField.DIGITAL_STYLE, definition.digitalStyle());
        block.setBoolValue(TextField.HIDE_ON_RELOAD, definition.hideOnReloadText());
        block.setBoolValue(TextField.SHOW_SLASH, definition.showSlash());
    }

    /* ------------------------------------------------------------------ */
    /* Serialization                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Writes this model back into the Point Blank JSON schema. When the model
     * originated from a file the original object is used as the base, so keys
     * the editor does not know about survive the round trip.
     */
    public JsonObject toJson() {
        JsonObject out = raw != null ? raw.deepCopy() : new JsonObject();
        out.addProperty("name", name);
        JsonObject size = childObject(out, "size");
        size.addProperty("width", width);
        size.addProperty("height", height);
        JsonObject padding = childObject(out, "padding");
        padding.addProperty("x", paddingX);
        padding.addProperty("y", paddingY);
        out.addProperty("inertia", inertia);
        out.addProperty("location", location);

        if (kind == UiDefinitionKind.AMMO) {
            out.addProperty("texture", texture);
            out.addProperty("reloadText", reloadText);
            out.add(AMMO_TEXT, blockJson(block(AMMO_TEXT)));
            writeOptionalBlock(out, CAPACITY_TEXT);
            writeOptionalBlock(out, FIRE_MODE_TEXT);
        } else {
            JsonObject textures = childObject(out, "textures");
            textures.addProperty("background", backgroundTexture);
            textures.addProperty("progress", progressTexture);
            textures.addProperty("mask", maskTexture);
            writeOptionalBlock(out, PERCENTAGE_TEXT);
        }
        return out;
    }

    public void markSaved(Path path) {
        this.sourcePath = path;
        this.fromScratch = false;
    }

    private void writeOptionalBlock(JsonObject out, String key) {
        UiTextBlock block = block(key);
        if (block != null && block.present()) {
            out.add(key, blockJson(block));
        } else {
            out.remove(key);
        }
    }

    private static JsonObject blockJson(UiTextBlock block) {
        JsonObject object = new JsonObject();
        for (TextField field : TextField.values()) {
            if (!block.supports(field)) {
                continue;
            }
            switch (field.type()) {
                case INT -> object.addProperty(field.jsonKey(), (int) Math.round(block.numberValue(field)));
                case FLOAT -> object.addProperty(field.jsonKey(), (float) block.numberValue(field));
                case COLOR -> object.add(field.jsonKey(), UiColors.toJson(block.colorValue(field)));
                case BOOL -> object.addProperty(field.jsonKey(), block.boolValue(field));
            }
        }
        return object;
    }

    private static JsonObject childObject(JsonObject parent, String key) {
        if (parent.has(key) && parent.get(key).isJsonObject()) {
            return parent.getAsJsonObject(key);
        }
        JsonObject child = new JsonObject();
        parent.add(key, child);
        return child;
    }

    /* ------------------------------------------------------------------ */
    /* JSON helpers                                                       */
    /* ------------------------------------------------------------------ */

    private static JsonObject getObject(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonObject() ? object.getAsJsonObject(key) : null;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if (!object.has(key)) {
            return fallback;
        }
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        try {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static float getFloat(JsonObject object, String key, float fallback) {
        try {
            return object.has(key) ? object.get(key).getAsFloat() : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        try {
            return object.has(key) ? object.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
