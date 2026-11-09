package com.gym.screen;

import com.gym.controller.Screen;
import javafx.scene.Parent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * E2E build-integrity harness for all 19 Gym-OS screens (the former FXML integrity harness).
 * Verifies: every screen in the {@link Screen} registry instantiates cleanly on demand, builds
 * a non-null root, and the registry inventory is complete — the exact guarantees the markup
 * integrity test used to pin, now against the Java-built controllers.
 */
public class ScreenBuildIntegrityTest {

    /** The full screen inventory (mirrors {@link Screen#values()}). */
    public static final Screen[] ALL_19_SCREENS = {
            Screen.LOGIN,
            Screen.APP_SHELL,
            Screen.DASHBOARD,
            Screen.CUSTOMER_LIST,
            Screen.CUSTOMER_PROFILE,
            Screen.MEDICAL_PROFILE,
            Screen.PROGRESS,
            Screen.TRAINER_LIST,
            Screen.TRAINER_PROFILE,
            Screen.AVAILABILITY,
            Screen.WORKOUT,
            Screen.LIVE_WORKOUT_HUD,
            Screen.SCHEDULE,
            Screen.EQUIPMENT,
            Screen.GYM_FLOOR_MAP,
            Screen.ATTENDANCE,
            Screen.MEMBERSHIP,
            Screen.PAYMENT,
            Screen.STAFF
    };

    @ParameterizedTest(name = "Screen build integrity: {0}")
    @EnumSource(value = Screen.class, names = {"APP_SHELL", "LOGIN", "DASHBOARD", "CUSTOMER_LIST",
            "CUSTOMER_PROFILE", "MEDICAL_PROFILE", "PROGRESS", "TRAINER_LIST", "TRAINER_PROFILE",
            "AVAILABILITY", "WORKOUT", "LIVE_WORKOUT_HUD", "SCHEDULE", "EQUIPMENT",
            "GYM_FLOOR_MAP", "ATTENDANCE", "MEMBERSHIP", "PAYMENT", "STAFF"})
    @DisplayName("Verify each of the 19 required screens builds a non-null root")
    void testScreenBuildIntegrity(Screen screen) throws Exception {
        Parent[] rootHolder = new Parent[1];
        Throwable[] error = new Throwable[1];
        javafx.application.Platform.runLater(() -> {
            try {
                rootHolder[0] = screen.create().getRoot();
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        long deadline = System.currentTimeMillis() + 10_000;
        while (rootHolder[0] == null && error[0] == null
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        if (error[0] != null) {
            throw new AssertionError("Failed to build screen " + screen.key(), error[0]);
        }
        assertNotNull(rootHolder[0],
                "Root node for screen " + screen.key() + " must not be null");
    }

    @Test
    @DisplayName("Summary check of screen registry inventory completeness")
    void testScreenInventoryPresence() {
        assertEquals(19, Screen.values().length,
                "The screen registry must expose exactly the 19 Gym-OS screens");
        assertEquals(19, ALL_19_SCREENS.length,
                "The documented inventory must match the registry size");
        System.out.printf("[E2E HARNESS] Screen registry presence: %d/19 registered.%n",
                Screen.values().length);
    }
}
