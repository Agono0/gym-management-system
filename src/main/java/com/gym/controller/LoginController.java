package com.gym.controller;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.effect.M3ParticleCanvas;
import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.controls.effect.M3Scanline;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.progress.M3CircularProgress;
import io.m3fx.controls.scene3d.M3GyroRing3D;
import io.m3fx.controls.selection.M3CheckBox;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the in-gallery Authentication &amp; Security Portal — the
 * "Authentication Vault" screen the shell loads when the Apple-style launch gates are disabled
 * (tests, standalone previews, the legacy sign-out path) or picked from the command palette.
 * It is <b>not</b> the real sign-in gate: {@link LaunchGateController} owns the splash → login
 * → morph choreography. This gallery screen is demo-only — any non-empty staff identifier /
 * email plus a 4+ character password signs in; validation answers with inline field errors and
 * a snackbar, and success navigates the shell to the dashboard via the pinned
 * {@code navDashboard} rail button. Pure UI: strictly no backend, DAO, or database logic.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former login.fxml: same layers (ambient particles → acrylic blur canopy → split workspace →
 * toast host), same fx:id-named fields, same handlers.
 */
public class LoginController implements ScreenController {

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

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane rootPane;
    private M3ParticleCanvas particleBackdrop;
    private M3GlassPane glassCanopy;
    private HBox mainHBox;
    private VBox heroViewport;
    private M3GyroRing3D kineticGyro;
    private M3Scanline hologramScanline;
    private M3PulseBeacon statusBeacon;
    private M3Chip securityBadgeChip;
    private StackPane cardGlow;
    private M3Card authCard;
    private StackPane feedbackContainer;
    private HBox feedbackBox;
    private M3Icon feedbackIcon;
    private Label feedbackLabel;
    private M3TextField usernameField;
    private Label usernameHelper;
    private PasswordField passwordField;
    private M3IconButton passwordToggleBtn;
    private Label capsLockAlert;
    private M3CheckBox rememberMeCheckBox;
    private M3Button loginButton;
    private M3CircularProgress loginSpinner;
    private M3Button forgotPasswordButton;
    private StackPane authSnackbar;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Revealed (plain) input swapped in for the masked {@code PasswordField} on demand. */
    private TextField revealedPasswordField;
    /** Whether the plain input is currently on screen (masked PasswordField by default). */
    private boolean passwordRevealed;
    /** Re-entrancy guard for the masked ↔ revealed text sync. */
    private boolean syncingPasswordText;

    /**
     * Builds the complete Authentication Vault view and initializes all behavior. Same ordering
     * semantics the FXMLLoader used to provide: the node tree exists and every field is
     * assigned before the former {@code initialize()} work runs.
     */
    public LoginController() {
        // Layer 0: ambient kinetic particles background.
        particleBackdrop = new M3ParticleCanvas();
        particleBackdrop.setParticleCount(35);
        particleBackdrop.getStyleClass().add("ambient-particles");

        // Layer 1: glass acrylic backdrop blur canopy.
        glassCanopy = new M3GlassPane();
        glassCanopy.setBlurRadius(24.0);
        glassCanopy.getStyleClass().add("acrylic-blur");

        // Layer 2: main split workspace layout.
        mainHBox = new HBox(32);
        mainHBox.setAlignment(Pos.CENTER);
        mainHBox.setPadding(new Insets(32, 24, 32, 24));

        // Column 1: hero viewport with 3D gyro centerpiece.
        heroViewport = new VBox(24);
        heroViewport.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(heroViewport, Priority.ALWAYS);
        heroViewport.getStyleClass().add("hero-container");

        HBox brandRow = new HBox(12);
        brandRow.setAlignment(Pos.CENTER_LEFT);
        M3Icon brandLogoIcon = M3Icon.symbol("stats");
        brandLogoIcon.getStyleClass().add("brand-logo-icon");
        brandRow.getChildren().addAll(
                brandLogoIcon,
                styledLabel("GYM-OS // ATHLETIC ENGINE", "brand-heading"));

        // 3D gyro kinetic centerpiece with scanline (fills the hero column, shrinks with it).
        StackPane gyroWrapper = new StackPane();
        gyroWrapper.setAlignment(Pos.CENTER);
        gyroWrapper.setPrefHeight(440);
        gyroWrapper.getStyleClass().add("gyro-wrapper");
        kineticGyro = new M3GyroRing3D();
        kineticGyro.setPrefSize(380, 380);
        hologramScanline = new M3Scanline();
        hologramScanline.getStyleClass().add("hero-scanline");
        gyroWrapper.getChildren().addAll(kineticGyro, hologramScanline);

        // Telemetry footer bar.
        HBox telemetryBar = new HBox(16);
        telemetryBar.setAlignment(Pos.CENTER_LEFT);
        telemetryBar.getStyleClass().add("telemetry-bar");
        statusBeacon = new M3PulseBeacon();
        statusBeacon.setStatus(M3PulseBeacon.BeaconStatus.ONLINE);
        Region telemetrySpacer = new Region();
        HBox.setHgrow(telemetrySpacer, Priority.ALWAYS);
        securityBadgeChip = new M3Chip("TLS 1.3 / WAL ACTIVE", ChipVariant.ASSIST);
        telemetryBar.getChildren().addAll(statusBeacon,
                styledLabel("STATION: FRONT_DESK_01", "telemetry-label"),
                telemetrySpacer,
                securityBadgeChip);

        heroViewport.getChildren().addAll(brandRow, gyroWrapper, telemetryBar);

        // Column 2: authentication bento card.
        cardGlow = new StackPane();
        cardGlow.getStyleClass().add("neon-halo");

        authCard = new M3Card(CardVariant.ELEVATED);
        authCard.setPrefWidth(480);
        authCard.getStyleClass().add("auth-card");
        authCard.setPadding(new Insets(40));

        VBox cardContent = new VBox(20);

        // Title header.
        VBox titleHeader = new VBox(6);
        titleHeader.getChildren().addAll(
                styledLabel("Operator Authentication", "card-title"),
                styledLabel("Enter credentials to initialize shift console", "card-subtitle"));

        // Feedback alert tray (dynamic error/warning box).
        feedbackContainer = new StackPane();
        feedbackContainer.setManaged(false);
        feedbackContainer.setVisible(false);
        feedbackBox = new HBox(12);
        feedbackBox.setAlignment(Pos.CENTER_LEFT);
        feedbackBox.getStyleClass().add("feedback-tray-error");
        feedbackIcon = M3Icon.symbol("close");
        feedbackLabel = new Label("Invalid credentials");
        feedbackLabel.setWrapText(true);
        feedbackBox.getChildren().addAll(feedbackIcon, feedbackLabel);
        feedbackContainer.getChildren().add(feedbackBox);

        // Staff ID field.
        VBox staffIdGroup = new VBox(6);
        staffIdGroup.getChildren().add(styledLabel("Staff Identifier", "field-label"));
        usernameField = new M3TextField("Staff Identifier", TextFieldVariant.OUTLINED);
        usernameField.setPrefHeight(56);
        usernameHelper = styledLabel("Case-sensitive unique employee ID", "helper-label");
        staffIdGroup.getChildren().addAll(usernameField, usernameHelper);

        // Password field & reveal toggle.
        VBox passwordGroup = new VBox(6);
        passwordGroup.getChildren().add(styledLabel("Master Password", "field-label"));
        HBox passwordRow = new HBox(8);
        passwordRow.setAlignment(Pos.CENTER_LEFT);
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter secure passphrase");
        passwordField.getStyleClass().add("app-password-field");
        passwordField.setPrefHeight(56);
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        passwordToggleBtn = new M3IconButton(M3Icon.symbol("search"), IconButtonVariant.STANDARD);
        passwordToggleBtn.setOnAction(e -> onTogglePasswordVisibility());
        passwordRow.getChildren().addAll(passwordField, passwordToggleBtn);
        capsLockAlert = new Label("[!] Caps Lock is ON");
        capsLockAlert.setManaged(false);
        capsLockAlert.getStyleClass().add("caps-alert-label");
        capsLockAlert.setVisible(false);
        passwordGroup.getChildren().addAll(passwordRow, capsLockAlert);

        // Session persistence.
        HBox rememberRow = new HBox(8);
        rememberRow.setAlignment(Pos.CENTER_LEFT);
        rememberMeCheckBox = new M3CheckBox("Keep active for full 12h shift");
        rememberRow.getChildren().add(rememberMeCheckBox);

        // Submit button with spinner.
        StackPane submitHolder = new StackPane();
        submitHolder.setAlignment(Pos.CENTER);
        submitHolder.setPrefHeight(56);
        loginButton = new M3Button("INITIALIZE CONSOLE", ButtonVariant.FILLED);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(56);
        loginButton.getStyleClass().add("btn-primary-action");
        loginButton.setGraphic(M3Icon.symbol("arrow_forward"));
        loginButton.setOnAction(e -> onLogin());
        loginSpinner = new M3CircularProgress();
        loginSpinner.setManaged(false);
        loginSpinner.setPrefSize(24, 24);
        loginSpinner.setVisible(false);
        submitHolder.getChildren().addAll(loginButton, loginSpinner);

        // Forgot password link.
        HBox forgotRow = new HBox();
        forgotRow.setAlignment(Pos.CENTER);
        forgotPasswordButton = new M3Button("Trouble signing in? Contact Administrator / Reset",
                ButtonVariant.TEXT);
        forgotPasswordButton.getStyleClass().add("btn-tertiary-link");
        forgotPasswordButton.setOnAction(e -> onForgotPassword());
        forgotRow.getChildren().add(forgotPasswordButton);

        cardContent.getChildren().addAll(
                titleHeader,
                feedbackContainer,
                staffIdGroup,
                passwordGroup,
                rememberRow,
                submitHolder,
                forgotRow);
        authCard.setContent(cardContent);
        cardGlow.getChildren().add(authCard);

        mainHBox.getChildren().addAll(heroViewport, cardGlow);

        // Layer 3: system notification & toast host container.
        authSnackbar = new StackPane();
        StackPane.setAlignment(authSnackbar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(authSnackbar, new Insets(0, 0, 32, 0));

        rootPane = new StackPane();
        rootPane.getStyleClass().addAll("login-root", "auth-root");
        rootPane.getChildren().addAll(particleBackdrop, glassCanopy, mainHBox, authSnackbar);

        installFeedbackLayer(rootPane);
        // Honest affordance: the bundled icon catalog lacks visibility icons, so the toggle
        // carries the SVG eye (the markup used to ship a placeholder magnifier glyph).
        passwordToggleBtn.setGraphic(M3Icon.ofSvg(EYE_PATH));
        passwordToggleBtn.setAccessibleText("Show password");
        // Editing again clears the inline tray and the field error.
        usernameField.textProperty().addListener((obs, ov, nv) -> clearErrors());

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        rootPane.setId("rootPane");
        particleBackdrop.setId("particleBackdrop");
        glassCanopy.setId("glassCanopy");
        mainHBox.setId("mainHBox");
        heroViewport.setId("heroViewport");
        kineticGyro.setId("kineticGyro");
        hologramScanline.setId("hologramScanline");
        statusBeacon.setId("statusBeacon");
        securityBadgeChip.setId("securityBadgeChip");
        cardGlow.setId("cardGlow");
        authCard.setId("authCard");
        feedbackContainer.setId("feedbackContainer");
        feedbackBox.setId("feedbackBox");
        feedbackIcon.setId("feedbackIcon");
        feedbackLabel.setId("feedbackLabel");
        usernameField.setId("usernameField");
        usernameHelper.setId("usernameHelper");
        passwordField.setId("passwordField");
        passwordToggleBtn.setId("passwordToggleBtn");
        capsLockAlert.setId("capsLockAlert");
        rememberMeCheckBox.setId("rememberMeCheckBox");
        loginButton.setId("loginButton");
        loginSpinner.setId("loginSpinner");
        forgotPasswordButton.setId("forgotPasswordButton");
        authSnackbar.setId("authSnackbar");
        passwordField.textProperty().addListener((obs, ov, nv) -> clearErrors());
    }

    /** Small label with one style class. */
    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    @Override
    public Parent getRoot() {
        return rootPane;
    }

    /**
     * Demo sign-in (same rules as the launch gate): non-empty identifier, 4+ character
     * password. Problems get inline field errors plus a snackbar; success navigates the shell
     * to the dashboard via {@code navDashboard}.
     */
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

    /** Demo answer for the reset link: resets are handled by the administrator — a real dialog. */
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

    /** Show/hide (eye) toggle: swaps the masked and plain inputs and flips its own glyph. */
    public void onTogglePasswordVisibility() {
        if (!(passwordField.getParent() instanceof HBox row)) {
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
        if (rootPane.getScene() == null
                || !(rootPane.getScene().getRoot() instanceof Parent sceneRoot)) {
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
        feedbackLabel.setText(message);
        feedbackContainer.setVisible(true);
        feedbackContainer.setManaged(true);
    }

    /** Clears the inline tray and the identifier field error. */
    private void clearErrors() {
        usernameField.setError(false);
        usernameField.setSupportingText("");
        feedbackContainer.setVisible(false);
        feedbackContainer.setManaged(false);
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
