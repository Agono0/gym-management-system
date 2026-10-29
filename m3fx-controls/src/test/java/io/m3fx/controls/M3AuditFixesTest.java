package io.m3fx.controls;

import static org.junit.jupiter.api.Assertions.*;

import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.internal.OverlayLayer;
import io.m3fx.controls.search.M3SearchBar;
import io.m3fx.controls.shape.M3ShapeMorph;
import io.m3fx.controls.shape.M3ShapeMorph.ShapeType;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Verification tests for the audit bug fixes:
 * 1. M3SearchBar suggestions preservation during typing/filtering
 * 2. OverlayLayer partitioning (transient dismiss does not close active modal dialogs)
 * 3. M3Chip on-close callback and closable state
 * 4. M3ShapeMorph canvas rendering without PathElement NPE
 */
class M3AuditFixesTest {

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Already started
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
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    @Test
    void searchBarPreservesSuggestionsAcrossQueries() throws Throwable {
        runOnFx(() -> {
            M3SearchBar searchBar = new M3SearchBar("Search gym members...");
            searchBar.getSuggestions().addAll("Alice Smith", "Bob Jones", "Charlie Brown");
            assertEquals(3, searchBar.getSuggestions().size());

            // Set query
            searchBar.setQuery("Alice");
            assertEquals(3, searchBar.getSuggestions().size(), "Master suggestions should not be mutated by filtering");

            searchBar.setQuery("");
            assertEquals(3, searchBar.getSuggestions().size(), "Master suggestions retained after clearing query");
        });
    }

    @Test
    void overlayLayerIsolatesTransientAndModal() throws Throwable {
        runOnFx(() -> {
            OverlayLayer layer = new OverlayLayer();
            Label modalDialog = new Label("Modal Dialog");
            Label snackbar = new Label("Transient Toast");

            layer.showModal(modalDialog);
            assertTrue(layer.isShowing());

            layer.showTransient(snackbar);
            assertTrue(layer.isShowing());

            // Dismissing transient layer should NOT dismiss the modal dialog
            layer.hideTransient();
            assertTrue(layer.isShowing(), "Modal dialog must remain open when transient snackbar dismisses");

            layer.hideModal();
            assertFalse(layer.isShowing(), "Overlay layer should be hidden when modal is dismissed");
        });
    }

    @Test
    void chipClosableAndOnCloseFires() throws Throwable {
        runOnFx(() -> {
            M3Chip chip = new M3Chip("Tag", ChipVariant.INPUT);
            assertTrue(chip.isClosable());

            AtomicBoolean closed = new AtomicBoolean(false);
            chip.setOnClose(() -> closed.set(true));

            assertNotNull(chip.getOnClose());
            chip.getOnClose().run();
            assertTrue(closed.get(), "onClose callback should execute");
        });
    }

    @Test
    void shapeMorphRendersAllPresetsWithoutNpe() throws Throwable {
        runOnFx(() -> {
            M3ShapeMorph morph = new M3ShapeMorph(ShapeType.ROUNDED_RECTANGLE);
            morph.resize(100, 100);

            for (ShapeType type : ShapeType.values()) {
                assertDoesNotThrow(() -> morph.morphTo(type), "Morphing to " + type + " must not throw");
            }
        });
    }
}

