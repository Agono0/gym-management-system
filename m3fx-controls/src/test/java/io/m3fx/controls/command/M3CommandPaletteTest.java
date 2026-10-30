package io.m3fx.controls.command;

import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * Unit tests for Global Spotlight Command Palette.
 */
class M3CommandPaletteTest {

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

    /** Polls an FX-thread condition until it holds or the timeout elapses. */
    private static void awaitFx(BooleanSupplier condition, String message)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        AtomicBoolean satisfied = new AtomicBoolean(false);
        while (System.currentTimeMillis() < deadline) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                satisfied.set(condition.getAsBoolean());
                latch.countDown();
            });
            assertTrue(latch.await(5, TimeUnit.SECONDS), "FX poll timed out");
            if (satisfied.get()) {
                return;
            }
            Thread.sleep(50);
        }
        fail(message);
    }

    private static KeyEvent keyPress(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, KeyEvent.CHAR_UNDEFINED, "", code,
                false, false, false, false);
    }

    /** Results container: overlay children are [scrim, card]; card children [searchRow, scroll]. */
    private static VBox resultsOf(M3CommandPalette palette) {
        VBox card = (VBox) palette.getChildren().get(1);
        ScrollPane scroll = (ScrollPane) card.getChildren().get(1);
        return (VBox) scroll.getContent();
    }

    /** Zero-based index of the row carrying the "selected" style class, or -1. */
    private static int selectedRowIndex(M3CommandPalette palette) {
        int index = -1;
        int rowCount = 0;
        for (Node child : resultsOf(palette).getChildren()) {
            if (child.getStyleClass().contains("m3-command-palette-cat-header")) {
                continue; // category headers are not selectable rows
            }
            if (child.getStyleClass().contains("selected")) {
                index = rowCount;
            }
            rowCount++;
        }
        return index;
    }

    @Test
    void paletteRegistrationAndToggle() throws Throwable {
        runOnFx(() -> {
            M3CommandPalette palette = new M3CommandPalette();
            assertFalse(palette.isOpen());

            AtomicBoolean executed = new AtomicBoolean(false);
            palette.registerAction("Workouts", "Start Session", "Live Telemetry", "fitness", () -> executed.set(true));

            assertEquals(1, palette.getMasterActions().size());

            palette.show();
            assertTrue(palette.isOpen());

            palette.close();
            assertFalse(palette.isOpen());

            palette.toggle();
            assertTrue(palette.isOpen());
        });
    }

    @Test
    void paletteSelectionResetsOnFilterChangeAndSupportsHomeEnd() throws Throwable {
        runOnFx(() -> {
            M3CommandPalette palette = new M3CommandPalette();
            palette.registerAction("Members", "Add Member", "Register a new member", "person", () -> { });
            palette.registerAction("Members", "Export List", "CSV export", "download", () -> { });
            palette.registerAction("Billing", "Charge Card", "Manual charge", "payment", () -> { });
            palette.registerAction("Billing", "Refund", "Reverse a charge", "undo", () -> { });
            palette.registerAction("Billing", "Invoice", "Print invoice", "print", () -> { });

            palette.show();
            assertTrue(palette.isOpen());
            assertEquals(0, selectedRowIndex(palette), "Selection starts at the first row");

            TextField search = palette.getSearchField();
            search.fireEvent(keyPress(KeyCode.DOWN));
            search.fireEvent(keyPress(KeyCode.DOWN));
            assertEquals(2, selectedRowIndex(palette), "Arrow keys move the selection");

            // A new filter resets the selection to the top of the fresh result list.
            search.setText("refund");
            assertEquals(0, selectedRowIndex(palette), "Filter change resets selection to 0");
            assertEquals(1, selectableRowCount(palette), "Only matching rows show");

            search.fireEvent(keyPress(KeyCode.END));
            assertEquals(0, selectedRowIndex(palette), "END clamps to the single result");

            search.setText("");
            assertEquals(0, selectedRowIndex(palette), "Clearing the filter resets selection");
            search.fireEvent(keyPress(KeyCode.END));
            assertEquals(4, selectedRowIndex(palette), "END jumps to the last row");
            search.fireEvent(keyPress(KeyCode.HOME));
            assertEquals(0, selectedRowIndex(palette), "HOME jumps to the first row");
        });
    }

    private static int selectableRowCount(M3CommandPalette palette) {
        int count = 0;
        for (Node child : resultsOf(palette).getChildren()) {
            if (!child.getStyleClass().contains("m3-command-palette-cat-header")) {
                count++;
            }
        }
        return count;
    }

    @Test
    void paletteEnterExecutesSelectedCommandAndCloses() throws Throwable {
        runOnFx(() -> {
            M3CommandPalette palette = new M3CommandPalette();
            AtomicReference<String> ran = new AtomicReference<>();
            palette.registerAction("Members", "Add Member", "Register a new member", "person", () -> ran.set("Add Member"));
            palette.registerAction("Billing", "Refund", "Reverse a charge", "undo", () -> ran.set("Refund"));

            palette.show();
            TextField search = palette.getSearchField();
            search.setText("refund");
            search.fireEvent(keyPress(KeyCode.ENTER));

            assertEquals("Refund", ran.get(), "Enter runs the filtered selected command");
            assertFalse(palette.isOpen(), "Palette closes after executing");
        });
    }

    @Test
    void paletteOpenCloseAnimationHidesAndRestoresVisibility() throws Throwable {
        AtomicReference<M3CommandPalette> ref = new AtomicReference<>();
        runOnFx(() -> {
            M3CommandPalette palette = new M3CommandPalette();
            palette.registerAction("Members", "Add Member", "Register a new member", "person", () -> { });
            palette.show();
            assertTrue(palette.isVisible(), "Open makes the palette visible");
            ref.set(palette);
        });

        runOnFx(() -> {
            // Close, then immediately re-open before the exit animation can finish: the
            // re-open must cancel the pending exit and keep the palette visible.
            ref.get().close();
            ref.get().show();
            assertTrue(ref.get().isVisible(), "Re-open during the exit keeps the palette visible");
            assertTrue(ref.get().isOpen());
        });

        runOnFx(() -> ref.get().close());
        assertFalse(ref.get().isOpen(), "isOpen flips immediately on close");
        awaitFx(() -> !ref.get().isVisible(),
                "Exit animation must hide the palette after close");
    }
}
