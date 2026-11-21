package io.m3fx.controls.textfield;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The password-reveal swap contract: an alternate input registered through
 * {@link M3TextField#trackAlternateInput(TextField)} participates in the float state, so the
 * floating label never overlays the swapped-in input's text (the login overlay bug).
 */
class M3TextFieldAlternateInputTest {

    private static boolean fxStarted;

    @BeforeAll
    static void initFx() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            ready.countDown();
        }
        if (!ready.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("FX toolkit did not start");
        }
        fxStarted = true;
        Platform.setImplicitExit(false);
    }

    @AfterAll
    static void keepToolkitAlive() {
        // fxStarted is intentionally unused beyond asserting bootstrap happened; the shared
        // toolkit must survive the class (implicit exit stays off for later test classes).
        assertTrue(fxStarted);
    }

    private static void runOnFx(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("FX step timed out");
        }
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @Test
    void alternateFocusAndTextDriveTheFloatState() throws Exception {
        runOnFx(() -> {
            M3TextField field = new M3TextField("Password", TextFieldVariant.FILLED);
            StackPane host = new StackPane(field);
            Scene scene = new Scene(host, 300, 120);
            // Focus transfer needs a shown window — show hidden-opacity, hide in finally
            // (a failed assertion must not leave the stage open, or Gradle hangs on the FX thread).
            Stage stage = new Stage();
            stage.setOpacity(0);
            stage.setScene(scene);
            stage.show();
            try {
                TextField masked = new TextField();
                masked.getStyleClass().addAll(field.getInput().getStyleClass());
                // Mirror the real swap: the alternate lives IN the scene while active
                // (a detached node cannot take focus).
                host.getChildren().add(masked);
                assertFalse(field.isFocused());

                field.trackAlternateInput(masked);

                // Masked input focused (the revealed state) → label floats even though the
                // primary input is detached and empty.
                assertFalse(field.getPseudoClassStates().contains(
                        javafx.css.PseudoClass.getPseudoClass("floated")));
                masked.requestFocus();
                assertTrue(field.getPseudoClassStates().contains(
                        javafx.css.PseudoClass.getPseudoClass("floated")),
                        "alternate focus floats the label");

                // Alternate text also floats (typing into the swapped-in input).
                masked.setText("hunter2");
                assertTrue(field.getPseudoClassStates().contains(
                        javafx.css.PseudoClass.getPseudoClass("floated")));

                // Cleared + focus off both inputs → resting state again. (Focusing the
                // primary input would legitimately keep the label floated.)
                masked.setText("");
                host.requestFocus();
                assertFalse(field.getPseudoClassStates().contains(
                        javafx.css.PseudoClass.getPseudoClass("floated")),
                        "empty alternate returns to resting");
            } finally {
                stage.hide();
            }
        });
    }

    @Test
    void clearingTheTrackingStopsTheParticipation() throws Exception {
        runOnFx(() -> {
            M3TextField field = new M3TextField("Password", TextFieldVariant.FILLED);
            TextField masked = new TextField();
            field.trackAlternateInput(masked);
            field.trackAlternateInput(null);
            masked.setText("typed after detach");
            assertFalse(field.getPseudoClassStates().contains(
                    javafx.css.PseudoClass.getPseudoClass("floated")),
                    "a detached alternate must not drive the float state");
        });
    }
}
