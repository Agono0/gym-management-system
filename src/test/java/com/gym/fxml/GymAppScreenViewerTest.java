package com.gym.fxml;

import com.gym.GymApp;
import com.gym.MainApp;
import com.gym.controller.AppShellController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the interactive Gym App screen viewer and AppShellController screen swapping.
 */
public class GymAppScreenViewerTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
        Platform.setImplicitExit(false);
    }

    @Test
    void testAppShellControllerLoadsScreensDynamically() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                URL shellUrl = getClass().getResource("/fxml/app-shell.fxml");
                assertNotNull(shellUrl, "Missing /fxml/app-shell.fxml");
                FXMLLoader loader = new FXMLLoader(shellUrl);
                Parent shellRoot = loader.load();
                assertNotNull(shellRoot);

                AppShellController controller = loader.getController();
                assertNotNull(controller, "AppShellController must be injected");

                // Verify Navigation Rail contains a ScrollPane for smooth side menu scrolling
                javafx.scene.Node railScroll = shellRoot.lookup(".rail-scroll-pane");
                assertNotNull(railScroll, "Side navigation items must be wrapped in a ScrollPane");
                assertTrue(railScroll instanceof javafx.scene.control.ScrollPane, "Must be a ScrollPane");
                assertTrue(((javafx.scene.control.ScrollPane) railScroll).isFitToWidth());

                // Test dynamic loading across various modules
                controller.onNavCustomers();
                controller.onNavTrainers();
                controller.onNavSchedule();
                controller.onNavMemberships();
                controller.onNavEquipment();
                controller.onNavAttendance();
                controller.onNavPayments();
                controller.onNavStaff();
                controller.onNavDashboard();

                // Verify loaded dashboard view is inside a ScrollPane with computed height
                javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) shellRoot.lookup("#contentArea");
                assertNotNull(contentArea);
                assertEquals(1, contentArea.getChildren().size());
                javafx.scene.Node contentChild = contentArea.getChildren().get(0);
                assertTrue(contentChild instanceof javafx.scene.control.ScrollPane, "Dashboard must be wrapped in ScrollPane");
                javafx.scene.control.ScrollPane sp = (javafx.scene.control.ScrollPane) contentChild;
                assertTrue(sp.isFitToWidth());
                assertFalse(sp.isFitToHeight(), "FitToHeight must be false to allow vertical scrolling");
                assertTrue(sp.getContent() instanceof javafx.scene.layout.Region);
                assertEquals(javafx.scene.layout.Region.USE_COMPUTED_SIZE, ((javafx.scene.layout.Region) sp.getContent()).getPrefHeight(), 0.001);

                // Load customer-profile (which has an inner ScrollPane) and verify no nested ScrollPane
                controller.loadScreen("customer-profile.fxml");
                javafx.scene.Node profileView = contentArea.getChildren().get(0);
                assertFalse(profileView instanceof javafx.scene.control.ScrollPane, "Screens with inner ScrollPane must not be wrapped in outer ScrollPane");

            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out executing on JavaFX thread");
        if (err.get() != null) {
            throw new AssertionError("Failed loading screens dynamically in AppShellController", err.get());
        }
    }

    @Test
    void testGymAppInstantiatesCleanly() {
        GymApp app = new GymApp();
        assertNotNull(app);
        assertNotNull(GymApp.SEED_PURPLE);
    }

    @Test
    void testSpeedDialAndSnackbarDoNotBlockMouseEvents() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                // Verify M3SpeedDial does not pick on bounds
                io.m3fx.controls.data.M3SpeedDial dial = new io.m3fx.controls.data.M3SpeedDial();
                assertFalse(dial.isPickOnBounds(), "Speed dial must not pick on bounds");
                assertEquals(javafx.scene.layout.Region.USE_PREF_SIZE, dial.getMaxWidth(), 0.001);
                assertEquals(javafx.scene.layout.Region.USE_PREF_SIZE, dial.getMaxHeight(), 0.001);

                // Verify dashboard FXML speed dial & snackbar
                URL url = getClass().getResource("/fxml/dashboard.fxml");
                assertNotNull(url);
                FXMLLoader loader = new FXMLLoader(url);
                Parent root = loader.load();
                javafx.scene.Node speedDial = root.lookup("#quickActionSpeedDial");
                assertNotNull(speedDial);
                assertFalse(speedDial.isPickOnBounds(), "Dashboard speed dial must not block mouse clicks");

                javafx.scene.Node snackbar = root.lookup("#dashboardSnackbar");
                assertNotNull(snackbar);
                assertTrue(snackbar.isMouseTransparent(), "Dashboard snackbar must be mouse transparent when idle");
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (err.get() != null) {
            throw new AssertionError("Speed dial or snackbar mouse blocking verification failed", err.get());
        }
    }

    @Test
    void testGymAppWindowMatchesScreenBounds() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                javafx.stage.Stage stage = new javafx.stage.Stage();
                GymApp app = new GymApp();
                app.start(stage);

                assertTrue(stage.isMaximized(), "GymApp stage must be maximized to match display screen");
                javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
                assertTrue(stage.getScene().getWidth() <= screen.getWidth() + 1.0);
                assertTrue(stage.getScene().getHeight() <= screen.getHeight() + 1.0);

                stage.hide();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (err.get() != null) {
            throw new AssertionError("GymApp screen bounds test failed", err.get());
        }
    }

    @Test
    void testEventDispatchingWithoutIllegalArgumentException() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                URL shellUrl = getClass().getResource("/fxml/app-shell.fxml");
                assertNotNull(shellUrl);
                FXMLLoader loader = new FXMLLoader(shellUrl);
                Parent shellRoot = loader.load();
                javafx.scene.Scene scene = new javafx.scene.Scene(shellRoot, 1280, 800);
                shellRoot.applyCss();

                // 1. Fire mouse click on searchTriggerBar
                javafx.scene.Node searchBar = shellRoot.lookup("#searchTriggerBar");
                assertNotNull(searchBar, "searchTriggerBar must exist");
                javafx.scene.input.MouseEvent mouseClick = new javafx.scene.input.MouseEvent(
                        javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                        0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 1,
                        false, false, false, false, true, false, false, false, false, false, null
                );
                searchBar.fireEvent(mouseClick);

                // 2. Fire mouse click on themeModeSwitch
                javafx.scene.Node themeSwitch = shellRoot.lookup("#themeModeSwitch");
                assertNotNull(themeSwitch, "themeModeSwitch must exist");
                themeSwitch.fireEvent(mouseClick);

                // 3. Fire action on railToggleBtn
                javafx.scene.Node railToggle = shellRoot.lookup("#railToggleBtn");
                assertNotNull(railToggle, "railToggleBtn must exist");
                railToggle.fireEvent(new javafx.event.ActionEvent());

            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (err.get() != null) {
            throw new AssertionError("FXML event dispatch failed with exception", err.get());
        }
    }

    @Test
    void testM3CarouselLayoutNoBoundSetException() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                io.m3fx.controls.carousel.M3Carousel carousel = new io.m3fx.controls.carousel.M3Carousel();
                carousel.getItems().add(new javafx.scene.control.Label("Card 1"));
                carousel.getItems().add(new javafx.scene.control.Label("Card 2"));

                javafx.scene.layout.StackPane container = new javafx.scene.layout.StackPane(carousel);
                javafx.scene.Scene scene = new javafx.scene.Scene(container, 600, 400);
                container.applyCss();
                container.layout();

                // Resize carousel multiple times to verify no "A bound value cannot be set"
                carousel.resize(400, 250);
                carousel.requestLayout();
                container.layout();

                carousel.resize(800, 300);
                carousel.requestLayout();
                container.layout();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (err.get() != null) {
            throw new AssertionError("M3Carousel layout failed", err.get());
        }
    }

    @Test
    void testNavigationRailAndPageScrollToEnd() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                URL shellUrl = getClass().getResource("/fxml/app-shell.fxml");
                assertNotNull(shellUrl);
                FXMLLoader loader = new FXMLLoader(shellUrl);
                Parent shellRoot = loader.load();
                javafx.scene.Scene scene = new javafx.scene.Scene(shellRoot, 1024, 600);
                shellRoot.applyCss();
                shellRoot.layout();

                // 1. Verify side menu ScrollPane can scroll to end and navHelp is reachable
                javafx.scene.control.ScrollPane railScroll = (javafx.scene.control.ScrollPane) shellRoot.lookup(".rail-scroll-pane");
                assertNotNull(railScroll);
                railScroll.setVvalue(1.0);
                assertEquals(1.0, railScroll.getVvalue(), 0.01);

                javafx.scene.Node navHelp = shellRoot.lookup("#navHelp");
                assertNotNull(navHelp, "Documentation button navHelp must exist in rail");

                // 2. Load trainer-list.fxml (which contains M3Carousel) and verify layout succeeds
                AppShellController controller = loader.getController();
                controller.loadScreen("trainer-list.fxml");
                shellRoot.applyCss();
                shellRoot.layout();

                // 3. Load membership.fxml (which contains M3Carousel) and verify layout succeeds
                controller.loadScreen("membership.fxml");
                shellRoot.applyCss();
                shellRoot.layout();

                // 4. Return to dashboard.fxml and test scrolling to bottom
                controller.loadScreen("dashboard.fxml");
                shellRoot.applyCss();
                shellRoot.layout();

                javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) shellRoot.lookup("#contentArea");
                javafx.scene.control.ScrollPane dashboardScroll = (javafx.scene.control.ScrollPane) contentArea.getChildren().get(0);
                dashboardScroll.setVvalue(1.0);
                assertEquals(1.0, dashboardScroll.getVvalue(), 0.01);

                javafx.scene.Node facilityRadar = shellRoot.lookup("#facilityRadarCard");
                assertNotNull(facilityRadar, "Facility radar card must be present at bottom of dashboard");

            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (err.get() != null) {
            throw new AssertionError("Navigation rail or page scroll test failed", err.get());
        }
    }

    @Test
    void testMembershipScreenLayoutAndComponents() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/membership.fxml"));
                Parent root = loader.load();
                Object ctrl = loader.getController();
                assertNotNull(ctrl, "MembershipController should be injected");

                javafx.scene.Scene scene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(root), 1280, 800);
                root.applyCss();
                root.layout();

                // 1. Verify Carousel
                io.m3fx.controls.carousel.M3Carousel carousel = (io.m3fx.controls.carousel.M3Carousel) root.lookup("#tierCarousel");
                assertNotNull(carousel, "tierCarousel must be present");
                assertEquals(3, carousel.getItems().size(), "3 membership tier cards should be in carousel");

                // 2. Verify Funnel Chart is populated
                io.m3fx.controls.chart.M3FunnelChart funnelChart = (io.m3fx.controls.chart.M3FunnelChart) root.lookup("#conversionFunnelChart");
                assertNotNull(funnelChart, "conversionFunnelChart must be present");
                assertTrue(funnelChart.getStages().size() >= 4, "Funnel chart should have 4 conversion stages");

                // 3. Verify Data Table is populated
                io.m3fx.controls.data.M3DataTable<?> table = (io.m3fx.controls.data.M3DataTable<?>) root.lookup("#membershipDataTable");
                assertNotNull(table, "membershipDataTable must be present");
                assertTrue(table.getItems().size() >= 5, "Membership table should have initial agreement rows");

                // 4. Verify search and filter controls
                io.m3fx.controls.textfield.M3TextField search = (io.m3fx.controls.textfield.M3TextField) root.lookup("#searchMemberField");
                assertNotNull(search, "searchMemberField must be present");
                io.m3fx.controls.chip.M3Chip chip = (io.m3fx.controls.chip.M3Chip) root.lookup("#chipExpiringFilter");
                assertNotNull(chip, "chipExpiringFilter must be present");

                // Filter search
                search.setText("Sarah");
                assertEquals(1, table.getItems().size(), "Search filter should match exactly 1 record");

                search.setText("");
                assertTrue(table.getItems().size() >= 5, "Clearing search should restore records");

            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (err.get() != null) {
            throw new AssertionError("Membership screen layout test failed", err.get());
        }
    }
}

