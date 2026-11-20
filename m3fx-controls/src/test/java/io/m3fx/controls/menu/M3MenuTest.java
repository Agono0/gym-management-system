package io.m3fx.controls.menu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Verifies the menu anchoring fix: {@code showBelow} never drops the menu at the screen origin
 * when the anchor has no screen coordinates yet — it defers until the anchor is laid out (and
 * aborts instead of guessing if the anchor never reaches a window).
 */
class M3MenuTest {

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
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
        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    @Test
    void showBelowDefersInsteadOfScreenOriginWhenNotLaidOut() throws Throwable {
        runOnFx(() -> {
            M3Menu menu = new M3Menu();
            menu.item("Action", () -> {});
            // Anchor in a scene but never laid out and without a window: localToScreen is null.
            Label anchor = new Label("Anchor");
            StackPane root = new StackPane(anchor);
            new Scene(root, 200, 100);

            assertDoesNotThrow(() -> menu.showBelow(anchor),
                    "the pre-layout path must not throw");
            // Without a window the deferred show cannot complete - and it must not have shown
            // the popup at the screen origin (the old fallback).
            assertFalse(menu.isShowing(), "no screen-origin fallback");
            menu.hide(); // cancels the deferral and detaches the listeners
            assertFalse(menu.isShowing());
        });
    }

    @Test
    void showBelowWithoutAnySceneAlsoDefersSafely() throws Throwable {
        runOnFx(() -> {
            M3Menu menu = new M3Menu();
            menu.item("Action", () -> {});
            Label anchor = new Label("Detached");
            // Not in a scene at all: localToScreen is null, no window can ever arrive.
            assertDoesNotThrow(() -> menu.showBelow(anchor));
            assertFalse(menu.isShowing(), "no screen-origin fallback");
            menu.hide();
        });
    }
}
