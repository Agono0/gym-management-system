package io.m3fx.controls.scene3d;

import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Hardware-accelerated M3 3D Viewport featuring orbit camera controls, ambient and specular
 * point lighting, cyber-grid ground plane, and smooth spring-assisted zoom and rotation.
 */
public class M3Scene3D extends Region {

    /** Default orbit camera pose, shared by the constructor and {@link #resetCamera()}. */
    protected static final double DEFAULT_PITCH = -22.0;
    /** Default orbit camera yaw. */
    protected static final double DEFAULT_YAW = 35.0;
    /** Default orbit camera distance. */
    protected static final double DEFAULT_DISTANCE = 420.0;

    private final Group root3D = new Group();
    private final Group worldGroup = new Group();
    private final SubScene subScene;
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    private final DoubleProperty cameraPitch = new SimpleDoubleProperty(this, "cameraPitch", DEFAULT_PITCH);
    private final DoubleProperty cameraYaw = new SimpleDoubleProperty(this, "cameraYaw", DEFAULT_YAW);
    private final DoubleProperty cameraDistance = new SimpleDoubleProperty(this, "cameraDistance", DEFAULT_DISTANCE);

    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate cameraTranslate = new Translate(0, 0, 0);

    private final AmbientLight ambientLight = new AmbientLight();
    private final PointLight pointLight = new PointLight();
    private PhongMaterial gridMaterial;

    private double dragStartX;
    private double dragStartY;
    private double startPitch;
    private double startYaw;

    /** Creates a 3D scene viewport with default dimensions. */
    public M3Scene3D() {
        this(400, 300);
    }

    /**
     * Creates a 3D scene viewport with specified dimensions.
     *
     * @param width pref width
     * @param height pref height
     */
    public M3Scene3D(double width, double height) {
        getStyleClass().add("m3-scene-3d");

        subScene = new SubScene(root3D, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);

        // Configure Camera
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setFieldOfView(42.0);

        rotateX.angleProperty().bind(cameraPitch);
        rotateY.angleProperty().bind(cameraYaw);

        Group cameraPivot = new Group();
        cameraPivot.getTransforms().addAll(rotateY, rotateX);
        cameraPivot.getChildren().add(camera);

        cameraDistance.addListener((obs, o, d) -> camera.setTranslateZ(-d.doubleValue()));
        camera.setTranslateZ(-cameraDistance.get());

        subScene.setCamera(camera);

        // Lights
        ambientLight.setColor(Color.rgb(180, 180, 195, 0.7));
        pointLight.setColor(Color.WHITE);
        pointLight.setTranslateX(200);
        pointLight.setTranslateY(-300);
        pointLight.setTranslateZ(-350);

        root3D.getChildren().addAll(cameraPivot, ambientLight, pointLight, worldGroup);

        buildCyberGridFloor();

        getChildren().add(subScene);

        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());

        initOrbitControls();

        sceneProperty().addListener((obs, o, n) -> updateThemeColors());

        setPrefSize(width, height);
        setMinSize(160, 120);

        updateThemeColors();
    }

    private void updateThemeColors() {
        Color primary = ThemeColors.resolve(this, ColorRole.PRIMARY);
        pointLight.setColor(Color.color(
                Math.min(1.0, primary.getRed() * 0.4 + 0.6),
                Math.min(1.0, primary.getGreen() * 0.4 + 0.6),
                Math.min(1.0, primary.getBlue() * 0.4 + 0.6)
        ));
        if (gridMaterial != null) {
            gridMaterial.setDiffuseColor(Color.color(
                    primary.getRed(), primary.getGreen(), primary.getBlue(), 0.25));
        }
    }

    private void buildCyberGridFloor() {
        Group grid = new Group();
        int lines = 11;
        double size = 300.0;
        double step = size / (lines - 1);
        double half = size / 2.0;

        // Grid tint derived from the primary role; recolored by updateThemeColors().
        gridMaterial = new PhongMaterial(Color.TRANSPARENT);

        for (int i = 0; i < lines; i++) {
            double pos = -half + i * step;

            // X-parallel line
            Box lineX = new Box(size, 0.5, 0.5);
            lineX.setTranslateX(0);
            lineX.setTranslateY(80);
            lineX.setTranslateZ(pos);
            lineX.setMaterial(gridMaterial);

            // Z-parallel line
            Box lineZ = new Box(0.5, 0.5, size);
            lineZ.setTranslateX(pos);
            lineZ.setTranslateY(80);
            lineZ.setTranslateZ(0);
            lineZ.setMaterial(gridMaterial);

            grid.getChildren().addAll(lineX, lineZ);
        }
        worldGroup.getChildren().add(grid);
    }

    private void initOrbitControls() {
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
            startPitch = cameraPitch.get();
            startYaw = cameraYaw.get();
            // Hand control back to the pointer: cancel any running camera springs.
            SpringAnimation.stop(cameraPitch);
            SpringAnimation.stop(cameraYaw);
        });

        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;

            double newYaw = startYaw + dx * 0.45;
            double newPitch = Math.max(-85.0, Math.min(85.0, startPitch - dy * 0.45));

            cameraYaw.set(newYaw);
            cameraPitch.set(newPitch);
        });

        subScene.addEventHandler(ScrollEvent.SCROLL, e -> {
            double delta = e.getDeltaY();
            double newDist = Math.max(80.0, Math.min(1200.0, cameraDistance.get() - delta * 0.75));
            SpringAnimation.animate(cameraDistance, newDist, SpringAnimation.FAST, null);
        });
    }

    /** Returns the 3D world group where models and shapes are mounted. */
    public Group getWorldGroup() {
        return worldGroup;
    }

    /** Orbit camera pitch angle in degrees (clamped to ±85° while dragging). */
    public DoubleProperty cameraPitchProperty() {
        return cameraPitch;
    }

    /** Orbit camera yaw angle in degrees. */
    public DoubleProperty cameraYawProperty() {
        return cameraYaw;
    }

    /** Orbit camera distance from the pivot (clamped to 80..1200 while scrolling). */
    public DoubleProperty cameraDistanceProperty() {
        return cameraDistance;
    }

    /** Springs the camera back to its default orbit pose. */
    public void resetCamera() {
        SpringAnimation.animate(cameraPitch, DEFAULT_PITCH, SpringAnimation.BOUNCY, null);
        SpringAnimation.animate(cameraYaw, DEFAULT_YAW, SpringAnimation.BOUNCY, null);
        SpringAnimation.animate(cameraDistance, DEFAULT_DISTANCE, SpringAnimation.BOUNCY, null);
    }
}

