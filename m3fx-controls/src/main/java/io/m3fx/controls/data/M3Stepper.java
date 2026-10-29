package io.m3fx.controls.data;

import io.m3fx.controls.icon.M3Icon;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.Arrays;

/**
 * Multi-step Workflow Stepper with illuminated active nodes, completed checkmark status,
 * error states, and responsive connecting progress pipelines.
 *
 * <p>Completed steps are clickable (and keyboard operable with Enter/Space) to navigate back.
 */
public class M3Stepper extends HBox {

    private final ObservableList<String> stepTitles = FXCollections.observableArrayList();
    private final ObservableList<Integer> errorSteps = FXCollections.observableArrayList();
    private final IntegerProperty currentStep = new SimpleIntegerProperty(this, "currentStep", 0);

    /** Creates an empty stepper. */
    public M3Stepper() {
        this(new String[0]);
    }

    /**
     * Creates a stepper with step titles.
     *
     * @param titles step titles
     */
    public M3Stepper(String... titles) {
        getStyleClass().add("m3-stepper");
        setAlignment(Pos.CENTER);
        setSpacing(0);

        if (titles != null && titles.length > 0) {
            stepTitles.addAll(Arrays.asList(titles));
        }

        stepTitles.addListener((ListChangeListener<String>) c -> rebuild());
        errorSteps.addListener((ListChangeListener<Integer>) c -> rebuild());
        currentStep.addListener((obs, o, n) -> rebuild());

        rebuild();
    }

    /** The step titles, one per step. */
    public ObservableList<String> getStepTitles() {
        return stepTitles;
    }

    /**
     * Indices of steps rendered in the error state (failed validation, rejected data...).
     * An errored step overrides the completed/active visual and can be clicked to retry.
     *
     * @return the observable list of errored step indices
     */
    public ObservableList<Integer> getErrorSteps() {
        return errorSteps;
    }

    /** The zero-based index of the active step (clamped to the step count). */
    public IntegerProperty currentStepProperty() {
        return currentStep;
    }

    public int getCurrentStep() {
        return currentStep.get();
    }

    public void setCurrentStep(int step) {
        int max = Math.max(0, stepTitles.size() - 1);
        this.currentStep.set(Math.max(0, Math.min(max, step)));
    }

    /** Advances to the next step (no-op past the last step). */
    public void nextStep() {
        setCurrentStep(getCurrentStep() + 1);
    }

    /** Returns to the previous step (no-op before the first step). */
    public void prevStep() {
        setCurrentStep(getCurrentStep() - 1);
    }

    private void rebuild() {
        getChildren().clear();
        int n = stepTitles.size();
        int active = getCurrentStep();

        for (int i = 0; i < n; i++) {
            boolean isCompleted = i < active;
            boolean isActive = i == active;
            boolean isError = errorSteps.contains(i);
            boolean isLast = i == n - 1;
            boolean clickable = isCompleted || isError;
            int stepIndex = i;

            VBox stepBox = new VBox(6);
            stepBox.setAlignment(Pos.CENTER);

            // Step Node (Circle + Indicator)
            StackPane nodeCircle = new StackPane();
            nodeCircle.setMinSize(32, 32);
            nodeCircle.setPrefSize(32, 32);
            nodeCircle.setMaxSize(32, 32);
            nodeCircle.setAccessibleRole(javafx.scene.AccessibleRole.BUTTON);
            nodeCircle.setAccessibleText("Step " + (i + 1) + ": " + stepTitles.get(i));

            if (isError) {
                Circle c = new Circle(14);
                c.getStyleClass().add("m3-stepper-circle-error");
                // M3 stepper error glyph is "!" (priority_high is not in the bundled icon catalog)
                Label bang = new Label("!");
                bang.getStyleClass().add("m3-stepper-num-error");
                nodeCircle.getChildren().addAll(c, bang);
            } else if (isCompleted) {
                Circle c = new Circle(14);
                c.getStyleClass().add("m3-stepper-circle-completed");
                M3Icon check = M3Icon.symbol("check");
                check.setScaleX(0.6);
                check.setScaleY(0.6);
                check.getStyleClass().add("m3-stepper-icon-completed");
                nodeCircle.getChildren().addAll(c, check);
            } else if (isActive) {
                Circle halo = new Circle(16);
                halo.getStyleClass().add("m3-stepper-circle-active");
                Label num = new Label(String.valueOf(i + 1));
                num.getStyleClass().add("m3-stepper-num-active");
                nodeCircle.getChildren().addAll(halo, num);
            } else {
                Circle c = new Circle(14);
                c.getStyleClass().add("m3-stepper-circle-pending");
                Label num = new Label(String.valueOf(i + 1));
                num.getStyleClass().add("m3-stepper-num-pending");
                nodeCircle.getChildren().addAll(c, num);
            }

            if (clickable) {
                nodeCircle.getStyleClass().add("m3-stepper-clickable");
                nodeCircle.setFocusTraversable(true);
                nodeCircle.setOnMouseClicked(e -> setCurrentStep(stepIndex));
                nodeCircle.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                        setCurrentStep(stepIndex);
                        e.consume();
                    }
                });
            }

            // Step Label
            Label title = new Label(stepTitles.get(i));
            title.getStyleClass().add("m3-stepper-title");
            if (isActive) {
                title.getStyleClass().add("active");
            } else if (isError) {
                title.getStyleClass().add("error");
            }

            stepBox.getChildren().addAll(nodeCircle, title);
            getChildren().add(stepBox);

            // Connecting progress line, wrapped in a 32px band top-aligned with the circles so
            // it stays centered on them regardless of label height
            if (!isLast) {
                Region line = new Region();
                line.setPrefHeight(2.0);
                line.setMinHeight(2.0);
                line.setMaxHeight(2.0);
                line.setMaxWidth(Double.MAX_VALUE);
                line.getStyleClass().add("m3-stepper-line");
                if (isCompleted && !isError) {
                    line.getStyleClass().add("completed");
                }

                StackPane lineBand = new StackPane(line);
                lineBand.setMinSize(0, 32);
                lineBand.setPrefSize(USE_PREF_SIZE, 32);
                lineBand.setMaxSize(Double.MAX_VALUE, 32);
                HBox.setHgrow(lineBand, Priority.ALWAYS);
                // lineBand's max height (32) caps stretching, so the default TOP alignment
                // keeps the connector centered on the 32px circles regardless of label height.
                getChildren().add(lineBand);
            }
        }
    }
}

