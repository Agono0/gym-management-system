package io.m3fx.controls.textfield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

/**
 * Locks the M3TextField contract: variant style classes, the floating-label state machine
 * (floated when focused or carrying text; resting when empty and unfocused), supporting text,
 * error state, maxLength truncation + counter, and the icon slot order/hit targets.
 *
 * <p>B11 evidence (pinned current behavior): the floating {@code Label} is never associated with
 * the editable input — the inner {@code TextField} publishes no accessible text and no prompt
 * text, so screen readers announce an unlabeled field. The spec requires the label to be
 * announced; B11 should fix this (see {@code inputHasNoAnnouncedLabelYet}).
 */
class M3TextFieldTest {

    private static final PseudoClass FLOATED = PseudoClass.getPseudoClass("floated");
    private static final PseudoClass INPUT_FOCUSED = PseudoClass.getPseudoClass("input-focused");
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final PseudoClass OVER = PseudoClass.getPseudoClass("over");

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to initialize");
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
        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    private static boolean hasPseudo(M3TextField field, PseudoClass pseudo) {
        return field.getPseudoClassStates().contains(pseudo);
    }

    @Test
    void variantStyleClassesFollowTheVariant() {
        M3TextField field = new M3TextField("Email", TextFieldVariant.FILLED);
        assertTrue(field.getStyleClass().contains("m3-text-field-filled"));
        field.setVariant(TextFieldVariant.OUTLINED);
        assertFalse(field.getStyleClass().contains("m3-text-field-filled"));
        assertTrue(field.getStyleClass().contains("m3-text-field-outlined"));
    }

    @Test
    void containerIsSpecHeight56() {
        M3TextField field = new M3TextField("Email", TextFieldVariant.FILLED);
        assertEquals(56, M3TextField.CONTAINER_HEIGHT, 1e-9);
        assertTrue(field.getChildren().size() >= 2, "label row and input row are stacked");
    }

    @Test
    void floatStateFollowsContent() throws Throwable {
        runOnFx(() -> {
            M3TextField field = new M3TextField("Email", TextFieldVariant.FILLED);
            new Scene(new StackPane(field)); // text listener works headless; focus needs a scene
            assertFalse(hasPseudo(field, FLOATED),
                    "empty + unfocused = resting label (M3 spec)");
            field.setText("user@example.com");
            assertTrue(hasPseudo(field, FLOATED), "carrying text floats the label");
            field.setText("");
            assertFalse(hasPseudo(field, FLOATED), "clearing returns to the resting label");
        });
    }

    @Test
    void focusedFieldFloatsTheLabelEvenWhenEmpty() throws Throwable {
        // Real focus needs a real window: a windowless scene cannot grant focus, so this test
        // shows a Stage (implicit exit disabled; hidden in finally per the house convention).
        Platform.setImplicitExit(false);
        AtomicReference<M3TextField> fieldRef = new AtomicReference<>();
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        try {
            runOnFx(() -> {
                M3TextField field = new M3TextField("Email", TextFieldVariant.FILLED);
                StackPane root = new StackPane(field);
                Scene scene = new Scene(root, 200, 120);
                Stage stage = new Stage();
                stage.setScene(scene);
                stage.show();
                field.getInput().requestFocus();
                assertTrue(field.getInput().isFocused(), "inner input takes focus");
                assertTrue(hasPseudo(field, FLOATED), "focus floats the label");
                assertTrue(hasPseudo(field, INPUT_FOCUSED), "input-focused tracks the caret owner");
                fieldRef.set(field);
                stageRef.set(stage);
            });
            runOnFx(() -> {
                assertTrue(hasPseudo(fieldRef.get(), FLOATED), "the float state persists");
                stageRef.get().hide();
            });
        } finally {
            try {
                runOnFx(() -> {
                    if (stageRef.get() != null) {
                        stageRef.get().hide();
                    }
                });
            } catch (Throwable cleanupFailure) {
                // best effort - a failed assertion must not leave the stage open
            }
        }
    }

    @Test
    void supportingTextBindsAndErrorFlagsTheField() {
        M3TextField field = new M3TextField("Email", TextFieldVariant.OUTLINED);
        field.setSupportingText("We will never share it");
        // Bottom row: support label left, counter right.
        javafx.scene.Node bottomRow = field.getChildren().get(2);
        javafx.scene.control.Label support = (javafx.scene.control.Label)
                ((javafx.scene.layout.HBox) bottomRow).getChildren().get(0);
        assertEquals("We will never share it", support.getText());

        assertFalse(hasPseudo(field, ERROR));
        field.setError(true);
        assertTrue(hasPseudo(field, ERROR), "error drives the :error pseudo-class");
        field.setError(false);
        assertFalse(hasPseudo(field, ERROR));
    }

    @Test
    void maxLengthTruncatesAndFlagsTheCounter() throws Throwable {
        runOnFx(() -> {
            M3TextField field = new M3TextField("Code", TextFieldVariant.FILLED);
            field.setMaxLength(5);
            javafx.scene.Node bottomRow = field.getChildren().get(2);
            javafx.scene.control.Label counter = (javafx.scene.control.Label)
                    ((javafx.scene.layout.HBox) bottomRow).getChildren().get(2);
            assertEquals("0 / 5", counter.getText(),
                    "the counter renders immediately once a limit is set");

            field.setText("12345678");
            assertEquals("12345", field.getText(), "input truncates at the limit");
            assertEquals("5 / 5", counter.getText());
            assertTrue(counter.getPseudoClassStates().contains(OVER),
                    "at-limit counter flags in the error color");

            field.setText("12");
            assertEquals("2 / 5", counter.getText());
            assertFalse(counter.getPseudoClassStates().contains(OVER));

            field.setMaxLength(-1);
            assertEquals("", counter.getText(), "-1 clears the counter");
        });
    }

    @Test
    void iconSlotsKeepOrderAndHitTargetSize() {
        M3TextField field = new M3TextField("Search", TextFieldVariant.OUTLINED);
        javafx.scene.layout.HBox inputRow = (javafx.scene.layout.HBox) field.getChildren().get(1);
        Rectangle leading = new Rectangle(18, 18);
        Rectangle trailing = new Rectangle(18, 18);
        field.setLeadingIcon(leading);
        field.setTrailingIcon(trailing);

        assertEquals(3, inputRow.getChildren().size(), "leading, input, trailing");
        javafx.scene.Node leadingHit = inputRow.getChildren().get(0);
        javafx.scene.Node trailingHit = inputRow.getChildren().get(2);
        assertEquals("m3-text-field-leading", leadingHit.getId());
        assertEquals("m3-text-field-trailing", trailingHit.getId());
        assertEquals(40, leadingHit.prefWidth(-1), 1e-9, "icon hit target stays 40px square");
        assertEquals(40, leadingHit.prefHeight(-1), 1e-9);
        assertTrue(((javafx.scene.layout.Pane) leadingHit).getChildren().contains(leading),
                "the icon sits inside its hit target");

        field.setLeadingIcon(null);
        assertEquals(2, inputRow.getChildren().size(), "null removes the slot");
        field.setTrailingIcon(null);
        assertEquals(1, inputRow.getChildren().size());
    }

    @Test
    void inputHasNoAnnouncedLabelYet() {
        // B11 red-first evidence: the floating label is a separate decorative Label; the inner
        // TextField announces nothing (no accessibleText, no promptText), so a screen reader
        // reads an unlabeled field. The M3 text-field spec requires the label to be announced.
        M3TextField field = new M3TextField("Email", TextFieldVariant.FILLED);
        assertNull(field.getInput().getAccessibleText(),
                "B11: the input must announce the label once fixed");
        assertEquals("", field.getInput().getPromptText(),
                "B11: promptText is another legitimate carrier for the label (empty here)");
        assertEquals("Email", field.getLabelText(), "the label text exists, just unassociated");
    }
}
