package com.argos.pbextra.client.gui;

import com.argos.pbextra.client.UiConfigRepository;
import com.argos.pbextra.client.UiPreviewLayout;
import com.argos.pbextra.client.UiPreviewRenderer;
import com.argos.pbextra.client.UiPreviewState;
import com.argos.pbextra.client.UiPreviewViewport;
import com.argos.pbextra.uieditor.GeneratedUiStore;
import com.argos.pbextra.uieditor.TextField;
import com.argos.pbextra.uieditor.UiColors;
import com.argos.pbextra.uieditor.UiDefinitionIndex;
import com.argos.pbextra.uieditor.UiDefinitionKind;
import com.argos.pbextra.uieditor.UiEditorModel;
import com.argos.pbextra.uieditor.UiTextBlock;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

// This Classs will the main UI editor Screnn for every single thing
public final class UiEditorScreen extends Screen {

    private static final int ROW_HEIGHT = 16;
    private static final int WIDGET_HEIGHT = 14;
    private static final int SCROLL_TOP = 26;
    private static final int FOOTER_HEIGHT = 40;
    private static final int PANEL_GAP = 6;
    private static final int NUM_WIDTH = 52;
    private static final int HEX_WIDTH = 34;
    private static final int SWATCH_WIDTH = 18;
    private static final int TOGGLE_WIDTH = 38;
    private static final int CYCLE_WIDTH = 96;

    private static final int LABEL_COLOR = 0xFFC2CCD6;
    private static final int SECTION_COLOR = 0xFF7FD8FF;
    private static final int HINT_COLOR = 0xFF90A0B0;
    private static final int OK_COLOR = 0xFF7CE07C;
    private static final int ERROR_COLOR = 0xFFFF8080;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int SELECTED_COLOR = 0xFF33D6FF;
    private static final int PANEL_BACKGROUND = 0xE6101418;
    private static final int PANEL_BORDER = 0xFF2E3841;

    // Width of the label column of the free text rows (name, texture, reload).
    private static final int TEXT_LABEL_WIDTH = 62;
    private static final int NEW_BUTTON_WIDTH = 44;
    // Modal "create new configuration" dialog.
    private static final int NEW_DIALOG_WIDTH = 244;
    private static final int NEW_DIALOG_HEIGHT = 112;
    private static final int NEW_DIALOG_CHOICE_HEIGHT = 16;
    // Modal "search definitions" dialog, shared by the Ammo and Heat editors.
    private static final int SEARCH_DIALOG_WIDTH = 344;
    private static final int SEARCH_DIALOG_HEIGHT = 196;
    private static final int SEARCH_VISIBLE_ROWS = 6;
    private static final int SEARCH_ROW_HEIGHT = 15;
    private static final int DIALOG_SCRIM = 0x99000000;
    private static final int DIALOG_BACKGROUND = 0xF0181C20;
    private static final int DIALOG_BORDER = 0xFFB0C0D0;
    private static final int DIALOG_FIELD = 0xFF000000;
    private static final int DIALOG_FIELD_BORDER = 0xFF505860;
    private static final int DIALOG_CHOICE = 0xFF262B30;
    private static final int DIALOG_CHOICE_HOVERED = 0xFF3A4249;
    private static final int DIALOG_CHOICE_SELECTED = 0xFF2E5C6E;
    private static final int DIALOG_TITLE = 0xFFFFD24D;

    // One row of the property panel: a widget, an optional label, or a header.
    private static final class Entry {
        final AbstractWidget widget;
        final int baseY;
        final boolean scrolls;
        final String label;
        final Runnable refresh;
        final boolean header;

        Entry(AbstractWidget widget, int baseY, boolean scrolls, String label, Runnable refresh,
              boolean header) {
            this.widget = widget;
            this.baseY = baseY;
            this.scrolls = scrolls;
            this.label = label;
            this.refresh = refresh;
            this.header = header;
        }
    }

    private UiDefinitionKind kind;
    private String requestedId;

    private UiEditorModel model;
    private final UiPreviewState state = new UiPreviewState();
    private final UiColorPicker colorPicker = new UiColorPicker();

    private UiPreviewViewport viewport;
    private int panelLeft;
    private int panelWidth;
    private int canvasLeft;
    private int canvasTop;
    private int canvasRight;
    private int canvasBottom;
    private int scrollBottom;

    private final List<Entry> entries = new ArrayList<>();
    private EditBox focusedBox;
    private boolean updatingControls;
    private boolean rebuildQueued;
    private int panelScroll;
    private int panelContentHeight;
    private int cursorY;

    private String selectedKey;
    private JsonObject sessionBaseline;
    private String sessionId;
    private boolean overwritePending;
    private String statusMessage = "";
    private int statusColor = HINT_COLOR;


    private boolean newDialogOpen;
    private UiDefinitionKind newDialogKind = UiDefinitionKind.AMMO;
    private String newDialogName = "";

    // True while the session's definition has never been written to disk.
    private boolean sessionFromScratch;
    // Content pack the session was opened from, when it was opened through Search.
    private String sessionSourceName;
    // Action that is waiting for a second click because the session is dirty.
    private String pendingDiscardAction;

    // The modal "search definitions" dialog (see {@link #renderSearchDialog}).
    private boolean searchOpen;
    private String searchQuery = "";
    private List<UiDefinitionIndex.Entry> searchEntries = List.of();
    private List<UiDefinitionIndex.Entry> searchResults = List.of();
    private int searchSelected;
    private int searchScroll;

    private boolean dragging;
    private String dragKey;
    private int dragStartX;
    private int dragStartY;
    private float dragGrabX;
    private float dragGrabY;
    private boolean dragMirrored;

    public UiEditorScreen(UiDefinitionKind kind, String requestedId) {
        super(Component.literal((kind == null ? "UI" : kind.displayName()) + " Editor"));
        this.kind = kind == null ? UiDefinitionKind.AMMO : kind;
        this.requestedId = requestedId;
    }

    /** Entry point used by {@code OpenUiEditorPacket}; runs on the client thread. */
    public static void open(String kindId, String definitionId) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new UiEditorScreen(UiDefinitionKind.byId(kindId), definitionId));
    }


    @Override
    protected void init() {
        super.init();
        if (model == null) {
            loadSession(kind, requestedId);
        }
        recomputeGeometry();
        rebuildControls();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    private void loadSession(UiDefinitionKind nextKind, String requested) {
        this.kind = nextKind == null ? UiDefinitionKind.AMMO : nextKind;
        beginSession(UiConfigRepository.open(this.kind, requested));
    }


    private void beginSession(UiEditorModel newModel) {
        beginSession(newModel, null);
    }


    private void beginSession(UiEditorModel newModel, String sourceName) {
        this.kind = newModel.kind();
        this.model = newModel;
        this.requestedId = newModel.id();
        this.sessionId = newModel.id();
        this.sessionBaseline = newModel.toJson().deepCopy();
        this.overwritePending = false;
        this.pendingDiscardAction = null;
        this.sessionFromScratch = newModel.isFromScratch();
        this.sessionSourceName = sourceName;
        this.selectedKey = defaultSelection(newModel);
        if (newModel.isFromScratch()) {
            status("New definition - press Save to create " + sessionId + ".json", HINT_COLOR);
        } else if (sourceName != null) {
            status("Opened '" + sessionId + "' from " + sourceName
                    + " - Save writes a generated override, the pack file is never touched.", HINT_COLOR);
        } else if (UiConfigRepository.hasOverride(this.kind, sessionId)) {
            status("Editing the generated override.", HINT_COLOR);
        } else {
            status("Editing the content pack definition (Save writes a copy).", HINT_COLOR);
        }
    }

    private void switchKind(UiDefinitionKind next) {
        if (next == null || next == this.kind) {
            return;
        }
        if (!confirmDiscard("switch:" + next.id(), "switching tabs")) {
            return;
        }
        loadSession(next, UiConfigRepository.defaultId(next));
        queueRebuild();
    }


    private boolean confirmDiscard(String action, String what) {
        if (!hasUnsavedChanges()) {
            pendingDiscardAction = null;
            return true;
        }
        if (action.equals(pendingDiscardAction)) {
            pendingDiscardAction = null;
            return true;
        }
        pendingDiscardAction = action;
        status("'" + model.id() + "' has unsaved changes - repeat " + what
                + " to discard them, or press Save first.", ERROR_COLOR);
        return false;
    }

    private boolean hasUnsavedChanges() {
        return sessionBaseline != null && model != null
                && !model.toJson().equals(sessionBaseline);
    }


    private void recomputeGeometry() {
        panelWidth = Mth.clamp(width / 3, 200, 252);
        panelLeft = width - panelWidth;
        canvasLeft = 4;
        canvasTop = SCROLL_TOP;
        canvasRight = panelLeft - PANEL_GAP;
        canvasBottom = height - 6;
        scrollBottom = height - FOOTER_HEIGHT;

        if (viewport == null || viewport.screenWidth() != width || viewport.screenHeight() != height) {
            viewport = new UiPreviewViewport(width, height);
        }
        fitViewport();
    }

    private void fitViewport() {
        viewport.fit(canvasLeft, canvasTop, Math.max(1, canvasRight - canvasLeft),
                Math.max(1, canvasBottom - canvasTop));
    }

    private UiPreviewLayout layout() {
        return new UiPreviewLayout(model, state, width, height, 0.0f, 0.0f);
    }

    private boolean inPreview(double mouseX, double mouseY) {
        return mouseX >= canvasLeft && mouseX <= canvasRight
                && mouseY >= canvasTop && mouseY <= canvasBottom;
    }

    private int panelContentLeft() {
        return panelLeft + 6;
    }

    private int panelContentRight() {
        return width - 6;
    }

    private int panelInnerWidth() {
        return panelContentRight() - panelContentLeft();
    }

    private String titleText() {
        return model.kind().displayName() + "  -  " + model.id()
                + (model.isFromScratch() ? "  [new]" : "");
    }


    private void rebuildControls() {
        entries.clear();
        focusedBox = null;
        cursorY = 0;

        // --- Configuration
        section("CONFIGURATION");
        int configRow = cursorY;
        int openWidth = 46;
        int openX = panelContentRight() - openWidth;
        int idWidth = Math.max(40, openX - 4 - panelContentLeft());
        EditBox idBox = new EditBox(font, panelContentLeft(), 0, idWidth,
                WIDGET_HEIGHT, Component.empty());
        idBox.setMaxLength(128);
        idBox.setValue(sessionId == null ? "" : sessionId);
        addEntry(idBox, configRow, true, null, null);
        FlatButton open = new FlatButton(font, openWidth, WIDGET_HEIGHT, "Open",
                () -> requestOpen(idBox.getValue()));
        open.setX(openX);
        addEntry(open, configRow, true, null, null);
        nextRow();

        int dialogRow = cursorY;
        FlatButton create = new FlatButton(font, NEW_BUTTON_WIDTH, WIDGET_HEIGHT, "New",
                this::openNewDialog);
        create.setX(panelContentLeft());
        create.textColor(SELECTED_COLOR);
        addEntry(create, dialogRow, true, null, null);
        int searchWidth = Math.max(56, panelInnerWidth() - NEW_BUTTON_WIDTH - 4);
        FlatButton search = new FlatButton(font, searchWidth, WIDGET_HEIGHT, "Search...",
                this::openSearchDialog);
        search.setX(panelContentLeft() + NEW_BUTTON_WIDTH + 4);
        search.textColor(SELECTED_COLOR);
        addEntry(search, dialogRow, true, null, null);
        nextRow();
        addTextRow("Name", model::name, model::setName);
        addReadOnly("Source: " + sourceLabel());
        addReadOnly("Save to: " + targetFileLabel());

        // --- Textures
        section("TEXTURES");
        if (kind == UiDefinitionKind.AMMO) {
            addTextRow("Texture", model::texture, model::setTexture);
        } else {
            addTextRow("Background", model::backgroundTexture, model::setBackgroundTexture);
            addTextRow("Progress", model::progressTexture, model::setProgressTexture);
            addTextRow("Mask", model::maskTexture, model::setMaskTexture);
        }

        // --- Layout
        section("LAYOUT");
        locationRow();
        addNumberRow("Width", () -> model.width(), value -> model.setWidth((int) Math.round(value)), true);
        addNumberRow("Height", () -> model.height(), value -> model.setHeight((int) Math.round(value)), true);
        addNumberRow("Inertia", () -> model.inertia(), value -> model.setInertia((float) value), false);
        addNumberRow("Padding X", () -> model.paddingX(), value -> model.setPaddingX((int) Math.round(value)), true);
        addNumberRow("Padding Y", () -> model.paddingY(), value -> model.setPaddingY((int) Math.round(value)), true);
        if (kind == UiDefinitionKind.AMMO) {
            addTextRow("Reload Text", model::reloadText, model::setReloadText);
        }

        // --- Elements
        section("ELEMENTS");
        blockSelector();
        optionalElementToggles();

        // --- Selected element
        section("ELEMENT");
        UiTextBlock block = selectedBlock();
        if (block == null) {
            addReadOnly("(select an element above)");
        } else {
            for (TextField field : TextField.values()) {
                if (block.supports(field)) {
                    fieldRow(block, field);
                }
            }
        }

        panelContentHeight = cursorY;

        // --- Fixed chrome (title tabs + footer)
        int tabWidth = (panelWidth - 8) / 2;
        FlatButton ammoTab = new FlatButton(font, tabWidth, 16, "Ammo UI",
                () -> switchKind(UiDefinitionKind.AMMO));
        ammoTab.setX(panelLeft + 2);
        ammoTab.textColor(kind == UiDefinitionKind.AMMO ? SELECTED_COLOR : FlatButton.DEFAULT_TEXT_COLOR);
        addEntry(ammoTab, 4, false, null, null);
        FlatButton heatTab = new FlatButton(font, tabWidth, 16, "Heat UI",
                () -> switchKind(UiDefinitionKind.HEAT));
        heatTab.setX(panelLeft + 6 + tabWidth);
        heatTab.textColor(kind == UiDefinitionKind.HEAT ? SELECTED_COLOR : FlatButton.DEFAULT_TEXT_COLOR);
        addEntry(heatTab, 4, false, null, null);

        int footerButtonWidth = (panelInnerWidth() - 8) / 3;
        int footerY = height - 18;
        FlatButton save = new FlatButton(font, footerButtonWidth, 16, "Save", this::save);
        save.setX(panelContentLeft());
        save.textColor(OK_COLOR);
        addEntry(save, footerY, false, null, null);
        FlatButton reset = new FlatButton(font, footerButtonWidth, 16, "Reset", this::reset);
        reset.setX(panelContentLeft() + footerButtonWidth + 4);
        addEntry(reset, footerY, false, null, null);
        FlatButton cancel = new FlatButton(font, footerButtonWidth, 16, "Cancel", this::cancel);
        cancel.setX(panelContentLeft() + 2 * footerButtonWidth + 8);
        addEntry(cancel, footerY, false, null, null);

        // Every row exists now, so the scroll offset can be applied to all of them
        // (scrolled rows get shifted, fixed chrome keeps its base position).
        applyScroll();
    }

    private String sourceLabel() {
        if (sessionFromScratch || model.isFromScratch()) {
            return "new file";
        }
        if (sessionSourceName != null && !sessionSourceName.isBlank()) {
            return sessionSourceName;
        }
        return UiConfigRepository.hasOverride(kind, model.id())
                ? "generated override" : "content pack";
    }

    // The file a save would write, relative to the game directory.
    private String targetFileLabel() {
        String id = model.id();
        if (id == null || id.isBlank()) {
            return "(no id)";
        }
        return GeneratedUiStore.DIRECTORY_NAME + "/"
                + (kind == UiDefinitionKind.HEAT ? "heat/" : "") + id + ".json";
    }

    private void requestOpen(String idText) {
        String requested = idText == null ? "" : idText.trim();
        if (!confirmDiscard("open:" + requested, "Open")) {
            return;
        }
        loadSession(kind, requested);
        queueRebuild();
    }



    private int cursor() {
        return cursorY;
    }

    private void nextRow() {
        cursorY += ROW_HEIGHT;
    }

    private void section(String title) {
        entries.add(new Entry(null, cursorY, true, title, null, true));
        nextRow();
    }

    private void addReadOnly(String text) {
        entries.add(new Entry(null, cursorY, true, text, null, false));
        nextRow();
    }

    private void addEntry(AbstractWidget widget, int baseY, boolean scrolls, String label, Runnable refresh) {
        entries.add(new Entry(widget, baseY, scrolls, label, refresh, false));
    }

    private void applyScroll() {
        int visible = Math.max(1, scrollBottom - SCROLL_TOP);
        int maxScroll = Math.max(0, panelContentHeight - visible);
        panelScroll = Mth.clamp(panelScroll, 0, maxScroll);
        for (Entry entry : entries) {
            if (entry.widget == null) {
                continue;
            }
            entry.widget.setY(entry.scrolls ? SCROLL_TOP + entry.baseY - panelScroll : entry.baseY);
        }
    }

    private int entryY(Entry entry) {
        return entry.scrolls ? SCROLL_TOP + entry.baseY - panelScroll : entry.baseY;
    }

    private boolean visible(Entry entry) {
        if (!entry.scrolls) {
            return true;
        }
        int y = entryY(entry);
        return y + ROW_HEIGHT > SCROLL_TOP && y < scrollBottom;
    }

    private void refreshControlValues() {
        updatingControls = true;
        try {
            for (Entry entry : entries) {
                if (entry.refresh != null) {
                    entry.refresh.run();
                }
            }
        } finally {
            updatingControls = false;
        }
    }

    private void focusBox(EditBox box) {
        for (Entry entry : entries) {
            if (entry.widget instanceof EditBox editBox) {
                editBox.setFocused(editBox == box);
            }
        }
        this.focusedBox = box;
    }

    private void queueRebuild() {
        rebuildQueued = true;
    }

    private void flushRebuild() {
        if (rebuildQueued) {
            rebuildQueued = false;
            recomputeGeometry();
            rebuildControls();
        }
    }

    private void changed() {
        overwritePending = false;
        pendingDiscardAction = null;
    }

    private void status(String message, int color) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = color;
    }


    private void locationRow() {
        int row = cursor();
        FlatButton cycle = new FlatButton(font, CYCLE_WIDTH, WIDGET_HEIGHT, model.location(), () -> {
            model.setLocation(kind.nextLocation(model.location()));
            changed();
            queueRebuild();
        });
        cycle.setX(panelContentRight() - CYCLE_WIDTH);
        addEntry(cycle, row, true, "Location", null);
        nextRow();
    }

    private void addNumberRow(String label, DoubleSupplier getter, DoubleConsumer setter, boolean integer) {
        EditBox box = new EditBox(font, panelContentRight() - NUM_WIDTH, 0, NUM_WIDTH,
                WIDGET_HEIGHT, Component.empty());
        box.setMaxLength(12);
        box.setValue(formatNumber(getter.getAsDouble(), integer));
        box.setResponder(text -> {
            if (updatingControls) {
                return;
            }
            Double parsed = parseNumber(text);
            if (parsed == null) {
                return;
            }
            setter.accept(parsed);
            changed();
        });
        Runnable refresh = () -> {
            String value = formatNumber(getter.getAsDouble(), integer);
            if (!value.equals(box.getValue())) {
                box.setValue(value);
            }
        };
        addEntry(box, cursor(), true, label, refresh);
        nextRow();
    }


    private void addTextRow(String label, Supplier<String> getter, Consumer<String> setter) {
        int boxWidth = Math.max(40, panelInnerWidth() - TEXT_LABEL_WIDTH - 2);
        EditBox box = new EditBox(font, panelContentLeft() + TEXT_LABEL_WIDTH, 0, boxWidth,
                WIDGET_HEIGHT, Component.empty());
        box.setMaxLength(256);
        String initial = getter.get();
        box.setValue(initial == null ? "" : initial);
        box.setResponder(text -> {
            if (updatingControls) {
                return;
            }
            setter.accept(text);
            changed();
        });
        Runnable refresh = () -> {
            if (box.isFocused()) {
                return;
            }
            String value = getter.get();
            if (value == null) {
                value = "";
            }
            if (!value.equals(box.getValue())) {
                box.setValue(value);
            }
        };
        addEntry(box, cursor(), true, label, refresh);
        nextRow();
    }

    private void addToggleRow(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        ToggleWidget toggle = new ToggleWidget(font, TOGGLE_WIDTH, WIDGET_HEIGHT, getter, value -> {
            setter.accept(value);
            changed();
        });
        toggle.setX(panelContentRight() - TOGGLE_WIDTH);
        addEntry(toggle, cursor(), true, label, null);
        nextRow();
    }

    private void addColorRow(String label, Supplier<Integer> getter, Consumer<Integer> setter) {
        int row = cursor();
        int boxX = panelContentRight() - SWATCH_WIDTH - 2 - HEX_WIDTH;
        EditBox box = new EditBox(font, boxX, 0, HEX_WIDTH, WIDGET_HEIGHT, Component.empty());
        box.setMaxLength(9);
        box.setValue(UiColors.format(colorOf(getter)));
        box.setResponder(text -> {
            if (updatingControls) {
                return;
            }
            setter.accept(UiColors.parse(text, colorOf(getter)));
            changed();
        });
        ColorSwatchWidget swatch = new ColorSwatchWidget(font, SWATCH_WIDTH, WIDGET_HEIGHT,
                () -> colorOf(getter), () -> colorPicker.open(width, height, label,
                        () -> colorOf(getter), value -> {
                            setter.accept(value);
                            updatingControls = true;
                            box.setValue(UiColors.format(value));
                            updatingControls = false;
                            changed();
                        }));
        swatch.setX(panelContentRight() - SWATCH_WIDTH);
        Runnable refresh = () -> {
            String text = UiColors.format(colorOf(getter));
            if (!text.equals(box.getValue())) {
                box.setValue(text);
            }
        };
        addEntry(swatch, row, true, null, null);
        addEntry(box, row, true, label, refresh);
        nextRow();
    }

    private void fieldRow(UiTextBlock block, TextField field) {
        String label = fieldLabel(field);
        switch (field.type()) {
            case INT -> addNumberRow(label, () -> block.numberValue(field),
                    value -> { block.setNumberValue(field, value); changed(); }, true);
            case FLOAT -> addNumberRow(label, () -> block.numberValue(field),
                    value -> { block.setNumberValue(field, value); changed(); }, false);
            case BOOL -> addToggleRow(label, () -> block.boolValue(field),
                    value -> { block.setBoolValue(field, value); changed(); });
            case COLOR -> addColorRow(label, () -> block.colorValue(field),
                    value -> { block.setColorValue(field, value); changed(); });
        }
    }

    private void blockSelector() {
        List<String> keys = presentBlockKeys();
        if (keys.isEmpty()) {
            addReadOnly("(no editable element)");
            return;
        }
        int gap = 2;
        int widthPerButton = Math.max(30, (panelInnerWidth() - gap * (keys.size() - 1)) / keys.size());
        int x = panelContentLeft();
        int row = cursor();
        for (String key : keys) {
            FlatButton button = new FlatButton(font, widthPerButton, WIDGET_HEIGHT, key,
                    () -> selectKey(key));
            button.setX(x);
            button.textColor(key.equals(selectedKey) ? SELECTED_COLOR : FlatButton.DEFAULT_TEXT_COLOR);
            addEntry(button, row, true, null, null);
            x += widthPerButton + gap;
        }
        nextRow();
    }

    private void selectKey(String key) {
        this.selectedKey = key;
        queueRebuild();
    }


    private void optionalElementToggles() {
        if (kind == UiDefinitionKind.AMMO) {
            addPresenceRow("Capacity Text", UiEditorModel.CAPACITY_TEXT);
            addPresenceRow("Fire Mode Text", UiEditorModel.FIRE_MODE_TEXT);
        } else {
            addPresenceRow("Percentage Text", UiEditorModel.PERCENTAGE_TEXT);
        }
    }

    private void addPresenceRow(String label, String key) {
        // The block is looked up per call so a stale button can never write into a
        // model that was already replaced by an Open/Create/Reset.
        ToggleWidget toggle = new ToggleWidget(font, TOGGLE_WIDTH, WIDGET_HEIGHT,
                () -> {
                    UiTextBlock block = model.block(key);
                    return block != null && block.present();
                },
                value -> {
                    UiTextBlock block = model.block(key);
                    if (block == null) {
                        return;
                    }
                    block.setPresent(value);
                    if (value) {
                        selectedKey = key;
                    } else if (key.equals(selectedKey)) {
                        selectedKey = defaultSelection(model);
                    }
                    changed();
                    queueRebuild();
                });
        toggle.setX(panelContentRight() - TOGGLE_WIDTH);
        addEntry(toggle, cursor(), true, label, null);
        nextRow();
    }

    private static String fieldLabel(TextField field) {
        return switch (field) {
            case X -> "X";
            case Y -> "Y";
            case SIZE -> "Size";
            case COLOR -> "Color";
            case EMPTY_COLOR -> "Empty Color";
            case FULL_COLOR -> "Full Color";
            case BORDER -> "Border";
            case DROP_SHADOW -> "Drop Shadow";
            case ITALIC -> "Italic";
            case BOLD -> "Bold";
            case ENABLED -> "Enabled";
            case DIGITAL_STYLE -> "Digital Style";
            case HIDE_ON_RELOAD -> "Hide On Reload";
            case SHOW_SLASH -> "Show Slash";
        };
    }


    private static int colorOf(Supplier<Integer> getter) {
        Integer value = getter.get();
        return value == null ? 0xFFFFFFFF : value;
    }

    private List<String> presentBlockKeys() {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, UiTextBlock> entry : model.blocks().entrySet()) {
            if (entry.getValue() != null && entry.getValue().present()) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    private UiTextBlock selectedBlock() {
        if (selectedKey == null) {
            return null;
        }
        UiTextBlock block = model.block(selectedKey);
        return block != null && block.present() ? block : null;
    }

    private static String defaultSelection(UiEditorModel model) {
        return model.kind() == UiDefinitionKind.HEAT
                ? UiEditorModel.PERCENTAGE_TEXT : UiEditorModel.AMMO_TEXT;
    }

    private static String formatNumber(double value, boolean integer) {
        if (integer) {
            return Integer.toString((int) Math.round(value));
        }
        float number = (float) value;
        if (number == Math.rint(number)) {
            return Integer.toString((int) number);
        }
        return Float.toString(number);
    }

    private static Double parseNumber(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
        // Here comes the rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        UiPreviewRenderer.render(graphics, font, layout(), viewport, canvasLeft, canvasTop,
                canvasRight, canvasBottom, true, selectedKey);

        graphics.drawString(font, titleText(), canvasLeft + 2, 6, TITLE_COLOR, false);
        graphics.drawString(font, "Drag a text to move it - scroll over the preview to zoom",
                canvasLeft + 2, 17, HINT_COLOR, false);

        graphics.fill(panelLeft, 2, width, height, PANEL_BACKGROUND);
        graphics.fill(panelLeft, 2, panelLeft + 1, height, PANEL_BORDER);

        for (Entry entry : entries) {
            if (!entry.scrolls) {
                drawEntry(graphics, entry, mouseX, mouseY, partialTick);
            }
        }

        graphics.enableScissor(panelLeft, SCROLL_TOP, width, scrollBottom);
        for (Entry entry : entries) {
            if (entry.scrolls && visible(entry)) {
                drawEntry(graphics, entry, mouseX, mouseY, partialTick);
            }
        }
        graphics.disableScissor();

        graphics.drawString(font, statusMessage, panelContentLeft(), height - 30, statusColor, false);

        colorPicker.render(graphics, font);
        if (newDialogOpen) {
            renderNewDialog(graphics, mouseX, mouseY);
        }
        if (searchOpen) {
            renderSearchDialog(graphics, mouseX, mouseY);
        }
    }

    private void drawEntry(GuiGraphics graphics, Entry entry, int mouseX, int mouseY, float partialTick) {
        int y = entryY(entry);
        if (entry.widget == null) {
            if (entry.label != null) {
                graphics.drawString(font, entry.label, panelContentLeft(), y + 4,
                        entry.header ? SECTION_COLOR : HINT_COLOR, false);
            }
            return;
        }
        if (entry.label != null) {
            graphics.drawString(font, entry.label, panelContentLeft(),
                    y + (WIDGET_HEIGHT - 8) / 2 + 1, LABEL_COLOR, false);
        }
        entry.widget.render(graphics, mouseX, mouseY, partialTick);
    }



    // Thhis is where the screen accepts input
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchOpen) {
            // The search dialog floats above everything and consumes the click
            // either way, so no widget below can be hit by accident.
            return handleSearchClick(mouseX, mouseY);
        }
        if (newDialogOpen) {
            // The create dialog floats above everything and consumes the click
            // either way, so no widget below can be hit by accident.
            return handleNewDialogClick(mouseX, mouseY);
        }
        if (colorPicker.isOpen()) {
            // The picker floats above everything; it consumes the click either way
            // (a click outside closes it), so no widget can be hit by accident.
            colorPicker.mouseClicked(mouseX, mouseY);
            return true;
        }
        if (button == 0 && inPreview(mouseX, mouseY)) {
            // Widgets never live inside the preview rectangle, so a click there can
            // only be a selection/drag - never an unrelated button.
            handlePreviewClick(mouseX, mouseY);
            flushRebuild();
            return true;
        }
        boolean handled = false;
        for (Entry entry : entries) {
            if (entry.scrolls && !visible(entry)) {
                continue;
            }
            if (clickEntry(entry, mouseX, mouseY, button)) {
                handled = true;
                break;
            }
        }
        if (!handled) {
            focusBox(null);
        }
        flushRebuild();
        return handled;
    }

    private boolean clickEntry(Entry entry, double mouseX, double mouseY, int button) {
        AbstractWidget widget = entry.widget;
        if (widget == null || !widget.active || !widget.visible || !widget.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (!widget.mouseClicked(mouseX, mouseY, button)) {
            return false;
        }
        focusBox(widget instanceof EditBox box ? box : null);
        return true;
    }

    private void handlePreviewClick(double mouseX, double mouseY) {
        float previewX = viewport.toPreviewX(mouseX);
        float previewY = viewport.toPreviewY(mouseY);
        UiPreviewLayout.Placed hit = topmostAt(previewX, previewY);
        if (hit == null) {
            return;
        }
        selectKey(hit.key());
        UiTextBlock block = hit.block();
        if (block.supports(TextField.X) && block.supports(TextField.Y)) {
            dragging = true;
            dragKey = hit.key();
            dragStartX = block.x();
            dragStartY = block.y();
            dragGrabX = previewX;
            dragGrabY = previewY;
            dragMirrored = layout().mirrored();
        }
    }

    private UiPreviewLayout.Placed topmostAt(float previewX, float previewY) {
        UiPreviewLayout.Placed found = null;
        for (UiPreviewLayout.Placed placed : layout().placed(font)) {
            UiTextBlock block = placed.block();
            if (block == null || !block.present()) {
                continue;
            }
            if (placed.contains(previewX, previewY)) {
                found = placed;
            }
        }
        return found;
    }

    private void updateDrag(double mouseX, double mouseY) {
        UiTextBlock block = dragKey == null ? null : model.block(dragKey);
        if (block == null) {
            dragging = false;
            return;
        }
        float previewX = viewport.toPreviewX(mouseX);
        float previewY = viewport.toPreviewY(mouseY);
        int deltaX = Math.round(previewX - dragGrabX);
        int deltaY = Math.round(previewY - dragGrabY);
        block.setX(dragStartX + (dragMirrored ? -deltaX : deltaX));
        block.setY(dragStartY + deltaY);
        changed();
        refreshControlValues();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (colorPicker.isOpen() && colorPicker.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        if (dragging) {
            updateDrag(mouseX, mouseY);
            return true;
        }
        if (focusedBox != null && focusedBox.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (colorPicker.mouseReleased()) {
            handled = true;
        }
        if (dragging) {
            dragging = false;
            handled = true;
        }
        if (focusedBox != null && focusedBox.mouseReleased(mouseX, mouseY, button)) {
            handled = true;
        }
        flushRebuild();
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (searchOpen) {
            scrollSearchResults((int) Math.round(-delta));
            return true;
        }
        if (newDialogOpen) {
            return true;
        }
        if (inPreview(mouseX, mouseY)) {
            viewport.zoomBy(delta);
            fitViewport();
            return true;
        }
        if (mouseX >= panelLeft) {
            panelScroll -= (int) Math.round(delta * ROW_HEIGHT * 2.0D);
            applyScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchOpen) {
            return handleSearchKey(keyCode);
        }
        if (newDialogOpen) {
            return handleNewDialogKey(keyCode);
        }
        if (colorPicker.isOpen() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            colorPicker.close();
            return true;
        }
        if (focusedBox != null && focusedBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchOpen) {
            if (isSearchCharacter(codePoint) && searchQuery.length() < 64) {
                searchQuery += codePoint;
                refreshSearchResults();
            }
            return true;
        }
        if (newDialogOpen) {
            if (isNameCharacter(codePoint) && newDialogName.length() < 96) {
                newDialogName += codePoint;
            }
            return true;
        }
        if (focusedBox != null && focusedBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------------ */
    /* Create new definition                                              */
    /* ------------------------------------------------------------------ */

    private void openNewDialog() {
        this.newDialogOpen = true;
        this.newDialogKind = kind;
        this.newDialogName = "";
        this.overwritePending = false;
        status("Choose a type and a name for the new configuration.", HINT_COLOR);
    }


    private void createNewConfiguration() {
        String id = newDialogName == null ? "" : newDialogName.trim();
        if (id.isEmpty()) {
            status("Enter a name for the new configuration.", ERROR_COLOR);
            return;
        }
        if (!UiConfigRepository.isValidId(id)) {
            status("'" + id + "' is not a valid name - use letters, digits, '_', '-' or '.'.",
                    ERROR_COLOR);
            return;
        }
        if (UiConfigRepository.hasOverride(newDialogKind, id)) {
            status("'" + id + ".json' already exists - open it to edit instead (nothing was changed).",
                    ERROR_COLOR);
            return;
        }
        if (!confirmDiscard("new:" + newDialogKind.id() + ":" + id, "Create")) {
            return;
        }
        newDialogOpen = false;
        beginSession(UiEditorModel.createDefault(newDialogKind, id));
        status("Created a new " + newDialogKind.displayName() + " '" + id
                + "' - edit it and press Save.", OK_COLOR);
        queueRebuild();
    }

    private int newDialogLeft() {
        return Math.max(4, (width - NEW_DIALOG_WIDTH) / 2);
    }

    private int newDialogTop() {
        return Math.max(4, (height - NEW_DIALOG_HEIGHT) / 2);
    }

    private int typeButtonWidth() {
        return (NEW_DIALOG_WIDTH - 12 - 4) / 2;
    }

    private int typeButtonY() {
        return newDialogTop() + 30;
    }

    private int ammoButtonX() {
        return newDialogLeft() + 6;
    }

    private int heatButtonX() {
        return ammoButtonX() + typeButtonWidth() + 4;
    }

    private int dialogFieldY() {
        return newDialogTop() + 62;
    }

    private int footerButtonY() {
        return newDialogTop() + NEW_DIALOG_HEIGHT - 30;
    }

    private static int dialogButtonWidth() {
        return 70;
    }

    private int createButtonX() {
        return newDialogLeft() + NEW_DIALOG_WIDTH - 6 - dialogButtonWidth();
    }

    private int cancelButtonX() {
        return createButtonX() - 4 - dialogButtonWidth();
    }


    private void renderNewDialog(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = newDialogLeft();
        int top = newDialogTop();
        int right = left + NEW_DIALOG_WIDTH;
        int bottom = top + NEW_DIALOG_HEIGHT;
        graphics.fill(0, 0, width, height, DIALOG_SCRIM);
        graphics.fill(left, top, right, bottom, DIALOG_BACKGROUND);
        graphics.renderOutline(left, top, NEW_DIALOG_WIDTH, NEW_DIALOG_HEIGHT, DIALOG_BORDER);
        graphics.drawString(font, "Create new configuration", left + 6, top + 8, DIALOG_TITLE, false);

        graphics.drawString(font, "Type", left + 6, top + 20, LABEL_COLOR, false);
        drawDialogChoice(graphics, mouseX, mouseY, ammoButtonX(), typeButtonY(), typeButtonWidth(),
                "Ammo UI", newDialogKind == UiDefinitionKind.AMMO, true);
        drawDialogChoice(graphics, mouseX, mouseY, heatButtonX(), typeButtonY(), typeButtonWidth(),
                "Heat UI", newDialogKind == UiDefinitionKind.HEAT, true);

        String name = newDialogName == null ? "" : newDialogName;
        String trimmed = name.trim();
        boolean ready = !trimmed.isEmpty() && UiConfigRepository.isValidId(trimmed);
        graphics.drawString(font, "Name", left + 6, top + 52, LABEL_COLOR, false);
        int fieldY = dialogFieldY();
        graphics.fill(left + 6, fieldY, right - 6, fieldY + NEW_DIALOG_CHOICE_HEIGHT, DIALOG_FIELD);
        graphics.renderOutline(left + 6, fieldY, NEW_DIALOG_WIDTH - 12, NEW_DIALOG_CHOICE_HEIGHT,
                DIALOG_FIELD_BORDER);
        graphics.drawString(font, name, left + 9, fieldY + 4, TITLE_COLOR, false);
        graphics.drawString(font, "_", left + 9 + font.width(name), fieldY + 4, SELECTED_COLOR, false);

        drawDialogChoice(graphics, mouseX, mouseY, createButtonX(), footerButtonY(),
                dialogButtonWidth(), "Create", false, ready);
        drawDialogChoice(graphics, mouseX, mouseY, cancelButtonX(), footerButtonY(),
                dialogButtonWidth(), "Cancel", false, true);
        graphics.drawString(font, "Enter to create  -  Esc to cancel", left + 6, bottom - 12,
                HINT_COLOR, false);
    }

    private void drawDialogChoice(GuiGraphics graphics, int mouseX, int mouseY, int x, int y,
                                  int choiceWidth, String label, boolean selected, boolean enabled) {
        boolean hovered = enabled && hit(mouseX, mouseY, x, y, choiceWidth, NEW_DIALOG_CHOICE_HEIGHT);
        int fill = selected ? DIALOG_CHOICE_SELECTED : hovered ? DIALOG_CHOICE_HOVERED : DIALOG_CHOICE;
        graphics.fill(x, y, x + choiceWidth, y + NEW_DIALOG_CHOICE_HEIGHT, DIALOG_BORDER);
        graphics.fill(x + 1, y + 1, x + choiceWidth - 1, y + NEW_DIALOG_CHOICE_HEIGHT - 1, fill);
        int textColor = !enabled ? 0xFF707070 : selected ? SELECTED_COLOR : TITLE_COLOR;
        graphics.drawString(font, label, x + (choiceWidth - font.width(label)) / 2,
                y + (NEW_DIALOG_CHOICE_HEIGHT - 8) / 2 + 1, textColor, false);
    }

    private boolean handleNewDialogClick(double mouseX, double mouseY) {
        if (!hit(mouseX, mouseY, newDialogLeft(), newDialogTop(), NEW_DIALOG_WIDTH, NEW_DIALOG_HEIGHT)) {
            newDialogOpen = false;
            return true;
        }
        if (hit(mouseX, mouseY, ammoButtonX(), typeButtonY(), typeButtonWidth(), NEW_DIALOG_CHOICE_HEIGHT)) {
            newDialogKind = UiDefinitionKind.AMMO;
            return true;
        }
        if (hit(mouseX, mouseY, heatButtonX(), typeButtonY(), typeButtonWidth(), NEW_DIALOG_CHOICE_HEIGHT)) {
            newDialogKind = UiDefinitionKind.HEAT;
            return true;
        }
        if (hit(mouseX, mouseY, createButtonX(), footerButtonY(), dialogButtonWidth(), NEW_DIALOG_CHOICE_HEIGHT)) {
            createNewConfiguration();
            return true;
        }
        if (hit(mouseX, mouseY, cancelButtonX(), footerButtonY(), dialogButtonWidth(), NEW_DIALOG_CHOICE_HEIGHT)) {
            newDialogOpen = false;
            return true;
        }
        return true;
    }

    private boolean handleNewDialogKey(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            newDialogOpen = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            createNewConfiguration();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!newDialogName.isEmpty()) {
                newDialogName = newDialogName.substring(0, newDialogName.length() - 1);
            }
            return true;
        }
        // Swallow everything else so the panel never reacts while the dialog is up.
        return true;
    }


    private void openSearchDialog() {
        this.searchOpen = true;
        this.searchQuery = "";
        this.searchSelected = 0;
        this.searchScroll = 0;
        this.searchEntries = UiDefinitionIndex.load(kind);
        refreshSearchResults();
        status("Searching the " + kind.displayName() + " definitions of the loaded content packs.", HINT_COLOR);
    }

    private void closeSearchDialog() {
        searchOpen = false;
        searchEntries = List.of();
        searchResults = List.of();
        searchQuery = "";
        searchSelected = 0;
        searchScroll = 0;
    }

    private void refreshSearchResults() {
        searchResults = UiDefinitionIndex.filter(searchEntries, searchQuery);
        searchSelected = Mth.clamp(searchSelected, 0, Math.max(0, searchResults.size() - 1));
        searchScroll = Mth.clamp(searchScroll, 0,
                Math.max(0, searchResults.size() - SEARCH_VISIBLE_ROWS));
    }

    private void scrollSearchResults(int rows) {
        if (searchResults.isEmpty()) {
            return;
        }
        searchScroll = Mth.clamp(searchScroll + rows, 0,
                Math.max(0, searchResults.size() - SEARCH_VISIBLE_ROWS));
    }

    private void moveSearchSelection(int delta) {
        if (searchResults.isEmpty()) {
            return;
        }
        searchSelected = Mth.clamp(searchSelected + delta, 0, searchResults.size() - 1);
        if (searchSelected < searchScroll) {
            searchScroll = searchSelected;
        } else if (searchSelected >= searchScroll + SEARCH_VISIBLE_ROWS) {
            searchScroll = searchSelected - SEARCH_VISIBLE_ROWS + 1;
        }
    }

    private void openSelectedSearchResult() {
        if (!searchResults.isEmpty()) {
            openSearchResult(searchResults.get(Mth.clamp(searchSelected, 0, searchResults.size() - 1)));
        }
    }


    private void openSearchResult(UiDefinitionIndex.Entry entry) {
        if (entry == null) {
            return;
        }
        if (!confirmDiscard("search:" + entry.kind().id() + ":" + entry.id() + ":" + entry.source(),
                "opening '" + entry.id() + "'")) {
            return;
        }
        UiEditorModel fresh = entry.json() != null
                ? UiEditorModel.fromJson(entry.kind(), entry.id(), entry.json().deepCopy(), null)
                : UiConfigRepository.open(entry.kind(), entry.id());
        closeSearchDialog();
        beginSession(fresh, entry.source());
        queueRebuild();
    }

    private int searchDialogLeft() {
        return Math.max(4, (width - SEARCH_DIALOG_WIDTH) / 2);
    }

    private int searchDialogTop() {
        return Math.max(4, (height - SEARCH_DIALOG_HEIGHT) / 2);
    }

    private int searchFieldY() {
        return searchDialogTop() + 30;
    }

    private int searchListY() {
        return searchDialogTop() + 58;
    }

    private int searchResultY(int row) {
        return searchListY() + row * SEARCH_ROW_HEIGHT;
    }

    /** Index into {@link #searchResults} for a mouse y, or -1 outside the list. */
    private int searchResultAt(double mouseY) {
        int row = (int) Math.floor((mouseY - searchListY()) / (double) SEARCH_ROW_HEIGHT);
        return row >= 0 && row < SEARCH_VISIBLE_ROWS ? searchScroll + row : -1;
    }

    private static boolean isSearchCharacter(char character) {
        return isNameCharacter(character) || character == ' ';
    }

    private boolean handleSearchKey(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeSearchDialog();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            openSelectedSearchResult();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                refreshSearchResults();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            moveSearchSelection(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            moveSearchSelection(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scrollSearchResults(-SEARCH_VISIBLE_ROWS);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            scrollSearchResults(SEARCH_VISIBLE_ROWS);
            return true;
        }
        // Swallow everything else so the property panel never reacts underneath.
        return true;
    }

    private boolean handleSearchClick(double mouseX, double mouseY) {
        int left = searchDialogLeft();
        int top = searchDialogTop();
        if (!hit(mouseX, mouseY, left, top, SEARCH_DIALOG_WIDTH, SEARCH_DIALOG_HEIGHT)) {
            // Like the create dialog: clicking outside cancels, and the edited
            // definition - including unsaved changes - is left untouched.
            closeSearchDialog();
            return true;
        }
        boolean insideList = mouseX >= left + 6 && mouseX < left + SEARCH_DIALOG_WIDTH - 6;
        int index = searchResultAt(mouseY);
        if (insideList && index >= 0 && index < searchResults.size()) {
            searchSelected = index;
            openSearchResult(searchResults.get(index));
        }
        return true;
    }


    private void renderSearchDialog(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = searchDialogLeft();
        int top = searchDialogTop();
        int right = left + SEARCH_DIALOG_WIDTH;
        int bottom = top + SEARCH_DIALOG_HEIGHT;
        graphics.fill(0, 0, width, height, DIALOG_SCRIM);
        graphics.fill(left, top, right, bottom, DIALOG_BACKGROUND);
        graphics.renderOutline(left, top, SEARCH_DIALOG_WIDTH, SEARCH_DIALOG_HEIGHT, DIALOG_BORDER);

        graphics.drawString(font, "Search " + kind.displayName() + " definitions",
                left + 6, top + 8, DIALOG_TITLE, false);
        String count = searchResults.size() + "/" + searchEntries.size();
        graphics.drawString(font, count, right - 6 - font.width(count), top + 8, HINT_COLOR, false);

        graphics.drawString(font, "Find", left + 6, searchFieldY() + 4, LABEL_COLOR, false);
        int fieldLeft = left + 32;
        int fieldRight = right - 6;
        graphics.fill(fieldLeft, searchFieldY(), fieldRight, searchFieldY() + 16, DIALOG_FIELD);
        graphics.renderOutline(fieldLeft, searchFieldY(), fieldRight - fieldLeft, 16, DIALOG_FIELD_BORDER);
        graphics.drawString(font, searchQuery, fieldLeft + 3, searchFieldY() + 4, TITLE_COLOR, false);
        graphics.drawString(font, "_", fieldLeft + 3 + font.width(searchQuery), searchFieldY() + 4,
                SELECTED_COLOR, false);

        if (searchResults.isEmpty()) {
            String message = searchEntries.isEmpty()
                    ? "No " + kind.displayName() + " definitions found in the loaded content packs."
                    : "Nothing matches '" + searchQuery + "'.";
            graphics.drawString(font, message, left + 6, searchListY() + 6, HINT_COLOR, false);
        } else {
            int visible = Math.min(SEARCH_VISIBLE_ROWS, searchResults.size() - searchScroll);
            for (int row = 0; row < visible; row++) {
                int index = searchScroll + row;
                UiDefinitionIndex.Entry entry = searchResults.get(index);
                int y = searchResultY(row);
                boolean selected = index == searchSelected;
                boolean hovered = hit(mouseX, mouseY, left + 6, y, SEARCH_DIALOG_WIDTH - 12, SEARCH_ROW_HEIGHT);
                if (selected || hovered) {
                    graphics.fill(left + 6, y, right - 6, y + SEARCH_ROW_HEIGHT,
                            selected ? DIALOG_CHOICE_SELECTED : DIALOG_CHOICE);
                }
                graphics.drawString(font, entry.id(), left + 9, y + 4,
                        selected ? SELECTED_COLOR : TITLE_COLOR, false);

                // The source pack is right aligned and shortened so it can never
                // collide with the definition id.
                int sourceRight = right - 9;
                int idEnd = left + 9 + font.width(entry.id()) + 6;
                String source = entry.source();
                while (!source.isEmpty() && sourceRight - font.width(source) < idEnd) {
                    source = source.substring(0, source.length() - 1);
                }
                graphics.drawString(font, source, sourceRight - font.width(source), y + 4,
                        HINT_COLOR, false);
            }
            if (searchResults.size() > SEARCH_VISIBLE_ROWS) {
                graphics.drawString(font, (searchScroll + 1) + "-" + (searchScroll + visible)
                                + " of " + searchResults.size(),
                        left + 6, searchResultY(SEARCH_VISIBLE_ROWS) + 2, HINT_COLOR, false);
            }
        }

        graphics.drawString(font, "Click a result to open it - Enter opens the selected - Esc cancels",
                left + 6, bottom - 12, HINT_COLOR, false);
    }


    private static boolean isNameCharacter(char character) {
        return character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9'
                || character == '_' || character == '-' || character == '.';
    }

    private static boolean hit(double mouseX, double mouseY, int x, int y, int hitWidth, int hitHeight) {
        return mouseX >= x && mouseX < x + hitWidth && mouseY >= y && mouseY < y + hitHeight;
    }


    /* ------------------------------------------------------------------ */
    /* Save / Reset / Cancel                                              */
    /* ------------------------------------------------------------------ */

    private void save() {
        String targetId = model.id();
        if (targetId == null || targetId.isBlank()) {
            status("Cannot save: the definition has no id.", ERROR_COLOR);
            return;
        }
        if (!UiConfigRepository.isValidId(targetId)) {
            status("Cannot save: '" + targetId
                    + "' is not a valid file name - use letters, digits, '_', '-' or '.'.",
                    ERROR_COLOR);
            return;
        }
        JsonObject json = model.toJson();
        if (!overwritePending && UiConfigRepository.hasOverride(kind, targetId)) {
            overwritePending = true;
            status("'" + targetId + ".json' already exists - click Save again to overwrite it.",
                    ERROR_COLOR);
            return;
        }
        // UiConfigRepository validates the JSON with Point Blank's own parser and
        // registers it (which updates the live HUD) before writing the file.
        String error = UiConfigRepository.save(model);
        if (error != null) {
            status("Save failed: " + error, ERROR_COLOR);
            return;
        }
        sessionBaseline = json.deepCopy();
        sessionId = targetId;
        sessionFromScratch = false;
        overwritePending = false;
        status("Saved '" + targetId + ".json' and applied it to the HUD.", OK_COLOR);
    }

    private void reset() {
        if (sessionBaseline == null) {
            return;
        }
        model = UiEditorModel.fromJson(kind, sessionId, sessionBaseline.deepCopy(), model.sourcePath());
        requestedId = sessionId;
        overwritePending = false;
        if (selectedBlock() == null) {
            selectedKey = defaultSelection(model);
        }
        status("Reverted to the values from the start of the session.", HINT_COLOR);
        queueRebuild();
    }

    private void cancel() {
        onClose();
    }
}
