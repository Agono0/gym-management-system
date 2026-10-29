package io.m3fx.controls.effect;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Futuristic Holographic UI Container featuring animated laser scan sweeps,
 * HUD corner reticle brackets, cybernetic dot matrix backdrop, and glowing border accents.
 */
public class M3Hologram extends StackPane {

    private final BooleanProperty scanning = new SimpleBooleanProperty(this, "scanning", true);
    private final StringProperty title = new SimpleStringProperty(this, "title", "HOLO-FIELD // ONLINE");

    private final Canvas overlayCanvas = new Canvas();
    private final StackPane contentWrapper = new StackPane();
    private final Label titleBadge = new Label();

    // Theme colors cached per attach (ThemeColors must never be resolved per animation frame).
    private Color cachedPrimary;
    private Color cachedOutline;
    // Static dot-grid + bracket layer, snapshotted once per size so the per-frame cost is one drawImage.
    private Image staticLayer;

    private double scanY = 0.0;
    private AnimationTimer timer;

    /** Creates an empty holographic container. */
    public M3Hologram() {
        this(null);
    }

    /**
     * Creates a holographic container wrapping the provided content.
     *
     * @param content child node
     */
    public M3Hologram(Node content) {
        getStyleClass().add("m3-hologram");

        titleBadge.getStyleClass().add("m3-hologram-badge");
        titleBadge.textProperty().bind(title);
        titleBadge.setFont(Font.font("Roboto", FontWeight.BOLD, 10));

        overlayCanvas.setMouseTransparent(true);
        overlayCanvas.widthProperty().bind(widthProperty());
        overlayCanvas.heightProperty().bind(heightProperty());

        contentWrapper.getStyleClass().add("m3-hologram-content");
        contentWrapper.setPadding(new Insets(16));
        if (content != null) {
            contentWrapper.getChildren().add(content);
        }

        VBox layout = new VBox(6, titleBadge, contentWrapper);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.setPadding(new Insets(10));

        getChildren().addAll(overlayCanvas, layout);

        widthProperty().addListener(e -> renderHUD());
        heightProperty().addListener(e -> renderHUD());
        sceneProperty().addListener((obs, o, n) -> {
            cachedPrimary = null;
            cachedOutline = null;
            staticLayer = null;
            renderHUD();
            updateTimer(isScanning());
        });
        visibleProperty().addListener(e -> updateTimer(isScanning()));
        disabledProperty().addListener(e -> updateTimer(isScanning()));
        scanningProperty().addListener((obs, o, n) -> updateTimer(n));

        updateTimer(isScanning());
    }

    /** True when the scan sweep should currently be animating. */
    private boolean shouldRun() {
        return getScene() != null && isVisible() && !isDisable();
    }

    private void updateTimer(boolean active) {
        if (active && shouldRun()) {
            if (timer == null) {
                timer = new AnimationTimer() {
                    private long lastTime = 0;

                    @Override
                    public void handle(long now) {
                        if (lastTime == 0) {
                            lastTime = now;
                            return;
                        }
                        // Clamp dt so a long frame stall cannot fling the beam.
                        double dt = Math.min((now - lastTime) / 1_000_000_000.0, 0.1);
                        lastTime = now;

                        double h = getHeight();
                        if (h > 0) {
                            scanY = (scanY + dt * 75.0) % h;
                            renderHUD();
                        }
                    }
                };
            }
            timer.start();
        } else {
            if (timer != null) {
                timer.stop();
            }
        }
    }

    private void renderHUD() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (cachedPrimary == null || cachedOutline == null) {
            cachedPrimary = ThemeColors.resolve(this, ColorRole.PRIMARY);
            cachedOutline = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);
            staticLayer = null;
        }

        GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
        if (staticLayer == null
                || Math.abs(staticLayer.getWidth() - w) > 0.5
                || Math.abs(staticLayer.getHeight() - h) > 0.5) {
            gc.clearRect(0, 0, w, h);
            renderStaticLayer(gc, w, h);
            staticLayer = overlayCanvas.snapshot(null, null);
        } else {
            gc.clearRect(0, 0, w, h);
            gc.drawImage(staticLayer, 0, 0);
        }

        // 3. Animated Laser Scanline Sweep (the only per-frame work)
        if (isScanning()) {
            Color beamColor = Color.color(
                    cachedPrimary.getRed(), cachedPrimary.getGreen(), cachedPrimary.getBlue(), 0.35);
            gc.setStroke(beamColor);
            gc.setLineWidth(1.8);
            gc.strokeLine(8, scanY, w - 8, scanY);

            // Subtle glow halo around beam
            Color halo = Color.color(
                    cachedPrimary.getRed(), cachedPrimary.getGreen(), cachedPrimary.getBlue(), 0.08);
            gc.setFill(halo);
            gc.fillRect(8, Math.max(0, scanY - 8), w - 16, 16);
        }
    }

    /** Draws the size-invariant backdrop: cyber dot grid and corner reticle brackets. */
    private void renderStaticLayer(GraphicsContext gc, double w, double h) {
        // 1. Subtle Cyber Dot Grid
        gc.setFill(Color.color(
                cachedOutline.getRed(), cachedOutline.getGreen(), cachedOutline.getBlue(), 0.12));
        double dotSpacing = 24.0;
        for (double x = 12; x < w - 12; x += dotSpacing) {
            for (double y = 12; y < h - 12; y += dotSpacing) {
                gc.fillOval(x, y, 1.5, 1.5);
            }
        }

        // 2. Corner Sci-Fi Reticle Brackets
        double cornerLen = Math.min(22.0, Math.min(w, h) * 0.15);
        gc.setStroke(cachedPrimary);
        gc.setLineWidth(2.0);

        // Top-Left
        gc.strokeLine(4, 4 + cornerLen, 4, 4);
        gc.strokeLine(4, 4, 4 + cornerLen, 4);

        // Top-Right
        gc.strokeLine(w - 4 - cornerLen, 4, w - 4, 4);
        gc.strokeLine(w - 4, 4, w - 4, 4 + cornerLen);

        // Bottom-Left
        gc.strokeLine(4, h - 4 - cornerLen, 4, h - 4);
        gc.strokeLine(4, h - 4, 4 + cornerLen, h - 4);

        // Bottom-Right
        gc.strokeLine(w - 4 - cornerLen, h - 4, w - 4, h - 4);
        gc.strokeLine(w - 4, h - 4 - cornerLen, w - 4, h - 4);
    }

    public BooleanProperty scanningProperty() {
        return scanning;
    }

    public boolean isScanning() {
        return scanning.get();
    }

    public void setScanning(boolean scanning) {
        this.scanning.set(scanning);
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public StackPane getContentWrapper() {
        return contentWrapper;
    }

    public void setContent(Node content) {
        contentWrapper.getChildren().setAll(content);
    }
}

