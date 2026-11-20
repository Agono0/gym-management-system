package io.m3fx.controls.textfield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.controls.M3Stylesheets;
import io.m3fx.core.theme.M3Theme;
import io.m3fx.core.theme.ThemeVariant;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * B11: the floating label rests INSIDE the container (empty + unfocused) and floats only on
 * focus or non-empty text - the {@code :floated} pseudo-class drives the CSS positions.
 */
class M3TextFieldRestingLabelTest {

    private static final PseudoClass FLOATED = PseudoClass.getPseudoClass("floated");

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
        Platform.setImplicitExit(false);
    }

    private static void runOnFx(Runnable action) throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(30, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    private static boolean floated(javafx.scene.Node node) {
        return node.getPseudoClassStates().contains(FLOATED);
    }

    @Test
    void labelRestsInsideTheContainerAndFloatsOnTextOrFocus() throws Throwable {
        M3TextField[] holder = new M3TextField[1];
        Stage[] stageHolder = new Stage[1];
        double[] restMinY = new double[1];
        double[] restFontSize = new double[1];
        runOnFx(() -> {
            M3TextField field = new M3TextField("Equipment Name", TextFieldVariant.FILLED);
            javafx.scene.control.Button focusSink = new javafx.scene.control.Button();
            StackPane root = new StackPane(field, focusSink);
            Scene scene = new Scene(root, 300, 120);
            M3Stylesheets.applyTo(scene, M3Theme.fromSeed(
                    javafx.scene.paint.Color.web("#6750A4"), ThemeVariant.TONAL_SPOT, false));
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
            stageHolder[0] = stage;
            holder[0] = field;
        });
        runOnFx(() -> {
            // The shown stage focuses the input: the focus path floats the label (B11).
            assertTrue(floated(holder[0]), "focus floats the label");
            // Blur to the focus sink: empty + unfocused -> the label rests inside the container.
            ((javafx.scene.control.Control) ((StackPane) stageHolder[0].getScene().getRoot())
                    .getChildren().get(1)).requestFocus();
        });
        runOnFx(() -> assertFalse(floated(holder[0]),
                "an empty unfocused field must not float the label"));
        runOnFx(() -> {
            javafx.scene.Node label = holder[0].lookup(".m3-text-field-label");
            javafx.scene.Node box = holder[0].lookup(".m3-text-field-box");
            Bounds boxBounds = box.getBoundsInParent();
            Bounds labelBounds = label.getBoundsInParent();
            assertTrue(boxBounds.contains(labelBounds.getMinX(), labelBounds.getCenterY()),
                    "the resting label is vertically inside the container");
            restMinY[0] = labelBounds.getMinY();
            // The pulse between hops may not have re-applied CSS after the blur; force it
            // before measuring (same pattern as the floated hop below).
            holder[0].applyCss();
            label = holder[0].lookup(".m3-text-field-label");
            restFontSize[0] = ((javafx.scene.control.Label) label).getFont().getSize();
        });
        runOnFx(() -> {
            // Text without focus -> floats: the label rises toward the container's top and
            // shrinks to the 12px floated size (both inside the 56dp container, per spec).
            holder[0].setText("Generator 3");
            assertTrue(floated(holder[0]), "non-empty text floats the label");
        });
        runOnFx(() -> {
            // The :floated CSS applies on the next pulse; force it before measuring.
            holder[0].applyCss();
            javafx.scene.Node label = holder[0].lookup(".m3-text-field-label");
            assertTrue(label.getBoundsInParent().getMinY() < restMinY[0],
                    "the floated label rises from the resting position");
            javafx.scene.control.Label fl = (javafx.scene.control.Label) label;
            System.out.println("PROBE float font=" + fl.getFont().getSize()
                    + " rest=" + restFontSize[0] + " minY=" + label.getBoundsInParent().getMinY());
            assertTrue(fl.getFont().getSize() < restFontSize[0], "the floated label shrinks");
            holder[0].setText("");
        });
        runOnFx(() -> assertFalse(floated(holder[0]),
                "clearing the text restores the resting label"));
        runOnFx(() -> {
            stageHolder[0].hide();
            stageHolder[0].close();
        });
    }

    @Test
    void leadingIconShiftsTheLabelWithTheInputText() throws Throwable {
        M3TextField[] holder = new M3TextField[1];
        runOnFx(() -> {
            M3TextField field = new M3TextField("Equipment Name", TextFieldVariant.FILLED);
            field.setLeadingIcon(new javafx.scene.control.Label("Eq"));
            Scene scene = new Scene(new StackPane(field), 300, 120);
            M3Stylesheets.applyTo(scene, M3Theme.fromSeed(
                    javafx.scene.paint.Color.web("#6750A4"), ThemeVariant.TONAL_SPOT, false));
            field.applyCss();
            field.layout();
            holder[0] = field;
        });
        runOnFx(() -> {
            javafx.scene.Node label = holder[0].lookup(".m3-text-field-label");
            assertEquals(40, label.getTranslateX(),
                    "the resting label aligns with the input text after a leading icon");
            holder[0].setLeadingIcon(null);
            assertEquals(0, holder[0].lookup(".m3-text-field-label").getTranslateX(),
                    "removing the icon restores the label alignment");
        });
    }
}
