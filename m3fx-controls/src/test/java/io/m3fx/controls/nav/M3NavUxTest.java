package io.m3fx.controls.nav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class M3NavUxTest {

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

    private static void press(javafx.scene.Node node, KeyCode code) {
        node.fireEvent(new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }

    private static List<M3NavDestination> threeDestinations() {
        return List.of(
                M3NavDestination.of("Home", "home"),
                M3NavDestination.of("Schedule", "calendar_month"),
                M3NavDestination.of("Profile", "person"));
    }

    @Test
    void barArrowsMoveSelectionAndFocus() throws Throwable {
        runOnFx(() -> {
            M3NavigationBar bar = new M3NavigationBar(threeDestinations());
            StackPane holder = new StackPane(bar);
            Scene scene = new Scene(holder, 480, 120);
            holder.applyCss();

            ToggleButton second = (ToggleButton) bar.getChildren().get(1);
            press(bar, KeyCode.RIGHT);
            assertEquals(1, bar.getSelectedIndex(), "Right moves to the next destination");
            assertSame(second, scene.getFocusOwner(), "Focus follows the selection");

            press(bar, KeyCode.RIGHT);
            assertEquals(2, bar.getSelectedIndex());

            press(bar, KeyCode.RIGHT);
            assertEquals(0, bar.getSelectedIndex(), "Right wraps past the last destination");

            press(bar, KeyCode.END);
            assertEquals(2, bar.getSelectedIndex(), "End jumps to the last destination");
        });
    }

    @Test
    void railArrowsMoveSelection() throws Throwable {
        runOnFx(() -> {
            M3NavigationRail rail = new M3NavigationRail(threeDestinations());
            new Scene(new StackPane(rail), 120, 320);

            rail.setSelectedIndex(2);
            press(rail, KeyCode.DOWN);
            assertEquals(0, rail.getSelectedIndex(), "Down wraps past the last destination");

            press(rail, KeyCode.UP);
            assertEquals(2, rail.getSelectedIndex(), "Up moves back to the previous destination");
        });
    }

    @Test
    void drawerArrowsMoveSelectionAndCloseModal() throws Throwable {
        runOnFx(() -> {
            M3NavigationDrawer drawer = new M3NavigationDrawer(threeDestinations(), true);
            StackPane holder = new StackPane(drawer);
            new Scene(holder, 480, 320);
            drawer.setOpen(true);

            press(drawer, KeyCode.DOWN);
            assertEquals(1, drawer.getSelectedIndex(), "Down moves to the next destination");
            assertFalse(drawer.isOpen(), "Keyboard activation closes a modal drawer like a click");
        });
    }
}
