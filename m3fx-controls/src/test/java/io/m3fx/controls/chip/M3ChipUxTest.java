package io.m3fx.controls.chip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class M3ChipUxTest {

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
    void filterChipAnnouncesSelectedState() {
        M3Chip filter = M3Chip.filter("Cardio");
        assertFalse(filter.getAccessibleText().contains("selected"), "Unselected filter chip");

        filter.setSelected(true);
        assertTrue(filter.getAccessibleText().contains("selected"), "Selected filter chip announces it");

        M3Chip assist = M3Chip.assist("Plain");
        assist.setSelected(true);
        assertFalse(assist.getAccessibleText().contains("selected"), "Only filter chips announce selection");
    }

    @Test
    void closableChipAnnouncesKeyboardRemoval() {
        M3Chip input = M3Chip.input("Tag");
        assertTrue(input.getAccessibleText().contains("press Delete to remove"));

        M3Chip plain = M3Chip.assist("Keep");
        assertFalse(plain.getAccessibleText().contains("press Delete to remove"));
    }

    @Test
    void spaceAndEnterFireActionPerVariant() throws Throwable {
        runOnFx(() -> {
            // Assist chips fire ACTION without staying selected (M3 keyboard table:
            // "Space or Enter activates, selects, or deselects the focused chip").
            M3Chip assist = M3Chip.assist("Add");
            AtomicInteger fired = new AtomicInteger();
            assist.setOnAction(e -> fired.incrementAndGet());
            assist.fireEvent(keyPressed(KeyCode.SPACE));
            assertEquals(1, fired.get(), "Space fires the action");
            assertFalse(assist.isSelected(), "assist chips do not stay selected");
            assist.fireEvent(keyPressed(KeyCode.ENTER));
            assertEquals(2, fired.get(), "Enter fires the action too");

            // Suggestion chips behave like assist chips.
            M3Chip suggestion = M3Chip.suggestion("Try");
            suggestion.setOnAction(e -> fired.incrementAndGet());
            suggestion.fireEvent(keyPressed(KeyCode.SPACE));
            assertEquals(3, fired.get(), "suggestion chips fire the action");
            assertFalse(suggestion.isSelected());
        });
    }

    @Test
    void spaceTogglesFilterChips() throws Throwable {
        runOnFx(() -> {
            M3Chip filter = M3Chip.filter("Cardio");
            AtomicInteger fired = new AtomicInteger();
            filter.setOnAction(e -> fired.incrementAndGet());
            assertFalse(filter.isSelected());

            filter.fireEvent(keyPressed(KeyCode.SPACE));
            assertTrue(filter.isSelected(), "Space selects a filter chip");
            assertEquals(1, fired.get(), "toggling fires the action");
            assertEquals("Cardio, selected", filter.getAccessibleText(),
                    "the selection announcement follows the keyboard toggle");

            filter.fireEvent(keyPressed(KeyCode.ENTER));
            assertFalse(filter.isSelected(), "Enter deselects a selected filter chip");
            assertEquals(2, fired.get());
            assertFalse(filter.getAccessibleText().contains("selected"));
        });
    }

    @Test
    void deleteKeyRunsOnCloseForClosableChips() throws Throwable {
        runOnFx(() -> {
            M3Chip chip = M3Chip.input("Tag");
            AtomicInteger closes = new AtomicInteger();
            chip.setOnClose(closes::incrementAndGet);

            chip.fireEvent(keyPressed(KeyCode.DELETE));
            assertEquals(1, closes.get(), "Delete removes a closable chip");
            chip.fireEvent(keyPressed(KeyCode.BACK_SPACE));
            assertEquals(2, closes.get(), "Backspace also removes a closable chip");

            M3Chip locked = M3Chip.assist("Keep");
            locked.setOnClose(closes::incrementAndGet);
            locked.fireEvent(keyPressed(KeyCode.DELETE));
            assertEquals(2, closes.get(), "Non-closable chips ignore Delete");
        });
    }

    @Test
    void accessibleTextFollowsLabel() {
        M3Chip chip = M3Chip.assist("Before");
        chip.setText("After");
        assertEquals("After", chip.getAccessibleText());
    }
}
