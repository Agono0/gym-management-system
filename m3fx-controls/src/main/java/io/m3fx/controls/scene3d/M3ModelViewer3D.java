package io.m3fx.controls.scene3d;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/**
 * 3D Commercial Gym Equipment Inspector featuring procedural 3D models across 12 equipment
 * categories, rendered with M3 dynamic theme materials, wireframe mode, and orbit controls.
 */
public class M3ModelViewer3D extends M3Scene3D {

    /** Available 3D equipment models. */
    public enum ModelType {
        DUMBBELL,
        BARBELL,
        KETTLEBELL,
        WORKOUT_BENCH,
        SQUAT_RACK,
        CABLE_CROSSOVER,
        LEG_PRESS,
        TREADMILL,
        EXERCISE_BIKE,
        ROWING_MACHINE,
        PUNCHING_BAG,
        PREACHER_BENCH
    }

    private final ObjectProperty<ModelType> currentModel =
            new SimpleObjectProperty<>(this, "currentModel", ModelType.DUMBBELL);
    private final BooleanProperty autoRotate =
            new SimpleBooleanProperty(this, "autoRotate", true);
    private final BooleanProperty wireframe =
            new SimpleBooleanProperty(this, "wireframe", false);

    private final Group modelContainer = new Group();
    private final Rotate modelRotateY = new Rotate(0, Rotate.Y_AXIS);

    private AnimationTimer rotationLoop;

    /** Creates a 3D model viewer with default Dumbbell model. */
    public M3ModelViewer3D() {
        this(ModelType.DUMBBELL);
    }

    /**
     * Creates a 3D model viewer with a specified model.
     *
     * @param initialModel initial equipment model
     */
    public M3ModelViewer3D(ModelType initialModel) {
        super(440, 320);
        getStyleClass().add("m3-model-viewer-3d");

        modelContainer.getTransforms().add(modelRotateY);
        getWorldGroup().getChildren().add(modelContainer);

        currentModelProperty().addListener((obs, o, n) -> rebuildModel(n));
        wireframeProperty().addListener((obs, o, n) -> applyDrawMode());
        autoRotateProperty().addListener((obs, o, n) -> updateRotationLoop(n));

        sceneProperty().addListener((obs, o, n) -> {
            rebuildModel(getCurrentModel());
            updateRotationLoop(isAutoRotate());
        });
        visibleProperty().addListener(e -> updateRotationLoop(isAutoRotate()));
        disabledProperty().addListener(e -> updateRotationLoop(isAutoRotate()));

        setCurrentModel(initialModel);
        rebuildModel(initialModel);
        updateRotationLoop(isAutoRotate());
    }

    /** True when the auto-rotation should currently be stepping. */
    private boolean shouldRun() {
        return isAutoRotate() && getScene() != null && isVisible() && !isDisable();
    }

    public ObjectProperty<ModelType> currentModelProperty() {
        return currentModel;
    }

    public ModelType getCurrentModel() {
        return currentModel.get();
    }

    public void setCurrentModel(ModelType model) {
        this.currentModel.set(model);
    }

    public BooleanProperty autoRotateProperty() {
        return autoRotate;
    }

    public boolean isAutoRotate() {
        return autoRotate.get();
    }

    public void setAutoRotate(boolean rotate) {
        this.autoRotate.set(rotate);
    }

    public BooleanProperty wireframeProperty() {
        return wireframe;
    }

    public boolean isWireframe() {
        return wireframe.get();
    }

    public void setWireframe(boolean wireframe) {
        this.wireframe.set(wireframe);
    }

    private void updateRotationLoop(boolean running) {
        if (rotationLoop != null) {
            rotationLoop.stop();
        }
        if (running && shouldRun()) {
            rotationLoop = new AnimationTimer() {
                private long lastTime = 0;
                @Override
                public void handle(long now) {
                    if (lastTime > 0) {
                        // Clamp dt so a long frame stall cannot jump the model.
                        double dt = Math.min((now - lastTime) / 1_000_000_000.0, 0.1);
                        modelRotateY.setAngle((modelRotateY.getAngle() + dt * 32.0) % 360.0);
                    }
                    lastTime = now;
                }
            };
            rotationLoop.start();
        }
    }

    private void rebuildModel(ModelType type) {
        if (type == null) {
            return; // null model: keep the currently built model rather than NPE in the switch
        }
        modelContainer.getChildren().clear();

        Color primary = ThemeColors.resolve(this, ColorRole.PRIMARY);
        Color tertiary = ThemeColors.resolve(this, ColorRole.TERTIARY);
        Color metalSteel = Color.rgb(200, 210, 225);
        Color darkIron = Color.rgb(45, 48, 55);

        PhongMaterial matPrimary = new PhongMaterial(primary);
        matPrimary.setSpecularColor(Color.WHITE);
        matPrimary.setSpecularPower(32.0);

        PhongMaterial matSteel = new PhongMaterial(metalSteel);
        matSteel.setSpecularColor(Color.WHITE);
        matSteel.setSpecularPower(64.0);

        PhongMaterial matIron = new PhongMaterial(darkIron);
        matIron.setSpecularColor(Color.rgb(120, 120, 130));
        matIron.setSpecularPower(16.0);

        PhongMaterial matAccent = new PhongMaterial(tertiary);
        matAccent.setSpecularColor(Color.WHITE);
        matAccent.setSpecularPower(28.0);

        switch (type) {
            case DUMBBELL -> buildDumbbell(matSteel, matPrimary, matIron);
            case BARBELL -> buildBarbell(matSteel, matPrimary, matAccent);
            case KETTLEBELL -> buildKettlebell(matPrimary, matSteel);
            case WORKOUT_BENCH -> buildWorkoutBench(matSteel, matPrimary, matIron);
            case SQUAT_RACK -> buildSquatRack(matSteel, matPrimary, matIron, matAccent);
            case CABLE_CROSSOVER -> buildCableCrossover(matSteel, matPrimary, matIron, matAccent);
            case LEG_PRESS -> buildLegPress(matSteel, matPrimary, matIron, matAccent);
            case TREADMILL -> buildTreadmill(matSteel, matPrimary, matIron, matAccent);
            case EXERCISE_BIKE -> buildExerciseBike(matSteel, matPrimary, matIron, matAccent);
            case ROWING_MACHINE -> buildRowingMachine(matSteel, matPrimary, matIron, matAccent);
            case PUNCHING_BAG -> buildPunchingBag(matSteel, matPrimary, matIron, matAccent);
            case PREACHER_BENCH -> buildPreacherBench(matSteel, matPrimary, matIron, matAccent);
        }

        applyDrawMode();
    }

    private void buildDumbbell(PhongMaterial steel, PhongMaterial primary, PhongMaterial iron) {
        Cylinder handle = new Cylinder(6.0, 100.0);
        handle.setRotationAxis(Rotate.Z_AXIS);
        handle.setRotate(90.0);
        handle.setMaterial(steel);

        Cylinder leftOuter = new Cylinder(36.0, 22.0);
        leftOuter.setRotationAxis(Rotate.Z_AXIS);
        leftOuter.setRotate(90.0);
        leftOuter.setTranslateX(-60.0);
        leftOuter.setMaterial(primary);

        Cylinder leftInner = new Cylinder(32.0, 12.0);
        leftInner.setRotationAxis(Rotate.Z_AXIS);
        leftInner.setRotate(90.0);
        leftInner.setTranslateX(-44.0);
        leftInner.setMaterial(iron);

        Cylinder rightOuter = new Cylinder(36.0, 22.0);
        rightOuter.setRotationAxis(Rotate.Z_AXIS);
        rightOuter.setRotate(90.0);
        rightOuter.setTranslateX(60.0);
        rightOuter.setMaterial(primary);

        Cylinder rightInner = new Cylinder(32.0, 12.0);
        rightInner.setRotationAxis(Rotate.Z_AXIS);
        rightInner.setRotate(90.0);
        rightInner.setTranslateX(44.0);
        rightInner.setMaterial(iron);

        modelContainer.getChildren().addAll(handle, leftOuter, leftInner, rightOuter, rightInner);
    }

    private void buildBarbell(PhongMaterial steel, PhongMaterial primary, PhongMaterial accent) {
        Cylinder bar = new Cylinder(4.5, 240.0);
        bar.setRotationAxis(Rotate.Z_AXIS);
        bar.setRotate(90.0);
        bar.setMaterial(steel);

        Cylinder p20Left = new Cylinder(48.0, 12.0);
        p20Left.setRotationAxis(Rotate.Z_AXIS);
        p20Left.setRotate(90.0);
        p20Left.setTranslateX(-80.0);
        p20Left.setMaterial(primary);

        Cylinder p10Left = new Cylinder(40.0, 10.0);
        p10Left.setRotationAxis(Rotate.Z_AXIS);
        p10Left.setRotate(90.0);
        p10Left.setTranslateX(-92.0);
        p10Left.setMaterial(accent);

        Cylinder p5Left = new Cylinder(32.0, 8.0);
        p5Left.setRotationAxis(Rotate.Z_AXIS);
        p5Left.setRotate(90.0);
        p5Left.setTranslateX(-102.0);
        p5Left.setMaterial(primary);

        Cylinder p20Right = new Cylinder(48.0, 12.0);
        p20Right.setRotationAxis(Rotate.Z_AXIS);
        p20Right.setRotate(90.0);
        p20Right.setTranslateX(80.0);
        p20Right.setMaterial(primary);

        Cylinder p10Right = new Cylinder(40.0, 10.0);
        p10Right.setRotationAxis(Rotate.Z_AXIS);
        p10Right.setRotate(90.0);
        p10Right.setTranslateX(92.0);
        p10Right.setMaterial(accent);

        Cylinder p5Right = new Cylinder(32.0, 8.0);
        p5Right.setRotationAxis(Rotate.Z_AXIS);
        p5Right.setRotate(90.0);
        p5Right.setTranslateX(102.0);
        p5Right.setMaterial(primary);

        modelContainer.getChildren().addAll(bar, p20Left, p10Left, p5Left, p20Right, p10Right, p5Right);
    }

    private void buildKettlebell(PhongMaterial bodyMat, PhongMaterial handleMat) {
        Sphere ball = new Sphere(42.0);
        ball.setMaterial(bodyMat);

        Cylinder flatBase = new Cylinder(30.0, 6.0);
        flatBase.setTranslateY(38.0);
        flatBase.setMaterial(bodyMat);

        Cylinder handleTop = new Cylinder(5.0, 48.0);
        handleTop.setRotationAxis(Rotate.Z_AXIS);
        handleTop.setRotate(90.0);
        handleTop.setTranslateY(-58.0);
        handleTop.setMaterial(handleMat);

        Cylinder handleLeft = new Cylinder(5.0, 24.0);
        handleLeft.setTranslateX(-22.0);
        handleLeft.setTranslateY(-46.0);
        handleLeft.setMaterial(handleMat);

        Cylinder handleRight = new Cylinder(5.0, 24.0);
        handleRight.setTranslateX(22.0);
        handleRight.setTranslateY(-46.0);
        handleRight.setMaterial(handleMat);

        modelContainer.getChildren().addAll(ball, flatBase, handleTop, handleLeft, handleRight);
    }

    private void buildWorkoutBench(PhongMaterial frameMat, PhongMaterial cushionMat, PhongMaterial darkMat) {
        Box spine = new Box(16.0, 12.0, 160.0);
        spine.setTranslateY(30.0);
        spine.setMaterial(frameMat);

        Box frontLeg = new Box(12.0, 50.0, 12.0);
        frontLeg.setTranslateY(55.0);
        frontLeg.setTranslateZ(-70.0);
        frontLeg.setMaterial(frameMat);

        Box rearLeg = new Box(12.0, 50.0, 12.0);
        rearLeg.setTranslateY(55.0);
        rearLeg.setTranslateZ(70.0);
        rearLeg.setMaterial(frameMat);

        Box frontFoot = new Box(70.0, 8.0, 12.0);
        frontFoot.setTranslateY(78.0);
        frontFoot.setTranslateZ(-70.0);
        frontFoot.setMaterial(darkMat);

        Box rearFoot = new Box(70.0, 8.0, 12.0);
        rearFoot.setTranslateY(78.0);
        rearFoot.setTranslateZ(70.0);
        rearFoot.setMaterial(darkMat);

        Box backrest = new Box(48.0, 10.0, 100.0);
        backrest.setTranslateY(10.0);
        backrest.setTranslateZ(-15.0);
        backrest.setRotationAxis(Rotate.X_AXIS);
        backrest.setRotate(-18.0);
        backrest.setMaterial(cushionMat);

        Box seat = new Box(44.0, 10.0, 42.0);
        seat.setTranslateY(22.0);
        seat.setTranslateZ(52.0);
        seat.setMaterial(cushionMat);

        modelContainer.getChildren().addAll(spine, frontLeg, rearLeg, frontFoot, rearFoot, backrest, seat);
    }

    private void buildSquatRack(PhongMaterial steel, PhongMaterial primary, PhongMaterial iron, PhongMaterial accent) {
        // 4 Vertical Corner Posts
        Box postFL = new Box(8.0, 160.0, 8.0); postFL.setTranslateX(-48.0); postFL.setTranslateZ(42.0); postFL.setMaterial(primary);
        Box postFR = new Box(8.0, 160.0, 8.0); postFR.setTranslateX(48.0); postFR.setTranslateZ(42.0); postFR.setMaterial(primary);
        Box postRL = new Box(8.0, 160.0, 8.0); postRL.setTranslateX(-48.0); postRL.setTranslateZ(-42.0); postRL.setMaterial(primary);
        Box postRR = new Box(8.0, 160.0, 8.0); postRR.setTranslateX(48.0); postRR.setTranslateZ(-42.0); postRR.setMaterial(primary);

        // Floor Base Bars
        Box baseLeft = new Box(10.0, 8.0, 104.0); baseLeft.setTranslateX(-48.0); baseLeft.setTranslateY(76.0); baseLeft.setMaterial(iron);
        Box baseRight = new Box(10.0, 8.0, 104.0); baseRight.setTranslateX(48.0); baseRight.setTranslateY(76.0); baseRight.setMaterial(iron);
        Box baseRear = new Box(88.0, 8.0, 8.0); baseRear.setTranslateY(76.0); baseRear.setTranslateZ(-42.0); baseRear.setMaterial(iron);

        // Top Connecting Crossbars & Header
        Box topSideL = new Box(8.0, 8.0, 92.0); topSideL.setTranslateX(-48.0); topSideL.setTranslateY(-76.0); topSideL.setMaterial(primary);
        Box topSideR = new Box(8.0, 8.0, 92.0); topSideR.setTranslateX(48.0); topSideR.setTranslateY(-76.0); topSideR.setMaterial(primary);
        Box topRear = new Box(88.0, 8.0, 8.0); topRear.setTranslateY(-76.0); topRear.setTranslateZ(-42.0); topRear.setMaterial(primary);
        Box topFrontHeader = new Box(88.0, 10.0, 6.0); topFrontHeader.setTranslateY(-76.0); topFrontHeader.setTranslateZ(42.0); topFrontHeader.setMaterial(primary);

        // Multi-Grip Pull-Up Bar
        Cylinder pullUpBar = new Cylinder(3.0, 104.0);
        pullUpBar.setRotationAxis(Rotate.Z_AXIS); pullUpBar.setRotate(90.0);
        pullUpBar.setTranslateY(-70.0); pullUpBar.setTranslateZ(48.0); pullUpBar.setMaterial(steel);

        Cylinder gripAngleL = new Cylinder(2.5, 26.0);
        gripAngleL.setRotationAxis(Rotate.Z_AXIS); gripAngleL.setRotate(32.0);
        gripAngleL.setTranslateX(-36.0); gripAngleL.setTranslateY(-64.0); gripAngleL.setTranslateZ(48.0); gripAngleL.setMaterial(iron);

        Cylinder gripAngleR = new Cylinder(2.5, 26.0);
        gripAngleR.setRotationAxis(Rotate.Z_AXIS); gripAngleR.setRotate(-32.0);
        gripAngleR.setTranslateX(36.0); gripAngleR.setTranslateY(-64.0); gripAngleR.setTranslateZ(48.0); gripAngleR.setMaterial(iron);

        // Safety Spotter Arms
        Box spotterL = new Box(6.0, 8.0, 88.0); spotterL.setTranslateX(-48.0); spotterL.setTranslateY(22.0); spotterL.setMaterial(steel);
        Box spotterStripL = new Box(6.5, 2.0, 88.0); spotterStripL.setTranslateX(-48.0); spotterStripL.setTranslateY(17.0); spotterStripL.setMaterial(iron);
        Box spotterR = new Box(6.0, 8.0, 88.0); spotterR.setTranslateX(48.0); spotterR.setTranslateY(22.0); spotterR.setMaterial(steel);
        Box spotterStripR = new Box(6.5, 2.0, 88.0); spotterStripR.setTranslateX(48.0); spotterStripR.setTranslateY(17.0); spotterStripR.setMaterial(iron);

        // J-Hooks & Barbell
        Box hookL = new Box(10.0, 14.0, 10.0); hookL.setTranslateX(-48.0); hookL.setTranslateY(-8.0); hookL.setTranslateZ(38.0); hookL.setMaterial(iron);
        Box hookLipL = new Box(10.0, 8.0, 4.0); hookLipL.setTranslateX(-48.0); hookLipL.setTranslateY(-12.0); hookLipL.setTranslateZ(32.0); hookLipL.setMaterial(accent);
        Box hookR = new Box(10.0, 14.0, 10.0); hookR.setTranslateX(48.0); hookR.setTranslateY(-8.0); hookR.setTranslateZ(38.0); hookR.setMaterial(iron);
        Box hookLipR = new Box(10.0, 8.0, 4.0); hookLipR.setTranslateX(48.0); hookLipR.setTranslateY(-12.0); hookLipR.setTranslateZ(32.0); hookLipR.setMaterial(accent);

        Cylinder barShaft = new Cylinder(3.5, 170.0);
        barShaft.setRotationAxis(Rotate.Z_AXIS); barShaft.setRotate(90.0);
        barShaft.setTranslateY(-14.0); barShaft.setTranslateZ(35.0); barShaft.setMaterial(steel);

        Cylinder plateL20 = new Cylinder(22.0, 8.0); plateL20.setRotationAxis(Rotate.Z_AXIS); plateL20.setRotate(90.0); plateL20.setTranslateX(-61.0); plateL20.setTranslateY(-14.0); plateL20.setTranslateZ(35.0); plateL20.setMaterial(primary);
        Cylinder plateL10 = new Cylinder(17.0, 6.0); plateL10.setRotationAxis(Rotate.Z_AXIS); plateL10.setRotate(90.0); plateL10.setTranslateX(-69.0); plateL10.setTranslateY(-14.0); plateL10.setTranslateZ(35.0); plateL10.setMaterial(accent);
        Cylinder plateR20 = new Cylinder(22.0, 8.0); plateR20.setRotationAxis(Rotate.Z_AXIS); plateR20.setRotate(90.0); plateR20.setTranslateX(61.0); plateR20.setTranslateY(-14.0); plateR20.setTranslateZ(35.0); plateR20.setMaterial(primary);
        Cylinder plateR10 = new Cylinder(17.0, 6.0); plateR10.setRotationAxis(Rotate.Z_AXIS); plateR10.setRotate(90.0); plateR10.setTranslateX(69.0); plateR10.setTranslateY(-14.0); plateR10.setTranslateZ(35.0); plateR10.setMaterial(accent);

        modelContainer.getChildren().addAll(
                postFL, postFR, postRL, postRR, baseLeft, baseRight, baseRear,
                topSideL, topSideR, topRear, topFrontHeader, pullUpBar, gripAngleL, gripAngleR,
                spotterL, spotterStripL, spotterR, spotterStripR,
                hookL, hookLipL, hookR, hookLipR, barShaft, plateL20, plateL10, plateR20, plateR10
        );
    }

    private void buildCableCrossover(PhongMaterial steel, PhongMaterial primary, PhongMaterial iron, PhongMaterial accent) {
        // Left Tower
        Box baseL = new Box(38.0, 8.0, 36.0); baseL.setTranslateX(-85.0); baseL.setTranslateY(76.0); baseL.setMaterial(primary);
        Box topL = new Box(34.0, 8.0, 32.0); topL.setTranslateX(-85.0); topL.setTranslateY(-74.0); topL.setMaterial(primary);
        Box colOuterL = new Box(6.0, 150.0, 6.0); colOuterL.setTranslateX(-100.0); colOuterL.setTranslateY(1.0); colOuterL.setMaterial(primary);
        Box colInnerL = new Box(6.0, 150.0, 6.0); colInnerL.setTranslateX(-70.0); colInnerL.setTranslateY(1.0); colInnerL.setMaterial(primary);
        Box stackL = new Box(22.0, 85.0, 18.0); stackL.setTranslateX(-85.0); stackL.setTranslateY(26.0); stackL.setMaterial(iron);
        Box pinL = new Box(2.0, 85.0, 2.0); pinL.setTranslateX(-85.0); pinL.setTranslateY(26.0); pinL.setTranslateZ(10.0); pinL.setMaterial(accent);
        Cylinder guideRodL1 = new Cylinder(2.0, 142.0); guideRodL1.setTranslateX(-91.0); guideRodL1.setTranslateY(1.0); guideRodL1.setMaterial(steel);
        Cylinder guideRodL2 = new Cylinder(2.0, 142.0); guideRodL2.setTranslateX(-79.0); guideRodL2.setTranslateY(1.0); guideRodL2.setMaterial(steel);

        // Right Tower
        Box baseR = new Box(38.0, 8.0, 36.0); baseR.setTranslateX(85.0); baseR.setTranslateY(76.0); baseR.setMaterial(primary);
        Box topR = new Box(34.0, 8.0, 32.0); topR.setTranslateX(85.0); topR.setTranslateY(-74.0); topR.setMaterial(primary);
        Box colInnerR = new Box(6.0, 150.0, 6.0); colInnerR.setTranslateX(70.0); colInnerR.setTranslateY(1.0); colInnerR.setMaterial(primary);
        Box colOuterR = new Box(6.0, 150.0, 6.0); colOuterR.setTranslateX(100.0); colOuterR.setTranslateY(1.0); colOuterR.setMaterial(primary);
        Box stackR = new Box(22.0, 85.0, 18.0); stackR.setTranslateX(85.0); stackR.setTranslateY(26.0); stackR.setMaterial(iron);
        Box pinR = new Box(2.0, 85.0, 2.0); pinR.setTranslateX(85.0); pinR.setTranslateY(26.0); pinR.setTranslateZ(10.0); pinR.setMaterial(accent);
        Cylinder guideRodR1 = new Cylinder(2.0, 142.0); guideRodR1.setTranslateX(79.0); guideRodR1.setTranslateY(1.0); guideRodR1.setMaterial(steel);
        Cylinder guideRodR2 = new Cylinder(2.0, 142.0); guideRodR2.setTranslateX(91.0); guideRodR2.setTranslateY(1.0); guideRodR2.setMaterial(steel);

        // Top Bridge & Pull-up Bar
        Box mainBridge = new Box(146.0, 8.0, 8.0); mainBridge.setTranslateY(-74.0); mainBridge.setMaterial(primary);
        Box crownArch = new Box(84.0, 6.0, 6.0); crownArch.setTranslateY(-84.0); crownArch.setMaterial(iron);
        Box strutL = new Box(6.0, 22.0, 6.0); strutL.setRotationAxis(Rotate.Z_AXIS); strutL.setRotate(-42.0); strutL.setTranslateX(-46.0); strutL.setTranslateY(-79.0); strutL.setMaterial(primary);
        Box strutR = new Box(6.0, 22.0, 6.0); strutR.setRotationAxis(Rotate.Z_AXIS); strutR.setRotate(42.0); strutR.setTranslateX(46.0); strutR.setTranslateY(-79.0); strutR.setMaterial(primary);
        Cylinder centerPullUp = new Cylinder(3.0, 44.0); centerPullUp.setRotationAxis(Rotate.Z_AXIS); centerPullUp.setRotate(90.0); centerPullUp.setTranslateY(-77.0); centerPullUp.setTranslateZ(12.0); centerPullUp.setMaterial(steel);

        // Slider Carriages & Pulleys
        Cylinder sliderL = new Cylinder(3.5, 136.0); sliderL.setTranslateX(-68.0); sliderL.setTranslateZ(6.0); sliderL.setMaterial(steel);
        Box trolleyL = new Box(12.0, 14.0, 12.0); trolleyL.setTranslateX(-68.0); trolleyL.setTranslateY(-15.0); trolleyL.setTranslateZ(6.0); trolleyL.setMaterial(primary);
        Cylinder pulleyL = new Cylinder(6.0, 3.5); pulleyL.setRotationAxis(Rotate.X_AXIS); pulleyL.setRotate(90.0); pulleyL.setTranslateX(-64.0); pulleyL.setTranslateY(-15.0); pulleyL.setTranslateZ(12.0); pulleyL.setMaterial(iron);

        Cylinder sliderR = new Cylinder(3.5, 136.0); sliderR.setTranslateX(68.0); sliderR.setTranslateZ(6.0); sliderR.setMaterial(steel);
        Box trolleyR = new Box(12.0, 14.0, 12.0); trolleyR.setTranslateX(68.0); trolleyR.setTranslateY(-15.0); trolleyR.setTranslateZ(6.0); trolleyR.setMaterial(primary);
        Cylinder pulleyR = new Cylinder(6.0, 3.5); pulleyR.setRotationAxis(Rotate.X_AXIS); pulleyR.setRotate(90.0); pulleyR.setTranslateX(64.0); pulleyR.setTranslateY(-15.0); pulleyR.setTranslateZ(12.0); pulleyR.setMaterial(iron);

        // Cable Handles
        Cylinder cableL = new Cylinder(1.2, 28.0); cableL.setRotationAxis(Rotate.Z_AXIS); cableL.setRotate(18.0); cableL.setTranslateX(-58.0); cableL.setTranslateY(-1.0); cableL.setTranslateZ(14.0); cableL.setMaterial(steel);
        Cylinder gripL = new Cylinder(2.5, 12.0); gripL.setRotationAxis(Rotate.X_AXIS); gripL.setRotate(90.0); gripL.setTranslateX(-52.0); gripL.setTranslateY(16.0); gripL.setTranslateZ(15.0); gripL.setMaterial(iron);

        Cylinder cableR = new Cylinder(1.2, 28.0); cableR.setRotationAxis(Rotate.Z_AXIS); cableR.setRotate(-18.0); cableR.setTranslateX(58.0); cableR.setTranslateY(-1.0); cableR.setTranslateZ(14.0); cableR.setMaterial(steel);
        Cylinder gripR = new Cylinder(2.5, 12.0); gripR.setRotationAxis(Rotate.X_AXIS); gripR.setRotate(90.0); gripR.setTranslateX(52.0); gripR.setTranslateY(16.0); gripR.setTranslateZ(15.0); gripR.setMaterial(iron);

        modelContainer.getChildren().addAll(
                baseL, topL, colOuterL, colInnerL, stackL, pinL, guideRodL1, guideRodL2,
                baseR, topR, colInnerR, colOuterR, stackR, pinR, guideRodR1, guideRodR2,
                mainBridge, crownArch, strutL, strutR, centerPullUp,
                sliderL, trolleyL, pulleyL, sliderR, trolleyR, pulleyR,
                cableL, gripL, cableR, gripR
        );
    }

    private void buildLegPress(PhongMaterial steel, PhongMaterial primary, PhongMaterial iron, PhongMaterial accent) {
        // Floor Chassis
        Box baseRailL = new Box(8.0, 8.0, 144.0); baseRailL.setTranslateX(-30.0); baseRailL.setTranslateY(76.0); baseRailL.setTranslateZ(5.0); baseRailL.setMaterial(primary);
        Box baseRailR = new Box(8.0, 8.0, 144.0); baseRailR.setTranslateX(30.0); baseRailR.setTranslateY(76.0); baseRailR.setTranslateZ(5.0); baseRailR.setMaterial(primary);
        Box crossFront = new Box(68.0, 8.0, 8.0); crossFront.setTranslateY(76.0); crossFront.setTranslateZ(-65.0); crossFront.setMaterial(iron);
        Box crossRear = new Box(68.0, 8.0, 8.0); crossRear.setTranslateY(76.0); crossRear.setTranslateZ(75.0); crossRear.setMaterial(iron);

        // Front Upright Stanchions
        Box postFrontL = new Box(8.0, 110.0, 8.0); postFrontL.setTranslateX(-30.0); postFrontL.setTranslateY(23.0); postFrontL.setTranslateZ(-65.0); postFrontL.setMaterial(primary);
        Box postFrontR = new Box(8.0, 110.0, 8.0); postFrontR.setTranslateX(30.0); postFrontR.setTranslateY(23.0); postFrontR.setTranslateZ(-65.0); postFrontR.setMaterial(primary);
        Box headerFront = new Box(68.0, 10.0, 8.0); headerFront.setTranslateY(-32.0); headerFront.setTranslateZ(-65.0); headerFront.setMaterial(primary);

        // 45-Degree Incline Rails & Guide Rods
        Box railInclineL = new Box(8.0, 8.0, 155.0); railInclineL.setRotationAxis(Rotate.X_AXIS); railInclineL.setRotate(-45.0); railInclineL.setTranslateX(-30.0); railInclineL.setTranslateY(22.0); railInclineL.setTranslateZ(-10.0); railInclineL.setMaterial(primary);
        Box railInclineR = new Box(8.0, 8.0, 155.0); railInclineR.setRotationAxis(Rotate.X_AXIS); railInclineR.setRotate(-45.0); railInclineR.setTranslateX(30.0); railInclineR.setTranslateY(22.0); railInclineR.setTranslateZ(-10.0); railInclineR.setMaterial(primary);

        Cylinder guideRodL = new Cylinder(3.0, 148.0); guideRodL.setRotationAxis(Rotate.X_AXIS); guideRodL.setRotate(45.0); guideRodL.setTranslateX(-30.0); guideRodL.setTranslateY(16.0); guideRodL.setTranslateZ(-10.0); guideRodL.setMaterial(steel);
        Cylinder guideRodR = new Cylinder(3.0, 148.0); guideRodR.setRotationAxis(Rotate.X_AXIS); guideRodR.setRotate(45.0); guideRodR.setTranslateX(30.0); guideRodR.setTranslateY(16.0); guideRodR.setTranslateZ(-10.0); guideRodR.setMaterial(steel);

        // 45-Degree Footplate
        Box footplate = new Box(62.0, 48.0, 6.0); footplate.setRotationAxis(Rotate.X_AXIS); footplate.setRotate(-45.0); footplate.setTranslateY(-22.0); footplate.setTranslateZ(-54.0); footplate.setMaterial(iron);
        Box footplateBack = new Box(48.0, 38.0, 6.0); footplateBack.setRotationAxis(Rotate.X_AXIS); footplateBack.setRotate(-45.0); footplateBack.setTranslateY(-25.0); footplateBack.setTranslateZ(-57.0); footplateBack.setMaterial(primary);
        Box calfLedge = new Box(58.0, 4.0, 4.0); calfLedge.setRotationAxis(Rotate.X_AXIS); calfLedge.setRotate(-45.0); calfLedge.setTranslateY(-6.0); calfLedge.setTranslateZ(-38.0); calfLedge.setMaterial(accent);

        // Sliding Sled Carriage with Seat & Backrest
        Box sledFrame = new Box(58.0, 10.0, 48.0); sledFrame.setRotationAxis(Rotate.X_AXIS); sledFrame.setRotate(-45.0); sledFrame.setTranslateY(22.0); sledFrame.setTranslateZ(-2.0); sledFrame.setMaterial(primary);
        Box seatCushion = new Box(44.0, 8.0, 34.0); seatCushion.setRotationAxis(Rotate.X_AXIS); seatCushion.setRotate(-15.0); seatCushion.setTranslateY(36.0); seatCushion.setTranslateZ(18.0); seatCushion.setMaterial(iron);
        Box backrestCushion = new Box(42.0, 8.0, 58.0); backrestCushion.setRotationAxis(Rotate.X_AXIS); backrestCushion.setRotate(-55.0); backrestCushion.setTranslateY(14.0); backrestCushion.setTranslateZ(2.0); backrestCushion.setMaterial(primary);
        Box headrestCushion = new Box(24.0, 8.0, 18.0); headrestCushion.setRotationAxis(Rotate.X_AXIS); headrestCushion.setRotate(-55.0); headrestCushion.setTranslateY(-18.0); headrestCushion.setTranslateZ(-20.0); headrestCushion.setMaterial(iron);

        // Weight Horns & Loaded Plates
        Cylinder hornL = new Cylinder(4.0, 38.0); hornL.setRotationAxis(Rotate.Z_AXIS); hornL.setRotate(90.0); hornL.setTranslateX(-46.0); hornL.setTranslateY(14.0); hornL.setTranslateZ(-4.0); hornL.setMaterial(steel);
        Cylinder hornR = new Cylinder(4.0, 38.0); hornR.setRotationAxis(Rotate.Z_AXIS); hornR.setRotate(90.0); hornR.setTranslateX(46.0); hornR.setTranslateY(14.0); hornR.setTranslateZ(-4.0); hornR.setMaterial(steel);

        Cylinder plateL1 = new Cylinder(22.0, 8.0); plateL1.setRotationAxis(Rotate.Z_AXIS); plateL1.setRotate(90.0); plateL1.setTranslateX(-42.0); plateL1.setTranslateY(14.0); plateL1.setTranslateZ(-4.0); plateL1.setMaterial(primary);
        Cylinder plateL2 = new Cylinder(22.0, 8.0); plateL2.setRotationAxis(Rotate.Z_AXIS); plateL2.setRotate(90.0); plateL2.setTranslateX(-50.0); plateL2.setTranslateY(14.0); plateL2.setTranslateZ(-4.0); plateL2.setMaterial(accent);
        Cylinder plateR1 = new Cylinder(22.0, 8.0); plateR1.setRotationAxis(Rotate.Z_AXIS); plateR1.setRotate(90.0); plateR1.setTranslateX(42.0); plateR1.setTranslateY(14.0); plateR1.setTranslateZ(-4.0); plateR1.setMaterial(primary);
        Cylinder plateR2 = new Cylinder(22.0, 8.0); plateR2.setRotationAxis(Rotate.Z_AXIS); plateR2.setRotate(90.0); plateR2.setTranslateX(50.0); plateR2.setTranslateY(14.0); plateR2.setTranslateZ(-4.0); plateR2.setMaterial(accent);

        modelContainer.getChildren().addAll(
                baseRailL, baseRailR, crossFront, crossRear, postFrontL, postFrontR, headerFront,
                railInclineL, railInclineR, guideRodL, guideRodR, footplate, footplateBack, calfLedge,
                sledFrame, seatCushion, backrestCushion, headrestCushion,
                hornL, hornR, plateL1, plateL2, plateR1, plateR2
        );
    }

    private void buildTreadmill(PhongMaterial steel, PhongMaterial primary, PhongMaterial iron, PhongMaterial accent) {
        // Running Deck & Belt
        Box deckRailL = new Box(6.0, 12.0, 156.0); deckRailL.setTranslateX(-25.0); deckRailL.setTranslateY(68.0); deckRailL.setTranslateZ(5.0); deckRailL.setMaterial(primary);
        Box deckRailR = new Box(6.0, 12.0, 156.0); deckRailR.setTranslateX(25.0); deckRailR.setTranslateY(68.0); deckRailR.setTranslateZ(5.0); deckRailR.setMaterial(primary);
        Box belt = new Box(42.0, 8.0, 140.0); belt.setTranslateY(68.0); belt.setTranslateZ(5.0); belt.setMaterial(iron);
        Box footStripL = new Box(5.5, 2.0, 140.0); footStripL.setTranslateX(-25.0); footStripL.setTranslateY(61.0); footStripL.setTranslateZ(5.0); footStripL.setMaterial(iron);
        Box footStripR = new Box(5.5, 2.0, 140.0); footStripR.setTranslateX(25.0); footStripR.setTranslateY(61.0); footStripR.setTranslateZ(5.0); footStripR.setMaterial(iron);

        // Rollers & Motor Shroud
        Cylinder rollerFront = new Cylinder(7.0, 44.0); rollerFront.setRotationAxis(Rotate.Z_AXIS); rollerFront.setRotate(90.0); rollerFront.setTranslateY(68.0); rollerFront.setTranslateZ(-64.0); rollerFront.setMaterial(steel);
        Cylinder rollerRear = new Cylinder(6.0, 44.0); rollerRear.setRotationAxis(Rotate.Z_AXIS); rollerRear.setRotate(90.0); rollerRear.setTranslateY(68.0); rollerRear.setTranslateZ(73.0); rollerRear.setMaterial(steel);
        Box motorHood = new Box(48.0, 18.0, 26.0); motorHood.setTranslateY(58.0); motorHood.setTranslateZ(-66.0); motorHood.setMaterial(primary);
        Box motorGrille = new Box(36.0, 4.0, 16.0); motorGrille.setTranslateY(48.0); motorGrille.setTranslateZ(-66.0); motorGrille.setMaterial(iron);

        // Inclined Uprights
        Box uprightL = new Box(8.0, 104.0, 12.0); uprightL.setRotationAxis(Rotate.X_AXIS); uprightL.setRotate(12.0); uprightL.setTranslateX(-32.0); uprightL.setTranslateY(16.0); uprightL.setTranslateZ(-42.0); uprightL.setMaterial(primary);
        Box uprightR = new Box(8.0, 104.0, 12.0); uprightR.setRotationAxis(Rotate.X_AXIS); uprightR.setRotate(12.0); uprightR.setTranslateX(32.0); uprightR.setTranslateY(16.0); uprightR.setTranslateZ(-42.0); uprightR.setMaterial(primary);

        // Handrails & Front Pulse Bar
        Cylinder handrailL = new Cylinder(3.5, 64.0); handrailL.setRotationAxis(Rotate.X_AXIS); handrailL.setRotate(90.0); handrailL.setTranslateX(-32.0); handrailL.setTranslateY(-32.0); handrailL.setTranslateZ(-12.0); handrailL.setMaterial(iron);
        Cylinder handrailR = new Cylinder(3.5, 64.0); handrailR.setRotationAxis(Rotate.X_AXIS); handrailR.setRotate(90.0); handrailR.setTranslateX(32.0); handrailR.setTranslateY(-32.0); handrailR.setTranslateZ(-12.0); handrailR.setMaterial(iron);
        Cylinder pulseBar = new Cylinder(3.0, 58.0); pulseBar.setRotationAxis(Rotate.Z_AXIS); pulseBar.setRotate(90.0); pulseBar.setTranslateY(-32.0); pulseBar.setTranslateZ(-38.0); pulseBar.setMaterial(iron);
        Cylinder sensorL = new Cylinder(3.5, 10.0); sensorL.setRotationAxis(Rotate.Z_AXIS); sensorL.setRotate(90.0); sensorL.setTranslateX(-12.0); sensorL.setTranslateY(-32.0); sensorL.setTranslateZ(-38.0); sensorL.setMaterial(steel);
        Cylinder sensorR = new Cylinder(3.5, 10.0); sensorR.setRotationAxis(Rotate.Z_AXIS); sensorR.setRotate(90.0); sensorR.setTranslateX(12.0); sensorR.setTranslateY(-32.0); sensorR.setTranslateZ(-38.0); sensorR.setMaterial(steel);

        // Tilted Cyber Console Screen
        Box consoleBase = new Box(60.0, 10.0, 14.0); consoleBase.setTranslateY(-36.0); consoleBase.setTranslateZ(-44.0); consoleBase.setMaterial(primary);
        Box monitorHead = new Box(50.0, 32.0, 8.0); monitorHead.setRotationAxis(Rotate.X_AXIS); monitorHead.setRotate(-25.0); monitorHead.setTranslateY(-52.0); monitorHead.setTranslateZ(-47.0); monitorHead.setMaterial(iron);
        Box screenFace = new Box(44.0, 24.0, 2.0); screenFace.setRotationAxis(Rotate.X_AXIS); screenFace.setRotate(-25.0); screenFace.setTranslateY(-52.0); screenFace.setTranslateZ(-51.0); screenFace.setMaterial(accent);

        modelContainer.getChildren().addAll(
                deckRailL, deckRailR, belt, footStripL, footStripR,
                rollerFront, rollerRear, motorHood, motorGrille,
                uprightL, uprightR, handrailL, handrailR, pulseBar, sensorL, sensorR,
                consoleBase, monitorHead, screenFace
        );
    }

    private void buildExerciseBike(PhongMaterial steel, PhongMaterial primary, PhongMaterial iron, PhongMaterial accent) {
        // Stabilizer Feet & Chassis
        Box footFront = new Box(48.0, 8.0, 12.0); footFront.setTranslateY(76.0); footFront.setTranslateZ(-38.0); footFront.setMaterial(iron);
        Box footRear = new Box(50.0, 8.0, 12.0); footRear.setTranslateY(76.0); footRear.setTranslateZ(46.0); footRear.setMaterial(iron);
        Box frameSpine = new Box(8.0, 10.0, 80.0); frameSpine.setTranslateY(72.0); frameSpine.setTranslateZ(4.0); frameSpine.setMaterial(primary);

        // Flywheel & Caliper
        Cylinder flywheelRim = new Cylinder(26.0, 8.0); flywheelRim.setRotationAxis(Rotate.Z_AXIS); flywheelRim.setRotate(90.0); flywheelRim.setTranslateY(38.0); flywheelRim.setTranslateZ(-32.0); flywheelRim.setMaterial(steel);
        Cylinder flywheelHub = new Cylinder(18.0, 10.0); flywheelHub.setRotationAxis(Rotate.Z_AXIS); flywheelHub.setRotate(90.0); flywheelHub.setTranslateY(38.0); flywheelHub.setTranslateZ(-32.0); flywheelHub.setMaterial(accent);
        Box caliper = new Box(12.0, 10.0, 14.0); caliper.setTranslateY(16.0); caliper.setTranslateZ(-32.0); caliper.setMaterial(iron);
        Cylinder tensionKnob = new Cylinder(4.0, 8.0); tensionKnob.setTranslateY(6.0); tensionKnob.setTranslateZ(-26.0); tensionKnob.setMaterial(accent);

        // Angled Frame Tubes
        Cylinder bbHub = new Cylinder(8.0, 22.0); bbHub.setRotationAxis(Rotate.Z_AXIS); bbHub.setRotate(90.0); bbHub.setTranslateY(52.0); bbHub.setTranslateZ(6.0); bbHub.setMaterial(iron);
        Cylinder downTube = new Cylinder(4.5, 54.0); downTube.setRotationAxis(Rotate.X_AXIS); downTube.setRotate(-38.0); downTube.setTranslateY(46.0); downTube.setTranslateZ(-14.0); downTube.setMaterial(primary);
        Cylinder seatTube = new Cylinder(4.5, 64.0); seatTube.setRotationAxis(Rotate.X_AXIS); seatTube.setRotate(20.0); seatTube.setTranslateY(24.0); seatTube.setTranslateZ(14.0); seatTube.setMaterial(primary);
        Cylinder seatPost = new Cylinder(3.8, 32.0); seatPost.setRotationAxis(Rotate.X_AXIS); seatPost.setRotate(20.0); seatPost.setTranslateY(-4.0); seatPost.setTranslateZ(20.0); seatPost.setMaterial(steel);
        Cylinder headTube = new Cylinder(4.5, 68.0); headTube.setRotationAxis(Rotate.X_AXIS); headTube.setRotate(-18.0); headTube.setTranslateY(8.0); headTube.setTranslateZ(-28.0); headTube.setMaterial(primary);
        Cylinder handleStem = new Cylinder(3.8, 32.0); handleStem.setRotationAxis(Rotate.X_AXIS); handleStem.setRotate(-18.0); handleStem.setTranslateY(-18.0); handleStem.setTranslateZ(-34.0); handleStem.setMaterial(steel);

        // Saddle
        Box saddleRear = new Box(20.0, 6.0, 18.0); saddleRear.setTranslateY(-18.0); saddleRear.setTranslateZ(24.0); saddleRear.setMaterial(iron);
        Box saddleNose = new Box(10.0, 5.0, 16.0); saddleNose.setTranslateY(-18.0); saddleNose.setTranslateZ(12.0); saddleNose.setMaterial(iron);

        // Pedals & Cranks
        Box crankL = new Box(4.0, 22.0, 6.0); crankL.setTranslateX(-12.0); crankL.setTranslateY(58.0); crankL.setTranslateZ(6.0); crankL.setMaterial(steel);
        Box pedalL = new Box(12.0, 4.0, 14.0); pedalL.setTranslateX(-21.0); pedalL.setTranslateY(67.0); pedalL.setTranslateZ(6.0); pedalL.setMaterial(iron);
        Box crankR = new Box(4.0, 22.0, 6.0); crankR.setTranslateX(12.0); crankR.setTranslateY(46.0); crankR.setTranslateZ(6.0); crankR.setMaterial(steel);
        Box pedalR = new Box(12.0, 4.0, 14.0); pedalR.setTranslateX(21.0); pedalR.setTranslateY(37.0); pedalR.setTranslateZ(6.0); pedalR.setMaterial(iron);

        // Handlebars & Screen
        Cylinder barClamp = new Cylinder(3.0, 26.0); barClamp.setRotationAxis(Rotate.Z_AXIS); barClamp.setRotate(90.0); barClamp.setTranslateY(-28.0); barClamp.setTranslateZ(-36.0); barClamp.setMaterial(steel);
        Cylinder hornL = new Cylinder(3.0, 22.0); hornL.setRotationAxis(Rotate.X_AXIS); hornL.setRotate(90.0); hornL.setTranslateX(-13.0); hornL.setTranslateY(-28.0); hornL.setTranslateZ(-45.0); hornL.setMaterial(iron);
        Cylinder hornR = new Cylinder(3.0, 22.0); hornR.setRotationAxis(Rotate.X_AXIS); hornR.setRotate(90.0); hornR.setTranslateX(13.0); hornR.setTranslateY(-28.0); hornR.setTranslateZ(-45.0); hornR.setMaterial(iron);
        Box screenBox = new Box(20.0, 14.0, 4.0); screenBox.setRotationAxis(Rotate.X_AXIS); screenBox.setRotate(-30.0); screenBox.setTranslateY(-39.0); screenBox.setTranslateZ(-41.0); screenBox.setMaterial(iron);
        Box screenGlow = new Box(16.0, 10.0, 1.5); screenGlow.setRotationAxis(Rotate.X_AXIS); screenGlow.setRotate(-30.0); screenGlow.setTranslateY(-39.0); screenGlow.setTranslateZ(-43.0); screenGlow.setMaterial(accent);

        modelContainer.getChildren().addAll(
                footFront, footRear, frameSpine, flywheelRim, flywheelHub, caliper, tensionKnob,
                bbHub, downTube, seatTube, seatPost, headTube, handleStem,
                saddleRear, saddleNose, crankL, pedalL, crankR, pedalR,
                barClamp, hornL, hornR, screenBox, screenGlow
        );
    }

    private void buildRowingMachine(PhongMaterial steel, PhongMaterial primary, PhongMaterial iron, PhongMaterial accent) {
        // Monorail & Supports
        Box monorail = new Box(12.0, 10.0, 155.0); monorail.setTranslateY(58.0); monorail.setTranslateZ(14.0); monorail.setMaterial(steel);
        Box railTrack = new Box(10.0, 2.0, 152.0); railTrack.setTranslateY(52.0); railTrack.setTranslateZ(14.0); railTrack.setMaterial(steel);
        Box rearFoot = new Box(36.0, 8.0, 10.0); rearFoot.setTranslateY(76.0); rearFoot.setTranslateZ(88.0); rearFoot.setMaterial(iron);
        Box rearPost = new Box(8.0, 20.0, 8.0); rearPost.setTranslateY(66.0); rearPost.setTranslateZ(88.0); rearPost.setMaterial(primary);

        Box frontLegL = new Box(6.0, 30.0, 8.0); frontLegL.setRotationAxis(Rotate.Z_AXIS); frontLegL.setRotate(-20.0); frontLegL.setTranslateX(-16.0); frontLegL.setTranslateY(66.0); frontLegL.setTranslateZ(-52.0); frontLegL.setMaterial(primary);
        Box frontLegR = new Box(6.0, 30.0, 8.0); frontLegR.setRotationAxis(Rotate.Z_AXIS); frontLegR.setRotate(20.0); frontLegR.setTranslateX(16.0); frontLegR.setTranslateY(66.0); frontLegR.setTranslateZ(-52.0); frontLegR.setMaterial(primary);
        Box frontFoot = new Box(44.0, 6.0, 8.0); frontFoot.setTranslateY(77.0); frontFoot.setTranslateZ(-52.0); frontFoot.setMaterial(iron);
        Box frameNeck = new Box(10.0, 12.0, 28.0); frameNeck.setRotationAxis(Rotate.X_AXIS); frameNeck.setRotate(16.0); frameNeck.setTranslateY(50.0); frameNeck.setTranslateZ(-54.0); frameNeck.setMaterial(primary);

        // Circular Flywheel Housing & Damper
        Cylinder flywheelCage = new Cylinder(24.0, 14.0); flywheelCage.setRotationAxis(Rotate.Z_AXIS); flywheelCage.setRotate(90.0); flywheelCage.setTranslateY(36.0); flywheelCage.setTranslateZ(-72.0); flywheelCage.setMaterial(iron);
        Cylinder meshBand = new Cylinder(24.5, 10.0); meshBand.setRotationAxis(Rotate.Z_AXIS); meshBand.setRotate(90.0); meshBand.setTranslateY(36.0); meshBand.setTranslateZ(-72.0); meshBand.setMaterial(steel);

        // Footplates
        Box footBracket = new Box(12.0, 14.0, 16.0); footBracket.setTranslateY(48.0); footBracket.setTranslateZ(-34.0); footBracket.setMaterial(primary);
        Box footplateL = new Box(13.0, 4.0, 30.0); footplateL.setRotationAxis(Rotate.X_AXIS); footplateL.setRotate(38.0); footplateL.setTranslateX(-15.0); footplateL.setTranslateY(44.0); footplateL.setTranslateZ(-34.0); footplateL.setMaterial(iron);
        Box footplateR = new Box(13.0, 4.0, 30.0); footplateR.setRotationAxis(Rotate.X_AXIS); footplateR.setRotate(38.0); footplateR.setTranslateX(15.0); footplateR.setTranslateY(44.0); footplateR.setTranslateZ(-34.0); footplateR.setMaterial(iron);

        // Sliding Seat
        Box seatPad = new Box(30.0, 6.0, 24.0); seatPad.setTranslateY(45.0); seatPad.setTranslateZ(28.0); seatPad.setMaterial(iron);
        Box seatCarriage = new Box(22.0, 6.0, 18.0); seatCarriage.setTranslateY(49.0); seatCarriage.setTranslateZ(28.0); seatCarriage.setMaterial(primary);

        // Handle & Monitor
        Cylinder handle = new Cylinder(3.0, 36.0); handle.setRotationAxis(Rotate.Z_AXIS); handle.setRotate(90.0); handle.setTranslateY(36.0); handle.setTranslateZ(-32.0); handle.setMaterial(iron);
        Cylinder monitorArm = new Cylinder(2.5, 22.0); monitorArm.setRotationAxis(Rotate.X_AXIS); monitorArm.setRotate(-25.0); monitorArm.setTranslateY(18.0); monitorArm.setTranslateZ(-58.0); monitorArm.setMaterial(steel);
        Box monitorConsole = new Box(18.0, 14.0, 4.0); monitorConsole.setRotationAxis(Rotate.X_AXIS); monitorConsole.setRotate(-20.0); monitorConsole.setTranslateY(8.0); monitorConsole.setTranslateZ(-63.0); monitorConsole.setMaterial(iron);
        Box monitorScreen = new Box(14.0, 10.0, 1.5); monitorScreen.setRotationAxis(Rotate.X_AXIS); monitorScreen.setRotate(-20.0); monitorScreen.setTranslateY(8.0); monitorScreen.setTranslateZ(-65.0); monitorScreen.setMaterial(accent);

        modelContainer.getChildren().addAll(
                monorail, railTrack, rearFoot, rearPost, frontLegL, frontLegR, frontFoot, frameNeck,
                flywheelCage, meshBand, footBracket, footplateL, footplateR, seatPad, seatCarriage,
                handle, monitorArm, monitorConsole, monitorScreen
        );
    }

    private void buildPunchingBag(PhongMaterial steel, PhongMaterial primary, PhongMaterial iron, PhongMaterial accent) {
        // Ceiling Mount Plate & Swivel
        Box mountPlate = new Box(34.0, 6.0, 34.0); mountPlate.setTranslateY(-75.0); mountPlate.setMaterial(iron);
        Box clevis = new Box(12.0, 10.0, 12.0); clevis.setTranslateY(-68.0); clevis.setMaterial(primary);
        Cylinder eyeBolt = new Cylinder(5.0, 8.0); eyeBolt.setTranslateY(-60.0); eyeBolt.setMaterial(steel);
        Cylinder masterRing = new Cylinder(6.0, 3.0); masterRing.setRotationAxis(Rotate.X_AXIS); masterRing.setRotate(90.0); masterRing.setTranslateY(-54.0); masterRing.setMaterial(steel);

        // 4 Suspension Chains
        Cylinder chainN = new Cylinder(1.8, 26.0); chainN.setRotationAxis(Rotate.X_AXIS); chainN.setRotate(-20.0); chainN.setTranslateY(-42.0); chainN.setTranslateZ(-7.0); chainN.setMaterial(steel);
        Cylinder chainS = new Cylinder(1.8, 26.0); chainS.setRotationAxis(Rotate.X_AXIS); chainS.setRotate(20.0); chainS.setTranslateY(-42.0); chainS.setTranslateZ(7.0); chainS.setMaterial(steel);
        Cylinder chainW = new Cylinder(1.8, 26.0); chainW.setRotationAxis(Rotate.Z_AXIS); chainW.setRotate(20.0); chainW.setTranslateX(-7.0); chainW.setTranslateY(-42.0); chainW.setMaterial(steel);
        Cylinder chainE = new Cylinder(1.8, 26.0); chainE.setRotationAxis(Rotate.Z_AXIS); chainE.setRotate(-20.0); chainE.setTranslateX(7.0); chainE.setTranslateY(-42.0); chainE.setMaterial(steel);

        // Cylindrical Heavy Bag with Top and Bottom Domes
        Cylinder bagBody = new Cylinder(22.0, 64.0); bagBody.setTranslateY(4.0); bagBody.setMaterial(primary);
        Sphere topDome = new Sphere(22.0); topDome.setTranslateY(-28.0); topDome.setMaterial(primary);
        Sphere bottomDome = new Sphere(22.0); bottomDome.setTranslateY(36.0); bottomDome.setMaterial(primary);

        // Contrast Strike Zone Band & Accent Logo
        Cylinder strikeBand = new Cylinder(22.4, 22.0); strikeBand.setTranslateY(2.0); strikeBand.setMaterial(iron);
        Box logoBadge = new Box(14.0, 10.0, 1.0); logoBadge.setTranslateY(2.0); logoBadge.setTranslateZ(22.8); logoBadge.setMaterial(accent);

        // Bottom Tie-down Ring
        Cylinder anchorRing = new Cylinder(3.5, 2.0); anchorRing.setRotationAxis(Rotate.X_AXIS); anchorRing.setRotate(90.0); anchorRing.setTranslateY(58.0); anchorRing.setMaterial(steel);

        modelContainer.getChildren().addAll(
                mountPlate, clevis, eyeBolt, masterRing,
                chainN, chainS, chainW, chainE,
                bagBody, topDome, bottomDome, strikeBand, logoBadge, anchorRing
        );
    }

    private void buildPreacherBench(PhongMaterial steel, PhongMaterial primary, PhongMaterial iron, PhongMaterial accent) {
        // Base Chassis
        Box spine = new Box(12.0, 12.0, 86.0); spine.setTranslateY(68.0); spine.setMaterial(primary);
        Box footFront = new Box(60.0, 8.0, 12.0); footFront.setTranslateY(76.0); footFront.setTranslateZ(-38.0); footFront.setMaterial(iron);
        Box footRear = new Box(48.0, 8.0, 12.0); footRear.setTranslateY(76.0); footRear.setTranslateZ(42.0); footRear.setMaterial(iron);

        // Front Upright & Diagonal Strut
        Box preacherPost = new Box(10.0, 66.0, 10.0); preacherPost.setTranslateY(32.0); preacherPost.setTranslateZ(-18.0); preacherPost.setMaterial(primary);
        Box diagStrut = new Box(8.0, 46.0, 8.0); diagStrut.setRotationAxis(Rotate.X_AXIS); diagStrut.setRotate(35.0); diagStrut.setTranslateY(48.0); diagStrut.setTranslateZ(-4.0); diagStrut.setMaterial(primary);

        // Rear Seat Post
        Box seatPost = new Box(10.0, 36.0, 10.0); seatPost.setTranslateY(50.0); seatPost.setTranslateZ(28.0); seatPost.setMaterial(primary);
        Box seatChrome = new Box(8.0, 28.0, 8.0); seatChrome.setTranslateY(36.0); seatChrome.setTranslateZ(28.0); seatChrome.setMaterial(steel);

        // Preacher Arm Pad (Angled 38°)
        Box armPad = new Box(54.0, 8.0, 36.0); armPad.setRotationAxis(Rotate.X_AXIS); armPad.setRotate(38.0); armPad.setTranslateZ(-18.0); armPad.setMaterial(primary);
        Box armPadBevel = new Box(54.0, 8.0, 12.0); armPadBevel.setRotationAxis(Rotate.X_AXIS); armPadBevel.setRotate(-20.0); armPadBevel.setTranslateY(-6.0); armPadBevel.setTranslateZ(-5.0); armPadBevel.setMaterial(iron);

        // Seat Cushion
        Box seatCushion = new Box(30.0, 8.0, 26.0); seatCushion.setTranslateY(22.0); seatCushion.setTranslateZ(28.0); seatCushion.setMaterial(iron);

        // Bar Cradle & Resting EZ-Bar
        Box cradleBar = new Box(56.0, 8.0, 8.0); cradleBar.setTranslateY(18.0); cradleBar.setTranslateZ(-30.0); cradleBar.setMaterial(primary);
        Box hookL = new Box(6.0, 14.0, 6.0); hookL.setTranslateX(-24.0); hookL.setTranslateY(14.0); hookL.setTranslateZ(-42.0); hookL.setMaterial(steel);
        Box hookR = new Box(6.0, 14.0, 6.0); hookR.setTranslateX(24.0); hookR.setTranslateY(14.0); hookR.setTranslateZ(-42.0); hookR.setMaterial(steel);

        Cylinder ezBar = new Cylinder(3.0, 84.0); ezBar.setRotationAxis(Rotate.Z_AXIS); ezBar.setRotate(90.0); ezBar.setTranslateY(10.0); ezBar.setTranslateZ(-40.0); ezBar.setMaterial(steel);
        Cylinder plateL = new Cylinder(14.0, 6.0); plateL.setRotationAxis(Rotate.Z_AXIS); plateL.setRotate(90.0); plateL.setTranslateX(-37.0); plateL.setTranslateY(10.0); plateL.setTranslateZ(-40.0); plateL.setMaterial(accent);
        Cylinder plateR = new Cylinder(14.0, 6.0); plateR.setRotationAxis(Rotate.Z_AXIS); plateR.setRotate(90.0); plateR.setTranslateX(37.0); plateR.setTranslateY(10.0); plateR.setTranslateZ(-40.0); plateR.setMaterial(accent);

        modelContainer.getChildren().addAll(
                spine, footFront, footRear, preacherPost, diagStrut, seatPost, seatChrome,
                armPad, armPadBevel, seatCushion, cradleBar, hookL, hookR, ezBar, plateL, plateR
        );
    }

    private void applyDrawMode() {
        DrawMode mode = isWireframe() ? DrawMode.LINE : DrawMode.FILL;
        for (Node n : modelContainer.getChildren()) {
            if (n instanceof javafx.scene.shape.Shape3D s) {
                s.setDrawMode(mode);
            }
        }
    }
}
