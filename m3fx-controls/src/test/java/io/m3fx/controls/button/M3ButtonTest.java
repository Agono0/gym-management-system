package io.m3fx.controls.button;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class M3ButtonTest {

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
    void factoriesSetVariantStyleClasses() {
        assertTrue(M3Button.filled("A").getStyleClass().contains("m3-button-filled"));
        assertTrue(M3Button.tonal("B").getStyleClass().contains("m3-button-tonal"));
        assertTrue(M3Button.outlined("C").getStyleClass().contains("m3-button-outlined"));
        assertTrue(M3Button.elevated("D").getStyleClass().contains("m3-button-elevated"));
        assertTrue(M3Button.text("E").getStyleClass().contains("m3-button-text"));
    }

    @Test
    void variantSwitchUpdatesStyleClasses() {
        M3Button button = new M3Button("A", ButtonVariant.FILLED);
        button.setVariant(ButtonVariant.TEXT);

        assertFalse(button.getStyleClass().contains("m3-button-filled"), "Old variant class removed");
        assertTrue(button.getStyleClass().contains("m3-button-text"), "New variant class added");
        assertEquals(ButtonVariant.TEXT, button.getVariant());
    }

    @Test
    void specContainerHeightIs40() {
        M3Button button = M3Button.filled("Save");
        assertEquals(40.0, M3Button.MIN_HEIGHT, "M3 button container height is 40dp");
        assertEquals(M3Button.MIN_HEIGHT, button.getMinHeight());
    }

    @Test
    void toggleableSelectionAndActionFire() {
        M3Button button = M3Button.filled("Toggle");
        AtomicInteger actions = new AtomicInteger();
        button.setOnAction(e -> actions.incrementAndGet());

        assertFalse(button.isSelected());
        button.setToggleable(true);
        button.activate();
        assertTrue(button.isSelected(), "activate() toggles a toggleable button");
        assertEquals(1, actions.get(), "activate() fires exactly one action");
        button.activate();
        assertFalse(button.isSelected());
        assertEquals(2, actions.get());
    }

    @Test
    void textAccessibleTextBound() {
        M3Button button = new M3Button("Save", ButtonVariant.FILLED);
        assertEquals("Save", button.getAccessibleText());
        button.setText("Save now");
        assertEquals("Save now", button.getAccessibleText(), "Accessible text follows the label");
    }

    @Test
    void skinDisposesWithoutError() throws Throwable {
        runOnFx(() -> {
            M3Button button = M3Button.filled("Dispose");
            StackPane holder = new StackPane(button);
            new Scene(holder);
            // Force skin creation (dispose() runs when the skin is replaced/removed).
            button.applyCss();
            assertNotNull(button.getSkin(), "Default skin created");

            holder.getChildren().remove(button);
            button.setSkin(null);
            assertNull(button.getSkin(), "Skin detachable without error");
        });
    }
}
