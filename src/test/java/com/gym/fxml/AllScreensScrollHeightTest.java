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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                assertNotNull(contentArea, "Shell must expose the #contentArea host");

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
                Parent previousView = null;
                for (String screen : allScreens) {
                    controller.loadScreen(screen);
                    shellRoot.applyCss();
                    shellRoot.layout();

                    Parent currentView = (Parent) contentArea.getChildren().get(0);
                    // Every listed screen must load: loadScreen() swallows loader errors, so a
                    // failed load would leave the previous screen instance in place.
                    assertNotNull(currentView, "Root of " + screen + " must be loaded into #contentArea");
                    assertNotSame(previousView, currentView,
                            "loadScreen(\"" + screen + "\") must replace the previous screen with a fresh root");
                    previousView = currentView;
                    assertTrue(currentView.isVisible(), "Root of " + screen + " must be visible after load");

                    double viewWidth = currentView instanceof Region region
                            ? region.getWidth()
                            : currentView.getBoundsInLocal().getWidth();
                    assertFiniteNonNegative(viewWidth, screen + " view width");

                    if (currentView instanceof ScrollPane sp) {
                        Region content = (Region) sp.getContent();
                        double viewportH = sp.getViewportBounds().getHeight();
                        double contentH = content == null ? 0 : content.getHeight();
                        assertFiniteNonNegative(viewportH, screen + " viewport height");
                        assertFiniteNonNegative(contentH, screen + " content height");
                        System.out.println(String.format("Screen: %-22s | [OUTER SCROLLPANE] Viewport H: %6.1f | Content H: %6.1f | Can Scroll: %s",
                                screen, viewportH, contentH, (contentH > viewportH)));
                    } else {
                        // View has internal layout or inner scrollpane
                        ScrollPane innerSp = findScrollPane(currentView);
                        double totalH = currentView instanceof Region region ? region.getHeight() : -1;
                        if (innerSp != null) {
                            Region innerContent = (Region) innerSp.getContent();
                            double innerH = innerContent != null ? innerContent.getHeight() : -1;
                            double viewportH = innerSp.getViewportBounds().getHeight();
                            assertFiniteNonNegative(viewportH, screen + " inner viewport height");
                            if (innerContent != null) {
                                assertFiniteNonNegative(innerH, screen + " inner content height");
                            }
                            if (currentView instanceof Region) {
                                assertFiniteNonNegative(totalH, screen + " total view height");
                            }
                            System.out.println(String.format("Screen: %-22s | [INNER SCROLLPANE] Viewport H: %6.1f | Content H: %6.1f | Total View H: %6.1f",
                                    screen, viewportH, innerH, totalH));
                        } else {
                            if (currentView instanceof Region) {
                                assertFiniteNonNegative(totalH, screen + " total view height");
                            }
                            System.out.println(String.format("Screen: %-22s | [NO SCROLLPANE] Total View H: %6.1f",
                                    screen, totalH));
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

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out measuring screens on the JavaFX thread");
        if (err.get() != null) {
            err.get().printStackTrace();
            throw new AssertionError(err.get());
        }
    }

    /** A measured width/height must be a real, non-negative number: never NaN, infinite or negative. */
    private static void assertFiniteNonNegative(double value, String what) {
        assertTrue(Double.isFinite(value) && value >= 0,
                what + " must be finite and non-negative but was " + value);
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

