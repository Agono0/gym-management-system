package io.m3fx.controls.scene3d;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/**
 * 3D Concentric Gyroscope Rings rotating across three orthogonal axes with futuristic M3 materials.
 */
public class M3GyroRing3D extends M3Scene3D {

    private final BooleanProperty spinning = new SimpleBooleanProperty(this, "spinning", true);

    private final Group ringOuter = new Group();
    private final Group ringMiddle = new Group();
    private final Group ringInner = new Group();
    private final Sphere coreOrb = new Sphere(14.0);

    private final Rotate rotOuter = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotMiddle = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotInner = new Rotate(0, Rotate.Z_AXIS);

    private AnimationTimer loop;

    /** Creates a 3D Gyro Ring viewport. */
    public M3GyroRing3D() {
        super(260, 260);
        getStyleClass().add("m3-gyro-ring-3d");

        ringOuter.getTransforms().add(rotOuter);
        ringMiddle.getTransforms().add(rotMiddle);
        ringInner.getTransforms().add(rotInner);

        buildRing(ringOuter, 75.0, 3.5);
        buildRing(ringMiddle, 56.0, 3.0);
        buildRing(ringInner, 38.0, 2.5);

        ringOuter.getChildren().add(ringMiddle);
        ringMiddle.getChildren().add(ringInner);
        ringInner.getChildren().add(coreOrb);

        getWorldGroup().getChildren().add(ringOuter);

        spinningProperty().addListener((obs, o, n) -> updateSpin(n));
        sceneProperty().addListener((obs, o, n) -> {
            applyMaterials();
            updateSpin(isSpinning());
        });
        visibleProperty().addListener(e -> updateSpin(isSpinning()));
        disabledProperty().addListener(e -> updateSpin(isSpinning()));

        applyMaterials();
        updateSpin(isSpinning());
    }

    /** True when the gyro should currently be spinning. */
    private boolean shouldRun() {
        return isSpinning() && getScene() != null && isVisible() && !isDisable();
    }

    public BooleanProperty spinningProperty() {
        return spinning;
    }

    public boolean isSpinning() {
        return spinning.get();
    }

    public void setSpinning(boolean spin) {
        this.spinning.set(spin);
    }

    private void buildRing(Group parent, double radius, double thickness) {
        int segments = 24;
        double angleStep = (2 * Math.PI) / segments;
        double segLen = 2.0 * radius * Math.sin(angleStep / 2.0);

        for (int i = 0; i < segments; i++) {
            double angle = i * angleStep;
            double midAngle = angle + angleStep / 2.0;

            Cylinder seg = new Cylinder(thickness, segLen);
            seg.setTranslateX(radius * Math.cos(midAngle));
            seg.setTranslateZ(radius * Math.sin(midAngle));
            // A Cylinder's axis is local Y; a Y-axis rotation alone would leave every segment
            // standing vertically. Tip it into the XZ ring plane first, then aim it along the
            // ring tangent at its midpoint angle.
            seg.getTransforms().addAll(
                    new Rotate(90.0, Rotate.X_AXIS),
                    new Rotate(-Math.toDegrees(midAngle), Rotate.Y_AXIS));

            parent.getChildren().add(seg);
        }
    }

    private void applyMaterials() {
        Color primary = ThemeColors.resolve(this, ColorRole.PRIMARY);
        Color tertiary = ThemeColors.resolve(this, ColorRole.TERTIARY);
        Color secondary = ThemeColors.resolve(this, ColorRole.SECONDARY);

        PhongMaterial mat1 = new PhongMaterial(primary);
        mat1.setSpecularColor(Color.WHITE);
        mat1.setSpecularPower(32.0);

        PhongMaterial mat2 = new PhongMaterial(tertiary);
        mat2.setSpecularColor(Color.WHITE);
        mat2.setSpecularPower(32.0);

        PhongMaterial mat3 = new PhongMaterial(secondary);
        mat3.setSpecularColor(Color.WHITE);
        mat3.setSpecularPower(32.0);

        PhongMaterial orbMat = new PhongMaterial(Color.WHITE);
        orbMat.setSpecularColor(primary);
        orbMat.setSpecularPower(64.0);
        coreOrb.setMaterial(orbMat);

        setGroupMaterial(ringOuter, mat1);
        setGroupMaterial(ringMiddle, mat2);
        setGroupMaterial(ringInner, mat3);
    }

    private void setGroupMaterial(Group g, PhongMaterial mat) {
        for (javafx.scene.Node n : g.getChildren()) {
            if (n instanceof Cylinder c) {
                c.setMaterial(mat);
            }
        }
    }

    private void updateSpin(boolean run) {
        if (loop != null) loop.stop();
        if (run && shouldRun()) {
            loop = new AnimationTimer() {
                private long last = 0;
                @Override
                public void handle(long now) {
                    if (last > 0) {
                        // Clamp dt so a long frame stall cannot jump the rings.
                        double dt = Math.min((now - last) / 1_000_000_000.0, 0.1);
                        rotOuter.setAngle((rotOuter.getAngle() + dt * 45.0) % 360.0);
                        rotMiddle.setAngle((rotMiddle.getAngle() + dt * 65.0) % 360.0);
                        rotInner.setAngle((rotInner.getAngle() + dt * 90.0) % 360.0);
                    }
                    last = now;
                }
            };
            loop.start();
        }
    }
}

