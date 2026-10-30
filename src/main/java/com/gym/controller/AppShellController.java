package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.command.M3CommandPalette;
import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.internal.MotionUtil;
import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.nav.M3NavigationRail;
import io.m3fx.controls.selection.M3Switch;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller skeleton for Adaptive Application Shell (app-shell.fxml).
 * Conforms to docs/ui/shell/APP_SHELL_UI.md Section 7 contract.
 * Pure UI stub: strictly no backend service, DAO, or database logic.
 *
 * <p>Motion: page swaps stay synchronous (the new page is in the scene graph as soon as
 * {@link #loadScreen(String)} returns; the fluid enter animation is applied afterwards),
 * and the navigation rail carries an Apple-style highlight pill that springs between
 * destinations. The pill is created in code with no fx:id and no new @FXML fields so the
 * FXML binding contract counts are untouched.
 */
public class AppShellController {

    /** Mirrors the theme currently applied to the scene; GymApp starts with the dark scheme. */
    private boolean darkMode = true;

    // Motion state (plain fields on purpose: no @FXML annotation, so the pinned
    // controller binding counts in ControllerBindingContractTest stay exact).
    /** Apple tab-bar style highlight that springs to the selected rail destination. */
    private Region navHighlightPill;
    /** True once the pill has snapped to its first valid geometry (later moves spring). */
    private boolean pillPlaced;
    /** The button the pill currently sits on — dedupes retargets from layoutBounds listeners. */
    private ToggleButton lastPillTarget;
    /** Last pill target geometry (for the retarget dedupe guard). */
    private double lastX, lastY, lastW, lastH;
    /** Spring-driven pill size (a Region's width/height are read-only during layout). */
    private final SimpleDoubleProperty pillWidth = new SimpleDoubleProperty();
    private final SimpleDoubleProperty pillHeight = new SimpleDoubleProperty();

    /** Compact icon button displayed in the rail header when collapsed. */
    private M3IconButton collapsedActionBtn;
    /** Cached list of all navigation destination toggle buttons. */
    private final List<ToggleButton> navButtons = new ArrayList<>();
    /** Rail collapsed state. */
    private boolean railCollapsed = false;

    /**
     * The Apple-style launch gate (splash → login → morph), created lazily and reused for
     * sign-out. Plain field on purpose: no @FXML annotation, so the pinned controller binding
     * counts in ControllerBindingContractTest stay exact. Only touched when
     * {@code GymApp.SHOW_LAUNCH_GATES} is enabled; tests never construct it.
     */
    private LaunchGateController launchGate;

    @FXML private BorderPane rootShellPane;
    @FXML private M3TopAppBar topAppBar;
    @FXML private M3IconButton railToggleBtn;
    @FXML private HBox searchTriggerBar;
    @FXML private M3PulseBeacon dbSyncBeacon;
    @FXML private M3Switch themeModeSwitch;
    @FXML private M3Chip userRoleChip;
    @FXML private M3IconButton logoutBtn;

    @FXML private M3NavigationRail navigationRail;
    @FXML private M3SplitButton globalActionSplitBtn;
    @FXML private ToggleButton navDashboard;
    @FXML private ToggleButton navCustomers;
    @FXML private ToggleButton navTrainers;
    @FXML private ToggleButton navSchedule;
    @FXML private ToggleButton navMemberships;
    @FXML private ToggleButton navEquipment;
    @FXML private ToggleButton navAttendance;
    @FXML private ToggleButton navPayments;
    @FXML private ToggleButton navStaff;
    @FXML private ToggleButton navHelp;

    @FXML private StackPane contentArea;
    @FXML private M3CommandPalette commandPalette;
    @FXML private StackPane globalSnackbar;

    public AppShellController() {}

    @FXML
    public void initialize() {
        if (navigationRail != null) {
            setupNavigationRail();
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.ScrollPane railScroll = (javafx.scene.control.ScrollPane) navigationRail.lookup(".rail-scroll-pane");
                if (railScroll != null) {
                    enableSmoothScrolling(railScroll);
                }
                setupNavRailMotion();
            });
        }
        loadScreen("dashboard.fxml");
        initCommandPalette();
    }

    private void setupNavigationRail() {
        navButtons.clear();
        if (navDashboard != null) navButtons.add(navDashboard);
        if (navCustomers != null) navButtons.add(navCustomers);
        if (navTrainers != null) navButtons.add(navTrainers);
        if (navSchedule != null) navButtons.add(navSchedule);
        if (navMemberships != null) navButtons.add(navMemberships);
        if (navEquipment != null) navButtons.add(navEquipment);
        if (navAttendance != null) navButtons.add(navAttendance);
        if (navPayments != null) navButtons.add(navPayments);
        if (navStaff != null) navButtons.add(navStaff);
        if (navHelp != null) navButtons.add(navHelp);

        for (ToggleButton btn : navButtons) {
            btn.setMinHeight(48.0);
            btn.setPrefHeight(48.0);
            btn.setGraphicTextGap(12.0);
            Tooltip.install(btn, new Tooltip(btn.getText()));
        }

        if (globalActionSplitBtn != null && globalActionSplitBtn.getParent() instanceof javafx.scene.layout.Pane actionContainer) {
            collapsedActionBtn = new M3IconButton(M3Icon.symbol("bolt"), IconButtonVariant.FILLED);
            collapsedActionBtn.setAccessibleText("Fast Check-In");
            Tooltip.install(collapsedActionBtn, new Tooltip("Fast Check-In"));
            collapsedActionBtn.setOnAction(e -> onFastCheckIn());
            collapsedActionBtn.setVisible(false);
            collapsedActionBtn.setManaged(false);
            actionContainer.getChildren().add(collapsedActionBtn);
        }
    }

    /**
     * Starts the Apple-style launch choreography (splash → login gate → fluid morph into this
     * shell). Called by {@code GymApp.start} only when {@code GymApp.SHOW_LAUNCH_GATES} is
     * enabled, so the FXML-based tests, which call {@code start} directly, never enter here.
     */
    public void beginLaunchGates() {
        launchGate().beginLaunchGates();
    }

    /**
     * The node the launch gate morphs out of the login card: this shell's whole layout surface
     * (top bar + rail + workspace), so the card visibly expands into the entire app.
     */
    public javafx.scene.layout.Region getMorphTarget() {
        return rootShellPane;
    }

    /**
     * Ensures the currently displayed page is fully settled (identity transforms). Used by the
     * launch gate after the morph; a page that is mid-animation is re-entered with the standard
     * full-scale Apple entrance so it never ends up half-faded behind the gate.
     */
    public void settleCurrentPage() {
        if (contentArea == null || contentArea.getChildren().isEmpty()) {
            return;
        }
        javafx.scene.Node page = contentArea.getChildren().get(0);
        if (page.getOpacity() < 1.0 || page.getTranslateX() != 0 || page.getTranslateY() != 0
                || page.getScaleX() != 1.0 || page.getScaleY() != 1.0) {
            MotionUtil.enter(page, MotionUtil.MotionDirection.UP, 1.0);
        }
    }

    /** Lazily creates (once) and returns the shared launch gate. */
    private LaunchGateController launchGate() {
        if (launchGate == null) {
            launchGate = new LaunchGateController(this);
        }
        return launchGate;
    }

    public void loadScreen(String fxmlName) {
        if (contentArea == null) return;
        try {
            java.net.URL url = getClass().getResource("/fxml/" + fxmlName);
            if (url != null) {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(url);
                javafx.scene.Parent view = loader.load();

                // Clear any rigid prefHeight from root so it can expand dynamically. The min-height
                // stretch applies ONLY to roots that are not already a ScrollPane: pinning a
                // ScrollPane's min height to its own pref pins it to the FULL content height,
                // which defeats its scrolling and forces enormous layout passes (the reported
                // trainer-tab freeze).
                if (view instanceof javafx.scene.layout.Region region) {
                    region.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                    if (!containsScrollPane(view)) {
                        region.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
                    }
                }

                javafx.scene.Node page;
                if (containsScrollPane(view)) {
                    // View already has its own dedicated ScrollPane layout
                    applySmoothScrolling(view);
                    contentArea.getChildren().setAll(view);
                    page = view;
                } else {
                    // View needs smooth vertical scrolling
                    javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(view);
                    sp.setFitToWidth(true);
                    sp.setFitToHeight(false);
                    sp.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
                    sp.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    sp.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                    enableSmoothScrolling(sp);
                    contentArea.getChildren().setAll(sp);
                    page = sp;
                }

                // Fluid entrance AFTER the synchronous swap: the page node is already in the
                // scene graph, so callers (and tests) that inspect state right after this
                // method returns always see the final navigation result. Exactly one page
                // child is ever present, so rapid navigation can never stack duplicates.
                MotionUtil.enter(page, MotionUtil.MotionDirection.UP, 1.0);
            }
        } catch (Exception e) {
            System.err.println("Failed to load /fxml/" + fxmlName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applySmoothScrolling(javafx.scene.Node node) {
        if (node instanceof javafx.scene.control.ScrollPane sp) {
            enableSmoothScrolling(sp);
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                applySmoothScrolling(child);
            }
        }
    }

    public static void enableSmoothScrolling(javafx.scene.control.ScrollPane sp) {
        if (sp == null) return;
        sp.setPannable(true);
        sp.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            double deltaY = event.getDeltaY();
            if (deltaY == 0) return;

            javafx.scene.Node content = sp.getContent();
            if (content == null) return;

            double contentHeight = content.getBoundsInLocal().getHeight();
            double viewportHeight = sp.getViewportBounds().getHeight();
            if (contentHeight <= viewportHeight) return;

            // Responsive 48px natural mouse-wheel step with direction handling
            double step = (deltaY > 0 ? -1.0 : 1.0) * (48.0 / (contentHeight - viewportHeight));
            double newVvalue = Math.max(0.0, Math.min(1.0, sp.getVvalue() + step));
            sp.setVvalue(newVvalue);
            event.consume();
        });
    }

    private boolean containsScrollPane(javafx.scene.Node node) {
        if (node instanceof javafx.scene.control.ScrollPane) return true;
        if (node instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                if (containsScrollPane(child)) return true;
            }
        }
        return false;
    }

    /**
     * Wires the Apple-style rail motion: a single highlight pill that springs to the selected
     * destination (like an iOS tab bar) and a subtle spring press-scale on every destination.
     * The pill is added in code (no fx:id, no @FXML field), is unmanaged (never disturbs the
     * rail layout), mouse-transparent (never blocks clicks or keyboard focus) and sits behind
     * the destination buttons. Failure here must never break shell setup.
     */
    private void setupNavRailMotion() {
        try {
            if (navigationRail == null || navDashboard == null || navHighlightPill != null) return;
            ScrollPane railScroll = findRailScrollPane();
            if (!(railScroll != null && railScroll.getContent() instanceof VBox buttonsBox)) return;
            ToggleGroup group = navDashboard.getToggleGroup();
            if (group == null) return;

            Region pill = new Region();
            pill.getStyleClass().add("app-shell-nav-highlight");
            pill.setManaged(false);
            pill.setMouseTransparent(true);
            pill.setVisible(false);
            pill.setStyle("-fx-background-color: -md-sys-color-secondary-container;"
                    + " -fx-background-radius: 14px; -fx-background-insets: 0;");
            // Painted first => renders behind every destination button (their backgrounds are
            // transparent, so the pill shows through like an Apple tab highlight). It lives
            // inside the scrolled content, so it rides along automatically when the rail scrolls.
            buttonsBox.getChildren().add(0, pill);
            navHighlightPill = pill;

            // Pill size is spring-driven through plain properties; resize applies them.
            pillWidth.addListener((obs, ov, nv) -> navHighlightPill.resize(nv.doubleValue(), pillHeight.get()));
            pillHeight.addListener((obs, ov, nv) -> navHighlightPill.resize(pillWidth.get(), nv.doubleValue()));

            for (Toggle toggle : group.getToggles()) {
                if (toggle instanceof ToggleButton button) {
                    // Physical press feedback on every destination (transform-only spring).
                    MotionUtil.addPressScale(button, 0.96);
                    // Geometry changes (rail width toggle, window resize, font metrics)
                    // re-target the pill without touching selection state.
                    button.layoutBoundsProperty().addListener((obs, ov, nv) -> {
                        if (pillPlaced && button.isSelected()) {
                            moveNavPill(button, true);
                        }
                    });
                }
            }
            group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                if (newToggle instanceof ToggleButton button) {
                    moveNavPill(button, true);
                }
            });
            if (group.getSelectedToggle() instanceof ToggleButton selected) {
                moveNavPill(selected, false);
            }
        } catch (Exception e) {
            System.err.println("Nav rail motion setup skipped: " + e.getMessage());
        }
    }

    private ScrollPane findRailScrollPane() {
        if (navigationRail == null) return null;
        if (navigationRail.lookup(".rail-scroll-pane") instanceof ScrollPane sp) return sp;
        for (javafx.scene.Node child : navigationRail.getChildrenUnmodifiable()) {
            if (child instanceof ScrollPane sp) return sp;
        }
        return null;
    }

    /**
     * Moves the highlight pill to the given destination. The first placement snaps (a pill
     * flying in from nowhere looks broken); every later move springs there with the soft
     * Apple settle tuning, preserving in-flight velocity when retargeted mid-morph.
     */
    private void moveNavPill(ToggleButton target, boolean animate) {
        Region pill = navHighlightPill;
        if (pill == null || target == null) return;
        Bounds bounds = target.getLayoutBounds();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) return; // not laid out yet

        // Pill and buttons share the same parent container, so the button's layout
        // coordinates are already in the pill's coordinate space.
        double x = target.getLayoutX();
        double y = target.getLayoutY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();

        // Retarget guard: an identical button with identical geometry must never restart the
        // springs. A pathological listener feedback (geometry change -> retarget -> geometry
        // change) would otherwise keep a spring alive forever, repainting the whole scene
        // every frame. Real geometry changes (rail toggle, window resize) still retarget.
        if (animate && pillPlaced && target == lastPillTarget
                && lastX == x && lastY == y && lastW == w && lastH == h) {
            return;
        }
        lastPillTarget = target;
        lastX = x;
        lastY = y;
        lastW = w;
        lastH = h;

        if (animate && pillPlaced) {
            pill.setVisible(true);
            SpringAnimation.animate(pill.translateXProperty(), x, SpringAnimation.APPLE_SETTLE, null);
            SpringAnimation.animate(pill.translateYProperty(), y, SpringAnimation.APPLE_SETTLE, null);
            SpringAnimation.animate(pillWidth, w, SpringAnimation.APPLE_SETTLE, null);
            SpringAnimation.animate(pillHeight, h, SpringAnimation.APPLE_SETTLE, null);
        } else {
            SpringAnimation.stop(pill.translateXProperty());
            SpringAnimation.stop(pill.translateYProperty());
            SpringAnimation.stop(pillWidth);
            SpringAnimation.stop(pillHeight);
            pill.setVisible(true);
            pill.setTranslateX(x);
            pill.setTranslateY(y);
            pillWidth.set(w);
            pillHeight.set(h);
            pillPlaced = true;
        }
    }

    private void initCommandPalette() {
        if (commandPalette == null) return;
        commandPalette.registerAction("Screens", "01. Authentication Vault (login.fxml)", "Sign In & Biometrics", "person", () -> {
            // Gates enabled: re-open the real login gate (same flow as sign-out). Otherwise the
            // legacy in-shell Authentication Vault screen stays available.
            if (com.gym.GymApp.SHOW_LAUNCH_GATES) {
                launchGate().showLoginGate();
            } else {
                loadScreen("login.fxml");
            }
        });
        commandPalette.registerAction("Screens", "02. Executive Dashboard (dashboard.fxml)", "Bento Operations Center", "home", () -> loadScreen("dashboard.fxml"));
        commandPalette.registerAction("Screens", "03. Member Directory (customer-list.fxml)", "Roster & Search Table", "group", () -> loadScreen("customer-list.fxml"));
        commandPalette.registerAction("Screens", "04. Athlete VIP Dossier (customer-profile.fxml)", "360 Profile & Hologram Pass", "person", () -> loadScreen("customer-profile.fxml"));
        commandPalette.registerAction("Screens", "05. Medical Profile (medical-profile.fxml)", "Injury Assessment & 3D Anatomy", "favorite", () -> loadScreen("medical-profile.fxml"));
        commandPalette.registerAction("Screens", "06. Athletic Progress (progress.fxml)", "VBT Scatter & 52-Week Heatmap", "stats", () -> loadScreen("progress.fxml"));
        commandPalette.registerAction("Screens", "07. Trainer Roster (trainer-list.fxml)", "Coaching Staff Roster", "person", () -> loadScreen("trainer-list.fxml"));
        commandPalette.registerAction("Screens", "08. Trainer Profile (trainer-profile.fxml)", "Coach Dossier & Capacity Gauge", "person", () -> loadScreen("trainer-profile.fxml"));
        commandPalette.registerAction("Screens", "09. Availability Matrix (availability.fxml)", "Weekly Booking & Slot Caps", "calendar", () -> loadScreen("availability.fxml"));
        commandPalette.registerAction("Screens", "10. Workout Builder (workout.fxml)", "Routine Pipeline & Exercises", "stats", () -> loadScreen("workout.fxml"));
        commandPalette.registerAction("Screens", "11. Live Workout HUD (live-workout-hud.fxml)", "Kinetic Rep Counter & Cardiac Zones", "time", () -> loadScreen("live-workout-hud.fxml"));
        commandPalette.registerAction("Screens", "12. Schedule Calendar (schedule.fxml)", "Class Agenda & Booking Drawer", "calendar", () -> loadScreen("schedule.fxml"));
        commandPalette.registerAction("Screens", "13. Equipment 3D Inventory (equipment.fxml)", "12 Procedural 3D Models & Maintenance", "settings", () -> loadScreen("equipment.fxml"));
        commandPalette.registerAction("Screens", "14. Gym Floorplan Map (gym-floor-map.fxml)", "2.5D Facility Blueprint & Occupancy", "home", () -> loadScreen("gym-floor-map.fxml"));
        commandPalette.registerAction("Screens", "15. Attendance Scanner (attendance.fxml)", "Turnstile Scanner & Peak Heatmap", "check", () -> loadScreen("attendance.fxml"));
        commandPalette.registerAction("Screens", "16. Membership Plans (membership.fxml)", "Tier Cards & Conversion Funnel", "favorite", () -> loadScreen("membership.fxml"));
        commandPalette.registerAction("Screens", "17. Cashless POS & Billing (payment.fxml)", "POS Terminal & Thermal Receipts", "money", () -> loadScreen("payment.fxml"));
        commandPalette.registerAction("Screens", "18. Staff RBAC Security (staff.fxml)", "Staff Permissions & Security Audit", "settings", () -> loadScreen("staff.fxml"));
    }

    @FXML
    public void onToggleRail() {
        if (navigationRail == null) return;
        railCollapsed = !railCollapsed;
        double targetWidth = railCollapsed ? 80.0 : 220.0;

        if (railCollapsed) {
            if (!navigationRail.getStyleClass().contains("collapsed")) {
                navigationRail.getStyleClass().add("collapsed");
            }
            for (ToggleButton btn : navButtons) {
                btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btn.setAlignment(Pos.CENTER);
            }
            if (globalActionSplitBtn != null) {
                globalActionSplitBtn.setVisible(false);
                globalActionSplitBtn.setManaged(false);
            }
            if (collapsedActionBtn != null) {
                collapsedActionBtn.setVisible(true);
                collapsedActionBtn.setManaged(true);
            }
        } else {
            navigationRail.getStyleClass().remove("collapsed");
            if (collapsedActionBtn != null) {
                collapsedActionBtn.setVisible(false);
                collapsedActionBtn.setManaged(false);
            }
            if (globalActionSplitBtn != null) {
                globalActionSplitBtn.setVisible(true);
                globalActionSplitBtn.setManaged(true);
            }
        }

        if (navigationRail.getScene() != null) {
            SpringAnimation.animate(navigationRail.prefWidthProperty(), targetWidth, SpringAnimation.FAST, () -> {
                navigationRail.setPrefWidth(targetWidth);
                if (!railCollapsed) {
                    for (ToggleButton btn : navButtons) {
                        btn.setContentDisplay(ContentDisplay.LEFT);
                        btn.setAlignment(Pos.CENTER_LEFT);
                    }
                }
                javafx.application.Platform.runLater(() -> {
                    if (navHighlightPill != null && lastPillTarget != null) {
                        moveNavPill(lastPillTarget, false);
                    }
                });
            });
        } else {
            navigationRail.setPrefWidth(targetWidth);
            if (!railCollapsed) {
                for (ToggleButton btn : navButtons) {
                    btn.setContentDisplay(ContentDisplay.LEFT);
                    btn.setAlignment(Pos.CENTER_LEFT);
                }
            }
            if (navHighlightPill != null && lastPillTarget != null) {
                moveNavPill(lastPillTarget, false);
            }
        }
    }

    @FXML
    public void onOpenCommandPalette() {
        if (commandPalette != null) {
            commandPalette.show();
        }
    }

    @FXML
    public void onToggleThemeMode() {
        if (rootShellPane != null && rootShellPane.getScene() != null) {
            // Flip our own state instead of reading the switch: the skin toggles the switch with
            // its own MOUSE_CLICKED handler, so the read could see either the old or new value.
            darkMode = !darkMode;
            javafx.scene.Scene scene = rootShellPane.getScene();
            javafx.scene.paint.Color seed = javafx.scene.paint.Color.web("#6750A4");
            io.m3fx.core.theme.M3Theme theme =
                    io.m3fx.core.theme.M3Theme.fromSeed(seed, io.m3fx.core.theme.ThemeVariant.TONAL_SPOT, darkMode);
            io.m3fx.controls.M3Stylesheets.applyTo(scene, theme);
        }
        if (themeModeSwitch != null) {
            // Keep the switch visual in sync with the applied scheme regardless of event order.
            themeModeSwitch.setSelected(darkMode);
        }
    }

    @FXML
    public void onOpenUserMenu() {
        onNavStaff();
    }

    @FXML
    public void onLogout() {
        // Real runs: sign out into the Apple-style login gate. Tests / gates-disabled runs keep
        // the original behavior (load the in-shell Authentication Vault screen).
        if (com.gym.GymApp.SHOW_LAUNCH_GATES) {
            launchGate().showLoginGate();
        } else {
            loadScreen("login.fxml");
        }
    }

    @FXML
    public void onFastCheckIn() {
        loadScreen("attendance.fxml");
    }

    @FXML
    public void onNavDashboard() {
        loadScreen("dashboard.fxml");
    }

    @FXML
    public void onNavCustomers() {
        loadScreen("customer-list.fxml");
    }

    @FXML
    public void onNavTrainers() {
        loadScreen("trainer-list.fxml");
    }

    @FXML
    public void onNavSchedule() {
        loadScreen("schedule.fxml");
    }

    @FXML
    public void onNavMemberships() {
        loadScreen("membership.fxml");
    }

    @FXML
    public void onNavEquipment() {
        loadScreen("equipment.fxml");
    }

    @FXML
    public void onNavAttendance() {
        loadScreen("attendance.fxml");
    }

    @FXML
    public void onNavPayments() {
        loadScreen("payment.fxml");
    }

    @FXML
    public void onNavStaff() {
        loadScreen("staff.fxml");
    }

    @FXML
    public void onNavHelp() {
        loadScreen("gym-floor-map.fxml");
    }
}
