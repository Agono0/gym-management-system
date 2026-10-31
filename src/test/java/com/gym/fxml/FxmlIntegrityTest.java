package com.gym.fxml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Headless JavaFX E2E Integrity Test Harness for all 19 FXML UI screens.
 * Verifies Requirement R4:
 * "All created FXML files must be syntactically valid JavaFX 21 XML,
 * resolve all component imports, load cleanly without parse errors, and pass ./gradlew check."
 *
 * <p>Strict by default: a missing screen file FAILS. The lenient mode (missing screens are
 * skipped via a failed assumption, for progressive milestone testing) is opt-in only, via
 * the explicit {@code -Dtest.lenient=true} system property.</p>
 */
public class FxmlIntegrityTest {

    public static final List<String> ALL_19_SCREENS = List.of(
            "login.fxml",
            "app-shell.fxml",
            "dashboard.fxml",
            "customer-list.fxml",
            "customer-profile.fxml",
            "medical-profile.fxml",
            "progress.fxml",
            "trainer-list.fxml",
            "trainer-profile.fxml",
            "availability.fxml",
            "workout.fxml",
            "live-workout-hud.fxml",
            "schedule.fxml",
            "equipment.fxml",
            "gym-floor-map.fxml",
            "attendance.fxml",
            "membership.fxml",
            "payment.fxml",
            "staff.fxml"
    );

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Platform already initialized
        }
    }

    /**
     * Loads the FXML resource on the JavaFX Application Thread and returns the root Parent node.
     * Captures and propagates any parsing, class-loading, or reflection exceptions.
     */
    public static Parent loadFxml(URL resource) throws Exception {
        assertNotNull(resource, "FXML URL cannot be null");
        AtomicReference<Parent> rootRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(resource);
                Parent root = loader.load();
                rootRef.set(root);
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        if (!finished) {
            throw new TimeoutException("FXMLLoader timed out after 10 seconds for " + resource);
        }

        if (errorRef.get() != null) {
            Throwable t = errorRef.get();
            if (t instanceof Exception e) {
                throw e;
            }
            throw new RuntimeException("Unexpected error during FXML load: " + resource, t);
        }

        return rootRef.get();
    }

    @ParameterizedTest(name = "FXML Screen Integrity: {0}")
    @ValueSource(strings = {
            "login.fxml",
            "app-shell.fxml",
            "dashboard.fxml",
            "customer-list.fxml",
            "customer-profile.fxml",
            "medical-profile.fxml",
            "progress.fxml",
            "trainer-list.fxml",
            "trainer-profile.fxml",
            "availability.fxml",
            "workout.fxml",
            "live-workout-hud.fxml",
            "schedule.fxml",
            "equipment.fxml",
            "gym-floor-map.fxml",
            "attendance.fxml",
            "membership.fxml",
            "payment.fxml",
            "staff.fxml"
    })
    @DisplayName("Verify integrity of each of the 19 required FXML screens")
    void testFxmlScreenIntegrity(String fxmlFileName) {
        URL resource = getClass().getResource("/fxml/" + fxmlFileName);
        // Inverted default: a missing screen file fails unless lenient mode was requested
        // explicitly with -Dtest.lenient=true.
        boolean lenientMode = Boolean.getBoolean("test.lenient");

        if (resource == null) {
            if (lenientMode) {
                Assumptions.assumeTrue(false,
                        "Screen '/fxml/" + fxmlFileName + "' not yet created (progressive milestone testing).");
                return;
            }
            fail("Mandatory screen '/fxml/" + fxmlFileName + "' does not exist on classpath. "
                    + "Run with -Dtest.lenient=true to skip missing screens instead.");
        }

        assertDoesNotThrow(() -> {
            Parent root = loadFxml(resource);
            assertNotNull(root, "Root node for /fxml/" + fxmlFileName + " must not be null");
        }, "Failed to parse and load FXML: /fxml/" + fxmlFileName);
    }

    @Test
    @DisplayName("Summary check of screen inventory presence")
    void testScreenInventoryPresence() {
        boolean lenientMode = Boolean.getBoolean("test.lenient");

        List<String> missingScreens = ALL_19_SCREENS.stream()
                .filter(name -> getClass().getResource("/fxml/" + name) == null)
                .toList();

        if (missingScreens.isEmpty()) {
            System.out.printf("[E2E HARNESS] Screen presence: %d/19 present.%n", ALL_19_SCREENS.size());
            return;
        }

        if (lenientMode) {
            int presentCount = ALL_19_SCREENS.size() - missingScreens.size();
            System.out.printf("[E2E HARNESS] Screen presence: %d/19 present. Missing: %s%n",
                    presentCount, missingScreens);
        } else {
            fail("Missing " + missingScreens.size() + "/19 screens: " + missingScreens
                    + ". Run with -Dtest.lenient=true to skip missing screens instead.");
        }
    }
}
