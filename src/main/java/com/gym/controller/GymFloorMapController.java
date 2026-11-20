package com.gym.controller;

import com.gym.dao.AttendanceDao;
import com.gym.dao.EquipmentDao;
import com.gym.dao.SessionDao;
import com.gym.model.Equipment;
import com.gym.model.EquipmentStatus;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
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
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.scene3d.M3ModelViewer3D;
import io.m3fx.controls.common.M3Tooltip;
import io.m3fx.core.theme.ColorRole;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 2.5D Interactive Blueprint &amp; 3D Spatial Linking screen on real data (C5).
 * Conforms to docs/ui/facility/GYM_FLOOR_MAP_UI.md Section 7 and the com.gym.ui foundation
 * contracts (app-ui-foundation.md §9): the header chip counts people currently inside
 * (AttendanceDao, today's check-ins without a check-out) and today's CONFIRMED sessions; the
 * zone telemetry cards derive from EquipmentDao per the documented zone→category mapping —
 * never invented percentages, and a zone without equipment rows shows an honest "no data"
 * state. Layer toggles: machine pins hide/show the map's equipment list; heatmap/zones/sensors
 * are disabled with explanatory tooltips because M3GymFloorMap does not expose those layers
 * (component gap flagged for lane B). "Place Equipment Pin" is disabled for the same reason
 * the honest way: the Equipment model has no floor-location fields, so there is nothing to
 * place.
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
    private VBox zoneCardsBox;
    private boolean pinsShown = true;
    private List<M3GymFloorMap.EquipmentNode> savedPins = List.of();

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
    /** The pin currently shown in the inspection card (presentational; map-local state). */
    private M3GymFloorMap.EquipmentNode inspectedNode;

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
        loadOccupancy();
    }

    // ------------------------------------------------------------------
    // Real data (C5): zone readiness + building occupancy
    // ------------------------------------------------------------------

    /**
     * The documented zone-to-equipment-category mapping (app-ui-foundation.md section 9).
     * Sessions and attendance carry no zone/equipment link, so zone "occupancy" is derived
     * from the zone's equipment readiness (operational = not RETIRED and not IN_MAINTENANCE);
     * a zone without equipment rows shows an honest "no data" state.
     */
    static final Map<String, String> ZONE_CATEGORIES = new LinkedHashMap<>(Map.of(
            "Cardio Deck", "CARDIO",
            "Free Weights Arena", "FREE WEIGHTS",
            "Olympic Platforms", "STRENGTH",
            "Functional Turf", "RECOVERY"));

    /**
     * Loads the telemetry rail and header chip from the real DAOs on a background task:
     * equipment readiness per zone (EquipmentDao), people currently inside (AttendanceDao:
     * today's check-ins without a check-out) and today's CONFIRMED session count (SessionDao).
     */
    public void loadOccupancy() {
        Data.loadAsync(zoneCardsBox,
                () -> {
                    Data.ensureReady();
                    List<Equipment> equipment = new EquipmentDao().findAll();
                    String today = LocalDate.now().toString();
                    int insideNow = 0;
                    for (var attendance : new AttendanceDao().findAll()) {
                        if (today.equals(attendance.getDate())
                                && (attendance.getCheckOut() == null
                                    || attendance.getCheckOut().isBlank())) {
                            insideNow++;
                        }
                    }
                    int confirmedToday = 0;
                    int sessionsToday = 0;
                    for (var session : new SessionDao().findAll()) {
                        if (today.equals(session.getDate())) {
                            sessionsToday++;
                            if (session.getStatus() != null
                                    && "CONFIRMED".equals(session.getStatus().name())) {
                                confirmedToday++;
                            }
                        }
                    }
                    return new OccupancySnapshot(equipment, insideNow, sessionsToday,
                            confirmedToday);
                },
                snapshot -> {
                    occupancyChip.setText("Inside now: " + snapshot.insideNow()
                            + " | Sessions today: " + snapshot.sessionsToday()
                            + " (" + snapshot.confirmedToday() + " confirmed)");
                    rebuildZoneCards(snapshot.equipment());
                },
                error -> {
                    occupancyChip.setText("Occupancy: unavailable");
                    zoneCardsBox.getChildren().setAll(
                            UiKit.errorState(error.getMessage(), this::loadOccupancy));
                });
    }

    /** One load's worth of telemetry data. */
    private record OccupancySnapshot(List<Equipment> equipment, int insideNow,
                                     int sessionsToday, int confirmedToday) {
    }

    /** Rebuilds the zone readiness cards from the loaded equipment rows. */
    private void rebuildZoneCards(List<Equipment> equipment) {
        zoneCardsBox.getChildren().clear();
        for (Map.Entry<String, String> zone : ZONE_CATEGORIES.entrySet()) {
            List<Equipment> inZone = equipment.stream()
                    .filter(item -> zone.getValue().equalsIgnoreCase(
                            item.getCategory() == null ? "" : item.getCategory().trim()))
                    .toList();
            zoneCardsBox.getChildren().add(zoneCard(zone.getKey(), zone.getValue(), inZone));
        }
    }

    /**
     * One zone readiness card: "n of m operational" with the ratio as the progress value, or
     * the honest "no equipment data" state when the zone's category has no rows.
     */
    private M3Card zoneCard(String zone, String category, List<Equipment> inZone) {
        M3Card card = new M3Card(CardVariant.ELEVATED);
        card.getStyleClass().add("zone-stat-card");
        VBox content = new VBox(8);
        content.getStyleClass().add("zone-stat-box");
        content.setPadding(new javafx.geometry.Insets(12, 14, 12, 14));
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(styledLabel(zone, "zone-name"), spacer);

        if (inZone.isEmpty()) {
            row.getChildren().add(styledLabel("No equipment data", "zone-val-low"));
            content.getChildren().addAll(row,
                    styledLabel("No " + category + " rows in the ledger", "zone-name"));
            card.setContent(content);
            return card;
        }
        long operational = inZone.stream()
                .filter(item -> item.getStatus() != EquipmentStatus.IN_MAINTENANCE
                        && item.getStatus() != EquipmentStatus.RETIRED)
                .count();
        double ratio = (double) operational / inZone.size();
        Label valueLabel = styledLabel(
                operational + " of " + inZone.size() + " operational", "zone-val-normal");
        if (ratio < 0.6) {
            valueLabel.getStyleClass().add("zone-val-warn");
        }
        row.getChildren().add(valueLabel);
        M3LinearProgress bar = new M3LinearProgress(ratio);
        bar.getStyleClass().add(ratio < 0.6 ? "progress-warn" : "progress-normal");
        content.getChildren().addAll(row, bar);
        card.setContent(content);
        return card;
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
        occupancyChip = new M3Chip("Occupancy: loading…", ChipVariant.ASSIST);
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
        // M3GymFloorMap exposes no heatmap/zone-boundary/sensor layer visibility (component
        // gap, flagged for lane B) - the honest UI states that instead of fake toggles.
        M3Tooltip.installIfAbsent(layerHeatmapChip,
                "Not available yet: the floor-map component has no heatmap layer toggle");
        M3Tooltip.installIfAbsent(layerZonesChip,
                "Not available yet: the floor-map component has no zone-boundary toggle");
        M3Tooltip.installIfAbsent(layerSensorsChip,
                "Not available yet: the floor-map component has no sensor layer");
        layerHeatmapChip.setDisable(true);
        layerZonesChip.setDisable(true);
        layerSensorsChip.setDisable(true);
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
        telemetryRail.getChildren().add(styledLabel("Zone Readiness Telemetry", "telemetry-section-title"));
        zoneCardsBox = new VBox(14);
        telemetryRail.getChildren().add(zoneCardsBox);
        // Loading state until the first real load lands (app-ui-foundation contract).
        zoneCardsBox.getChildren().add(UiKit.loadingState());

        Region railSpacer = new Region();
        VBox.setVgrow(railSpacer, Priority.ALWAYS);
        telemetryRail.getChildren().add(railSpacer);

        VBox legendBox = new VBox(8);
        legendBox.getStyleClass().add("quick-legend-box");
        legendBox.getChildren().add(styledLabel("Map Legend", "legend-header"));
        legendBox.getChildren().addAll(
                // Semantic role mapping, no hardcoded hex: available = success → the tertiary
                // accent (green family under this seed), occupied/live = the error role,
                // maintenance/service = the neutral secondary accent.
                legendRow(UiKit.themeRole(ColorRole.TERTIARY), "Available / Operational"),
                legendRow(UiKit.themeRole(ColorRole.ERROR), "Occupied / Live Set"),
                legendRow(UiKit.themeRole(ColorRole.SECONDARY), "In Maintenance / Service Req"));
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
        // The Equipment model has no floor-location fields, so there is nothing to place -
        // the honest answer is a disabled button with the reason (C5 contract).
        placePinBtn.setDisable(true);
        M3Tooltip.installIfAbsent(placePinBtn,
                "Unavailable: equipment records have no floor-location fields yet");
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
        inspectedNode = node;
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

    /**
     * Pins layer toggle: hides/shows the map's equipment pins. The component redraws on
     * scene/size/mouse events but not on list changes, so the map node is re-added to its
     * container (public API) to force the redraw - a proper redraw hook is the flagged
     * lane-B gap.
     */
    public void onTogglePinsLayer() {
        if (pinsShown) {
            savedPins = List.copyOf(gymFloorMap.getEquipmentList());
            gymFloorMap.getEquipmentList().clear();
        } else if (!savedPins.isEmpty()) {
            gymFloorMap.getEquipmentList().setAll(savedPins);
        }
        pinsShown = !pinsShown;
        refreshMapCanvas();
    }

    /** Forces a map redraw through the public API (re-attaching fires the scene listener). */
    private void refreshMapCanvas() {
        canvasStackPane.getChildren().setAll(gymFloorMap, inspectionGlassCard);
    }

    public void onToggleHeatmapLayer() {
        // Disabled in buildView: the component exposes no heatmap layer.
    }

    public void onToggleZonesLayer() {
        // Disabled in buildView: the component exposes no zone-boundary toggle.
    }

    public void onToggleSensorsLayer() {
        // Disabled in buildView: the component exposes no sensor layer.
    }

    public void onCloseInspection() {
        inspectionGlassCard.setVisible(false);
    }

    /**
     * Presentational: flips the inspected pin into the map's MAINTENANCE state so the map and
     * the inspection card agree. The map's pins are the component's demo nodes (the Equipment
     * model has no floor locations), so there is deliberately no DAO write behind this.
     */
    public void onScheduleServiceFromMap() {
        if (inspectedNode == null) {
            return;
        }
        inspectedNode.status = M3GymFloorMap.EquipmentStatus.MAINTENANCE;
        inspectStatusChip.setText(M3GymFloorMap.EquipmentStatus.MAINTENANCE.label.toUpperCase());
        refreshMapCanvas();
    }

    /**
     * Navigates to the Equipment screen through the shell's rail-key handoff (falls back to a
     * snackbar in standalone previews/tests without the owning shell).
     */
    public void onOpenFullAssetDetails() {
        AppShellController shell = shellFromScene();
        if (shell == null) {
            M3Overlay feedback = new M3Overlay();
            feedback.showSnackbar("Open the shell to view the full asset page.");
            return;
        }
        shell.openScreenViaRail("navEquipment", Screen.EQUIPMENT, null);
    }

    /** The owning shell controller, published by AppShellController in the scene properties. */
    private AppShellController shellFromScene() {
        if (viewRoot.getScene() == null
                || !(viewRoot.getScene().getRoot() instanceof javafx.scene.Parent sceneRoot)) {
            return null;
        }
        Object shell = sceneRoot.getProperties().get(AppShellController.SHELL_CONTROLLER_KEY);
        return shell instanceof AppShellController controller ? controller : null;
    }

    public void onTogglePerspective() {
        // No-op toggle perspective handler
    }

    public void onExportBlueprint() {
        // No-op export blueprint handler
    }
}
