package io.m3fx.controls.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class M3SheetUxTest {

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

    @Test
    void bottomSheetFocusesContentAndReturnsFocusOnScrimDismissal() throws Throwable {
        runOnFx(() -> {
            StackPane root = new StackPane();
            io.m3fx.controls.internal.OverlayLayer layer =
                    new io.m3fx.controls.internal.OverlayLayer();
            javafx.scene.control.Button opener = new Button("Open");
            root.getChildren().addAll(layer, opener);
            Scene scene = new Scene(root, 480, 400);
            opener.requestFocus();

            M3BottomSheet sheet = new M3BottomSheet();
            javafx.scene.control.Button inside = new Button("Sheet action");
            sheet.setContent(new javafx.scene.layout.VBox(new Label("Body"), inside));
            sheet.showIn(layer);
            assertSame(inside, scene.getFocusOwner(), "Focus moves to the first content control");

            // Simulates the scrim click path, which clears the layer without calling hide().
            layer.hideSheet();
            assertSame(opener, scene.getFocusOwner(), "Focus returns to the opener");
        });
    }

    @Test
    void sideSheetFocusReturnsOnScrimDismissal() throws Throwable {
        runOnFx(() -> {
            StackPane root = new StackPane();
            io.m3fx.controls.internal.OverlayLayer layer =
                    new io.m3fx.controls.internal.OverlayLayer();
            javafx.scene.control.Button opener = new Button("Open");
            root.getChildren().addAll(layer, opener);
            Scene scene = new Scene(root, 640, 400);
            opener.requestFocus();

            M3SideSheet sheet = new M3SideSheet();
            sheet.setContent(new Label("Body"));
            sheet.showIn(layer);
            assertTrue(scene.getFocusOwner() != opener, "Focus moved into the side sheet");

            layer.hideModal();
            assertSame(opener, scene.getFocusOwner(), "Focus returns to the opener");
        });
    }
}
