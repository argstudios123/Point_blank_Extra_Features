package com.arg.pbextra.client.gui;

import com.arg.pbextra.client.UiConfigRepository;
import com.arg.pbextra.client.UiPreviewLayout;
import com.arg.pbextra.client.UiPreviewRenderer;
import com.arg.pbextra.client.UiPreviewState;
import com.arg.pbextra.client.UiPreviewViewport;
import com.arg.pbextra.uieditor.GeneratedUiStore;
import com.arg.pbextra.uieditor.TextField;
import com.arg.pbextra.uieditor.UiColors;
import com.arg.pbextra.uieditor.UiDefinitionIndex;
import com.arg.pbextra.uieditor.UiDefinitionKind;
import com.arg.pbextra.uieditor.UiEditorModel;
import com.arg.pbextra.uieditor.UiTextBlock;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

    private int LABEL_COLOR;
    private int SECTION_COLOR;
    private int HINT_COLOR;
    private int OK_COLOR;
    private int ERROR_COLOR;
    private int TITLE_COLOR;
    private int SELECTED_COLOR;
    private int PANEL_BACKGROUND;
    private int PANEL_BORDER;

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
    private int DIALOG_SCRIM;
    private int DIALOG_BACKGROUND;
    private int DIALOG_BORDER;
    private int DIALOG_FIELD;
    private int DIALOG_FIELD_BORDER;
    private int DIALOG_CHOICE;
    private int DIALOG_CHOICE_HOVERED;
    private int DIALOG_CHOICE_SELECTED;
    private int DIALOG_TITLE;

    // Modal "save" dialog, shared by the Ammo and Heat editors.
    private static final int SAVE_DIALOG_WIDTH = 404;
    private static final int SAVE_DIALOG_HEIGHT = 236;
    private static final int SAVE_PACK_VISIBLE_ROWS = 4;
    private static final int SAVE_PACK_ROW_HEIGHT = 14;
    private static final int SAVE_FOOTER_BUTTON_WIDTH = 70;
    private static final int SAVE_NAME_FIELD_LEFT = 70;
    private static final int SAVE_BROWSE_BUTTON_WIDTH = 58;
    private static final int TEXTURE_BROWSE_BUTTON_WIDTH = 50;
    private static final int BROWSE_VISIBLE_ROWS = 8;
    private static final int BROWSE_ROW_HEIGHT = 14;
    private static final int BROWSE_UP_BUTTON_WIDTH = 50;
    private static final int BROWSE_CREATE_BUTTON_WIDTH = 100;
    private static final int CREATE_FOLDER_DIALOG_WIDTH = 300;
    private static final int CREATE_FOLDER_DIALOG_HEIGHT = 120;
    private static final int HUD_MIN_SIZE = 1;
    private static final int HUD_MAX_SIZE = 100000;
    private static final int HUD_MAX_PADDING = 100000;
    private static final double NUMERIC_SHIFT_MULTIPLIER = 10.0D;

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

    /** A numeric edit box that the mouse wheel can adjust. */
    private record NumericField(EditBox box, DoubleSupplier getter, DoubleConsumer setter,
                                double step, boolean integer, double minValue, double maxValue) {
    }

    private record BrowserEntry(Path path, String name, boolean directory) {
    }

    private enum BrowserPurpose {
        SAVE_DESTINATION,
        TEXTURE
    }

    private enum HudDragMode {
        NONE,
        PADDING_X,
        PADDING_Y,
        WIDTH,
        HEIGHT
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
    private final List<NumericField> numericFields = new ArrayList<>();
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

    // The modal "save" dialog.
    private boolean saveDialogOpen;
    private boolean saveToContentPack;
    private String saveFileName = "";
    private List<String> savePackNames = List.of();
    private int savePackSelected;
    private int savePackScroll;
    private boolean saveOverwritePending;
    private String saveDialogStatus = "";
    private int saveDialogStatusColor = HINT_COLOR;
    private Path saveDestinationDirectory;

    // Shared directory / texture browser, drawn on top of the save dialog.
    private boolean browserOpen;
    private BrowserPurpose browserPurpose = BrowserPurpose.SAVE_DESTINATION;
    private Path browserRoot;
    private Path browserDirectory;
    private List<BrowserEntry> browserEntries = List.of();
    private int browserSelected;
    private int browserScroll;
    private String browserStatus = "";
    private int browserStatusColor = HINT_COLOR;
    private Consumer<String> textureBrowserSetter;
    private String textureBrowserLabel = "";

    // Small naming dialog on top of the texture browser.
    private boolean createFolderDialogOpen;
    private String createFolderName = "";
    private String createFolderStatus = "";
    private int createFolderStatusColor = HINT_COLOR;

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

    // Middle-mouse preview panning (separate from element dragging).
    private boolean panning;
    private double panLastX;
    private double panLastY;

    // HUD padding / width / height handles.
    private HudDragMode hudDragMode = HudDragMode.NONE;
    private int hudDragStartValue;
    private float hudDragGrabX;
    private float hudDragGrabY;

    public UiEditorScreen(UiDefinitionKind kind, String requestedId) {
        super(Component.literal((kind == null ? "UI" : kind.displayName()) + " Editor"));
        this.kind = kind == null ? UiDefinitionKind.AMMO : kind;
        this.requestedId = requestedId;
        applyThemeColors();
        this.statusColor = HINT_COLOR;
        this.saveDialogStatusColor = HINT_COLOR;
        this.browserStatusColor = HINT_COLOR;
        this.createFolderStatusColor = HINT_COLOR;
    }

    private void applyThemeColors() {
        UiEditorTheme.Palette palette = UiEditorTheme.active().palette();
        LABEL_COLOR = palette.label();
        SECTION_COLOR = palette.heading();
        HINT_COLOR = palette.hint();
        OK_COLOR = palette.success();
        ERROR_COLOR = palette.error();
        TITLE_COLOR = palette.title();
        SELECTED_COLOR = palette.selected();
        PANEL_BACKGROUND = palette.panelBackground();
        PANEL_BORDER = palette.panelBorder();
        DIALOG_SCRIM = palette.scrim();
        DIALOG_BACKGROUND = palette.dialogBackground();
        DIALOG_BORDER = palette.buttonBorderHovered();
        DIALOG_FIELD = palette.fieldBackground();
        DIALOG_FIELD_BORDER = palette.buttonBorder();
        DIALOG_CHOICE = palette.buttonFill();
        DIALOG_CHOICE_HOVERED = palette.buttonFillHovered();
        DIALOG_CHOICE_SELECTED = palette.choiceSelected();
        DIALOG_TITLE = palette.dialogTitle();
    }

    private int buttonTextColor() {
        return UiEditorTheme.active().palette().buttonText();
    }

    private void cycleTheme() {
        UiEditorTheme next = UiEditorTheme.active().next();
        next.select();
        applyThemeColors();
        status("Editor theme: " + next.displayName(), HINT_COLOR);
        queueRebuild();
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
            fitViewport();
        }
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
        numericFields.clear();
        focusedBox = null;
        cursorY = 0;

        // --- Configuration
        section("CONFIGURATION");
        int configRow = cursorY;
        int openWidth = 46;
        int openX = panelContentRight() - openWidth;
        int idWidth = Math.max(40, openX - 4 - panelContentLeft());
        EditBox idBox = new ThemedEditBox(font, panelContentLeft(), 0, idWidth,
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

        FlatButton theme = new FlatButton(font, panelInnerWidth(), WIDGET_HEIGHT,
                "Theme: " + UiEditorTheme.active().displayName(), this::cycleTheme);
        theme.setX(panelContentLeft());
        theme.textColor(SELECTED_COLOR);
        addEntry(theme, cursor(), true, null, null);
        nextRow();

        addTextRow("Name", model::name, model::setName);
        addReadOnly("Source: " + sourceLabel());
        addReadOnly("Save to: " + targetFileLabel());

        // --- Textures
        section("TEXTURES");
        if (kind == UiDefinitionKind.AMMO) {
            addTextRow("Texture", model::texture, model::setTexture, true);
        } else {
            addTextRow("Background", model::backgroundTexture, model::setBackgroundTexture, true);
            addTextRow("Progress", model::progressTexture, model::setProgressTexture, true);
            addTextRow("Mask", model::maskTexture, model::setMaskTexture, true);
        }

        // --- Layout
        section("LAYOUT");
        locationRow();
        addNumberRow("Width", () -> model.width(), value -> model.setWidth((int) Math.round(value)),
                true, 1.0D, 1.0D, 100000.0D);
        addNumberRow("Height", () -> model.height(), value -> model.setHeight((int) Math.round(value)),
                true, 1.0D, 1.0D, 100000.0D);
        addNumberRow("Inertia", () -> model.inertia(), value -> model.setInertia((float) value),
                false, 0.05D, 0.0D, 1000.0D);
        addNumberRow("Padding X", () -> model.paddingX(), value -> model.setPaddingX((int) Math.round(value)),
                true, 1.0D, -100000.0D, 100000.0D);
        addNumberRow("Padding Y", () -> model.paddingY(), value -> model.setPaddingY((int) Math.round(value)),
                true, 1.0D, -100000.0D, 100000.0D);
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
        ammoTab.textColor(kind == UiDefinitionKind.AMMO ? SELECTED_COLOR : buttonTextColor());
        addEntry(ammoTab, 4, false, null, null);
        FlatButton heatTab = new FlatButton(font, tabWidth, 16, "Heat UI",
                () -> switchKind(UiDefinitionKind.HEAT));
        heatTab.setX(panelLeft + 6 + tabWidth);
        heatTab.textColor(kind == UiDefinitionKind.HEAT ? SELECTED_COLOR : buttonTextColor());
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
        addNumberRow(label, getter, setter, integer, 0.0D, 0.0D, 0.0D);
    }

    private void addNumberRow(String label, DoubleSupplier getter, DoubleConsumer setter, boolean integer,
                              double scrollStep, double minValue, double maxValue) {
        EditBox box = new ThemedEditBox(font, panelContentRight() - NUM_WIDTH, 0, NUM_WIDTH,
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
        if (scrollStep > 0.0D) {
            numericFields.add(new NumericField(box, getter, setter, scrollStep, integer,
                    minValue, maxValue));
        }
        addEntry(box, cursor(), true, label, refresh);
        nextRow();
    }

    private NumericField numericFieldAt(double mouseX, double mouseY) {
        for (NumericField field : numericFields) {
            EditBox box = field.box();
            if (box.active && box.visible
                    && box.getY() >= SCROLL_TOP && box.getY() + box.getHeight() <= scrollBottom
                    && box.isMouseOver(mouseX, mouseY)) {
                return field;
            }
        }
        return null;
    }

    private void scrollNumericField(NumericField field, double delta) {
        if (delta == 0.0D) {
            return;
        }
        double step = field.step();
        if (KeyModifier.SHIFT.isActive(null)) {
            step *= NUMERIC_SHIFT_MULTIPLIER;
        }
        double value = field.getter().getAsDouble() + Math.signum(delta) * step;
        value = Mth.clamp(value, field.minValue(), field.maxValue());
        if (field.integer()) {
            value = Math.round(value);
        }
        field.setter().accept(value);
        changed();
        updatingControls = true;
        try {
            field.box().setValue(formatNumber(field.getter().getAsDouble(), field.integer()));
        } finally {
            updatingControls = false;
        }
    }


    private void addTextRow(String label, Supplier<String> getter, Consumer<String> setter) {
        addTextRow(label, getter, setter, false);
    }

    private void addTextRow(String label, Supplier<String> getter, Consumer<String> setter,
                            boolean browseTexture) {
        int reserved = browseTexture ? TEXTURE_BROWSE_BUTTON_WIDTH + 4 : 0;
        int boxWidth = Math.max(40, panelInnerWidth() - TEXT_LABEL_WIDTH - 2 - reserved);
        EditBox box = new ThemedEditBox(font, panelContentLeft() + TEXT_LABEL_WIDTH, 0, boxWidth,
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
        if (browseTexture) {
            FlatButton browse = new FlatButton(font, TEXTURE_BROWSE_BUTTON_WIDTH, WIDGET_HEIGHT,
                    "Browse...", () -> openTextureBrowser(label, setter));
            browse.setX(panelContentRight() - TEXTURE_BROWSE_BUTTON_WIDTH);
            browse.textColor(SELECTED_COLOR);
            addEntry(browse, cursor(), true, null, null);
        }
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
        EditBox box = new ThemedEditBox(font, boxX, 0, HEX_WIDTH, WIDGET_HEIGHT, Component.empty());
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
                    value -> { block.setNumberValue(field, value); changed(); }, true,
                    1.0D, -100000.0D, 100000.0D);
            case FLOAT -> addNumberRow(label, () -> block.numberValue(field),
                    value -> { block.setNumberValue(field, value); changed(); }, false,
                    0.05D,
                    field == TextField.SIZE ? 0.1D : -100000.0D,
                    100000.0D);
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
            button.textColor(key.equals(selectedKey) ? SELECTED_COLOR : buttonTextColor());
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

    // Minecraft 26.1.2's Screen#extractRenderStateWithTooltipAndSubtitles already
    // extracts this screen's background (panorama fade, background blur and menu
    // background texture) immediately before it calls this method. GuiRenderState
    // only accepts a single blur request per frame ("Can only blur once per frame"),
    // so extracting the background here a second time crashed the editor. The Ammo
    // and Heat editors share this screen, so both are fixed by this one change.
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int themeBackground = UiEditorTheme.active().palette().screenBackground();
        if ((themeBackground >>> 24) != 0) {
            graphics.fill(0, 0, width, height, themeBackground);
        }
        UiPreviewRenderer.render(graphics, font, layout(), viewport, canvasLeft, canvasTop,
                canvasRight, canvasBottom, true, selectedKey);

        graphics.text(font, titleText(), canvasLeft + 2, 6, TITLE_COLOR, false);
        graphics.text(font, "Drag a text to move it - scroll over the preview to zoom",
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

        graphics.text(font, statusMessage, panelContentLeft(), height - 30, statusColor, false);

        colorPicker.render(graphics, font);
        if (newDialogOpen) {
            renderNewDialog(graphics, mouseX, mouseY);
        }
        if (searchOpen) {
            renderSearchDialog(graphics, mouseX, mouseY);
        }
        if (saveDialogOpen) {
            renderSaveDialog(graphics, mouseX, mouseY);
        }
        if (browserOpen) {
            renderBrowser(graphics, mouseX, mouseY);
        }
        if (createFolderDialogOpen) {
            renderCreateFolderDialog(graphics, mouseX, mouseY);
        }
    }

    private void drawEntry(GuiGraphicsExtractor graphics, Entry entry, int mouseX, int mouseY, float partialTick) {
        int y = entryY(entry);
        if (entry.widget == null) {
            if (entry.label != null) {
                graphics.text(font, entry.label, panelContentLeft(), y + 4,
                        entry.header ? SECTION_COLOR : HINT_COLOR, false);
            }
            return;
        }
        if (entry.label != null) {
            graphics.text(font, entry.label, panelContentLeft(),
                    y + (WIDGET_HEIGHT - 8) / 2 + 1, LABEL_COLOR, false);
        }
        entry.widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }



    // Thhis is where the screen accepts input
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (createFolderDialogOpen) {
            boolean handled = handleCreateFolderClick(mouseX, mouseY);
            flushRebuild();
            return handled;
        }
        if (browserOpen) {
            boolean handled = handleBrowserClick(mouseX, mouseY);
            flushRebuild();
            return handled;
        }
        if (saveDialogOpen) {
            boolean handled = handleSaveDialogClick(mouseX, mouseY);
            flushRebuild();
            return handled;
        }
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
        if (button == 2 && inPreview(mouseX, mouseY)) {
            // Middle mouse pans the preview without touching element positions.
            focusBox(null);
            panning = true;
            panLastX = mouseX;
            panLastY = mouseY;
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
        if (!widget.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false)) {
            return false;
        }
        focusBox(widget instanceof EditBox box ? box : null);
        return true;
    }

    private void handlePreviewClick(double mouseX, double mouseY) {
        focusBox(null);
        float previewX = viewport.toPreviewX(mouseX);
        float previewY = viewport.toPreviewY(mouseY);
        if (beginHudDrag(mouseX, mouseY, previewX, previewY)) {
            return;
        }
        // The selected handle wins first, but a non-selected handle never steals a
        // click from a text element underneath it. This keeps the handles from
        // interfering with normal preview interactions.
        UiPreviewLayout.Placed hit = handleAt(mouseX, mouseY, true);
        if (hit == null) {
            hit = topmostAt(previewX, previewY);
        }
        if (hit == null) {
            hit = handleAt(mouseX, mouseY, false);
        }
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

    private boolean beginHudDrag(double mouseX, double mouseY, float previewX, float previewY) {
        UiPreviewLayout previewLayout = layout();
        if (UiPreviewRenderer.paddingXHandle(previewLayout, viewport).contains(mouseX, mouseY)) {
            return startHudDrag(HudDragMode.PADDING_X, model.paddingX(), previewX, previewY);
        }
        if (UiPreviewRenderer.paddingYHandle(previewLayout, viewport).contains(mouseX, mouseY)) {
            return startHudDrag(HudDragMode.PADDING_Y, model.paddingY(), previewX, previewY);
        }
        if (UiPreviewRenderer.widthHandle(previewLayout, viewport).contains(mouseX, mouseY)) {
            return startHudDrag(HudDragMode.WIDTH, model.width(), previewX, previewY);
        }
        if (UiPreviewRenderer.heightHandle(previewLayout, viewport).contains(mouseX, mouseY)) {
            return startHudDrag(HudDragMode.HEIGHT, model.height(), previewX, previewY);
        }
        return false;
    }

    private boolean startHudDrag(HudDragMode mode, int startValue,
                                 float previewX, float previewY) {
        focusBox(null);
        this.hudDragMode = mode;
        this.hudDragStartValue = startValue;
        this.hudDragGrabX = previewX;
        this.hudDragGrabY = previewY;
        return true;
    }

    private void updateHudDrag(double mouseX, double mouseY) {
        float previewX = viewport.toPreviewX(mouseX);
        float previewY = viewport.toPreviewY(mouseY);
        int deltaX = Math.round(previewX - hudDragGrabX);
        int deltaY = Math.round(previewY - hudDragGrabY);
        UiPreviewLayout previewLayout = layout();
        switch (hudDragMode) {
            case PADDING_X -> model.setPaddingX(Mth.clamp(
                    hudDragStartValue + previewLayout.paddingXSign() * deltaX,
                    -HUD_MAX_PADDING, HUD_MAX_PADDING));
            case PADDING_Y -> model.setPaddingY(Mth.clamp(
                    hudDragStartValue + previewLayout.paddingYSign() * deltaY,
                    -HUD_MAX_PADDING, HUD_MAX_PADDING));
            case WIDTH -> model.setWidth(Mth.clamp(
                    hudDragStartValue + previewLayout.paddingXSign() * deltaX,
                    HUD_MIN_SIZE, HUD_MAX_SIZE));
            case HEIGHT -> model.setHeight(Mth.clamp(
                    hudDragStartValue + previewLayout.paddingYSign() * deltaY,
                    HUD_MIN_SIZE, HUD_MAX_SIZE));
            default -> {
                return;
            }
        }
        changed();
        refreshControlValues();
    }

    private UiPreviewLayout.Placed handleAt(double mouseX, double mouseY, boolean selectedOnly) {
        List<UiPreviewLayout.Placed> placed = layout().placed(font);
        for (int index = placed.size() - 1; index >= 0; index--) {
            UiPreviewLayout.Placed candidate = placed.get(index);
            UiTextBlock block = candidate.block();
            if (block == null || !block.present()
                    || !block.supports(TextField.X) || !block.supports(TextField.Y)) {
                continue;
            }
            if (selectedOnly && !candidate.key().equals(selectedKey)) {
                continue;
            }
            if (UiPreviewRenderer.handleHit(candidate, viewport, mouseX, mouseY)) {
                return candidate;
            }
        }
        return null;
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
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (createFolderDialogOpen || browserOpen || saveDialogOpen) {
            return true;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (colorPicker.isOpen() && colorPicker.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        if (hudDragMode != HudDragMode.NONE) {
            updateHudDrag(mouseX, mouseY);
            return true;
        }
        if (panning) {
            viewport.panBy(mouseX - panLastX, mouseY - panLastY,
                    canvasLeft, canvasTop, canvasRight - canvasLeft, canvasBottom - canvasTop);
            panLastX = mouseX;
            panLastY = mouseY;
            return true;
        }
        if (dragging) {
            updateDrag(mouseX, mouseY);
            return true;
        }
        if (focusedBox != null && focusedBox.mouseDragged(event, dragX, dragY)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (createFolderDialogOpen || browserOpen || saveDialogOpen) {
            panning = false;
            hudDragMode = HudDragMode.NONE;
            return true;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        boolean handled = false;
        if (colorPicker.mouseReleased()) {
            handled = true;
        }
        if (hudDragMode != HudDragMode.NONE) {
            hudDragMode = HudDragMode.NONE;
            handled = true;
        }
        if (panning) {
            panning = false;
            handled = true;
        }
        if (dragging) {
            dragging = false;
            handled = true;
        }
        if (focusedBox != null && focusedBox.mouseReleased(event)) {
            handled = true;
        }
        flushRebuild();
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double delta = scrollY;
        if (createFolderDialogOpen) {
            return true;
        }
        if (browserOpen) {
            scrollBrowserList((int) Math.round(-delta));
            return true;
        }
        if (saveDialogOpen) {
            if (saveToContentPack) {
                scrollSavePackList((int) Math.round(-delta));
            }
            return true;
        }
        if (searchOpen) {
            scrollSearchResults((int) Math.round(-delta));
            return true;
        }
        if (newDialogOpen) {
            return true;
        }
        if (inPreview(mouseX, mouseY)) {
            viewport.zoomAt(delta, mouseX, mouseY, canvasLeft, canvasTop,
                    canvasRight - canvasLeft, canvasBottom - canvasTop);
            return true;
        }
        NumericField numericField = numericFieldAt(mouseX, mouseY);
        if (numericField != null) {
            scrollNumericField(numericField, delta);
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
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
        if (createFolderDialogOpen) {
            return handleCreateFolderKey(keyCode);
        }
        if (browserOpen) {
            return handleBrowserKey(keyCode);
        }
        if (saveDialogOpen) {
            return handleSaveDialogKey(keyCode);
        }
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
        if (focusedBox != null && focusedBox.keyPressed(event)) {
            return true;
        }
        if (moveSelectedWithArrow(keyCode, event.hasShiftDown())) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private boolean moveSelectedWithArrow(int keyCode, boolean largeStep) {
        UiTextBlock block = selectedBlock();
        if (block == null) {
            return false;
        }
        int deltaX = 0;
        int deltaY = 0;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> deltaX = -1;
            case GLFW.GLFW_KEY_RIGHT -> deltaX = 1;
            case GLFW.GLFW_KEY_UP -> deltaY = -1;
            case GLFW.GLFW_KEY_DOWN -> deltaY = 1;
            default -> {
                return false;
            }
        }
        int step = largeStep ? 10 : 1;
        boolean mirrored = layout().mirrored();
        block.setX(block.x() + (mirrored ? -deltaX : deltaX) * step);
        block.setY(block.y() + deltaY * step);
        changed();
        refreshControlValues();
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char codePoint = (char) event.codepoint();
        if (createFolderDialogOpen) {
            if (isFolderNameCharacter(codePoint) && createFolderName.length() < 96) {
                createFolderName += codePoint;
            }
            return true;
        }
        if (browserOpen) {
            return true;
        }
        if (saveDialogOpen) {
            if (isNameCharacter(codePoint) && saveFileName.length() < 96) {
                saveFileName += codePoint;
                saveOverwritePending = false;
            }
            return true;
        }
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
        if (focusedBox != null && focusedBox.charTyped(event)) {
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


    private void renderNewDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = newDialogLeft();
        int top = newDialogTop();
        int right = left + NEW_DIALOG_WIDTH;
        int bottom = top + NEW_DIALOG_HEIGHT;
        graphics.fill(0, 0, width, height, DIALOG_SCRIM);
        graphics.fill(left, top, right, bottom, DIALOG_BACKGROUND);
        graphics.outline(left, top, NEW_DIALOG_WIDTH, NEW_DIALOG_HEIGHT, DIALOG_BORDER);
        graphics.text(font, "Create new configuration", left + 6, top + 8, DIALOG_TITLE, false);

        graphics.text(font, "Type", left + 6, top + 20, LABEL_COLOR, false);
        drawDialogChoice(graphics, mouseX, mouseY, ammoButtonX(), typeButtonY(), typeButtonWidth(),
                "Ammo UI", newDialogKind == UiDefinitionKind.AMMO, true);
        drawDialogChoice(graphics, mouseX, mouseY, heatButtonX(), typeButtonY(), typeButtonWidth(),
                "Heat UI", newDialogKind == UiDefinitionKind.HEAT, true);

        String name = newDialogName == null ? "" : newDialogName;
        String trimmed = name.trim();
        boolean ready = !trimmed.isEmpty() && UiConfigRepository.isValidId(trimmed);
        graphics.text(font, "Name", left + 6, top + 52, LABEL_COLOR, false);
        int fieldY = dialogFieldY();
        graphics.fill(left + 6, fieldY, right - 6, fieldY + NEW_DIALOG_CHOICE_HEIGHT, DIALOG_FIELD);
        graphics.outline(left + 6, fieldY, NEW_DIALOG_WIDTH - 12, NEW_DIALOG_CHOICE_HEIGHT,
                DIALOG_FIELD_BORDER);
        graphics.text(font, name, left + 9, fieldY + 4, TITLE_COLOR, false);
        graphics.text(font, "_", left + 9 + font.width(name), fieldY + 4, SELECTED_COLOR, false);

        drawDialogChoice(graphics, mouseX, mouseY, createButtonX(), footerButtonY(),
                dialogButtonWidth(), "Create", false, ready);
        drawDialogChoice(graphics, mouseX, mouseY, cancelButtonX(), footerButtonY(),
                dialogButtonWidth(), "Cancel", false, true);
        graphics.text(font, "Enter to create  -  Esc to cancel", left + 6, bottom - 12,
                HINT_COLOR, false);
    }

    private void drawDialogChoice(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y,
                                  int choiceWidth, String label, boolean selected, boolean enabled) {
        boolean hovered = enabled && hit(mouseX, mouseY, x, y, choiceWidth, NEW_DIALOG_CHOICE_HEIGHT);
        int fill = selected ? DIALOG_CHOICE_SELECTED : hovered ? DIALOG_CHOICE_HOVERED : DIALOG_CHOICE;
        graphics.fill(x, y, x + choiceWidth, y + NEW_DIALOG_CHOICE_HEIGHT, DIALOG_BORDER);
        graphics.fill(x + 1, y + 1, x + choiceWidth - 1, y + NEW_DIALOG_CHOICE_HEIGHT - 1, fill);
        int textColor = !enabled ? UiEditorTheme.active().palette().buttonTextDisabled()
                : selected ? SELECTED_COLOR : TITLE_COLOR;
        graphics.text(font, label, x + (choiceWidth - font.width(label)) / 2,
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


    private void renderSearchDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = searchDialogLeft();
        int top = searchDialogTop();
        int right = left + SEARCH_DIALOG_WIDTH;
        int bottom = top + SEARCH_DIALOG_HEIGHT;
        graphics.fill(0, 0, width, height, DIALOG_SCRIM);
        graphics.fill(left, top, right, bottom, DIALOG_BACKGROUND);
        graphics.outline(left, top, SEARCH_DIALOG_WIDTH, SEARCH_DIALOG_HEIGHT, DIALOG_BORDER);

        graphics.text(font, "Search " + kind.displayName() + " definitions",
                left + 6, top + 8, DIALOG_TITLE, false);
        String count = searchResults.size() + "/" + searchEntries.size();
        graphics.text(font, count, right - 6 - font.width(count), top + 8, HINT_COLOR, false);

        graphics.text(font, "Find", left + 6, searchFieldY() + 4, LABEL_COLOR, false);
        int fieldLeft = left + 32;
        int fieldRight = right - 6;
        graphics.fill(fieldLeft, searchFieldY(), fieldRight, searchFieldY() + 16, DIALOG_FIELD);
        graphics.outline(fieldLeft, searchFieldY(), fieldRight - fieldLeft, 16, DIALOG_FIELD_BORDER);
        graphics.text(font, searchQuery, fieldLeft + 3, searchFieldY() + 4, TITLE_COLOR, false);
        graphics.text(font, "_", fieldLeft + 3 + font.width(searchQuery), searchFieldY() + 4,
                SELECTED_COLOR, false);

        if (searchResults.isEmpty()) {
            String message = searchEntries.isEmpty()
                    ? "No " + kind.displayName() + " definitions found in the loaded content packs."
                    : "Nothing matches '" + searchQuery + "'.";
            graphics.text(font, message, left + 6, searchListY() + 6, HINT_COLOR, false);
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
                graphics.text(font, entry.id(), left + 9, y + 4,
                        selected ? SELECTED_COLOR : TITLE_COLOR, false);

                // The source pack is right aligned and shortened so it can never
                // collide with the definition id.
                int sourceRight = right - 9;
                int idEnd = left + 9 + font.width(entry.id()) + 6;
                String source = entry.source();
                while (!source.isEmpty() && sourceRight - font.width(source) < idEnd) {
                    source = source.substring(0, source.length() - 1);
                }
                graphics.text(font, source, sourceRight - font.width(source), y + 4,
                        HINT_COLOR, false);
            }
            if (searchResults.size() > SEARCH_VISIBLE_ROWS) {
                graphics.text(font, (searchScroll + 1) + "-" + (searchScroll + visible)
                                + " of " + searchResults.size(),
                        left + 6, searchResultY(SEARCH_VISIBLE_ROWS) + 2, HINT_COLOR, false);
            }
        }

        graphics.text(font, "Click a result to open it - Enter opens the selected - Esc cancels",
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
        this.saveDialogOpen = true;
        this.saveToContentPack = false;
        this.saveFileName = model.id() == null ? "" : model.id();
        this.savePackNames = UiConfigRepository.contentPackNames();
        this.savePackSelected = 0;
        this.savePackScroll = 0;
        this.saveOverwritePending = false;
        this.saveDialogStatus = "Choose the generated folder or a content pack.";
        this.saveDialogStatusColor = HINT_COLOR;
        this.saveDestinationDirectory = UiConfigRepository.generatedDirectory(kind);
        this.overwritePending = false;
    }

    private void saveDialogStatus(String message, int color) {
        this.saveDialogStatus = message == null ? "" : message;
        this.saveDialogStatusColor = color;
    }

    private String saveDialogId() {
        String value = saveFileName == null ? "" : saveFileName.trim();
        if (value.toLowerCase(Locale.ROOT).endsWith(".json")) {
            value = value.substring(0, value.length() - ".json".length()).trim();
        }
        return value;
    }

    private String selectedSavePack() {
        if (savePackNames.isEmpty()) {
            return null;
        }
        int index = Mth.clamp(savePackSelected, 0, savePackNames.size() - 1);
        return savePackNames.get(index);
    }

    private Path normalizedSaveDestination() {
        if (saveDestinationDirectory == null) {
            return null;
        }
        return saveToContentPack
                ? UiConfigRepository.normalizeContentPackDestination(kind, saveDestinationDirectory)
                : UiConfigRepository.normalizeGeneratedDestination(kind, saveDestinationDirectory);
    }

    private Path saveDialogTarget() {
        String id = saveDialogId();
        if (!UiConfigRepository.isValidId(id)) {
            return null;
        }
        Path directory = normalizedSaveDestination();
        return directory == null ? null : directory.resolve(id + ".json");
    }

    private String saveDialogTargetText() {
        String id = saveDialogId();
        if (!UiConfigRepository.isValidId(id)) {
            return "Target: (enter a valid file name)";
        }
        Path target = saveDialogTarget();
        if (target == null) {
            return saveToContentPack
                    ? "Target: (choose or browse a content pack)"
                    : "Target: (browse to the generated_ui_jsons folder)";
        }
        return "Target: " + UiConfigRepository.displayPath(target);
    }

    private void confirmSaveDialog() {
        String id = saveDialogId();
        if (id.isEmpty()) {
            saveDialogStatus("Enter a file name.", ERROR_COLOR);
            return;
        }
        if (!UiConfigRepository.isValidId(id)) {
            saveDialogStatus("'" + id
                    + "' is not a valid file name - use letters, digits, '_', '-' or '.'.",
                    ERROR_COLOR);
            return;
        }
        Path directory = normalizedSaveDestination();
        if (directory == null) {
            saveDialogStatus(saveToContentPack
                    ? "Select or browse to an existing Point Blank content pack."
                    : "Browse to a valid generated_ui_jsons folder.", ERROR_COLOR);
            return;
        }
        Path target = directory.resolve(id + ".json");
        if (Files.isRegularFile(target) && !saveOverwritePending) {
            saveOverwritePending = true;
            saveDialogStatus("'" + target.getFileName()
                    + "' already exists - press Save again to overwrite it.", ERROR_COLOR);
            return;
        }
        saveOverwritePending = false;

        String previousId = model.id();
        String packName = saveToContentPack ? UiConfigRepository.contentPackNameOf(directory) : null;
        model.setId(id);
        JsonObject json = model.toJson();
        // UiConfigRepository validates the JSON with Point Blank's own parser and
        // registers it (which updates the live HUD) before writing the file.
        String error = saveToContentPack
                ? UiConfigRepository.saveToContentPackDirectory(model, directory)
                : UiConfigRepository.saveToGeneratedDirectory(model, directory);
        if (error != null) {
            model.setId(previousId);
            saveDialogStatus("Save failed: " + error, ERROR_COLOR);
            return;
        }
        sessionBaseline = json.deepCopy();
        sessionId = id;
        requestedId = id;
        sessionFromScratch = false;
        overwritePending = false;
        saveDialogOpen = false;
        if (saveToContentPack && packName != null) {
            sessionSourceName = packName;
        }
        status("Saved '" + id + ".json' to " + UiConfigRepository.displayPath(target)
                + " and applied it to the HUD.", OK_COLOR);
        queueRebuild();
        // Also refresh the panel now: a save can be confirmed with Enter, which
        // otherwise would leave the id box showing the old name until the next click.
        flushRebuild();
    }

    private int saveDialogLeft() {
        return Math.max(4, (width - SAVE_DIALOG_WIDTH) / 2);
    }

    private int saveDialogTop() {
        return Math.max(4, (height - SAVE_DIALOG_HEIGHT) / 2);
    }

    private int saveDestinationY() {
        return saveDialogTop() + 34;
    }

    private int saveDestinationWidth() {
        return (SAVE_DIALOG_WIDTH - 12 - 4) / 2;
    }

    private int saveGeneratedX() {
        return saveDialogLeft() + 6;
    }

    private int saveContentPackX() {
        return saveGeneratedX() + saveDestinationWidth() + 4;
    }

    private int saveNameY() {
        return saveDialogTop() + 60;
    }

    private int savePackHeaderY() {
        return saveDialogTop() + 82;
    }

    private int savePackListY() {
        return saveDialogTop() + 94;
    }

    private int savePackRowY(int row) {
        return savePackListY() + row * SAVE_PACK_ROW_HEIGHT;
    }

    private int savePackIndexAt(double mouseY) {
        int row = (int) Math.floor((mouseY - savePackListY()) / (double) SAVE_PACK_ROW_HEIGHT);
        return row >= 0 && row < SAVE_PACK_VISIBLE_ROWS ? savePackScroll + row : -1;
    }

    private int saveTargetY() {
        return saveDialogTop() + 164;
    }

    private int saveStatusY() {
        return saveDialogTop() + 178;
    }

    private int saveFooterY() {
        return saveDialogTop() + SAVE_DIALOG_HEIGHT - 28;
    }

    private int saveDialogSaveX() {
        return saveDialogLeft() + SAVE_DIALOG_WIDTH - 6 - SAVE_FOOTER_BUTTON_WIDTH;
    }

    private int saveDialogCancelX() {
        return saveDialogSaveX() - 4 - SAVE_FOOTER_BUTTON_WIDTH;
    }

    private int saveBrowseButtonX() {
        return saveDialogLeft() + SAVE_DIALOG_WIDTH - 6 - SAVE_BROWSE_BUTTON_WIDTH;
    }

    private int saveBrowseButtonY() {
        return saveTargetY() - 6;
    }

    private void setSaveDestination(boolean contentPack) {
        this.saveToContentPack = contentPack;
        this.saveOverwritePending = false;
        if (contentPack) {
            this.savePackNames = UiConfigRepository.contentPackNames();
            this.savePackSelected = 0;
            this.savePackScroll = 0;
            this.saveDestinationDirectory =
                    UiConfigRepository.contentPackDirectory(kind, selectedSavePack());
            saveDialogStatus(savePackNames.isEmpty()
                            ? "No content packs found in pointblank/ - use the generated folder instead."
                            : "Choose the Point Blank content pack that should receive the JSON.",
                    savePackNames.isEmpty() ? ERROR_COLOR : HINT_COLOR);
        } else {
            this.saveDestinationDirectory = UiConfigRepository.generatedDirectory(kind);
            saveDialogStatus("The JSON will be written to " + GeneratedUiStore.DIRECTORY_NAME + "/.",
                    HINT_COLOR);
        }
    }

    private void scrollSavePackList(int rows) {
        if (savePackNames.isEmpty()) {
            return;
        }
        savePackScroll = Mth.clamp(savePackScroll + rows, 0,
                Math.max(0, savePackNames.size() - SAVE_PACK_VISIBLE_ROWS));
        savePackSelected = Mth.clamp(savePackSelected, savePackScroll,
                Math.min(savePackNames.size() - 1, savePackScroll + SAVE_PACK_VISIBLE_ROWS - 1));
    }

    private void moveSavePackSelection(int delta) {
        if (savePackNames.isEmpty()) {
            return;
        }
        savePackSelected = Mth.clamp(savePackSelected + delta, 0, savePackNames.size() - 1);
        if (savePackSelected < savePackScroll) {
            savePackScroll = savePackSelected;
        } else if (savePackSelected >= savePackScroll + SAVE_PACK_VISIBLE_ROWS) {
            savePackScroll = savePackSelected - SAVE_PACK_VISIBLE_ROWS + 1;
        }
        saveOverwritePending = false;
    }

    private boolean handleSaveDialogKey(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            saveDialogOpen = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirmSaveDialog();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!saveFileName.isEmpty()) {
                saveFileName = saveFileName.substring(0, saveFileName.length() - 1);
                saveOverwritePending = false;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            setSaveDestination(!saveToContentPack);
            return true;
        }
        if (saveToContentPack && keyCode == GLFW.GLFW_KEY_UP) {
            moveSavePackSelection(-1);
            return true;
        }
        if (saveToContentPack && keyCode == GLFW.GLFW_KEY_DOWN) {
            moveSavePackSelection(1);
            return true;
        }
        if (saveToContentPack && keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scrollSavePackList(-SAVE_PACK_VISIBLE_ROWS);
            return true;
        }
        if (saveToContentPack && keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            scrollSavePackList(SAVE_PACK_VISIBLE_ROWS);
            return true;
        }
        // Swallow everything else so the editor behind the dialog never reacts.
        return true;
    }

    private boolean handleSaveDialogClick(double mouseX, double mouseY) {
        int left = saveDialogLeft();
        int top = saveDialogTop();
        if (!hit(mouseX, mouseY, left, top, SAVE_DIALOG_WIDTH, SAVE_DIALOG_HEIGHT)) {
            // Cancelling leaves the editor state (including unsaved changes) intact.
            saveDialogOpen = false;
            return true;
        }
        if (hit(mouseX, mouseY, saveGeneratedX(), saveDestinationY(), saveDestinationWidth(),
                NEW_DIALOG_CHOICE_HEIGHT)) {
            setSaveDestination(false);
            return true;
        }
        if (hit(mouseX, mouseY, saveContentPackX(), saveDestinationY(), saveDestinationWidth(),
                NEW_DIALOG_CHOICE_HEIGHT)) {
            setSaveDestination(true);
            return true;
        }
        if (saveToContentPack && mouseX >= left + 6 && mouseX < left + SAVE_DIALOG_WIDTH - 6) {
            int index = savePackIndexAt(mouseY);
            if (index >= 0 && index < savePackNames.size()) {
                savePackSelected = index;
                saveDestinationDirectory = UiConfigRepository.contentPackDirectory(
                        kind, savePackNames.get(index));
                saveOverwritePending = false;
                saveDialogStatus("Destination: " + UiConfigRepository.displayPath(
                        saveDestinationDirectory), HINT_COLOR);
                return true;
            }
        }
        if (hit(mouseX, mouseY, saveBrowseButtonX(), saveBrowseButtonY(), SAVE_BROWSE_BUTTON_WIDTH,
                NEW_DIALOG_CHOICE_HEIGHT)) {
            openSaveDestinationBrowser();
            return true;
        }
        if (hit(mouseX, mouseY, saveDialogSaveX(), saveFooterY(), SAVE_FOOTER_BUTTON_WIDTH,
                NEW_DIALOG_CHOICE_HEIGHT)) {
            confirmSaveDialog();
            return true;
        }
        if (hit(mouseX, mouseY, saveDialogCancelX(), saveFooterY(), SAVE_FOOTER_BUTTON_WIDTH,
                NEW_DIALOG_CHOICE_HEIGHT)) {
            saveDialogOpen = false;
            return true;
        }
        return true;
    }

    private void openSaveDestinationBrowser() {
        Path root;
        Path start;
        if (saveToContentPack) {
            root = UiConfigRepository.pointBlankRoot();
            if (!Files.isDirectory(root)) {
                saveDialogStatus("The pointblank/ content pack folder was not found.", ERROR_COLOR);
                return;
            }
            start = nearestExistingDirectory(normalizedSaveDestination(), root);
        } else {
            root = UiConfigRepository.gameDirectory();
            start = nearestExistingDirectory(normalizedSaveDestination(), root);
        }
        this.browserPurpose = BrowserPurpose.SAVE_DESTINATION;
        this.browserRoot = root.toAbsolutePath().normalize();
        this.textureBrowserSetter = null;
        this.textureBrowserLabel = "";
        this.browserOpen = true;
        navigateBrowserTo(start);
    }

    private void openTextureBrowser(String label, Consumer<String> setter) {
        Path root = UiConfigRepository.pointBlankRoot();
        if (!Files.isDirectory(root)) {
            status("Point Blank content pack folder not found: "
                    + UiConfigRepository.displayPath(root), ERROR_COLOR);
            return;
        }
        this.browserPurpose = BrowserPurpose.TEXTURE;
        this.browserRoot = root.toAbsolutePath().normalize();
        this.textureBrowserSetter = setter;
        this.textureBrowserLabel = label == null ? "Texture" : label;
        this.browserOpen = true;
        navigateBrowserTo(root);
    }

    private static Path nearestExistingDirectory(Path preferred, Path fallback) {
        Path current = preferred;
        while (current != null && !Files.isDirectory(current)) {
            current = current.getParent();
        }
        return current == null ? fallback : current;
    }

    private void navigateBrowserTo(Path directory) {
        if (browserRoot == null) {
            return;
        }
        Path normalized = directory == null ? browserRoot : directory.toAbsolutePath().normalize();
        if (!normalized.startsWith(browserRoot)) {
            normalized = browserRoot;
        }
        if (!Files.isDirectory(normalized)) {
            browserStatus("That folder cannot be opened.", ERROR_COLOR);
            return;
        }
        this.browserDirectory = normalized;
        this.browserSelected = 0;
        this.browserScroll = 0;
        browserStatus = browserPurpose == BrowserPurpose.TEXTURE
                ? "Select a PNG under assets/pointblank/textures."
                : "Choose a folder, then click Use Folder.";
        browserStatusColor = HINT_COLOR;
        loadBrowserEntries();
    }

    private void loadBrowserEntries() {
        List<BrowserEntry> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(browserDirectory)) {
            for (Path child : stream) {
                String name = child.getFileName() == null ? "" : child.getFileName().toString();
                try {
                    if (Files.isDirectory(child)) {
                        entries.add(new BrowserEntry(child, name, true));
                    } else if (browserPurpose == BrowserPurpose.TEXTURE && isPng(name)
                            && Files.isRegularFile(child)) {
                        entries.add(new BrowserEntry(child, name, false));
                    }
                } catch (RuntimeException ignored) {
                    // A single unreadable entry must not break the whole listing.
                }
            }
        } catch (IOException | SecurityException exception) {
            this.browserEntries = List.of();
            this.browserStatus = "Cannot read folder: " + exception.getMessage();
            this.browserStatusColor = ERROR_COLOR;
            return;
        }
        entries.sort(Comparator.comparing(BrowserEntry::directory).reversed()
                .thenComparing(BrowserEntry::name, String.CASE_INSENSITIVE_ORDER));
        this.browserEntries = entries;
        this.browserSelected = Mth.clamp(browserSelected, 0, Math.max(0, entries.size() - 1));
        this.browserScroll = Mth.clamp(browserScroll, 0,
                Math.max(0, entries.size() - BROWSE_VISIBLE_ROWS));
    }

    private static boolean isPng(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(".png");
    }

    private void closeBrowser() {
        this.browserOpen = false;
        this.browserDirectory = null;
        this.browserRoot = null;
        this.browserEntries = List.of();
        this.browserSelected = 0;
        this.browserScroll = 0;
        this.browserStatus = "";
        this.browserStatusColor = HINT_COLOR;
        this.textureBrowserSetter = null;
        this.textureBrowserLabel = "";
        this.createFolderDialogOpen = false;
        this.createFolderName = "";
        this.createFolderStatus = "";
        this.createFolderStatusColor = HINT_COLOR;
    }

    private boolean canBrowserGoUp() {
        return browserDirectory != null && browserRoot != null
                && !browserDirectory.equals(browserRoot)
                && browserDirectory.getParent() != null
                && browserDirectory.getParent().startsWith(browserRoot);
    }

    private void browserGoUp() {
        if (!canBrowserGoUp()) {
            return;
        }
        navigateBrowserTo(browserDirectory.getParent());
    }

    /** True only when the texture browser is inside an existing content pack. */
    private boolean canCreateFolderHere() {
        if (browserPurpose != BrowserPurpose.TEXTURE
                || browserDirectory == null || browserRoot == null) {
            return false;
        }
        Path directory = browserDirectory.toAbsolutePath().normalize();
        Path pointBlank = UiConfigRepository.pointBlankRoot();
        if (!directory.startsWith(browserRoot) || !directory.startsWith(pointBlank)
                || directory.equals(pointBlank) || !Files.isDirectory(directory)
                || !Files.isWritable(directory)) {
            return false;
        }
        Path relative = pointBlank.relativize(directory);
        if (relative.getNameCount() == 0) {
            return false;
        }
        Path pack = pointBlank.resolve(relative.getName(0).toString()).normalize();
        return Files.isDirectory(pack.resolve("assets/pointblank")) && directory.startsWith(pack);
    }

    private void openCreateFolderDialog() {
        if (!canCreateFolderHere()) {
            browserStatus("Open a content pack folder before creating folders.", ERROR_COLOR);
            return;
        }
        this.createFolderDialogOpen = true;
        this.createFolderName = "";
        this.createFolderStatus = "Inside: "
                + UiConfigRepository.displayPath(browserDirectory);
        this.createFolderStatusColor = HINT_COLOR;
    }

    private static String validateFolderName(String name) {
        if (name == null || name.isBlank()) {
            return "Enter a folder name.";
        }
        if (name.length() > 96) {
            return "Folder name is too long.";
        }
        if (name.equals(".") || name.equals("..")) {
            return "'" + name + "' is not a valid folder name.";
        }
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (character < ' ' || character == 0x7F
                    || character == '/' || character == '\\' || character == ':'
                    || character == '*' || character == '?' || character == '"'
                    || character == '<' || character == '>' || character == '|') {
                return "Folder name contains an invalid character.";
            }
        }
        if (name.endsWith(".") || name.endsWith(" ")) {
            return "Folder name cannot end with a dot or space.";
        }
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.equals("CON") || upper.equals("PRN") || upper.equals("AUX")
                || upper.equals("NUL") || upper.matches("COM[1-9]")
                || upper.matches("LPT[1-9]")) {
            return "That name is reserved by the filesystem.";
        }
        return null;
    }

    private static boolean isFolderNameCharacter(char character) {
        return character >= ' ' && character != 0x7F
                && character != '/' && character != '\\' && character != ':'
                && character != '*' && character != '?' && character != '"'
                && character != '<' && character != '>' && character != '|';
    }

    private void confirmCreateFolder() {
        String rawName = createFolderName == null ? "" : createFolderName;
        if (rawName.isBlank()) {
            this.createFolderStatus = "Enter a folder name.";
            this.createFolderStatusColor = ERROR_COLOR;
            return;
        }
        String name = rawName.trim();
        if (!rawName.equals(name)) {
            this.createFolderStatus = "Folder name cannot start or end with a space.";
            this.createFolderStatusColor = ERROR_COLOR;
            return;
        }
        String error = validateFolderName(name);
        if (error != null) {
            this.createFolderStatus = error;
            this.createFolderStatusColor = ERROR_COLOR;
            return;
        }
        if (!canCreateFolderHere()) {
            this.createFolderStatus = "This folder is not inside a writable content pack.";
            this.createFolderStatusColor = ERROR_COLOR;
            return;
        }
        Path base = browserDirectory.toAbsolutePath().normalize();
        Path target = base.resolve(name).normalize();
        if (!target.getParent().equals(base) || !target.startsWith(browserRoot)) {
            this.createFolderStatus = "That name would create a folder outside the content pack.";
            this.createFolderStatusColor = ERROR_COLOR;
            return;
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            this.createFolderStatus = "A folder or file with that name already exists.";
            this.createFolderStatusColor = ERROR_COLOR;
            return;
        }
        try {
            Files.createDirectory(target);
        } catch (FileAlreadyExistsException exception) {
            this.createFolderStatus = "A folder or file with that name already exists.";
            this.createFolderStatusColor = ERROR_COLOR;
            return;
        } catch (IOException | SecurityException exception) {
            this.createFolderStatus = "Could not create folder: " + exception.getMessage();
            this.createFolderStatusColor = ERROR_COLOR;
            return;
        }

        this.createFolderDialogOpen = false;
        this.createFolderName = "";
        loadBrowserEntries();
        for (int index = 0; index < browserEntries.size(); index++) {
            if (browserEntries.get(index).name().equalsIgnoreCase(name)) {
                this.browserSelected = index;
                if (browserSelected < browserScroll) {
                    browserScroll = browserSelected;
                } else if (browserSelected >= browserScroll + BROWSE_VISIBLE_ROWS) {
                    browserScroll = browserSelected - BROWSE_VISIBLE_ROWS + 1;
                }
                break;
            }
        }
        browserStatus("Created folder '" + name + "'. Click it to open.", OK_COLOR);
    }

    private void browserStatus(String message, int color) {
        this.browserStatus = message == null ? "" : message;
        this.browserStatusColor = color;
    }

    private int createFolderLeft() {
        return Math.max(4, (width - CREATE_FOLDER_DIALOG_WIDTH) / 2);
    }

    private int createFolderTop() {
        return Math.max(4, (height - CREATE_FOLDER_DIALOG_HEIGHT) / 2);
    }

    private int createFolderNameY() {
        return createFolderTop() + 24;
    }

    private int createFolderStatusY() {
        return createFolderTop() + 48;
    }

    private int createFolderFooterY() {
        return createFolderTop() + CREATE_FOLDER_DIALOG_HEIGHT - 30;
    }

    private int createFolderCreateX() {
        return createFolderLeft() + CREATE_FOLDER_DIALOG_WIDTH - 6 - SAVE_FOOTER_BUTTON_WIDTH;
    }

    private int createFolderCancelX() {
        return createFolderCreateX() - 4 - SAVE_FOOTER_BUTTON_WIDTH;
    }

    private void renderCreateFolderDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = createFolderLeft();
        int top = createFolderTop();
        int right = left + CREATE_FOLDER_DIALOG_WIDTH;
        int bottom = top + CREATE_FOLDER_DIALOG_HEIGHT;
        graphics.fill(0, 0, width, height, DIALOG_SCRIM);
        graphics.fill(left, top, right, bottom, DIALOG_BACKGROUND);
        graphics.outline(left, top, CREATE_FOLDER_DIALOG_WIDTH, CREATE_FOLDER_DIALOG_HEIGHT,
                DIALOG_BORDER);
        graphics.text(font, "Create folder", left + 6, top + 8, DIALOG_TITLE, false);

        int nameY = createFolderNameY();
        graphics.text(font, "Name", left + 6, nameY + 4, LABEL_COLOR, false);
        int fieldLeft = left + 52;
        int fieldRight = right - 6;
        graphics.fill(fieldLeft, nameY, fieldRight, nameY + 16, DIALOG_FIELD);
        graphics.outline(fieldLeft, nameY, fieldRight - fieldLeft, 16, DIALOG_FIELD_BORDER);
        String shownName = fitTail(createFolderName, fieldRight - fieldLeft - 6);
        graphics.text(font, shownName, fieldLeft + 3, nameY + 4, TITLE_COLOR, false);
        graphics.text(font, "_", fieldLeft + 3 + font.width(shownName), nameY + 4,
                SELECTED_COLOR, false);

        graphics.text(font, fitText(createFolderStatus, CREATE_FOLDER_DIALOG_WIDTH - 12),
                left + 6, createFolderStatusY(), createFolderStatusColor, false);

        drawDialogChoice(graphics, mouseX, mouseY, createFolderCreateX(), createFolderFooterY(),
                SAVE_FOOTER_BUTTON_WIDTH, "Create", false, true);
        drawDialogChoice(graphics, mouseX, mouseY, createFolderCancelX(), createFolderFooterY(),
                SAVE_FOOTER_BUTTON_WIDTH, "Cancel", false, true);
        graphics.text(font, "Enter creates - Esc cancels", left + 6, bottom - 11,
                HINT_COLOR, false);
    }

    private boolean handleCreateFolderClick(double mouseX, double mouseY) {
        int left = createFolderLeft();
        int top = createFolderTop();
        if (!hit(mouseX, mouseY, left, top, CREATE_FOLDER_DIALOG_WIDTH,
                CREATE_FOLDER_DIALOG_HEIGHT)) {
            // Closing the naming dialog must not close the texture browser.
            createFolderDialogOpen = false;
            return true;
        }
        if (hit(mouseX, mouseY, createFolderCreateX(), createFolderFooterY(),
                SAVE_FOOTER_BUTTON_WIDTH, NEW_DIALOG_CHOICE_HEIGHT)) {
            confirmCreateFolder();
            return true;
        }
        if (hit(mouseX, mouseY, createFolderCancelX(), createFolderFooterY(),
                SAVE_FOOTER_BUTTON_WIDTH, NEW_DIALOG_CHOICE_HEIGHT)) {
            createFolderDialogOpen = false;
            return true;
        }
        return true;
    }

    private boolean handleCreateFolderKey(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            createFolderDialogOpen = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirmCreateFolder();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!createFolderName.isEmpty()) {
                createFolderName = createFolderName.substring(0, createFolderName.length() - 1);
            }
            return true;
        }
        return true;
    }

    private int browserPathY() {
        return saveDialogTop() + 22;
    }


    private int browserUpY() {
        return saveDialogTop() + 34;
    }

    private int browserListY() {
        return saveDialogTop() + 56;
    }

    private int browserRowY(int row) {
        return browserListY() + row * BROWSE_ROW_HEIGHT;
    }

    private int browserStatusY() {
        return saveDialogTop() + 176;
    }

    private int browserUpX() {
        return saveDialogLeft() + SAVE_DIALOG_WIDTH - 6 - BROWSE_UP_BUTTON_WIDTH;
    }

    private int browserRowAt(double mouseY) {
        int row = (int) Math.floor((mouseY - browserListY()) / (double) BROWSE_ROW_HEIGHT);
        return row >= 0 && row < BROWSE_VISIBLE_ROWS ? browserScroll + row : -1;
    }

    private BrowserEntry selectedBrowserEntry() {
        if (browserSelected < 0 || browserSelected >= browserEntries.size()) {
            return null;
        }
        return browserEntries.get(browserSelected);
    }

    private void scrollBrowserList(int rows) {
        int max = Math.max(0, browserEntries.size() - BROWSE_VISIBLE_ROWS);
        browserScroll = Mth.clamp(browserScroll + rows, 0, max);
        browserSelected = Mth.clamp(browserSelected, browserScroll,
                Math.min(browserEntries.size() - 1, browserScroll + BROWSE_VISIBLE_ROWS - 1));
    }

    private void moveBrowserSelection(int delta) {
        if (browserEntries.isEmpty()) {
            return;
        }
        browserSelected = Mth.clamp(browserSelected + delta, 0, browserEntries.size() - 1);
        if (browserSelected < browserScroll) {
            browserScroll = browserSelected;
        } else if (browserSelected >= browserScroll + BROWSE_VISIBLE_ROWS) {
            browserScroll = browserSelected - BROWSE_VISIBLE_ROWS + 1;
        }
    }

    private Path normalizeDestination(Path directory) {
        return saveToContentPack
                ? UiConfigRepository.normalizeContentPackDestination(kind, directory)
                : UiConfigRepository.normalizeGeneratedDestination(kind, directory);
    }

    private void applyBrowserSelection() {
        if (browserPurpose == BrowserPurpose.SAVE_DESTINATION) {
            Path normalized = normalizeDestination(browserDirectory);
            if (normalized == null) {
                browserStatus(saveToContentPack
                        ? "Choose an existing pack or its assets/pointblank/ui folder."
                        : "Choose the generated_ui_jsons folder.", ERROR_COLOR);
                return;
            }
            this.saveDestinationDirectory = normalized;
            this.saveOverwritePending = false;
            if (saveToContentPack) {
                String packName = UiConfigRepository.contentPackNameOf(normalized);
                int index = savePackNames.indexOf(packName);
                if (index >= 0) {
                    savePackSelected = index;
                }
            }
            closeBrowser();
            saveDialogStatus("Destination: " + UiConfigRepository.displayPath(normalized),
                    OK_COLOR);
            return;
        }

        BrowserEntry entry = selectedBrowserEntry();
        if (entry == null || entry.directory()) {
            browserStatus("Select a PNG texture file.", ERROR_COLOR);
            return;
        }
        String resourcePath = UiConfigRepository.textureResourcePath(entry.path());
        if (resourcePath == null) {
            browserStatus("That PNG is not a usable resource path. Use "
                    + "assets/<namespace>/textures and lowercase resource characters.", ERROR_COLOR);
            return;
        }
        Consumer<String> setter = textureBrowserSetter;
        String label = textureBrowserLabel;
        closeBrowser();
        if (setter != null) {
            setter.accept(resourcePath);
            changed();
            String selectionName = "Texture".equalsIgnoreCase(label)
                    ? "texture" : label + " texture";
            Identifier textureId = UiPreviewLayout.texture(resourcePath);
            boolean loaded = false;
            if (textureId != null) {
                try {
                    loaded = Minecraft.getInstance().getResourceManager()
                            .getResource(textureId).isPresent();
                } catch (RuntimeException ignored) {
                    // The path is still valid; the preview uses its placeholder.
                }
            }
            String message = "Selected " + selectionName + ": " + resourcePath;
            if (!loaded) {
                message += " (not loaded yet - reload resources to preview it)";
            }
            status(message, loaded ? OK_COLOR : HINT_COLOR);
        }
        queueRebuild();
        flushRebuild();
    }

    private boolean handleBrowserClick(double mouseX, double mouseY) {
        int left = saveDialogLeft();
        int top = saveDialogTop();
        if (!hit(mouseX, mouseY, left, top, SAVE_DIALOG_WIDTH, SAVE_DIALOG_HEIGHT)) {
            // Cancelling only closes the browser; the save dialog and its file
            // name/settings stay exactly as they were.
            closeBrowser();
            return true;
        }
        if (hit(mouseX, mouseY, browserUpX(), browserUpY(), BROWSE_UP_BUTTON_WIDTH,
                NEW_DIALOG_CHOICE_HEIGHT)) {
            browserGoUp();
            return true;
        }
        if (mouseX >= left + 6 && mouseX < left + SAVE_DIALOG_WIDTH - 6) {
            int index = browserRowAt(mouseY);
            if (index >= 0 && index < browserEntries.size()) {
                BrowserEntry entry = browserEntries.get(index);
                browserSelected = index;
                if (entry.directory()) {
                    navigateBrowserTo(entry.path());
                } else {
                    browserStatus("Selected: " + entry.name(), HINT_COLOR);
                }
                return true;
            }
        }
        if (hit(mouseX, mouseY, saveDialogSaveX(), saveFooterY(), SAVE_FOOTER_BUTTON_WIDTH,
                NEW_DIALOG_CHOICE_HEIGHT)) {
            applyBrowserSelection();
            return true;
        }
        if (browserPurpose == BrowserPurpose.TEXTURE
                && hit(mouseX, mouseY, left + 6, saveFooterY(), BROWSE_CREATE_BUTTON_WIDTH,
                NEW_DIALOG_CHOICE_HEIGHT)) {
            openCreateFolderDialog();
            return true;
        }
        if (hit(mouseX, mouseY, saveDialogCancelX(), saveFooterY(), SAVE_FOOTER_BUTTON_WIDTH,
                NEW_DIALOG_CHOICE_HEIGHT)) {
            closeBrowser();
            return true;
        }
        return true;
    }

    private boolean handleBrowserKey(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeBrowser();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            BrowserEntry entry = selectedBrowserEntry();
            if (browserPurpose == BrowserPurpose.TEXTURE && entry != null && entry.directory()) {
                navigateBrowserTo(entry.path());
            } else {
                applyBrowserSelection();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            browserGoUp();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            moveBrowserSelection(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            moveBrowserSelection(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scrollBrowserList(-BROWSE_VISIBLE_ROWS);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            scrollBrowserList(BROWSE_VISIBLE_ROWS);
            return true;
        }
        // Swallow everything else so the editor behind the browser never reacts.
        return true;
    }

    private String fitText(String value, int maxWidth) {
        if (value == null) {
            return "";
        }
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String result = value;
        while (result.length() > 3 && font.width(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    /** Truncates the start of a single line so the end (a file name) stays readable. */
    private String fitTail(String value, int maxWidth) {
        if (value == null) {
            return "";
        }
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String result = value;
        while (result.length() > 3 && font.width("..." + result) > maxWidth) {
            result = result.substring(1);
        }
        return "..." + result;
    }

    private void renderBrowser(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = saveDialogLeft();
        int top = saveDialogTop();
        int right = left + SAVE_DIALOG_WIDTH;
        int bottom = top + SAVE_DIALOG_HEIGHT;
        graphics.fill(0, 0, width, height, DIALOG_SCRIM);
        graphics.fill(left, top, right, bottom, DIALOG_BACKGROUND);
        graphics.outline(left, top, SAVE_DIALOG_WIDTH, SAVE_DIALOG_HEIGHT, DIALOG_BORDER);

        String title = browserPurpose == BrowserPurpose.TEXTURE
                ? ("Texture".equalsIgnoreCase(textureBrowserLabel)
                        ? "Choose texture" : "Choose " + textureBrowserLabel + " texture")
                : "Choose save folder";
        graphics.text(font, title, left + 6, top + 8, DIALOG_TITLE, false);

        int visible = Math.min(BROWSE_VISIBLE_ROWS,
                browserEntries.size() - browserScroll);
        if (!browserEntries.isEmpty()) {
            String count = (browserScroll + 1) + "-" + (browserScroll + visible)
                    + " of " + browserEntries.size();
            graphics.text(font, count, right - 6 - font.width(count), top + 8,
                    HINT_COLOR, false);
        }

        String path = fitTail("Folder: " + UiConfigRepository.displayPath(browserDirectory),
                SAVE_DIALOG_WIDTH - 12 - BROWSE_UP_BUTTON_WIDTH - 4);
        graphics.text(font, path, left + 6, browserPathY(), HINT_COLOR, false);
        drawDialogChoice(graphics, mouseX, mouseY, browserUpX(), browserUpY(),
                BROWSE_UP_BUTTON_WIDTH, "Up", false, canBrowserGoUp());

        if (browserEntries.isEmpty()) {
            String message = browserPurpose == BrowserPurpose.TEXTURE
                    ? "No PNG textures or subfolders here."
                    : "No subfolders here.";
            graphics.text(font, message, left + 6, browserListY() + 4, HINT_COLOR, false);
        } else {
            for (int row = 0; row < visible; row++) {
                int index = browserScroll + row;
                BrowserEntry entry = browserEntries.get(index);
                int y = browserRowY(row);
                boolean selected = index == browserSelected;
                boolean hovered = hit(mouseX, mouseY, left + 6, y,
                        SAVE_DIALOG_WIDTH - 12, BROWSE_ROW_HEIGHT);
                if (selected || hovered) {
                    graphics.fill(left + 6, y, right - 6, y + BROWSE_ROW_HEIGHT,
                            selected ? DIALOG_CHOICE_SELECTED : DIALOG_CHOICE);
                }
                String name = entry.directory() ? entry.name() + "/" : entry.name();
                int color = selected ? SELECTED_COLOR
                        : entry.directory() ? SECTION_COLOR : TITLE_COLOR;
                graphics.text(font, fitText(name, SAVE_DIALOG_WIDTH - 24),
                        left + 9, y + 3, color, false);
            }
        }

        graphics.text(font, fitText(browserStatus, SAVE_DIALOG_WIDTH - 12),
                left + 6, browserStatusY(), browserStatusColor, false);

        BrowserEntry selectedEntry = selectedBrowserEntry();
        boolean canSelect = browserPurpose == BrowserPurpose.TEXTURE
                ? selectedEntry != null && !selectedEntry.directory()
                : normalizeDestination(browserDirectory) != null;
        String selectLabel = browserPurpose == BrowserPurpose.TEXTURE ? "Select" : "Use Folder";
        if (browserPurpose == BrowserPurpose.TEXTURE) {
            drawDialogChoice(graphics, mouseX, mouseY, left + 6, saveFooterY(),
                    BROWSE_CREATE_BUTTON_WIDTH, "Create Folder", false, canCreateFolderHere());
        }
        drawDialogChoice(graphics, mouseX, mouseY, saveDialogSaveX(), saveFooterY(),
                SAVE_FOOTER_BUTTON_WIDTH, selectLabel, false, canSelect);
        drawDialogChoice(graphics, mouseX, mouseY, saveDialogCancelX(), saveFooterY(),
                SAVE_FOOTER_BUTTON_WIDTH, "Cancel", false, true);
        graphics.text(font, "Click a folder to open it - Enter selects - Esc cancels",
                left + 6, bottom - 11, HINT_COLOR, false);
    }

    private void renderSaveDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = saveDialogLeft();
        int top = saveDialogTop();
        int right = left + SAVE_DIALOG_WIDTH;
        int bottom = top + SAVE_DIALOG_HEIGHT;
        graphics.fill(0, 0, width, height, DIALOG_SCRIM);
        graphics.fill(left, top, right, bottom, DIALOG_BACKGROUND);
        graphics.outline(left, top, SAVE_DIALOG_WIDTH, SAVE_DIALOG_HEIGHT, DIALOG_BORDER);

        graphics.text(font, "Save UI JSON", left + 6, top + 8, DIALOG_TITLE, false);
        String summary = fitTail(kind.displayName() + "  " + model.id(), SAVE_DIALOG_WIDTH / 2 - 12);
        graphics.text(font, summary, right - 6 - font.width(summary), top + 8, HINT_COLOR, false);

        graphics.text(font, "Destination", left + 6, top + 22, LABEL_COLOR, false);
        drawDialogChoice(graphics, mouseX, mouseY, saveGeneratedX(), saveDestinationY(),
                saveDestinationWidth(), "Generated UI JSONs", !saveToContentPack, true);
        drawDialogChoice(graphics, mouseX, mouseY, saveContentPackX(), saveDestinationY(),
                saveDestinationWidth(), "Content Pack", saveToContentPack, true);

        int nameY = saveNameY();
        graphics.text(font, "File name", left + 6, nameY + 4, LABEL_COLOR, false);
        int fieldLeft = left + SAVE_NAME_FIELD_LEFT;
        int fieldRight = right - 6;
        graphics.fill(fieldLeft, nameY, fieldRight, nameY + 16, DIALOG_FIELD);
        graphics.outline(fieldLeft, nameY, fieldRight - fieldLeft, 16, DIALOG_FIELD_BORDER);
        String shownName = fitTail(saveFileName, fieldRight - fieldLeft - 6);
        graphics.text(font, shownName, fieldLeft + 3, nameY + 4, TITLE_COLOR, false);
        graphics.text(font, "_", fieldLeft + 3 + font.width(shownName), nameY + 4,
                SELECTED_COLOR, false);

        if (saveToContentPack) {
            graphics.text(font, "Content pack", left + 6, savePackHeaderY(), LABEL_COLOR, false);
            if (savePackNames.isEmpty()) {
                graphics.text(font, "No content packs found in pointblank/.",
                        left + 6, savePackListY() + 4, ERROR_COLOR, false);
                graphics.text(font, "Use the generated folder, or add a pack and reopen Save.",
                        left + 6, savePackListY() + 16, HINT_COLOR, false);
            } else {
                String count = (savePackSelected + 1) + "/" + savePackNames.size();
                graphics.text(font, count, right - 6 - font.width(count), savePackHeaderY(),
                        HINT_COLOR, false);
                int visible = Math.min(SAVE_PACK_VISIBLE_ROWS,
                        savePackNames.size() - savePackScroll);
                for (int row = 0; row < visible; row++) {
                    int index = savePackScroll + row;
                    int y = savePackRowY(row);
                    boolean selected = index == savePackSelected;
                    boolean hovered = hit(mouseX, mouseY, left + 6, y,
                            SAVE_DIALOG_WIDTH - 12, SAVE_PACK_ROW_HEIGHT);
                    if (selected || hovered) {
                        graphics.fill(left + 6, y, right - 6, y + SAVE_PACK_ROW_HEIGHT,
                                selected ? DIALOG_CHOICE_SELECTED : DIALOG_CHOICE);
                    }
                    graphics.text(font, fitText(savePackNames.get(index), SAVE_DIALOG_WIDTH - 24),
                            left + 9, y + 3, selected ? SELECTED_COLOR : TITLE_COLOR, false);
                }
                if (savePackNames.size() > SAVE_PACK_VISIBLE_ROWS) {
                    graphics.text(font, (savePackScroll + 1) + "-" + (savePackScroll + visible)
                                    + " of " + savePackNames.size(),
                            left + 6, savePackRowY(SAVE_PACK_VISIBLE_ROWS) + 1, HINT_COLOR, false);
                }
            }
        } else {
            graphics.text(font, "Writes into the existing generated UI folder.",
                    left + 6, savePackListY() + 4, HINT_COLOR, false);
            graphics.text(font, "Point Blank uses the JSON immediately after saving.",
                    left + 6, savePackListY() + 16, HINT_COLOR, false);
            graphics.text(font, "Existing files ask for a second Save click.",
                    left + 6, savePackListY() + 28, HINT_COLOR, false);
        }

        graphics.text(font, fitTail(saveDialogTargetText(),
                        SAVE_DIALOG_WIDTH - 12 - SAVE_BROWSE_BUTTON_WIDTH - 4),
                left + 6, saveTargetY(), HINT_COLOR, false);
        drawDialogChoice(graphics, mouseX, mouseY, saveBrowseButtonX(), saveBrowseButtonY(),
                SAVE_BROWSE_BUTTON_WIDTH, "Browse...", false, true);
        graphics.text(font, fitText(saveDialogStatus, SAVE_DIALOG_WIDTH - 12),
                left + 6, saveStatusY(), saveDialogStatusColor, false);

        boolean ready = UiConfigRepository.isValidId(saveDialogId())
                && normalizedSaveDestination() != null;
        drawDialogChoice(graphics, mouseX, mouseY, saveDialogSaveX(), saveFooterY(),
                SAVE_FOOTER_BUTTON_WIDTH, "Save", false, ready);
        drawDialogChoice(graphics, mouseX, mouseY, saveDialogCancelX(), saveFooterY(),
                SAVE_FOOTER_BUTTON_WIDTH, "Cancel", false, true);
        graphics.text(font, "Enter saves - Esc cancels - Tab switches destination",
                left + 6, bottom - 11, HINT_COLOR, false);
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
