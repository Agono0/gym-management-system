package com.gym.controller;

import com.gym.model.Equipment;
import com.gym.model.EquipmentCondition;
import com.gym.model.EquipmentStatus;
import com.gym.dao.EquipmentDao;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.common.M3Tooltip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.scene3d.M3ModelViewer3D;
import io.m3fx.controls.scene3d.M3ModelViewer3D.ModelType;
import io.m3fx.controls.selection.M3Switch;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Equipment Inventory &amp; 3D Diagnostics screen on real {@link EquipmentDao} data (C5).
 *
 * <p>Conforms to docs/ui/facility/EQUIPMENT_INVENTORY_UI.md Section 7 and the
 * {@code com.gym.ui} foundation contracts (app-ui-foundation.md): the ledger is an
 * {@link M3DataTable} loaded through {@link Data#loadAsync} (search/sort/pagination come from
 * the table itself), the inspector chips drive {@link EquipmentDao#update}, "Log Today's
 * Maintenance" is one atomic update (status IN_MAINTENANCE + lastMaintenance = today, rule 13
 * reading: the two fields change together, not as independent hand edits), Delete is the DAO's
 * hard delete behind the confirm-dialog contract (the Equipment model has no INACTIVE-like
 * status; see app-ui-foundation.md §8), and Export writes a real CSV whose absolute path is
 * announced in a snackbar. The 3D twin is presentational: it binds the selected row's name and
 * maps the category to a display model.
 *
 * <p>The node tree is built in the constructor — the programmatic replacement for the former
 * equipment.fxml. fx:id parity: every id the markup used to tag keeps its JavaFX id on the
 * replacement node (the condition/status radio ids now sit on the selection chips; the stock
 * TableView column ids are gone with the stock TableView itself).
 */
public class EquipmentController implements ScreenController {

    // ---- View fields (the former fx:id injections, now built by the constructor) ----
    private StackPane viewRoot;
    private M3DataTable<Equipment> equipmentTableView;
    private Label selectedModelTitleLabel;
    private M3ModelViewer3D modelViewer3D;
    private M3Switch wireframeToggleSwitch;
    private M3Chip conditionNewChip;
    private M3Chip conditionGoodChip;
    private M3Chip conditionFairChip;
    private M3Chip conditionPoorChip;
    private M3Chip statusAvailChip;
    private M3Chip statusMaintChip;
    private M3Chip statusRetireChip;
    private Label assetCountLabel;
    private M3Button logMaintenanceBtn;
    private M3Button deleteBtn;
    private M3SideSheet maintenanceSideSheet;
    private M3DataTable<String> maintenanceHistoryTable;
    /** Overlay host for snackbars and dialogs, installed on the screen root (no fx:id). */
    private M3Overlay overlay;

    private final ObjectProperty<Equipment> selected = new SimpleObjectProperty<>(this, "selected");
    /** Load/empty/error host for the ledger; the table stays mounted inside it. */
    private StackPane ledgerStateHost;
    /** Empty and error states for the ledger, shown instead of the (empty) table. */
    private StackPane stateOverlay;
    /** Guards chip-group writes while a selection apply is in flight. */
    private boolean applyingSelection;
    private List<Equipment> loaded = List.of();
    /** The active category filter; {@code null} = All Assets. */
    private String activeCategory;

    /** Real seeded categories: the filter chips and the zone mapping both key on these. */
    static final String[] EQUIPMENT_CATEGORIES = {"CARDIO", "STRENGTH", "FREE WEIGHTS", "RECOVERY"};

    /** Creates the screen, builds the tree and starts the first real data load. */
    public EquipmentController() {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("equipment-layout");

        // Top app bar: title + add action (search lives in the ledger table itself, B2/B6).
        VBox topBarBox = new VBox(10);
        topBarBox.getStyleClass().add("top-bar-box");
        topBarBox.setPadding(new Insets(16, 24, 12, 24));

        HBox headerBox = new HBox(16);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getStyleClass().add("app-header-box");
        M3Button addBtn = new M3Button("+ Add Equipment", ButtonVariant.FILLED);
        addBtn.getStyleClass().add("filled-primary-btn");
        addBtn.setOnAction(e -> onAdd());
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(
                styledLabel("Equipment Inventory & 3D Diagnostics", "header-title-text"),
                headerSpacer,
                addBtn);

        // Category filter chips (one real category each; "All Assets" clears the filter).
        HBox filterStrip = new HBox(8);
        filterStrip.setAlignment(Pos.CENTER_LEFT);
        filterStrip.getStyleClass().add("category-filter-strip");
        M3Chip allAssetsChip = categoryChip("All Assets", "filterAllAssetsChip", null);
        allAssetsChip.setSelected(true);
        M3Chip cardioChip = categoryChip("Cardio", null, "CARDIO");
        M3Chip strengthChip = categoryChip("Strength", null, "STRENGTH");
        M3Chip freeWeightsChip = categoryChip("Free Weights", null, "FREE WEIGHTS");
        M3Chip recoveryChip = categoryChip("Recovery", null, "RECOVERY");
        ToggleGroup categoryGroup = new ToggleGroup();
        categoryGroup.getToggles().addAll(
                allAssetsChip, cardioChip, strengthChip, freeWeightsChip, recoveryChip);
        filterStrip.getChildren().addAll(
                allAssetsChip, cardioChip, strengthChip, freeWeightsChip, recoveryChip);

        topBarBox.getChildren().addAll(headerBox, filterStrip);
        layout.setTop(topBarBox);

        // Center: master split pane (ledger left, 3D twin right).
        VBox ledgerPane = new VBox(10);
        ledgerPane.getStyleClass().add("table-container-box");

        equipmentTableView = new M3DataTable<>();
        equipmentTableView.getStyleClass().add("m3-data-table");
        equipmentTableView.setId("equipmentTableView");
        equipmentTableView.getColumns().setAll(
                new M3DataTable.Column<>("ID", e -> String.valueOf(e.getId()), 60),
                new M3DataTable.Column<>("Asset Name", e -> nvl(e.getName()), 200),
                new M3DataTable.Column<>("Category", e -> nvl(e.getCategory()), 130),
                new M3DataTable.Column<>("Condition", e -> labelOf(e.getCondition()), 110),
                new M3DataTable.Column<>("Status", e -> labelOf(e.getStatus()), 130),
                new M3DataTable.Column<>("Last Maint.", e -> nvl(e.getLastMaintenance()), 110));
        equipmentTableView.setPageSize(12);
        equipmentTableView.setOnRowAction(this::selectEquipment);
        VBox.setVgrow(equipmentTableView, Priority.ALWAYS);

        // Table footer: honest counts from the loaded rows (the fake "84" is gone).
        HBox tableFooter = new HBox(12);
        tableFooter.setAlignment(Pos.CENTER_LEFT);
        tableFooter.getStyleClass().add("table-footer-box");
        assetCountLabel = new Label("Loading equipment…");
        assetCountLabel.getStyleClass().add("footer-label");
        tableFooter.getChildren().add(assetCountLabel);

        // The table stays mounted through every load (loadAsync swaps inside this host);
        // empty/error states show in a sibling overlay so the restore snapshot stays intact.
        ledgerStateHost = new StackPane(new VBox(10, equipmentTableView, tableFooter));
        VBox.setVgrow(ledgerStateHost, Priority.ALWAYS);
        stateOverlay = new StackPane();
        stateOverlay.setVisible(false);
        VBox.setVgrow(stateOverlay, Priority.ALWAYS);
        ledgerPane.getChildren().addAll(ledgerStateHost, stateOverlay);

        VBox inspectorPane = new VBox(14);
        inspectorPane.getStyleClass().add("inspector-container-box");

        // 3D model viewport box.
        VBox viewportBox = new VBox(8);
        viewportBox.getStyleClass().add("model-3d-glass-container");
        VBox.setVgrow(viewportBox, Priority.ALWAYS);
        viewportBox.setPadding(new Insets(10, 14, 10, 14));
        HBox viewportToolbar = new HBox(10);
        viewportToolbar.setAlignment(Pos.CENTER_LEFT);
        viewportToolbar.getStyleClass().add("viewport-toolbar");
        selectedModelTitleLabel = new Label("Select an asset in the ledger");
        selectedModelTitleLabel.getStyleClass().add("model-title");
        wireframeToggleSwitch = new M3Switch();
        M3Button resetCamBtn = new M3Button("Reset Cam", ButtonVariant.OUTLINED);
        resetCamBtn.getStyleClass().add("text-btn-compact");
        resetCamBtn.setOnAction(e -> onResetCamera());
        Region viewportSpacer = new Region();
        HBox.setHgrow(viewportSpacer, Priority.ALWAYS);
        viewportToolbar.getChildren().addAll(
                selectedModelTitleLabel,
                viewportSpacer,
                styledLabel("Wireframe X-Ray", "switch-label"),
                wireframeToggleSwitch,
                resetCamBtn);
        modelViewer3D = new M3ModelViewer3D();
        modelViewer3D.setPrefSize(520, 360);
        VBox.setVgrow(modelViewer3D, Priority.ALWAYS);
        modelViewer3D.getStyleClass().add("viewport-3d-canvas");
        viewportBox.getChildren().addAll(viewportToolbar, modelViewer3D);

        // Detailed lifecycle & condition dock (selection-driven, DAO-backed).
        M3Card inspectorCard = new M3Card(CardVariant.ELEVATED);
        inspectorCard.getStyleClass().add("inspector-details-card");
        VBox detailsInner = new VBox(12);
        detailsInner.getStyleClass().add("details-card-inner");
        detailsInner.setPadding(new Insets(14, 16, 14, 16));
        detailsInner.getChildren().add(
                styledLabel("Asset State & Maintenance Controls", "card-section-title"));

        ToggleGroup conditionGroup = new ToggleGroup();
        ToggleGroup statusGroup = new ToggleGroup();

        HBox conditionRow = new HBox(16);
        conditionRow.setAlignment(Pos.CENTER_LEFT);
        conditionNewChip = selectionChip("New", EquipmentCondition.NEW.name(),
                "conditionNewRadio", conditionGroup, () -> applyCondition(EquipmentCondition.NEW));
        conditionGoodChip = selectionChip("Good", EquipmentCondition.GOOD.name(),
                "conditionGoodRadio", conditionGroup, () -> applyCondition(EquipmentCondition.GOOD));
        conditionFairChip = selectionChip("Fair", EquipmentCondition.FAIR.name(),
                "conditionFairRadio", conditionGroup, () -> applyCondition(EquipmentCondition.FAIR));
        conditionPoorChip = selectionChip("Poor", EquipmentCondition.POOR.name(),
                "conditionPoorRadio", conditionGroup, () -> applyCondition(EquipmentCondition.POOR));
        conditionRow.getChildren().addAll(
                styledLabel("Condition:", "dock-label"),
                conditionNewChip, conditionGoodChip, conditionFairChip, conditionPoorChip);

        HBox statusRow = new HBox(16);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusAvailChip = selectionChip("Available", EquipmentStatus.AVAILABLE.name(),
                "statusAvailRadio", statusGroup, () -> applyStatus(EquipmentStatus.AVAILABLE));
        statusMaintChip = selectionChip("In maintenance", EquipmentStatus.IN_MAINTENANCE.name(),
                "statusMaintRadio", statusGroup, () -> applyStatus(EquipmentStatus.IN_MAINTENANCE));
        statusRetireChip = selectionChip("Retired", EquipmentStatus.RETIRED.name(),
                "statusRetireRadio", statusGroup, () -> applyStatus(EquipmentStatus.RETIRED));
        statusRow.getChildren().addAll(
                styledLabel("Status:", "dock-label"),
                statusAvailChip, statusMaintChip, statusRetireChip);

        // Honest disabled state: every inspector control acts on the selected ledger row, so
        // without a selection they start disabled (library disabled styling + a tooltip
        // explaining why) instead of promising a write they cannot route.
        for (M3Chip inspectorChip : List.of(conditionNewChip, conditionGoodChip,
                conditionFairChip, conditionPoorChip, statusAvailChip, statusMaintChip,
                statusRetireChip)) {
            inspectorChip.setDisable(true);
            Tooltip.install(inspectorChip, M3Tooltip.plain(
                    "Select an equipment row first — the change writes to that row."));
        }

        // Quick maintenance logger CTA. Both dock actions (and the condition/status chips
        // above) act on the SELECTED ledger row, so they start disabled and carry a tooltip
        // explaining why — the enabled styling with nothing selected read as broken.
        HBox dockActions = new HBox(12);
        dockActions.setAlignment(Pos.CENTER_LEFT);
        M3Button logMaintenanceBtn = new M3Button("Log Today's Maintenance", ButtonVariant.TONAL);
        logMaintenanceBtn.setGraphic(M3Icon.symbol("edit"));
        logMaintenanceBtn.getStyleClass().add("tonal-button");
        logMaintenanceBtn.setDisable(true);
        logMaintenanceBtn.setOnAction(e -> handleLogMaintenanceToday());
        M3Button deleteBtn = new M3Button("Delete Item", ButtonVariant.TEXT);
        deleteBtn.setGraphic(M3Icon.symbol("delete"));
        deleteBtn.getStyleClass().add("danger-text-btn");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> handleDeleteSelected());
        Region dockSpacer = new Region();
        HBox.setHgrow(dockSpacer, Priority.ALWAYS);
        dockActions.getChildren().addAll(logMaintenanceBtn, dockSpacer, deleteBtn);
        Tooltip.install(logMaintenanceBtn, M3Tooltip.plain("Select an equipment row first."));
        Tooltip.install(deleteBtn, M3Tooltip.plain("Select an equipment row first."));

        detailsInner.getChildren().addAll(conditionRow, statusRow, dockActions);
        this.logMaintenanceBtn = logMaintenanceBtn;
        this.deleteBtn = deleteBtn;
        inspectorCard.setContent(detailsInner);
        inspectorPane.getChildren().addAll(viewportBox, inspectorCard);

        SplitPane workspaceSplit = new SplitPane(ledgerPane, inspectorPane);
        workspaceSplit.setDividerPositions(0.56);
        workspaceSplit.getStyleClass().add("bento-split-pane");
        workspaceSplit.setPadding(new Insets(8, 24, 48, 24));
        layout.setCenter(workspaceSplit);

        // Bottom: contextual action dock.
        HBox actionDock = new HBox(16);
        actionDock.setAlignment(Pos.CENTER_RIGHT);
        actionDock.getStyleClass().add("contextual-action-dock");
        actionDock.setPadding(new Insets(12, 24, 16, 24));
        M3Button exportBtn = new M3Button("Export Asset Ledger (CSV)", ButtonVariant.OUTLINED);
        exportBtn.getStyleClass().add("outlined-btn");
        exportBtn.setOnAction(e -> onExportCsv());
        M3Button refreshBtn = new M3Button("Refresh Inventory", ButtonVariant.TEXT);
        refreshBtn.getStyleClass().add("text-btn");
        refreshBtn.setOnAction(e -> onRefresh());
        Region bottomDockSpacer = new Region();
        HBox.setHgrow(bottomDockSpacer, Priority.ALWAYS);
        actionDock.getChildren().addAll(exportBtn, bottomDockSpacer, refreshBtn);
        layout.setBottom(actionDock);

        // SideSheet overlay for detailed asset history & maintenance ledger (kept as built; no
        // opener exists in the facility spec's skeleton, so it stays out of C5's data scope).
        maintenanceSideSheet = new M3SideSheet();
        maintenanceSideSheet.setVisible(false);
        maintenanceSideSheet.setManaged(false);
        StackPane.setAlignment(maintenanceSideSheet, Pos.CENTER_RIGHT);
        maintenanceSideSheet.getStyleClass().add("maintenance-side-sheet");
        VBox sheetContent = new VBox(12);
        maintenanceHistoryTable = new M3DataTable<>();
        VBox.setVgrow(maintenanceHistoryTable, Priority.ALWAYS);
        sheetContent.getChildren().addAll(
                styledLabel("Maintenance History Ledger", "title-medium"),
                maintenanceHistoryTable);
        maintenanceSideSheet.setContent(sheetContent);

        viewRoot = new StackPane();
        viewRoot.getStyleClass().add("equipment-root");

        // fx:id parity: every node the markup used to tag keeps its JavaFX id on the
        // replacement node (selection chips inherit the radio ids; the stock TableView column
        // ids are gone with the stock TableView — the ledger is an M3DataTable now).
        maintenanceSideSheet.setId("maintenanceSideSheet");
        maintenanceHistoryTable.setId("maintenanceHistoryTable");
        assetCountLabel.setId("assetCountLabel");
        selectedModelTitleLabel.setId("selectedModelTitleLabel");
        modelViewer3D.setId("modelViewer3D");
        wireframeToggleSwitch.setId("wireframeToggleSwitch");
        viewRoot.getChildren().addAll(layout, maintenanceSideSheet);
        overlay = UiKit.installFeedback(viewRoot);

        loadEquipment();
    }

    /** Null-safe cell text: blank values render as an em dash. */
    private static String nvl(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String labelOf(EquipmentCondition condition) {
        if (condition == null) {
            return "—";
        }
        return switch (condition) {
            case NEW -> "New";
            case GOOD -> "Good";
            case FAIR -> "Fair";
            case POOR -> "Poor";
        };
    }

    private static String labelOf(EquipmentStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case AVAILABLE -> "Available";
            case IN_MAINTENANCE -> "In maintenance";
            case RETIRED -> "Retired";
        };
    }

    /**
     * Builds one selection chip carrying the former radio's fx:id. The ToggleGroup provides the
     * radio semantics; the action applies the change to the selected equipment through the DAO.
     */
    private static M3Chip selectionChip(String text, String id, String fxId,
                                        ToggleGroup group, Runnable apply) {
        M3Chip chip = new M3Chip(text, ChipVariant.FILTER);
        chip.setId(fxId);
        chip.setToggleGroup(group);
        chip.getStyleClass().add("m3-selection-chip");
        chip.setOnAction(e -> {
            // Radio semantics: the acted-on chip is always the selected one (the ToggleGroup
            // deselects the siblings; the guard covers clicking the already-selected chip).
            if (!chip.isSelected()) {
                chip.setSelected(true);
            }
            apply.run();
        });
        return chip;
    }

    /** Radio semantics for the category filter: the acted-on chip stays selected. */
    private static void keepOneSelected(ToggleGroup group, M3Chip fallback) {
        for (javafx.scene.control.Toggle toggle : group.getToggles()) {
            if (toggle instanceof M3Chip chip) {
                chip.setOnAction(e -> {
                    if (!chip.isSelected()) {
                        chip.setSelected(true);
                    }
                    for (javafx.scene.control.Toggle other : group.getToggles()) {
                        other.setSelected(other == chip);
                    }
                });
            }
        }
        group.selectToggle(fallback);
    }

    /**
     * Builds one category filter chip; {@code category == null} is the "All Assets" chip.
     * Radio semantics live in {@link #enforceSingleSelection}: the acted-on chip is always the
     * selected one, so the category change applies on every activation.
     */
    private M3Chip categoryChip(String text, String fxId, String category) {
        M3Chip chip = new M3Chip(text, ChipVariant.FILTER);
        chip.getStyleClass().add("m3-category-chip");
        if (fxId != null) {
            chip.setId(fxId);
        }
        chip.setOnAction(e -> {
            enforceSingleSelection(chip);
            onCategoryChanged(category);
        });
        return chip;
    }

    /** Radio semantics: the acted-on chip stays selected even after ToggleGroup toggles it. */
    private void enforceSingleSelection(M3Chip chip) {
        if (!chip.isSelected()) {
            chip.setSelected(true);
        }
    }

    /** Small label with one style class. */
    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    @Override
    public Parent getRoot() {
        return viewRoot;
    }

    // ------------------------------------------------------------------
    // Ledger loading (EquipmentDao on a background task)
    // ------------------------------------------------------------------

    /**
     * Loads the equipment ledger through {@link Data#loadAsync}: real rows or the honest empty
     * state, with the footer counting exactly what the DAO returned.
     */
    public void loadEquipment() {
        stateOverlay.getChildren().clear();
        stateOverlay.setVisible(false);
        Data.loadAsync(ledgerStateHost,
                () -> {
                    Data.ensureReady();
                    return new ArrayList<>(new EquipmentDao().findAll());
                },
                equipment -> {
                    loaded = equipment;
                    onCategoryChanged(activeCategory);
                    if (equipment.isEmpty()) {
                        stateOverlay.getChildren().setAll(UiKit.emptyState(
                                "No equipment on record yet.",
                                "Add Equipment", this::onAdd));
                        stateOverlay.setVisible(true);
                    }
                    Equipment current = selected.get();
                    if (current != null) {
                        Equipment refreshed = equipment.stream()
                                .filter(e -> e.getId() == current.getId())
                                .findFirst().orElse(null);
                        selectEquipment(refreshed);
                    }
                },
                error -> {
                    stateOverlay.getChildren().setAll(
                            UiKit.errorState(error.getMessage(), this::loadEquipment));
                    stateOverlay.setVisible(true);
                });
    }

    private void onCategoryChanged(String category) {
        activeCategory = category;
        List<Equipment> filtered = new ArrayList<>();
        for (Equipment item : loaded) {
            if (category == null || category.equalsIgnoreCase(
                    item.getCategory() == null ? "" : item.getCategory().trim())) {
                filtered.add(item);
            }
        }
        equipmentTableView.getItems().setAll(filtered);
        assetCountLabel.setText(filtered.size() + " of " + loaded.size() + " equipment items");
        // Clear the current selection when it leaves the filtered view.
        if (selected.get() != null
                && filtered.stream().noneMatch(e -> e.getId() == selected.get().getId())) {
            selectEquipment(null);
        }
    }

    // ------------------------------------------------------------------
    // Selection + inspector
    // ------------------------------------------------------------------

    /** Selects (or clears) the inspected equipment item and mirrors it into the inspector. */
    public void selectEquipment(Equipment item) {
        selected.set(item);
        applyingSelection = true;
        try {
            EquipmentCondition condition = item == null ? null : item.getCondition();
            EquipmentStatus status = item == null ? null : item.getStatus();
            conditionNewChip.setSelected(condition == EquipmentCondition.NEW);
            conditionGoodChip.setSelected(condition == EquipmentCondition.GOOD);
            conditionFairChip.setSelected(condition == EquipmentCondition.FAIR);
            conditionPoorChip.setSelected(condition == EquipmentCondition.POOR);
            statusAvailChip.setSelected(status == EquipmentStatus.AVAILABLE);
            statusMaintChip.setSelected(status == EquipmentStatus.IN_MAINTENANCE);
            statusRetireChip.setSelected(status == EquipmentStatus.RETIRED);
        } finally {
            applyingSelection = false;
        }
        // The inspector only acts on a selected row: controls enable with a selection and
        // return to their honest disabled state (with the why-tooltip) when it clears.
        boolean hasSelection = item != null;
        for (M3Chip inspectorChip : List.of(conditionNewChip, conditionGoodChip,
                conditionFairChip, conditionPoorChip, statusAvailChip, statusMaintChip,
                statusRetireChip)) {
            inspectorChip.setDisable(!hasSelection);
        }
        logMaintenanceBtn.setDisable(!hasSelection);
        deleteBtn.setDisable(!hasSelection);
        if (item == null) {
            selectedModelTitleLabel.setText("Select an asset in the ledger");
            modelViewer3D.setCurrentModel(ModelType.WORKOUT_BENCH);
            return;
        }
        selectedModelTitleLabel.setText(item.getName());
        modelViewer3D.setCurrentModel(modelForCategory(item.getCategory()));
    }

    /**
     * Presentational category → display-model mapping (documented in app-ui-foundation.md §8):
     * CARDIO → treadmill, STRENGTH → squat rack, FREE WEIGHTS → dumbbells, RECOVERY →
     * kettlebells; anything else renders the workout bench.
     */
    static ModelType modelForCategory(String category) {
        String value = category == null ? "" : category.trim().toUpperCase();
        return switch (value) {
            case "CARDIO" -> ModelType.TREADMILL;
            case "STRENGTH" -> ModelType.SQUAT_RACK;
            case "FREE WEIGHTS" -> ModelType.DUMBBELL;
            case "RECOVERY" -> ModelType.KETTLEBELL;
            default -> ModelType.WORKOUT_BENCH;
        };
    }

    /** Applies a condition change to the selected equipment through the DAO. */
    public void applyCondition(EquipmentCondition condition) {
        applyInspectorChange("condition", item -> item.setCondition(condition));
    }

    /** Applies a status change to the selected equipment through the DAO. */
    public void applyStatus(EquipmentStatus status) {
        applyInspectorChange("status", item -> item.setStatus(status));
    }

    private void applyInspectorChange(String what, java.util.function.Consumer<Equipment> change) {
        Equipment item = selected.get();
        if (item == null) {
            overlay.showSnackbar("Select an equipment row first.");
            selectEquipment(null);
            return;
        }
        applyingSelection = true;
        try {
            change.accept(item);
        } finally {
            applyingSelection = false;
        }
        Data.submit(
                () -> {
                    new EquipmentDao().update(item);
                    return item;
                },
                updated -> {
                    overlay.showSnackbar(updated.getName() + " " + what + " updated.");
                    // Rebase the selection onto the freshly written row (keeps the 3D twin honest).
                    selectEquipment(updated);
                },
                error -> {
                    // Surface the DAO's verdict verbatim and re-derive the chips from the
                    // in-memory row (which still holds the pre-write state).
                    overlay.showSnackbar("Could not update " + what + ": " + error.getMessage());
                    selectEquipment(item);
                });
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    /** Opens the Add Equipment dialog backed by {@link EquipmentDao#insert}. */
    public void onAdd() {
        M3TextField nameField = new M3TextField("Equipment name", TextFieldVariant.OUTLINED);
        M3TextField categoryField = new M3TextField(
                "Category (CARDIO, STRENGTH, FREE WEIGHTS, RECOVERY)", TextFieldVariant.OUTLINED);
        categoryField.setSupportingText("One of the ledger categories; free text is kept as typed");
        M3TextField purchaseField = new M3TextField(
                "Purchase date (YYYY-MM-DD, optional)", TextFieldVariant.OUTLINED);
        Label hint = new Label("New equipment starts Available / New condition.");
        hint.setWrapText(true);

        M3Button cancel = M3Button.text("Cancel");
        M3Button add = M3Button.text("Add Equipment");
        M3Dialog dialog = M3Dialog.basic("Add equipment", new VBox(10,
                UiKit.formRow("Name", nameField),
                UiKit.formRow("Category", categoryField),
                UiKit.formRow("Purchased", purchaseField),
                hint),
                cancel, add);
        dialog.setDefaultAction(add::activate);
        add.setOnAction(e -> {
            String name = nameField.getText().trim();
            String category = categoryField.getText().trim().toUpperCase();
            String purchase = purchaseField.getText().trim();
            boolean nameOk = !name.isEmpty();
            boolean categoryOk = !category.isEmpty();
            boolean purchaseOk = purchase.isEmpty() || purchase.matches("\\d{4}-\\d{2}-\\d{2}");
            nameField.setError(!nameOk);
            categoryField.setError(!categoryOk);
            purchaseField.setError(!purchaseOk);
            if (!nameOk || !categoryOk || !purchaseOk) {
                return;
            }
            EquipmentDao dao = new EquipmentDao();
            Data.submit(
                    () -> dao.insert(new Equipment(0, name, category,
                            purchase.isEmpty() ? null : purchase,
                            EquipmentCondition.NEW, EquipmentStatus.AVAILABLE, null)),
                    inserted -> {
                        dialog.hide();
                        loadEquipment();
                        overlay.showSnackbar(inserted.getName()
                                + " added to the ledger (EQ-" + inserted.getId() + ").");
                    },
                    error -> overlay.showSnackbar(
                            "Could not add equipment: " + error.getMessage()));
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    /**
     * Rule 13 reading: "Log Today's Maintenance" is one atomic DAO update — the status becomes
     * IN_MAINTENANCE and lastMaintenance becomes today together, never as two hand edits.
     */
    public void handleLogMaintenanceToday() {
        Equipment item = selected.get();
        if (item == null) {
            overlay.showSnackbar("Select an equipment row first.");
            return;
        }
        EquipmentDao dao = new EquipmentDao();
        Data.submit(
                () -> {
                    item.setStatus(EquipmentStatus.IN_MAINTENANCE);
                    item.setLastMaintenance(LocalDate.now().toString());
                    dao.update(item);
                    return item;
                },
                updated -> {
                    overlay.showSnackbar(updated.getName()
                            + " logged for maintenance today ("
                            + LocalDate.now() + ").");
                    selectEquipment(updated);
                },
                error -> overlay.showSnackbar(
                        "Could not log maintenance: " + error.getMessage()));
    }

    /**
     * Two-step delete (UiKit confirm contract). The Equipment model has no soft-delete state and
     * the DAO exposes a real hard delete, so the confirm dialog says exactly that; rule 4's
     * "refuses while references exist" is the DAO/schema's to answer, and a failure surfaces
     * verbatim in a snackbar.
     */
    public void handleDeleteSelected() {
        Equipment item = selected.get();
        if (item == null) {
            overlay.showSnackbar("Select an equipment row first.");
            return;
        }
        UiKit.confirmDialog(overlay, "Delete equipment",
                item.getName() + " will be removed from the ledger permanently. "
                        + "To take it out of service without deleting, set its status to Retired.",
                "Delete", "Cancel",
                () -> {
                    EquipmentDao dao = new EquipmentDao();
                    Data.submit(
                            () -> {
                                dao.delete(item.getId());
                                return item;
                            },
                            deleted -> {
                                overlay.showSnackbar(deleted.getName() + " deleted from the ledger.");
                                selectEquipment(null);
                                loadEquipment();
                            },
                            error -> overlay.showSnackbar(
                                    "Could not delete: " + error.getMessage()));
                });
    }

    /**
     * Writes the real ledger CSV under {@code build/exports/} and announces the absolute path.
     */
    public void onExportCsv() {
        if (loaded.isEmpty()) {
            overlay.showSnackbar("Nothing to export — the ledger is empty.");
            return;
        }
        List<Equipment> snapshot = List.copyOf(loaded);
        Data.submit(
                () -> {
                    Path dir = Path.of("build", "exports");
                    Files.createDirectories(dir);
                    String stamp = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                    Path file = dir.resolve("equipment-ledger-" + stamp + ".csv");
                    StringBuilder csv = new StringBuilder(
                            "id,name,category,purchase_date,condition,status,last_maintenance\n");
                    for (Equipment item : snapshot) {
                        csv.append(item.getId()).append(',')
                                .append(csvCell(item.getName())).append(',')
                                .append(csvCell(item.getCategory())).append(',')
                                .append(csvCell(item.getPurchaseDate())).append(',')
                                .append(csvCell(labelOf(item.getCondition()))).append(',')
                                .append(csvCell(labelOf(item.getStatus()))).append(',')
                                .append(csvCell(item.getLastMaintenance())).append('\n');
                    }
                    Files.writeString(file, csv.toString(), StandardCharsets.UTF_8);
                    return file;
                },
                file -> overlay.showSnackbar("Exported " + snapshot.size()
                        + " items to " + file.toAbsolutePath()),
                error -> overlay.showSnackbar("Could not export: " + error.getMessage()));
    }

    /** CSV cell escaping: quotes wrap cells; embedded quotes double up. */
    private static String csvCell(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            return '"' + safe.replace("\"", "\"\"") + '"';
        }
        return safe;
    }

    public void onRefresh() {
        loadEquipment();
    }

    public void onResetCamera() {
        modelViewer3D.resetCamera();
    }

    /** Lets the floor map's "Full Asset Page" handoff preselect an equipment item. */
    public void selectEquipmentById(int id) {
        Equipment match = loaded.stream()
                .filter(item -> item.getId() == id)
                .findFirst().orElse(null);
        selectEquipment(match);
    }
}
