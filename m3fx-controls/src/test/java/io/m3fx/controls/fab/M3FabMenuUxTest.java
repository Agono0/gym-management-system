package io.m3fx.controls.fab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class M3FabMenuUxTest {

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

    private static MouseEvent clickAt(double x, double y) {
        // (type, x, y, screenX, screenY, button, clickCount, shift, ctrl, alt, meta,
        //  primary, middle, secondary, synthesized, popupTrigger, stillSincePress, pickResult)
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED, x, y, x, y, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null);
    }

    private static MouseEvent mouseAt(javafx.event.EventType<MouseEvent> type, double x, double y) {
        return new MouseEvent(
                type, x, y, x, y, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null);
    }

    private static M3Fab actionFab(M3FabMenu menu, int index) {
        Pane actionsBox = (Pane) menu.getChildren().get(0);
        Pane row = (Pane) actionsBox.getChildren().get(index);
        return (M3Fab) row.getChildren().get(1);
    }

    @Test
    void clickingRowLabelFiresActionOnce() throws Throwable {
        runOnFx(() -> {
            M3FabMenu menu = new M3FabMenu();
            AtomicInteger runs = new AtomicInteger();
            menu.addAction("Add", "add", runs::incrementAndGet);
            StackPane holder = new StackPane(menu);
            new Scene(holder, 200, 200);
            holder.applyCss();
            menu.expand();

            Pane actionsBox = (Pane) menu.getChildren().get(0);
            Pane row = (Pane) actionsBox.getChildren().get(0);
            row.fireEvent(clickAt(2, 2));
            assertEquals(1, runs.get(), "Row click runs the action once");

            M3Fab fab = actionFab(menu, 0);
            fab.fireEvent(mouseAt(MouseEvent.MOUSE_PRESSED, 2, 2));
            fab.fireEvent(mouseAt(MouseEvent.MOUSE_RELEASED, 2, 2));
            assertEquals(2, runs.get(), "Full press on the FAB itself fires exactly once");
        });
    }

    @Test
    void expandAndCollapseMoveFocus() throws Throwable {
        runOnFx(() -> {
            M3FabMenu menu = new M3FabMenu();
            menu.addAction("First", "add", () -> {});
            menu.addAction("Second", "add", () -> {});
            StackPane holder = new StackPane(menu);
            Scene scene = new Scene(holder, 200, 260);
            holder.applyCss();

            menu.expand();
            assertSame(actionFab(menu, 0), scene.getFocusOwner(), "Opening focuses the first action");

            menu.collapse();
            assertSame(menu.getChildren().get(1), scene.getFocusOwner(), "Closing returns focus to the FAB");
        });
    }
}
