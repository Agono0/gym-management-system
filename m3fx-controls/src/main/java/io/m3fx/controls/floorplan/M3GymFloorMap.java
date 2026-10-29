package io.m3fx.controls.floorplan;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.controls.scene3d.M3ModelViewer3D.ModelType;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interactive 2D/2.5D Gym Facility Floorplan & Equipment Zone Map displaying real-time
 * equipment occupancy status pins, zone heat boundaries, and seamless one-click 3D inspection.
 */
public class M3GymFloorMap extends StackPane {

    /** Design-space width all equipment/zone coordinates are expressed against. */
    private static final double DESIGN_WIDTH = 720.0;
    /** Design-space height all equipment/zone coordinates are expressed against. */
    private static final double DESIGN_HEIGHT = 420.0;

    private static final Font ZONE_TITLE_FONT = Font.font("Roboto", FontWeight.BOLD, 10);
    private static final Font PIN_LABEL_FONT = Font.font("Roboto", FontWeight.BOLD, 10);

    /**
     * Equipment operational status. Status colors are resolved from M3 theme roles at draw
     * time (see {@link #statusColor}) — primary for available, tertiary for in use and error
     * for maintenance — so pins follow the active seed and light/dark variant.
     */
    public enum EquipmentStatus {
        AVAILABLE("Available"),
        IN_USE("In Use"),
        MAINTENANCE("Maintenance");

        public final String label;

        EquipmentStatus(String label) {
            this.label = label;
        }
    }

    /** Gym fitness zone descriptor. Coordinates are expressed in the 720x420 design space. */
    public static class ZoneArea {
        public final String name;
        public final double x, y, width, height;
        public final Color color;

        public ZoneArea(String name, double x, double y, double width, double height, Color color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
        }
    }

    /** Interactive equipment map node. */
    public static class EquipmentNode {
        public final String id;
        public final String name;
        public final ModelType modelType;
        public final double x, y;
        public EquipmentStatus status;
        public String currentUser;
        public int timeRemainingMinutes;

        public EquipmentNode(String id, String name, ModelType modelType, double x, double y,
                             EquipmentStatus status, String currentUser, int timeRemainingMinutes) {
            this.id = id;
            this.name = name;
            this.modelType = modelType;
            this.x = x;
            this.y = y;
            this.status = status;
            this.currentUser = currentUser;
            this.timeRemainingMinutes = timeRemainingMinutes;
        }
    }

    private final Canvas canvas = new Canvas();
    private final List<ZoneArea> zones = new ArrayList<>();
    private final ObservableList<EquipmentNode> equipmentList = FXCollections.observableArrayList();

    private final ObjectProperty<Consumer<EquipmentNode>> onInspect3D =
            new SimpleObjectProperty<>(this, "onInspect3D", null);

    private EquipmentNode hoveredNode = null;
    private EquipmentNode selectedNode = null;

    private final M3Card detailCard = new M3Card(CardVariant.ELEVATED);
    private final Label detailTitle = new Label();
    private final Label detailStatus = new Label();
    private final Label detailUser = new Label();
    private final M3Button inspect3DBtn = new M3Button("View in 3D Inspector", ButtonVariant.FILLED);

    /** Creates an interactive gym floorplan map with default zones and equipment. */
    public M3GymFloorMap() {
        getStyleClass().add("m3-gym-floor-map");

        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        buildDefaultZones();
        buildDefaultEquipment();

        initDetailCard();

        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            hoveredNode = null;
            setCursor(Cursor.DEFAULT);
            redraw();
        });

        widthProperty().addListener(e -> redraw());
        heightProperty().addListener(e -> redraw());
        sceneProperty().addListener((obs, o, n) -> redraw());

        setPrefSize(720, 420);
        setMinSize(480, 300);
    }

    private void buildDefaultZones() {
        // Zone accents come from theme roles so the map adapts to any seed/variant.
        zones.add(new ZoneArea("FREE WEIGHTS ARENA", 20, 20, 320, 180,
                ThemeColors.resolve(this, ColorRole.PRIMARY)));
        zones.add(new ZoneArea("CARDIO DECK", 360, 20, 320, 180,
                ThemeColors.resolve(this, ColorRole.TERTIARY)));
        zones.add(new ZoneArea("FUNCTIONAL CROSSFIT CAGE", 20, 220, 320, 180,
                ThemeColors.resolve(this, ColorRole.SECONDARY)));
        zones.add(new ZoneArea("RECOVERY LOUNGE & SPA", 360, 220, 320, 180,
                ThemeColors.resolve(this, ColorRole.INVERSE_PRIMARY)));
    }

    private void buildDefaultEquipment() {
        equipmentList.addAll(
                new EquipmentNode("EQ-01", "Power Squat Cage & Barbell", ModelType.SQUAT_RACK, 80, 80,
                        EquipmentStatus.IN_USE, "Alexander Vance", 8),
                new EquipmentNode("EQ-02", "Dual Pulley Cable Crossover", ModelType.CABLE_CROSSOVER, 180, 80,
                        EquipmentStatus.AVAILABLE, null, 0),
                new EquipmentNode("EQ-03", "Commercial 45° Leg Press", ModelType.LEG_PRESS, 280, 80,
                        EquipmentStatus.IN_USE, "Marcus Chen", 12),
                new EquipmentNode("EQ-04", "Olympic Flat Barbell Bench", ModelType.BARBELL, 100, 150,
                        EquipmentStatus.AVAILABLE, null, 0),
                new EquipmentNode("EQ-05", "Pro Hex Dumbbells (5-50kg)", ModelType.DUMBBELL, 200, 150,
                        EquipmentStatus.AVAILABLE, null, 0),
                new EquipmentNode("EQ-06", "Preacher Bicep Curl Bench", ModelType.PREACHER_BENCH, 290, 150,
                        EquipmentStatus.IN_USE, "Lucas Grey", 14),

                new EquipmentNode("EQ-07", "Commercial HIIT Treadmill #1", ModelType.TREADMILL, 420, 80,
                        EquipmentStatus.AVAILABLE, null, 0),
                new EquipmentNode("EQ-08", "Magnetic Exercise Bike #2", ModelType.EXERCISE_BIKE, 520, 80,
                        EquipmentStatus.MAINTENANCE, null, 0),
                new EquipmentNode("EQ-09", "Water Ergometer Rowing Machine", ModelType.ROWING_MACHINE, 620, 80,
                        EquipmentStatus.IN_USE, "Elena Rostova", 18),

                new EquipmentNode("EQ-10", "Heavy Leather Punching Bag", ModelType.PUNCHING_BAG, 100, 290,
                        EquipmentStatus.IN_USE, "Tyson Fury", 7),
                new EquipmentNode("EQ-11", "Competition Kettlebell Station", ModelType.KETTLEBELL, 220, 290,
                        EquipmentStatus.AVAILABLE, null, 0),
                new EquipmentNode("EQ-12", "Multi-Angle Utility Bench", ModelType.WORKOUT_BENCH, 480, 290,
                        EquipmentStatus.AVAILABLE, null, 0)
        );
    }

    private void initDetailCard() {
        detailCard.setVisible(false);
        detailCard.setManaged(false);
        detailCard.setMaxSize(240, 160);

        detailTitle.setFont(Font.font("Roboto", FontWeight.BOLD, 13));
        detailTitle.setStyle("-fx-text-fill: -md-sys-color-on-surface;");

        detailStatus.setFont(Font.font("Roboto", FontWeight.BOLD, 11));
        detailUser.setFont(Font.font("Roboto", FontWeight.NORMAL, 11));
        detailUser.setStyle("-fx-text-fill: -md-sys-color-on-surface-variant;");

        inspect3DBtn.setOnAction(e -> {
            if (selectedNode != null && getOnInspect3D() != null) {
                getOnInspect3D().accept(selectedNode);
            }
        });

        VBox content = new VBox(6, detailTitle, detailStatus, detailUser, inspect3DBtn);
        content.setPadding(new Insets(12));
        detailCard.setContent(content);

        getChildren().add(detailCard);
    }

    /** Returns the current design-space-to-actual horizontal scale factor. */
    private double scaleX() {
        return getWidth() > 0 ? getWidth() / DESIGN_WIDTH : 1.0;
    }

    /** Returns the current design-space-to-actual vertical scale factor. */
    private double scaleY() {
        return getHeight() > 0 ? getHeight() / DESIGN_HEIGHT : 1.0;
    }

    private void handleMouseMoved(MouseEvent e) {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        // Hit test in design space so it matches the scaled drawing.
        double mx = e.getX() / scaleX();
        double my = e.getY() / scaleY();
        EquipmentNode found = null;

        for (EquipmentNode eq : equipmentList) {
            double dx = mx - eq.x;
            double dy = my - eq.y;
            if (dx * dx + dy * dy <= 225) { // 15px design-space radius hit test
                found = eq;
                break;
            }
        }

        if (found != hoveredNode) {
            hoveredNode = found;
            setCursor(hoveredNode != null ? Cursor.HAND : Cursor.DEFAULT);
            redraw();
        }
    }

    private void handleMouseClicked(MouseEvent e) {
        if (hoveredNode != null) {
            selectedNode = hoveredNode;
            showDetailCard(selectedNode);
        } else {
            selectedNode = null;
            detailCard.setVisible(false);
        }
        redraw();
    }

    private void showDetailCard(EquipmentNode node) {
        detailTitle.setText(node.name);
        detailStatus.setText("Status: " + node.status.label.toUpperCase());
        detailStatus.setStyle("-fx-text-fill: " + toHex(statusColor(node.status)) + ";");

        if (node.status == EquipmentStatus.IN_USE) {
            String occupant = (node.currentUser == null || node.currentUser.isBlank())
                    ? "Unknown" : node.currentUser;
            detailUser.setText("Occupied by: " + occupant
                    + "\nTime left: ~" + node.timeRemainingMinutes + " min");
        } else if (node.status == EquipmentStatus.MAINTENANCE) {
            detailUser.setText("Under routine technician maintenance");
        } else {
            detailUser.setText("Open for all members right now");
        }

        // Position popup near the node (design coords scaled to the live size, clamped inside).
        double cardX = Math.max(6, Math.min(getWidth() - 250, node.x * scaleX() + 18));
        double cardY = Math.max(6, Math.min(getHeight() - 170, node.y * scaleY() - 40));
        detailCard.setLayoutX(cardX);
        detailCard.setLayoutY(cardY);
        detailCard.setVisible(true);
    }

    /** Resolves the theme color role standing in for each equipment status. */
    private Color statusColor(EquipmentStatus status) {
        return switch (status) {
            case AVAILABLE -> ThemeColors.resolve(this, ColorRole.PRIMARY);
            case IN_USE -> ThemeColors.resolve(this, ColorRole.TERTIARY);
            case MAINTENANCE -> ThemeColors.resolve(this, ColorRole.ERROR);
        };
    }

    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0 || !Double.isFinite(w) || !Double.isFinite(h)) return;
        w = Math.min(2560.0, w);
        h = Math.min(1600.0, h);

        double sx = w / DESIGN_WIDTH;
        double sy = h / DESIGN_HEIGHT;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // 1. Cyber Blueprint Grid
        Color gridColor = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);
        gc.setStroke(Color.color(gridColor.getRed(), gridColor.getGreen(), gridColor.getBlue(), 0.12));
        gc.setLineWidth(1.0);
        for (double x = 0; x < w; x += 24) {
            gc.strokeLine(x, 0, x, h);
        }
        for (double y = 0; y < h; y += 24) {
            gc.strokeLine(0, y, w, y);
        }

        // 2. Gym Zones (design coords scaled to the live size)
        Color pinLabelColor = ThemeColors.resolve(this, ColorRole.ON_SURFACE);
        Color pinRingColor = ThemeColors.resolve(this, ColorRole.SURFACE);
        for (ZoneArea z : zones) {
            double zx = z.x * sx;
            double zy = z.y * sy;
            double zw = z.width * sx;
            double zh = z.height * sy;

            // Zone fill
            Color fill = Color.color(z.color.getRed(), z.color.getGreen(), z.color.getBlue(), 0.08);
            gc.setFill(fill);
            gc.fillRoundRect(zx, zy, zw, zh, 14, 14);

            // Zone border
            Color border = Color.color(z.color.getRed(), z.color.getGreen(), z.color.getBlue(), 0.35);
            gc.setStroke(border);
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(zx, zy, zw, zh, 14, 14);

            // Zone Title
            gc.setFill(z.color);
            gc.setFont(ZONE_TITLE_FONT);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setTextBaseline(VPos.TOP);
            gc.fillText(z.name, zx + 12, zy + 10);
        }

        // 3. Equipment Pins
        for (EquipmentNode eq : equipmentList) {
            boolean isHover = (eq == hoveredNode);
            boolean isSelect = (eq == selectedNode);
            double r = isHover || isSelect ? 13.0 : 9.0;
            double px = eq.x * sx;
            double py = eq.y * sy;
            Color statusCol = statusColor(eq.status);

            // Halo glow
            if (isHover || isSelect) {
                Color halo = Color.color(
                        statusCol.getRed(), statusCol.getGreen(), statusCol.getBlue(), 0.35);
                gc.setFill(halo);
                gc.fillOval(px - r * 1.8, py - r * 1.8, r * 3.6, r * 3.6);
            }

            // Core pin
            gc.setFill(statusCol);
            gc.fillOval(px - r, py - r, r * 2, r * 2);

            gc.setStroke(pinRingColor);
            gc.setLineWidth(2.0);
            gc.strokeOval(px - r, py - r, r * 2, r * 2);

            // Label beside pin
            gc.setFill(pinLabelColor);
            gc.setFont(PIN_LABEL_FONT);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.TOP);
            gc.fillText(eq.id, px, py + r + 4);
        }
    }

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    /** Returns the live equipment list rendered as pins on the map. */
    public ObservableList<EquipmentNode> getEquipmentList() {
        return equipmentList;
    }

    /** Callback property invoked when the user requests the 3D inspector for a node. */
    public ObjectProperty<Consumer<EquipmentNode>> onInspect3DProperty() {
        return onInspect3D;
    }

    /** Returns the current 3D-inspect callback. */
    public Consumer<EquipmentNode> getOnInspect3D() {
        return onInspect3D.get();
    }

    /**
     * Sets the 3D-inspect callback, invoked with the selected equipment node.
     *
     * @param onInspect3D callback, may be {@code null} to disable
     */
    public void setOnInspect3D(Consumer<EquipmentNode> onInspect3D) {
        this.onInspect3D.set(onInspect3D);
    }
}
