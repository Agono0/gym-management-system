package io.m3fx.controls.progress;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Control;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * M3 linear progress indicator: determinate (0-1) and indeterminate modes.
 *
 * <p>See
 * <a href="https://m3.material.io/components/progress-indicators/overview">M3 progress
 * indicators</a>. Track and active indicator 4dp, primary active indicator on a
 * surface-container-highest track, 4dp stop indicator dot in primary-container separated by a 4dp
 * gap (updated M3 spec; values cross-checked against the Jetpack Compose material3
 * {@code LinearProgressIndicator} defaults). The indeterminate animation replays the two-bar
 * translate/scale cycles with the exact keyframe fractions and easings from
 * {@code material-web/progress/internal/_linear-progress.scss} (2s infinite loop).
 */
public class M3LinearProgress extends Control {

    /** Determinate value meaning "indeterminate". */
    public static final double INDETERMINATE = -1;

    private final DoubleProperty progress =
            new SimpleDoubleProperty(this, "progress", INDETERMINATE);
    private final BooleanProperty indeterminate =
            new SimpleBooleanProperty(this, "indeterminate", true);

    /** Creates an indeterminate linear progress. */
    public M3LinearProgress() {
        this(INDETERMINATE);
    }

    /**
     * Creates a linear progress with a value.
     *
     * @param progress 0-1, or {@link #INDETERMINATE}
     */
    public M3LinearProgress(double progress) {
        getStyleClass().add("m3-linear-progress");
        setProgress(progress);
        progressProperty().addListener((obs, oldV, newV) -> syncIndeterminate(newV.doubleValue()));
        setAccessibleRole(AccessibleRole.PROGRESS_INDICATOR);
        setFocusTraversable(false);
        setMinHeight(4);
        setPrefHeight(4);
        setMaxHeight(4);
        syncIndeterminate(getProgress());
    }

    /** Returns the progress property. */
    public DoubleProperty progressProperty() {
        return progress;
    }

    /** Returns the progress value. */
    public double getProgress() {
        return progress.get();
    }

    /**
     * Sets progress (0-1) or {@link #INDETERMINATE}.
     *
     * @param progress the new value
     */
    public void setProgress(double progress) {
        this.progress.set(progress);
    }

    /** Returns the indeterminate property. */
    public BooleanProperty indeterminateProperty() {
        return indeterminate;
    }

    /** Returns whether the indicator is indeterminate. */
    public boolean isIndeterminate() {
        return indeterminate.get();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new LinearSkin(this);
    }

    private void syncIndeterminate(double value) {
        boolean nowIndeterminate = value < 0;
        indeterminate.set(nowIndeterminate);
        pseudoClassStateChanged(PseudoClass.getPseudoClass("indeterminate"), nowIndeterminate);
    }

    /**
     * Skin drawing the M3 track, active indicator, stop indicator dot and the two-bar
     * indeterminate animation with auto-layout panes. The determinate row is an {@link HBox}
     * (bar | 4dp gap | track | 4dp gap | 4dp dot) and the indeterminate layer is a
     * {@link StackPane} whose bar preferred widths and translations the timeline drives.
     */
    static final class LinearSkin extends SkinBase<M3LinearProgress> {

        private static final double THICKNESS = 4; // md.comp.linear-progress track/active height
        private static final double STOP_DOT = 4; // stop indicator size (4dp dot)
        private static final double GAP = 4; // 4dp gap around the stop indicator
        private static final double SHOW_DOT_THRESHOLD = 0.01; // Compose drawStopIndicatorThreshold
        private static final Duration CYCLE = Duration.millis(2000); // indeterminate duration

        private final StackPane root = new StackPane();
        private final HBox determinateRow = new HBox();
        private final Region activeBar = new Region();
        private final Region leadGap = new Region();
        private final Region trackTail = new Region();
        private final Region endGap = new Region();
        private final Region stopDot = new Region();

        private final StackPane indeterminateLayer = new StackPane();
        private final Region track = new Region();
        private final Region primaryBar = new Region();
        private final Region secondaryBar = new Region();
        private final Rectangle clip = new Rectangle();

        private Timeline indeterminateLoop;
        private double loopWidth = -1;
        private boolean inScene;

        private final ChangeListener<Number> progressListener =
                (obs, oldV, newV) -> layoutDeterminate();
        private final ChangeListener<Boolean> indeterminateListener = (obs, oldV, newV) ->
                updateMode();
        private final ChangeListener<Number> widthListener = (obs, oldV, newV) -> {
            if (getSkinnable() != null && getSkinnable().isIndeterminate()) {
                startLoop(); // rebuild the timeline for the new width
            }
            layoutDeterminate();
        };

        LinearSkin(M3LinearProgress control) {
            super(control);

            String trackStyle =
                    "-fx-background-color: -md-sys-color-surface-container-highest;"
                            + " -fx-background-radius: 2;";
            String barStyle = "-fx-background-color: -md-sys-color-primary;"
                    + " -fx-background-radius: 2;";
            activeBar.setStyle(barStyle);
            secondaryBar.setStyle(barStyle);
            trackTail.setStyle(trackStyle);
            track.setStyle(trackStyle);
            stopDot.setStyle(
                    "-fx-background-color: -md-sys-color-primary-container;"
                            + " -fx-background-radius: 2;");

            // Determinate row: active bar | 4dp gap | trailing track | 4dp gap | 4dp stop dot.
            // The 4dp cells stay managed so the stop area is always reserved.
            activeBar.getStyleClass().add("m3-linear-active");
            leadGap.getStyleClass().add("m3-linear-gap");
            trackTail.getStyleClass().add("m3-linear-track");
            endGap.getStyleClass().add("m3-linear-gap");
            stopDot.getStyleClass().add("m3-linear-stop");
            activeBar.setPrefHeight(THICKNESS);
            leadGap.setPrefSize(GAP, THICKNESS);
            trackTail.setPrefHeight(THICKNESS);
            endGap.setPrefSize(GAP, THICKNESS);
            stopDot.setPrefSize(STOP_DOT, STOP_DOT);
            stopDot.setOpacity(0);
            HBox.setHgrow(trackTail, Priority.ALWAYS);
            activeBar.setVisible(false);
            leadGap.setVisible(false);
            determinateRow.setFillHeight(true);
            determinateRow.getChildren()
                    .addAll(activeBar, leadGap, trackTail, endGap, stopDot);
            determinateRow.setMaxWidth(Double.MAX_VALUE);

            // Indeterminate layer: full-width track behind two animated bars.
            track.getStyleClass().add("m3-linear-track");
            primaryBar.getStyleClass().add("m3-linear-active");
            secondaryBar.getStyleClass().add("m3-linear-secondary");
            track.setPrefHeight(THICKNESS);
            track.setMaxWidth(Double.MAX_VALUE);
            primaryBar.setPrefHeight(THICKNESS);
            primaryBar.setMaxWidth(USE_PREF_SIZE); // StackPane honors the animated pref width
            secondaryBar.setPrefHeight(THICKNESS);
            secondaryBar.setMaxWidth(USE_PREF_SIZE);
            StackPane.setAlignment(primaryBar, Pos.CENTER_LEFT);
            StackPane.setAlignment(secondaryBar, Pos.CENTER_LEFT);
            primaryBar.setVisible(false);
            secondaryBar.setVisible(false);
            indeterminateLayer.getChildren().addAll(track, primaryBar, secondaryBar);
            indeterminateLayer.setMaxWidth(Double.MAX_VALUE);

            root.getStyleClass().add("m3-linear-progress-bar");
            root.getChildren().addAll(indeterminateLayer, determinateRow);
            clip.widthProperty().bind(root.widthProperty());
            clip.heightProperty().bind(root.heightProperty());
            root.setClip(clip); // the indeterminate bars travel past the right edge
            getChildren().add(root);

            control.progressProperty().addListener(progressListener);
            control.indeterminateProperty().addListener(indeterminateListener);
            control.widthProperty().addListener(widthListener);
            control.sceneProperty().addListener((obs, oldV, newV) -> {
                inScene = newV != null;
                updateMode();
            });
            updateMode();
        }

        private void updateMode() {
            M3LinearProgress control = getSkinnable();
            if (control == null) {
                return;
            }
            boolean indeterminate = control.isIndeterminate();
            determinateRow.setVisible(!indeterminate);
            indeterminateLayer.setVisible(indeterminate);
            primaryBar.setVisible(indeterminate && inScene);
            secondaryBar.setVisible(indeterminate && inScene);
            if (indeterminate) {
                stopLoop();
                if (inScene) {
                    startLoop();
                }
            } else {
                stopLoop();
                layoutDeterminate();
            }
        }

        /** Updates the determinate row per the updated spec (bar | gap | track | gap | dot). */
        private void layoutDeterminate() {
            M3LinearProgress control = getSkinnable();
            if (control == null || control.isIndeterminate() || control.getWidth() <= 0) {
                return;
            }
            double frac = Math.max(0, Math.min(1, control.getProgress()));
            boolean showDot = frac > SHOW_DOT_THRESHOLD;
            stopDot.setOpacity(showDot ? 1 : 0);
            stopDot.setVisible(true); // managed: the stop area stays reserved
            boolean hasBar = frac > 0;
            activeBar.setVisible(hasBar);
            leadGap.setVisible(hasBar);
            if (hasBar) {
                double trackArea = Math.max(0, control.getWidth() - GAP - STOP_DOT);
                activeBar.setPrefWidth(frac * trackArea);
            }
        }

        private void startLoop() {
            double w = getSkinnable().getWidth();
            if (w <= 0) {
                return;
            }
            if (indeterminateLoop != null && loopWidth == w) {
                return; // already running at this width
            }
            stopLoop();
            loopWidth = w;
            indeterminateLoop = buildLoop(w);
            indeterminateLoop.setCycleCount(Timeline.INDEFINITE);
            indeterminateLoop.play();
        }

        private void stopLoop() {
            if (indeterminateLoop != null) {
                indeterminateLoop.stop();
                indeterminateLoop = null;
            }
            loopWidth = -1;
        }

        /**
         * Builds the 2s two-bar cycle. Frame fractions and easings are taken verbatim from
         * {@code material-web/progress/internal/_linear-progress.scss} (which ports the MDC
         * indeterminate animation); each bar animates translateX (as a fraction of the width)
         * and preferred width (the CSS {@code scaleX} of a full-width bar).
         */
        private Timeline buildLoop(double w) {
            Interpolator pTx1 = Interpolator.SPLINE(0.5, 0, 0.701732, 0.495819);
            Interpolator pTx2 = Interpolator.SPLINE(0.302435, 0.381352, 0.55, 0.956352);
            Interpolator pW1 = Interpolator.SPLINE(0.334731, 0.12482, 0.785844, 1);
            Interpolator pW2 = Interpolator.SPLINE(0.06, 0.11, 0.6, 1);
            Interpolator sTx1 = Interpolator.SPLINE(0.15, 0, 0.515058, 0.409685);
            Interpolator sTx2 = Interpolator.SPLINE(0.31033, 0.284058, 0.8, 0.733712);
            Interpolator sTx3 = Interpolator.SPLINE(0.4, 0.627035, 0.6, 0.902026);
            Interpolator sW1 = Interpolator.SPLINE(0.205028, 0.057051, 0.57661, 0.453971);
            Interpolator sW2 = Interpolator.SPLINE(0.152313, 0.196432, 0.648374, 1.00432);
            Interpolator sW3 = Interpolator.SPLINE(0.257759, -0.003163, 0.211762, 1.38179);

            return new Timeline(
                    // primary-indeterminate-translate: 0, 20%: 0 -> 59.15%: 83.6714%
                    //   -> 100%: 200.611%; primary-indeterminate-scale: 0.08
                    //   -> 69.15%: 0.661479 -> 100%: 0.08
                    frame(0, kv(primaryBar.translateXProperty(), 0, Interpolator.LINEAR),
                            kv(primaryBar.prefWidthProperty(), 0.08 * w, Interpolator.LINEAR)),
                    frame(0.20, kv(primaryBar.translateXProperty(), 0, Interpolator.LINEAR)),
                    frame(0.3665, kv(primaryBar.prefWidthProperty(), 0.08 * w, Interpolator.LINEAR)),
                    frame(0.5915, kv(primaryBar.translateXProperty(), 0.836714 * w, pTx1)),
                    frame(0.6915, kv(primaryBar.prefWidthProperty(), 0.661479 * w, pW1)),
                    frame(1, kv(primaryBar.translateXProperty(), 2.00611 * w, pTx2),
                            kv(primaryBar.prefWidthProperty(), 0.08 * w, pW2)),
                    // secondary-indeterminate-translate: 0 -> 25%: 37.6519% -> 48.35%: 84.3862%
                    //   -> 100%: 160.278%; secondary-indeterminate-scale: 0.08
                    //   -> 19.15%: 0.457104 -> 44.15%: 0.72796 -> 100%: 0.08
                    frame(0, kv(secondaryBar.translateXProperty(), 0, Interpolator.LINEAR),
                            kv(secondaryBar.prefWidthProperty(), 0.08 * w, Interpolator.LINEAR)),
                    frame(0.1915, kv(secondaryBar.prefWidthProperty(), 0.457104 * w, sW1)),
                    frame(0.25, kv(secondaryBar.translateXProperty(), 0.376519 * w, sTx1)),
                    frame(0.4415, kv(secondaryBar.prefWidthProperty(), 0.72796 * w, sW2)),
                    frame(0.4835, kv(secondaryBar.translateXProperty(), 0.843862 * w, sTx2)),
                    frame(1, kv(secondaryBar.translateXProperty(), 1.60278 * w, sTx3),
                            kv(secondaryBar.prefWidthProperty(), 0.08 * w, sW3)));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static KeyFrame frame(double fraction, KeyValue... keyValues) {
            return new KeyFrame(Duration.millis(CYCLE.toMillis() * fraction), keyValues);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static KeyValue kv(
                DoubleProperty property, double value, Interpolator interpolator) {
            return new KeyValue(property, value, interpolator);
        }

        @Override
        public void dispose() {
            stopLoop();
            M3LinearProgress control = getSkinnable();
            if (control != null) {
                control.progressProperty().removeListener(progressListener);
                control.indeterminateProperty().removeListener(indeterminateListener);
                control.widthProperty().removeListener(widthListener);
            }
            clip.widthProperty().unbind();
            clip.heightProperty().unbind();
            super.dispose();
        }
    }

    /**
     * Creates an M3-styled circular indicator node.
     *
     * <p>Kept as a factory (instead of a second control file) so both indicators share one home.
     *
     * @param progress 0-1, or {@link #INDETERMINATE}
     * @return a circular progress node
     */
    public static ProgressIndicator circular(double progress) {
        ProgressIndicator indicator = new ProgressIndicator(progress);
        indicator.getStyleClass().add("m3-circular-progress");
        indicator.setAccessibleRole(AccessibleRole.PROGRESS_INDICATOR);
        indicator.setFocusTraversable(false);
        return indicator;
    }
}
