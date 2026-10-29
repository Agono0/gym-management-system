package io.m3fx.controls.scene3d;

import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;

/**
 * M3 2.5D Parallax Tilt Card delivering holographic spatial depth, cursor-following perspective,
 * multiplane content floating, dynamic specular sheen, and spring-physics recovery.
 *
 * <p>Implemented strictly within 2D rectangular bounds (via Shear, Scale, Layer Parallax, and
 * Dynamic DropShadow) to guarantee 100% crash-free Prism rendering across all scene graphs.
 */
public class M3TiltCard3D extends StackPane {

    private final DoubleProperty maxTiltAngle = new SimpleDoubleProperty(this, "maxTiltAngle", 14.0);
    private final DoubleProperty currentRotateX = new SimpleDoubleProperty(this, "currentRotateX", 0.0);
    private final DoubleProperty currentRotateY = new SimpleDoubleProperty(this, "currentRotateY", 0.0);

    private final Shear shear = new Shear(0.0, 0.0);
    private final Scale scale = new Scale(1.0, 1.0);
    private final DropShadow dynamicShadow = new DropShadow();

    private final Pane glareOverlay = new Pane();
    private final StackPane contentContainer = new StackPane();

    // Cached per attach; ThemeColors must not be resolved on every mouse-move event.
    private Color cachedPrimary;

    /** Creates an empty 3D tilt card. */
    public M3TiltCard3D() {
        this(null);
    }

    /**
     * Creates a 3D tilt card with content.
     *
     * @param content child content
     */
    public M3TiltCard3D(Node content) {
        getStyleClass().add("m3-tilt-card-3d");

        // 2.5D transforms remain strictly within 2D RectBounds
        getTransforms().addAll(shear, scale);

        // Dynamic reactive drop shadow simulates spatial elevation lift
        dynamicShadow.setColor(Color.rgb(0, 0, 0, 0.22));
        dynamicShadow.setRadius(14.0);
        dynamicShadow.setOffsetY(4.0);
        setEffect(dynamicShadow);

        glareOverlay.getStyleClass().add("m3-tilt-card-glare");
        glareOverlay.setMouseTransparent(true);
        glareOverlay.setOpacity(0.0);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        contentContainer.getStyleClass().add("m3-tilt-card-content");
        if (content != null) {
            contentContainer.getChildren().add(content);
        }

        getChildren().addAll(contentContainer, glareOverlay);

        sceneProperty().addListener((obs, o, n) -> cachedPrimary = null);

        initGestures();

        setPrefSize(280, 180);
    }

    private void initGestures() {
        addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            double w = getWidth();
            double h = getHeight();
            if (w <= 0 || h <= 0) return;

            // Normalized coordinates from center: -1.0 to 1.0
            double nx = (e.getX() - (w / 2.0)) / (w / 2.0);
            double ny = (e.getY() - (h / 2.0)) / (h / 2.0);

            nx = Math.max(-1.0, Math.min(1.0, nx));
            ny = Math.max(-1.0, Math.min(1.0, ny));

            double targetDegX = -ny * getMaxTiltAngle();
            double targetDegY = nx * getMaxTiltAngle();

            SpringAnimation.animate(currentRotateX, targetDegX, SpringAnimation.FAST, null);
            SpringAnimation.animate(currentRotateY, targetDegY, SpringAnimation.FAST, null);

            // 2.5D perspective shear matrix
            double targetShearX = nx * 0.045;
            double targetShearY = ny * 0.045;
            SpringAnimation.animate(shear.xProperty(), targetShearX, SpringAnimation.FAST, null);
            SpringAnimation.animate(shear.yProperty(), targetShearY, SpringAnimation.FAST, null);

            // Slight scale depth expansion
            SpringAnimation.animate(scale.xProperty(), 1.02, SpringAnimation.FAST, null);
            SpringAnimation.animate(scale.yProperty(), 1.02, SpringAnimation.FAST, null);

            // Multiplane parallax content shift
            SpringAnimation.animate(contentContainer.translateXProperty(), nx * 7.0, SpringAnimation.FAST, null);
            SpringAnimation.animate(contentContainer.translateYProperty(), ny * 7.0, SpringAnimation.FAST, null);

            // Dynamic shadow cast in opposing direction of light source
            dynamicShadow.setOffsetX(-nx * 12.0);
            dynamicShadow.setOffsetY(-ny * 12.0 + 4.0);
            dynamicShadow.setRadius(16.0 + Math.sqrt(nx * nx + ny * ny) * 10.0);

            // Specular holographic sheen gradient across surface
            double intensity = Math.sqrt(nx * nx + ny * ny) * 0.45;
            if (cachedPrimary == null) {
                cachedPrimary = ThemeColors.resolve(this, ColorRole.PRIMARY);
            }
            Color glareColor = cachedPrimary;
            Color sheen = Color.color(
                    Math.min(1.0, glareColor.getRed() * 1.5),
                    Math.min(1.0, glareColor.getGreen() * 1.5),
                    Math.min(1.0, glareColor.getBlue() * 1.5),
                    intensity
            );

            LinearGradient glareGrad = new LinearGradient(
                    0.5 - nx * 0.4, 0.5 - ny * 0.4,
                    0.5 + nx * 0.4, 0.5 + ny * 0.4,
                    true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.TRANSPARENT),
                    new Stop(0.5, sheen),
                    new Stop(1.0, Color.TRANSPARENT)
            );
            glareOverlay.setBackground(new Background(
                    new BackgroundFill(glareGrad, new CornerRadii(12), null)));
            glareOverlay.setOpacity(1.0);
        });

        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            // Spring recovery back to resting state
            SpringAnimation.animate(currentRotateX, 0.0, SpringAnimation.BOUNCY, null);
            SpringAnimation.animate(currentRotateY, 0.0, SpringAnimation.BOUNCY, null);

            SpringAnimation.animate(shear.xProperty(), 0.0, SpringAnimation.BOUNCY, null);
            SpringAnimation.animate(shear.yProperty(), 0.0, SpringAnimation.BOUNCY, null);

            SpringAnimation.animate(scale.xProperty(), 1.0, SpringAnimation.BOUNCY, null);
            SpringAnimation.animate(scale.yProperty(), 1.0, SpringAnimation.BOUNCY, null);

            SpringAnimation.animate(contentContainer.translateXProperty(), 0.0, SpringAnimation.BOUNCY, null);
            SpringAnimation.animate(contentContainer.translateYProperty(), 0.0, SpringAnimation.BOUNCY, null);

            dynamicShadow.setOffsetX(0.0);
            dynamicShadow.setOffsetY(4.0);
            dynamicShadow.setRadius(14.0);

            SpringAnimation.animate(glareOverlay.opacityProperty(), 0.0, SpringAnimation.FAST, null);
        });
    }

    public DoubleProperty maxTiltAngleProperty() {
        return maxTiltAngle;
    }

    public double getMaxTiltAngle() {
        return maxTiltAngle.get();
    }

    public void setMaxTiltAngle(double angle) {
        this.maxTiltAngle.set(angle);
    }

    public DoubleProperty currentRotateXProperty() {
        return currentRotateX;
    }

    public DoubleProperty currentRotateYProperty() {
        return currentRotateY;
    }

    public StackPane getContentContainer() {
        return contentContainer;
    }

    /** Returns the single content node (for FXML &lt;content&gt; support), or null if empty. */
    public Node getContent() {
        return contentContainer.getChildren().isEmpty() ? null : contentContainer.getChildren().get(0);
    }

    public void setContent(Node content) {
        contentContainer.getChildren().setAll(content);
    }
}
