package com.gym.controller;

import java.util.function.Supplier;

/**
 * Central registry of the Gym-OS application screens — the FXML replacement.
 *
 * <p>Where navigation previously said {@code loadScreen("dashboard.fxml")} and the shell
 * resolved the string to an {@code FXMLLoader} resource, it now says
 * {@code loadScreen(Screen.DASHBOARD)} and this enum instantiates the screen's controller,
 * which builds its whole view in its constructor. One entry per screen; the key keeps the
 * human-readable screen name ({@code "dashboard"} — the old FXML file name minus extension)
 * for logs, tests and diagnostics.
 */
public enum Screen {

    LOGIN("login", LoginController::new),
    APP_SHELL("app-shell", AppShellController::new),
    DASHBOARD("dashboard", DashboardController::new),
    CUSTOMER_LIST("customer-list", CustomerListController::new),
    CUSTOMER_PROFILE("customer-profile", CustomerProfileController::new),
    MEDICAL_PROFILE("medical-profile", MedicalProfileController::new),
    PROGRESS("progress", ProgressController::new),
    TRAINER_LIST("trainer-list", TrainerListController::new),
    TRAINER_PROFILE("trainer-profile", TrainerProfileController::new),
    AVAILABILITY("availability", AvailabilityController::new),
    WORKOUT("workout", WorkoutController::new),
    LIVE_WORKOUT_HUD("live-workout-hud", LiveWorkoutController::new),
    SCHEDULE("schedule", ScheduleController::new),
    EQUIPMENT("equipment", EquipmentController::new),
    GYM_FLOOR_MAP("gym-floor-map", GymFloorMapController::new),
    ATTENDANCE("attendance", AttendanceController::new),
    MEMBERSHIP("membership", MembershipController::new),
    PAYMENT("payment", PaymentController::new),
    STAFF("staff", StaffController::new);

    private String key;
    private Supplier<? extends ScreenController> factory;

    Screen(String key, Supplier<? extends ScreenController> factory) {
        this.key = key;
        this.factory = factory;
    }

    /** The registry key of this screen (the historical FXML name without extension). */
    public String key() {
        return key;
    }

    /**
     * Instantiates the screen's controller. The controller builds its entire node tree in its
     * constructor, so the returned value is immediately usable via {@link ScreenController#getRoot()}.
     */
    public ScreenController create() {
        return factory.get();
    }

    /** Resolves a screen by its registry key, e.g. {@code "dashboard"}. */
    public static Screen byKey(String key) {
        for (Screen screen : values()) {
            if (screen.key.equals(key)) {
                return screen;
            }
        }
        throw new IllegalArgumentException("Unknown screen key: " + key);
    }
}
