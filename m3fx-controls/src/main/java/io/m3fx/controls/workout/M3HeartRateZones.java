package io.m3fx.controls.workout;

import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.controls.effect.M3PulseBeacon.BeaconStatus;
import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * 5-Zone Cardiac Intensity Biometrics Visualizer adhering to sports science standards
 * with real-time spring marker tracking and live telemetry pulse indications.
 */
public class M3HeartRateZones extends VBox {

    /**
     * Cardiac intensity zones. Zone colors are derived from M3 theme roles at draw time
     * (see {@link #zoneColor}) rather than fixed hex values, so the bar adapts to any
     * seed and light/dark variant.
     */
    public enum Zone {
        ZONE_1("WARM UP", 0.50, 0.60),
        ZONE_2("FAT BURN", 0.60, 0.70),
        ZONE_3("AEROBIC", 0.70, 0.80),
        ZONE_4("ANAEROBIC", 0.80, 0.90),
        ZONE_5("REDLINE PEAK", 0.90, 1.00);

        public final String title;
        public final double minRatio;
        public final double maxRatio;

        Zone(String title, double minRatio, double maxRatio) {
            this.title = title;
            this.minRatio = minRatio;
            this.maxRatio = maxRatio;
        }
    }

    private static final Font ZONE_LABEL_FONT = Font.font("Roboto", FontWeight.BOLD, 9);

    private final IntegerProperty currentBpm = new SimpleIntegerProperty(this, "currentBpm", 142);
    private final IntegerProperty maxHr = new SimpleIntegerProperty(this, "maxHr", 190);
    private final DoubleProperty animatedBpm = new SimpleDoubleProperty(this, "animatedBpm", 142.0);

    private final Canvas barCanvas = new Canvas();
    private final Label readoutLabel = new Label();
    private final M3PulseBeacon statusBeacon = new M3PulseBeacon(BeaconStatus.ONLINE, "");

    /** Creates a heart rate zone visualizer with default 190 max HR. */
    public M3HeartRateZones() {
        this(190);
    }

    /**
     * Creates a heart rate zone visualizer with specified maximum heart rate.
     *
     * @param maxHeartRate maximum heart rate threshold (BPM)
     */
    public M3HeartRateZones(int maxHeartRate) {
        getStyleClass().add("m3-heart-rate-zones");
        setSpacing(10);
        setPadding(new Insets(14));

        setMaxHr(maxHeartRate);

        readoutLabel.getStyleClass().add("m3-heart-rate-readout");
        readoutLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 15));

        HBox headerRow = new HBox(10, statusBeacon, readoutLabel);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        barCanvas.widthProperty().bind(widthProperty().subtract(28));
        barCanvas.setHeight(48);

        getChildren().addAll(headerRow, barCanvas);

        currentBpm.addListener((obs, o, n) -> {
            SpringAnimation.animate(animatedBpm, n.doubleValue(), SpringAnimation.FAST, null);
            updateReadout();
        });

        animatedBpm.addListener(e -> redraw());
        widthProperty().addListener(e -> redraw());
        maxHr.addListener(e -> {
            updateReadout();
            redraw();
        });
        sceneProperty().addListener((obs, o, n) -> redraw());

        updateReadout();
        redraw();
    }

    /** Determines the current cardiac intensity zone based on active BPM. */
    public Zone getCurrentZone() {
        double ratio = (double) getCurrentBpm() / Math.max(1, getMaxHr());
        if (ratio < 0.60) return Zone.ZONE_1;
        if (ratio < 0.70) return Zone.ZONE_2;
        if (ratio < 0.80) return Zone.ZONE_3;
        if (ratio < 0.90) return Zone.ZONE_4;
        return Zone.ZONE_5;
    }

    private void updateReadout() {
        int bpm = getCurrentBpm();
        int max = Math.max(1, getMaxHr());
        int pct = (int) Math.round(((double) bpm / max) * 100);
        Zone zone = getCurrentZone();

        readoutLabel.setText(String.format("%d BPM  •  %d%% MAX HR  •  %s (%s)",
                bpm, pct, zone.name().replace('_', ' '), zone.title));

        // Update telemetry status beacon
        switch (zone) {
            case ZONE_1 -> statusBeacon.setStatus(BeaconStatus.ONLINE);
            case ZONE_2 -> statusBeacon.setStatus(BeaconStatus.SYNCING);
            case ZONE_3 -> statusBeacon.setStatus(BeaconStatus.ONLINE);
            case ZONE_4 -> statusBeacon.setStatus(BeaconStatus.WARNING);
            case ZONE_5 -> statusBeacon.setStatus(BeaconStatus.CRITICAL);
        }
    }

    /**
     * Resolves the theme color role standing in for each cardiac zone. M3 defines no
     * success/warning tokens, so the intensity ramp maps to: muted (warm-up), tertiary,
     * primary, secondary and error (redline).
     */
    private Color zoneColor(Zone zone) {
        return switch (zone) {
            case ZONE_1 -> ThemeColors.resolve(this, ColorRole.ON_SURFACE_VARIANT);
            case ZONE_2 -> ThemeColors.resolve(this, ColorRole.TERTIARY);
            case ZONE_3 -> ThemeColors.resolve(this, ColorRole.PRIMARY);
            case ZONE_4 -> ThemeColors.resolve(this, ColorRole.SECONDARY);
            case ZONE_5 -> ThemeColors.resolve(this, ColorRole.ERROR);
        };
    }

    private void redraw() {
        double w = barCanvas.getWidth();
        double h = barCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = barCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double trackY = 8.0;
        double trackH = 18.0;
        double segmentGap = 4.0;
        Zone[] zones = Zone.values();
        double usableW = w - (segmentGap * (zones.length - 1));
        double segW = usableW / zones.length;

        // Resolve once per redraw, not once per segment.
        Color labelColor = ThemeColors.resolve(this, ColorRole.ON_SURFACE_VARIANT);

        // 1. Draw 5 Zone Segments
        for (int i = 0; i < zones.length; i++) {
            Zone z = zones[i];
            double segX = i * (segW + segmentGap);

            gc.setFill(zoneColor(z));
            gc.fillRoundRect(segX, trackY, segW, trackH, 6, 6);

            // Subtitle label underneath
            gc.setFill(labelColor);
            gc.setFont(ZONE_LABEL_FONT);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.TOP);
            gc.fillText("Z" + (i + 1) + " " + z.title, segX + segW / 2.0, trackY + trackH + 6);
        }

        // 2. Marker Pin for active animated BPM
        double curBpm = getAnimatedBpm();
        double max = Math.max(1, getMaxHr());
        double minDisplayBpm = max * 0.50; // starts at 50%
        double norm = (curBpm - minDisplayBpm) / (max - minDisplayBpm);
        norm = Math.max(0.0, Math.min(1.0, norm));

        double markerX = norm * w;

        // Marker indicator triangle (token-driven: on-surface fill with a surface halo)
        Color markerFill = ThemeColors.resolve(this, ColorRole.ON_SURFACE);
        Color markerHalo = ThemeColors.resolve(this, ColorRole.SURFACE);
        gc.setFill(markerFill);
        gc.setStroke(markerHalo);
        gc.setLineWidth(1.0);

        double[] tx = { markerX - 6, markerX + 6, markerX };
        double[] ty = { trackY - 7, trackY - 7, trackY };
        gc.fillPolygon(tx, ty, 3);
        gc.strokePolygon(tx, ty, 3);

        // Vertical indicator line across track
        gc.setStroke(markerFill);
        gc.setLineWidth(2.0);
        gc.strokeLine(markerX, trackY, markerX, trackY + trackH);
    }

    /** Live heart rate in BPM; drives the spring-animated marker. */
    public IntegerProperty currentBpmProperty() {
        return currentBpm;
    }

    public int getCurrentBpm() {
        return currentBpm.get();
    }

    public void setCurrentBpm(int bpm) {
        this.currentBpm.set(bpm);
    }

    /** Maximum heart rate in BPM used to normalize zones. */
    public IntegerProperty maxHrProperty() {
        return maxHr;
    }

    public int getMaxHr() {
        return maxHr.get();
    }

    public void setMaxHr(int maxHr) {
        this.maxHr.set(maxHr);
    }

    /** Spring-animated BPM value that eases toward {@link #currentBpm}. */
    public DoubleProperty animatedBpmProperty() {
        return animatedBpm;
    }

    public double getAnimatedBpm() {
        return animatedBpm.get();
    }
}

