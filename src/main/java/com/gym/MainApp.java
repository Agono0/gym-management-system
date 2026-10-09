package com.gym;

import io.m3fx.controls.M3Stylesheets;
import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.appbar.TopAppBarVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.common.M3Badge;
import io.m3fx.controls.common.M3Divider;
import io.m3fx.controls.common.M3Tooltip;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.fab.FabSize;
import io.m3fx.controls.fab.M3Fab;
import io.m3fx.controls.fab.M3FabMenu;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.internal.OverlayLayer;
import io.m3fx.controls.layout.M3AdaptiveScaffold;
import io.m3fx.controls.list.M3ListItem;
import io.m3fx.controls.menu.M3Select;
import io.m3fx.controls.nav.M3NavDestination;
import io.m3fx.controls.nav.M3NavigationBar;
import io.m3fx.controls.nav.M3NavigationDrawer;
import io.m3fx.controls.nav.M3NavigationRail;
import io.m3fx.controls.picker.M3DateDialog;
import io.m3fx.controls.picker.M3DatePicker;
import io.m3fx.controls.picker.M3TimePicker;
import io.m3fx.controls.progress.M3LinearProgress;
import io.m3fx.controls.progress.M3WavyProgress;
import io.m3fx.controls.search.M3SearchBar;
import io.m3fx.controls.segmented.M3SegmentedButton;
import io.m3fx.controls.selection.M3CheckBox;
import io.m3fx.controls.selection.M3RadioButton;
import io.m3fx.controls.selection.M3Switch;
import io.m3fx.controls.slider.M3RangeSlider;
import io.m3fx.controls.slider.M3Slider;
import io.m3fx.controls.snackbar.M3Snackbar;
import io.m3fx.controls.tabs.M3Tabs;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import io.m3fx.core.theme.M3Theme;
import io.m3fx.core.theme.ThemeVariant;
import java.util.List;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Gym app entry point and m3fx gallery: every library component on one page per section, with a
 * live seed picker, light/dark toggle and variant selector proving the theme system.
 */
public class MainApp extends Application {

    private M3Theme theme = M3Theme.baseline(false);
    private Scene scene;

    @Override
    public void start(Stage stage) {
        OverlayLayer overlay = new OverlayLayer();
        BorderPane root = new BorderPane();

        M3TopAppBar appBar = new M3TopAppBar("Gym Management", TopAppBarVariant.SMALL);
        appBar.setNavigationIcon(M3Icon.symbol("menu"));
        M3IconButton themeToggle = M3IconButton.standard(M3Icon.symbol("dark"));
        themeToggle.setAccessibleText("Toggle light/dark theme");
        appBar.setActions(themeToggle);

        HBox themeBar = buildThemeBar();
        VBox top = new VBox(appBar, themeBar);

        List<String> pages = List.of(
                "Buttons", "Selection", "Fields", "Navigation", "Overlays", "Sliders & progress");
        M3NavigationDrawer sidebar = new M3NavigationDrawer(
                List.of(
                        M3NavDestination.of("Buttons", "star"),
                        M3NavDestination.of("Selection", "check"),
                        M3NavDestination.of("Fields", "edit"),
                        M3NavDestination.of("Navigation", "menu"),
                        M3NavDestination.of("Overlays", "info"),
                        M3NavDestination.of("Sliders", "stats")),
                false);

        ScrollPane center = new ScrollPane();
        center.setFitToWidth(true);
        center.setPadding(new Insets(16));
        M3Snackbar snackbar = new M3Snackbar(overlay);
        sidebar.selectedIndexProperty().addListener((obs, oldV, index) ->
                center.setContent(buildPage(pages.get(index.intValue()), overlay, snackbar)));
        center.setContent(buildPage("Buttons", overlay, snackbar));

        themeToggle.setOnAction(e -> {
            theme = M3Theme.fromSeed(theme.seed(), theme.variant(), !theme.dark(), theme.contrast());
            M3Stylesheets.applyTo(scene, theme);
        });

        root.setTop(top);
        root.setLeft(sidebar);
        root.setCenter(center);
        sidebar.setPrefWidth(180);

        StackPane shell = new StackPane(root, overlay);
        scene = new Scene(shell, 1100, 750);
        M3Stylesheets.applyTo(scene, theme);
        stage.setTitle("Gym Management — m3fx gallery");
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildThemeBar() {
        ColorPicker seedPicker = new ColorPicker((Color) theme.seed());
        seedPicker.setAccessibleText("Seed color");
        M3Select<ThemeVariant> variantBox = new M3Select<>(
                FXCollections.observableArrayList(ThemeVariant.values()));
        variantBox.setValue(theme.variant());
        variantBox.setAccessibleText("Theme variant");
        M3Button darkToggle = M3Button.tonal("Dark");
        darkToggle.setToggleable(true);
        darkToggle.setSelected(theme.dark());
        Runnable retheme = () -> {
            theme = M3Theme.fromSeed(seedPicker.getValue(), variantBox.getValue(),
                    darkToggle.isSelected(), M3Theme.CONTRAST_STANDARD);
            M3Stylesheets.applyTo(scene, theme);
        };
        seedPicker.setOnAction(e -> retheme.run());
        variantBox.setOnAction(e -> retheme.run());
        darkToggle.setOnAction(e -> retheme.run());
        Label seedLabel = new Label("Seed:");
        seedLabel.getStyleClass().add("m3-section-title");
        HBox bar = new HBox(12, seedLabel, seedPicker, variantBox, darkToggle);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        return bar;
    }

    private VBox buildPage(String page, OverlayLayer overlay, M3Snackbar snackbar) {
        return switch (page) {
            case "Selection" -> selectionPage();
            case "Fields" -> fieldsPage(overlay);
            case "Navigation" -> navigationPage();
            case "Overlays" -> overlaysPage(overlay, snackbar);
            case "Sliders & progress" -> slidersPage();
            default -> buttonsPage();
        };
    }

    private VBox section(String title, javafx.scene.Node... children) {
        Label header = new Label(title);
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        header.getStyleClass().add("m3-section-title");
        VBox box = new VBox(8, header);
        box.getChildren().addAll(children);
        box.setPadding(new Insets(0, 0, 16, 0));
        return box;
    }

    private VBox buttonsPage() {
        FlowPane buttons = new FlowPane(8, 8,
                M3Button.filled("Filled"), M3Button.tonal("Tonal"), M3Button.outlined("Outlined"),
                M3Button.elevated("Elevated"), M3Button.text("Text"));
        M3Button disabled = M3Button.filled("Disabled");
        disabled.setDisable(true);
        buttons.getChildren().add(disabled);
        FlowPane icons = new FlowPane(8, 8,
                M3IconButton.standard(M3Icon.symbol("favorite")),
                M3IconButton.filled(M3Icon.symbol("favorite")),
                M3IconButton.tonal(M3Icon.symbol("favorite")),
                M3IconButton.outlined(M3Icon.symbol("favorite")));
        FlowPane fabs = new FlowPane(8, 8,
                new M3Fab(M3Icon.symbol("add"), FabSize.SMALL),
                new M3Fab(M3Icon.symbol("add"), FabSize.MEDIUM),
                new M3Fab(M3Icon.symbol("add"), FabSize.LARGE),
                M3Fab.extended("Extended", M3Icon.symbol("add")));
        M3FabMenu fabMenu = new M3FabMenu(M3Icon.symbol("add"));
        fabMenu.addAction("Workout", M3Icon.symbol("stats"), null)
                .addAction("Member", M3Icon.symbol("person"), null);
        M3Button toggleA = M3Button.tonal("Toggle me");
        toggleA.setToggleable(true);
        M3Button toggleB = M3Button.outlined("Toggle me");
        toggleB.setToggleable(true);
        FlowPane chips = new FlowPane(8, 8,
                M3Chip.assist("Assist"), M3Chip.filter("Filter"), M3Chip.input("Input"),
                M3Chip.suggestion("Suggestion"));
        return new VBox(section("Buttons", buttons), section("Toggle buttons", new HBox(8, toggleA, toggleB)),
                section("Icon buttons", icons),
                section("FAB", fabs), section("FAB menu (Expressive)", fabMenu),
                section("Chips", chips));
    }

    private VBox selectionPage() {
        M3CheckBox check = new M3CheckBox("Checkbox");
        M3CheckBox check2 = M3CheckBox.selected("Selected", true);
        M3CheckBox check3 = new M3CheckBox("Indeterminate");
        check3.setIndeterminate(true);
        M3RadioButton radio1 = new M3RadioButton("Option A");
        M3RadioButton radio2 = M3RadioButton.selected("Option B", true);
        javafx.scene.control.ToggleGroup group = new javafx.scene.control.ToggleGroup();
        radio1.setToggleGroup(group);
        radio2.setToggleGroup(group);
        M3Switch plain = new M3Switch(false);
        M3Switch on = new M3Switch(true);
        M3Switch iconSwitch = new M3Switch(true);
        iconSwitch.setShowIcon(true);
        return new VBox(
                section("Checkbox", new HBox(16, check, check2, check3)),
                section("Radio", new HBox(16, radio1, radio2)),
                section("Switch", new HBox(16, plain, on, iconSwitch)));
    }

    private VBox fieldsPage(OverlayLayer overlay) {        M3TextField filled = new M3TextField("Name", TextFieldVariant.FILLED);
        filled.setSupportingText("Supporting text");
        M3TextField outlined = new M3TextField("Email", TextFieldVariant.OUTLINED);
        outlined.setSupportingText("We'll never share it");
        M3TextField error = new M3TextField("Password", TextFieldVariant.OUTLINED);
        error.setError(true);
        error.setSupportingText("Too short");
        M3TextField counter = new M3TextField("Bio", TextFieldVariant.FILLED);
        counter.setMaxLength(120);
        M3SearchBar search = new M3SearchBar("Search members…");
        search.setOverlay(overlay);
        search.getSuggestions().addAll("Abdelrhman", "Ziad", "Yousef", "Abdel Raouf");
        return new VBox(section("Text fields", filled, outlined, error, counter),
                section("Search", search));
    }

    private VBox navigationPage() {
        List<M3NavDestination> dests = List.of(
                M3NavDestination.of("Home", "home"),
                M3NavDestination.of("Plan", "edit"),
                M3NavDestination.of("Stats", "stats"));
        M3NavigationBar bar = new M3NavigationBar(dests);
        M3NavigationRail rail = new M3NavigationRail(dests);
        rail.setPrefHeight(280);
        M3Tabs tabs = new M3Tabs(io.m3fx.controls.tabs.TabVariant.PRIMARY,
                new Tab("Members"), new Tab("Trainers"), new Tab("Payments"));
        M3SegmentedButton segmented = new M3SegmentedButton(List.of("Day", "Week", "Month"));
        segmented.setSelectedIndex(0);
        M3Badge badge = new M3Badge(3);
        M3Tooltip.install(bar, "Navigation bar");
        M3AdaptiveScaffold scaffold = new M3AdaptiveScaffold();
        scaffold.setDestinations(dests);
        scaffold.setContent(new Label("Resize the window: bar ↔ rail ↔ drawer"));
        scaffold.setPrefHeight(320);
        Label sizeLabel = new Label();
        Runnable showSize = () -> sizeLabel.setText("Window class: " + scaffold.getSizeClass());
        scaffold.sizeClassProperty().addListener((obs, oldV, newV) -> showSize.run());
        showSize.run();
        return new VBox(section("Navigation bar", bar), section("Rail + badge", new HBox(16, rail, badge)),
                section("Tabs", tabs), section("Segmented", segmented),
                section("Adaptive scaffold (live class above the demo)", sizeLabel, scaffold));
    }

    private VBox overlaysPage(OverlayLayer overlay, M3Snackbar snackbar) {
        M3Button openDialog = M3Button.filled("Open dialog");
        openDialog.setOnAction(e -> M3Dialog.basic("Freeze membership?",
                new Label("The member keeps access until Friday."),
                M3Button.text("Cancel"), M3Button.text("Confirm")).showIn(overlay));
        M3Button showSnack = M3Button.tonal("Show snackbar");
        showSnack.setOnAction(e -> snackbar.show("Saved", "Undo", () -> {}));
        M3Button pickDate = M3Button.outlined("Pick date");
        pickDate.setOnAction(e -> M3DateDialog.showIn(overlay,
                date -> snackbar.show("Picked " + date)));
        M3Card card = M3Card.of(CardVariant.ELEVATED, new Label("Elevated card body"));
        M3Card outlined = M3Card.of(CardVariant.OUTLINED, new Label("Outlined card body"));
        M3ListItem item = new M3ListItem("Yousef", "Premium · expires 12 Jun", M3ListItem.TWO_LINES);
        item.setLeading(M3Icon.symbol("person"));
        return new VBox(section("Dialog, snackbar & date", new HBox(8, openDialog, showSnack, pickDate)),
                section("Cards", new HBox(8, card, outlined)),
                section("List + divider", item, new M3Divider()));
    }

    private VBox slidersPage() {
        M3Slider continuous = new M3Slider(0, 100, 60);
        M3Slider discreteSlider = M3Slider.discrete(0, 10, 4, 1);
        M3RangeSlider range = new M3RangeSlider(0, 100, 25, 75);
        M3LinearProgress determinate = new M3LinearProgress(0.6);
        M3LinearProgress indeterminate = new M3LinearProgress();
        M3WavyProgress wavyDeterminate = new M3WavyProgress(0.6);
        M3WavyProgress wavy = new M3WavyProgress();
        return new VBox(section("Sliders", continuous, discreteSlider, range),
                section("Progress", determinate, indeterminate,
                        M3LinearProgress.circular(M3LinearProgress.INDETERMINATE)),
                section("Wavy (Expressive)", wavyDeterminate, wavy),
                section("Pickers", new HBox(8, new M3DatePicker(), new M3TimePicker())));
    }

    /**
     * App entry point.
     *
     * @param args CLI args
     */
    public static void main(String[] args) {
        launch(args);
    }
}
