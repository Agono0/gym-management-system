package io.m3fx.controls.progress;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Control;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.HBox;

/**
 * M3 linear progress indicator: determinate (0-1) and indeterminate modes.
 *
 * <p>See
 * <a href="https://m3.material.io/components/progress-indicators/overview">M3 progress
 * indicators</a>. Track 4dp (16dp stop indicator gap), primary active indicator on
 * surface-container-highest track with a primary-container stop dot.
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

    /** Skin delegating to a styled JavaFX ProgressBar. */
    static final class LinearSkin extends SkinBase<M3LinearProgress> {
        private final ProgressBar bar = new ProgressBar();

        LinearSkin(M3LinearProgress control) {
            super(control);
            bar.getStyleClass().add("m3-linear-progress-bar");
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.progressProperty().bind(control.progressProperty());
            HBox box = new HBox(bar);
            HBox.setHgrow(bar, javafx.scene.layout.Priority.ALWAYS);
            getChildren().add(box);
        }

        @Override
        public void dispose() {
            bar.progressProperty().unbind();
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
