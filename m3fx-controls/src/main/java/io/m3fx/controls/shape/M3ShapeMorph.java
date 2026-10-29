package io.m3fx.controls.shape;

import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.internal.SpringAnimation.SpringParams;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * M3 Expressive shape morphing node capable of smoothly animating between expressive
 * geometric shapes (Pill, Rounded Rectangle, Clover, Scallop, Diamond, Circle).
 *
 * <p>See <a href="https://m3.material.io/styles/shape/overview">M3 shape spec</a>.
 * Morphing is driven by {@link SpringAnimation} physics and rendered via a high-performance Canvas.
 */
public class M3ShapeMorph extends Region {

    /** M3 Expressive shape presets. */
    public enum ShapeType {
        ROUNDED_RECTANGLE,
        PILL,
        CIRCLE,
        CLOVER,
        SCALLOP,
        DIAMOND
    }

    private final ObjectProperty<ShapeType> shapeType =
            new SimpleObjectProperty<>(this, "shapeType", ShapeType.ROUNDED_RECTANGLE);
    private final DoubleProperty morphProgress =
            new SimpleDoubleProperty(this, "morphProgress", 1.0);

    private ShapeType fromShape = ShapeType.ROUNDED_RECTANGLE;
    private ShapeType toShape = ShapeType.ROUNDED_RECTANGLE;
    private final Canvas canvas = new Canvas();

    /** Creates a shape morphing node with default rounded rectangle shape. */
    public M3ShapeMorph() {
        this(ShapeType.ROUNDED_RECTANGLE);
    }

    /**
     * Creates a shape morphing node with an initial shape.
     *
     * @param initialShape the initial shape
     */
    public M3ShapeMorph(ShapeType initialShape) {
        getStyleClass().add("m3-shape-morph");
        getChildren().add(canvas);
        setShapeType(initialShape);
        this.fromShape = initialShape;
        this.toShape = initialShape;

        morphProgressProperty().addListener((obs, oldV, newV) -> renderCanvas());
        shapeTypeProperty().addListener((obs, oldV, newV) -> {
            fromShape = oldV == null ? newV : oldV;
            toShape = newV;
            renderCanvas();
        });

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, o, n) -> renderCanvas());
        heightProperty().addListener((obs, o, n) -> renderCanvas());
        sceneProperty().addListener((obs, o, n) -> renderCanvas());

        setMinSize(48, 48);
        setPrefSize(120, 120);
    }

    /** Returns the shape type property. */
    public ObjectProperty<ShapeType> shapeTypeProperty() {
        return shapeType;
    }

    /** Returns current target shape type. */
    public ShapeType getShapeType() {
        return shapeType.get();
    }

    /** Sets target shape type. */
    public void setShapeType(ShapeType shapeType) {
        this.shapeType.set(shapeType);
    }

    /** Returns the morph progress property (0.0 = fromShape, 1.0 = toShape). */
    public DoubleProperty morphProgressProperty() {
        return morphProgress;
    }

    /** Returns morph progress. */
    public double getMorphProgress() {
        return morphProgress.get();
    }

    /**
     * Morphs smoothly to a new shape using spring physics.
     *
     * @param newShape target shape; {@code null} is ignored
     * @param params spring tuning
     */
    public void morphTo(ShapeType newShape, SpringParams params) {
        if (newShape == null) {
            return;
        }
        if (newShape == this.toShape && morphProgress.get() >= 1.0) {
            return;
        }
        this.fromShape = this.toShape;
        this.toShape = newShape;
        this.shapeType.set(newShape);
        this.morphProgress.set(0.0);
        SpringAnimation.animate(morphProgress, 1.0, params == null ? SpringAnimation.DEFAULT : params, null);
    }

    /**
     * Morphs smoothly to a new shape using default M3 Expressive bouncy spring.
     *
     * @param newShape target shape
     */
    public void morphTo(ShapeType newShape) {
        morphTo(newShape, SpringAnimation.BOUNCY);
    }

    private void renderCanvas() {
        double w = getWidth() > 0 ? getWidth() : getPrefWidth();
        double h = getHeight() > 0 ? getHeight() : getPrefHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        Color fill = ThemeColors.resolve(this, ColorRole.PRIMARY_CONTAINER);
        Color stroke = ThemeColors.resolve(this, ColorRole.PRIMARY);

        gc.setFill(fill);
        gc.setStroke(stroke);
        gc.setLineWidth(2.0);

        double t = Math.max(0.0, Math.min(1.0, getMorphProgress()));
        drawMorphedShape(gc, w, h, fromShape, toShape, t);
    }

    private static void drawMorphedShape(
            GraphicsContext gc, double w, double h, ShapeType from, ShapeType to, double t) {
        double padding = 3.0;
        double cx = w / 2.0;
        double cy = h / 2.0;
        double maxRadius = (Math.min(w, h) / 2.0) - padding;

        double cornerRadiusFrom = getCornerRadius(from, maxRadius);
        double cornerRadiusTo = getCornerRadius(to, maxRadius);
        double cr = cornerRadiusFrom + (cornerRadiusTo - cornerRadiusFrom) * t;

        double petalDepthFrom = getPetalDepth(from);
        double petalDepthTo = getPetalDepth(to);
        double pd = petalDepthFrom + (petalDepthTo - petalDepthFrom) * t;

        double diamondFrom = from == ShapeType.DIAMOND ? 1.0 : 0.0;
        double diamondTo = to == ShapeType.DIAMOND ? 1.0 : 0.0;
        double dw = diamondFrom + (diamondTo - diamondFrom) * t;

        // Blend the petal counts so a Clover(4) <-> Scallop(8) morph starts and ends on the
        // correct shape instead of rendering both endpoints with a single lobe count.
        int lobes = (int) Math.round(lobeCount(from) + (lobeCount(to) - lobeCount(from)) * t);

        if (pd > 0.02) {
            // Lobed / Scalloped / Clover shape
            gc.beginPath();
            double angleStep = (2 * Math.PI) / lobes;
            double rBase = maxRadius * (1.0 - pd * 0.35);
            double rCtrl = maxRadius * (1.0 + pd * 0.35);

            double x0 = cx + rBase * Math.cos(0);
            double y0 = cy + rBase * Math.sin(0);
            gc.moveTo(x0, y0);

            for (int i = 0; i < lobes; i++) {
                double a1 = i * angleStep;
                double aMid = a1 + angleStep * 0.5;
                double a2 = (i + 1) * angleStep;

                double cx1 = cx + rCtrl * Math.cos(aMid);
                double cy1 = cy + rCtrl * Math.sin(aMid);
                double xEnd = cx + rBase * Math.cos(a2);
                double yEnd = cy + rBase * Math.sin(a2);

                gc.bezierCurveTo(cx1, cy1, cx1, cy1, xEnd, yEnd);
            }
            gc.closePath();
            gc.fill();
            gc.stroke();
            return;
        }

        if (dw > 0.5) {
            // Diamond polygon
            gc.beginPath();
            gc.moveTo(cx, padding);
            gc.lineTo(w - padding, cy);
            gc.lineTo(cx, h - padding);
            gc.lineTo(padding, cy);
            gc.closePath();
            gc.fill();
            gc.stroke();
            return;
        }

        // Rounded Rect / Pill / Circle
        double rx = padding;
        double ry = padding;
        double rw = w - padding * 2;
        double rh = h - padding * 2;
        double arc = Math.min(cr * 2, Math.min(rw, rh));

        gc.fillRoundRect(rx, ry, rw, rh, arc, arc);
        gc.strokeRoundRect(rx, ry, rw, rh, arc, arc);
    }

    private static double getCornerRadius(ShapeType type, double maxRadius) {
        return switch (type) {
            case ROUNDED_RECTANGLE -> 16.0;
            case PILL -> maxRadius;
            case CIRCLE -> maxRadius;
            case DIAMOND -> 0.0;
            default -> 24.0;
        };
    }

    private static double getPetalDepth(ShapeType type) {
        return switch (type) {
            case CLOVER -> 0.45;
            case SCALLOP -> 0.30;
            default -> 0.0;
        };
    }

    private static int lobeCount(ShapeType type) {
        return type == ShapeType.SCALLOP ? 8 : 4;
    }
}
