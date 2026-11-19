package io.m3fx.controls.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

    @Test
    void skinSwapRemovesClickAndKeyHandlers() throws Throwable {
        runOnFx(() -> {
            M3Switch toggle = new M3Switch(false);
            StackPane holder = new StackPane(toggle);
            new Scene(holder);
            toggle.applyCss();
            // The framework disposes the default skin and installs ours; the disposed skin's
            // MOUSE_CLICKED/KEY_PRESSED handlers must be gone with it.
            toggle.setSkin(new M3Switch.SwitchSkin(toggle));

            toggle.fireEvent(click());
            assertTrue(toggle.isSelected(), "exactly one handler toggles per click");
            toggle.fireEvent(click());
            assertFalse(toggle.isSelected(), "no double toggle from the stale skin");

            toggle.fireEvent(keyPress(KeyCode.SPACE));
            assertTrue(toggle.isSelected(), "Space toggles once after the skin swap");
            toggle.fireEvent(keyPress(KeyCode.ENTER));
            assertFalse(toggle.isSelected(), "Enter toggles once after the skin swap");
        });
    }

    @Test
    void disposeIsIdempotentAndDetachesHandlers() throws Throwable {
        runOnFx(() -> {
            M3Switch toggle = new M3Switch(false);
            StackPane holder = new StackPane(toggle);
            new Scene(holder);
            toggle.applyCss();
            M3Switch.SwitchSkin skin = new M3Switch.SwitchSkin(toggle);
            toggle.setSkin(skin);
            skin.dispose();
            skin.dispose(); // must be idempotent

            boolean before = toggle.isSelected();
            toggle.fireEvent(click());
            assertEquals(before, toggle.isSelected(), "a disposed skin must not toggle on click");
            toggle.fireEvent(keyPress(KeyCode.SPACE));
            assertEquals(before, toggle.isSelected(), "a disposed skin must not toggle on Space");
        });
    }

    private static MouseEvent click() {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 10, 10, 10, 10, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null);
    }

    private static KeyEvent keyPress(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }
}
