package com.gym.controller;

import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.floorplan.M3GymFloorMap;
import io.m3fx.controls.scene3d.M3ModelViewer3D;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

/**
 * Controller skeleton for 2.5D Interactive Blueprint & 3D Spatial Linking screen.
 * Conforms strictly to docs/ui/facility/GYM_FLOOR_MAP_UI.md Section 7 contract.
 * Pure UI stub without backend services or SQL logic.
 */
public class GymFloorMapController {

    /** Zoom bounds and step for the blueprint viewport. */
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 1.15;

    @FXML private StackPane canvasStackPane;
    @FXML private M3GymFloorMap gymFloorMap;
    @FXML private M3Chip occupancyChip;
    @FXML private Label zoomPercentLabel;

    @FXML private M3Chip layerPinsChip;
    @FXML private M3Chip layerHeatmapChip;
    @FXML private M3Chip layerZonesChip;
    @FXML private M3Chip layerSensorsChip;

    @FXML private M3GlassPane inspectionGlassCard;
    @FXML private Label inspectEquipmentNameLabel;
    @FXML private M3Chip inspectStatusChip;
    @FXML private M3Chip inspectConditionChip;
    @FXML private M3ModelViewer3D inspectModelViewer3D;
    @FXML private Label inspectLastMaintLabel;

    /** Scales the blueprint inside the clipped viewport; 1.0 = 100%. */
    private final Scale mapScale = new Scale(1.0, 1.0);

    public GymFloorMapController() {
        // Public no-arg constructor required by FXMLLoader
    }

    @FXML
    public void initialize() {
        // Clip the zoomed blueprint to the canvas viewport so it scales instead of spilling
        // over the telemetry rail; keep zoom pivoted at the center of the map.
        if (canvasStackPane != null) {
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(canvasStackPane.widthProperty());
            clip.heightProperty().bind(canvasStackPane.heightProperty());
            canvasStackPane.setClip(clip);
        }
        if (gymFloorMap != null) {
            mapScale.pivotXProperty().bind(gymFloorMap.widthProperty().divide(2));
            mapScale.pivotYProperty().bind(gymFloorMap.heightProperty().divide(2));
            gymFloorMap.getTransforms().add(mapScale);
            // One-click 3D inspection: open the floating card for the tapped machine pin.
            gymFloorMap.setOnInspect3D(this::showInspectionCard);
        }
    }

    private void showInspectionCard(M3GymFloorMap.EquipmentNode node) {
        if (inspectionGlassCard == null || node == null) {
            return;
        }
        if (inspectEquipmentNameLabel != null) {
            inspectEquipmentNameLabel.setText(node.name + " #" + node.id);
        }
        if (inspectStatusChip != null && node.status != null) {
            inspectStatusChip.setText(node.status.label.toUpperCase());
        }
        if (inspectLastMaintLabel != null) {
            inspectLastMaintLabel.setText("Operator: "
                    + (node.currentUser == null || node.currentUser.isBlank() ? "—" : node.currentUser)
                    + " · " + node.timeRemainingMinutes + " min remaining");
        }
        if (inspectModelViewer3D != null && node.modelType != null) {
            inspectModelViewer3D.setCurrentModel(node.modelType);
        }
        inspectionGlassCard.setVisible(true);
    }

    private void applyZoom(double factor) {
        double zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, mapScale.getX() * factor));
        mapScale.setX(zoom);
        mapScale.setY(zoom);
        if (zoomPercentLabel != null) {
            zoomPercentLabel.setText(Math.round(zoom * 100) + "%");
        }
    }

    @FXML
    public void onZoomIn() {
        applyZoom(ZOOM_STEP);
    }

    @FXML
    public void onZoomIn(ActionEvent event) {
        onZoomIn();
    }

    @FXML
    public void onZoomOut() {
        applyZoom(1.0 / ZOOM_STEP);
    }

    @FXML
    public void onZoomOut(ActionEvent event) {
        onZoomOut();
    }

    @FXML
    public void onResetView() {
        mapScale.setX(1.0);
        mapScale.setY(1.0);
        if (zoomPercentLabel != null) {
            zoomPercentLabel.setText("100%");
        }
    }

    @FXML
    public void onResetView(ActionEvent event) {
        onResetView();
    }

    @FXML
    public void onTogglePinsLayer() {
        // No-op toggle pins layer handler
    }

    @FXML
    public void onTogglePinsLayer(ActionEvent event) {
        onTogglePinsLayer();
    }

    @FXML
    public void onToggleHeatmapLayer() {
        // No-op toggle heatmap layer handler
    }

    @FXML
    public void onToggleHeatmapLayer(ActionEvent event) {
        onToggleHeatmapLayer();
    }

    @FXML
    public void onToggleZonesLayer() {
        // No-op toggle zones layer handler
    }

    @FXML
    public void onToggleZonesLayer(ActionEvent event) {
        onToggleZonesLayer();
    }

    @FXML
    public void onToggleSensorsLayer() {
        // No-op toggle sensors layer handler
    }

    @FXML
    public void onToggleSensorsLayer(ActionEvent event) {
        onToggleSensorsLayer();
    }

    @FXML
    public void onCloseInspection() {
        if (inspectionGlassCard != null) {
            inspectionGlassCard.setVisible(false);
        }
    }

    @FXML
    public void onCloseInspection(ActionEvent event) {
        onCloseInspection();
    }

    @FXML
    public void onScheduleServiceFromMap() {
        // No-op schedule service handler
    }

    @FXML
    public void onScheduleServiceFromMap(ActionEvent event) {
        onScheduleServiceFromMap();
    }

    @FXML
    public void onOpenFullAssetDetails() {
        // No-op open asset details handler
    }

    @FXML
    public void onOpenFullAssetDetails(ActionEvent event) {
        onOpenFullAssetDetails();
    }

    @FXML
    public void onPlacePin() {
        // No-op place pin handler
    }

    @FXML
    public void onPlacePin(ActionEvent event) {
        onPlacePin();
    }

    @FXML
    public void onTogglePerspective() {
        // No-op toggle perspective handler
    }

    @FXML
    public void onTogglePerspective(ActionEvent event) {
        onTogglePerspective();
    }

    @FXML
    public void onExportBlueprint() {
        // No-op export blueprint handler
    }

    @FXML
    public void onExportBlueprint(ActionEvent event) {
        onExportBlueprint();
    }
}
