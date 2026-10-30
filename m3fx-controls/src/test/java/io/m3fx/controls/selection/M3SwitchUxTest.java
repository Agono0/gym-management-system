package io.m3fx.controls.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class M3SwitchUxTest {

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
    void announcesOnAndOff() {
        M3Switch on = new M3Switch(true);
        assertEquals("On", on.getAccessibleText());

        M3Switch off = new M3Switch(false);
        assertEquals("Off", off.getAccessibleText());

        off.setSelected(true);
        assertEquals("On", off.getAccessibleText(), "Announcement follows the state");
    }

    @Test
    void spaceTogglesAndIsConsumed() throws Throwable {
        runOnFx(() -> {
            M3Switch toggle = new M3Switch(false);
            javafx.scene.layout.StackPane holder = new javafx.scene.layout.StackPane(toggle);
            javafx.scene.Scene scene = new javafx.scene.Scene(holder);
            holder.applyCss();
            // fireEvent() retargets per node, so consumption is observed on the control itself:
            // this handler runs after the skin's (registration order) on the same dispatcher.
            AtomicReference<Boolean> consumed = new AtomicReference<>(false);
            toggle.addEventHandler(KeyEvent.KEY_PRESSED, e -> consumed.set(e.isConsumed()));

            toggle.fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE, false, false, false, false));
            assertTrue(toggle.isSelected(), "Space toggles the switch");
            assertTrue(consumed.get(), "Space is consumed so the page does not scroll");

            toggle.fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
            assertTrue(!toggle.isSelected(), "Enter toggles back");
            assertTrue(consumed.get(), "Enter is consumed as well");
        });
    }

    @Test
    void handCursorShowsPointerAffordance() {
        assertEquals(Cursor.HAND, new M3Switch().getCursor());
    }

    @Test
    void skinSwapKeepsSwitchWorking() throws Throwable {
        runOnFx(() -> {
            M3Switch toggle = new M3Switch(false);
            StackPane holder = new StackPane(toggle);
            new Scene(holder);
            toggle.applyCss();

            toggle.setSkin(null);
            toggle.setSelected(true);
            assertTrue(toggle.isSelected(), "State changes still work after the skin is detached");
        });
    }
}
