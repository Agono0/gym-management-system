package io.m3fx.controls.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.controls.button.M3Button;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class M3DialogUxTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
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
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    private static KeyEvent keyPressed(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }

    @Test
    void enterFiresDefaultButton() throws Throwable {
        runOnFx(() -> {
            StackPane root = new StackPane();
            io.m3fx.controls.internal.OverlayLayer layer =
                    new io.m3fx.controls.internal.OverlayLayer();
            root.getChildren().add(layer);
            Scene scene = new Scene(root, 480, 320);

            M3Button ok = M3Button.filled("OK");
            M3Dialog dialog = M3Dialog.basic("Delete member?", new Label("This cannot be undone."),
                    M3Button.text("Cancel"), ok);
            dialog.setDefaultAction(ok::activate);
            AtomicInteger okRuns = new AtomicInteger();
            ok.setOnAction(e -> okRuns.incrementAndGet());

            dialog.showIn(layer);
            dialog.fireEvent(keyPressed(KeyCode.ENTER));
            assertEquals(1, okRuns.get(), "Enter fires the default action");
            assertTrue(dialog.getDefaultAction() != null);
        });
    }

    @Test
    void enterIsIgnoredWhileTextFieldHasFocus() throws Throwable {
        runOnFx(() -> {
            StackPane root = new StackPane();
            io.m3fx.controls.internal.OverlayLayer layer =
                    new io.m3fx.controls.internal.OverlayLayer();
            root.getChildren().add(layer);
            Scene scene = new Scene(root, 480, 320);

            TextField input = new TextField();
            M3Button ok = M3Button.filled("Rename");
            M3Dialog dialog = M3Dialog.basic("Rename", input, ok);
            dialog.setDefaultAction(ok::activate);
            AtomicInteger okRuns = new AtomicInteger();
            ok.setOnAction(e -> okRuns.incrementAndGet());

            dialog.showIn(layer);
            input.requestFocus();
            assertSame(input, scene.getFocusOwner(), "Text field owns focus inside the dialog");

            dialog.fireEvent(keyPressed(KeyCode.ENTER));
            assertEquals(0, okRuns.get(), "Enter must not trigger the default action from a text field");
        });
    }

    @Test
    void initialFocusLandsOnFirstAction() throws Throwable {
        runOnFx(() -> {
            StackPane root = new StackPane();
            io.m3fx.controls.internal.OverlayLayer layer =
                    new io.m3fx.controls.internal.OverlayLayer();
            root.getChildren().add(layer);
            Scene scene = new Scene(root, 480, 320);

            M3Button cancel = M3Button.text("Cancel");
            M3Dialog dialog = M3Dialog.basic("Title", new Label("Body"), cancel, M3Button.filled("OK"));
            dialog.showIn(layer);
            assertSame(cancel, scene.getFocusOwner(), "First focusable control inside the dialog");
        });
    }

    @Test
    void scrimDismissalReturnsFocusToOpener() throws Throwable {
        runOnFx(() -> {
            StackPane root = new StackPane();
            io.m3fx.controls.internal.OverlayLayer layer =
                    new io.m3fx.controls.internal.OverlayLayer();
            M3Button opener = M3Button.filled("Open");
            root.getChildren().addAll(layer, opener);
            Scene scene = new Scene(root, 480, 320);
            opener.requestFocus();

            M3Dialog dialog = M3Dialog.basic("Title", new Label("Body"), M3Button.text("OK"));
            dialog.showIn(layer);
            assertTrue(scene.getFocusOwner() != opener, "Focus moved into the dialog");

            // Simulates the scrim click path, which clears the layer without calling hide().
            layer.hideModal();
            assertSame(opener, scene.getFocusOwner(), "Focus returns to the opener after scrim dismissal");
        });
    }
}
