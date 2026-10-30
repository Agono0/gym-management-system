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
