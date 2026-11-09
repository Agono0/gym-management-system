package com.gym.screen;

import com.gym.controller.AppShellController;
import com.gym.controller.Screen;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
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
                AppShellController controller = new AppShellController();
                Parent shellRoot = controller.getRoot();

                Stage stage = new Stage();
                Scene scene = new Scene(shellRoot, 1280, 720);
                stage.setScene(scene);
                stage.show();

                shellRoot.applyCss();
                shellRoot.layout();

                StackPane contentArea = (StackPane) shellRoot.lookup("#contentArea");
                assertNotNull(contentArea, "Shell must expose the #contentArea host");

                List<Screen> allScreens = List.of(
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
                        Screen.STAFF
                );

                System.out.println("=== ALL SCREENS VIEWPORT MEASUREMENTS (Scene H: 720, ContentArea H: " + contentArea.getHeight() + ") ===");
                Parent previousView = null;
                for (Screen screen : allScreens) {
                    controller.loadScreen(screen);
                    shellRoot.applyCss();
                    shellRoot.layout();

                    Parent currentView = (Parent) contentArea.getChildren().get(0);
                    // Every listed screen must load: loadScreen() swallows build errors, so a
                    // failed build would leave the previous screen instance in place.
                    assertNotNull(currentView, "Root of " + screen.key() + " must be loaded into #contentArea");
                    assertNotSame(previousView, currentView,
                            "loadScreen(" + screen.key() + ") must replace the previous screen with a fresh root");
                    previousView = currentView;
                    assertTrue(currentView.isVisible(), "Root of " + screen.key() + " must be visible after load");

                    double viewWidth = currentView instanceof Region region
                            ? region.getWidth()
                            : currentView.getBoundsInLocal().getWidth();
                    assertFiniteNonNegative(viewWidth, screen.key() + " view width");

                    if (currentView instanceof ScrollPane sp) {
                        Region content = (Region) sp.getContent();
                        double viewportH = sp.getViewportBounds().getHeight();
                        double contentH = content == null ? 0 : content.getHeight();
                        assertFiniteNonNegative(viewportH, screen.key() + " viewport height");
                        assertFiniteNonNegative(contentH, screen.key() + " content height");
                        System.out.println(String.format("Screen: %-22s | [OUTER SCROLLPANE] Viewport H: %6.1f | Content H: %6.1f | Can Scroll: %s",
                                screen.key(), viewportH, contentH, (contentH > viewportH)));
                    } else {
                        // View has internal layout or inner scrollpane
                        ScrollPane innerSp = findScrollPane(currentView);
                        double totalH = currentView instanceof Region region ? region.getHeight() : -1;
                        if (innerSp != null) {
                            Region innerContent = (Region) innerSp.getContent();
                            double innerH = innerContent != null ? innerContent.getHeight() : -1;
                            double viewportH = innerSp.getViewportBounds().getHeight();
                            assertFiniteNonNegative(viewportH, screen.key() + " inner viewport height");
                            if (innerContent != null) {
                                assertFiniteNonNegative(innerH, screen.key() + " inner content height");
                            }
                            if (currentView instanceof Region) {
                                assertFiniteNonNegative(totalH, screen.key() + " total view height");
                            }
                            System.out.println(String.format("Screen: %-22s | [INNER SCROLLPANE] Viewport H: %6.1f | Content H: %6.1f | Total View H: %6.1f",
                                    screen.key(), viewportH, innerH, totalH));
                        } else {
                            if (currentView instanceof Region) {
                                assertFiniteNonNegative(totalH, screen.key() + " total view height");
                            }
                            System.out.println(String.format("Screen: %-22s | [NO SCROLLPANE] Total View H: %6.1f",
                                    screen.key(), totalH));
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

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Timed out measuring screens on the JavaFX thread");
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
