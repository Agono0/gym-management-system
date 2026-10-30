package io.m3fx.controls.slider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class M3SliderUxTest {

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

    private static void press(M3Slider slider, KeyCode code, boolean shift) {
        slider.fireEvent(new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", code, shift, false, false, false));
    }

    @Test
    void accessibleTextTracksValue() {
        M3Slider slider = new M3Slider(0, 100, 50);
        assertEquals("Value: 50", slider.getAccessibleText());

        slider.setValue(42.5);
        assertEquals("Value: 42.5", slider.getAccessibleText());

        slider.setAccessibleLabel("Volume");
        assertEquals("Volume: 42.5", slider.getAccessibleText());
    }

    @Test
    void shiftArrowsStepFinelyAndClamp() throws Throwable {
        runOnFx(() -> {
            M3Slider slider = new M3Slider(0, 100, 50);
            new Scene(new StackPane(slider), 200, 60);

            press(slider, KeyCode.RIGHT, true);
            assertEquals(51.0, slider.getValue(), 1e-9, "Shift+Right steps a tenth of the block");

            press(slider, KeyCode.LEFT, true);
            press(slider, KeyCode.LEFT, true);
            assertEquals(49.0, slider.getValue(), 1e-9, "Shift+Left steps back");

            slider.setValue(100);
            press(slider, KeyCode.RIGHT, true);
            assertEquals(100.0, slider.getValue(), 1e-9, "Fine step clamps to max");

            slider.setValue(0);
            press(slider, KeyCode.LEFT, true);
            assertEquals(0.0, slider.getValue(), 1e-9, "Fine step clamps to min");

            assertEquals("Value: 0", slider.getAccessibleText(), "Announced value follows");
        });
    }
}
