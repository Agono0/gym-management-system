package com.gym;

import io.m3fx.controls.M3Stylesheets;
import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.appbar.TopAppBarVariant;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.button.M3SplitButton.SplitButtonSize;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.carousel.M3Carousel;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.command.M3CommandPalette;
import io.m3fx.controls.common.M3Badge;
import io.m3fx.controls.common.M3Divider;
import io.m3fx.controls.common.M3Tooltip;
import io.m3fx.controls.floorplan.M3GymFloorMap;
import io.m3fx.controls.workout.M3HeartRateZones;
import io.m3fx.controls.workout.M3IntervalTimer;
import io.m3fx.controls.workout.M3RepCounter;
import io.m3fx.controls.workout.M3WorkoutHUD;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.fab.FabSize;
import io.m3fx.controls.fab.M3Fab;
import io.m3fx.controls.fab.M3FabMenu;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.internal.OverlayLayer;
import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.internal.SpringAnimation.SpringParams;
import io.m3fx.controls.layout.M3AdaptiveScaffold;
import io.m3fx.controls.list.M3ListItem;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.menu.M3Select;
import io.m3fx.controls.nav.M3NavDestination;
import io.m3fx.controls.nav.M3NavigationBar;
import io.m3fx.controls.nav.M3NavigationDrawer;
import io.m3fx.controls.nav.M3NavigationRail;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.picker.M3Calendar;
import io.m3fx.controls.picker.M3DateDialog;
import io.m3fx.controls.picker.M3DatePicker;
import io.m3fx.controls.picker.M3TimePicker;
import io.m3fx.controls.progress.M3CircularProgress;
import io.m3fx.controls.progress.M3LinearProgress;
import io.m3fx.controls.progress.M3WavyProgress;
import io.m3fx.controls.search.M3SearchBar;
import io.m3fx.controls.segmented.M3SegmentedButton;
import io.m3fx.controls.selection.M3CheckBox;
import io.m3fx.controls.selection.M3RadioButton;
import io.m3fx.controls.selection.M3Switch;
import io.m3fx.controls.shape.M3ShapeMorph;
import io.m3fx.controls.shape.M3ShapeMorph.ShapeType;
import io.m3fx.controls.sheet.M3BottomSheet;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.slider.M3RangeSlider;
import io.m3fx.controls.slider.M3Slider;
import io.m3fx.controls.snackbar.M3Snackbar;
import io.m3fx.controls.tabs.M3Tabs;
import io.m3fx.controls.tabs.TabVariant;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import io.m3fx.controls.toolbar.M3Toolbar;
import io.m3fx.controls.toolbar.M3Toolbar.ToolbarStyle;
import io.m3fx.controls.toolbar.M3Toolbar.ToolbarVariant;
import io.m3fx.controls.chart.M3BarChart;
import io.m3fx.controls.chart.M3DataPoint;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.chart.M3DonutChart;
import io.m3fx.controls.chart.M3GaugeChart;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.chart.M3RadarChart;
import io.m3fx.controls.chart.M3Sparkline;
import io.m3fx.controls.chart.M3SplineChart;
import io.m3fx.controls.chart.M3ScatterPlot;
import io.m3fx.controls.chart.M3ScatterSeries;
import io.m3fx.controls.chart.M3ScatterPoint;
import io.m3fx.controls.chart.M3FunnelChart;
import io.m3fx.controls.chart.M3FunnelStage;
import io.m3fx.controls.chart.M3ActivityCalendar;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3SpeedDial;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.data.M3Stepper;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.data.M3Timeline.Status;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.effect.M3GlowShadow;
import io.m3fx.controls.effect.M3Hologram;
import io.m3fx.controls.effect.M3ParticleCanvas;
import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.controls.effect.M3PulseBeacon.BeaconStatus;
import io.m3fx.controls.effect.M3Scanline;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D.MuscleGroup;
import io.m3fx.controls.scene3d.M3GyroRing3D;
import io.m3fx.controls.scene3d.M3ModelViewer3D;
import io.m3fx.controls.scene3d.M3ModelViewer3D.ModelType;
import io.m3fx.controls.scene3d.M3Scene3D;
import io.m3fx.controls.scene3d.M3TiltCard3D;
import io.m3fx.core.theme.ColorRole;
import io.m3fx.core.theme.M3Theme;
import io.m3fx.core.theme.ThemeVariant;
import io.m3fx.core.token.ElevationLevel;
import io.m3fx.core.token.ShapeTokens;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * Flagship Material Design 3 and M3 Expressive showcase application for m3fx.
 * Demonstrates all components, variants, states, physical spring animations,
 * dynamic color derivation, and responsive adaptation.
 */
public class MainApp extends Application {

    private M3Theme theme = M3Theme.baseline(false);
    private Scene scene;
    private M3Overlay overlay;
    private boolean expressivePhysics = true;

    private M3NavigationDrawer sidebar;
    private List<String> pageNames;
    private M3CommandPalette commandPalette;
    private M3ModelViewer3D globalModelViewer;

    // Palette seed presets
    private static final Color SEED_PURPLE = Color.web("#6750A4");
    private static final Color SEED_TEAL   = Color.web("#006A6A");
    private static final Color SEED_CORAL  = Color.web("#B3261E");
    private static final Color SEED_AZURE  = Color.web("#0061A4");
    private static final Color SEED_GREEN  = Color.web("#386A20");
    private static final Color SEED_AMBER  = Color.web("#825500");

    @Override
    public void start(Stage stage) {
        overlay = new M3Overlay();
        BorderPane root = new BorderPane();

        // Top App Bar + Theme Control Strip
        VBox topArea = buildTopArea();
        root.setTop(topArea);

        // Sidebar navigation with 16 destination categories
        pageNames = List.of(
                "Overview & Tokens",
                "Buttons & FABs",
                "Split Buttons & Toolbars",
                "Selection & Inputs",
                "Chips & Badges",
                "Cards & Lists",
                "Progress & Sliders",
                "Carousels",
                "Overlays & Dialogs",
                "Physics Playground",
                "Cyber Analytics & Charts",
                "3D Hologram & Viewports",
                "Futuristic Visual Effects",
                "Enterprise Data & Telemetry",
                "Live Workout HUD",
                "Gym Floorplan Map",
                "Gym App Screens (19 FXMLs)");

        List<M3NavDestination> destinations = List.of(
                M3NavDestination.of("Overview", "home"),
                M3NavDestination.of("Buttons", "star"),
                M3NavDestination.of("Toolbars", "menu"),
                M3NavDestination.of("Inputs", "edit"),
                M3NavDestination.of("Chips", "favorite"),
                M3NavDestination.of("Cards", "person"),
                M3NavDestination.of("Progress", "stats"),
                M3NavDestination.of("Carousels", "arrow_forward"),
                M3NavDestination.of("Overlays", "info"),
                M3NavDestination.of("Physics", "refresh"),
                M3NavDestination.of("Cyber Analytics", "stats"),
                M3NavDestination.of("3D Holograms", "settings"),
                M3NavDestination.of("Visual Effects", "light"),
                M3NavDestination.of("Telemetry Data", "calendar"),
                M3NavDestination.of("Live Workout", "fitness"),
                M3NavDestination.of("Floorplan", "home"),
                M3NavDestination.of("Gym App Screens", "star"));

        sidebar = new M3NavigationDrawer(destinations, false);
        sidebar.setPrefWidth(220);
        root.setLeft(sidebar);

        ScrollPane centerScroll = new ScrollPane();
        centerScroll.setFitToWidth(true);
        centerScroll.setPadding(new Insets(20));
        centerScroll.getStyleClass().add("m3-gallery-scroll");
        root.setCenter(centerScroll);

        sidebar.selectedIndexProperty().addListener((obs, oldV, index) -> {
            int idx = Math.max(0, Math.min(pageNames.size() - 1, index.intValue()));
            centerScroll.setContent(buildPage(pageNames.get(idx)));
            centerScroll.setVvalue(0);
        });

        // Initial page: Overview
        centerScroll.setContent(buildPage(pageNames.get(0)));

        commandPalette = new M3CommandPalette();
        initCommandPaletteActions();

        StackPane mainShell = new StackPane(root, overlay, commandPalette);
        scene = new Scene(mainShell, 1220, 840);
        M3Stylesheets.applyTo(scene, theme);
        M3CommandPalette.attachGlobalShortcut(scene, commandPalette);

        stage.setTitle("m3fx — Material Design 3 & Expressive Showcase");
        stage.setScene(scene);
        stage.show();
    }

    private void navigateTo(String pageName) {
        if (sidebar != null && pageNames != null) {
            int idx = pageNames.indexOf(pageName);
            if (idx >= 0) {
                sidebar.setSelectedIndex(idx);
            }
        }
    }

    private void toggleTheme() {
        theme = M3Theme.fromSeed(theme.seed(), theme.variant(), !theme.dark(), theme.contrast());
        M3Stylesheets.applyTo(scene, theme);
    }

    private void initCommandPaletteActions() {
        commandPalette.registerAction("Workouts", "Start Live Workout Session", "Open kinetic rep counter & heart rate cockpit", "fitness", () -> navigateTo("Live Workout HUD"));
        commandPalette.registerAction("Workouts", "Launch HIIT Tabata Timer", "45s work / 15s rest interval countdown", "refresh", () -> navigateTo("Live Workout HUD"));
        commandPalette.registerAction("Equipment", "View Gym Floorplan & Occupancy", "Interactive map with live equipment pins", "home", () -> navigateTo("Gym Floorplan Map"));

        // All 12 Procedural 3D Gym Equipment Models
        for (ModelType mt : ModelType.values()) {
            String label = mt.name().replace('_', ' ');
            commandPalette.registerAction("Equipment 3D", "Inspect " + label + " in 3D", "Interactive procedural hardware-accelerated 3D mesh", "settings", () -> {
                navigateTo("3D Hologram & Viewports");
                if (globalModelViewer != null) globalModelViewer.setCurrentModel(mt);
            });
        }

        commandPalette.registerAction("Members", "View Alexander Vance VIP Pass", "Holographic 3D parallax tilt card", "person", () -> navigateTo("3D Hologram & Viewports"));
        commandPalette.registerAction("Analytics", "Open Velocity-Based Training (VBT) Scatter", "Load-velocity curve & OLS regression", "stats", () -> navigateTo("Cyber Analytics & Charts"));
        commandPalette.registerAction("Analytics", "Open Member Conversion Funnel", "Multi-stage lifecycle & drop-off metrics", "stats", () -> navigateTo("Cyber Analytics & Charts"));
        commandPalette.registerAction("Analytics", "Open 52-Week Consistency Heatmap", "Annual training consistency punch-card", "stats", () -> navigateTo("Cyber Analytics & Charts"));
        commandPalette.registerAction("Analytics", "Open Athlete Skill Radar", "Multidimensional performance attributes", "stats", () -> navigateTo("Cyber Analytics & Charts"));
        commandPalette.registerAction("Analytics", "Open Peak Hours Heatmap", "Occupancy density matrix", "stats", () -> navigateTo("Cyber Analytics & Charts"));
        commandPalette.registerAction("Themes", "Toggle Dark / Light Mode", "Switch system color scheme", "dark", this::toggleTheme);
        commandPalette.registerAction("Effects", "Open Kinetic Particle Network", "Interactive node physics & shockwaves", "light", () -> navigateTo("Futuristic Visual Effects"));
        commandPalette.registerAction("Screens", "Open Gym App FXML Porting Studio", "Interactive viewer for all 19 FXML screens", "star", () -> navigateTo("Gym App Screens (19 FXMLs)"));
        commandPalette.registerAction("Screens", "Launch Full Application Shell Window", "Open app-shell.fxml in full desktop window", "home", () -> {
            try {
                new GymApp().start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private VBox buildTopArea() {
        M3TopAppBar appBar = new M3TopAppBar("Material Design 3 & Expressive", TopAppBarVariant.SMALL);
        appBar.setNavigationIcon(M3Icon.symbol("menu"));

        M3IconButton themeToggle = M3IconButton.standard(M3Icon.symbol("dark"));
        themeToggle.setAccessibleText("Toggle light/dark mode");
        themeToggle.setOnAction(e -> toggleTheme());

        M3Button cmdBtn = M3Button.tonal("⌘K Search");
        cmdBtn.setGraphic(M3Icon.symbol("search"));
        cmdBtn.setOnAction(e -> {
            if (commandPalette != null) commandPalette.show();
        });

        appBar.setActions(cmdBtn, themeToggle);

        HBox themeBar = new HBox(12);
        themeBar.setAlignment(Pos.CENTER_LEFT);
        themeBar.setPadding(new Insets(8, 16, 8, 16));
        themeBar.getStyleClass().add("m3-theme-bar");

        Label seedLabel = new Label("Seed:");
        seedLabel.setStyle("-fx-font-weight: bold;");

        // Seed preset color dots
        HBox seedSwatches = new HBox(6);
        seedSwatches.setAlignment(Pos.CENTER_LEFT);
        for (Color c : List.of(SEED_PURPLE, SEED_TEAL, SEED_CORAL, SEED_AZURE, SEED_GREEN, SEED_AMBER)) {
            Circle dot = new Circle(10, c);
            dot.setStroke(Color.WHITE);
            dot.setStrokeWidth(1.5);
            dot.setStyle("-fx-cursor: hand;");
            dot.setOnMouseClicked(e -> {
                theme = M3Theme.fromSeed(c, theme.variant(), theme.dark(), theme.contrast());
                M3Stylesheets.applyTo(scene, theme);
            });
            seedSwatches.getChildren().add(dot);
        }

        ColorPicker customColorPicker = new ColorPicker(theme.seed());
        customColorPicker.setOnAction(e -> {
            theme = M3Theme.fromSeed(customColorPicker.getValue(), theme.variant(), theme.dark(), theme.contrast());
            M3Stylesheets.applyTo(scene, theme);
        });

        Label variantLabel = new Label("Scheme:");
        variantLabel.setStyle("-fx-font-weight: bold;");
        ComboBox<ThemeVariant> variantBox = new ComboBox<>(FXCollections.observableArrayList(ThemeVariant.values()));
        variantBox.setValue(ThemeVariant.TONAL_SPOT);
        variantBox.setOnAction(e -> {
            if (variantBox.getValue() != null) {
                theme = M3Theme.fromSeed(theme.seed(), variantBox.getValue(), theme.dark(), theme.contrast());
                M3Stylesheets.applyTo(scene, theme);
            }
        });

        Label contrastLabel = new Label("Contrast:");
        contrastLabel.setStyle("-fx-font-weight: bold;");
        ComboBox<String> contrastBox = new ComboBox<>(FXCollections.observableArrayList("Standard (0.0)", "Medium (0.5)", "High (1.0)"));
        contrastBox.setValue("Standard (0.0)");
        contrastBox.setOnAction(e -> {
            double c = switch (contrastBox.getSelectionModel().getSelectedIndex()) {
                case 1 -> M3Theme.CONTRAST_MEDIUM;
                case 2 -> M3Theme.CONTRAST_HIGH;
                default -> M3Theme.CONTRAST_STANDARD;
            };
            theme = M3Theme.fromSeed(theme.seed(), theme.variant(), theme.dark(), c);
            M3Stylesheets.applyTo(scene, theme);
        });

        M3Switch physicsSwitch = new M3Switch(true);
        physicsSwitch.setShowIcon(true);
        Label physicsLabel = new Label("Expressive Physics");
        physicsSwitch.selectedProperty().addListener((obs, oldV, isExp) -> expressivePhysics = isExp);

        HBox physicsBox = new HBox(6, physicsSwitch, physicsLabel);
        physicsBox.setAlignment(Pos.CENTER_LEFT);

        themeBar.getChildren().addAll(
                seedLabel, seedSwatches, customColorPicker,
                new M3Divider(Orientation.VERTICAL),
                variantLabel, variantBox,
                contrastLabel, contrastBox,
                new M3Divider(Orientation.VERTICAL),
                physicsBox);

        return new VBox(appBar, themeBar);
    }

    private Node buildPage(String name) {
        return switch (name) {
            case "Overview & Tokens" -> overviewPage();
            case "Buttons & FABs" -> buttonsPage();
            case "Split Buttons & Toolbars" -> splitButtonsAndToolbarsPage();
            case "Selection & Inputs" -> inputsPage();
            case "Chips & Badges" -> chipsAndBadgesPage();
            case "Cards & Lists" -> cardsAndListsPage();
            case "Progress & Sliders" -> progressAndSlidersPage();
            case "Carousels" -> carouselsPage();
            case "Overlays & Dialogs" -> overlaysPage();
            case "Physics Playground" -> physicsPlaygroundPage();
            case "Cyber Analytics & Charts" -> chartsPage();
            case "3D Hologram & Viewports" -> scene3DPage();
            case "Futuristic Visual Effects" -> effectsPage();
            case "Enterprise Data & Telemetry" -> telemetryPage();
            case "Live Workout HUD" -> workoutHudPage();
            case "Gym Floorplan Map" -> floorplanPage();
            case "Gym App Screens (19 FXMLs)" -> gymAppScreensPage();
            default -> new Label("Select a section from the navigation drawer.");
        };
    }

    // ==========================================
    // 1. Overview & Tokens
    // ==========================================
    private VBox overviewPage() {
        Label intro = new Label("Welcome to m3fx — Pure Material Design 3 and M3 Expressive library for JavaFX.\n"
                + "Every control is driven by dynamic M3 color tokens, real mass-spring-damper physics, and standard typography.");
        intro.setWrapText(true);
        intro.setStyle("-fx-font-size: 15px; -fx-padding: 0 0 8 0;");

        // Color role swatches
        FlowPane swatches = new FlowPane(8, 8);
        swatches.getChildren().addAll(
                colorSwatch("Primary", ColorRole.PRIMARY, ColorRole.ON_PRIMARY),
                colorSwatch("Primary Container", ColorRole.PRIMARY_CONTAINER, ColorRole.ON_PRIMARY_CONTAINER),
                colorSwatch("Secondary", ColorRole.SECONDARY, ColorRole.ON_SECONDARY),
                colorSwatch("Secondary Container", ColorRole.SECONDARY_CONTAINER, ColorRole.ON_SECONDARY_CONTAINER),
                colorSwatch("Tertiary", ColorRole.TERTIARY, ColorRole.ON_TERTIARY),
                colorSwatch("Tertiary Container", ColorRole.TERTIARY_CONTAINER, ColorRole.ON_TERTIARY_CONTAINER),
                colorSwatch("Surface", ColorRole.SURFACE, ColorRole.ON_SURFACE),
                colorSwatch("Surface Container", ColorRole.SURFACE_CONTAINER, ColorRole.ON_SURFACE),
                colorSwatch("Surface Container High", ColorRole.SURFACE_CONTAINER_HIGH, ColorRole.ON_SURFACE),
                colorSwatch("Error", ColorRole.ERROR, ColorRole.ON_ERROR)
        );

        // Typography scale preview
        VBox typeBox = new VBox(6);
        typeBox.getChildren().addAll(
                styledLabel("Display Large (57px)", 36, true),
                styledLabel("Headline Medium (28px)", 24, true),
                styledLabel("Title Medium (16px)", 16, true),
                styledLabel("Body Large (16px regular)", 16, false),
                styledLabel("Label Large (14px medium)", 14, true)
        );

        // Elevation scale
        HBox elevBox = new HBox(16);
        elevBox.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i <= 5; i++) {
            ElevationLevel el = ElevationLevel.of(i);
            StackPane box = new StackPane(new Label("L" + i));
            box.setMinSize(64, 48);
            box.setStyle("-fx-background-color: -md-sys-color-surface-container-high; -fx-background-radius: 8px;");
            box.setEffect(el.toEffect(Color.rgb(0, 0, 0, 0.3)));
            elevBox.getChildren().add(box);
        }

        return new VBox(20,
                section("Introduction", intro),
                section("Dynamic Color Roles (Updated live from Seed)", swatches),
                section("Type Scale Preview", typeBox),
                section("Elevation Levels (0 to 5)", elevBox)
        );
    }

    private StackPane colorSwatch(String name, ColorRole bgRole, ColorRole fgRole) {
        Label lbl = new Label(name);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -md-sys-color-" + fgRole.cssName() + ";");
        StackPane pane = new StackPane(lbl);
        pane.setMinSize(160, 48);
        pane.setPadding(new Insets(8));
        pane.setStyle("-fx-background-color: -md-sys-color-" + bgRole.cssName() + "; -fx-background-radius: 8px;");
        return pane;
    }

    private Label styledLabel(String text, double size, boolean bold) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + size + "px;" + (bold ? "-fx-font-weight: bold;" : ""));
        return l;
    }

    // ==========================================
    // 2. Buttons & FABs
    // ==========================================
    private VBox buttonsPage() {
        // Variants
        M3Button filled = M3Button.filled("Filled").withIcon(M3Icon.symbol("check"));
        M3Button tonal = M3Button.tonal("Tonal").withIcon(M3Icon.symbol("favorite"));
        M3Button outlined = M3Button.outlined("Outlined");
        M3Button elevated = M3Button.elevated("Elevated");
        M3Button textBtn = M3Button.text("Text Button");
        M3Button toggleBtn = M3Button.filled("Toggleable");
        toggleBtn.setToggleable(true);
        M3Button disabledBtn = M3Button.filled("Disabled");
        disabledBtn.setDisable(true);

        HBox buttonsRow = new HBox(12, filled, tonal, outlined, elevated, textBtn, toggleBtn, disabledBtn);
        buttonsRow.setAlignment(Pos.CENTER_LEFT);

        // Icon buttons
        M3IconButton ibStandard = M3IconButton.standard(M3Icon.symbol("search"));
        M3IconButton ibFilled = M3IconButton.filled(M3Icon.symbol("favorite"));
        M3IconButton ibTonal = M3IconButton.tonal(M3Icon.symbol("settings"));
        M3IconButton ibOutlined = M3IconButton.outlined(M3Icon.symbol("person"));
        M3IconButton ibToggle = M3IconButton.filled(M3Icon.symbol("star"));
        ibToggle.setToggleable(true);

        HBox iconButtonsRow = new HBox(12, ibStandard, ibFilled, ibTonal, ibOutlined, ibToggle);
        iconButtonsRow.setAlignment(Pos.CENTER_LEFT);

        // FABs
        M3Fab fabSmall = new M3Fab(M3Icon.symbol("add"), FabSize.SMALL);
        M3Fab fabMedium = new M3Fab(M3Icon.symbol("add"), FabSize.MEDIUM);
        M3Fab fabLarge = new M3Fab(M3Icon.symbol("add"), FabSize.LARGE);
        M3Fab fabExtended = M3Fab.extended("New Workout", M3Icon.symbol("add"));

        HBox fabsRow = new HBox(16, fabSmall, fabMedium, fabLarge, fabExtended);
        fabsRow.setAlignment(Pos.CENTER_LEFT);

        // Interactive FAB Menu
        M3FabMenu fabMenu = new M3FabMenu();
        fabMenu.addAction("Add Member", "person", () -> overlay.showSnackbar("Add member clicked"));
        fabMenu.addAction("Book Session", "edit", () -> overlay.showSnackbar("Book session clicked"));
        fabMenu.addAction("Record Payment", "check", () -> overlay.showSnackbar("Record payment clicked"));

        return new VBox(20,
                section("Buttons (M3 Control + Skin with Spring Press Feedback)", buttonsRow),
                section("Icon Buttons", iconButtonsRow),
                section("Floating Action Buttons (Small, Medium, Large, Extended)", fabsRow),
                section("M3 Expressive FAB Menu (Expanding Action Column)", fabMenu)
        );
    }

    // ==========================================
    // 3. Split Buttons & Toolbars (M3 Expressive)
    // ==========================================
    private VBox splitButtonsAndToolbarsPage() {
        M3Menu actionMenu = new M3Menu()
                .item("Share with Trainer", M3Icon.symbol("person"), () -> overlay.showSnackbar("Shared with trainer"))
                .item("Export PDF Report", M3Icon.symbol("edit"), () -> overlay.showSnackbar("Exporting PDF"))
                .divider()
                .item("Archive Member", M3Icon.symbol("delete"), () -> overlay.showSnackbar("Archived"));

        M3SplitButton splitFilled = M3SplitButton.filled("Send Plan", actionMenu);
        splitFilled.setOnAction(e -> overlay.showSnackbar("Primary action: Plan Sent!"));

        M3SplitButton splitTonal = M3SplitButton.tonal("Save Routine", actionMenu);
        M3SplitButton splitOutlined = M3SplitButton.outlined("Export", actionMenu);
        M3SplitButton splitElevated = M3SplitButton.elevated("Open", actionMenu);

        HBox splitRow = new HBox(16, splitFilled, splitTonal, splitOutlined, splitElevated);
        splitRow.setAlignment(Pos.CENTER_LEFT);

        // Horizontal Floating Toolbar (Standard & Vibrant)
        M3Toolbar floatingStandard = new M3Toolbar(ToolbarVariant.FLOATING, ToolbarStyle.STANDARD);
        floatingStandard.getItems().addAll(
                M3IconButton.standard(M3Icon.symbol("home")),
                M3IconButton.standard(M3Icon.symbol("search")),
                new M3Divider(Orientation.VERTICAL),
                M3IconButton.standard(M3Icon.symbol("favorite")),
                M3IconButton.standard(M3Icon.symbol("settings"))
        );

        M3Toolbar floatingVibrant = new M3Toolbar(ToolbarVariant.FLOATING, ToolbarStyle.VIBRANT);
        floatingVibrant.getItems().addAll(
                M3IconButton.standard(M3Icon.symbol("edit")),
                M3IconButton.standard(M3Icon.symbol("check")),
                M3IconButton.standard(M3Icon.symbol("delete"))
        );

        HBox toolbarsRow = new HBox(24, floatingStandard, floatingVibrant);
        toolbarsRow.setAlignment(Pos.CENTER_LEFT);

        // Vertical Toolbar
        M3Toolbar verticalToolbar = new M3Toolbar(ToolbarVariant.FLOATING, ToolbarStyle.STANDARD, Orientation.VERTICAL);
        verticalToolbar.getItems().addAll(
                M3IconButton.standard(M3Icon.symbol("person")),
                M3IconButton.standard(M3Icon.symbol("star")),
                M3IconButton.standard(M3Icon.symbol("info"))
        );

        return new VBox(20,
                section("M3 Expressive Split Buttons (Connected Primary Action + Animated Menu Dropdown)", splitRow),
                section("Horizontal Floating Toolbars (Standard & Vibrant Styles, Level 2 Elevation)", toolbarsRow),
                section("Vertical Floating Toolbar", verticalToolbar)
        );
    }

    // ==========================================
    // 4. Selection & Inputs
    // ==========================================
    private VBox inputsPage() {
        // Switches
        M3Switch swOff = new M3Switch(false);
        M3Switch swOn = new M3Switch(true);
        M3Switch swIcon = new M3Switch(true);
        swIcon.setShowIcon(true);
        M3Switch swDisabled = new M3Switch(true);
        swDisabled.setDisable(true);

        HBox switchRow = new HBox(16,
                new Label("Off:"), swOff,
                new Label("On:"), swOn,
                new Label("Icon:"), swIcon,
                new Label("Disabled:"), swDisabled);
        switchRow.setAlignment(Pos.CENTER_LEFT);

        // Checkboxes & Radio
        M3CheckBox cb1 = new M3CheckBox("Active Membership");
        cb1.setSelected(true);
        M3CheckBox cb2 = new M3CheckBox("Indeterminate State");
        cb2.setIndeterminate(true);
        M3CheckBox cbDisabled = new M3CheckBox("Disabled");
        cbDisabled.setDisable(true);

        ToggleGroup radioGroup = new ToggleGroup();
        M3RadioButton rb1 = new M3RadioButton("Monthly Plan");
        rb1.setToggleGroup(radioGroup);
        rb1.setSelected(true);
        M3RadioButton rb2 = new M3RadioButton("Annual Plan");
        rb2.setToggleGroup(radioGroup);

        HBox selectRow = new HBox(20, cb1, cb2, cbDisabled, new M3Divider(Orientation.VERTICAL), rb1, rb2);
        selectRow.setAlignment(Pos.CENTER_LEFT);

        // Text Fields
        M3TextField tfFilled = new M3TextField("Full Name", TextFieldVariant.FILLED);
        tfFilled.setSupportingText("Required for registration");
        tfFilled.setLeadingIcon(M3Icon.symbol("person"));

        M3TextField tfOutlined = new M3TextField("Email Address", TextFieldVariant.OUTLINED);
        tfOutlined.setSupportingText("We'll never share your email");
        tfOutlined.setLeadingIcon(M3Icon.symbol("search"));

        M3TextField tfError = new M3TextField("Password", TextFieldVariant.OUTLINED);
        tfError.setError(true);
        tfError.setSupportingText("Minimum 8 characters");

        M3TextField tfCounter = new M3TextField("Member Bio", TextFieldVariant.FILLED);
        tfCounter.setMaxLength(80);

        HBox fieldsRow = new HBox(16, tfFilled, tfOutlined, tfError, tfCounter);

        // Search Bar
        M3SearchBar searchBar = new M3SearchBar("Search members or workouts…");
        searchBar.setOverlay(overlay.getLayer());
        searchBar.getSuggestions().addAll(
                "Abdelrhman Mohamed", "Ziad Ali", "Yousef Ahmed", "Crossfit Advanced", "HIIT Cardio", "Yoga Flow");

        return new VBox(20,
                section("M3 Switches (Real Mass-Spring-Damper Thumb Glide & Expansion)", switchRow),
                section("Checkboxes & Radio Buttons", selectRow),
                section("M3 Text Fields (Floating Label Transitions, Counter & Error States)", fieldsRow),
                section("M3 Search Bar (Suggestions Panel with Scrim Dismiss)", searchBar)
        );
    }

    // ==========================================
    // 5. Chips & Badges
    // ==========================================
    private VBox chipsAndBadgesPage() {
        M3Chip assist = new M3Chip("Assist Chip", ChipVariant.ASSIST);
        assist.setGraphic(M3Icon.symbol("star"));

        M3Chip filter = new M3Chip("Filter Chip", ChipVariant.FILTER);
        filter.setSelected(true);

        M3Chip input = new M3Chip("Input Chip", ChipVariant.INPUT);
        input.setClosable(true);

        M3Chip suggestion = new M3Chip("Suggestion", ChipVariant.SUGGESTION);

        FlowPane chipsPane = new FlowPane(10, 10, assist, filter, input, suggestion);
        input.setOnClose(() -> {
            chipsPane.getChildren().remove(input);
            overlay.showSnackbar("Input chip closed & removed!");
        });
        input.setOnAction(e -> overlay.showSnackbar("Input chip clicked"));

        // Badges
        M3Badge dotBadge = new M3Badge();
        M3Badge countBadge = new M3Badge(5);
        M3Badge largeBadge = new M3Badge(99);

        HBox badgesRow = new HBox(16,
                new Label("Dot Badge:"), dotBadge,
                new Label("Count Badge:"), countBadge,
                new Label("Large Badge:"), largeBadge);
        badgesRow.setAlignment(Pos.CENTER_LEFT);

        // Select Menu
        M3Select<String> select = new M3Select<>();
        select.setPromptText("Select Training Tier");
        select.getItems().addAll("Standard Access", "Pro Tier (Includes Spa)", "VIP All-Inclusive");

        return new VBox(20,
                section("M3 Chips (Assist, Filter, Input with Close, Suggestion)", chipsPane),
                section("M3 Badges", badgesRow),
                section("M3 Select Dropdown", select)
        );
    }

    // ==========================================
    // 6. Cards & Lists
    // ==========================================
    private VBox cardsAndListsPage() {
        M3Card elevated = M3Card.of(CardVariant.ELEVATED, new Label("Elevated Card (Level 1 → 2 on Hover)"));
        elevated.setClickable(true);
        elevated.setPrefSize(240, 100);

        M3Card filled = M3Card.of(CardVariant.FILLED, new Label("Filled Card (Surface Container Highest)"));
        filled.setClickable(true);
        filled.setPrefSize(240, 100);

        M3Card outlined = M3Card.of(CardVariant.OUTLINED, new Label("Outlined Card (1px Outline Variant)"));
        outlined.setClickable(true);
        outlined.setPrefSize(240, 100);

        HBox cardsRow = new HBox(16, elevated, filled, outlined);

        // List Items
        M3ListItem item1 = new M3ListItem("John Doe", "Active Member · Premium Tier", M3ListItem.TWO_LINES);
        item1.setLeading(M3Icon.symbol("person"));
        item1.setTrailing(new M3CheckBox(true));

        M3ListItem item2 = new M3ListItem("Cardio Circuit 9:00 AM", "Studio A with Coach Sarah · 12 attending", M3ListItem.TWO_LINES);
        item2.setLeading(M3Icon.symbol("star"));
        item2.setTrailing(M3IconButton.standard(M3Icon.symbol("favorite")));

        VBox listBox = new VBox(item1, new M3Divider(), item2);
        listBox.setStyle("-fx-background-color: -md-sys-color-surface-container-low; -fx-background-radius: 12px; -fx-padding: 8px;");

        return new VBox(20,
                section("M3 Cards (Elevated, Filled, Outlined with Tactile Ripple & Elevation Physics)", cardsRow),
                section("M3 List Items & Dividers", listBox)
        );
    }

    // ==========================================
    // 7. Progress & Sliders
    // ==========================================
    private VBox progressAndSlidersPage() {
        M3Slider continuous = new M3Slider(0, 100, 65);
        M3Slider discrete = M3Slider.discrete(0, 10, 4, 1);
        M3RangeSlider range = new M3RangeSlider(0, 100, 20, 80);

        // Linear Progress
        M3LinearProgress linDet = new M3LinearProgress(0.70);
        M3LinearProgress linIndet = new M3LinearProgress();

        // Expressive Wavy Progress
        M3WavyProgress wavyDet = new M3WavyProgress(0.60);
        M3WavyProgress wavyIndet = new M3WavyProgress();

        // Dedicated Canvas M3 Circular Progress
        M3CircularProgress circDet = new M3CircularProgress(0.75);
        M3CircularProgress circIndet = new M3CircularProgress();

        HBox circularBox = new HBox(24,
                new VBox(8, new Label("Determinate (75%):"), circDet),
                new VBox(8, new Label("M3 Rotating Sweep:"), circIndet));
        circularBox.setAlignment(Pos.CENTER_LEFT);

        return new VBox(20,
                section("Sliders (Continuous, Discrete with Stops, Range Slider)",
                        new VBox(12, new Label("Continuous (65%):"), continuous,
                                new Label("Discrete (Stops 0-10):"), discrete,
                                new Label("Range Slider:"), range)),
                section("Linear Progress (Determinate & Indeterminate)", new VBox(12, linDet, linIndet)),
                section("M3 Expressive Wavy Progress Indicators", new VBox(12, wavyDet, wavyIndet)),
                section("M3 Circular Progress (Authentic Sweep Physics)", circularBox)
        );
    }

    // ==========================================
    // 8. Carousels (M3 Expressive)
    // ==========================================
    private VBox carouselsPage() {
        M3Carousel carousel = new M3Carousel(M3Carousel.CarouselLayout.MULTI_BROWSE);

        for (int i = 1; i <= 6; i++) {
            VBox cardBody = new VBox(8);
            cardBody.setAlignment(Pos.CENTER);
            cardBody.setPadding(new Insets(16));
            cardBody.getChildren().addAll(
                    M3Icon.symbol(i % 2 == 0 ? "star" : "favorite"),
                    new Label("Workout Program #" + i),
                    new Label("High Intensity • 45 min")
            );
            cardBody.setStyle("-fx-background-color: -md-sys-color-surface-container-high; -fx-background-radius: 16px;");
            cardBody.setPrefSize(220, 160);
            carousel.getItems().add(cardBody);
        }

        return new VBox(20,
                section("M3 Multi-browse Carousel (Drag Gestures & Spring Snap Physics)", carousel)
        );
    }

    // ==========================================
    // 9. Overlays & Dialogs
    // ==========================================
    private VBox overlaysPage() {
        M3Button openDialog = M3Button.filled("Open Confirmation Dialog");
        openDialog.setOnAction(e -> {
            M3Button cancelBtn = M3Button.text("Cancel");
            M3Button confirmBtn = M3Button.text("Confirm");

            M3Dialog dialog = M3Dialog.basic("Freeze Membership?",
                    new Label("The membership will be paused until next Monday."),
                    cancelBtn, confirmBtn);

            cancelBtn.setOnAction(ev -> dialog.hide());
            confirmBtn.setOnAction(ev -> {
                dialog.hide();
                overlay.showSnackbar("Membership successfully frozen");
            });

            overlay.showDialog(dialog);
        });

        M3Button openSheet = M3Button.tonal("Open Bottom Sheet");
        openSheet.setOnAction(e -> {
            M3BottomSheet sheet = new M3BottomSheet();
            VBox content = new VBox(12);
            content.getChildren().addAll(
                    styledLabel("Member Quick Actions", 18, true),
                    new M3ListItem("Renew Annual Plan", "Extend current plan by 12 months", M3ListItem.TWO_LINES),
                    new M3ListItem("Assign Personal Trainer", "Select from available certified trainers", M3ListItem.TWO_LINES),
                    M3Button.filled("Close").withIcon(M3Icon.symbol("check"))
            );
            sheet.setContent(content);
            overlay.showSheet(sheet);
        });

        M3Button openSideSheet = M3Button.outlined("Open Side Sheet");
        openSideSheet.setOnAction(e -> {
            M3SideSheet sideSheet = new M3SideSheet();
            VBox content = new VBox(12);
            content.getChildren().addAll(
                    styledLabel("Filter Members", 18, true),
                    new M3CheckBox("Active Only"),
                    new M3CheckBox("Expiring within 7 days"),
                    new M3CheckBox("Premium Tiers"),
                    M3Button.tonal("Apply Filters")
            );
            sideSheet.setContent(content);
            overlay.showSheet(sideSheet);
        });

        M3Button showSnack = M3Button.outlined("Show Snackbar with Action");
        showSnack.setOnAction(e -> overlay.showSnackbar("Member deleted", "Undo", () -> overlay.showSnackbar("Deletion undone!")));

        M3Button pickDate = M3Button.elevated("Open Calendar Date Dialog");
        pickDate.setOnAction(e -> M3DateDialog.showIn(overlay.getLayer(), date -> overlay.showSnackbar("Selected: " + date)));

        HBox btnRow = new HBox(12, openDialog, openSheet, openSideSheet, showSnack, pickDate);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        HBox inlinePickers = new HBox(16, new M3DatePicker(), new M3TimePicker());
        inlinePickers.setAlignment(Pos.CENTER_LEFT);

        return new VBox(20,
                section("Modal Overlays (Dialog, Bottom Sheet, Side Sheet, Snackbar Queue)", btnRow),
                section("Inline Date & Time Pickers", inlinePickers),
                section("Full Calendar Month Grid", new M3Calendar())
        );
    }

    // ==========================================
    // 10. Motion & Physics Playground
    // ==========================================
    private VBox physicsPlaygroundPage() {
        Label desc = new Label("Interactive Mass-Spring-Damper sandbox. Tap or drag the interactive elements to experience"
                + " real-time physical momentum, continuous velocity preservation on interruptions, and expressive shape morphing.");
        desc.setWrapText(true);

        // Bouncy Interactive Box
        StackPane bouncyBox = new StackPane(new Label("Click Me!"));
        bouncyBox.setMinSize(120, 80);
        bouncyBox.setPrefSize(120, 80);
        bouncyBox.setStyle("-fx-background-color: -md-sys-color-primary; -fx-background-radius: 16px; -fx-text-fill: -md-sys-color-on-primary; -fx-cursor: hand;");
        bouncyBox.getChildren().get(0).setStyle("-fx-text-fill: -md-sys-color-on-primary; -fx-font-weight: bold;");

        // Live spring sliders
        M3Slider stiffnessSlider = new M3Slider(100, 1000, 450);
        Label stiffnessLabel = new Label("Stiffness: 450");
        stiffnessSlider.valueProperty().addListener((o, ov, nv) -> stiffnessLabel.setText("Stiffness: " + nv.intValue()));

        M3Slider dampingSlider = new M3Slider(0.2, 1.2, 0.55);
        Label dampingLabel = new Label("Damping Ratio: 0.55");
        dampingSlider.valueProperty().addListener((o, ov, nv) -> dampingLabel.setText(String.format("Damping Ratio: %.2f", nv.doubleValue())));

        bouncyBox.setOnMouseClicked(e -> {
            SpringParams params = new SpringParams(stiffnessSlider.getValue(), dampingSlider.getValue());
            SpringAnimation.animate(bouncyBox.scaleXProperty(), 1.35, params, () ->
                    SpringAnimation.animate(bouncyBox.scaleXProperty(), 1.0, params, null));
            SpringAnimation.animate(bouncyBox.scaleYProperty(), 1.35, params, () ->
                    SpringAnimation.animate(bouncyBox.scaleYProperty(), 1.0, params, null));
        });

        HBox controlsBox = new HBox(20,
                new VBox(6, stiffnessLabel, stiffnessSlider),
                new VBox(6, dampingLabel, dampingSlider));

        // Shape morphing demo
        M3ShapeMorph morphNode = new M3ShapeMorph(ShapeType.ROUNDED_RECTANGLE);
        morphNode.setPrefSize(120, 120);

        HBox morphButtons = new HBox(8);
        morphButtons.setAlignment(Pos.CENTER_LEFT);
        for (ShapeType st : ShapeType.values()) {
            M3Button btn = M3Button.tonal(st.name());
            btn.setOnAction(e -> morphNode.morphTo(st));
            morphButtons.getChildren().add(btn);
        }

        VBox morphSection = new VBox(12,
                new Label("Click any preset to morph smoothly with SpringAnimation:"),
                morphButtons,
                new StackPane(morphNode));

        return new VBox(20,
                section("Spring Physics Engine Sandbox", desc, bouncyBox, controlsBox),
                section("M3 Expressive Shape Morphing System", morphSection)
        );
    }

    // ==========================================
    // 11. Cyber Analytics & Charts
    // ==========================================
    private VBox chartsPage() {
        Label desc = new Label("High-performance Canvas-based M3 Spline Charts, Rounded Bar Charts, Multi-Ring Concentric Gauges, and Occupancy Heatmaps — all tied directly into the M3 dynamic theme.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: -md-sys-color-on-surface-variant;");

        // Spline Area Chart
        M3SplineChart splineChart = new M3SplineChart();
        M3DataSeries revSeries = new M3DataSeries("Monthly Revenue ($k)");
        revSeries.getData().addAll(
                new M3DataPoint("Jan", 42), new M3DataPoint("Feb", 48), new M3DataPoint("Mar", 55),
                new M3DataPoint("Apr", 62), new M3DataPoint("May", 78), new M3DataPoint("Jun", 85),
                new M3DataPoint("Jul", 92), new M3DataPoint("Aug", 88), new M3DataPoint("Sep", 96),
                new M3DataPoint("Oct", 110), new M3DataPoint("Nov", 118), new M3DataPoint("Dec", 128)
        );

        M3DataSeries costSeries = new M3DataSeries("Operating Costs ($k)");
        costSeries.getData().addAll(
                new M3DataPoint("Jan", 28), new M3DataPoint("Feb", 30), new M3DataPoint("Mar", 32),
                new M3DataPoint("Apr", 35), new M3DataPoint("May", 40), new M3DataPoint("Jun", 42),
                new M3DataPoint("Jul", 45), new M3DataPoint("Aug", 44), new M3DataPoint("Sep", 48),
                new M3DataPoint("Oct", 52), new M3DataPoint("Nov", 54), new M3DataPoint("Dec", 58)
        );

        splineChart.getSeriesList().addAll(revSeries, costSeries);
        splineChart.setPrefHeight(250);

        HBox splineControls = new HBox(16);
        splineControls.setAlignment(Pos.CENTER_LEFT);
        M3Switch fillToggle = new M3Switch(true);
        fillToggle.selectedProperty().addListener((obs, o, n) -> splineChart.setAreaFill(n));
        M3Switch gridToggle = new M3Switch(true);
        gridToggle.selectedProperty().addListener((obs, o, n) -> splineChart.setShowGrid(n));
        splineControls.getChildren().addAll(
                new Label("Gradient Area Fill:"), fillToggle,
                new Label("Dashed Gridlines:"), gridToggle,
                new Label("(Hover chart for interactive crosshair & tooltips)")
        );

        // Grouped Bar Chart
        M3BarChart barChart = new M3BarChart();
        M3DataSeries newMembers = M3DataSeries.of("New Memberships", 48, 65, 82, 74, 96, 110);
        M3DataSeries renewals = M3DataSeries.of("Renewals", 120, 135, 142, 158, 170, 185);
        barChart.getSeriesList().addAll(newMembers, renewals);
        barChart.setPrefHeight(220);

        // Donut Chart & Heatmap row
        M3DonutChart donutChart = new M3DonutChart();
        donutChart.setCenterHeadline("81%");
        donutChart.setCenterSubtitle("Daily Target Score");
        donutChart.addRing("Cardio Volume", 82, 100, null);
        donutChart.addRing("Strength Training", 68, 100, null);
        donutChart.addRing("Recovery & Sleep", 94, 100, null);
        donutChart.setPrefSize(240, 240);

        M3Heatmap heatmap = new M3Heatmap();
        // Populate realistic gym peak hour curve
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 16; c++) {
                int hour = 6 + c;
                double intensity = 0.15;
                if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 20)) {
                    intensity = 0.75 + (r % 3 == 0 ? 0.20 : 0.10);
                } else if (hour >= 12 && hour <= 14) {
                    intensity = 0.50;
                }
                heatmap.setValue(r, c, Math.min(1.0, intensity));
            }
        }
        heatmap.setPrefHeight(220);
        HBox.setHgrow(heatmap, Priority.ALWAYS);

        HBox radialAndHeatmap = new HBox(24,
                new VBox(10, new Label("Concentric Target Rings"), donutChart),
                new VBox(10, new Label("Weekly Peak-Hours Occupancy Heatmap (Mon–Sun × 06:00–21:00)"), heatmap));
        radialAndHeatmap.setAlignment(Pos.CENTER_LEFT);

        // Radar Chart (Athlete Performance Attributes)
        M3RadarChart radarChart = new M3RadarChart(
                "STRENGTH", "ENDURANCE", "AGILITY", "FLEXIBILITY", "SPEED", "RECOVERY"
        );
        M3DataSeries currentProfile = new M3DataSeries("Current Athlete Profile");
        currentProfile.getData().addAll(
                new M3DataPoint("STRENGTH", 88),
                new M3DataPoint("ENDURANCE", 74),
                new M3DataPoint("AGILITY", 82),
                new M3DataPoint("FLEXIBILITY", 60),
                new M3DataPoint("SPEED", 90),
                new M3DataPoint("RECOVERY", 78)
        );
        M3DataSeries benchmarkProfile = new M3DataSeries("Target Benchmark");
        benchmarkProfile.setColor(Color.web("#00E5FF"));
        benchmarkProfile.getData().addAll(
                new M3DataPoint("STRENGTH", 95),
                new M3DataPoint("ENDURANCE", 85),
                new M3DataPoint("AGILITY", 90),
                new M3DataPoint("FLEXIBILITY", 80),
                new M3DataPoint("SPEED", 92),
                new M3DataPoint("RECOVERY", 85)
        );
        radarChart.getSeriesList().addAll(currentProfile, benchmarkProfile);
        radarChart.setPrefSize(380, 320);

        // Radial Gauges
        M3GaugeChart hrGauge = new M3GaugeChart("Heart Rate", 40, 200, "BPM");
        hrGauge.setValue(156);
        hrGauge.setPrefSize(200, 200);

        M3GaugeChart powerGauge = new M3GaugeChart("Power Output", 0, 600, "WATTS");
        powerGauge.setValue(385);
        powerGauge.setPrefSize(200, 200);

        M3Slider gaugeSlider = new M3Slider(40, 200, 156);
        gaugeSlider.setPrefWidth(220);
        gaugeSlider.valueProperty().addListener((obs, o, n) -> {
            hrGauge.setValue(n.doubleValue());
            powerGauge.setValue(n.doubleValue() * 2.8);
        });

        VBox gaugeColumn = new VBox(12,
                new HBox(16, hrGauge, powerGauge),
                new HBox(10, new Label("Live Telemetry Simulator:"), gaugeSlider)
        );
        gaugeColumn.setAlignment(Pos.CENTER);

        HBox radarAndGaugeRow = new HBox(24,
                new VBox(10, new Label("Multidimensional Athlete Skill Radar (Hover vertices for values)"), radarChart),
                new VBox(10, new Label("Biometric Radial Telemetry Gauges (Spring Physics)"), gaugeColumn)
        );
        radarAndGaugeRow.setAlignment(Pos.CENTER_LEFT);

        // Velocity-Based Training (VBT) Scatter Plot & OLS Regression
        M3ScatterPlot scatterPlot = new M3ScatterPlot();
        scatterPlot.setXAxisTitle("Relative Mechanical Load (% of 1RM)");
        scatterPlot.setYAxisTitle("Mean Concentric Velocity (m/s)");
        scatterPlot.setShowRegression(true);
        scatterPlot.setCrosshairEnabled(true);
        scatterPlot.setPrefHeight(280);

        M3ScatterSeries squatVbt = new M3ScatterSeries("Back Squat VBT Profile", Color.web("#4CAF50"));
        squatVbt.getPoints().addAll(
                new M3ScatterPoint(40, 1.05, 12, "40% 1RM - 100kg (Warmup Velocity)"),
                new M3ScatterPoint(50, 0.94, 13, "50% 1RM - 125kg (Dynamic Speed)"),
                new M3ScatterPoint(60, 0.81, 14, "60% 1RM - 150kg (Strength-Speed)"),
                new M3ScatterPoint(70, 0.67, 16, "70% 1RM - 175kg (Accelerative Strength)"),
                new M3ScatterPoint(80, 0.51, 18, "80% 1RM - 200kg (Maximum Strength)"),
                new M3ScatterPoint(90, 0.35, 20, "90% 1RM - 225kg (Near-Maximal)"),
                new M3ScatterPoint(95, 0.24, 22, "95% 1RM - 238kg (1RM Threshold Velocity)")
        );

        M3ScatterSeries benchVbt = new M3ScatterSeries("Barbell Bench Press VBT Profile", Color.web("#00E5FF"));
        benchVbt.getPoints().addAll(
                new M3ScatterPoint(45, 0.92, 12, "45% 1RM - 65kg (Speed-Strength)"),
                new M3ScatterPoint(55, 0.79, 13, "55% 1RM - 80kg (Strength-Speed)"),
                new M3ScatterPoint(65, 0.64, 15, "65% 1RM - 95kg (Accelerative)"),
                new M3ScatterPoint(75, 0.49, 17, "75% 1RM - 110kg (Max Strength)"),
                new M3ScatterPoint(85, 0.34, 19, "85% 1RM - 125kg (Heavy Drive)"),
                new M3ScatterPoint(92, 0.21, 21, "92% 1RM - 135kg (Sticking Point Boundary)")
        );

        scatterPlot.getSeriesList().addAll(squatVbt, benchVbt);

        HBox scatterControls = new HBox(16);
        scatterControls.setAlignment(Pos.CENTER_LEFT);
        M3Switch regToggle = new M3Switch(true);
        regToggle.selectedProperty().addListener((obs, o, n) -> scatterPlot.setShowRegression(n));
        M3Switch crosshairToggle = new M3Switch(true);
        crosshairToggle.selectedProperty().addListener((obs, o, n) -> scatterPlot.setCrosshairEnabled(n));
        scatterControls.getChildren().addAll(
                new Label("Linear Regression Trendline (OLS y=mx+b):"), regToggle,
                new Label("Dynamic Crosshair & Readout:"), crosshairToggle,
                new Label("(Bubble radius corresponds to mechanical barbell load)")
        );

        // Member Acquisition & Conversion Funnel
        M3FunnelChart funnelChart = new M3FunnelChart();
        funnelChart.setPrefHeight(320);
        funnelChart.getStages().addAll(
                new M3FunnelStage("Website Visits & Digital Ad Reach", 12500, Color.web("#7E57C2")),
                new M3FunnelStage("Free 3-Day VIP Trial Passes Claimed", 5200, Color.web("#5C6BC0")),
                new M3FunnelStage("Attended Facility Tour & First Workout", 3400, Color.web("#26A69A")),
                new M3FunnelStage("Converted to Paid Tier (Basic / Premium / Pro)", 1850, Color.web("#4CAF50")),
                new M3FunnelStage("Retained Active at 90+ Days (Sticky Athletes)", 1520, Color.web("#29B6F6"))
        );

        // 52-Week Annual Member Workout Consistency Heatmap
        M3ActivityCalendar activityCalendar = new M3ActivityCalendar(2026);
        activityCalendar.setPrefHeight(190);
        java.time.LocalDate simDate = java.time.LocalDate.of(2026, 1, 1);
        java.util.Random rnd = new java.util.Random(42);
        while (simDate.getYear() == 2026) {
            java.time.DayOfWeek dow = simDate.getDayOfWeek();
            boolean isWorkoutDay = (dow != java.time.DayOfWeek.SUNDAY && rnd.nextDouble() > 0.28);
            if (isWorkoutDay) {
                int sets = 3 + rnd.nextInt(9);
                activityCalendar.setCount(simDate, sets);
            }
            simDate = simDate.plusDays(1);
        }

        Label streakBadge = new Label(
                String.format("🔥 Longest Streak: %d Days   |   ⚡ Current Streak: %d Days   |   🏋️ Total Workouts: %d Sessions",
                        activityCalendar.calculateLongestStreak(),
                        activityCalendar.calculateCurrentStreak(),
                        activityCalendar.calculateTotalCount())
        );
        streakBadge.setFont(javafx.scene.text.Font.font("Roboto", javafx.scene.text.FontWeight.BOLD, 12));
        streakBadge.setStyle("-fx-text-fill: -md-sys-color-primary; -fx-padding: 6px 12px; -fx-background-color: -md-sys-color-primary-container; -fx-background-radius: 8px;");

        VBox calendarSection = new VBox(10,
                streakBadge,
                activityCalendar
        );

        return new VBox(20,
                section("Cyber Analytics & Metrics Overview", desc),
                section("Velocity-Based Training (VBT) Scatter Plot & OLS Regression", scatterControls, scatterPlot),
                section("Gym Member Lifecycle & Conversion Funnel Chart", funnelChart),
                section("52-Week Annual Member Training Consistency Heatmap", calendarSection),
                section("M3 Smooth Spline Chart (Interactive Crosshair & Tooltips)", splineControls, splineChart),
                section("M3 Grouped Bar Chart (Pill-Top Rounded Columns)", barChart),
                section("Expressive Radial Gauges & Density Heatmap", radialAndHeatmap),
                section("Athletic Skill Matrix & Biometric Telemetry Speedometers", radarAndGaugeRow)
        );
    }

    // ==========================================
    // 12. 3D Hologram & Viewports
    // ==========================================
    private VBox scene3DPage() {
        Label desc = new Label("Hardware-accelerated JavaFX 3D viewports with orbit cameras, specular lighting, procedural 3D gym models, targeted human muscle visualizer, and holographic tilt parallax cards.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: -md-sys-color-on-surface-variant;");

        // 3D Tilt Card (Holographic VIP Member Card)
        VBox vipCardContent = new VBox(10);
        vipCardContent.setAlignment(Pos.CENTER_LEFT);
        vipCardContent.setPadding(new Insets(20));

        HBox vipHeader = new HBox(12);
        vipHeader.setAlignment(Pos.CENTER_LEFT);
        M3Icon vipIcon = M3Icon.symbol("star");
        vipIcon.setStyle(".m3-icon-svg { -fx-fill: -md-sys-color-primary; }");
        Label vipBadge = new Label("VIP DIAMOND ALL-INCLUSIVE");
        vipBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -md-sys-color-primary;");
        vipHeader.getChildren().addAll(vipIcon, vipBadge);

        Label memberName = new Label("ALEXANDER VANCE");
        memberName.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: -md-sys-color-on-surface;");

        Label memberDetails = new Label("Member ID: #M3-8842-GYM\nAccess: 24/7 Global Access & Hydro-Spa");
        memberDetails.setStyle("-fx-font-size: 12px; -fx-text-fill: -md-sys-color-on-surface-variant;");

        Label tiltHint = new Label("(Move mouse across card to test 3D holographic tilt & specular glare)");
        tiltHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -md-sys-color-primary; -fx-font-style: italic;");

        vipCardContent.getChildren().addAll(vipHeader, memberName, memberDetails, tiltHint);
        M3TiltCard3D tiltCard = new M3TiltCard3D(vipCardContent);
        tiltCard.setPrefSize(340, 200);

        // Gyro Rings 3D
        M3GyroRing3D gyroRing = new M3GyroRing3D();
        VBox gyroBox = new VBox(8, new Label("3D Gyroscope Hologram"), gyroRing);
        gyroBox.setAlignment(Pos.CENTER);

        HBox tiltAndGyroRow = new HBox(32,
                new VBox(8, new Label("Interactive 3D Parallax Tilt Card"), tiltCard),
                gyroBox);
        tiltAndGyroRow.setAlignment(Pos.CENTER_LEFT);

        // 3D Gym Equipment Inspector
        globalModelViewer = new M3ModelViewer3D(ModelType.DUMBBELL);
        M3ModelViewer3D modelViewer = globalModelViewer;
        modelViewer.setPrefSize(500, 320);

        FlowPane modelButtons = new FlowPane(Orientation.HORIZONTAL, 8, 8);
        modelButtons.setAlignment(Pos.CENTER_LEFT);
        for (ModelType mt : ModelType.values()) {
            String label = mt.name().replace('_', ' ');
            M3Button btn = M3Button.tonal(label);
            btn.setOnAction(e -> modelViewer.setCurrentModel(mt));
            modelButtons.getChildren().add(btn);
        }

        M3Button wireframeBtn = M3Button.outlined("Toggle Wireframe");
        wireframeBtn.setOnAction(e -> modelViewer.setWireframe(!modelViewer.isWireframe()));

        M3Button resetCamBtn = M3Button.text("Reset Camera");
        resetCamBtn.setOnAction(e -> modelViewer.resetCamera());

        modelButtons.getChildren().addAll(wireframeBtn, resetCamBtn);

        VBox modelSection = new VBox(10,
                new Label("Click a model preset and drag with mouse to orbit/pan (scroll wheel to zoom):"),
                modelButtons,
                modelViewer);

        // 3D Human Anatomy Viewer
        M3AnatomyViewer3D anatomyViewer = new M3AnatomyViewer3D();
        anatomyViewer.setPrefSize(500, 340);

        HBox anatomyButtons = new HBox(8);
        anatomyButtons.setAlignment(Pos.CENTER_LEFT);
        for (MuscleGroup mg : MuscleGroup.values()) {
            M3Button btn = M3Button.filled(mg.name());
            btn.setOnAction(e -> anatomyViewer.setActiveMuscle(mg));
            anatomyButtons.getChildren().add(btn);
        }

        VBox anatomySection = new VBox(10,
                new Label("Select workout muscle group to illuminate anatomical focus in 3D:"),
                anatomyButtons,
                anatomyViewer);

        return new VBox(20,
                section("Hardware-Accelerated 3D Engine & Holograms", desc),
                section("Interactive Holographic Parallax & 3D Gyro Rings", tiltAndGyroRow),
                section("3D Gym Equipment Inspector (Orbit Camera + Specular Shaders)", modelSection),
                section("3D Targeted Muscle Anatomy Visualizer", anatomySection)
        );
    }

    // ==========================================
    // 13. Futuristic Visual Effects & Shaders
    // ==========================================
    private VBox effectsPage() {
        Label desc = new Label("Atmospheric visual effects: frosted glassmorphism containers, neon glow elevation accents, kinetic ambient particle physics, and futuristic HUD scanline overlays.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: -md-sys-color-on-surface-variant;");

        // Glassmorphism Pane
        VBox glassContent = new VBox(12);
        glassContent.setAlignment(Pos.CENTER_LEFT);
        Label glassTitle = new Label("Cyber-Glass Translucent Pane");
        glassTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -md-sys-color-on-surface;");
        Label glassDesc = new Label("Frosted acrylic surface with real-time box blur, translucent surface container tinting, and 1px specular rim highlight.");
        glassDesc.setWrapText(true);
        glassDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: -md-sys-color-on-surface-variant;");

        M3Button glassAction = M3Button.filled("Glass Action Button");
        glassContent.getChildren().addAll(glassTitle, glassDesc, glassAction);

        M3GlassPane glassPane = new M3GlassPane(glassContent);
        glassPane.setPrefSize(420, 180);

        // Neon Glow Elevation
        M3Button glowBtn1 = M3Button.filled("Primary Neon Glow");
        M3GlowShadow.apply(glowBtn1, ColorRole.PRIMARY, 18.0, 0.4);

        M3Button glowBtn2 = M3Button.tonal("Tertiary Neon Glow");
        M3GlowShadow.apply(glowBtn2, ColorRole.TERTIARY, 18.0, 0.4);

        M3Button pulsingBtn = M3Button.elevated("Breathing Radar Pulse");
        pulsingBtn.setOnAction(e -> M3GlowShadow.animatePulse(pulsingBtn, Color.CYAN, 8.0, 24.0));

        HBox glowRow = new HBox(20, glowBtn1, glowBtn2, pulsingBtn);
        glowRow.setAlignment(Pos.CENTER_LEFT);

        // Kinetic Particle Canvas
        M3ParticleCanvas particleCanvas = new M3ParticleCanvas();
        particleCanvas.setPrefSize(600, 240);

        VBox particleSection = new VBox(10,
                new Label("Hover cursor over the particle field to repel nodes — click anywhere to trigger a shockwave spring ripple:"),
                particleCanvas);

        // Telemetry Scanline
        StackPane scanContainer = new StackPane();
        Label hudLabel = new Label("TELEMETRY VIEWPORT [SCANNING SECTOR 04]\nSTATUS: ACTIVE ONLINE (ALL SYSTEMS NOMINAL)");
        hudLabel.setStyle("-fx-font-size: 13px; -fx-font-family: monospace; -fx-text-fill: -md-sys-color-primary; -fx-padding: 30;");
        M3Scanline scanline = new M3Scanline();
        scanContainer.getChildren().addAll(hudLabel, scanline);
        scanContainer.setStyle("-fx-background-color: -md-sys-color-surface-container-high; -fx-background-radius: 12px;");
        scanContainer.setPrefHeight(120);

        // M3Hologram Cyber Frame Showcase
        VBox holoContent = new VBox(8);
        holoContent.getChildren().addAll(
                new Label("BIOMETRIC SCANNER HUB"),
                new Label("Hardware ID: #NEO-SENSOR-902-MK4"),
                new Label("Heart Rate Variability: 84ms (Peak Athletic Condition)"),
                new Label("Lactate Threshold: 3.8 mmol/L"),
                new M3Button("Calibrate Transponders", ButtonVariant.FILLED)
        );
        M3Hologram hologram = new M3Hologram(holoContent);
        hologram.setTitle("TELEMETRY MATRIX // REAL-TIME SYNC");
        hologram.setPrefSize(440, 190);

        // M3PulseBeacon Status Showcase
        HBox beaconRow = new HBox(28,
                new M3PulseBeacon(BeaconStatus.ONLINE, "Core Cloud Engine"),
                new M3PulseBeacon(BeaconStatus.SYNCING, "Real-Time Sensor Bus"),
                new M3PulseBeacon(BeaconStatus.WARNING, "HVAC Zone 03 High Load"),
                new M3PulseBeacon(BeaconStatus.CRITICAL, "Emergency Exit 2 Sensor"),
                new M3PulseBeacon(BeaconStatus.OFFLINE, "Backup Generator")
        );
        beaconRow.setAlignment(Pos.CENTER_LEFT);

        return new VBox(20,
                section("Atmospheric Shaders & Visual Effects", desc),
                section("Cyber-Glass Frosted Glassmorphism (M3GlassPane)", glassPane),
                section("Neon Glow Elevation & Atmospheric Accents (M3GlowShadow)", glowRow),
                section("Kinetic Particle Network & Reactive Shockwaves (M3ParticleCanvas)", particleSection),
                section("HUD Sci-Fi Telemetry & Scanline Overlay (M3Scanline)", scanContainer),
                section("Cybernetic Hologram Frame (M3Hologram)", hologram),
                section("Live Operational Telemetry Beacons (M3PulseBeacon)", beaconRow)
        );
    }

    // ==========================================
    // 14. Enterprise Data & Telemetry
    // ==========================================
    private VBox telemetryPage() {
        Label desc = new Label("High-density enterprise telemetry components: KPI stat tiles with trend sparklines, real-time searchable data grid with pagination, activity timeline, stepper wizard, and radial speed dial.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: -md-sys-color-on-surface-variant;");

        // 4 KPI Stat Cards with Sparklines
        M3StatCard card1 = new M3StatCard("Total Monthly Revenue", "$128,450", "+18.4%");
        card1.setSparkline(42, 55, 68, 72, 85, 96, 110, 128);

        M3StatCard card2 = new M3StatCard("Active Memberships", "2,840", "+12.1%");
        card2.setSparkline(180, 210, 205, 230, 250, 270, 284);

        M3StatCard card3 = new M3StatCard("Maintenance Alerts", "4 Pending", "-15.0%");
        card3.setDeltaPositive(true); // lower maintenance is positive
        card3.setSparkline(12, 10, 8, 9, 6, 5, 4);

        M3StatCard card4 = new M3StatCard("Facility Capacity", "91.5%", "+5.8%");
        card4.setSparkline(65, 70, 75, 82, 88, 89, 92);

        HBox kpiRow = new HBox(16, card1, card2, card3, card4);
        kpiRow.setAlignment(Pos.CENTER_LEFT);

        // High-Density Data Table
        record MemberData(String name, String tier, String status, double[] visits) {}
        M3DataTable<MemberData> table = new M3DataTable<>();
        table.getColumns().add(new M3DataTable.Column<>("Member Name", MemberData::name, 160));
        table.getColumns().add(new M3DataTable.Column<>("Membership Plan", MemberData::tier, 140));
        table.getColumns().add(new M3DataTable.Column<MemberData>("Status", MemberData::status, 110)
                .withCellFactory((item, val) -> {
                    Label badge = new Label(val);
                    if ("Active".equals(val)) {
                        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10px; " +
                                "-fx-background-color: -md-sys-color-primary-container; -fx-text-fill: -md-sys-color-on-primary-container;");
                    } else {
                        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10px; " +
                                "-fx-background-color: -md-sys-color-error-container; -fx-text-fill: -md-sys-color-on-error-container;");
                    }
                    return badge;
                }));
        table.getColumns().add(new M3DataTable.Column<MemberData>("Weekly Activity Trend", item -> "", 140)
                .withCellFactory((item, val) -> {
                    M3Sparkline sp = M3Sparkline.of(item.visits());
                    sp.setPrefSize(100, 24);
                    return sp;
                }));

        table.setPageSize(4);
        table.getItems().addAll(
                new MemberData("Alexander Vance", "VIP Diamond", "Active", new double[]{3, 4, 2, 5, 6, 5, 6}),
                new MemberData("Elena Rostova", "Pro Fitness Tier", "Active", new double[]{1, 2, 3, 2, 4, 3, 4}),
                new MemberData("Marcus Sterling", "VIP All-Inclusive", "Active", new double[]{4, 5, 4, 6, 5, 6, 7}),
                new MemberData("Samantha Chen", "Standard Gym Pass", "Expiring", new double[]{2, 1, 0, 1, 2, 1, 0}),
                new MemberData("David Kim", "Standard Gym Pass", "Active", new double[]{3, 2, 3, 4, 3, 4, 5}),
                new MemberData("Chloe Dubois", "Pro Tier + Spa", "Active", new double[]{2, 3, 4, 3, 5, 4, 5}),
                new MemberData("Liam Gallagher", "Student Access", "Expiring", new double[]{1, 1, 2, 1, 0, 1, 0}),
                new MemberData("Noah Martinez", "VIP Diamond", "Active", new double[]{5, 6, 5, 7, 6, 7, 8})
        );

        // Activity Timeline
        M3Timeline timeline = new M3Timeline();
        timeline.addItem("06:00 AM", "Gym Facility Opened", "Automated biometric turnstiles unlocked; HVAC set to 21°C.", Status.COMPLETED);
        timeline.addItem("08:30 AM", "Morning HIIT Power Session", "Trainer Marcus - 24 participants in Studio A.", Status.COMPLETED);
        timeline.addItem("11:45 AM", "Hydro-Massage Sanitation", "Scheduled ozone filtration check in Progress.", Status.ACTIVE);
        timeline.addItem("05:30 PM", "Sunset Powerlifting Meet", "Platform 3 booked for team qualifier.", Status.PENDING);

        // Stepper Wizard & Radial Speed Dial
        M3Stepper stepper = new M3Stepper("Member Profile", "Select Plan", "Payment Method", "Access Card Issued");
        stepper.setPrefWidth(550);

        HBox stepperControls = new HBox(12);
        stepperControls.setAlignment(Pos.CENTER);
        M3Button prevStepBtn = M3Button.outlined("Previous Step");
        prevStepBtn.setOnAction(e -> stepper.prevStep());
        M3Button nextStepBtn = M3Button.filled("Next Step");
        nextStepBtn.setOnAction(e -> stepper.nextStep());
        stepperControls.getChildren().addAll(prevStepBtn, nextStepBtn);

        VBox stepperSection = new VBox(12, stepper, stepperControls);
        stepperSection.setAlignment(Pos.CENTER);

        M3SpeedDial speedDial = new M3SpeedDial();
        speedDial.addAction("Add Member", M3Icon.symbol("person"), () -> overlay.showSnackbar("Add Member Shortcut Activated"));
        speedDial.addAction("Record Payment", M3Icon.symbol("money"), () -> overlay.showSnackbar("Payment Terminal Launched"));
        speedDial.addAction("Class Calendar", M3Icon.symbol("calendar"), () -> overlay.showSnackbar("Calendar Opened"));
        speedDial.addAction("Settings", M3Icon.symbol("settings"), () -> overlay.showSnackbar("System Config"));

        HBox stepperAndDialRow = new HBox(40,
                new VBox(10, new Label("Multi-Step Onboarding Stepper"), stepperSection),
                new VBox(10, new Label("Radial Speed Dial Wheel"), speedDial));
        stepperAndDialRow.setAlignment(Pos.CENTER_LEFT);

        return new VBox(20,
                section("Enterprise Telemetry & Operations", desc),
                section("High-Impact Telemetry Stat Cards with Sparklines", kpiRow),
                section("M3 Data Table (Search, Column Filters, Inline Badges, Pagination)", table),
                section("Operational Activity Timeline (Status Pipelines)", timeline),
                section("Workflow Steppers & Radial Speed Dial", stepperAndDialRow)
        );
    }

    // ==========================================
    // 15. Live Workout HUD & Biometrics
    // ==========================================
    private VBox workoutHudPage() {
        Label desc = new Label("Smart Gym Live Workout Cockpit: Kinetic repetition counter with spring physics bounce, rolling cadence velocity (reps/min), 5-zone sports science cardiac intensity bar, and HIIT interval timer.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: -md-sys-color-on-surface-variant;");

        M3WorkoutHUD hud = new M3WorkoutHUD();
        hud.setExerciseName("Barbell Bench Press — Hypertrophy Protocol");

        return new VBox(20,
                section("Live Athletic Telemetry Station", desc),
                section("Active Session Cockpit", hud)
        );
    }

    // ==========================================
    // 16. Interactive Gym Floorplan Map
    // ==========================================
    private VBox floorplanPage() {
        Label desc = new Label("Interactive 2D/2.5D Gym Facility Blueprint & Equipment Occupancy Map. Displays live equipment availability pins (Available, In Use, Maintenance). Click any equipment pin to inspect real-time specs or seamlessly jump into the 3D procedural model inspector!");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: -md-sys-color-on-surface-variant;");

        M3GymFloorMap floorMap = new M3GymFloorMap();
        floorMap.setOnInspect3D(node -> {
            navigateTo("3D Hologram & Viewports");
            if (globalModelViewer != null) {
                globalModelViewer.setCurrentModel(node.modelType);
            }
        });

        return new VBox(20,
                section("Facility Blueprint & Equipment Map", desc),
                section("Real-Time Fitness Floor Status (Click node to inspect in 3D)", floorMap)
        );
    }

    // ==========================================
    // 17. Gym App Screens (19 FXMLs Porting Viewer)
    // ==========================================
    private VBox gymAppScreensPage() {
        Label desc = new Label("Live interactive porting viewer for all 19 production-grade Material Design 3 FXML screens. Select any screen below to preview its layout, widgets, and controller injection in real-time.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: -md-sys-color-on-surface-variant;");

        LinkedHashMap<String, String> screensMap = new LinkedHashMap<>();
        screensMap.put("01. Authentication Vault (login.fxml)", "login.fxml");
        screensMap.put("02. Adaptive Application Shell (app-shell.fxml)", "app-shell.fxml");
        screensMap.put("03. Executive Operations Dashboard (dashboard.fxml)", "dashboard.fxml");
        screensMap.put("04. Member Directory & Search Roster (customer-list.fxml)", "customer-list.fxml");
        screensMap.put("05. Member 360° VIP Dossier (customer-profile.fxml)", "customer-profile.fxml");
        screensMap.put("06. Medical & Injury Clearance (medical-profile.fxml)", "medical-profile.fxml");
        screensMap.put("07. Athletic Transformation & VBT (progress.fxml)", "progress.fxml");
        screensMap.put("08. Coach Roster & Workload (trainer-list.fxml)", "trainer-list.fxml");
        screensMap.put("09. Coach Master Profile & Certs (trainer-profile.fxml)", "trainer-profile.fxml");
        screensMap.put("10. Coach Availability Matrix (availability.fxml)", "availability.fxml");
        screensMap.put("11. Kinetic Workout Routine Builder (workout.fxml)", "workout.fxml");
        screensMap.put("12. Live Workout Telemetry HUD (live-workout-hud.fxml)", "live-workout-hud.fxml");
        screensMap.put("13. Master Schedule & Booking (schedule.fxml)", "schedule.fxml");
        screensMap.put("14. Equipment 3D Inventory (equipment.fxml)", "equipment.fxml");
        screensMap.put("15. Gym Facility Blueprint Map (gym-floor-map.fxml)", "gym-floor-map.fxml");
        screensMap.put("16. Fast-Lane Attendance Check-In (attendance.fxml)", "attendance.fxml");
        screensMap.put("17. Membership Plan Tiers (membership.fxml)", "membership.fxml");
        screensMap.put("18. Cashless POS & Billing (payment.fxml)", "payment.fxml");
        screensMap.put("19. Staff RBAC Security & Audit (staff.fxml)", "staff.fxml");

        ComboBox<String> screenSelector = new ComboBox<>(FXCollections.observableArrayList(screensMap.keySet()));
        screenSelector.getSelectionModel().selectFirst();
        screenSelector.setPrefWidth(420);

        Label statusLabel = new Label("Loaded: login.fxml");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -md-sys-color-primary; -fx-padding: 6px 12px; -fx-background-color: -md-sys-color-primary-container; -fx-background-radius: 8px;");

        M3Button launchShellBtn = M3Button.filled("Launch App Shell Window");
        launchShellBtn.setGraphic(M3Icon.symbol("home"));
        launchShellBtn.setOnAction(e -> {
            try {
                new GymApp().start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        M3Button launchLoginBtn = M3Button.outlined("Launch Standalone Login");
        launchLoginBtn.setGraphic(M3Icon.symbol("settings"));
        launchLoginBtn.setOnAction(e -> {
            try {
                URL url = getClass().getResource("/fxml/login.fxml");
                if (url != null) {
                    Parent root = FXMLLoader.load(url);
                    Scene sc = new Scene(root, 960, 640);
                    M3Stylesheets.applyTo(sc, theme);
                    Stage st = new Stage();
                    st.setTitle("GYM-OS // Member & Staff Authentication");
                    st.setScene(sc);
                    st.show();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topControls = new HBox(12,
                new Label("Select Screen:"),
                screenSelector,
                statusLabel,
                spacer,
                launchShellBtn,
                launchLoginBtn);
        topControls.setAlignment(Pos.CENTER_LEFT);

        StackPane viewportHolder = new StackPane();
        viewportHolder.setMinHeight(700);
        viewportHolder.setPrefHeight(820);
        VBox.setVgrow(viewportHolder, Priority.ALWAYS);
        viewportHolder.setStyle("-fx-border-color: -md-sys-color-outline-variant; -fx-border-radius: 12px; -fx-background-color: -md-sys-color-surface-container-low; -fx-background-radius: 12px; -fx-padding: 8px;");

        Consumer<String> loadScreenIntoViewport = fxmlName -> {
            try {
                URL url = getClass().getResource("/fxml/" + fxmlName);
                if (url != null) {
                    FXMLLoader loader = new FXMLLoader(url);
                    Parent root = loader.load();
                    if (root instanceof Region region) {
                        region.setPrefHeight(Region.USE_COMPUTED_SIZE);
                        region.setMinHeight(Region.USE_PREF_SIZE);
                    }
                    if (hasNestedScrollPane(root)) {
                        viewportHolder.getChildren().setAll(root);
                    } else {
                        ScrollPane sp = new ScrollPane(root);
                        sp.setFitToWidth(true);
                        sp.setFitToHeight(false);
                        sp.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                        viewportHolder.getChildren().setAll(sp);
                    }
                    statusLabel.setText("Loaded: " + fxmlName + " (" + root.getClass().getSimpleName() + ")");
                }
            } catch (Exception ex) {
                Label err = new Label("Error loading /fxml/" + fxmlName + ": " + ex.getMessage());
                err.setStyle("-fx-text-fill: -md-sys-color-error;");
                viewportHolder.getChildren().setAll(err);
                ex.printStackTrace();
            }
        };

        screenSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selectedLabel) -> {
            if (selectedLabel != null) {
                String fxml = screensMap.get(selectedLabel);
                loadScreenIntoViewport.accept(fxml);
            }
        });

        // Load initial screen (login.fxml)
        loadScreenIntoViewport.accept("login.fxml");

        return new VBox(16,
                section("Gym Management System — 19 Interactive FXML Screens", desc),
                topControls,
                viewportHolder);
    }

    private static VBox section(String title, Node... content) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: -md-sys-color-primary; -fx-padding: 0 0 4 0;");
        VBox box = new VBox(10);
        box.getChildren().add(heading);
        box.getChildren().addAll(content);
        box.setStyle("-fx-background-color: -md-sys-color-surface; -fx-padding: 8 0 16 0;");
        return box;
    }

    private static boolean hasNestedScrollPane(Node node) {
        if (node instanceof ScrollPane) return true;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (hasNestedScrollPane(child)) return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
