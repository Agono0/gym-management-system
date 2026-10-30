package io.m3fx.controls.tabs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class M3TabsUxTest {

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

    private static void press(M3Tabs tabs, KeyCode code) {
        tabs.fireEvent(new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }

    @Test
    void arrowsWrapAndHomeEndJump() throws Throwable {
        runOnFx(() -> {
            M3Tabs tabs = new M3Tabs(
                    TabVariant.PRIMARY,
                    new Tab("One"), new Tab("Two"), new Tab("Three"));
            new Scene(new StackPane(tabs), 300, 200);

            tabs.getSelectionModel().select(2);
            press(tabs, KeyCode.RIGHT);
            assertEquals(0, tabs.getSelectionModel().getSelectedIndex(), "Right wraps past the last tab");

            tabs.getSelectionModel().select(0);
            press(tabs, KeyCode.LEFT);
            assertEquals(2, tabs.getSelectionModel().getSelectedIndex(), "Left wraps past the first tab");

            press(tabs, KeyCode.HOME);
            assertEquals(0, tabs.getSelectionModel().getSelectedIndex(), "Home jumps to the first tab");

            press(tabs, KeyCode.END);
            assertEquals(2, tabs.getSelectionModel().getSelectedIndex(), "End jumps to the last tab");
        });
    }

    @Test
    void wrappingSkipsDisabledTabs() throws Throwable {
        runOnFx(() -> {
            Tab first = new Tab("One");
            first.setDisable(true);
            M3Tabs tabs = new M3Tabs(TabVariant.PRIMARY, first, new Tab("Two"), new Tab("Three"));
            new Scene(new StackPane(tabs), 300, 200);

            tabs.getSelectionModel().select(2);
            press(tabs, KeyCode.RIGHT);
            assertEquals(1, tabs.getSelectionModel().getSelectedIndex(), "Wrap skips the disabled tab");
        });
    }
}
