package com.gym.controller;

import com.gym.dao.CustomerDao;
import com.gym.model.Customer;
import com.gym.dao.SessionDao;
import com.gym.dao.TrainerAvailabilityDao;
import com.gym.dao.TrainerDao;
import com.gym.model.Session;
import com.gym.model.SessionStatus;
import com.gym.model.Trainer;
import com.gym.model.TrainerAvailability;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Controller for the Trainer List screen.
 *
 * <p>Data contract (DECISIONS.md 77/78, task C8): the roster, carousel, stat cards and the
 * sports-science footer run on real {@code TrainerDao} rows through one {@link Data#loadAsync}
 * snapshot, with the derived figures computed from the rule-15 assignments (CustomerDao
 * trainerId), the rule-8 slot caps (TrainerAvailabilityDao) and rule-17 CONFIRMED sessions
 * (SessionDao). Live search and the domain chips filter the visible rows without re-querying;
 * the sort menu reorders a display copy, never the source list. Onboarding inserts a real
 * coach through {@code TrainerDao.insert} (the DAO owns the phone/email/rate validation) and
 * removal confirms first, then calls {@code TrainerDao.delete}, surfacing any
 * {@code ValidationException} (e.g. references still exist) verbatim.
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

    /**
     * Content host around the workspace: {@link Data#loadAsync} swaps it for loading/error
     * states while the coach snapshot builds.
     */
    private StackPane contentHost;
    /** The workspace node, restored after loading/error states. */
    private Node workspaceView;

    /** Immutable result of one background coach snapshot. */
    private record TrainerSnapshot(long trainerId, String name, String specialization,
                                   int years, double rate, int assignedAthletes,
                                   int weeklySlotCap, int confirmedThisWeek,
                                   int slotsToday, int slotsTodayCap, String email) {
    }

    /** Rows of the current snapshot in table order (id|name|spec|exp|rate|athletes|sat|email). */
    private final List<String> rosterRows = new ArrayList<>();
    /** Trainer id of the currently rendered snapshot (used by the domain chips' copy). */
    private final Map<String, Long> rowTrainerIds = new HashMap<>();

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
        loadTrainers();

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
        statTotalCoaches = new M3StatCard("TOTAL ACTIVE COACHES", "-", "From the coach roster");
        statCapacitySaturation = new M3StatCard("STAFF CAPACITY UTILIZATION", "-", "Rule-15 athletes vs rule-8 caps");
        statTodaySlots = new M3StatCard("SLOTS BOOKED TODAY", "-", "Rule-17 CONFIRMED sessions");
        statAverageRate = new M3StatCard("AVERAGE HOURLY RATE", "-", "Across the roster");
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
        btnSortOptions = new M3SplitButton("Sort: Name", ButtonVariant.FILLED, null);
        btnAddTrainer = new M3Button("Onboard Coach", ButtonVariant.FILLED);
        btnAddTrainer.setOnAction(e -> onAdd());
        commandRow.getChildren().addAll(
                txtSearch,
                new Separator(Orientation.VERTICAL),
                btnSortOptions,
                btnAddTrainer);
        FlowPane domainChips = new FlowPane(8, 8);
        domainChips.setAlignment(Pos.CENTER_LEFT);
        chipFilterAll = new M3Chip("All", ChipVariant.ASSIST);
        chipFilterAll.setSelected(true);
        // The four domain chips are relabeled from the real roster in applySnapshot.
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
        // Content host so the real-data load can swap the workspace for loading/error states.
        workspaceView = workspace;
        contentHost = new StackPane(workspace);
        workspaceScroll.setContent(contentHost);
        root.getChildren().addAll(topAppBar, workspaceScroll);
        return root;
    }

    /** The domain-coverage radar chart, populated from the snapshot in populateCharts. */
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
                    open.setOnAction(e -> onOpenProfile(rowTrainerIds.get(row) == null ? -1 : rowTrainerIds.get(row)));
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
                .item("Sort: Name", () -> sortByField(1, true))
                .item("Sort: Experience", () -> sortByField(3, false))
                .item("Sort: Hourly Rate", () -> sortByField(4, false))
                .item("Sort: Active Athletes", () -> sortByField(5, false));
        btnSortOptions.setMenu(sortMenu);
    }

    /**
     * Sorts a display COPY of the roster by the chosen pipe field and refreshes the visible
     * rows — the source list order (the database's) is never permanently mutated.
     */
    private void sortByField(int fieldIndex, boolean ascending) {
        Comparator<String> byField = Comparator.comparing(
                r -> field(r, fieldIndex).toLowerCase(Locale.ROOT));
        if (fieldIndex == 3 || fieldIndex == 4 || fieldIndex == 5) {
            byField = Comparator.comparingDouble(r -> parseNumber(field(r, fieldIndex)));
        }
        List<String> ordered = new ArrayList<>(rosterRows);
        ordered.sort(ascending ? byField : byField.reversed());
        tableTrainers.getItems().setAll(ordered);
        applyAgendaFilter(ordered);
    }

    /** Applies the active domain chip plus the search query to the visible roster. */
    private void applyFilters() {
        applyAgendaFilter(rosterRows);
    }

    /** Filters the given source rows by the domain chip and the search query. */
    private void applyAgendaFilter(List<String> sourceRows) {
        String query = txtSearch.getText() != null
                ? txtSearch.getText().trim().toLowerCase(Locale.ROOT)
                : "";
        String domain = null;
        if (chipFilterStrength.isSelected()) {
            domain = chipFilterStrength.getText().toLowerCase(Locale.ROOT);
        } else if (chipFilterHypertrophy.isSelected()) {
            domain = chipFilterHypertrophy.getText().toLowerCase(Locale.ROOT);
        } else if (chipFilterRehab.isSelected()) {
            domain = chipFilterRehab.getText().toLowerCase(Locale.ROOT);
        } else if (chipFilterCardio.isSelected()) {
            domain = chipFilterCardio.getText().toLowerCase(Locale.ROOT);
        }
        List<String> visible = new ArrayList<>();
        for (String row : sourceRows) {
            boolean matchesDomain = domain == null
                    || field(row, 2).toLowerCase(Locale.ROOT).contains(domain);
            boolean matchesQuery = query.isEmpty() || row.toLowerCase(Locale.ROOT).contains(query);
            if (matchesDomain && matchesQuery) {
                visible.add(row);
            }
        }
        tableTrainers.getItems().setAll(visible);
        chipFilterAll.setText("All (" + sourceRows.size() + ")");
    }

    /**
     * Loads the real coach roster on a background task: the bootstrap and the Trainer/Customer/
     * Slot/Session queries plus the derived per-coach figures run off the FX thread; the stat
     * cards, carousel, roster and charts land on the FX thread. Shows
     * {@link UiKit#loadingState()} while running and {@link UiKit#errorState()} with retry on
     * failure.
     */
    private void loadTrainers() {
        Data.loadAsync(contentHost,
            () -> {
                Data.ensureReady();
                return buildSnapshot();
            },
            snapshot -> {
                contentHost.getChildren().set(0, workspaceView);
                applySnapshot(snapshot);
            },
            error -> contentHost.getChildren().setAll(UiKit.errorState(
                    error.getMessage() == null ? error.toString() : error.getMessage(),
                    this::loadTrainers)));
    }

    /**
     * Queries the coaches and computes the derived figures off the FX thread: rule-15 assigned
     * athletes per coach, rule-8 weekly slot caps, rule-17 CONFIRMED sessions this week and
     * today, and today's slot fill.
     */
    private List<TrainerSnapshot> buildSnapshot() throws Exception {
        Data.ensureReady();
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.with(java.time.DayOfWeek.MONDAY).plusDays(6);

        List<Trainer> trainers = new TrainerDao().findAll();
        List<Customer> customers = new CustomerDao().findAll();
        List<TrainerAvailability> slots = new TrainerAvailabilityDao().findAll();
        List<Session> sessions = new SessionDao().findAll();

        // Rule 15: assigned athletes per coach via the authoritative trainerId.
        Map<Long, Integer> athletesByCoach = new HashMap<>();
        for (Customer customer : customers) {
            if (customer.getTrainerId() != null) {
                athletesByCoach.merge((long) customer.getTrainerId(), 1, Integer::sum);
            }
        }
        // Rule 8: weekly slot capacity per coach (sum of maxCustomers of this week's slots).
        Map<Long, Integer> weeklyCapByCoach = new HashMap<>();
        Map<Long, Integer> todayCapByCoach = new HashMap<>();
        Map<Long, Integer> todaySlotsByCoach = new HashMap<>();
        for (TrainerAvailability slot : slots) {
            LocalDate date = slot.getDate() == null || slot.getDate().isBlank()
                    ? null : LocalDate.parse(slot.getDate());
            if (date == null || date.isBefore(today) || date.isAfter(weekEnd)
                    || slot.getStatus() == com.gym.model.AvailabilityStatus.CLOSED) {
                continue;
            }
            weeklyCapByCoach.merge((long) slot.getTrainerId(), slot.getMaxCustomers(), Integer::sum);
            if (date.equals(today)) {
                todayCapByCoach.merge((long) slot.getTrainerId(), slot.getMaxCustomers(), Integer::sum);
            }
        }
        // Rule 17: CONFIRMED sessions per slot this week / today.
        Map<Integer, Integer> confirmedBySlot = new HashMap<>();
        for (Session session : sessions) {
            if (session.getStatus() == SessionStatus.CONFIRMED
                    && session.getAvailabilityId() != null) {
                confirmedBySlot.merge(session.getAvailabilityId(), 1, Integer::sum);
            }
        }
        Map<Long, Integer> confirmedTodayByCoach = new HashMap<>();
        for (TrainerAvailability slot : slots) {
            LocalDate date = slot.getDate() == null || slot.getDate().isBlank()
                    ? null : LocalDate.parse(slot.getDate());
            if (date == null || !date.equals(today)) {
                continue;
            }
            int booked = confirmedBySlot.getOrDefault(slot.getId(), 0);
            confirmedTodayByCoach.merge((long) slot.getTrainerId(), booked, Integer::sum);
            todaySlotsByCoach.merge((long) slot.getTrainerId(), booked, Integer::sum);
        }

        List<TrainerSnapshot> snapshot = new ArrayList<>();
        for (Trainer trainer : trainers) {
            long id = trainer.getId();
            int athletes = athletesByCoach.getOrDefault(id, 0);
            int weeklyCap = weeklyCapByCoach.getOrDefault(id, 0);
            int saturation = weeklyCap == 0 ? 0 : Math.min(100, athletes * 100 / weeklyCap);
            snapshot.add(new TrainerSnapshot(id, trainer.getFullName(),
                    displayValue(trainer.getSpecialization()), trainer.getExperienceYears(),
                    trainer.getHourlyRate() == null ? 0 : trainer.getHourlyRate().doubleValue(),
                    athletes, weeklyCap, confirmedTodayByCoach.getOrDefault(id, 0),
                    todaySlotsByCoach.getOrDefault(id, 0),
                    todayCapByCoach.getOrDefault(id, 0),
                    trainer.getEmail() == null ? "" : trainer.getEmail()));
        }
        return snapshot;
    }

    private static String displayValue(String value) {
        return value == null || value.isBlank() ? "General coaching" : value;
    }

    /** Applies a finished snapshot: stats, carousel, roster rows, chips, radar and heatmap. */
    private void applySnapshot(List<TrainerSnapshot> coaches) {
        // Stat cards (all real).
        double avgRate = coaches.stream().mapToDouble(TrainerSnapshot::rate)
                .average().orElse(0);
        int totalAthletes = coaches.stream().mapToInt(TrainerSnapshot::assignedAthletes).sum();
        int totalWeeklyCap = coaches.stream().mapToInt(TrainerSnapshot::weeklySlotCap).sum();
        int confirmedToday = coaches.stream().mapToInt(TrainerSnapshot::confirmedThisWeek).sum();
        int todayCap = coaches.stream().mapToInt(TrainerSnapshot::slotsTodayCap).sum();
        double utilization = totalWeeklyCap == 0 ? 0
                : Math.min(100.0, totalAthletes * 100.0 / totalWeeklyCap);
        statTotalCoaches.setValue(String.valueOf(coaches.size()));
        statTotalCoaches.setDeltaText("From the coach roster");
        statCapacitySaturation.setValue(String.format(Locale.ROOT, "%.1f%%", utilization));
        statCapacitySaturation.setDeltaText(totalAthletes + " / " + totalWeeklyCap
                + " Athletes (rule 15 vs rule 8)");
        statTodaySlots.setValue(confirmedToday + " / " + todayCap);
        statTodaySlots.setDeltaText(todayCap == 0 ? "No slots published for today"
                : String.format(Locale.ROOT, "%.1f%% Fill Rate",
                        confirmedToday * 100.0 / todayCap));
        statAverageRate.setValue(String.format(Locale.ROOT, "$%,.2f", avgRate));
        double minRate = coaches.stream().mapToDouble(TrainerSnapshot::rate).min().orElse(0);
        double maxRate = coaches.stream().mapToDouble(TrainerSnapshot::rate).max().orElse(0);
        statAverageRate.setDeltaText(String.format(Locale.ROOT,
                "Min $%,.2f / Max $%,.2f", minRate, maxRate));

        // Carousel: one real card per coach with slots today (the on-duty coaches).
        carouselCoaches.getItems().clear();
        int featured = 0;
        for (TrainerSnapshot coach : coaches) {
            if (coach.slotsTodayCap() == 0) {
                continue;
            }
            String initials = initials(coach.name());
            String detail = displayValue(coach.specialization()) + " · "
                    + coach.slotsTodayCap() + " slots today, "
                    + coach.confirmedThisWeek() + " confirmed";
            VBox body = new VBox(6);
            body.setAlignment(Pos.CENTER_LEFT);
            body.setPadding(new Insets(16));
            Label initialsLabel = new Label(initials);
            initialsLabel.getStyleClass().add("m3-stat-value");
            Label nameLabel = new Label(coach.name());
            nameLabel.getStyleClass().add("m3-section-title");
            Label detailLabel = new Label(detail);
            detailLabel.setWrapText(true);
            body.getChildren().addAll(initialsLabel, nameLabel, detailLabel);
            M3Card card = new M3Card();
            card.setContent(body);
            card.setPrefSize(260, 188);
            carouselCoaches.getItems().add(card);
            featured++;
        }
        if (featured == 0) {
            VBox body = new VBox(6);
            body.setAlignment(Pos.CENTER_LEFT);
            body.setPadding(new Insets(16));
            body.getChildren().add(styledLabel(
                    "No coach has slots published for today.", "body-medium"));
            M3Card card = new M3Card();
            card.setContent(body);
            card.setPrefSize(260, 188);
            carouselCoaches.getItems().add(card);
        }

        // Roster rows (id|name|spec|exp|rate|athletes|saturation|email).
        rosterRows.clear();
        rowTrainerIds.clear();
        for (TrainerSnapshot coach : coaches) {
            double saturation = coach.weeklySlotCap() == 0 ? 0
                    : Math.min(100.0, coach.assignedAthletes() * 100.0 / coach.weeklySlotCap());
            String row = coach.trainerId() + "|" + coach.name() + "|" + coach.specialization()
                    + "|" + coach.years() + "|"
                    + String.format(Locale.ROOT, "%.2f", coach.rate()) + "|"
                    + coach.assignedAthletes() + "|"
                    + String.format(Locale.ROOT, "%.0f%%", saturation) + "|"
                    + coach.email();
            rosterRows.add(row);
            rowTrainerIds.put(row, coach.trainerId());
        }

        relabelDomainChips(coaches);
        populateCharts(coaches);
        applyFilters();
    }

    /** Relabels the four domain chips from the most common real specializations. */
    private void relabelDomainChips(List<TrainerSnapshot> coaches) {
        Map<String, Integer> counts = new TreeMap<>();
        for (TrainerSnapshot coach : coaches) {
            String firstWord = coach.specialization().split("[ &/]+")[0].toLowerCase(Locale.ROOT);
            counts.merge(firstWord, 1, Integer::sum);
        }
        List<String> top = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(4)
                .forEach(e -> top.add(capitalize(e.getKey())));
        M3Chip[] chips = {chipFilterStrength, chipFilterHypertrophy, chipFilterRehab, chipFilterCardio};
        for (int i = 0; i < chips.length; i++) {
            chips[i].setText(i < top.size() ? top.get(i) : "Domain " + (i + 1));
        }
    }

    private static String capitalize(String word) {
        return word == null || word.isEmpty() ? word
                : word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "--";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            sb.append(parts[i].charAt(0));
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    /** Fills the radar (real domain coverage counts) and the heatmap (real CONFIRMED sessions). */
    private void populateCharts(List<TrainerSnapshot> coaches) {
        // Domain coverage: coaches counted per real specialization keyword, normalized to 100.
        Map<String, Integer> domainCounts = new TreeMap<>();
        for (TrainerSnapshot coach : coaches) {
            for (String word : coach.specialization().split("[ &/]+")) {
                String key = capitalize(word.toLowerCase(Locale.ROOT));
                if (key.length() > 2) {
                    domainCounts.merge(key, 1, Integer::sum);
                }
            }
        }
        List<String> axes = new ArrayList<>();
        List<Double> normalized = new ArrayList<>();
        int max = domainCounts.values().stream().max(Integer::sum).orElse(0);
        for (Map.Entry<String, Integer> entry : domainCounts.entrySet()) {
            axes.add(entry.getKey());
            normalized.add(max == 0 ? 0 : entry.getValue() * 100.0 / max);
        }
        chartSpecialization.getAxes().setAll(axes);
        chartSpecialization.getSeriesList().clear();
        M3DataSeries coverage = new M3DataSeries("Coaches per domain");
        for (int i = 0; i < axes.size(); i++) {
            coverage.getData().add(new M3DataPoint(axes.get(i), normalized.get(i)));
        }
        chartSpecialization.getSeriesList().addAll(coverage);

        // Weekly booking density from real CONFIRMED sessions (rule 17), this week.
        heatmapBookingDensity.setDimensions(7, 16);
        heatmapBookingDensity.setRowLabels(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
        int[][] booked = new int[7][16];
        try {
            LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            for (Session session : new SessionDao().findAll()) {
                if (session.getStatus() != SessionStatus.CONFIRMED
                        || session.getDate() == null || session.getStartTime() == null) {
                    continue;
                }
                LocalDate date = LocalDate.parse(session.getDate());
                long offset = java.time.temporal.ChronoUnit.DAYS.between(monday, date);
                if (offset < 0 || offset > 6) {
                    continue;
                }
                int hour = Integer.parseInt(session.getStartTime().substring(0, 2)) - 6;
                if (hour >= 0 && hour < 16) {
                    booked[(int) offset][hour]++;
                }
            }
        } catch (Exception e) {
            // A failed session read leaves the heatmap at zero rather than inventing load.
        }
        int peak = 1;
        for (int day = 0; day < 7; day++) {
            for (int slot = 0; slot < 16; slot++) {
                peak = Math.max(peak, booked[day][slot]);
            }
        }
        for (int day = 0; day < 7; day++) {
            for (int slot = 0; slot < 16; slot++) {
                heatmapBookingDensity.setValue(day, slot,
                        booked[day][slot] * 1.0 / peak);
            }
        }
    }

    /** Returns the requested pipe field of a roster row, or an empty string when missing. */
    private static String field(String row, int index) {
        String[] parts = row.split("\\|", -1);
        return index < parts.length ? parts[index] : "";
    }

    /** Parses a numeric roster field (rate, years, percent) tolerantly for sorting. */
    private static double parseNumber(String raw) {
        try {
            return Double.parseDouble(raw.replace("%", "").replace("$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Row/activation entry point shared by the row click and the per-row Open button. */
    private void openProfileRow(String row) {
        onOpenProfile(rowTrainerIds.get(row) == null ? -1 : rowTrainerIds.get(row));
    }

    /**
     * Opens the chosen coach's dossier: navigates the shell to trainer-profile (firing the
     * pinned {@code navTrainers} rail destination on the way, exactly like a rail click) and
     * hands the coach's real identity over to {@link TrainerProfileController#showTrainer}.
     * Standalone previews and tests without the owning shell answer with a snackbar.
     */
    public void onOpenProfile(long trainerId) {
        TrainerSnapshot coach = null;
        for (TrainerSnapshot snapshot : lastCoaches) {
            if (snapshot.trainerId() == trainerId) {
                coach = snapshot;
            }
        }
        if (coach == null) {
            feedback.showSnackbar("Coach #" + trainerId + " is not on the roster.");
            return;
        }
        TrainerProfileController.TrainerSnapshot payload =
                new TrainerProfileController.TrainerSnapshot(
                        (int) coach.trainerId(), coach.name(), coach.specialization(),
                        coach.years(), coach.rate(), coach.assignedAthletes(),
                        coach.weeklySlotCap() == 0 ? 0
                                : Math.min(100, coach.assignedAthletes() * 100 / coach.weeklySlotCap()),
                        coach.email());
        AppShellController shell = shellFromScene();
        if (shell != null) {
            shell.openScreenViaRail("navTrainers", Screen.TRAINER_PROFILE, controller -> {
                if (controller instanceof TrainerProfileController profile) {
                    profile.showTrainer(payload);
                }
            });
            return;
        }
        feedback.showSnackbar("Opening coach profile — " + coach.name() + " (standalone preview)");
    }

    /** The coach rows of the last successful snapshot (used by the profile handoff). */
    private List<TrainerSnapshot> lastCoaches = new ArrayList<>();

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
                // Deferred so a screen swap that is mid-mutation of the pane hosting this screen
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

    /**
     * Onboards a real coach through {@code TrainerDao.insert}: the dialog collects the name
     * and a rule-valid Egyptian phone, the DAO owns the email/rate/format validation, and any
     * {@code ValidationException} surfaces verbatim.
     */
    public void onAdd() {
        M3TextField nameField = new M3TextField();
        nameField.setLabelText("Coach full name");
        M3TextField phoneField = new M3TextField();
        phoneField.setLabelText("Phone");
        phoneField.setSupportingText("11 digits starting 010, 011, 012 or 015");
        M3Button cancel = M3Button.text("Cancel");
        M3Button confirm = M3Button.filled("Onboard");
        M3Dialog dialog = M3Dialog.basic(
                "Onboard a new coach",
                new VBox(10, nameField, phoneField,
                        new Label("The coach joins the roster with a Starter capacity profile"
                                + " and can publish slots from the Availability screen.")),
                cancel, confirm);
        dialog.setDefaultAction(confirm::activate);
        cancel.setOnAction(e -> dialog.hide());
        confirm.setOnAction(e -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
            boolean nameOk = !name.isEmpty();
            boolean phoneOk = com.gym.util.PhoneUtil.isValid(phone);
            nameField.setError(!nameOk);
            phoneField.setError(!phoneOk);
            if (!nameOk || !phoneOk) {
                return;
            }
            dialog.hide();
            Trainer trainer = new Trainer(0, name, phone,
                    name.toLowerCase(Locale.ROOT).replace(' ', '.')
                            + "@cybercoregym.io",
                    "Strength & Conditioning", 0, "Onboarding", LocalDate.now().toString(),
                    new java.math.BigDecimal("50.00"));
            Data.submit(
                    () -> new TrainerDao().insert(trainer),
                    inserted -> {
                        feedback.showSnackbar(inserted.getFullName()
                                + " onboarded to the roster (id " + inserted.getId() + ").");
                        loadTrainers();
                    },
                    error -> feedback.showSnackbar("Could not onboard coach: "
                            + error.getMessage()));
        });
        feedback.showDialog(dialog);
        nameField.getInput().requestFocus();
    }

    /**
     * Rule 4 removal: confirms first, then calls {@code TrainerDao.delete} — the DAO refuses
     * the hard delete while references exist (assigned customers, sessions, slots) and the
     * {@code ValidationException} surfaces verbatim.
     */
    public void onDelete(int trainerId) {
        String row = null;
        String name = "";
        for (String candidate : rosterRows) {
            if (field(candidate, 0).equals(String.valueOf(trainerId))) {
                row = candidate;
                name = field(candidate, 1);
            }
        }
        if (row == null) {
            return;
        }
        M3Button cancel = M3Button.text("Cancel");
        M3Button confirm = M3Button.text("Delete");
        M3Dialog dialog = M3Dialog.basic("Remove " + name + "?",
                new Label("Rule 4: the hard delete is refused while the coach still has"
                        + " assigned athletes, sessions or slots."),
                cancel, confirm);
        dialog.setDefaultAction(confirm::activate);
        cancel.setOnAction(e -> dialog.hide());
        confirm.setOnAction(e -> {
            dialog.hide();
            Data.submit(
                    () -> {
                        new TrainerDao().delete(trainerId);
                        return trainerId;
                    },
                    removed -> {
                        feedback.showSnackbar("Coach #" + removed + " removed from the roster.");
                        loadTrainers();
                    },
                    error -> feedback.showSnackbar("Could not remove coach: "
                            + error.getMessage()));
        });
        feedback.showDialog(dialog);
    }
}
