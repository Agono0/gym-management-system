package io.m3fx.controls.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Smoke test for the exported {@link M3CommandPalette} package: the public control constructs
 * with sensible defaults, actions register, and the search field filters to the matching
 * command which ENTER then executes. (Deeper keyboard/selection/animation coverage lives in
 * {@link M3CommandPaletteTest}.)
 */
class M3CommandPaletteSmokeTest {

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX failed to initialize");
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

    private static KeyEvent keyPress(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, KeyEvent.CHAR_UNDEFINED, "", code,
                false, false, false, false);
    }

    @Test
    void paletteConstructsWithSensibleDefaults() throws Throwable {
        runOnFx(() -> {
            M3CommandPalette palette = new M3CommandPalette();
            assertFalse(palette.isOpen(), "a fresh palette starts closed");
            assertTrue(palette.getMasterActions().isEmpty(), "a fresh palette has no actions");
            assertTrue(palette.getStyleClass().contains("m3-command-palette-overlay"),
                    "the overlay carries its style class");
            TextField search = palette.getSearchField();
            assertNotNull(search, "the search field must exist out of the box");
        });
    }

    @Test
    void paletteRegistersActionsAndFilteredSearchExecutesTheMatch() throws Throwable {
        runOnFx(() -> {
            M3CommandPalette palette = new M3CommandPalette();
            AtomicBoolean refunded = new AtomicBoolean(false);
            palette.registerAction("Members", "Add Member", "Register a new member", "person", () -> { });
            palette.registerAction("Billing", "Charge Card", "Manual charge", "payment", () -> { });
            palette.registerAction("Billing", "Refund", "Reverse a charge", "undo", () -> refunded.set(true));

            assertEquals(3, palette.getMasterActions().size(), "all registered actions are kept");

            palette.show();
            assertTrue(palette.isOpen(), "show() opens the palette");

            // Filtered search: only the matching command stays selectable, ENTER executes it
            // and closes the palette.
            palette.getSearchField().setText("refund");
            palette.getSearchField().fireEvent(keyPress(KeyCode.ENTER));
            assertTrue(refunded.get(), "ENTER must execute the filtered command");
            assertFalse(palette.isOpen(), "the palette closes after executing");

            palette.close();
        });
    }
}
