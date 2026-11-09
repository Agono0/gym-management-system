package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.carousel.M3Carousel;
import io.m3fx.controls.chart.M3DataPoint;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.chart.M3RadarChart;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.search.M3SearchBar;
import io.m3fx.controls.textfield.M3TextField;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the Trainer List screen.
 * UI-contract only (no backend/service dependencies): owns the demo roster, the domain-chip
 * and search filtering, the sort menu, the onboarding dialog and the on-duty carousel.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former trainer-list.fxml: large app bar, stat bento, on-duty carousel, glass filter strip,
 * master roster table, sports-science radar + heatmap footer.
 */
public class TrainerListController implements ScreenController {

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private VBox viewRoot;
    private M3TopAppBar topAppBar;
    private M3StatCard statTotalCoaches;
    private M3StatCard statCapacitySaturation;
    private M3StatCard statTodaySlots;
    private M3StatCard statAverageRate;
    private M3IconButton btnCarouselPrev;
    private M3IconButton btnCarouselNext;
    private M3Carousel carouselCoaches;
    private M3GlassPane filterGlassPane;
    private M3TextField txtSearch;
    private M3Chip chipFilterAll;
    private M3Chip chipFilterStrength;
    private M3Chip chipFilterHypertrophy;
    private M3Chip chipFilterRehab;
    private M3Chip chipFilterCardio;
    private M3SplitButton btnSortOptions;
    private M3Button btnAddTrainer;
    private M3DataTable<String> tableTrainers;
    private TableColumn<String, ?> colId;
    private TableColumn<String, ?> colName;
    private TableColumn<String, ?> colSpecialization;
    private TableColumn<String, ?> colExperience;
    private TableColumn<String, ?> colRate;
    private TableColumn<String, ?> colAssignedAthletes;
    private TableColumn<String, ?> colCapacityGauge;
    private TableColumn<String, ?> colActions;
    private M3Card cardSpecializationRadar;
    private M3RadarChart chartSpecialization;
    private M3Card cardFacilityDensity;
    private M3Heatmap heatmapBookingDensity;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Demo roster rows in "id|name|specialization|exp|rate|athletes|saturation|email" shape. */
    private final List<String> demoTrainers = new ArrayList<>(List.of(
            "101|Elena Rostova|Strength|8|74.50|23|92%|e.rostova@cybercoregym.io",
            "102|Marcus Vance|Hypertrophy|11|82.00|19|79%|m.vance@cybercoregym.io",
            "103|Kim Park|Post-Rehab|6|68.00|14|56%|k.park@cybercoregym.io",
            "104|Ana Silva|Cardio & VO2|4|58.00|21|84%|a.silva@cybercoregym.io",
            "105|Leo Martins|Strength|9|76.00|17|68%|l.martins@cybercoregym.io",
            "106|Zoe Chen|Hypertrophy|5|62.50|16|64%|z.chen@cybercoregym.io",
            "107|Omar Haddad|Cardio & VO2|7|66.00|22|88%|o.haddad@cybercoregym.io",
            "108|Sofia Reyes|Post-Rehab|3|54.00|9|36%|s.reyes@cybercoregym.io",
            "109|Dana Wolfe|Strength|12|90.00|24|96%|d.wolfe@cybercoregym.io",
            "110|Nils Berg|Hypertrophy|2|52.00|11|44%|n.berg@cybercoregym.io",
            "111|Maya Lin|Post-Rehab|10|78.00|15|60%|m.lin@cybercoregym.io",
            "112|Tomas Rivera|Cardio & VO2|6|64.00|18|72%|t.rivera@cybercoregym.io",
            "113|Ava Kowalski|Strength|1|50.00|8|32%|a.kowalski@cybercoregym.io",
            "114|Jonas Weber|Hypertrophy|8|72.00|20|80%|j.weber@cybercoregym.io",
            "115|Priya Nair|Post-Rehab|5|60.00|12|48%|p.nair@cybercoregym.io",
            "116|Igor Petrov|Cardio & VO2|9|70.00|13|52%|i.petrov@cybercoregym.io"));

    /**
     * Builds the complete Coaching &amp; Athletic Staff Management view and initializes all
     * behavior. Same ordering semantics the FXMLLoader used to provide: the node tree exists
     * and every field is assigned before the former {@code initialize()} work runs.
     */
    public TrainerListController() {
        // Column declarations wired onto tableTrainers in wireRosterColumns()
        // (M3DataTable.Column is not declarable in markup).
        colId = column("ID", 70, true);
        colName = column("Coach Full Name", 180, true);
        colSpecialization = column("Primary Specialization", 160, true);
        colExperience = column("Exp (Yrs)", 90, true);
        colRate = column("Hourly Rate", 110, true);
        colAssignedAthletes = column("Active Athletes", 120, true);
        colCapacityGauge = column("Capacity Saturation", 180, true);
        colActions = column("Actions", 160, false);

        viewRoot = buildView();

        installFeedbackLayer(topAppBar);
        wireRosterColumns();
        keepDomainChipsExclusive();
        wireSearchAndChips();
        wireSortMenu();

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        topAppBar.setId("topAppBar");
        statTotalCoaches.setId("statTotalCoaches");
        statCapacitySaturation.setId("statCapacitySaturation");
        statTodaySlots.setId("statTodaySlots");
        statAverageRate.setId("statAverageRate");
        btnCarouselPrev.setId("btnCarouselPrev");
        btnCarouselNext.setId("btnCarouselNext");
        carouselCoaches.setId("carouselCoaches");
        filterGlassPane.setId("filterGlassPane");
        txtSearch.setId("txtSearch");
        chipFilterAll.setId("chipFilterAll");
        chipFilterStrength.setId("chipFilterStrength");
        chipFilterHypertrophy.setId("chipFilterHypertrophy");
        chipFilterRehab.setId("chipFilterRehab");
        chipFilterCardio.setId("chipFilterCardio");
        btnSortOptions.setId("btnSortOptions");
        btnAddTrainer.setId("btnAddTrainer");
        tableTrainers.setId("tableTrainers");
        colId.setId("colId");
        colName.setId("colName");
        colSpecialization.setId("colSpecialization");
        colExperience.setId("colExperience");
        colRate.setId("colRate");
        colAssignedAthletes.setId("colAssignedAthletes");
        colCapacityGauge.setId("colCapacityGauge");
        colActions.setId("colActions");
        cardSpecializationRadar.setId("cardSpecializationRadar");
        chartSpecialization.setId("chartSpecialization");
        cardFacilityDensity.setId("cardFacilityDensity");
        heatmapBookingDensity.setId("heatmapBookingDensity");
        populateCarousel();
        populateCharts();
        applyFilters();
    }

    /** One roster table column definition (header text + preferred width). */
    private static TableColumn<String, ?> column(String text, double prefWidth, boolean sortable) {
        TableColumn<String, ?> col = new TableColumn<>(text);
        col.setPrefWidth(prefWidth);
        col.setSortable(sortable);
        return col;
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for trainer-list.fxml) and
     * wires every handler.
     */
    private VBox buildView() {
        VBox root = new VBox();
        root.setSpacing(0);
        root.getStyleClass().add("trainer-list-root");

        topAppBar = new M3TopAppBar("Coaching & Athletic Staff Management",
                io.m3fx.controls.appbar.TopAppBarVariant.LARGE);

        ScrollPane workspaceScroll = new ScrollPane();
        workspaceScroll.setFitToWidth(true);
        workspaceScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        workspaceScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(workspaceScroll, Priority.ALWAYS);

        VBox workspace = new VBox(20);
        workspace.setPadding(new Insets(24, 24, 64, 24));

        // Stat card bento grid (reflows to 2x2 at ~804px content width instead of clipping).
        FlowPane statGrid = new FlowPane(16, 16);
        statGrid.setAlignment(Pos.CENTER_LEFT);
        statTotalCoaches = new M3StatCard("TOTAL ACTIVE COACHES", "16", "+2 this month");
        statCapacitySaturation = new M3StatCard("STAFF CAPACITY UTILIZATION", "84.2%", "182 / 216 Clients");
        statTodaySlots = new M3StatCard("SLOTS BOOKED TODAY", "68 / 82", "82.9% Fill Rate");
        statAverageRate = new M3StatCard("AVERAGE HOURLY RATE", "$74.50", "Min $50.00 / Max $120.00");
        statGrid.getChildren().addAll(statTotalCoaches, statCapacitySaturation, statTodaySlots, statAverageRate);

        // Featured on-duty carousel header.
        VBox carouselSection = new VBox(12);
        HBox carouselHeader = new HBox();
        carouselHeader.setAlignment(Pos.CENTER_LEFT);
        btnCarouselPrev = new M3IconButton(M3Icon.symbol("chevron_left"), IconButtonVariant.STANDARD);
        btnCarouselPrev.setAccessibleText("Previous Coach Cards");
        btnCarouselPrev.setOnAction(e -> onCarouselPrev());
        btnCarouselNext = new M3IconButton(M3Icon.symbol("chevron_right"), IconButtonVariant.STANDARD);
        btnCarouselNext.setAccessibleText("Next Coach Cards");
        btnCarouselNext.setOnAction(e -> onCarouselNext());
        Region carouselSpacer = new Region();
        HBox.setHgrow(carouselSpacer, Priority.ALWAYS);
        carouselHeader.getChildren().addAll(
                styledLabel("On-Duty Athletic Coaches Today", "title-medium"),
                carouselSpacer,
                btnCarouselPrev,
                btnCarouselNext);
        carouselCoaches = new M3Carousel();
        carouselCoaches.setPrefHeight(220);
        carouselSection.getChildren().addAll(carouselHeader, carouselCoaches);

        // Search, filter & command strip (glass pane) — rows wrap instead of clipping at
        // ~804px content width: search + commands on row 1, domain chips reflow on row 2.
        filterGlassPane = new M3GlassPane();
        filterGlassPane.getStyleClass().add("filter-strip-container");
        VBox filterContent = new VBox(12);
        filterContent.setPadding(new Insets(16));
        HBox commandRow = new HBox(12);
        commandRow.setAlignment(Pos.CENTER_LEFT);
        txtSearch = new M3TextField("Search coaches...", io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        txtSearch.setSupportingText("Filters the roster below by name, specialization, or email");
        txtSearch.setPrefWidth(380);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        btnSortOptions = new M3SplitButton("Sort: Saturation", ButtonVariant.FILLED, null);
        btnAddTrainer = new M3Button("+ Onboard Coach", ButtonVariant.FILLED);
        btnAddTrainer.setOnAction(e -> onAdd());
        commandRow.getChildren().addAll(
                txtSearch,
                new Separator(Orientation.VERTICAL),
                btnSortOptions,
                btnAddTrainer);
        FlowPane domainChips = new FlowPane(8, 8);
        domainChips.setAlignment(Pos.CENTER_LEFT);
        chipFilterAll = new M3Chip("All (16)", ChipVariant.ASSIST);
        chipFilterAll.setSelected(true);
        chipFilterStrength = new M3Chip("Strength", ChipVariant.ASSIST);
        chipFilterHypertrophy = new M3Chip("Hypertrophy", ChipVariant.ASSIST);
        chipFilterRehab = new M3Chip("Post-Rehab", ChipVariant.ASSIST);
        chipFilterCardio = new M3Chip("Cardio & VO2", ChipVariant.ASSIST);
        domainChips.getChildren().addAll(
                styledLabel("Domain:", "label-small"),
                chipFilterAll, chipFilterStrength, chipFilterHypertrophy, chipFilterRehab, chipFilterCardio);
        filterContent.getChildren().addAll(commandRow, domainChips);
        filterGlassPane.setContent(filterContent);

        // Master table roster.
        VBox rosterSection = new VBox(12);
        rosterSection.getChildren().add(styledLabel("Master Coaching Roster", "title-medium"));
        tableTrainers = new M3DataTable<>();
        tableTrainers.setMinHeight(360);
        VBox.setVgrow(tableTrainers, Priority.ALWAYS);
        rosterSection.getChildren().add(tableTrainers);

        // Sports science HUD footer (radar & heatmap).
        HBox chartFooter = new HBox(20);
        cardSpecializationRadar = new M3Card();
        HBox.setHgrow(cardSpecializationRadar, Priority.ALWAYS);
        VBox radarContent = new VBox(8);
        radarContent.getChildren().addAll(
                styledLabel("Coaching Domain Coverage", "title-medium"),
                radarChart());
        cardSpecializationRadar.setContent(radarContent);
        cardFacilityDensity = new M3Card();
        HBox.setHgrow(cardFacilityDensity, Priority.ALWAYS);
        VBox densityContent = new VBox(8);
        heatmapBookingDensity = new M3Heatmap();
        heatmapBookingDensity.setPrefHeight(240);
        densityContent.getChildren().addAll(
                styledLabel("Facility Weekly Booking Density", "title-medium"),
                heatmapBookingDensity);
        cardFacilityDensity.setContent(densityContent);
        chartFooter.getChildren().addAll(cardSpecializationRadar, cardFacilityDensity);

        workspace.getChildren().addAll(statGrid, carouselSection, filterGlassPane, rosterSection, chartFooter);
        workspaceScroll.setContent(workspace);
        root.getChildren().addAll(topAppBar, workspaceScroll);
        return root;
    }

    /** The domain-coverage radar chart with its demo series. */
    private M3RadarChart radarChart() {
        chartSpecialization = new M3RadarChart();
        chartSpecialization.setPrefHeight(240);
        return chartSpecialization;
    }

    /** Small label with one style class. */
    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    @Override
    public Parent getRoot() {
        return viewRoot;
    }

    /**
     * Maps the column definitions (header text + preferred width) onto the M3DataTable:
     * its own {@code Column} model is code-only, so without this wiring the roster renders
     * with no header row. Extractors are pipe-index based so each column shows its own field
     * and header sorting works; the Actions column carries a blank extractor (the table
     * requires one once rows exist) and renders an Open button per row via its cell factory
     * instead.
     */
    private void wireRosterColumns() {
        javafx.collections.ObservableList<M3DataTable.Column<String>> columns = tableTrainers.getColumns();
        columns.add(new M3DataTable.Column<>("ID", r -> field(r, 0), colId.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colName.getText(), r -> field(r, 1), colName.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colSpecialization.getText(), r -> field(r, 2), colSpecialization.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colExperience.getText(), r -> field(r, 3), colExperience.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colRate.getText(), r -> "$" + field(r, 4), colRate.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAssignedAthletes.getText(), r -> field(r, 5), colAssignedAthletes.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colCapacityGauge.getText(), r -> field(r, 6), colCapacityGauge.getPrefWidth()));
        columns.add(new M3DataTable.Column<String>(colActions.getText(), r -> "", colActions.getPrefWidth())
                .withCellFactory((row, s) -> {
                    M3Button open = M3Button.text("Open");
                    open.setOnAction(e -> onOpenProfile(Integer.parseInt(field(row, 0))));
                    return open;
                }));
        // The labeled txtSearch field above the table is this screen's search affordance; the
        // table's built-in unlabeled search bar would duplicate it, so it is not shown here.
        tableTrainers.getChildren().removeIf(n -> n instanceof M3SearchBar);
        tableTrainers.setOnRowAction(this::openProfileRow);
    }

    /** Puts the five domain chips in one ToggleGroup so exactly one filter is active at a time. */
    private void keepDomainChipsExclusive() {
        ToggleGroup domainGroup = new ToggleGroup();
        domainGroup.getToggles().addAll(
                chipFilterAll, chipFilterStrength, chipFilterHypertrophy, chipFilterRehab, chipFilterCardio);
    }

    /** Wires the search field and the domain chips to the visible roster (live filtering). */
    private void wireSearchAndChips() {
        txtSearch.getInput().textProperty().addListener((obs, o, v) -> applyFilters());
        chipFilterAll.setOnAction(e -> applyFilters());
        chipFilterStrength.setOnAction(e -> applyFilters());
        chipFilterHypertrophy.setOnAction(e -> applyFilters());
        chipFilterRehab.setOnAction(e -> applyFilters());
        chipFilterCardio.setOnAction(e -> applyFilters());
    }

    /** Attaches the sort menu to the split button: choosing an entry reorders the visible roster. */
    private void wireSortMenu() {
        M3Menu sortMenu = new M3Menu()
                .item("Sort: Saturation", () -> sortByField(6, true))
                .item("Sort: Experience", () -> sortByField(3, false))
                .item("Sort: Hourly Rate", () -> sortByField(4, false))
                .item("Sort: Name", () -> sortByField(1, true));
        btnSortOptions.setMenu(sortMenu);
    }

    /** Sorts the demo roster by the chosen pipe field and refreshes the visible rows. */
    private void sortByField(int fieldIndex, boolean ascending) {
        Comparator<String> byField = Comparator.comparing(r -> field(r, fieldIndex).toLowerCase(Locale.ROOT));
        if (fieldIndex == 3 || fieldIndex == 4 || fieldIndex == 6) {
            byField = Comparator.comparingDouble(r -> parseNumber(field(r, fieldIndex)));
        }
        demoTrainers.sort(ascending ? byField : byField.reversed());
        applyFilters();
    }

    /** Applies the active domain chip plus the search query to the visible roster. */
    private void applyFilters() {
        String query = txtSearch.getText() != null
                ? txtSearch.getText().trim().toLowerCase(Locale.ROOT)
                : "";
        String domain = null;
        if (chipFilterStrength.isSelected()) {
            domain = "strength";
        } else if (chipFilterHypertrophy.isSelected()) {
            domain = "hypertrophy";
        } else if (chipFilterRehab.isSelected()) {
            domain = "rehab";
        } else if (chipFilterCardio.isSelected()) {
            domain = "cardio";
        }
        List<String> visible = new ArrayList<>();
        for (String row : demoTrainers) {
            boolean matchesDomain = domain == null || field(row, 2).toLowerCase(Locale.ROOT).contains(domain);
            boolean matchesQuery = query.isEmpty() || row.toLowerCase(Locale.ROOT).contains(query);
            if (matchesDomain && matchesQuery) {
                visible.add(row);
            }
        }
        tableTrainers.getItems().setAll(visible);
        chipFilterAll.setText("All (" + demoTrainers.size() + ")");
    }

    /** Fills the on-duty carousel with one summary card per featured coach. */
    private void populateCarousel() {
        String[][] featured = {
                {"ER", "Elena Rostova", "Olympic Weightlifting · on duty 07:00-15:00"},
                {"MV", "Marcus Vance", "Hypertrophy Blocks · on duty 09:00-17:00"},
                {"KP", "Kim Park", "Post-Rehab Return-to-Play · on duty 11:00-19:00"},
                {"AS", "Ana Silva", "VO2 Intervals · on duty 06:00-14:00"},
                {"LM", "Leo Martins", "Powerbuilding · on duty 13:00-21:00"},
                {"OH", "Omar Haddad", "Conditioning Circuits · on duty 08:00-16:00"}};
        for (String[] f : featured) {
            VBox body = new VBox(6);
            body.setAlignment(Pos.CENTER_LEFT);
            body.setPadding(new Insets(16));
            Label initials = new Label(f[0]);
            initials.getStyleClass().add("m3-stat-value");
            Label name = new Label(f[1]);
            name.getStyleClass().add("m3-section-title");
            Label detail = new Label(f[2]);
            detail.setWrapText(true);
            body.getChildren().addAll(initials, name, detail);
            M3Card card = new M3Card();
            card.setContent(body);
            card.setPrefSize(260, 188);
            carouselCoaches.getItems().add(card);
        }
    }

    /** Fills the radar (domain coverage) and the heatmap (weekly booking density). */
    private void populateCharts() {
        chartSpecialization.getAxes().addAll("Strength", "Hypertrophy", "Rehab", "Cardio", "Mobility", "Nutrition");
        M3DataSeries coverage = new M3DataSeries("Team Coverage");
        coverage.getData().addAll(
                new M3DataPoint("Strength", 92),
                new M3DataPoint("Hypertrophy", 78),
                new M3DataPoint("Rehab", 61),
                new M3DataPoint("Cardio", 84),
                new M3DataPoint("Mobility", 55),
                new M3DataPoint("Nutrition", 47));
        chartSpecialization.getSeriesList().addAll(coverage);

        heatmapBookingDensity.setDimensions(7, 16);
        heatmapBookingDensity.setRowLabels(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
        for (int day = 0; day < 7; day++) {
            for (int slot = 0; slot < 16; slot++) {
                int hour = slot + 6;
                double intensity = 0.15;
                if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 20)) {
                    intensity = 0.75 + (day % 3 == 0 ? 0.20 : 0.10);
                } else if (hour >= 12 && hour <= 14) {
                    intensity = 0.50;
                } else if (day >= 5) {
                    intensity = 0.35;
                }
                heatmapBookingDensity.setValue(day, slot, Math.min(1.0, intensity));
            }
        }
    }

    /** Returns the requested pipe field of a demo row, or an empty string when missing. */
    private static String field(String row, int index) {
        String[] parts = row.split("\\|");
        return index < parts.length ? parts[index] : "";
    }

    /** Parses a numeric demo field (rate, years, percent) tolerantly for sorting. */
    private static double parseNumber(String raw) {
        try {
            return Double.parseDouble(raw.replace("%", "").replace("$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Row/activation entry point shared by the row click and the per-row Open button. */
    private void openProfileRow(String row) {
        onOpenProfile(Integer.parseInt(field(row, 0)));
    }

    /**
     * Opens the chosen coach's dossier: navigates the shell to trainer-profile (firing the
     * pinned {@code navTrainers} rail destination on the way, exactly like a rail click) and
     * hands the coach's roster data over to {@link TrainerProfileController#showTrainer}.
     * Standalone previews and tests without the owning shell answer with a snackbar.
     */
    public void onOpenProfile(int trainerId) {
        String row = demoTrainers.stream()
                .filter(r -> field(r, 0).equals(String.valueOf(trainerId)))
                .findFirst().orElse(null);
        if (row == null) {
            feedback.showSnackbar("Coach #" + trainerId + " is not on the roster (demo).");
            return;
        }
        TrainerProfileController.TrainerSnapshot snapshot = snapshotOf(row);
        AppShellController shell = shellFromScene();
        if (shell != null) {
            shell.openScreenViaRail("navTrainers", Screen.TRAINER_PROFILE, controller -> {
                if (controller instanceof TrainerProfileController profile) {
                    profile.showTrainer(snapshot);
                }
            });
            return;
        }
        feedback.showSnackbar("Opening coach profile — " + field(row, 1) + " (standalone preview)");
    }

    /** Builds the profile-screen payload from a roster row (demo fields only, no backend). */
    private TrainerProfileController.TrainerSnapshot snapshotOf(String row) {
        return new TrainerProfileController.TrainerSnapshot(
                Integer.parseInt(field(row, 0)),
                field(row, 1),
                field(row, 2),
                (int) Math.round(parseNumber(field(row, 3))),
                parseNumber(field(row, 4)),
                (int) Math.round(parseNumber(field(row, 5))),
                (int) Math.round(parseNumber(field(row, 6))),
                field(row, 7));
    }

    /**
     * The owning shell controller, published in the scene-root properties by
     * {@link AppShellController} ({@code AppShellController.SHELL_CONTROLLER_KEY}); null in
     * standalone previews and tests.
     */
    private AppShellController shellFromScene() {
        if (topAppBar.getScene() == null
                || !(topAppBar.getScene().getRoot() instanceof javafx.scene.Parent sceneRoot)) {
            return null;
        }
        Object shell = sceneRoot.getProperties().get(AppShellController.SHELL_CONTROLLER_KEY);
        return shell instanceof AppShellController controller ? controller : null;
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

    public void onCarouselPrev() {
        carouselCoaches.scrollPrev();
    }

    public void onCarouselNext() {
        carouselCoaches.scrollNext();
    }

    /** Opens the onboarding dialog; confirming appends the coach to the visible roster. */
    public void onAdd() {
        M3TextField nameField = new M3TextField();
        nameField.setLabelText("Coach full name");
        M3Button cancel = M3Button.text("Cancel");
        M3Button confirm = M3Button.filled("Onboard");
        M3Dialog dialog = M3Dialog.basic(
                "Onboard a new coach",
                new Label("The coach joins the demo roster with a Starter capacity profile."),
                cancel, confirm);
        dialog.setDefaultAction(confirm::activate);
        cancel.setOnAction(e -> dialog.hide());
        confirm.setOnAction(e -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                nameField.setError(true);
                nameField.setSupportingText("Enter the coach's full name to continue.");
                return;
            }
            dialog.hide();
            int nextId = 101 + demoTrainers.size();
            demoTrainers.add(nextId + "|" + name + "|Strength|0|50.00|0|0%|"
                    + name.toLowerCase(Locale.ROOT).replace(' ', '.') + "@cybercoregym.io");
            applyFilters();
            feedback.showSnackbar(name + " onboarded to the roster (demo)", "Open profile",
                    () -> onOpenProfile(nextId));
        });
        feedback.showDialog(dialog);
        nameField.getInput().requestFocus();
    }

    /** Destructive demo removal: confirms first, then removes with an Undo snackbar action. */
    public void onDelete(int trainerId) {
        String row = demoTrainers.stream()
                .filter(r -> field(r, 0).equals(String.valueOf(trainerId)))
                .findFirst().orElse(null);
        if (row == null) {
            return;
        }
        M3Button cancel = M3Button.text("Cancel");
        M3Button confirm = M3Button.text("Delete");
        M3Dialog dialog = M3Dialog.basic("Remove " + field(row, 1) + "?",
                new Label("The coach is removed from the demo roster. This cannot be undone."),
                cancel, confirm);
        dialog.setDefaultAction(confirm::activate);
        cancel.setOnAction(e -> dialog.hide());
        confirm.setOnAction(e -> {
            dialog.hide();
            int index = demoTrainers.indexOf(row);
            demoTrainers.remove(row);
            applyFilters();
            feedback.showSnackbar(field(row, 1) + " removed from the roster (demo)", "Undo",
                    () -> {
                        demoTrainers.add(Math.min(index, demoTrainers.size()), row);
                        applyFilters();
                    });
        });
        feedback.showDialog(dialog);
    }
}
