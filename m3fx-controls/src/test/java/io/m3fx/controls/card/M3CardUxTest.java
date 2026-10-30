package io.m3fx.controls.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class M3CardUxTest {

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
    void clickableCardAnnouncesAndActsLikeButton() throws Throwable {
        runOnFx(() -> {
            javafx.scene.layout.StackPane holder = new javafx.scene.layout.StackPane();
            M3Card card = new M3Card(CardVariant.ELEVATED);
            holder.getChildren().add(card);
            new Scene(holder);
            holder.applyCss();
            AtomicInteger actions = new AtomicInteger();
            card.addEventHandler(javafx.event.ActionEvent.ACTION, e -> actions.incrementAndGet());

            card.setClickable(true);
            assertEquals(AccessibleRole.BUTTON, card.getAccessibleRole(), "Clickable card is a button");
            assertTrue(card.isFocusTraversable(), "Clickable card is keyboard reachable");

            card.fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE, false, false, false, false));
            card.fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
            assertEquals(2, actions.get(), "Space and Enter activate a clickable card");

            card.setClickable(false);
            assertEquals(AccessibleRole.NODE, card.getAccessibleRole(), "Static card stays a node");
            card.fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE, false, false, false, false));
            assertEquals(2, actions.get(), "Static card ignores keyboard activation");
        });
    }
}
