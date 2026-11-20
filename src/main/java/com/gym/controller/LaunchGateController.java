package com.gym.controller;

import com.gym.ui.Data;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.effect.M3ParticleCanvas;
import io.m3fx.controls.glass.GlassEffectParams;
import io.m3fx.controls.glass.GlassShape;
import io.m3fx.controls.glass.M3GlassPane;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.internal.MotionUtil;
import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.progress.M3LinearProgress;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * The Apple-style launch experience for GYM-OS: splash screen → login gate → fluid morph into
 * the shell, plus the sign-out flow back to the gate.
 *
 * <p>Every node is built in code with plain private fields (no FXML anywhere in the app):
 * {@link AppShellController} creates exactly one instance and delegates to it; overlays are
 * added as children of the shell's scene root (the {@code app-shell-root} StackPane) and
 * removed again when the morph completes.
 *
 * <p>Motion comes exclusively from the shared helpers ({@link MotionUtil#splashIn},
 * {@link MotionUtil#splashOut}, {@link MotionUtil#shake}, {@link MotionUtil#morphFromBounds},
 * {@link MotionUtil#enter}, {@link MotionUtil#exit}) plus tiny local beats built on
 * {@link SpringAnimation}. Everything runs on the FX thread; overlays are created, shown and
 * removed only there; a failed motion helper can never strand the user behind the gate (every
 * choreography step has a plain fade fallback).
 *
 * <p>Surfaces use the real liquid glass (Apple iOS 26 material, {@code io.m3fx.controls.glass}):
 * the splash brand panel is a CLEAR glass pane floating over an ambient {@link M3ParticleCanvas}
 * backdrop, and the login card is a REGULAR interactive glass pane (RECT, 28px corners). Glass is
 * reserved for these two floating chrome surfaces — the choreography around them
 * (splashOut dissolve, brand handoff, shell morph via {@code MotionUtil.morphFromBounds}) is
 * untouched, so a glass failure can no more strand the gate than a motion failure could.
 */
public class LaunchGateController {

    // ---- visual constants (colors always come from -md-sys-color-* looked-up colors) ----
    private static final double BRAND_MARK_SIZE = 96;
    private static final double BRAND_GLYPH_SCALE = 44.0 / M3Icon.DEFAULT_SIZE;
    private static final double CARD_WIDTH = 400;
    /** Login card glass corner radius (RECT shape); matches the previous glass card. */
    private static final double GATE_CARD_RADIUS = 28;
    /** CLEAR splash glass panel side — the brand mark plus headline breathing room. */
    private static final double SPLASH_GLASS_PANEL_SIZE = 280;
    private static final double SPLASH_SCRIM_OPACITY = 0.97;
    private static final double SIGNOUT_SCRIM_OPACITY = 0.88;

    /** The shell that owns this gate (created once by {@link AppShellController#initialize} flow). */
    private AppShellController shell;

    // ---- overlay state (all FX-thread-only, guarded by gateShowing) ----
    private StackPane gateOverlay;      // full-window child of the shell's scene root
    private Region scrim;               // dimming backdrop (also the "veil" that hides boot)
    private StackPane splashLayer;      // splash content (brand + title + progress)
    private VBox splashText;            // the part of the splash that dissolves via splashOut
    private StackPane brandHolder;      // keeps the brand mark centered during the splash
    private M3GlassPane splashGlassPanel; // CLEAR glass surface behind the splash brand
    private StackPane gateLayer;        // login gate content
    private StackPane brandMark;        // THE brand mark node — shared by splash and login card
    private StackPane brandSlot;        // slot inside the login card the brand settles into
    private M3GlassPane loginCard;
    private M3LinearProgress splashProgress;
    private M3LinearProgress signInBeat;
    private M3TextField usernameField;
    private M3TextField passwordField;
    private TextField passwordPlain;    // revealed (plain) input inside passwordField
    private PasswordField passwordMasked; // hidden (masked) input swapped in by default
    private M3IconButton passwordToggle;
    private Label errorCaption;
    private StackPane capsHint;
    private M3Button signInButton;
    private FadeTransition overlayFade; // local veil fade (not a MotionUtil transition)
    private boolean passwordHidden = true;
    private boolean syncingPassword;    // re-entrancy guard for the plain↔masked text sync
    private boolean signingIn;
    /** Bumped on every gate reset so a stale sign-in callback (gate re-opened mid-auth) is a no-op. */
    private int gateGeneration;
    /** True while any gate overlay is on the scene root — guards against stacking duplicates. */
    private boolean gateShowing;

    /** The real password, kept in sync between the masked and the revealed input. */
    private final StringProperty realPassword = new SimpleStringProperty("");

    /**
     * Creates the launch gate for the given shell. Called by {@link AppShellController} only.
     *
     * @param shell the shell controller used for prewarming, the morph target and page settling
     */
    public LaunchGateController(AppShellController shell) {
        this.shell = shell;
    }

    // =====================================================================================
    // Public entry points
    // =====================================================================================

    /**
     * Runs the full cold-start choreography: splash (fade/zoom in, brand + determinate progress
     * while the dashboard prewarms) → splashOut → login gate. Called by {@code GymApp.start}
     * when {@code SHOW_LAUNCH_GATES} is enabled.
     */
    public void beginLaunchGates() {
        Platform.runLater(() -> {
            if (!ensureOverlayBuilt()) {
                return;
            }
            scrim.setOpacity(SPLASH_SCRIM_OPACITY);
            resetGateState();
            gateShowing = true;
            MotionUtil.splashIn(gateOverlay);
            Platform.runLater(this::playSplashBeats);
        });
    }

    /**
     * Shows the login gate over the running shell (sign-out flow and the command-palette
     * Authentication Vault action): quick dim of the shell, fresh fields, gate card enters.
     * The gate overlay is created once and reused — a second call never stacks a duplicate.
     */
    public void showLoginGate() {
        Platform.runLater(() -> {
            if (gateShowing) {
                return; // already on screen (splash or gate) — never stack overlays
            }
            if (!ensureOverlayBuilt()) {
                return;
            }
            scrim.setOpacity(SIGNOUT_SCRIM_OPACITY);
            resetGateState();
            splashLayer.setVisible(false);
            gateLayer.setVisible(true);
            loginCard.setVisible(true);
            gateShowing = true;

            // Quick fade/dim of the shell beneath, then the card rises in. Stop any veil
            // fade still in flight first (a very fast re-entry must never fight it).
            stopOverlayFade();
            gateOverlay.setOpacity(0);
            overlayFade = new FadeTransition(Duration.millis(220), gateOverlay);
            overlayFade.setToValue(1.0);
            overlayFade.setOnFinished(e -> overlayFade = null);
            overlayFade.play();

            MotionUtil.splashIn(loginCard);
            focusUsernameAfterLayout();
        });
    }

    /** @return true while a splash or login gate overlay is on the scene root */
    public boolean isGateShowing() {
        return gateShowing;
    }

    // =====================================================================================
    // Overlay construction
    // =====================================================================================

    /**
     * Builds the full-window overlay once and attaches it to the shell's scene root. Returns
     * false (and disables the gates) when the scene is unavailable or its root is not a Pane —
     * the app keeps working normally in that case.
     */
    private boolean ensureOverlayBuilt() {
        if (gateOverlay != null) {
            return true;
        }
        Region morphTarget = shell.getMorphTarget();
        Scene scene = morphTarget == null ? null : morphTarget.getScene();
        if (scene == null || !(scene.getRoot() instanceof Pane rootPane)) {
            System.err.println("Launch gates: scene root is not a Pane — gates disabled.");
            return false;
        }

        gateOverlay = new StackPane();
        gateOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        // The overlay itself stays transparent: the scrim child owns all dimming, so the
        // sign-out flow can show the dimmed shell through it.

        scrim = new Region();
        scrim.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scrim.setStyle("-fx-background-color: -md-sys-color-surface;");
        scrim.setOpacity(SPLASH_SCRIM_OPACITY);

        buildSplashLayer();
        buildGateLayer();

        gateOverlay.getChildren().addAll(scrim, splashLayer, gateLayer);
        gateLayer.setVisible(false);
        rootPane.getChildren().add(gateOverlay); // last child → paints above shell + palette
        return true;
    }

    /** Splash: the brand mark stays at the exact screen center while the text sits below it. */
    private void buildSplashLayer() {
        splashLayer = new StackPane();
        splashLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        brandMark = buildBrandMark();
        brandHolder = new StackPane(brandMark);
        brandHolder.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        brandHolder.setPickOnBounds(false);

        splashText = new VBox(10);
        splashText.setAlignment(Pos.TOP_CENTER);
        splashText.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        // Top spacer pushes the text block to start right under the centered brand mark.
        Region underBrand = new Region();
        underBrand.setPrefHeight(BRAND_MARK_SIZE / 2 + 44);
        splashText.getChildren().add(underBrand);

        Label title = new Label("GYM-OS");
        title.setStyle("-fx-font-size: 34px; -fx-font-weight: 700;"
                + " -fx-text-fill: -md-sys-color-on-surface;");
        Label tagline = new Label("Enterprise Gym Management System");
        tagline.setStyle("-fx-font-size: 14px; -fx-text-fill: -md-sys-color-on-surface-variant;");

        splashProgress = new M3LinearProgress(0);
        splashProgress.setPrefWidth(280);
        splashProgress.setMinWidth(280);
        splashProgress.setMaxWidth(280);
        VBox.setMargin(splashProgress, new Insets(26, 0, 0, 0));

        Label version = new Label("Enterprise 2026 Edition · Material 3 Expressive");
        version.setStyle("-fx-font-size: 11px; -fx-text-fill: -md-sys-color-outline;"
                + " -fx-opacity: 0.85;");
        VBox.setMargin(version, new Insets(18, 0, 0, 0));

        splashText.getChildren().addAll(title, tagline, splashProgress, version);

        // Ambient particle field: gives the CLEAR glass panel living content to refract (Apple:
        // clear glass is for content-rich backdrops). Purely decorative — mouse-transparent and
        // non-interactive — and it hides with the splash layer, so the brand handoff choreography
        // never sees it.
        M3ParticleCanvas particles = new M3ParticleCanvas();
        particles.setInteractive(false);
        particles.setMouseTransparent(true);
        particles.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // CLEAR liquid glass behind the brand mark. Deliberately a sibling surface, not the
        // brand's parent: the brand reparents into the login card at the splash→gate handoff,
        // and keeping it out of the glass means that handoff (and its fallbacks) stay intact.
        splashGlassPanel = M3GlassPane.clear();
        splashGlassPanel.setPrefSize(SPLASH_GLASS_PANEL_SIZE, SPLASH_GLASS_PANEL_SIZE);
        splashGlassPanel.setMaxSize(SPLASH_GLASS_PANEL_SIZE, SPLASH_GLASS_PANEL_SIZE);
        splashGlassPanel.setMouseTransparent(true);

        splashLayer.getChildren().addAll(particles, splashGlassPanel, brandHolder, splashText);
    }

    /** Login gate: one focal glass card, generous whitespace, nothing else. */
    private void buildGateLayer() {
        gateLayer = new StackPane();
        gateLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        brandSlot = new StackPane();
        brandSlot.setAlignment(Pos.CENTER);

        Label headline = new Label("Welcome back");
        headline.setStyle("-fx-font-size: 26px; -fx-font-weight: 700;"
                + " -fx-text-fill: -md-sys-color-on-surface;");
        Label supporting = new Label("Sign in to the GYM-OS operations center.");
        supporting.setStyle("-fx-font-size: 13px; -fx-text-fill: -md-sys-color-on-surface-variant;");

        usernameField = new M3TextField("Username", TextFieldVariant.FILLED);
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.getInput().setPromptText("admin");
        usernameField.setLeadingIcon(M3Icon.symbol("person"));
        usernameField.getInput().textProperty().addListener((obs, ov, nv) -> clearError());
        usernameField.getInput().setOnAction(e -> attemptSignIn());

        passwordField = new M3TextField("Password", TextFieldVariant.FILLED);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setLeadingIcon(M3Icon.symbol("lock"));
        buildPasswordInput();

        capsHint = buildCapsHint();
        capsHint.setVisible(false);
        capsHint.setManaged(false);

        errorCaption = new Label();
        errorCaption.setWrapText(true);
        errorCaption.setStyle("-fx-font-size: 12px; -fx-text-fill: -md-sys-color-error;");
        errorCaption.setVisible(false);
        errorCaption.setManaged(false);

        signInButton = M3Button.filled("Sign in");
        signInButton.setMaxWidth(Double.MAX_VALUE);
        signInButton.setPrefHeight(48);
        signInButton.setOnAction(e -> attemptSignIn());
        MotionUtil.addPressScale(signInButton, 0.97);

        signInBeat = new M3LinearProgress(0);
        signInBeat.setMaxWidth(Double.MAX_VALUE);
        signInBeat.setVisible(false);
        signInBeat.setManaged(false);

        Label demoHint = new Label("Seeded accounts — admin / admin123, manager / mgr123,"
                + " staff / staff123. The inactive oldstaff / old123 account is refused (rule 16).");
        demoHint.setWrapText(true);
        demoHint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        demoHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -md-sys-color-on-surface-variant;"
                + " -fx-opacity: 0.75;");

        VBox card = new VBox(10,
                brandSlot, headline, supporting,
                usernameField, passwordField,
                capsHint, errorCaption,
                signInButton, signInBeat, demoHint);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(24, 20, 24, 20));
        card.setMaxWidth(CARD_WIDTH - 32); // inside the glass content box's 16px padding

        VBox.setMargin(headline, new Insets(18, 0, 0, 0));
        VBox.setMargin(usernameField, new Insets(18, 0, 0, 0));
        VBox.setMargin(signInButton, new Insets(14, 0, 0, 0));
        VBox.setMargin(demoHint, new Insets(12, 0, 0, 0));

        // REGULAR interactive liquid glass (Apple: glass is for interactive elements): RECT with
        // 28px corners, refraction/specular/saturation at the spec defaults, tint handled by the
        // skin. The pane's cornerRadius/glassOpacity mirror the params so the legacy surface
        // tuning (0.85 translucent fill over the scrim) is preserved by whichever layer reads it.
        GlassEffectParams cardGlass = new GlassEffectParams()
                .in(GlassShape.RECT, GATE_CARD_RADIUS)
                .interactive();
        loginCard = M3GlassPane.of(cardGlass);
        loginCard.setContent(card);
        loginCard.setCornerRadius(GATE_CARD_RADIUS);
        loginCard.setGlassOpacity(0.85);
        loginCard.setPrefWidth(CARD_WIDTH);
        loginCard.setMaxWidth(CARD_WIDTH);
        loginCard.setEffect(new DropShadow(BlurType.GAUSSIAN,
                Color.rgb(0, 0, 0, 0.45), 40, 0.15, 0, 14));

        gateLayer.getChildren().add(loginCard);
    }

    /**
     * The password input: an {@link M3TextField} whose displayed input swaps between a
     * {@link PasswordField} (hidden, default) and the field's own plain {@link TextField}
     * (revealed). Both stay synced to {@link #realPassword}, so the floating label state of
     * the M3TextField keeps working in either mode. The M3TextField exposes its input row
     * publicly ({@code getInput().getParent()}), and no library file is modified.
     */
    private void buildPasswordInput() {
        passwordPlain = passwordField.getInput();
        passwordPlain.setPromptText("Your password");
        passwordMasked = new PasswordField();
        passwordMasked.getStyleClass().addAll(passwordPlain.getStyleClass());
        passwordMasked.setPromptText("Your password");

        // Plain (hidden) ↔ masked (shown): both mirror realPassword so either can be swapped
        // into view at any time without losing typed text.
        passwordPlain.textProperty().addListener((obs, ov, nv) -> {
            if (syncingPassword) {
                return;
            }
            syncingPassword = true;
            realPassword.set(nv);
            passwordMasked.setText(nv);
            syncingPassword = false;
        });
        passwordMasked.textProperty().addListener((obs, ov, nv) -> {
            if (syncingPassword) {
                return;
            }
            syncingPassword = true;
            realPassword.set(nv);
            passwordPlain.setText(nv);
            syncingPassword = false;
        });
        realPassword.addListener((obs, ov, nv) -> clearError());

        passwordField.setTrailingIcon(buildPasswordToggle());

        // Default state: masked input shown, plain input parked (still synced, never displayed).
        passwordHidden = true;
        if (passwordPlain.getParent() instanceof HBox row) {
            row.getChildren().set(row.getChildren().indexOf(passwordPlain), passwordMasked);
            HBox.setHgrow(passwordMasked, Priority.ALWAYS);
        } else {
            // Unexpected internal structure: stay functional with the plain field visible.
            System.err.println("Launch gates: password input row not found — reveal toggle disabled.");
            passwordToggle.setVisible(false);
        }

        // Caps Lock hint + Enter-to-sign-in on whichever input is currently on screen.
        passwordField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.CAPS) {
                boolean show = !capsHint.isVisible();
                capsHint.setVisible(show);
                capsHint.setManaged(show);
            }
        });
        passwordPlain.focusedProperty().addListener((obs, ov, focused) -> hideCapsHintWhenUnfocused());
        passwordMasked.focusedProperty().addListener((obs, ov, focused) -> hideCapsHintWhenUnfocused());
        passwordPlain.setOnAction(e -> attemptSignIn());
        passwordMasked.setOnAction(e -> attemptSignIn());
    }

    private void hideCapsHintWhenUnfocused() {
        Platform.runLater(() -> {
            if (!passwordPlain.isFocused() && !passwordMasked.isFocused()
                    && (capsHint.isVisible() || capsHint.isManaged())) {
                capsHint.setVisible(false);
                capsHint.setManaged(false);
            }
        });
    }

    /** Show/hide (eye) toggle: swaps the masked and plain inputs and flips its own glyph. */
    private M3IconButton buildPasswordToggle() {
        passwordToggle = M3IconButton.standard(M3Icon.symbol("visibility"));
        passwordToggle.setAccessibleText(passwordHidden ? "Show password" : "Hide password");
        passwordToggle.setOnAction(e -> togglePasswordVisibility());
        return passwordToggle;
    }

    private void togglePasswordVisibility() {
        if (!(passwordPlain.getParent() instanceof HBox row)) {
            return;
        }
        Node visibleNow = passwordHidden ? passwordMasked : passwordPlain;
        Node visibleNext = passwordHidden ? passwordPlain : passwordMasked;
        int index = row.getChildren().indexOf(visibleNow);
        if (index < 0) {
            return;
        }
        row.getChildren().set(index, visibleNext);
        HBox.setHgrow(visibleNext, Priority.ALWAYS);
        passwordHidden = !passwordHidden;
        passwordToggle.setAccessibleText(passwordHidden ? "Show password" : "Hide password");
        passwordToggle.setGraphic(M3Icon.symbol(passwordHidden ? "visibility" : "visibility_off"));
        visibleNext.requestFocus();
        if (visibleNext instanceof TextField tf) {
            tf.positionCaret(tf.getText() == null ? 0 : tf.getText().length());
        }
    }

    private StackPane buildCapsHint() {
        Label text = new Label("Caps Lock is on");
        text.setStyle("-fx-font-size: 11px; -fx-text-fill: -md-sys-color-on-secondary-container;");
        StackPane chip = new StackPane(text);
        chip.setStyle("-fx-background-color: -md-sys-color-secondary-container;"
                + " -fx-background-radius: 8px;");
        chip.setPadding(new Insets(4, 10, 4, 10));
        return chip;
    }

    /**
     * The brand mark: the app's stats motif inside a rounded primary-container badge. Built
     * once for the splash and reused inside the login card, so the splash → gate handoff reads
     * as one object settling (Apple keynote continuity).
     */
    private StackPane buildBrandMark() {
        M3Icon glyph = M3Icon.symbol("stats");
        recolorBrandGlyph(glyph, BRAND_GLYPH_SCALE);
        StackPane badge = new StackPane(glyph);
        badge.setAlignment(Pos.CENTER);
        badge.setMinSize(BRAND_MARK_SIZE, BRAND_MARK_SIZE);
        badge.setPrefSize(BRAND_MARK_SIZE, BRAND_MARK_SIZE);
        badge.setMaxSize(BRAND_MARK_SIZE, BRAND_MARK_SIZE);
        badge.setStyle("-fx-background-color: -md-sys-color-primary-container;"
                + " -fx-background-radius: 30px;");
        badge.setEffect(new DropShadow(BlurType.GAUSSIAN,
                Color.rgb(0, 0, 0, 0.4), 26, 0.2, 0, 10));
        return badge;
    }

    /**
     * Forces the brand glyph to the primary-container foreground color and scales it up inside
     * its badge. The library CSS paints every icon {@code on-surface-variant} by default, and
     * CSS beats a programmatic fill — so the override goes on the SVGPath's inline style.
     * Defensive: if the internals ever change shape the default fill still renders.
     */
    private void recolorBrandGlyph(M3Icon glyph, double scale) {
        if (!glyph.getChildren().isEmpty() && glyph.getChildren().get(0) instanceof SVGPath svg) {
            svg.setStyle("-fx-fill: -md-sys-color-on-primary-container;");
            svg.setScaleX(scale);
            svg.setScaleY(scale);
        }
    }

    // =====================================================================================
    // Splash choreography
    // =====================================================================================

    /** Determinate 0→1 progress over ~1.4s while the dashboard prewarms between beats. */
    private void playSplashBeats() {
        if (gateOverlay == null || splashProgress == null) {
            return;
        }
        splashProgress.setProgress(0.04);
        Timeline beats = new Timeline(
                new KeyFrame(Duration.millis(260),
                        e -> prewarmDashboard(),
                        new KeyValue(splashProgress.progressProperty(), 0.34)),
                new KeyFrame(Duration.millis(700),
                        new KeyValue(splashProgress.progressProperty(), 0.62)),
                new KeyFrame(Duration.millis(1050),
                        new KeyValue(splashProgress.progressProperty(), 0.86)),
                new KeyFrame(Duration.millis(1400),
                        e -> finishSplash(),
                        new KeyValue(splashProgress.progressProperty(), 1.0)));
        beats.play();
    }

    /** Prewarms the shell's first screen so the post-morph shell paints instantly. */
    private void prewarmDashboard() {
        try {
            shell.loadScreen(Screen.DASHBOARD);
        } catch (Throwable t) {
            System.err.println("Launch gates: dashboard prewarm skipped (" + t + ")");
        }
    }

    /**
     * Splash → gate handoff. The text block dissolves via {@link MotionUtil#splashOut} while
     * the brand mark stays put; then the brand is reparented into the login card and springs
     * to its slot as the card blooms around it.
     */
    private void finishSplash() {
        if (gateOverlay == null || splashText == null) {
            return;
        }
        MotionUtil.splashOut(splashText, () -> {
            // First statement: hide before splashOut's reset can repaint the dissolved text.
            splashLayer.setVisible(false);
            handBrandIntoCard();
            gateLayer.setVisible(true);
            loginCard.setVisible(true);
            MotionUtil.splashIn(loginCard);
            Platform.runLater(this::settleBrandIntoCard);
            focusUsernameAfterLayout();
        });
    }

    /** Scene position of the brand mark, captured just before the splash hands it to the card. */
    private double lastBrandSceneX;
    private double lastBrandSceneY;

    /** Moves the single brand mark node from the splash layer into the card's slot. */
    private void handBrandIntoCard() {
        try {
            if (brandMark != null && brandMark.getParent() != null) {
                Bounds inScene = brandMark.localToScene(brandMark.getBoundsInLocal());
                lastBrandSceneX = inScene.getMinX();
                lastBrandSceneY = inScene.getMinY();
                ((Pane) brandMark.getParent()).getChildren().remove(brandMark);
            }
            if (brandSlot != null) {
                if (brandMark == null) {
                    brandMark = buildBrandMark(); // defensive: continuity lost, rebuild identical
                }
                brandSlot.getChildren().add(brandMark);
            }
        } catch (Throwable t) {
            System.err.println("Launch gates: brand handoff fell back to a rebuilt mark (" + t + ")");
            if (brandSlot != null && brandSlot.getChildren().isEmpty()) {
                brandMark = buildBrandMark();
                brandSlot.getChildren().add(brandMark);
            }
        }
    }

    /**
     * Springs the brand mark from its old (splash) screen position to its rest position inside
     * the card: measure both scene positions, offset by the delta, then let the spring drive
     * the translate back to 0. Transform-only, so it never disturbs the card's layout.
     */
    private void settleBrandIntoCard() {
        try {
            if (brandMark == null || brandMark.getScene() == null) {
                return;
            }
            // The gate layer only became visible this pulse — the queued runnable can run
            // BEFORE the card's first real layout pass, so force one (idempotent when clean).
            if (brandMark.getScene().getRoot() instanceof Pane rootPane) {
                rootPane.applyCss();
                rootPane.layout();
            }
            Bounds now = brandMark.localToScene(brandMark.getBoundsInLocal());
            brandMark.setTranslateX(lastBrandSceneX - now.getMinX());
            brandMark.setTranslateY(lastBrandSceneY - now.getMinY());
            SpringAnimation.animate(brandMark.translateXProperty(), 0,
                    SpringAnimation.APPLE_SETTLE, null);
            SpringAnimation.animate(brandMark.translateYProperty(), 0,
                    SpringAnimation.APPLE_SETTLE, null);
        } catch (Throwable t) {
            System.err.println("Launch gates: brand settle skipped (" + t + ")");
        }
    }

    private void focusUsernameAfterLayout() {
        Platform.runLater(() -> {
            if (usernameField != null && usernameField.getScene() != null) {
                usernameField.getInput().requestFocus();
            }
        });
    }

    // =====================================================================================
    // Validation + sign-in
    // =====================================================================================

    private void resetGateState() {
        gateGeneration++;
        if (usernameField != null) {
            usernameField.getInput().setText("");
            usernameField.setError(false);
        }
        syncingPassword = true;
        realPassword.set("");
        if (passwordPlain != null) {
            passwordPlain.setText("");
        }
        if (passwordMasked != null) {
            passwordMasked.setText("");
        }
        syncingPassword = false;
        if (passwordField != null) {
            passwordField.setError(false);
        }
        if (passwordHidden == false) {
            togglePasswordVisibility(); // always come back masked
        }
        clearError();
        signingIn = false;
        if (signInButton != null) {
            signInButton.setDisable(false);
            signInButton.setText("Sign in");
        }
        if (signInBeat != null) {
            signInBeat.setVisible(false);
            signInBeat.setManaged(false);
            signInBeat.setProgress(0);
        }
    }

    private void clearError() {
        if (errorCaption != null) {
            errorCaption.setVisible(false);
            errorCaption.setManaged(false);
        }
        if (usernameField != null) {
            usernameField.setError(false);
        }
        if (passwordField != null) {
            passwordField.setError(false);
        }
    }

    /** Apple-style inline failure: spring shake of the card + an error caption. No dialogs. */
    private void showValidationError(String message, M3TextField offendingField) {
        errorCaption.setText(message);
        errorCaption.setVisible(true);
        errorCaption.setManaged(true);
        if (offendingField != null) {
            offendingField.setError(true);
        }
        MotionUtil.slideIn(errorCaption, 6, 120);
        MotionUtil.shake(loginCard);
    }

    /**
     * Real sign-in (rule 16): light client-side checks for instant feedback, then the actual
     * credential check runs off the FX thread via {@link Data#submit} →
     * {@link Data#authenticate}. Success completes the morph and publishes the signed-in
     * identity to the shell; every failure shakes the card with short sentence-case copy
     * (wrong credentials highlight the password field, an inactive account blames nobody).
     */
    private void attemptSignIn() {
        if (gateOverlay == null || signingIn || loginCard == null || !loginCard.isVisible()) {
            return;
        }
        String username = usernameField.getInput().getText() == null
                ? "" : usernameField.getInput().getText().trim();
        String password = realPassword.get() == null ? "" : realPassword.get();
        clearError();

        if (username.isEmpty()) {
            showValidationError("Enter your username to continue.", usernameField);
            usernameField.getInput().requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showValidationError("Enter your password to continue.", passwordField);
            return;
        }

        // "Signing in…" beat: button disabled, thin progress line as motion feedback while the
        // real authentication runs on a background task (it may also bootstrap the database on
        // the very first run, which takes a moment). The outcome comes from the callbacks.
        signingIn = true;
        signInButton.setDisable(true);
        signInButton.setText("Signing in…");
        signInBeat.setVisible(true);
        signInBeat.setManaged(true);
        signInBeat.setProgress(0);
        Timeline beat = new Timeline(
                new KeyFrame(Duration.millis(700),
                        new KeyValue(signInBeat.progressProperty(), 1.0)));
        beat.play();

        final int generation = gateGeneration;
        Data.submit(
                () -> Data.authenticate(username, password),
                result -> {
                    if (generation != gateGeneration || gateOverlay == null) {
                        return; // gate was reset or removed while the check ran
                    }
                    if (result.kind() == Data.AuthResult.Status.OK) {
                        shell.setSignedInUser(result.fullName(), result.role());
                        completeSignIn();
                        return;
                    }
                    signInFailure(result.error(),
                            result.kind() == Data.AuthResult.Status.INVALID_CREDENTIALS
                                    ? passwordField : null);
                },
                error -> {
                    if (generation != gateGeneration || gateOverlay == null) {
                        return;
                    }
                    signInFailure("Sign-in is unavailable right now — try again.", null);
                });
    }

    /** Rolls the gate back to its editable state and surfaces the failure copy. */
    private void signInFailure(String message, M3TextField offendingField) {
        signingIn = false;
        signInButton.setDisable(false);
        signInButton.setText("Sign in");
        signInBeat.setVisible(false);
        signInBeat.setManaged(false);
        signInBeat.setProgress(0);
        showValidationError(message, offendingField);
    }

    // =====================================================================================
    // Success morph: card → whole app
    // =====================================================================================

    /**
     * The showpiece. Captures the login card's scene bounds, lets the shell morph out of that
     * footprint ({@link MotionUtil#morphFromBounds}, transform-only) while the gate veil lifts,
     * then removes the overlay entirely. Any failure falls back to a plain fade so the user is
     * never stranded behind the gate.
     */
    private void completeSignIn() {
        if (gateOverlay == null || loginCard == null) {
            return;
        }
        try {
            Bounds cardBounds = loginCard.localToScene(loginCard.getBoundsInLocal());
            Region morphTarget = shell.getMorphTarget();
            if (morphTarget == null || morphTarget.getScene() == null
                    || cardBounds.getWidth() <= 0 || cardBounds.getHeight() <= 0) {
                throw new IllegalStateException("morph target or card bounds unavailable");
            }
            loginCard.setVisible(false); // the shell now "is" the card expanding
            MotionUtil.morphFromBounds(morphTarget, cardBounds, true, this::afterMorph);
            liftVeil();
        } catch (Throwable t) {
            System.err.println("Launch gates: morph unavailable, using fade (" + t + ")");
            MotionUtil.exit(gateOverlay, this::removeOverlay);
        }
    }

    /** Fades the gate scrim away while the shell is morphing beneath it. */
    private void liftVeil() {
        if (gateOverlay == null) {
            return;
        }
        stopOverlayFade();
        overlayFade = new FadeTransition(Duration.millis(260), gateOverlay);
        overlayFade.setToValue(0.0);
        overlayFade.setOnFinished(e -> {
            overlayFade = null;
            removeOverlay();
        });
        overlayFade.play();
    }

    /** Stops the in-flight veil fade (if any) so a new one always starts from a clean state. */
    private void stopOverlayFade() {
        FadeTransition fade = overlayFade;
        overlayFade = null;
        if (fade != null) {
            fade.stop();
        }
    }

    /** Runs when the shell has fully expanded: clean up, settle, done. */
    private void afterMorph() {
        removeOverlay();
        try {
            Region morphTarget = shell.getMorphTarget();
            if (morphTarget != null) {
                MotionUtil.cancelNodeMotion(morphTarget); // defensive identity
            }
            shell.settleCurrentPage(); // re-enters via MotionUtil.enter(page, UP, 1.0) if needed
        } catch (Throwable t) {
            System.err.println("Launch gates: post-morph settle skipped (" + t + ")");
        }
    }

    /** Removes the overlay from the scene root and drops every reference to it. */
    private void removeOverlay() {
        stopOverlayFade();
        if (gateOverlay != null && gateOverlay.getParent() instanceof Pane parent) {
            parent.getChildren().remove(gateOverlay);
        }
        gateOverlay = null;
        gateShowing = false;
        signingIn = false;
    }
}
