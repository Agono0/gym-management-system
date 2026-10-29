package io.m3fx.controls.scene3d;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

import java.util.EnumMap;
import java.util.Map;

/**
 * Interactive 3D Human Anatomy Visualizer for targeted workout routines.
 * Selecting muscle groups highlights anatomical segments with glowing M3 specular materials.
 */
public class M3AnatomyViewer3D extends M3Scene3D {

    /** Major workout target muscle groups. */
    public enum MuscleGroup {
        CHEST,
        BACK,
        SHOULDERS,
        ARMS,
        CORE,
        LEGS
    }

    private final ObjectProperty<MuscleGroup> activeMuscle =
            new SimpleObjectProperty<>(this, "activeMuscle", MuscleGroup.CHEST);

    private final Group bodyGroup = new Group();
    private final Map<MuscleGroup, Group> muscleNodes = new EnumMap<>(MuscleGroup.class);

    private PhongMaterial activeMat;
    private PhongMaterial idleMat;
    private PhongMaterial boneMat;

    /** Creates a 3D anatomy viewer. */
    public M3AnatomyViewer3D() {
        super(440, 340);
        getStyleClass().add("m3-anatomy-viewer-3d");

        getWorldGroup().getChildren().add(bodyGroup);

        initMaterials();
        buildAnatomyModel();

        activeMuscleProperty().addListener((obs, o, n) -> updateHighlight(n));
        sceneProperty().addListener((obs, o, n) -> {
            initMaterials();
            updateHighlight(getActiveMuscle());
        });

        updateHighlight(getActiveMuscle());
    }

    public ObjectProperty<MuscleGroup> activeMuscleProperty() {
        return activeMuscle;
    }

    public MuscleGroup getActiveMuscle() {
        return activeMuscle.get();
    }

    public void setActiveMuscle(MuscleGroup muscle) {
        this.activeMuscle.set(muscle);
    }

    private void initMaterials() {
        Color primary = ThemeColors.resolve(this, ColorRole.PRIMARY);
        activeMat = new PhongMaterial(primary);
        activeMat.setSpecularColor(Color.WHITE);
        activeMat.setSpecularPower(32.0);

        // Idle body adapts to the theme: dark against light surfaces and vice versa.
        Color idle = ThemeColors.resolve(this, ColorRole.INVERSE_SURFACE);
        Color idleSpecular = ThemeColors.resolve(this, ColorRole.INVERSE_ON_SURFACE);
        idleMat = new PhongMaterial(Color.color(idle.getRed(), idle.getGreen(), idle.getBlue(), 0.7));
        idleMat.setSpecularColor(idleSpecular);
        idleMat.setSpecularPower(16.0);

        // Structural skeleton tone: neutral mid outline, legible in both themes.
        boneMat = new PhongMaterial(ThemeColors.resolve(this, ColorRole.OUTLINE));
    }

    private void buildAnatomyModel() {
        bodyGroup.getChildren().clear();
        muscleNodes.clear();

        // Head & Neck
        Sphere head = new Sphere(16.0);
        head.setTranslateY(-110.0);
        head.setMaterial(boneMat);

        Cylinder neck = new Cylinder(6.0, 14.0);
        neck.setTranslateY(-94.0);
        neck.setMaterial(boneMat);

        // CHEST Group
        Group chestGroup = new Group();
        Box pecLeft = new Box(18.0, 16.0, 10.0);
        pecLeft.setTranslateX(-11.0);
        pecLeft.setTranslateY(-74.0);
        pecLeft.setTranslateZ(-6.0);

        Box pecRight = new Box(18.0, 16.0, 10.0);
        pecRight.setTranslateX(11.0);
        pecRight.setTranslateY(-74.0);
        pecRight.setTranslateZ(-6.0);

        chestGroup.getChildren().addAll(pecLeft, pecRight);
        muscleNodes.put(MuscleGroup.CHEST, chestGroup);

        // SHOULDERS Group
        Group shoulderGroup = new Group();
        Sphere deltLeft = new Sphere(12.0);
        deltLeft.setTranslateX(-29.0);
        deltLeft.setTranslateY(-80.0);

        Sphere deltRight = new Sphere(12.0);
        deltRight.setTranslateX(29.0);
        deltRight.setTranslateY(-80.0);

        shoulderGroup.getChildren().addAll(deltLeft, deltRight);
        muscleNodes.put(MuscleGroup.SHOULDERS, shoulderGroup);

        // ARMS Group
        Group armGroup = new Group();
        Cylinder bicepLeft = new Cylinder(8.0, 28.0);
        bicepLeft.setTranslateX(-30.0);
        bicepLeft.setTranslateY(-56.0);

        Cylinder bicepRight = new Cylinder(8.0, 28.0);
        bicepRight.setTranslateX(30.0);
        bicepRight.setTranslateY(-56.0);

        Cylinder forearmLeft = new Cylinder(6.5, 26.0);
        forearmLeft.setTranslateX(-30.0);
        forearmLeft.setTranslateY(-26.0);

        Cylinder forearmRight = new Cylinder(6.5, 26.0);
        forearmRight.setTranslateX(30.0);
        forearmRight.setTranslateY(-26.0);

        armGroup.getChildren().addAll(bicepLeft, bicepRight, forearmLeft, forearmRight);
        muscleNodes.put(MuscleGroup.ARMS, armGroup);

        // BACK Group
        Group backGroup = new Group();
        Box lats = new Box(36.0, 30.0, 8.0);
        lats.setTranslateY(-72.0);
        lats.setTranslateZ(6.0);
        backGroup.getChildren().add(lats);
        muscleNodes.put(MuscleGroup.BACK, backGroup);

        // CORE / ABS Group
        Group coreGroup = new Group();
        Box absUpper = new Box(22.0, 14.0, 10.0);
        absUpper.setTranslateY(-56.0);
        absUpper.setTranslateZ(-4.0);

        Box absLower = new Box(20.0, 14.0, 10.0);
        absLower.setTranslateY(-40.0);
        absLower.setTranslateZ(-4.0);

        coreGroup.getChildren().addAll(absUpper, absLower);
        muscleNodes.put(MuscleGroup.CORE, coreGroup);

        // LEGS Group
        Group legGroup = new Group();
        Cylinder quadLeft = new Cylinder(11.0, 42.0);
        quadLeft.setTranslateX(-12.0);
        quadLeft.setTranslateY(-6.0);

        Cylinder quadRight = new Cylinder(11.0, 42.0);
        quadRight.setTranslateX(12.0);
        quadRight.setTranslateY(-6.0);

        Cylinder calfLeft = new Cylinder(9.0, 42.0);
        calfLeft.setTranslateX(-12.0);
        calfLeft.setTranslateY(38.0);

        Cylinder calfRight = new Cylinder(9.0, 42.0);
        calfRight.setTranslateX(12.0);
        calfRight.setTranslateY(38.0);

        legGroup.getChildren().addAll(quadLeft, quadRight, calfLeft, calfRight);
        muscleNodes.put(MuscleGroup.LEGS, legGroup);

        bodyGroup.getChildren().addAll(head, neck, chestGroup, shoulderGroup, armGroup, backGroup, coreGroup, legGroup);

        // Allow clicking individual muscle groups
        for (Map.Entry<MuscleGroup, Group> entry : muscleNodes.entrySet()) {
            entry.getValue().setOnMouseClicked(e -> {
                setActiveMuscle(entry.getKey());
                e.consume();
            });
        }
    }

    private void updateHighlight(MuscleGroup selected) {
        for (Map.Entry<MuscleGroup, Group> entry : muscleNodes.entrySet()) {
            boolean isActive = entry.getKey() == selected;
            PhongMaterial mat = isActive ? activeMat : idleMat;
            for (Node n : entry.getValue().getChildren()) {
                if (n instanceof javafx.scene.shape.Shape3D s) {
                    s.setMaterial(mat);
                }
            }
        }
    }
}

