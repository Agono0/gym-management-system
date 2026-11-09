package com.gym.screen;

import com.gym.GymApp;
import com.gym.controller.AppShellController;
import com.gym.controller.Screen;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GymAppFullInteractiveAuditTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
        Platform.setImplicitExit(false);
    }

    @Test
    void testAllScreensAndInteractionsInRealStage() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        List<String> loadedScreens = new ArrayList<>();

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                GymApp app = new GymApp();
                app.start(stage);

                Scene scene = stage.getScene();
                assertNotNull(scene);
                Parent root = scene.getRoot();
                assertNotNull(root);

                // The shell publishes itself in the scene-root properties once attached.
                AppShellController controller =
                        (AppShellController) root.getProperties().get(AppShellController.SHELL_CONTROLLER_KEY);
                assertNotNull(controller, "the running GymApp must publish its shell controller");

                // Test Shell Operations
                controller.onToggleRail();
                scene.getRoot().applyCss();
                scene.getRoot().layout();

                controller.onToggleRail();
                scene.getRoot().applyCss();
                scene.getRoot().layout();

                controller.onToggleThemeMode();
                scene.getRoot().applyCss();
                scene.getRoot().layout();

                controller.onOpenCommandPalette();
                controller.onFastCheckIn();

                // Test every screen in the registry
                Screen[] allScreens = {
                        Screen.DASHBOARD,
                        Screen.CUSTOMER_LIST,
                        Screen.CUSTOMER_PROFILE,
                        Screen.TRAINER_LIST,
                        Screen.TRAINER_PROFILE,
                        Screen.SCHEDULE,
                        Screen.AVAILABILITY,
                        Screen.MEMBERSHIP,
                        Screen.EQUIPMENT,
                        Screen.ATTENDANCE,
                        Screen.PAYMENT,
                        Screen.PROGRESS,
                        Screen.MEDICAL_PROFILE,
                        Screen.WORKOUT,
                        Screen.LIVE_WORKOUT_HUD,
                        Screen.GYM_FLOOR_MAP,
                        Screen.STAFF,
                        Screen.LOGIN
                };

                for (Screen screen : allScreens) {
                    controller.loadScreen(screen);
                    scene.getRoot().applyCss();
                    scene.getRoot().layout();
                    loadedScreens.add(screen.key());
                }

                stage.hide();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Timed out running interactive audit");
        if (err.get() != null) {
            err.get().printStackTrace();
            throw new AssertionError("Interactive audit failed on screen: " + loadedScreens, err.get());
        }
    }
}
