package com.gym.fxml;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
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

public class ScrollPaneBehaviorTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
        Platform.setImplicitExit(false);
    }

    @Test
    void testScrollPaneMeasurements() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                // 1. Load app-shell.fxml
                URL shellUrl = getClass().getResource("/fxml/app-shell.fxml");
                Parent shellRoot = new FXMLLoader(shellUrl).load();

                // 2. Load dashboard.fxml
                URL dashUrl = getClass().getResource("/fxml/dashboard.fxml");
                Parent dashRoot = new FXMLLoader(dashUrl).load();

                // Wrap dashboard in ScrollPane as AppShellController does
                ScrollPane sp = new ScrollPane(dashRoot);
                sp.setFitToWidth(true);
                sp.setFitToHeight(false);
                com.gym.controller.AppShellController.enableSmoothScrolling(sp);

                StackPane contentArea = (StackPane) shellRoot.lookup("#contentArea");
                contentArea.getChildren().setAll(sp);

                Stage stage = new Stage();
                Scene scene = new Scene(shellRoot, 1280, 720);
                stage.setScene(scene);
                stage.show();

                shellRoot.applyCss();
                shellRoot.layout();

                System.out.println("=== SCROLL DIAGNOSTICS ===");
                System.out.println("Scene height: " + scene.getHeight());
                System.out.println("ShellRoot height: " + ((Region) shellRoot).getHeight());

                // Inspect Navigation Rail ScrollPane
                ScrollPane railScroll = (ScrollPane) shellRoot.lookup(".rail-scroll-pane");
                System.out.println("Rail ScrollPane height: " + railScroll.getHeight());
                System.out.println("Rail ViewportBounds: " + railScroll.getViewportBounds());
                Region railContent = (Region) railScroll.getContent();
                System.out.println("Rail Content height: " + railContent.getHeight() + " (prefHeight: " + railContent.prefHeight(railScroll.getWidth()) + ")");
                System.out.println("Rail Scroll vmin: " + railScroll.getVmin() + " vmax: " + railScroll.getVmax() + " vvalue: " + railScroll.getVvalue());

                // Inspect Main Dashboard ScrollPane
                System.out.println("Dashboard ScrollPane height: " + sp.getHeight());
                System.out.println("Dashboard ViewportBounds: " + sp.getViewportBounds());
                Region dashRegion = (Region) dashRoot;
                System.out.println("Dashboard Content height: " + dashRegion.getHeight() + " (prefHeight: " + dashRegion.prefHeight(sp.getWidth()) + ")");
                System.out.println("Dashboard Scroll vmin: " + sp.getVmin() + " vmax: " + sp.getVmax() + " vvalue: " + sp.getVvalue());

                // Test firing scroll event
                ScrollEvent scrollEvent = new ScrollEvent(
                        ScrollEvent.SCROLL,
                        100, 100, 100, 100,
                        false, false, false, false,
                        true, false,
                        0, -40, 0, -40,
                        ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                        ScrollEvent.VerticalTextScrollUnits.LINES, -3,
                        1, null
                );
                sp.fireEvent(scrollEvent);
                System.out.println("After scroll event, Dashboard vvalue: " + sp.getVvalue());

                stage.hide();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        latch.await(5, TimeUnit.SECONDS);
        if (err.get() != null) {
            err.get().printStackTrace();
            throw new AssertionError(err.get());
        }
    }
}
