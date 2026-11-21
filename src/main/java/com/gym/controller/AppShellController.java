package com.gym.controller;

import com.gym.model.StaffRole;
import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.appbar.TopAppBarVariant;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.command.M3CommandPalette;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.controls.glass.GlassEffectParams;
import io.m3fx.controls.glass.GlassShape;
import io.m3fx.controls.glass.M3GlassPane;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.internal.MotionUtil;
import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.layout.WindowSizeClass;
import io.m3fx.controls.list.M3ListItem;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.nav.M3NavDestination;
import io.m3fx.controls.nav.M3NavigationBar;
import io.m3fx.controls.nav.M3NavigationRail;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.selection.M3Switch;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for the Adaptive Application Shell — the host of all 19 Gym-OS screens.
 * Conforms to docs/ui/shell/APP_SHELL_UI.md Section 7 contract.
 * Pure UI stub: strictly no backend service, DAO, or database logic.
 *
 * <p>The shell builds its whole node tree in Java ({@link #buildView()}), the programmatic
 * replacement for the former app-shell.fxml: same nodes, same style classes, same handler
 * wiring. Screen navigation goes through the {@link Screen} registry instead of an
 * {@code FXMLLoader}: {@link #loadScreen(Screen)} instantiates a screen controller, which
 * builds its own view, and the shell mounts the returned root in the workspace.
 *
 * <p>Motion: page swaps stay synchronous (the new page is in the scene graph as soon as
 * {@link #loadScreen(Screen)} returns; the fluid enter animation is applied afterwards),
 * and the navigation rail carries an Apple-style highlight pill that springs between
 * destinations.
 */
public class AppShellController implements ScreenController {

    /**
     * Scene-root property key under which this shell publishes itself so screen controllers can
     * reach it with a guarded, static-free lookup (the same key
     * {@code GymAppFullInteractiveAuditTest} reads). Plain constant, never mutable state.
     */
    public static final String SHELL_CONTROLLER_KEY = "appShellController";

    /**
     * Corner radius of the command palette's glass surface, matching the
     * {@code .m3-command-palette-card} stylesheet radius (16px) so the floating card keeps its
     * established geometry when its opaque surface moves onto liquid glass.
     */
    private static final double COMMAND_PALETTE_GLASS_RADIUS = 16;

    /** Mirrors the theme currently applied to the scene; GymApp starts with the dark scheme. */
    private boolean darkMode = true;

    // Motion state (plain fields).
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

    // ---- Responsive shell state (docs/SPEC_NOTES/app-ui-foundation.md §6.1) ----
    /** Bottom navigation bar shown only in the compact width class. */
    private M3NavigationBar navBar;
    /** Index within the compact bar's primary destinations of the current screen. */
    private int compactPrimaryIndex;
    /** Current window width class (library breakpoints; "large" is the >= 1200 refinement). */
    private WindowSizeClass widthClass = WindowSizeClass.EXPANDED;
    /** True inside EXPANDED once the window is >= 1200 (wider workspace padding). */
    private boolean largeWindow;
    /** User override of the rail state; null = follow the current width class default. */
    private Boolean railOverride;
    /** The compact bar's primary destinations (rail keys, bar order). */
    private static final String[] COMPACT_PRIMARY_KEYS = {
        "navDashboard", "navCustomers", "navTrainers", "navSchedule"};

    /**
     * The Apple-style launch gate (splash → login → morph), created lazily and reused for
     * sign-out. Only touched when {@code GymApp.SHOW_LAUNCH_GATES} is enabled; tests never
     * construct it.
     */
    private LaunchGateController launchGate;

    /**
     * Controller instance created by the most recent successful {@link #loadScreen(Screen)} call,
     * so cross-screen handoffs ({@link #openScreenViaRail}) can pass data to the freshly loaded
     * screen.
     */
    private Object lastLoadedController;

    /**
     * Lazy snackbar host for shell-level feedback (failed screen loads). Attached to the scene
     * root on first use, mirroring the {@code installFeedbackLayer} pattern of the trainer
     * screens.
     */
    private M3Overlay shellFeedback;

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private BorderPane rootShellPane;
    private M3TopAppBar topAppBar;
    private M3IconButton railToggleBtn;
    private HBox topRow;
    private HBox searchTriggerBar;
    private HBox telemetryPill;
    private M3PulseBeacon dbSyncBeacon;
    private M3Switch themeModeSwitch;
    /** Q1: drives MotionTokens.setReducedMotion live (no restart). */
    private M3Switch reducedMotionSwitch;
    /** Q1: drives MotionTokens.setMotionScheme live (no restart). */
    private M3Switch schemeSwitch;
    private M3Chip userRoleChip;
    /** The Ctrl+K badge inside the search trigger; hidden when the cluster is tight. */
    private Label shortcutBadge;
    /** The signed-in identity from the login gate (null until a gate sign-in publishes it). */
    private String signedInName;
    private StaffRole signedInRole;
    private M3IconButton logoutBtn;

    private M3NavigationRail navigationRail;
    private M3SplitButton globalActionSplitBtn;
    private ToggleButton navDashboard;
    private ToggleButton navCustomers;
    private ToggleButton navTrainers;
    private ToggleButton navSchedule;
    private ToggleButton navMemberships;
    private ToggleButton navEquipment;
    private ToggleButton navAttendance;
    private ToggleButton navPayments;
    private ToggleButton navStaff;
    private ToggleButton navHelp;

    private StackPane contentArea;
    private M3CommandPalette commandPalette;
    private StackPane globalSnackbar;

    /**
     * Builds the complete shell view and initializes all behavior. Same ordering semantics the
     * FXMLLoader used to provide: the node tree exists and every field is assigned before the
     * former {@code initialize()} work runs.
     */
    public AppShellController() {
        rootShellPane = new BorderPane();
        // B/Q4: allow the shell to shrink below its content's natural minimums — the top
        // bar chain (title label, search trigger, system cluster) pins ~876px otherwise,
        // which kept the responsive chrome from ever reaching the compact class and
        // rendered 600/840 windows as a clipped expanded shell.
        rootShellPane.setMinWidth(0);

        // TOP: glass canopy top app bar container.
        StackPane appBarContainer = new StackPane();
        appBarContainer.getStyleClass().add("glass-app-bar-container");

        topAppBar = new M3TopAppBar("GYM-OS // ENTERPRISE 2026", TopAppBarVariant.SMALL);
        topAppBar.setMinWidth(0);
        // Q4: the title label must not pin the top-bar minimum — it ellipsizes instead.
        Label shellTitle = (Label) topAppBar.lookup(".m3-top-app-bar-title");
        if (shellTitle != null) {
            shellTitle.setMinWidth(0);
        }
        topAppBar.setPrefHeight(64.0);
        topAppBar.getStyleClass().add("glass-app-bar");

        railToggleBtn = new M3IconButton(M3Icon.symbol("menu"), IconButtonVariant.STANDARD);
        railToggleBtn.setOnAction(e -> onToggleRail());
        topAppBar.setNavigationIcon(railToggleBtn);

        // Center + Right: search trigger and system controls share ONE row so the search
        // field can never slide underneath the telemetry/user cluster at narrow widths.
        topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPickOnBounds(false);
        topRow.setPadding(new Insets(0, 16, 0, 300));

        searchTriggerBar = new HBox(12);
        searchTriggerBar.setAlignment(Pos.CENTER_LEFT);
        searchTriggerBar.getStyleClass().add("global-search-trigger");
        searchTriggerBar.setMaxWidth(380);
        HBox.setHgrow(searchTriggerBar, Priority.ALWAYS);
        searchTriggerBar.setOnMouseClicked(e -> onOpenCommandPalette());
        // B/Q3: the search trigger is keyboard operable too (Tab to it, Enter/Space opens
        // the palette; Ctrl+K works anywhere in the scene via the scene accelerator).
        searchTriggerBar.setFocusTraversable(true);
        searchTriggerBar.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER
                    || e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                onOpenCommandPalette();
                e.consume();
            }
        });
        searchTriggerBar.setAccessibleText(
                "Quick Command or Search — opens the command palette (Ctrl+K)");
        Region searchSpacer = new Region();
        HBox.setHgrow(searchSpacer, Priority.ALWAYS);
        searchTriggerBar.getChildren().addAll(
                M3Icon.symbol("search"),
                placeholderLabel("Quick Command or Search...", "search-placeholder"),
                searchSpacer,
                shortcutBadge = placeholderLabel("Ctrl + K", "shortcut-badge"));

        Region topRowSpacer = new Region();
        HBox.setHgrow(topRowSpacer, Priority.ALWAYS);

        HBox systemCluster = new HBox(16);
        systemCluster.setAlignment(Pos.CENTER_RIGHT);

        telemetryPill = new HBox(8);
        telemetryPill.setAlignment(Pos.CENTER);
        telemetryPill.getStyleClass().add("telemetry-pill");
        telemetryPill.setId("telemetryPill");
        dbSyncBeacon = new M3PulseBeacon();
        dbSyncBeacon.setStatus(M3PulseBeacon.BeaconStatus.ONLINE);
        telemetryPill.getChildren().addAll(
                dbSyncBeacon,
                placeholderLabel("DB: WAL ONLINE", "telemetry-text"));

        themeModeSwitch = new M3Switch(true);
        // B/Q3: the theme toggle fires on the selection change, so keyboard Space toggles
        // (M3Switch's own key handling) re-theme the app exactly like mouse clicks do.
        themeModeSwitch.selectedProperty().addListener((obs, o, sel) -> onToggleThemeMode());

        // B/Q1: shell motion controls (live, no restart). The library reads both flags per
        // call, so MotionUtil/SpringAnimation degrade or re-resolve the moment they flip:
        // reduced motion turns springs into snaps and screen/dialog transitions into <=150ms
        // fades (MotionUtil.isReducedMotion guards), and the scheme selects the spring
        // language (SPATIAL_STANDARD_* vs SPATIAL_EXPRESSIVE_*). Initial values come from
        // the m3fx.reducedMotion / m3fx.motionScheme system properties via the token layer.
        reducedMotionSwitch = new M3Switch(io.m3fx.core.token.MotionTokens.isReducedMotion());
        reducedMotionSwitch.getStyleClass().add("motion-switch");
        reducedMotionSwitch.setId("reducedMotionSwitch");
        reducedMotionSwitch.setAccessibleText("Reduce motion: springs snap and transitions "
                + "become short fades");
        reducedMotionSwitch.selectedProperty().addListener((obs, o, on) ->
                io.m3fx.core.token.MotionTokens.setReducedMotion(on));
        io.m3fx.controls.common.M3Tooltip.install(reducedMotionSwitch,
                "Reduce motion: springs snap, transitions become short fades (accessibility)");
        schemeSwitch = new M3Switch(
                io.m3fx.core.token.MotionTokens.getMotionScheme()
                        == io.m3fx.core.token.MotionTokens.MotionScheme.STANDARD);
        schemeSwitch.getStyleClass().add("motion-switch");
        schemeSwitch.setId("schemeSwitch");
        schemeSwitch.setAccessibleText("Standard springs: settle without bounce");
        schemeSwitch.selectedProperty().addListener((obs, o, standard) ->
                io.m3fx.core.token.MotionTokens.setMotionScheme(standard
                        ? io.m3fx.core.token.MotionTokens.MotionScheme.STANDARD
                        : io.m3fx.core.token.MotionTokens.MotionScheme.EXPRESSIVE));
        io.m3fx.controls.common.M3Tooltip.install(schemeSwitch,
                "Motion scheme: expressive springs bounce, standard springs settle");

        userRoleChip = new M3Chip("Alex Carter [ADMIN]", ChipVariant.ASSIST);
        userRoleChip.setOnAction(e -> onOpenUserMenu());

        logoutBtn = new M3IconButton(M3Icon.symbol("close"), IconButtonVariant.STANDARD);
        logoutBtn.setOnAction(e -> onLogout());

        systemCluster.getChildren().addAll(telemetryPill, reducedMotionSwitch, schemeSwitch,
                themeModeSwitch, userRoleChip, logoutBtn);
        topRow.getChildren().addAll(searchTriggerBar, topRowSpacer, systemCluster);
        appBarContainer.getChildren().addAll(topAppBar, topRow);
        rootShellPane.setTop(appBarContainer);

        // LEFT: expandable M3 navigation rail.
        navigationRail = new M3NavigationRail();
        navigationRail.setPrefWidth(220.0);
        navigationRail.getStyleClass().add("side-nav-rail");

        // Header fast action split button.
        VBox railActionContainer = new VBox();
        railActionContainer.setAlignment(Pos.CENTER);
        railActionContainer.getStyleClass().add("rail-action-container");
        railActionContainer.setPadding(new Insets(16.0, 8.0, 16.0, 8.0));
        globalActionSplitBtn = new M3SplitButton("FAST CHECK-IN", ButtonVariant.FILLED, null);
        globalActionSplitBtn.setOnAction(e -> onFastCheckIn());
        globalActionSplitBtn.setMaxWidth(Double.MAX_VALUE);
        railActionContainer.getChildren().add(globalActionSplitBtn);

        // Navigation destination items (smooth scrollable rail).
        ScrollPane railScroll = new ScrollPane();
        railScroll.setFitToWidth(true);
        railScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        railScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        railScroll.getStyleClass().add("rail-scroll-pane");
        railScroll.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(railScroll, Priority.ALWAYS);

        VBox railButtonsBox = new VBox(4);
        railButtonsBox.setAlignment(Pos.TOP_CENTER);
        railButtonsBox.setPadding(new Insets(0, 4.0, 64.0, 4.0));

        ToggleGroup railNavGroup = new ToggleGroup();
        navDashboard = railDestination("Dashboard", "home", railNavGroup, true, this::onNavDashboard);
        navCustomers = railDestination("Members", "group", railNavGroup, false, this::onNavCustomers);
        navTrainers = railDestination("Trainers", "person", railNavGroup, false, this::onNavTrainers);
        navSchedule = railDestination("Schedule", "calendar", railNavGroup, false, this::onNavSchedule);
        navMemberships = railDestination("Plans", "favorite", railNavGroup, false, this::onNavMemberships);
        navEquipment = railDestination("Equipment", "stats", railNavGroup, false, this::onNavEquipment);
        navAttendance = railDestination("Attendance", "check", railNavGroup, false, this::onNavAttendance);
        navPayments = railDestination("Finance", "money", railNavGroup, false, this::onNavPayments);
        navStaff = railDestination("Staff & Admin", "settings", railNavGroup, false, this::onNavStaff);
        navHelp = railDestination("Documentation", "info", railNavGroup, false, this::onNavHelp);
        railButtonsBox.getChildren().addAll(
                navDashboard, navCustomers, navTrainers, navSchedule, navMemberships,
                navEquipment, navAttendance, navPayments,
                new Separator(),
                navStaff, navHelp);
        railScroll.setContent(railButtonsBox);
        navigationRail.getChildren().addAll(railActionContainer, railScroll);
        rootShellPane.setLeft(navigationRail);

        // CENTER: main dynamic workspace viewport (dynamically injected active module views).
        contentArea = new StackPane();
        contentArea.getStyleClass().add("workspace-container");
        rootShellPane.setCenter(contentArea);

        viewRoot = new StackPane();
        viewRoot.getStyleClass().add("app-shell-root");
        viewRoot.getChildren().add(rootShellPane);

        // Floating overlays.
        commandPalette = new M3CommandPalette();
        commandPalette.setVisible(false);
        commandPalette.setManaged(false);

        globalSnackbar = new StackPane();
        globalSnackbar.setPickOnBounds(false);
        globalSnackbar.setMouseTransparent(true);
        globalSnackbar.setMaxSize(480.0, 48.0);
        StackPane.setAlignment(globalSnackbar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(globalSnackbar, new Insets(0, 0, 24.0, 0));
        viewRoot.getChildren().addAll(commandPalette, globalSnackbar);
        // B/Q3: Ctrl+K opens the command palette from anywhere in the shell scene (the
        // search placeholder promises it).
        viewRoot.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.K
                    && e.isControlDown() && !e.isConsumed()) {
                onOpenCommandPalette();
                e.consume();
            }
        });


    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        rootShellPane.setId("rootShellPane");
        topAppBar.setId("topAppBar");
        railToggleBtn.setId("railToggleBtn");
        searchTriggerBar.setId("searchTriggerBar");
        dbSyncBeacon.setId("dbSyncBeacon");
        themeModeSwitch.setId("themeModeSwitch");
        userRoleChip.setId("userRoleChip");
        logoutBtn.setId("logoutBtn");
        navigationRail.setId("navigationRail");
        globalActionSplitBtn.setId("globalActionSplitBtn");
        navDashboard.setId("navDashboard");
        navCustomers.setId("navCustomers");
        navTrainers.setId("navTrainers");
        navSchedule.setId("navSchedule");
        navMemberships.setId("navMemberships");
        navEquipment.setId("navEquipment");
        navAttendance.setId("navAttendance");
        navPayments.setId("navPayments");
        navStaff.setId("navStaff");
        navHelp.setId("navHelp");
        contentArea.setId("contentArea");
        commandPalette.setId("commandPalette");
        globalSnackbar.setId("globalSnackbar");
        initialize();
    }

    /** Small styled label used inside the search trigger and telemetry pill. */
    private static Label placeholderLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    /** Builds one navigation rail destination toggle button. */
    private static ToggleButton railDestination(String text, String symbolName, ToggleGroup group,
                                                boolean selected, Runnable action) {
        ToggleButton button = new ToggleButton(text, M3Icon.symbol(symbolName));
        button.setToggleGroup(group);
        button.setSelected(selected);
        button.getStyleClass().add("m3-navigation-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(e -> action.run());
        return button;
    }

    @Override
    public Parent getRoot() {
        return viewRoot;
    }

    private void initialize() {
        // Publish this shell in the scene-root properties once attached, so screen
        // controllers can find it with a guarded lookup (SHELL_CONTROLLER_KEY).
        rootShellPane.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null && scene.getRoot() != null) {
                scene.getRoot().getProperties().put(SHELL_CONTROLLER_KEY, this);
            }
        });
        setupNavigationRail();
        setupResponsiveChrome();
        javafx.application.Platform.runLater(() -> {
            ScrollPane railScroll = (ScrollPane) navigationRail.lookup(".rail-scroll-pane");
            if (railScroll != null) {
                enableSmoothScrolling(railScroll);
            }
            setupNavRailMotion();
        });
        loadScreen(Screen.DASHBOARD);
        initCommandPalette();
    }

    /**
     * Wires the adaptive chrome to the window width (breakpoints come from the library's
     * {@link WindowSizeClass#forWidth}): compact hides the rail and docks a bottom navigation
     * bar, medium defaults to the collapsed rail, expanded to the expanded rail, and
     * large (>= 1200) adds a wider workspace padding. Runs on every width change; the chrome
     * is only re-applied when the class or the large flag actually flips.
     */
    private void setupResponsiveChrome() {
        buildCompactNavBar();
        rootShellPane.widthProperty().addListener((obs, oldWidth, width) -> {
            double w = width.doubleValue();
            if (w <= 0) {
                return; // pre-layout event, no real width yet
            }
            WindowSizeClass next = WindowSizeClass.forWidth(w);
            boolean nextLarge = next == WindowSizeClass.EXPANDED && w >= 1200;
            if (next == widthClass && nextLarge == largeWindow) {
                return;
            }
            boolean classChanged = next != widthClass;
            widthClass = next;
            largeWindow = nextLarge;
            if (classChanged) {
                railOverride = null; // each class starts from its default
            }
            applyResponsiveChrome();
        });
    }

    /**
     * Applies the current width class to the shell chrome: rail visibility, bottom bar,
     * workspace padding, and the top row (whose 300px title clearance plus search and telemetry
     * cluster would otherwise pin the shell's minimum width above the compact breakpoints).
     * The command palette (Ctrl + K) stays the search answer where the search trigger is hidden.
     */
    private void applyResponsiveChrome() {
        boolean compact = widthClass == WindowSizeClass.COMPACT;
        boolean medium = widthClass == WindowSizeClass.MEDIUM;
        navigationRail.setVisible(!compact);
        navigationRail.setManaged(!compact);
        if (compact) {
            rootShellPane.setBottom(navBar);
        } else {
            rootShellPane.setBottom(null);
            boolean collapsed = railOverride != null ? railOverride : medium;
            applyRailCollapsed(collapsed);
        }
        // The search trigger needs the expanded title clearance (300px) to not overlap the app
        // bar title; below expanded the command palette (Ctrl + K) stays the search answer.
        boolean expanded = !compact && !medium;
        searchTriggerBar.setVisible(expanded);
        searchTriggerBar.setManaged(expanded);
        telemetryPill.setVisible(expanded);
        telemetryPill.setManaged(expanded);
        // Q4: the motion switches widened the system cluster — the Ctrl+K badge only fits
        // at large widths; below it the placeholder alone carries the affordance.
        boolean large = widthClass == WindowSizeClass.EXPANDED && largeWindow;
        shortcutBadge.setVisible(large);
        shortcutBadge.setManaged(large);
        // Q4: the title clearance now differs per class — 380px fits the long title at
        // expanded; below it the compact title ('GYM-OS', ~174px with the menu button)
        // needs 200px so the system cluster (two switches wider since Q1) never overlaps
        // it at 600-840 widths.
        topRow.setPadding(expanded ? new Insets(0, 16, 0, 380)
                : new Insets(0, 16, 0, 200));
        // The full product title runs under the system cluster at narrow widths; short forms
        // keep the app bar readable instead of overlapping it.
        // Q4: the medium top bar is too tight for the long title once the motion
        // switches share the system cluster — the short form reads better there too.
        topAppBar.setTitle(compact || medium ? "GYM-OS" : "GYM-OS // ENTERPRISE 2026");
        contentArea.setPadding(largeWindow && !compact ? new Insets(0, 12, 0, 12) : Insets.EMPTY);
    }

    /**
     * Builds the compact bottom navigation bar. The M3 spec allows 3-5 destinations per bar
     * (each cell is a fixed 64dp wide, so all 10 cannot fit a 600dp window), so the bar carries
     * the 4 primary destinations plus "More", whose menu holds the remaining 5 — every rail
     * destination stays reachable at compact width.
     */
    private void buildCompactNavBar() {
        List<M3NavDestination> destinations = new ArrayList<>();
        destinations.add(M3NavDestination.of("Dashboard", "home"));
        destinations.add(M3NavDestination.of("Members", "group"));
        destinations.add(M3NavDestination.of("Trainers", "person"));
        destinations.add(M3NavDestination.of("Schedule", "calendar"));
        destinations.add(M3NavDestination.of("More", "more"));
        navBar = new M3NavigationBar(destinations);
        navBar.setId("navBar");
        navBar.selectedIndexProperty().addListener((obs, oldIndex, index) ->
                onCompactNavSelected(index.intValue()));
    }

    /** Compact bar selection: the four primaries navigate, "More" opens the overflow menu. */
    private void onCompactNavSelected(int index) {
        if (index >= 0 && index < COMPACT_PRIMARY_KEYS.length) {
            compactPrimaryIndex = index;
            fireRailButton(COMPACT_PRIMARY_KEYS[index]);
            return;
        }
        openCompactMoreMenu();
    }

    /**
     * The compact overflow menu ("More"): the five destinations that do not fit the bar.
     * Choosing one fires the pinned rail destination (the same pathway as a rail click, so the
     * hidden rail's state stays consistent); on menu close the bar reverts to the last primary
     * destination because "More" itself is not a screen.
     */
    private void openCompactMoreMenu() {
        M3Menu menu = new M3Menu();
        menu.item("Plans", M3Icon.symbol("favorite"), () -> navigateFromCompact("navMemberships"));
        menu.item("Equipment", M3Icon.symbol("stats"), () -> navigateFromCompact("navEquipment"));
        menu.item("Attendance", M3Icon.symbol("check"), () -> navigateFromCompact("navAttendance"));
        menu.item("Finance", M3Icon.symbol("money"), () -> navigateFromCompact("navPayments"));
        menu.item("Staff & Admin", M3Icon.symbol("settings"), () -> navigateFromCompact("navStaff"));
        menu.divider();
        menu.item("Documentation", M3Icon.symbol("info"), this::onNavHelp);
        menu.popup().setOnHidden(e -> navBar.setSelectedIndex(compactPrimaryIndex));
        ToggleButton moreButton = compactNavBarButton(COMPACT_PRIMARY_KEYS.length);
        if (moreButton != null) {
            menu.showBelow(moreButton);
        }
    }

    /** Fires a hidden-rail destination on behalf of the compact chrome (loads the screen). */
    private void navigateFromCompact(String railButtonKey) {
        fireRailButton(railButtonKey);
    }

    /**
     * The bar's destination button at the given index (the library keeps the buttons as its
     * direct HBox children, in destination order).
     */
    private ToggleButton compactNavBarButton(int index) {
        List<ToggleButton> buttons = new ArrayList<>();
        for (Node child : navBar.getChildren()) {
            if (child instanceof ToggleButton button) {
                buttons.add(button);
            }
        }
        return index >= 0 && index < buttons.size() ? buttons.get(index) : null;
    }

    private void setupNavigationRail() {
        navButtons.clear();
        navButtons.add(navDashboard);
        navButtons.add(navCustomers);
        navButtons.add(navTrainers);
        navButtons.add(navSchedule);
        navButtons.add(navMemberships);
        navButtons.add(navEquipment);
        navButtons.add(navAttendance);
        navButtons.add(navPayments);
        navButtons.add(navStaff);
        navButtons.add(navHelp);

        for (ToggleButton btn : navButtons) {
            btn.setMinHeight(48.0);
            btn.setPrefHeight(48.0);
            btn.setGraphicTextGap(12.0);
            Tooltip.install(btn, new Tooltip(btn.getText()));
        }

        if (globalActionSplitBtn.getParent() instanceof javafx.scene.layout.Pane actionContainer) {
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
     * enabled, so the tests, which call {@code start} directly, never enter here.
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
        if (contentArea.getChildren().isEmpty()) {
            return;
        }
        Node page = contentArea.getChildren().get(0);
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

    /**
     * Navigates the shell to the given screen: instantiates its controller (which builds its
     * own view), clears any rigid pref-height constraint, mounts the view in the workspace
     * (inside a smooth-scrolling {@link ScrollPane} when the view has none of its own), and
     * plays the fluid entrance. Synchronous: the page is in the scene graph when this returns.
     */
    public void loadScreen(Screen screen) {
        try {
            ScreenController controller = screen.create();
            lastLoadedController = controller;
            Parent view = controller.getRoot();

            // Clear any rigid prefHeight from root so it can expand dynamically. The min-height
            // stretch applies ONLY to roots that are not already a ScrollPane: pinning a
            // ScrollPane's min height to its own pref pins it to the FULL content height,
            // which defeats its scrolling and forces enormous layout passes (the reported
            // trainer-tab freeze).
            if (view instanceof Region region) {
                region.setPrefHeight(Region.USE_COMPUTED_SIZE);
                if (!containsScrollPane(view)) {
                    region.setMinHeight(Region.USE_PREF_SIZE);
                }
            }

            Node page;
            if (containsScrollPane(view)) {
                // View already has its own dedicated ScrollPane layout
                applySmoothScrolling(view);
                contentArea.getChildren().setAll(view);
                page = view;
            } else {
                // View needs smooth vertical scrolling
                ScrollPane sp = new ScrollPane(view);
                sp.setFitToWidth(true);
                sp.setFitToHeight(false);
                sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                sp.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                enableSmoothScrolling(sp);
                contentArea.getChildren().setAll(sp);
                page = sp;
            }

            // Realize the page synchronously (CSS pass, which creates every control skin).
            // Without it, nodes that live under a Control whose skin is created lazily by the
            // CSS pass (ScrollPane, M3Card, M3DataTable, ...) are unreachable through
            // Node.lookup and getChildrenUnmodifiable traversal, so callers that inspect the
            // freshly loaded screen right away — screen controllers handing data over, tests
            // asserting the navigation result — see a half-detached subtree. This is
            // idempotent: the next pulse would run the same pass.
            page.applyCss();

            // Fluid entrance AFTER the synchronous swap: the page node is already in the
            // scene graph, so callers (and tests) that inspect state right after this
            // method returns always see the final navigation result. Exactly one page
            // child is ever present, so rapid navigation can never stack duplicates.
            MotionUtil.enter(page, MotionUtil.MotionDirection.UP, 1.0);
        } catch (Exception e) {
            System.err.println("Failed to load screen " + screen.key() + ": " + e.getMessage());
            e.printStackTrace();
            showShellSnackbar("Could not open " + screenDisplayName(screen.key()));
        }
    }

    /**
     * Navigates the shell to a detail screen on behalf of another screen: fires the pinned rail
     * button with the given key (the exact pathway of a rail click, so the ToggleGroup
     * selection and the highlight pill stay consistent), then loads the detail screen
     * synchronously and hands its freshly created controller to {@code setup} for data passing.
     * Both steps are guarded — a missing rail button or a failed load simply skips that step,
     * and {@code setup} never runs with a controller of the wrong type (callers instanceof-check).
     *
     * @param railButtonKey key of the pinned rail destination to fire (e.g. {@code navTrainers})
     * @param screen        the detail screen to load after the rail click
     * @param setup         receives the detail screen's controller (may be any object; callers check)
     */
    public void openScreenViaRail(String railButtonKey, Screen screen, Consumer<Object> setup) {
        fireRailButton(railButtonKey);
        loadScreen(screen);
        if (setup != null && lastLoadedController != null) {
            setup.accept(lastLoadedController);
        }
    }

    /** Fires the pinned rail destination with the given key (no-op when absent or disabled). */
    private void fireRailButton(String railButtonKey) {
        ToggleButton button = railButtonByKey(railButtonKey);
        if (button == null || button.isDisabled()) {
            return;
        }
        // Keep the compact bottom bar in sync when the fired destination is one of its primaries
        // (in-screen handoffs navigate through rail keys while the bar is showing).
        for (int i = 0; i < COMPACT_PRIMARY_KEYS.length; i++) {
            if (COMPACT_PRIMARY_KEYS[i].equals(railButtonKey)
                    && navBar.getSelectedIndex() != i) {
                compactPrimaryIndex = i;
                navBar.setSelectedIndex(i);
                break;
            }
        }
        boolean wasSelected = button.isSelected();
        button.fire();
        // JavaFX 21 ToggleButton.fire() toggles selection (setSelected(!isSelected())) before
        // firing the action, so firing an already-selected rail destination would deselect it
        // and leave the rail without a highlight. Restore it — the click pathway on a rail
        // button never deselects the destination it navigates to.
        if (wasSelected) {
            button.setSelected(true);
        }
    }

    /** Maps a rail destination key to its pinned field (direct, no tree search needed). */
    private ToggleButton railButtonByKey(String railButtonKey) {
        return switch (railButtonKey) {
            case "navDashboard" -> navDashboard;
            case "navCustomers" -> navCustomers;
            case "navTrainers" -> navTrainers;
            case "navSchedule" -> navSchedule;
            case "navMemberships" -> navMemberships;
            case "navEquipment" -> navEquipment;
            case "navAttendance" -> navAttendance;
            case "navPayments" -> navPayments;
            case "navStaff" -> navStaff;
            case "navHelp" -> navHelp;
            default -> null;
        };
    }

    /**
     * Surfaces shell-level feedback as an M3Snackbar: the overlay host is created lazily and
     * attached to the scene root (same pattern as the trainer screens'
     * {@code installFeedbackLayer}); when no scene exists yet (early initialization, headless
     * loads) the answer is skipped — the caller's stderr detail remains.
     */
    private void showShellSnackbar(String message) {
        if (!(rootShellPane.getScene() != null
                && rootShellPane.getScene().getRoot() instanceof javafx.scene.layout.Pane sceneRoot)) {
            return;
        }
        if (shellFeedback == null) {
            shellFeedback = new M3Overlay();
        }
        if (!sceneRoot.getChildren().contains(shellFeedback)) {
            sceneRoot.getChildren().add(shellFeedback);
        }
        shellFeedback.showSnackbar(message);
    }

    /** Human-facing screen name ("dashboard") for feedback messages. */
    private static String screenDisplayName(String screenKey) {
        return screenKey;
    }

    private void applySmoothScrolling(Node node) {
        if (node instanceof ScrollPane sp) {
            enableSmoothScrolling(sp);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applySmoothScrolling(child);
            }
        }
    }

    public static void enableSmoothScrolling(ScrollPane sp) {
        if (sp == null) return;
        sp.setPannable(true);
        sp.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            double deltaY = event.getDeltaY();
            if (deltaY == 0) return;

            Node content = sp.getContent();
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

    private boolean containsScrollPane(Node node) {
        if (node instanceof ScrollPane) return true;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (containsScrollPane(child)) return true;
            }
        }
        return false;
    }

    /**
     * Wires the Apple-style rail motion: a single highlight pill that springs to the selected
     * destination (like an iOS tab bar) and a subtle spring press-scale on every destination.
     * The pill is added in code, is unmanaged (never disturbs the rail layout),
     * mouse-transparent (never blocks clicks or keyboard focus) and sits behind
     * the destination buttons. Failure here must never break shell setup.
     */
    private void setupNavRailMotion() {
        try {
            if (navHighlightPill != null) return;
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
        if (navigationRail.lookup(".rail-scroll-pane") instanceof ScrollPane sp) return sp;
        for (Node child : navigationRail.getChildrenUnmodifiable()) {
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
        commandPalette.registerAction("Screens", "01. Authentication Vault", "Sign In & Biometrics", "person", () -> {
            // Gates enabled: re-open the real login gate (same flow as sign-out). Otherwise the
            // in-shell Authentication Vault screen stays available.
            if (com.gym.GymApp.SHOW_LAUNCH_GATES) {
                launchGate().showLoginGate();
            } else {
                loadScreen(Screen.LOGIN);
            }
        });
        commandPalette.registerAction("Screens", "02. Executive Dashboard", "Bento Operations Center", "home", () -> loadScreen(Screen.DASHBOARD));
        commandPalette.registerAction("Screens", "03. Member Directory", "Roster & Search Table", "group", () -> loadScreen(Screen.CUSTOMER_LIST));
        commandPalette.registerAction("Screens", "04. Athlete VIP Dossier", "360 Profile & Hologram Pass", "person", () -> loadScreen(Screen.CUSTOMER_PROFILE));
        commandPalette.registerAction("Screens", "05. Medical Profile", "Injury Assessment & 3D Anatomy", "favorite", () -> loadScreen(Screen.MEDICAL_PROFILE));
        commandPalette.registerAction("Screens", "06. Athletic Progress", "VBT Scatter & 52-Week Heatmap", "stats", () -> loadScreen(Screen.PROGRESS));
        commandPalette.registerAction("Screens", "07. Trainer Roster", "Coaching Staff Roster", "person", () -> loadScreen(Screen.TRAINER_LIST));
        commandPalette.registerAction("Screens", "08. Trainer Profile", "Coach Dossier & Capacity Gauge", "person", () -> loadScreen(Screen.TRAINER_PROFILE));
        commandPalette.registerAction("Screens", "09. Availability Matrix", "Weekly Booking & Slot Caps", "calendar", () -> loadScreen(Screen.AVAILABILITY));
        commandPalette.registerAction("Screens", "10. Workout Builder", "Routine Pipeline & Exercises", "stats", () -> loadScreen(Screen.WORKOUT));
        commandPalette.registerAction("Screens", "11. Live Workout HUD", "Kinetic Rep Counter & Cardiac Zones", "time", () -> loadScreen(Screen.LIVE_WORKOUT_HUD));
        commandPalette.registerAction("Screens", "12. Schedule Calendar", "Class Agenda & Booking Drawer", "calendar", () -> loadScreen(Screen.SCHEDULE));
        commandPalette.registerAction("Screens", "13. Equipment 3D Inventory", "12 Procedural 3D Models & Maintenance", "settings", () -> loadScreen(Screen.EQUIPMENT));
        commandPalette.registerAction("Screens", "14. Gym Floorplan Map", "2.5D Facility Blueprint & Occupancy", "home", () -> loadScreen(Screen.GYM_FLOOR_MAP));
        commandPalette.registerAction("Screens", "15. Attendance Scanner", "Turnstile Scanner & Peak Heatmap", "check", () -> loadScreen(Screen.ATTENDANCE));
        commandPalette.registerAction("Screens", "16. Membership Plans", "Tier Cards & Conversion Funnel", "favorite", () -> loadScreen(Screen.MEMBERSHIP));
        commandPalette.registerAction("Screens", "17. Cashless POS & Billing", "POS Terminal & Thermal Receipts", "money", () -> loadScreen(Screen.PAYMENT));
        commandPalette.registerAction("Screens", "18. Staff RBAC Security", "Staff Permissions & Security Audit", "settings", () -> loadScreen(Screen.STAFF));
        // Deferred one pulse so the lookup runs with the shell fully attached (same pattern as
        // the rail scroll-pane lookup above); a missed lookup simply keeps the plain card.
        javafx.application.Platform.runLater(this::applyCommandPaletteGlass);
    }

    /**
     * Moves the command palette's floating card onto a real liquid glass surface (Apple iOS 26):
     * the card node is re-parented into a REGULAR interactive {@link M3GlassPane} (RECT, 16px
     * corners) that takes the card's exact place in the palette, and the card's opaque
     * surface-container fill plus outline border are cleared — Apple's rule that glass never
     * sits on an opaque background. The palette's own show/close choreography, keyboard handling
     * and search wiring all keep animating the same card node, so nothing behavioral changes.
     *
     * <p>Deliberately NOT glassified: the docked top app bar and navigation rail. Apple reserves
     * glass for chrome that floats OVER content; the shell's BorderPane-docked bar and rail never
     * overlap page content, so glass there would have nothing to refract and would only add a
     * backdrop capture cost. Glass stays on the one truly floating chrome surface this shell owns.
     *
     * <p>Defensive by construction: every step is guarded and any failure leaves the palette
     * untouched with its normal opaque card — glass chrome must never break a working control.
     */
    private void applyCommandPaletteGlass() {
        try {
            Node card = commandPalette.lookup(".m3-command-palette-card");
            // The palette card is an M3Card (a Region); glass needs its max-size constraints,
            // which live on Region, not Node.
            if (!(card instanceof javafx.scene.layout.Region cardRegion
                    && card.getParent() instanceof javafx.scene.layout.Pane paletteRoot)) {
                System.err.println("Command palette glass: card node not found — plain card kept.");
                return;
            }
            int index = paletteRoot.getChildren().indexOf(card);
            if (index < 0) {
                return;
            }
            M3GlassPane glass = M3GlassPane.of(new GlassEffectParams()
                    .in(GlassShape.RECT, COMMAND_PALETTE_GLASS_RADIUS)
                    .interactive());
            // Replace-then-reparent: the glass takes the card's slot first (so no child index
            // shifts underneath us), then the card moves into the glass's content box.
            paletteRoot.getChildren().set(index, glass);
            glass.setContent(card);
            // The card's own paint moves onto the glass (inline style outranks the m3fx.css
            // rule that fills .m3-command-palette-card). Only color properties are cleared —
            // the palette's spacing/padding stay authoritative.
            card.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            // The palette enters/exits by animating the card (fade + rise, MotionUtil.enter/exit);
            // mirror the card's opacity onto the glass so the surface materializes and dissolves
            // together with its content. Transforms stay on the card, untouched.
            glass.opacityProperty().bind(card.opacityProperty());
            // Preserve the original placement constraints (they live on the child node).
            if (StackPane.getAlignment(card) != null) {
                StackPane.setAlignment(glass, StackPane.getAlignment(card));
            }
            if (StackPane.getMargin(card) != null) {
                StackPane.setMargin(glass, StackPane.getMargin(card));
            }
            glass.setMaxSize(cardRegion.getMaxWidth(), cardRegion.getMaxHeight());
        } catch (Throwable t) {
            System.err.println("Command palette glass skipped: " + t);
        }
    }

    /**
     * The manual rail toggle. At medium/expanded widths it flips collapsed/expanded and
     * records the user override (which wins over the width class default until the class
     * changes). At compact width there is no rail — the top-bar menu button opens the compact
     * overflow menu instead.
     */
    public void onToggleRail() {
        if (widthClass == WindowSizeClass.COMPACT) {
            openCompactMoreMenu();
            return;
        }
        applyRailCollapsed(!railCollapsed);
        railOverride = railCollapsed;
    }

    /**
     * Applies the collapsed/expanded visuals to the rail (the existing `.collapsed` CSS class,
     * icon-only content display, split-button swap, spring-animated width). Reused by both the
     * manual toggle and the responsive width-class defaults.
     */
    private void applyRailCollapsed(boolean collapsed) {
        railCollapsed = collapsed;
        double targetWidth = collapsed ? 80.0 : 220.0;

        if (collapsed) {
            if (!navigationRail.getStyleClass().contains("collapsed")) {
                navigationRail.getStyleClass().add("collapsed");
            }
            for (ToggleButton btn : navButtons) {
                btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btn.setAlignment(Pos.CENTER);
            }
            globalActionSplitBtn.setVisible(false);
            globalActionSplitBtn.setManaged(false);
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
            globalActionSplitBtn.setVisible(true);
            globalActionSplitBtn.setManaged(true);
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

    /**
     * Publishes the signed-in identity from the real login gate (rule 16): the user chip shows
     * the account's full name and role. The default stays the seeded ADMIN account so
     * gates-disabled runs (tests, previews) keep their established look. The identity is also
     * stored so screen controllers can read it (C7's staff screen renders it in its viewer
     * badge) without re-deriving it from the database.
     */
    public void setSignedInUser(String fullName, StaffRole role) {
        String name = fullName == null || fullName.isBlank() ? "Signed in" : fullName;
        signedInName = name;
        signedInRole = role == null ? StaffRole.STAFF : role;
        userRoleChip.setText(name + " [" + signedInRole + "]");
    }

    /** The signed-in account's full name, or null when no gate sign-in has happened yet. */
    public String getSignedInName() {
        return signedInName;
    }

    /** The signed-in account's role, or null when no gate sign-in has happened yet. */
    public StaffRole getSignedInRole() {
        return signedInRole;
    }

    public void onOpenCommandPalette() {
        commandPalette.show();
    }

    public void onToggleThemeMode() {
        if (rootShellPane.getScene() != null) {
            // Flip our own state instead of reading the switch: the skin toggles the switch with
            // its own MOUSE_CLICKED handler, so the read could see either the old or new value.
            darkMode = !darkMode;
            javafx.scene.Scene scene = rootShellPane.getScene();
            io.m3fx.core.theme.M3Theme theme =
                    io.m3fx.core.theme.M3Theme.fromSeed(com.gym.GymApp.SEED_PURPLE, io.m3fx.core.theme.ThemeVariant.TONAL_SPOT, darkMode);
            io.m3fx.controls.M3Stylesheets.applyTo(scene, theme);
        }
        // Keep the switch visual in sync with the applied scheme regardless of event order.
        themeModeSwitch.setSelected(darkMode);
    }

    public void onOpenUserMenu() {
        onNavStaff();
    }

    public void onLogout() {
        // Real runs: sign out into the Apple-style login gate. Tests / gates-disabled runs keep
        // the original behavior (load the in-shell Authentication Vault screen).
        if (com.gym.GymApp.SHOW_LAUNCH_GATES) {
            launchGate().showLoginGate();
        } else {
            loadScreen(Screen.LOGIN);
        }
    }

    public void onFastCheckIn() {
        loadScreen(Screen.ATTENDANCE);
    }

    public void onNavDashboard() {
        loadScreen(Screen.DASHBOARD);
    }

    public void onNavCustomers() {
        loadScreen(Screen.CUSTOMER_LIST);
    }

    public void onNavTrainers() {
        loadScreen(Screen.TRAINER_LIST);
    }

    public void onNavSchedule() {
        loadScreen(Screen.SCHEDULE);
    }

    public void onNavMemberships() {
        loadScreen(Screen.MEMBERSHIP);
    }

    public void onNavEquipment() {
        loadScreen(Screen.EQUIPMENT);
    }

    public void onNavAttendance() {
        loadScreen(Screen.ATTENDANCE);
    }

    public void onNavPayments() {
        loadScreen(Screen.PAYMENT);
    }

    public void onNavStaff() {
        loadScreen(Screen.STAFF);
    }

    /**
     * The Documentation destination: a real about-and-help dialog (keyboard shortcuts, the
     * seeded admin credentials from the login hint, version, and where the business rules
     * live) — no navigation to a random screen. The rail destination itself stays selected so
     * the highlight pill lands on Documentation.
     */
    public void onNavHelp() {
        M3Overlay overlay = shellOverlay();
        if (overlay == null) {
            showShellSnackbar("Help opens inside the app window.");
            return;
        }
        M3Button close = M3Button.text("Close");
        M3Dialog dialog = M3Dialog.basic("About & help", helpContent(), close);
        close.setOnAction(e -> dialog.hide());
        dialog.setDefaultAction(close::activate);
        overlay.showDialog(dialog);
    }

    /** The shell's lazily-created overlay host, or null when there is no scene yet. */
    private M3Overlay shellOverlay() {
        if (!(rootShellPane.getScene() != null
                && rootShellPane.getScene().getRoot() instanceof javafx.scene.layout.Pane sceneRoot)) {
            return null;
        }
        if (shellFeedback == null) {
            shellFeedback = new M3Overlay();
        }
        if (!sceneRoot.getChildren().contains(shellFeedback)) {
            sceneRoot.getChildren().add(shellFeedback);
        }
        return shellFeedback;
    }

    /** Genuinely useful help content: shortcuts, seeded credentials, version, rules pointer. */
    private VBox helpContent() {
        Label shortcuts = new Label("Keyboard");
        shortcuts.getStyleClass().add("title-medium");
        Label accounts = new Label("Seeded accounts (rule 16)");
        accounts.getStyleClass().add("title-medium");
        Label about = new Label("About");
        about.getStyleClass().add("title-medium");

        VBox box = new VBox(8,
                shortcuts,
                helpRow("keyboard", "Ctrl + K", "Open the command palette: jump to any screen or action."),
                helpRow("keyboard_arrow_down", "Arrow keys", "Move across rail destinations and the compact bottom bar."),
                helpRow("check", "Enter / Esc", "Confirm or dismiss dialogs, menus and the palette."),
                accounts,
                helpRow("person", "admin / admin123", "ADMIN role, active. manager / mgr123 and staff / staff123 also sign in."),
                helpRow("info", "oldstaff / old123", "Seeded inactive account - refused at sign-in (rule 16)."),
                about,
                helpRow("star", "GYM-OS Enterprise 2026", "Version 1.0.0 - Java-built Material 3 Expressive UI."),
                helpRow("event", "Business rules 1-19", "Enforced in the DAOs (com.gym.dao); see docs/UML.md."));
        box.setPrefWidth(420);
        return box;
    }

    /** One two-line help row with a leading icon (same pattern as the screen-level lists). */
    private M3ListItem helpRow(String symbol, String headline, String supporting) {
        M3ListItem item = new M3ListItem(headline, supporting, M3ListItem.TWO_LINES);
        item.setLeading(M3Icon.symbol(symbol));
        return item;
    }
}
