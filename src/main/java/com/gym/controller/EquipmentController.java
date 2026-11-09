package com.gym.controller;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.scene3d.M3ModelViewer3D;
import io.m3fx.controls.selection.M3Switch;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.textfield.M3TextField;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller skeleton for Equipment Inventory & 3D Diagnostics screen.
 * Conforms strictly to docs/ui/facility/EQUIPMENT_INVENTORY_UI.md Section 7 contract.
 * Pure UI stub without backend services or SQL logic.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former equipment.fxml: top search deck with filter chips, ledger/3D-twin split, maintenance
 * side sheet.
 */
public class EquipmentController implements ScreenController {

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private TableView<String> equipmentTableView;
    private TableColumn<String, ?> colId;
    private TableColumn<String, ?> colName;
    private TableColumn<String, ?> colCategory;
    private TableColumn<String, ?> colCondition;
    private TableColumn<String, ?> colStatus;
    private TableColumn<String, ?> colLastMaintenance;
    private Label selectedModelTitleLabel;
    private M3ModelViewer3D modelViewer3D;
    private M3Switch wireframeToggleSwitch;
    private RadioButton conditionNewRadio;
    private RadioButton conditionGoodRadio;
    private RadioButton conditionFairRadio;
    private RadioButton conditionPoorRadio;
    private RadioButton statusAvailRadio;
    private RadioButton statusMaintRadio;
    private RadioButton statusRetireRadio;
    private M3TextField searchField;
    private Label assetCountLabel;
    private M3SideSheet maintenanceSideSheet;
    private M3DataTable<String> maintenanceHistoryTable;

    /**
     * Builds the complete Equipment Inventory view. The former {@code initialize()} stub ran no
     * additional wiring, so the constructor only needs the tree.
     */
    public EquipmentController() {
        // Primary screen layout.
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("equipment-layout");

        // Top app bar & search deck.
        VBox topBarBox = new VBox(10);
        topBarBox.getStyleClass().add("top-bar-box");
        topBarBox.setPadding(new Insets(16, 24, 12, 24));

        HBox headerBox = new HBox(16);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getStyleClass().add("app-header-box");
        searchField = new M3TextField("Search equipment, serial, category...",
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        searchField.setPrefWidth(320);
        searchField.getStyleClass().add("search-field");
        M3Button addBtn = new M3Button("+ Add Equipment", ButtonVariant.FILLED);
        addBtn.getStyleClass().add("filled-primary-btn");
        addBtn.setOnAction(e -> onAdd());
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(
                styledLabel("Equipment Inventory & 3D Diagnostics", "header-title-text"),
                headerSpacer,
                searchField,
                addBtn);

        // Category filter chips.
        HBox filterStrip = new HBox(8);
        filterStrip.setAlignment(Pos.CENTER_LEFT);
        filterStrip.getStyleClass().add("category-filter-strip");
        M3Chip allAssetsChip = new M3Chip("All Assets", ChipVariant.FILTER);
        allAssetsChip.setSelected(true);
        filterStrip.getChildren().addAll(
                allAssetsChip,
                new M3Chip("Free Weights", ChipVariant.FILTER),
                new M3Chip("Cardio", ChipVariant.FILTER),
                new M3Chip("Plate-Loaded", ChipVariant.FILTER),
                new M3Chip("Cable Machines", ChipVariant.FILTER),
                new M3Chip("Recovery & Mobility", ChipVariant.FILTER));

        topBarBox.getChildren().addAll(headerBox, filterStrip);
        layout.setTop(topBarBox);

        // Center: master split pane (ledger left, 3D twin right).
        VBox ledgerPane = new VBox(10);
        ledgerPane.getStyleClass().add("table-container-box");

        equipmentTableView = new TableView<>();
        equipmentTableView.getStyleClass().add("m3-data-table");
        equipmentTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(equipmentTableView, Priority.ALWAYS);
        colId = new TableColumn<>("ID");
        colId.setPrefWidth(60);
        colName = new TableColumn<>("Asset Name");
        colName.setPrefWidth(200);
        colCategory = new TableColumn<>("Category");
        colCategory.setPrefWidth(130);
        colCondition = new TableColumn<>("Condition");
        colCondition.setPrefWidth(100);
        colStatus = new TableColumn<>("Status");
        colStatus.setPrefWidth(110);
        colLastMaintenance = new TableColumn<>("Last Maint.");
        colLastMaintenance.setPrefWidth(110);
        equipmentTableView.getColumns().setAll(colId, colName, colCategory, colCondition,
                colStatus, colLastMaintenance);

        // Table footer.
        HBox tableFooter = new HBox(12);
        tableFooter.setAlignment(Pos.CENTER_LEFT);
        tableFooter.getStyleClass().add("table-footer-box");
        assetCountLabel = new Label("Showing 12 of 84 equipment items");
        assetCountLabel.getStyleClass().add("footer-label");
        tableFooter.getChildren().add(assetCountLabel);
        ledgerPane.getChildren().addAll(equipmentTableView, tableFooter);

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
        selectedModelTitleLabel = new Label("Olympic Power Cage");
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

        // Detailed lifecycle & condition dock.
        M3Card inspectorCard = new M3Card(CardVariant.ELEVATED);
        inspectorCard.getStyleClass().add("inspector-details-card");
        VBox detailsInner = new VBox(12);
        detailsInner.getStyleClass().add("details-card-inner");
        detailsInner.setPadding(new Insets(14, 16, 14, 16));
        detailsInner.getChildren().add(
                styledLabel("Asset State & Maintenance Controls", "card-section-title"));

        ToggleGroup conditionGroup = new ToggleGroup();
        ToggleGroup statusGroup = new ToggleGroup();

        // Condition selector radio row.
        HBox conditionRow = new HBox(16);
        conditionRow.setAlignment(Pos.CENTER_LEFT);
        conditionNewRadio = radio("NEW", conditionGroup, this::handleConditionRadio);
        conditionGoodRadio = radio("GOOD", conditionGroup, this::handleConditionRadio);
        conditionFairRadio = radio("FAIR", conditionGroup, this::handleConditionRadio);
        conditionPoorRadio = radio("POOR", conditionGroup, this::handleConditionRadio);
        conditionRow.getChildren().addAll(
                styledLabel("Condition:", "dock-label"),
                conditionNewRadio, conditionGoodRadio, conditionFairRadio, conditionPoorRadio);

        // Status selector radio row.
        HBox statusRow = new HBox(16);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusAvailRadio = radio("AVAILABLE", statusGroup, this::handleStatusRadio);
        statusMaintRadio = radio("MAINTENANCE", statusGroup, this::handleStatusRadio);
        statusRetireRadio = radio("RETIRED", statusGroup, this::handleStatusRadio);
        statusRow.getChildren().addAll(
                styledLabel("Status:", "dock-label"),
                statusAvailRadio, statusMaintRadio, statusRetireRadio);

        // Quick maintenance logger CTA.
        HBox dockActions = new HBox(12);
        dockActions.setAlignment(Pos.CENTER_LEFT);
        M3Button logMaintenanceBtn = new M3Button("🛠️ Log Today's Maintenance", ButtonVariant.TONAL);
        logMaintenanceBtn.getStyleClass().add("tonal-button");
        logMaintenanceBtn.setOnAction(e -> handleLogMaintenanceToday());
        M3Button deleteBtn = new M3Button("🗑️ Delete Item", ButtonVariant.TEXT);
        deleteBtn.getStyleClass().add("danger-text-btn");
        deleteBtn.setOnAction(e -> handleDeleteSelected());
        Region dockSpacer = new Region();
        HBox.setHgrow(dockSpacer, Priority.ALWAYS);
        dockActions.getChildren().addAll(logMaintenanceBtn, dockSpacer, deleteBtn);

        detailsInner.getChildren().addAll(conditionRow, statusRow, dockActions);
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
        M3Button exportBtn = new M3Button("Export Asset Ledger (PDF)", ButtonVariant.OUTLINED);
        exportBtn.getStyleClass().add("outlined-btn");
        exportBtn.setOnAction(e -> onExportPdf());
        M3Button refreshBtn = new M3Button("Refresh Inventory", ButtonVariant.TEXT);
        refreshBtn.getStyleClass().add("text-btn");
        refreshBtn.setOnAction(e -> onRefresh());
        Region bottomDockSpacer = new Region();
        HBox.setHgrow(bottomDockSpacer, Priority.ALWAYS);
        actionDock.getChildren().addAll(exportBtn, bottomDockSpacer, refreshBtn);
        layout.setBottom(actionDock);

        // SideSheet overlay for detailed asset history & maintenance ledger.
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

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        equipmentTableView.setId("equipmentTableView");
        colId.setId("colId");
        colName.setId("colName");
        colCategory.setId("colCategory");
        colCondition.setId("colCondition");
        colStatus.setId("colStatus");
        colLastMaintenance.setId("colLastMaintenance");
        selectedModelTitleLabel.setId("selectedModelTitleLabel");
        modelViewer3D.setId("modelViewer3D");
        wireframeToggleSwitch.setId("wireframeToggleSwitch");
        conditionNewRadio.setId("conditionNewRadio");
        conditionGoodRadio.setId("conditionGoodRadio");
        conditionFairRadio.setId("conditionFairRadio");
        conditionPoorRadio.setId("conditionPoorRadio");
        statusAvailRadio.setId("statusAvailRadio");
        statusMaintRadio.setId("statusMaintRadio");
        statusRetireRadio.setId("statusRetireRadio");
        searchField.setId("searchField");
        assetCountLabel.setId("assetCountLabel");
        maintenanceSideSheet.setId("maintenanceSideSheet");
        maintenanceHistoryTable.setId("maintenanceHistoryTable");
        viewRoot.getChildren().addAll(layout, maintenanceSideSheet);
    }

    /** Builds one radio button inside the given group. */
    private static RadioButton radio(String text, ToggleGroup group, Runnable action) {
        RadioButton button = new RadioButton(text);
        button.setToggleGroup(group);
        button.setOnAction(e -> action.run());
        return button;
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

    public void onAdd() {
        // No-op event handler
    }

    public void onResetCamera() {
        modelViewer3D.resetCamera();
    }

    public void handleConditionRadio() {
        // No-op condition handler
    }

    public void handleStatusRadio() {
        // No-op status handler
    }

    public void handleLogMaintenanceToday() {
        // No-op maintenance log handler
    }

    public void handleDeleteSelected() {
        // No-op delete handler
    }

    public void onExportPdf() {
        // No-op export handler
    }

    public void onRefresh() {
        // No-op refresh handler
    }

    // Signatures matching docs/UML.md specification
    public void onStatusChange(int equipmentId, Object status) {}
    public void onConditionChange(int equipmentId, Object condition) {}
    public void onLogMaintenance(int equipmentId, String date) {}
    public void onDelete(int equipmentId) {}
}
