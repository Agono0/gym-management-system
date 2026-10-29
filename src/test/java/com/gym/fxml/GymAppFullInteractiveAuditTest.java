package com.gym.fxml;

import com.gym.GymApp;
import com.gym.controller.AppShellController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
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

                AppShellController controller = (AppShellController) root.getProperties().get("appShellController");
                if (controller == null) {
                    // Look through FXMLLoader in GymApp or locate controller
                    URL url = getClass().getResource("/fxml/app-shell.fxml");
                    FXMLLoader loader = new FXMLLoader(url);
                    Parent shell = loader.load();
                    controller = loader.getController();
                }

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

                // Test every FXML screen in existence
                String[] allScreens = {
                        "dashboard.fxml",
                        "customer-list.fxml",
                        "customer-profile.fxml",
                        "trainer-list.fxml",
                        "trainer-profile.fxml",
                        "schedule.fxml",
                        "availability.fxml",
                        "membership.fxml",
                        "equipment.fxml",
                        "attendance.fxml",
                        "payment.fxml",
                        "progress.fxml",
                        "medical-profile.fxml",
                        "workout.fxml",
                        "live-workout-hud.fxml",
                        "gym-floor-map.fxml",
                        "staff.fxml",
                        "login.fxml"
                };

                for (String screen : allScreens) {
                    controller.loadScreen(screen);
                    scene.getRoot().applyCss();
                    scene.getRoot().layout();
                    loadedScreens.add(screen);
                }

                stage.hide();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS), "Timed out running interactive audit");
        if (err.get() != null) {
            err.get().printStackTrace();
            throw new AssertionError("Interactive audit failed on screen: " + loadedScreens, err.get());
        }
    }
}

