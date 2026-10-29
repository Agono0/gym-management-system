package io.m3fx.controls.effect;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.animation.AnimationTimer;
import javafx.beans.property.*;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Real-time Telemetry Status Beacon featuring concentric pulsing sonar ripple waves
 * and M3 role-driven status colors.
 */
public class M3PulseBeacon extends HBox {

    /** Status states for the telemetry beacon. */
    public enum BeaconStatus {
        ONLINE,
        WARNING,
        CRITICAL,
        SYNCING,
        OFFLINE
    }

    private final ObjectProperty<BeaconStatus> status =
            new SimpleObjectProperty<>(this, "status", BeaconStatus.ONLINE);
    private final BooleanProperty pulsing =
            new SimpleBooleanProperty(this, "pulsing", true);
    private final StringProperty labelText =
            new SimpleStringProperty(this, "labelText", "");

    private final Canvas canvas = new Canvas(28, 28);
    private final Label label = new Label();

    private double wavePhase = 0.0;
    private Color cachedColor;
    private AnimationTimer loop;

    /** Creates an online telemetry beacon without text. */
    public M3PulseBeacon() {
        this(BeaconStatus.ONLINE, "");
    }

    /**
     * Creates a telemetry beacon with specified status and descriptive label.
     *
     * @param initialStatus telemetry status
     * @param text label beside beacon
     */
    public M3PulseBeacon(BeaconStatus initialStatus, String text) {
        getStyleClass().add("m3-pulse-beacon");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);

        label.getStyleClass().add("m3-pulse-beacon-label");
        label.setFont(Font.font("Roboto", FontWeight.BOLD, 12));
        label.textProperty().bind(labelText);

        getChildren().add(canvas);
        if (text != null && !text.isEmpty()) {
            setLabelText(text);
            getChildren().add(label);
        }

        labelText.addListener((obs, o, n) -> {
            if (n != null && !n.isEmpty()) {
                if (!getChildren().contains(label)) {
                    getChildren().add(label);
                }
            } else {
                getChildren().remove(label);
            }
        });

        status.addListener((obs, o, n) -> {
            updateColor();
            redraw();
            // OFFLINE draws no waves, so the loop has nothing to animate.
            updateLoop(isPulsing());
        });
        pulsing.addListener((obs, o, n) -> updateLoop(n));
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            updateColor();
            redraw();
            updateLoop(isPulsing());
        });
        visibleProperty().addListener(e -> updateLoop(isPulsing()));
        disabledProperty().addListener(e -> updateLoop(isPulsing()));

        setStatus(initialStatus);
        updateColor();
        updateLoop(isPulsing());
    }

    /** True when the sonar pulse should currently be animating. */
    private boolean shouldRun() {
        return isPulsing()
                && getStatus() != BeaconStatus.OFFLINE
                && getScene() != null
                && isVisible()
                && !isDisable();
    }

    private void updateLoop(boolean active) {
        if (active && shouldRun()) {
            if (loop == null) {
                loop = new AnimationTimer() {
                    private long last = 0;

                    @Override
                    public void handle(long now) {
                        if (last == 0) {
                            last = now;
                            return;
                        }
                        // Clamp dt so a long frame stall cannot skip the pulse phase.
                        double dt = Math.min((now - last) / 1_000_000_000.0, 0.1);
                        last = now;

                        // Wave cycle period ~ 1.4 seconds
                        wavePhase = (wavePhase + dt * 0.72) % 1.0;
                        redraw();
                    }
                };
            }
            loop.start();
        } else {
            if (loop != null) {
                loop.stop();
            }
            redraw();
        }
    }

    private void updateColor() {
        cachedColor = resolveStatusColor(getStatus());
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, 28, 28);

        double cx = 14.0;
        double cy = 14.0;
        double baseRadius = 4.5;
        if (cachedColor == null) {
            updateColor();
        }
        Color baseColor = cachedColor;

        // Concentric Expanding Pulse Rings
        if (isPulsing() && getStatus() != BeaconStatus.OFFLINE) {
            double maxR = 12.0;

            // Wave 1
            double r1 = baseRadius + (maxR - baseRadius) * wavePhase;
            double a1 = (1.0 - wavePhase) * 0.55;
            gc.setStroke(Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), a1));
            gc.setLineWidth(1.8);
            gc.strokeOval(cx - r1, cy - r1, r1 * 2, r1 * 2);

            // Wave 2 (offset by 0.5 phase)
            double p2 = (wavePhase + 0.5) % 1.0;
            double r2 = baseRadius + (maxR - baseRadius) * p2;
            double a2 = (1.0 - p2) * 0.45;
            gc.setStroke(Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), a2));
            gc.setLineWidth(1.4);
            gc.strokeOval(cx - r2, cy - r2, r2 * 2, r2 * 2);
        }

        // Center Solid Status Orb
        gc.setFill(baseColor);
        gc.fillOval(cx - baseRadius, cy - baseRadius, baseRadius * 2, baseRadius * 2);

        // Subtle specular highlight on dot
        gc.setFill(Color.color(1, 1, 1, 0.6));
        gc.fillOval(cx - baseRadius * 0.5, cy - baseRadius * 0.5, baseRadius * 0.6, baseRadius * 0.6);
    }

    private Color resolveStatusColor(BeaconStatus s) {
        return switch (s) {
            case ONLINE -> ThemeColors.resolve(this, ColorRole.PRIMARY);
            case WARNING -> ThemeColors.resolve(this, ColorRole.TERTIARY);
            case CRITICAL -> ThemeColors.resolve(this, ColorRole.ERROR);
            case SYNCING -> ThemeColors.resolve(this, ColorRole.SECONDARY);
            case OFFLINE -> ThemeColors.resolve(this, ColorRole.OUTLINE);
        };
    }

    public ObjectProperty<BeaconStatus> statusProperty() {
        return status;
    }

    public BeaconStatus getStatus() {
        return status.get();
    }

    public void setStatus(BeaconStatus status) {
        this.status.set(status);
    }

    public BooleanProperty pulsingProperty() {
        return pulsing;
    }

    public boolean isPulsing() {
        return pulsing.get();
    }

    public void setPulsing(boolean pulsing) {
        this.pulsing.set(pulsing);
    }

    public StringProperty labelTextProperty() {
        return labelText;
    }

    public String getLabelText() {
        return labelText.get();
    }

    public void setLabelText(String labelText) {
        this.labelText.set(labelText);
    }
}

