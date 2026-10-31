package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.effect.M3ParticleCanvas;
import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.controls.effect.M3Scanline;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.progress.M3CircularProgress;
import io.m3fx.controls.scene3d.M3GyroRing3D;
import io.m3fx.controls.selection.M3CheckBox;
import io.m3fx.controls.textfield.M3TextField;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the in-gallery Authentication &amp; Security Portal (login.fxml) — the
 * "Authentication Vault" screen the shell loads when the Apple-style launch gates are disabled
 * (tests, standalone previews, the legacy sign-out path) or picked from the command palette.
 * It is <b>not</b> the real sign-in gate: {@link LaunchGateController} owns the splash → login
 * → morph choreography. This gallery screen is demo-only — any non-empty staff identifier /
 * email plus a 4+ character password signs in; validation answers with inline field errors and
 * a snackbar, and success navigates the shell to the dashboard via the pinned
 * {@code #navDashboard} rail button. Pure UI: strictly no backend, DAO, or database logic.
 */
public class LoginController {

    /** Demo password rule, matching the launch gate's rule (LaunchGateController). */
    private static final int MIN_PASSWORD_LENGTH = 4;

    /**
     * Material Symbols paths for the visibility glyphs the bundled {@code M3Icons} catalog lacks
     * (same paths as the launch gate). Apache 2.0, (c) Google — attribution kept per the repo
     * licensing rules.
     */
    private static final String EYE_PATH =
            "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5z"
            + "M12 17a5 5 0 1 1 0-10 5 5 0 0 1 0 10zm0-8a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
    private static final String EYE_OFF_PATH =
            "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75"
            + "-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7z"
            + "M2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84"
            + "l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3"
            + " .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2z"
            + "m4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";

    @FXML private StackPane rootPane;
    @FXML private M3ParticleCanvas particleBackdrop;
    @FXML private M3GlassPane glassCanopy;
    @FXML private HBox mainHBox;
    @FXML private VBox heroViewport;
    @FXML private M3GyroRing3D kineticGyro;
    @FXML private M3Scanline hologramScanline;
    @FXML private M3PulseBeacon statusBeacon;
    @FXML private M3Chip securityBadgeChip;
    @FXML private StackPane cardGlow;
    @FXML private M3Card authCard;
    @FXML private StackPane feedbackContainer;
    @FXML private HBox feedbackBox;
    @FXML private M3Icon feedbackIcon;
    @FXML private Label feedbackLabel;
    @FXML private M3TextField usernameField;
    @FXML private Label usernameHelper;
    @FXML private PasswordField passwordField;
    @FXML private M3IconButton passwordToggleBtn;
    @FXML private Label capsLockAlert;
    @FXML private M3CheckBox rememberMeCheckBox;
    @FXML private M3Button loginButton;
    @FXML private M3CircularProgress loginSpinner;
    @FXML private M3Button forgotPasswordButton;
    @FXML private StackPane authSnackbar;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Revealed (plain) input swapped in for the masked {@code PasswordField} on demand. */
    private TextField revealedPasswordField;
    /** Whether the plain input is currently on screen (masked PasswordField by default). */
    private boolean passwordRevealed;
    /** Re-entrancy guard for the masked ↔ revealed text sync. */
    private boolean syncingPasswordText;

    public LoginController() {}

    @FXML
    public void initialize() {
        installFeedbackLayer(rootPane);
        // Honest affordance: the bundled icon catalog lacks visibility icons, so the toggle
        // carries the SVG eye (the FXML ships a placeholder magnifier glyph).
        if (passwordToggleBtn != null) {
            passwordToggleBtn.setGraphic(M3Icon.ofSvg(EYE_PATH));
            passwordToggleBtn.setAccessibleText("Show password");
        }
        // Editing again clears the inline tray and the field error.
        if (usernameField != null) {
            usernameField.textProperty().addListener((obs, ov, nv) -> clearErrors());
        }
        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, ov, nv) -> clearErrors());
        }
    }

    /**
     * Demo sign-in (same rules as the launch gate): non-empty identifier, 4+ character
     * password. Problems get inline field errors plus a snackbar; success navigates the shell
     * to the dashboard via {@code #navDashboard}.
     */
    @FXML
    public void onLogin() {
        String identifier = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        clearErrors();

        boolean identifierOk = !identifier.isEmpty();
        usernameField.setError(!identifierOk);
        if (!identifierOk) {
            usernameField.setSupportingText("Enter your staff identifier or email to continue.");
        }

        boolean passwordOk = password.length() >= MIN_PASSWORD_LENGTH;
        if (!passwordOk) {
            showFeedback("Your password needs at least " + MIN_PASSWORD_LENGTH + " characters.");
        }

        if (!identifierOk || !passwordOk) {
            feedback.showSnackbar("Sign-in blocked — fix the highlighted fields (demo).");
            return;
        }
        navigateToDashboard();
    }

    @FXML
    public void onLogin(ActionEvent event) {
        onLogin();
    }

    /** Demo answer for the reset link: resets are handled by the administrator — a real dialog. */
    @FXML
    public void onForgotPassword() {
        M3Button gotIt = M3Button.text("Got it");
        M3Dialog dialog = M3Dialog.basic("Trouble signing in?",
                new Label("Password resets are handled by the administrator at the front desk "
                        + "(demo). On this console any non-empty staff identifier and a 4+ "
                        + "character password signs in."),
                gotIt);
        dialog.setDefaultAction(gotIt::activate);
        gotIt.setOnAction(e -> dialog.hide());
        feedback.showDialog(dialog);
    }

    @FXML
    public void onForgotPassword(ActionEvent event) {
        onForgotPassword();
    }

    /** Show/hide (eye) toggle: swaps the masked and plain inputs and flips its own glyph. */
    @FXML
    public void onTogglePasswordVisibility() {
        if (passwordField == null || !(passwordField.getParent() instanceof HBox row)) {
            return;
        }
        ensureRevealedField();
        Node visibleNow = passwordRevealed ? revealedPasswordField : passwordField;
        Node visibleNext = passwordRevealed ? passwordField : revealedPasswordField;
        int index = row.getChildren().indexOf(visibleNow);
        if (index < 0) {
            return;
        }
        row.getChildren().set(index, visibleNext);
        HBox.setHgrow(visibleNext, Priority.ALWAYS);
        passwordRevealed = !passwordRevealed;
        passwordToggleBtn.setGraphic(M3Icon.ofSvg(passwordRevealed ? EYE_OFF_PATH : EYE_PATH));
        passwordToggleBtn.setAccessibleText(passwordRevealed ? "Hide password" : "Show password");
        visibleNext.requestFocus();
        if (visibleNext instanceof TextField textField) {
            textField.positionCaret(textField.getText() == null ? 0 : textField.getText().length());
        }
    }

    @FXML
    public void onTogglePasswordVisibility(ActionEvent event) {
        onTogglePasswordVisibility();
    }

    // ------------------------------------------------------------------
    // Demo auth helpers
    // ------------------------------------------------------------------

    /**
     * Navigates the shell to the dashboard by calling the shell's public
     * {@code onNavDashboard} — the same guarded shell lookup the other cross-screen actions use
     * (the shell publishes itself in the scene-root properties under
     * {@link AppShellController#SHELL_CONTROLLER_KEY}). Standalone previews answer via snackbar.
     */
    private void navigateToDashboard() {
        String standaloneAnswer = "Signed in (demo) — the dashboard opens in the app window.";
        if (rootPane == null || rootPane.getScene() == null
                || !(rootPane.getScene().getRoot() instanceof javafx.scene.Parent sceneRoot)) {
            feedback.showSnackbar(standaloneAnswer);
            return;
        }
        Object shell = sceneRoot.getProperties().get(AppShellController.SHELL_CONTROLLER_KEY);
        if (shell instanceof AppShellController controller) {
            controller.onNavDashboard();
            return;
        }
        feedback.showSnackbar(standaloneAnswer);
    }

    /**
     * Creates the revealed (plain) input once and keeps it mirrored with the masked
     * {@link PasswordField} in both directions, so the typed password survives the swap.
     */
    private void ensureRevealedField() {
        if (revealedPasswordField != null) {
            return;
        }
        revealedPasswordField = new TextField();
        revealedPasswordField.getStyleClass().addAll(passwordField.getStyleClass());
        revealedPasswordField.setPromptText(passwordField.getPromptText());
        revealedPasswordField.setPrefHeight(passwordField.getPrefHeight());
        passwordField.textProperty().addListener((obs, ov, nv) -> {
            if (syncingPasswordText) {
                return;
            }
            syncingPasswordText = true;
            revealedPasswordField.setText(nv);
            syncingPasswordText = false;
        });
        revealedPasswordField.textProperty().addListener((obs, ov, nv) -> {
            if (syncingPasswordText) {
                return;
            }
            syncingPasswordText = true;
            passwordField.setText(nv);
            syncingPasswordText = false;
        });
    }

    /** Inline tray for the masked password field (it has no per-field error API). */
    private void showFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message);
        }
        if (feedbackContainer != null) {
            feedbackContainer.setVisible(true);
            feedbackContainer.setManaged(true);
        }
    }

    /** Clears the inline tray and the identifier field error. */
    private void clearErrors() {
        if (usernameField != null) {
            usernameField.setError(false);
            usernameField.setSupportingText("");
        }
        if (feedbackContainer != null) {
            feedbackContainer.setVisible(false);
            feedbackContainer.setManaged(false);
        }
    }

    /**
     * Hosts the shared feedback overlay on the scene root while this screen is visible. Attach
     * and detach are deferred with {@link Platform#runLater(Runnable) Platform.runLater} so the
     * children list of the scene root (or of the pane hosting this screen) is never mutated
     * re-entrantly while the shell swaps screen content.
     */
    private void installFeedbackLayer(Node anchor) {
        anchor.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null && scene.getRoot() instanceof Pane root
                    && !root.getChildren().contains(feedback)) {
                root.getChildren().add(feedback);
            }
            if (oldScene != null && oldScene.getRoot() instanceof Pane oldRoot) {
                // Deferred so a screen swap that is mid-mutation of the pane hosting the screen
                // never re-enters a JavaFX children list from inside this listener.
                Platform.runLater(() -> oldRoot.getChildren().remove(feedback));
            }
        });
    }
}
