package com.gym.fxml;

import com.gym.controller.AppShellController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AllScreensScrollHeightTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
        Platform.setImplicitExit(false);
    }

    @Test
    void testAllScreensScrollability() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                URL shellUrl = getClass().getResource("/fxml/app-shell.fxml");
                FXMLLoader loader = new FXMLLoader(shellUrl);
                Parent shellRoot = loader.load();
                AppShellController controller = loader.getController();

                Stage stage = new Stage();
                Scene scene = new Scene(shellRoot, 1280, 720);
                stage.setScene(scene);
                stage.show();

                shellRoot.applyCss();
                shellRoot.layout();

                StackPane contentArea = (StackPane) shellRoot.lookup("#contentArea");

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
                        "staff.fxml"
                };

                System.out.println("=== ALL SCREENS VIEWPORT MEASUREMENTS (Scene H: 720, ContentArea H: " + contentArea.getHeight() + ") ===");
                for (String screen : allScreens) {
                    controller.loadScreen(screen);
                    shellRoot.applyCss();
                    shellRoot.layout();

                    Parent currentView = (Parent) contentArea.getChildren().get(0);
                    if (currentView instanceof ScrollPane sp) {
                        Region content = (Region) sp.getContent();
                        System.out.println(String.format("Screen: %-22s | [OUTER SCROLLPANE] Viewport H: %6.1f | Content H: %6.1f | Can Scroll: %s",
                                screen, sp.getViewportBounds().getHeight(), content.getHeight(), (content.getHeight() > sp.getViewportBounds().getHeight())));
                    } else {
                        // View has internal layout or inner scrollpane
                        ScrollPane innerSp = findScrollPane(currentView);
                        if (innerSp != null) {
                            Region innerContent = (Region) innerSp.getContent();
                            double innerH = innerContent != null ? innerContent.getHeight() : -1;
                            System.out.println(String.format("Screen: %-22s | [INNER SCROLLPANE] Viewport H: %6.1f | Content H: %6.1f | Total View H: %6.1f",
                                    screen, innerSp.getViewportBounds().getHeight(), innerH, ((Region) currentView).getHeight()));
                        } else {
                            System.out.println(String.format("Screen: %-22s | [NO SCROLLPANE] Total View H: %6.1f",
                                    screen, ((Region) currentView).getHeight()));
                        }
                    }
                }

                stage.hide();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        if (err.get() != null) {
            err.get().printStackTrace();
            throw new AssertionError(err.get());
        }
    }

    private ScrollPane findScrollPane(javafx.scene.Node node) {
        if (node instanceof ScrollPane sp) return sp;
        if (node instanceof Parent p) {
            for (javafx.scene.Node child : p.getChildrenUnmodifiable()) {
                ScrollPane found = findScrollPane(child);
                if (found != null) return found;
            }
        }
        return null;
    }
}

