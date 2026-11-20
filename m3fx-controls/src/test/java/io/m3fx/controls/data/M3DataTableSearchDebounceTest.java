package io.m3fx.controls.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * B6: the data table used to run a full {@code filterItems} + {@code rebuildRows} per search
 * keystroke. Typing now debounces (at most one rebuild per {@link M3DataTable#SEARCH_DEBOUNCE_MILLIS}
 * quiet period) while Enter still applies immediately.
 */
class M3DataTableSearchDebounceTest {

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

    private static M3DataTable<String> tableWithRows() {
        M3DataTable<String> table = new M3DataTable<>();
        table.getColumns().add(new M3DataTable.Column<String>("Name", String::toString));
        table.getItems().addAll("Sarah Connor", "Marcus Vance", "Priya Nair");
        return table;
    }

    @Test
    void rapidKeystrokeBurstRebuildsOnce() throws Throwable {
        AtomicReference<M3DataTable<String>> holder = new AtomicReference<>();
        int[] before = new int[1];
        int[] after = new int[1];
        runOnFx(() -> {
            M3DataTable<String> table = tableWithRows();
            holder.set(table);
            new Scene(new StackPane(table)); // attach (no stage needed for the debounce timer)
            before[0] = table.rebuildCountForTesting;
            // A rapid keystroke burst: three query changes in one FX turn.
            table.searchBarForTesting().setQuery("s");
            table.searchBarForTesting().setQuery("sa");
            table.searchBarForTesting().setQuery("sar");
            assertEquals(before[0], table.rebuildCountForTesting,
                    "no rebuild may run synchronously during the burst");
        });
        // The debounced rebuild fires on a later pulse; wait past the 200ms window.
        Thread.sleep(M3DataTable.SEARCH_DEBOUNCE_MILLIS + 250);
        runOnFx(() -> after[0] = holder.get().rebuildCountForTesting);
        assertEquals(before[0] + 1, after[0],
                "a burst must collapse to exactly one rebuild");
    }

    @Test
    void enterAppliesTheQueryImmediately() throws Throwable {
        AtomicReference<M3DataTable<String>> holder = new AtomicReference<>();
        int[] before = new int[1];
        int[] after = new int[1];
        runOnFx(() -> {
            M3DataTable<String> table = tableWithRows();
            holder.set(table);
            new Scene(new StackPane(table));
            before[0] = table.rebuildCountForTesting;
            table.searchBarForTesting().setQuery("marcus"); // schedules the debounced run
            assertEquals(before[0], table.rebuildCountForTesting);
            // Enter (key press on the search bar) applies at once, cancelling the debounce.
            table.searchBarForTesting().fireEvent(new javafx.scene.input.KeyEvent(
                    javafx.scene.input.KeyEvent.KEY_PRESSED, "", "",
                    javafx.scene.input.KeyCode.ENTER, false, false, false, false));
            after[0] = table.rebuildCountForTesting;
            assertEquals(before[0] + 1, after[0], "Enter must apply the query immediately");
            assertEquals(1, table.visibleItemCountForTesting(),
                    "the query filtered the rows right away");
        });
    }

    @Test
    void searchStillFiltersCorrectly() throws Throwable {
        AtomicReference<M3DataTable<String>> holder = new AtomicReference<>();
        int[] before = new int[1];
        int[] after = new int[1];
        runOnFx(() -> {
            M3DataTable<String> table = tableWithRows();
            holder.set(table);
            new Scene(new StackPane(table));
            before[0] = table.rebuildCountForTesting;
            table.searchBarForTesting().setQuery("connor");
        });
        Thread.sleep(M3DataTable.SEARCH_DEBOUNCE_MILLIS + 250);
        runOnFx(() -> {
            after[0] = holder.get().rebuildCountForTesting;
            assertEquals(before[0] + 1, after[0]);
            assertEquals(1, holder.get().visibleItemCountForTesting(),
                    "only Sarah Connor matches");
        });
    }
}
