package com.gym.controller;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.floorplan.M3GymFloorMap;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.progress.M3LinearProgress;
import io.m3fx.controls.scene3d.M3ModelViewer3D;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

/**
 * Controller skeleton for 2.5D Interactive Blueprint &amp; 3D Spatial Linking screen.
 * Conforms strictly to docs/ui/facility/GYM_FLOOR_MAP_UI.md Section 7 contract.
 * Pure UI stub without backend services or SQL logic.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former gym-floor-map.fxml: layer toggles, blueprint canvas with the floating 3D inspection
 * card, zone telemetry rail, blueprint toolbar.
 */
public class GymFloorMapController implements ScreenController {

    /** Zoom bounds and step for the blueprint viewport. */
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 1.15;

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private StackPane canvasStackPane;
    private M3GymFloorMap gymFloorMap;
    private M3Chip occupancyChip;
    private Label zoomPercentLabel;
    private M3Chip layerPinsChip;
    private M3Chip layerHeatmapChip;
    private M3Chip layerZonesChip;
    private M3Chip layerSensorsChip;
    private M3GlassPane inspectionGlassCard;
    private Label inspectEquipmentNameLabel;
    private M3Chip inspectStatusChip;
    private M3Chip inspectConditionChip;
    private M3ModelViewer3D inspectModelViewer3D;
    private Label inspectLastMaintLabel;

    /** Scales the blueprint inside the clipped viewport; 1.0 = 100%. */
    private final Scale mapScale = new Scale(1.0, 1.0);

    /**
     * Builds the complete 2.5D Blueprint view. The former {@code initialize()} clipping and
     * inspect wiring happen at the end of the constructor, after the tree exists.
     */
    public GymFloorMapController() {
        viewRoot = buildView();

        // Clip the zoomed blueprint to the canvas viewport so it scales instead of spilling
        // over the telemetry rail; keep zoom pivoted at the center of the map.
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(canvasStackPane.widthProperty());
        clip.heightProperty().bind(canvasStackPane.heightProperty());
        canvasStackPane.setClip(clip);
        mapScale.pivotXProperty().bind(gymFloorMap.widthProperty().divide(2));
        mapScale.pivotYProperty().bind(gymFloorMap.heightProperty().divide(2));
        gymFloorMap.getTransforms().add(mapScale);
        // One-click 3D inspection: open the floating card for the tapped machine pin.

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        canvasStackPane.setId("canvasStackPane");
        gymFloorMap.setId("gymFloorMap");
        occupancyChip.setId("occupancyChip");
        zoomPercentLabel.setId("zoomPercentLabel");
        layerPinsChip.setId("layerPinsChip");
        layerHeatmapChip.setId("layerHeatmapChip");
        layerZonesChip.setId("layerZonesChip");
        layerSensorsChip.setId("layerSensorsChip");
        inspectionGlassCard.setId("inspectionGlassCard");
        inspectEquipmentNameLabel.setId("inspectEquipmentNameLabel");
        inspectStatusChip.setId("inspectStatusChip");
        inspectConditionChip.setId("inspectConditionChip");
        inspectModelViewer3D.setId("inspectModelViewer3D");
        inspectLastMaintLabel.setId("inspectLastMaintLabel");
        gymFloorMap.setOnInspect3D(this::showInspectionCard);
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for gym-floor-map.fxml) and
     * wires every handler.
     */
    private StackPane buildView() {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("floor-map-layout");

        // Top app bar & layer selection header.
        VBox topBarBox = new VBox(8);
        topBarBox.getStyleClass().add("top-bar-box");
        topBarBox.setPadding(new Insets(16, 24, 12, 24));

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getStyleClass().add("header-row");
        occupancyChip = new M3Chip("Occupancy: 64 / 120 (53%)", ChipVariant.ASSIST);
        occupancyChip.getStyleClass().add("occupancy-badge");
        M3IconButton zoomOutBtn = new M3IconButton(M3Icon.symbol("remove"), IconButtonVariant.STANDARD);
        zoomOutBtn.getStyleClass().add("zoom-btn");
        zoomOutBtn.setOnAction(e -> onZoomOut());
        zoomPercentLabel = new Label("100%");
        zoomPercentLabel.getStyleClass().add("zoom-label");
        M3IconButton zoomInBtn = new M3IconButton(M3Icon.symbol("add"), IconButtonVariant.STANDARD);
        zoomInBtn.getStyleClass().add("zoom-btn");
        zoomInBtn.setOnAction(e -> onZoomIn());
        M3Button resetViewBtn = new M3Button("Reset View", ButtonVariant.TEXT);
        resetViewBtn.getStyleClass().add("text-btn-compact");
        resetViewBtn.setOnAction(e -> onResetView());
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(
                styledLabel("Gym Floor Blueprint & 3D Spatial Twin", "header-title-text"),
                occupancyChip,
                headerSpacer,
                zoomOutBtn,
                zoomPercentLabel,
                zoomInBtn,
                new Separator(Orientation.VERTICAL),
                resetViewBtn);

        // Layer toggle strip.
        HBox layerStrip = new HBox(8);
        layerStrip.setAlignment(Pos.CENTER_LEFT);
        layerStrip.getStyleClass().add("layer-toggle-strip");
        layerPinsChip = new M3Chip("Machine Pins", ChipVariant.FILTER);
        layerPinsChip.setSelected(true);
        layerHeatmapChip = new M3Chip("Occupancy Heatmap", ChipVariant.FILTER);
        layerHeatmapChip.setSelected(true);
        layerZonesChip = new M3Chip("Zone Boundaries", ChipVariant.FILTER);
        layerZonesChip.setSelected(true);
        layerSensorsChip = new M3Chip("IoT Sensors", ChipVariant.FILTER);
        layerSensorsChip.setSelected(false);
        layerPinsChip.setOnAction(e -> onTogglePinsLayer());
        layerHeatmapChip.setOnAction(e -> onToggleHeatmapLayer());
        layerZonesChip.setOnAction(e -> onToggleZonesLayer());
        layerSensorsChip.setOnAction(e -> onToggleSensorsLayer());
        layerStrip.getChildren().addAll(
                styledLabel("Layers:", "layer-label"),
                layerPinsChip, layerHeatmapChip, layerZonesChip, layerSensorsChip);

        topBarBox.getChildren().addAll(headerRow, layerStrip);
        layout.setTop(topBarBox);

        // Center: interactive 2.5D blueprint canvas stack.
        canvasStackPane = new StackPane();
        canvasStackPane.getStyleClass().add("blueprint-canvas-container");

        gymFloorMap = new M3GymFloorMap();
        gymFloorMap.setPrefSize(800, 520);

        // Floating 1-click 3D inspection card (initially hidden).
        inspectionGlassCard = new M3GlassPane();
        inspectionGlassCard.setVisible(false);
        inspectionGlassCard.setMaxSize(380, 440);
        StackPane.setAlignment(inspectionGlassCard, Pos.CENTER);
        inspectionGlassCard.getStyleClass().add("pin-popover-glass");
        VBox popoverContent = new VBox(10);
        popoverContent.getStyleClass().add("popover-inner-box");
        popoverContent.setPadding(new Insets(16));
        HBox popoverHeader = new HBox();
        popoverHeader.setAlignment(Pos.CENTER_LEFT);
        inspectEquipmentNameLabel = new Label("Olympic Power Cage #01");
        inspectEquipmentNameLabel.getStyleClass().add("popover-title");
        M3IconButton closeInspectBtn = new M3IconButton(M3Icon.symbol("close"), IconButtonVariant.STANDARD);
        closeInspectBtn.getStyleClass().add("popover-close-btn");
        closeInspectBtn.setOnAction(e -> onCloseInspection());
        Region popoverSpacer = new Region();
        HBox.setHgrow(popoverSpacer, Priority.ALWAYS);
        popoverHeader.getChildren().addAll(inspectEquipmentNameLabel, popoverSpacer, closeInspectBtn);
        HBox statusChipsRow = new HBox(8);
        inspectStatusChip = new M3Chip("AVAILABLE", ChipVariant.ASSIST);
        inspectStatusChip.getStyleClass().add("chip-status-available");
        inspectConditionChip = new M3Chip("CONDITION: GOOD", ChipVariant.ASSIST);
        inspectConditionChip.getStyleClass().add("chip-condition-good");
        statusChipsRow.getChildren().addAll(inspectStatusChip, inspectConditionChip);
        inspectModelViewer3D = new M3ModelViewer3D();
        inspectModelViewer3D.setPrefSize(340, 220);
        inspectModelViewer3D.getStyleClass().add("popover-3d-viewport");
        inspectLastMaintLabel = new Label("Last Maintenance: 2026-10-15");
        inspectLastMaintLabel.getStyleClass().add("popover-subtext");
        HBox popoverActions = new HBox(10);
        popoverActions.setAlignment(Pos.CENTER);
        M3Button markMaintenanceBtn = new M3Button("Mark Maintenance", ButtonVariant.TONAL);
        markMaintenanceBtn.getStyleClass().add("tonal-btn-compact");
        markMaintenanceBtn.setOnAction(e -> onScheduleServiceFromMap());
        M3Button fullAssetBtn = new M3Button("Full Asset Page", ButtonVariant.FILLED);
        fullAssetBtn.getStyleClass().add("filled-btn-compact");
        fullAssetBtn.setOnAction(e -> onOpenFullAssetDetails());
        popoverActions.getChildren().addAll(markMaintenanceBtn, fullAssetBtn);
        popoverContent.getChildren().addAll(popoverHeader, statusChipsRow,
                inspectModelViewer3D, inspectLastMaintLabel, popoverActions);
        inspectionGlassCard.getGlassChildren().add(popoverContent);

        canvasStackPane.getChildren().addAll(gymFloorMap, inspectionGlassCard);
        layout.setCenter(canvasStackPane);

        // Right: zone occupancy & environmental telemetry.
        VBox telemetryRail = new VBox(14);
        telemetryRail.setPrefWidth(300);
        telemetryRail.getStyleClass().add("spatial-telemetry-rail");
        telemetryRail.setPadding(new Insets(8, 24, 48, 16));
        telemetryRail.getChildren().add(styledLabel("Zone Congestion Telemetry", "telemetry-section-title"));

        telemetryRail.getChildren().addAll(
                zoneCard("Cardio Deck", "78% (High)", "zone-val-warn", 0.78, "progress-warn"),
                zoneCard("Free Weights Arena", "62% (Med)", "zone-val-normal", 0.62, "progress-normal"),
                zoneCard("Olympic Platforms", "85% (Critical)", "zone-val-high", 0.85, "progress-high"),
                zoneCard("Functional Turf", "34% (Low)", "zone-val-low", 0.34, "progress-low"));

        Region railSpacer = new Region();
        VBox.setVgrow(railSpacer, Priority.ALWAYS);
        telemetryRail.getChildren().add(railSpacer);

        VBox legendBox = new VBox(8);
        legendBox.getStyleClass().add("quick-legend-box");
        legendBox.getChildren().add(styledLabel("Map Legend", "legend-header"));
        legendBox.getChildren().addAll(
                legendRow(Color.web("#00E676"), "Available / Operational"),
                legendRow(Color.web("#FF1744"), "Occupied / Live Set"),
                legendRow(Color.web("#FF9100"), "In Maintenance / Service Req"));
        telemetryRail.getChildren().add(legendBox);
        layout.setRight(telemetryRail);

        // Bottom: blueprint toolbar & pin placement controls.
        HBox bottomDock = new HBox(16);
        bottomDock.setAlignment(Pos.CENTER_RIGHT);
        bottomDock.getStyleClass().add("blueprint-bottom-dock");
        bottomDock.setPadding(new Insets(12, 24, 16, 24));
        M3Button perspectiveBtn = new M3Button("2.5D / 2D Perspective Toggle", ButtonVariant.OUTLINED);
        perspectiveBtn.getStyleClass().add("outlined-btn");
        perspectiveBtn.setOnAction(e -> onTogglePerspective());
        M3Button exportBtn = new M3Button("Export Blueprint Layout (DXF/PDF)", ButtonVariant.TEXT);
        exportBtn.getStyleClass().add("text-btn");
        exportBtn.setOnAction(e -> onExportBlueprint());
        M3Button placePinBtn = new M3Button("+ Place Equipment Pin", ButtonVariant.FILLED);
        placePinBtn.getStyleClass().add("filled-primary-btn");
        placePinBtn.setOnAction(e -> onPlacePin());
        Region dockSpacer = new Region();
        HBox.setHgrow(dockSpacer, Priority.ALWAYS);
        bottomDock.getChildren().addAll(perspectiveBtn, dockSpacer, exportBtn, placePinBtn);
        layout.setBottom(bottomDock);

        StackPane root = new StackPane();
        root.getStyleClass().add("floor-map-root");
        root.getChildren().add(layout);
        return root;
    }

    /** One zone congestion card (name, percentage label, linear progress). */
    private static M3Card zoneCard(String zone, String value, String valueStyle,
                                   double progress, String progressStyle) {
        M3Card card = new M3Card(CardVariant.ELEVATED);
        card.getStyleClass().add("zone-stat-card");
        VBox content = new VBox(8);
        content.getStyleClass().add("zone-stat-box");
        content.setPadding(new Insets(12, 14, 12, 14));
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add(valueStyle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(
                styledLabel(zone, "zone-name"),
                spacer,
                valueLabel);
        M3LinearProgress bar = new M3LinearProgress(progress);
        bar.getStyleClass().add(progressStyle);
        content.getChildren().addAll(row, bar);
        card.setContent(content);
        return card;
    }

    /** One legend row (colored dot + caption). */
    private static HBox legendRow(Color color, String caption) {
        Circle dot = new Circle(6, color);
        HBox row = new HBox(8, dot, styledLabel(caption, "legend-text"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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

    private void showInspectionCard(M3GymFloorMap.EquipmentNode node) {
        if (node == null) {
            return;
        }
        inspectEquipmentNameLabel.setText(node.name + " #" + node.id);
        if (node.status != null) {
            inspectStatusChip.setText(node.status.label.toUpperCase());
        }
        inspectLastMaintLabel.setText("Operator: "
                + (node.currentUser == null || node.currentUser.isBlank() ? "—" : node.currentUser)
                + " · " + node.timeRemainingMinutes + " min remaining");
        if (node.modelType != null) {
            inspectModelViewer3D.setCurrentModel(node.modelType);
        }
        inspectionGlassCard.setVisible(true);
    }

    private void applyZoom(double factor) {
        double zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, mapScale.getX() * factor));
        mapScale.setX(zoom);
        mapScale.setY(zoom);
        zoomPercentLabel.setText(Math.round(zoom * 100) + "%");
    }

    public void onZoomIn() {
        applyZoom(ZOOM_STEP);
    }

    public void onZoomOut() {
        applyZoom(1.0 / ZOOM_STEP);
    }

    public void onResetView() {
        mapScale.setX(1.0);
        mapScale.setY(1.0);
        zoomPercentLabel.setText("100%");
    }

    public void onTogglePinsLayer() {
        // No-op toggle pins layer handler
    }

    public void onToggleHeatmapLayer() {
        // No-op toggle heatmap layer handler
    }

    public void onToggleZonesLayer() {
        // No-op toggle zones layer handler
    }

    public void onToggleSensorsLayer() {
        // No-op toggle sensors layer handler
    }

    public void onCloseInspection() {
        inspectionGlassCard.setVisible(false);
    }

    public void onScheduleServiceFromMap() {
        // No-op schedule service handler
    }

    public void onOpenFullAssetDetails() {
        // No-op open asset details handler
    }

    public void onPlacePin() {
        // No-op place pin handler
    }

    public void onTogglePerspective() {
        // No-op toggle perspective handler
    }

    public void onExportBlueprint() {
        // No-op export blueprint handler
    }
}
