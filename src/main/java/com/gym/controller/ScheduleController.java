package com.gym.controller;

import com.gym.dao.CustomerDao;
import com.gym.dao.SessionDao;
import com.gym.dao.TrainerAvailabilityDao;
import com.gym.dao.TrainerDao;
import com.gym.model.AvailabilityStatus;
import com.gym.model.Customer;
import com.gym.model.CustomerStatus;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
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
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3DonutChart;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.core.theme.ColorRole;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for Master Schedule & Class Timetable screen.
 * Conforms to Section 7 of docs/ui/workouts/SCHEDULE_CALENDAR_UI.md and docs/UML.md.
 *
 * <p>Data contract (DECISIONS.md 77/78, task C4): the week agenda, the coach rail and the
 * booking flows run on real sessions — one {@link Data#loadAsync} snapshot per view
 * (`SessionDao` + `TrainerAvailabilityDao` + customers + coaches), real week navigation over
 * all 7 day columns, and every lifecycle action through {@code SessionDao} with the rules
 * owned by the DAO or mirrored verbatim from the CLI's {@code GymRulesHelper}: rule 1 booking
 * (inside-slot CONFIRMED, outside-slot REQUESTED, admin override, default 60 min), rule 10
 * cancel (>= 2h free, late cancels still CANCELLED with the reason recorded), rule 17 capacity
 * (CONFIRMED-only occupancy), rule 18 transitions.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former schedule.fxml: week navigation bar, coach rail, agenda matrix, session inspector.
 */
public class ScheduleController implements ScreenController {

    /** Day columns of the agenda matrix — the whole week, Mon through Sun. */
    private static final int DAY_COLUMNS = 7;
    private static final int FIRST_HOUR = 7;
    private static final int LAST_HOUR = 20;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEE dd");
    private static final DateTimeFormatter RANGE_START_FMT = DateTimeFormatter.ofPattern("MMM dd");
    private static final DateTimeFormatter RANGE_END_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private GridPane agendaGrid;
    private M3TextField searchTrainerField;
    private ListView<String> trainerListView;
    private Label lblInspectorTitle;
    private Label lblSessionStatus;
    private Label lblAthleteName;
    private Label lblCoachName;
    private Label lblSessionTime;
    private Label lblPriceOrSub;
    private M3DonutChart slotDonutChart;
    private M3Timeline sessionTimeline;
    private M3Button btnConfirmAction;
    private M3Button btnCompleteAction;
    private M3Button btnNoShowAction;
    private M3Button btnCancelAction;
    private M3SplitButton actionSplitButton;
    private M3IconButton btnPrevWeek;
    private Label lblCurrentDateRange;
    private M3IconButton btnNextWeek;
    private M3Button btnToday;
    private M3Button btnShowAvailableTrainers;
    private M3Chip chipFilterConfirmed;
    private M3Chip chipFilterRequested;
    private M3Chip chipFilterCancelled;
    private VBox inspectorContainer;
    private M3IconButton btnCloseInspector;
    private M3Button btnRunLive;

    /** Start (Monday) of the week shown in the header; refreshed by the navigation buttons. */
    private LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /**
     * One agenda session assembled in the background snapshot: the database row plus the
     * display strings resolved from the customers/coaches/slots.
     */
    private record SessionRecord(int sessionId, LocalDate date, String start, int durationMin,
                                 String customerName, int trainerId, String trainerName,
                                 SessionStatus status, Integer availabilityId, Integer slotCap,
                                 int confirmedInSlot, String price, String notes) {
    }

    /**
     * The immutable result of one background week snapshot: agenda sessions, the coach roster,
     * the week's published slots and the rule-5/1 booking candidates.
     */
    private record WeekSnapshot(List<SessionRecord> sessions, List<Trainer> trainers,
                                List<TrainerAvailability> slots,
                                List<Customer> bookableCustomers) {
    }

    /** Sessions of the currently rendered week (drives the cards and the inspector). */
    private final List<SessionRecord> weekSessions = new ArrayList<>();
    /** Coach roster of the currently rendered week snapshot. */
    private final List<Trainer> trainerRoster = new ArrayList<>();
    /** The week's published slots, kept for the booking flows. */
    private final List<TrainerAvailability> weekSlots = new ArrayList<>();
    /** Rule-1 booking candidates (ACTIVE customers), refreshed with every snapshot. */
    private final List<Customer> bookableCustomers = new ArrayList<>();

    /** Demo session cards currently on the agenda grid, in placement order. */
    private final List<M3Card> agendaCards = new ArrayList<>();

    /** The session currently loaded into the inspector, or null before the first click. */
    private SessionRecord selectedSession;

    /** Whether the coach rail currently shows only this week's available coaches. */
    private boolean showingAvailableOnly;

    /** Content host around the agenda: swaps loading/error states around DAO loads. */
    private StackPane contentHost;
    /** The scrollable agenda node, restored after loading/error states. */
    private javafx.scene.Node agendaView;

    /**
     * Builds the complete Schedule &amp; Timetable view and initializes all behavior. Same
     * ordering semantics the FXMLLoader used to provide: the node tree exists and every field is
     * assigned before the former {@code initialize()} work runs.
     */
    public ScheduleController() {
        // Top app bar & time navigation.
        M3TopAppBar topAppBar = new M3TopAppBar("Schedule & Timetable",
                io.m3fx.controls.appbar.TopAppBarVariant.SMALL);
        topAppBar.setNavigationIcon(M3Icon.symbol("arrow_back"));

        HBox timeNavRow = new HBox(8);
        timeNavRow.setAlignment(Pos.CENTER_LEFT);
        timeNavRow.setPadding(new Insets(4, 16, 12, 24));
        btnPrevWeek = new M3IconButton(M3Icon.symbol("chevron_left"), IconButtonVariant.STANDARD);
        btnPrevWeek.setAccessibleText("Previous Week");
        btnPrevWeek.setOnAction(e -> onPrevWeek());
        lblCurrentDateRange = new Label("Oct 26 - Nov 01, 2026");
        lblCurrentDateRange.getStyleClass().add("m3-list-headline");
        btnNextWeek = new M3IconButton(M3Icon.symbol("chevron_right"), IconButtonVariant.STANDARD);
        btnNextWeek.setAccessibleText("Next Week");
        btnNextWeek.setOnAction(e -> onNextWeek());
        btnToday = new M3Button("Today", ButtonVariant.TONAL);
        btnToday.getStyleClass().add("m3-button-tonal");
        btnToday.setOnAction(e -> onToday());
        actionSplitButton = new M3SplitButton("Book Slot", ButtonVariant.FILLED, null);
        actionSplitButton.getStyleClass().add("m3-split-button-filled");
        actionSplitButton.setOnAction(e -> onBook());
        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);
        timeNavRow.getChildren().addAll(
                styledLabel("This week's sessions and bookings", "m3-list-supporting"),
                navSpacer,
                btnPrevWeek,
                lblCurrentDateRange,
                btnNextWeek,
                btnToday,
                new Separator(Orientation.VERTICAL),
                actionSplitButton);

        VBox topBarContainer = new VBox(8);
        topBarContainer.getStyleClass().add("top-bar-container");
        topBarContainer.getChildren().addAll(topAppBar, timeNavRow);

        // Left filter & resource rail.
        VBox filterRail = new VBox(16);
        filterRail.getStyleClass().add("filter-rail");
        filterRail.setMinWidth(210);
        filterRail.setPadding(new Insets(16));
        searchTrainerField = new M3TextField("Search Coaches or Classes",
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        searchTrainerField.setSupportingText("Filters the coach roster below");
        trainerListView = new ListView<>();
        VBox.setVgrow(trainerListView, Priority.ALWAYS);
        btnShowAvailableTrainers = new M3Button("Available Coaches Matrix", ButtonVariant.OUTLINED);
        btnShowAvailableTrainers.getStyleClass().add("m3-button-outlined");
        btnShowAvailableTrainers.setMaxWidth(Double.MAX_VALUE);
        btnShowAvailableTrainers.setOnAction(e -> showAvailableTrainers());
        filterRail.getChildren().addAll(
                searchTrainerField,
                styledLabel("COACH ROSTER", "m3-list-overline"),
                trainerListView,
                btnShowAvailableTrainers);

        // Center calendar matrix.
        VBox calendarPane = new VBox(8);
        calendarPane.getStyleClass().add("calendar-matrix-pane");
        calendarPane.setPadding(new Insets(12));
        FlowPane statusFilterStrip = new FlowPane(8, 4);
        chipFilterConfirmed = new M3Chip("Confirmed", ChipVariant.ASSIST);
        chipFilterConfirmed.setSelected(true);
        chipFilterRequested = new M3Chip("Requested", ChipVariant.ASSIST);
        chipFilterRequested.setSelected(true);
        chipFilterCancelled = new M3Chip("Cancelled", ChipVariant.ASSIST);
        chipFilterCancelled.setSelected(false);
        statusFilterStrip.getChildren().addAll(chipFilterConfirmed, chipFilterRequested, chipFilterCancelled);
        agendaGrid = new GridPane();
        agendaGrid.setHgap(8);
        agendaGrid.setVgap(8);
        agendaGrid.getStyleClass().add("agenda-grid-container");
        ScrollPane agendaScroll = new ScrollPane(agendaGrid);
        agendaScroll.setFitToWidth(true);
        agendaScroll.setFitToHeight(false);
        agendaView = agendaScroll;
        // Content host so the real-data week load can swap the agenda for loading/error states.
        contentHost = new StackPane(agendaScroll);
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        calendarPane.getChildren().addAll(statusFilterStrip, contentHost);

        // Right contextual inspector side sheet.
        inspectorContainer = new VBox(16);
        inspectorContainer.getStyleClass().add("sidesheet-surface");
        inspectorContainer.setMinWidth(280);
        inspectorContainer.setMaxWidth(380);
        inspectorContainer.setPadding(new Insets(20));

        HBox inspectorHeader = new HBox();
        inspectorHeader.setAlignment(Pos.CENTER_LEFT);
        lblInspectorTitle = new Label("Session Inspector");
        lblInspectorTitle.getStyleClass().add("m3-list-headline");
        btnCloseInspector = new M3IconButton(M3Icon.symbol("close"), IconButtonVariant.STANDARD);
        btnCloseInspector.setAccessibleText("Close Inspector");
        btnCloseInspector.setOnAction(e -> onCloseInspector());
        Region inspectorSpacer = new Region();
        HBox.setHgrow(inspectorSpacer, Priority.ALWAYS);
        inspectorHeader.getChildren().addAll(lblInspectorTitle, inspectorSpacer, btnCloseInspector);

        M3Card summaryCard = new M3Card(CardVariant.OUTLINED);
        summaryCard.getStyleClass().add("inspector-summary-card");
        VBox summaryContent = new VBox(10);
        summaryContent.setPadding(new Insets(14));
        lblSessionStatus = new Label("CONFIRMED");
        lblSessionStatus.getStyleClass().add("badge-confirmed");
        lblAthleteName = new Label("Athlete: John Doe");
        lblAthleteName.getStyleClass().add("m3-list-headline");
        lblCoachName = new Label("Coach: Marcus Vance");
        lblCoachName.getStyleClass().add("m3-list-supporting");
        lblSessionTime = new Label("Time: 08:00 - 09:00 AM");
        lblSessionTime.getStyleClass().add("m3-list-supporting");
        lblPriceOrSub = new Label("Price: Covered by Subscription");
        lblPriceOrSub.getStyleClass().add("m3-list-supporting");
        slotDonutChart = new M3DonutChart();
        slotDonutChart.setPrefSize(120, 120);
        summaryContent.getChildren().addAll(lblSessionStatus, lblAthleteName, lblCoachName,
                lblSessionTime, lblPriceOrSub, slotDonutChart);
        summaryCard.setContent(summaryContent);

        sessionTimeline = new M3Timeline();
        VBox.setVgrow(sessionTimeline, Priority.ALWAYS);

        VBox lifecycleButtons = new VBox(8);
        btnConfirmAction = new M3Button("Confirm Session", ButtonVariant.FILLED);
        btnConfirmAction.getStyleClass().add("m3-button-filled");
        btnConfirmAction.setMaxWidth(Double.MAX_VALUE);
        btnConfirmAction.setOnAction(e -> onConfirmClick());
        btnCompleteAction = new M3Button("Complete Session", ButtonVariant.TONAL);
        btnCompleteAction.getStyleClass().add("m3-button-tonal");
        btnCompleteAction.setMaxWidth(Double.MAX_VALUE);
        btnCompleteAction.setOnAction(e -> onCompleteClick());
        btnNoShowAction = new M3Button("Mark No-Show", ButtonVariant.OUTLINED);
        btnNoShowAction.getStyleClass().add("m3-button-outlined");
        btnNoShowAction.setMaxWidth(Double.MAX_VALUE);
        btnNoShowAction.setOnAction(e -> onNoShowClick());
        btnCancelAction = new M3Button("Cancel Session", ButtonVariant.TEXT);
        btnCancelAction.getStyleClass().add("m3-button-text-danger");
        btnCancelAction.setMaxWidth(Double.MAX_VALUE);
        btnCancelAction.setOnAction(e -> onCancel());
        btnRunLive = new M3Button("Run Live", ButtonVariant.FILLED);
        btnRunLive.getStyleClass().add("m3-button-filled");
        btnRunLive.setMaxWidth(Double.MAX_VALUE);
        btnRunLive.setOnAction(e -> onRunLive());
        lifecycleButtons.getChildren().addAll(btnConfirmAction, btnCompleteAction,
                btnNoShowAction, btnCancelAction, btnRunLive);

        inspectorContainer.getChildren().addAll(
                inspectorHeader,
                summaryCard,
                styledLabel("LIFECYCLE AUDIT TRAIL", "m3-list-overline"),
                sessionTimeline,
                lifecycleButtons);

        SplitPane workspaceSplit = new SplitPane(filterRail, calendarPane, inspectorContainer);
        workspaceSplit.setDividerPositions(0.25, 0.72);

        BorderPane layout = new BorderPane();
        layout.setTop(topBarContainer);
        layout.setCenter(workspaceSplit);

        viewRoot = new StackPane();
        viewRoot.getStyleClass().add("schedule-root");
        viewRoot.getChildren().add(layout);


    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        agendaGrid.setId("agendaGrid");
        searchTrainerField.setId("searchTrainerField");
        trainerListView.setId("trainerListView");
        lblInspectorTitle.setId("lblInspectorTitle");
        lblSessionStatus.setId("lblSessionStatus");
        lblAthleteName.setId("lblAthleteName");
        lblCoachName.setId("lblCoachName");
        lblSessionTime.setId("lblSessionTime");
        lblPriceOrSub.setId("lblPriceOrSub");
        slotDonutChart.setId("slotDonutChart");
        sessionTimeline.setId("sessionTimeline");
        btnConfirmAction.setId("btnConfirmAction");
        btnCompleteAction.setId("btnCompleteAction");
        btnNoShowAction.setId("btnNoShowAction");
        btnCancelAction.setId("btnCancelAction");
        actionSplitButton.setId("actionSplitButton");
        btnPrevWeek.setId("btnPrevWeek");
        lblCurrentDateRange.setId("lblCurrentDateRange");
        btnNextWeek.setId("btnNextWeek");
        btnToday.setId("btnToday");
        btnShowAvailableTrainers.setId("btnShowAvailableTrainers");
        chipFilterConfirmed.setId("chipFilterConfirmed");
        chipFilterRequested.setId("chipFilterRequested");
        chipFilterCancelled.setId("chipFilterCancelled");
        inspectorContainer.setId("inspectorContainer");
        btnCloseInspector.setId("btnCloseInspector");
        installFeedbackLayer(agendaGrid);
        updateDateRangeLabel();
        wireStatusFilters();
        // The inspector is contextual: it starts collapsed so the agenda (this screen's primary
        // answer) gets the full width, and opens over the session the user clicks.
        collapseInspector();
        loadSchedule();
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

    /** Wires the coach rail search filter; the rail itself fills from the week snapshot. */
    private void wireCoachRailSearch() {
        trainerListView.setPlaceholder(new Label("No coaches match your search."));
        searchTrainerField.getInput().textProperty().addListener((obs, o, query) ->
                applyCoachRail());
    }

    /** Fills the coach rail from the real roster, honoring the search query and the toggle. */
    private void applyCoachRail() {
        String query = searchTrainerField.getInput().getText() == null
                ? "" : searchTrainerField.getInput().getText().trim().toLowerCase(Locale.ROOT);
        List<String> visible = new ArrayList<>();
        for (Trainer trainer : trainerRoster) {
            if (showingAvailableOnly && openSlotCount(trainer.getId()) == 0) {
                continue;
            }
            String summary = trainer.getFullName() + " — " + displayValue(trainer.getSpecialization())
                    + " · " + openSlotCount(trainer.getId()) + " open slots this week";
            if (query.isEmpty()
                    || summary.toLowerCase(Locale.ROOT).contains(query)) {
                visible.add(summary);
            }
        }
        trainerListView.getItems().setAll(visible);
    }

    /** Open (not CLOSED, under-cap) slot count for one coach within the rendered week. */
    private int openSlotCount(int trainerId) {
        int open = 0;
        for (TrainerAvailability slot : weekSlots) {
            if (slot.getTrainerId() != trainerId
                    || slot.getStatus() == AvailabilityStatus.CLOSED
                    || !inRenderedWeek(LocalDate.parse(slot.getDate()))) {
                continue;
            }
            int confirmed = 0;
            for (SessionRecord session : weekSessions) {
                if (session.availabilityId() != null
                        && session.availabilityId() == slot.getId()
                        && session.status() == SessionStatus.CONFIRMED) {
                    confirmed++;
                }
            }
            if (slot.getMaxCustomers() > confirmed) {
                open++;
            }
        }
        return open;
    }

    private static String displayValue(String value) {
        return value == null || value.isBlank() ? "General coaching" : value;
    }

    private boolean inRenderedWeek(LocalDate date) {
        return !date.isBefore(weekStart) && !date.isAfter(weekStart.plusDays(6));
    }

    /** Wires the three status chips so the agenda reacts to filtering immediately. */
    private void wireStatusFilters() {
        chipFilterConfirmed.setOnAction(e -> applyAgendaFilter());
        chipFilterRequested.setOnAction(e -> applyAgendaFilter());
        chipFilterCancelled.setOnAction(e -> applyAgendaFilter());
    }

    /** Shows only the agenda cards whose status chip is selected; an empty result is reachable. */
    private void applyAgendaFilter() {
        for (M3Card card : agendaCards) {
            Object status = card.getProperties().get("sessionStatus");
            boolean show = switch (String.valueOf(status)) {
                case "CONFIRMED" -> chipFilterConfirmed.isSelected();
                case "REQUESTED" -> chipFilterRequested.isSelected();
                case "CANCELLED" -> chipFilterCancelled.isSelected();
                default -> true;
            };
            card.setVisible(show);
            card.setManaged(show);
        }
    }

    private void updateDateRangeLabel() {
        lblCurrentDateRange.setText(RANGE_START_FMT.format(weekStart)
                + " - " + RANGE_END_FMT.format(weekStart.plusDays(6)));
    }

    public void onPrevWeek() {
        weekStart = weekStart.minusWeeks(1);
        updateDateRangeLabel();
        loadSchedule();
    }

    public void onNextWeek() {
        weekStart = weekStart.plusWeeks(1);
        updateDateRangeLabel();
        loadSchedule();
    }

    public void onToday() {
        weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        updateDateRangeLabel();
        loadSchedule();
    }

    /** Hides the inspector and hands its width back to the agenda (2-pane split). */
    private void collapseInspector() {
        inspectorContainer.setVisible(false);
        inspectorContainer.setManaged(false);
        if (inspectorContainer.getParent() instanceof SplitPane split) {
            split.setDividerPositions(0.28);
        }
    }

    /** Reveals the inspector and rebalances the split to rail / agenda / inspector. */
    private void expandInspector() {
        inspectorContainer.setVisible(true);
        inspectorContainer.setManaged(true);
        if (inspectorContainer.getParent() instanceof SplitPane split) {
            split.setDividerPositions(0.25, 0.72);
        }
    }

    public void onCloseInspector() {
        collapseInspector();
    }

    /**
     * Loads the rendered week from the database on a background task: the bootstrap, the
     * session/slot/customer/coach queries and the snapshot computation run off the FX thread;
     * the agenda, coach rail and header land on the FX thread. Shows
     * {@link UiKit#loadingState()} while running and {@link UiKit#errorState()} with retry on
     * failure.
     */
    private void loadSchedule() {
        Data.loadAsync(contentHost,
            () -> {
                Data.ensureReady();
                return buildWeekSnapshot(weekStart);
            },
            snapshot -> {
                contentHost.getChildren().set(0, agendaView);
                applyWeek(snapshot);
            },
            error -> contentHost.getChildren().setAll(UiKit.errorState(
                    error.getMessage() == null ? error.toString() : error.getMessage(),
                    this::loadSchedule)));
    }

    /**
     * Queries the sessions of the rendered week (plus slots, customers and coaches for the
     * display strings and the booking flows). Runs on the background thread — no JavaFX types
     * touched here.
     */
    private WeekSnapshot buildWeekSnapshot(LocalDate monday) throws Exception {
        Data.ensureReady();
        LocalDate weekEnd = monday.plusDays(6);

        List<Session> allSessions = new SessionDao().findAll();
        List<TrainerAvailability> slots = new TrainerAvailabilityDao().findAll();
        List<Customer> customers = new CustomerDao().findAll();
        List<Trainer> trainers = new TrainerDao().findAll();

        Map<Integer, String> customerNames = new HashMap<>();
        for (Customer customer : customers) {
            customerNames.put(customer.getId(), customer.getFullName());
        }
        Map<Integer, String> trainerNames = new HashMap<>();
        for (Trainer trainer : trainers) {
            trainerNames.put(trainer.getId(), trainer.getFullName());
        }
        Map<Integer, Integer> slotCaps = new HashMap<>();
        for (TrainerAvailability slot : slots) {
            slotCaps.put(slot.getId(), slot.getMaxCustomers());
        }
        // Rule 17: occupancy counts CONFIRMED sessions only, per slot.
        Map<Integer, Integer> confirmedBySlot = new HashMap<>();
        for (Session session : allSessions) {
            if (session.getStatus() == SessionStatus.CONFIRMED
                    && session.getAvailabilityId() != null) {
                confirmedBySlot.merge(session.getAvailabilityId(), 1, Integer::sum);
            }
        }

        List<SessionRecord> records = new ArrayList<>();
        for (Session session : allSessions) {
            if (session.getDate() == null
                    || !inRange(LocalDate.parse(session.getDate()), monday, weekEnd)) {
                continue;
            }
            Integer slotCap = session.getAvailabilityId() == null
                    ? null : slotCaps.get(session.getAvailabilityId());
            records.add(new SessionRecord(session.getId(),
                    LocalDate.parse(session.getDate()), session.getStartTime(),
                    session.getDurationMin(),
                    customerNames.getOrDefault(session.getCustomerId(),
                            "Member #" + session.getCustomerId()),
                    session.getTrainerId(),
                    trainerNames.getOrDefault(session.getTrainerId(),
                            "Coach #" + session.getTrainerId()),
                    session.getStatus() == null ? SessionStatus.REQUESTED : session.getStatus(),
                    session.getAvailabilityId(), slotCap,
                    session.getAvailabilityId() == null
                            ? 0 : confirmedBySlot.getOrDefault(session.getAvailabilityId(), 0),
                    session.getPrice() == null ? "" : session.getPrice().toPlainString(),
                    session.getSessionNotes()));
        }
        records.sort((a, b) -> a.start().compareTo(b.start()));

        // Rule-1 booking candidates: ACTIVE customers (FK + workflow validation is the DAO's).
        List<Customer> bookable = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.getStatus() == CustomerStatus.ACTIVE) {
                bookable.add(customer);
            }
        }
        return new WeekSnapshot(records, trainers, slots, bookable);
    }

    private static boolean inRange(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /** Applies a finished week snapshot: agenda matrix, coach rail, header, inspector reset. */
    private void applyWeek(WeekSnapshot snapshot) {
        weekSessions.clear();
        weekSessions.addAll(snapshot.sessions());
        trainerRoster.clear();
        trainerRoster.addAll(snapshot.trainers());
        weekSlots.clear();
        weekSlots.addAll(snapshot.slots());
        bookableCustomers.clear();
        bookableCustomers.addAll(snapshot.bookableCustomers());

        renderWeek();
        applyCoachRail();
        collapseInspector();
        selectedSession = null;
        feedback.showSnackbar(weekSessions.size() + " sessions this week ("
                + lblCurrentDateRange.getText() + ").");
    }

    /**
     * Renders the agenda matrix with hour rows, all 7 day headers and one card per real
     * session, placed at its day column and start-hour row. Cards are tagged with their status
     * (for the chips), clickable (loads the inspector) and keyboard operable.
     */
    private void renderWeek() {
        agendaGrid.getChildren().clear();
        agendaCards.clear();
        ColumnConstraints timeCol = new ColumnConstraints(64);
        ColumnConstraints dayCol = new ColumnConstraints();
        dayCol.setHgrow(Priority.ALWAYS);
        List<ColumnConstraints> columns = new ArrayList<>();
        columns.add(timeCol);
        for (int d = 0; d < DAY_COLUMNS; d++) {
            columns.add(dayCol);
        }
        agendaGrid.getColumnConstraints().setAll(columns);
        // Every row needs its own constraint: one shared instance only sizes row 0, which used
        // to collapse the other hour rows to bare text height and crush the matrix.
        RowConstraints headerRow = new RowConstraints(32);
        RowConstraints hourRow = new RowConstraints(56);
        List<RowConstraints> rows = new ArrayList<>();
        rows.add(headerRow);
        for (int h = FIRST_HOUR; h <= LAST_HOUR; h++) {
            rows.add(hourRow);
        }
        agendaGrid.getRowConstraints().setAll(rows);

        for (int d = 0; d < DAY_COLUMNS; d++) {
            Label header = new Label(DAY_FMT.format(weekStart.plusDays(d)));
            header.getStyleClass().add("m3-list-headline");
            GridPane.setHalignment(header, HPos.CENTER);
            agendaGrid.add(header, d + 1, 0);
        }

        for (int h = FIRST_HOUR; h <= LAST_HOUR; h++) {
            Label time = new Label(String.format("%02d:00", h));
            time.getStyleClass().add("m3-list-overline");
            GridPane.setValignment(time, VPos.TOP);
            agendaGrid.add(time, 0, h - FIRST_HOUR + 1);
        }
        for (SessionRecord session : weekSessions) {
            int hour;
            try {
                hour = Integer.parseInt(session.start().substring(0, 2));
            } catch (RuntimeException e) {
                continue;
            }
            if (hour < FIRST_HOUR || hour > LAST_HOUR) {
                continue;
            }
            M3Card card = createSessionCard(session);
            agendaCards.add(card);
            agendaGrid.add(card, session.date().getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue() + 1,
                    hour - FIRST_HOUR + 1);
        }
        applyAgendaFilter();
    }

    /** Builds one agenda card: tagged, clickable, keyboard operable. */
    private M3Card createSessionCard(SessionRecord session) {
        Label title = new Label(session.customerName());
        title.getStyleClass().add("m3-list-supporting");
        Label caption = new Label(session.start() + " · " + statusLabel(session.status()));
        caption.getStyleClass().add("m3-list-overline");
        VBox box = new VBox(2, title, caption);
        box.setPadding(new Insets(6));
        M3Card card = new M3Card(CardVariant.OUTLINED);
        card.setContent(box);
        card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        card.getProperties().put("sessionStatus", session.status().name());
        card.getProperties().put("sessionId", session.sessionId());
        card.setOnMouseClicked(e -> openInspector(session));
        card.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER, SPACE -> {
                    openInspector(session);
                    e.consume();
                }
                default -> { }
            }
        });
        card.setFocusTraversable(true);
        return card;
    }

    /** User-facing agenda copy for a session status (sentence case, no enum shout). */
    private static String statusLabel(SessionStatus status) {
        return switch (status) {
            case REQUESTED -> "Requested";
            case CONFIRMED -> "Confirmed";
            case COMPLETED -> "Completed";
            case NO_SHOW -> "No-show";
            case CANCELLED -> "Cancelled";
        };
    }

    /** Loads a real session into the inspector with rule-18-aware lifecycle buttons. */
    private void openInspector(SessionRecord session) {
        selectedSession = session;
        lblInspectorTitle.setText("Session Inspector");
        lblSessionStatus.setText(statusLabel(session.status()));
        lblAthleteName.setText("Athlete: " + session.customerName());
        lblCoachName.setText("Coach: " + session.trainerName());
        LocalTime start = LocalTime.parse(session.start());
        lblSessionTime.setText("Time: " + session.start() + " - "
                + start.plusMinutes(session.durationMin()).toString() + " ("
                + session.durationMin() + " min)");
        lblPriceOrSub.setText(session.notes() == null || session.notes().isBlank()
                ? "Notes: none recorded" : "Notes: " + session.notes());
        int slotCap = session.slotCap() == null ? 0 : session.slotCap();
        slotDonutChart.getRings().clear();
        if (session.availabilityId() != null && slotCap > 0) {
            slotDonutChart.getRings().addAll(
                    new M3DonutChart.Ring("Booked", session.confirmedInSlot(), slotCap, null),
                    new M3DonutChart.Ring("Free", slotCap - session.confirmedInSlot(), slotCap,
                            UiKit.themeRole(ColorRole.SECONDARY)));
            slotDonutChart.setCenterHeadline(session.confirmedInSlot() + "/" + slotCap);
        } else {
            slotDonutChart.setCenterHeadline("—");
        }
        sessionTimeline.getItems().clear();
        sessionTimeline.addItem(session.date().toString(), "Booked",
                "Session on the schedule for " + session.start(), M3Timeline.Status.COMPLETED);
        sessionTimeline.addItem("—", "Current status", statusLabel(session.status()),
                session.status() == SessionStatus.COMPLETED
                        ? M3Timeline.Status.COMPLETED : M3Timeline.Status.ACTIVE);
        // Rule 18 drives the lifecycle pair: only REQUESTED confirms; only CONFIRMED
        // completes, no-shows, cancels or runs live in the HUD.
        btnConfirmAction.setDisable(session.status() != SessionStatus.REQUESTED);
        btnCompleteAction.setDisable(session.status() != SessionStatus.CONFIRMED);
        btnNoShowAction.setDisable(session.status() != SessionStatus.CONFIRMED);
        btnCancelAction.setDisable(session.status() != SessionStatus.CONFIRMED);
        btnRunLive.setDisable(session.status() != SessionStatus.CONFIRMED);
        expandInspector();
    }

    /**
     * Applies a rule-18 lifecycle transition through {@code SessionDao.update} (the DAO owns
     * the transition check) and re-queries the week so the agenda, capacity and inspector all
     * reflect the database.
     */
    private void applyLifecycle(SessionStatus target, String successMessage) {
        if (selectedSession == null) {
            feedback.showSnackbar("Select a session on the agenda first.");
            return;
        }
        int sessionId = selectedSession.sessionId();
        Data.submit(
                () -> {
                    Session session = new SessionDao().findById(sessionId);
                    if (session == null) {
                        throw new com.gym.exception.ValidationException(
                                "Session no longer exists in the database");
                    }
                    // Rule 17 (mirrored from the CLI): confirming into a full slot is refused.
                    if (target == SessionStatus.CONFIRMED && session.getAvailabilityId() != null) {
                        TrainerAvailability slot =
                                new TrainerAvailabilityDao().findById(session.getAvailabilityId());
                        int confirmed = 0;
                        for (Session other : new SessionDao().findAll()) {
                            if (other.getStatus() == SessionStatus.CONFIRMED
                                    && session.getAvailabilityId().equals(other.getAvailabilityId())) {
                                confirmed++;
                            }
                        }
                        if (slot != null && confirmed >= slot.getMaxCustomers()) {
                            throw new com.gym.exception.ValidationException(
                                    "Rule 17: Cannot confirm session - linked slot is FULL");
                        }
                    }
                    session.setStatus(target);
                    new SessionDao().update(session);
                    return session;
                },
                updated -> {
                    feedback.showSnackbar(successMessage + " — session #"
                            + updated.getId() + " is now " + statusLabel(updated.getStatus()) + ".");
                    loadSchedule();
                },
                error -> feedback.showSnackbar("Could not update session: " + error.getMessage()));
    }

    /**
     * Rule-1 booking menu: book inside a published OPEN slot (CONFIRMED, rule 17 cap-checked),
     * request a time outside the slots (REQUESTED), or the admin override (CONFIRMED, ignores
     * slot rules). Default duration is 60 minutes.
     */
    public void onBook() {
        M3Menu menu = new M3Menu();
        menu.item("Book inside a published slot", () ->
                openSlotPickerMenu(SessionStatus.CONFIRMED, true));
        menu.item("Request a time outside the slots", () ->
                openTrainerPickerMenu(SessionStatus.REQUESTED, false));
        menu.item("Reserve (admin override)", () ->
                openTrainerPickerMenu(SessionStatus.CONFIRMED, false));
        showMenuBelow(actionSplitButton, menu,
                "Booking opens inside the app window (use File > run, or the app shell).");
    }

    /**
     * One menu entry per published OPEN slot with free capacity (rule 17). Picking a slot
     * continues to the customer picker; the session lands CONFIRMED with the slot attached.
     */
    private void openSlotPickerMenu(SessionStatus target, boolean useSlot) {
        M3Menu menu = new M3Menu();
        boolean any = false;
        for (TrainerAvailability slot : weekSlots) {
            if (slot.getStatus() == AvailabilityStatus.CLOSED
                    || !inRenderedWeek(LocalDate.parse(slot.getDate()))) {
                continue;
            }
            int confirmed = 0;
            for (SessionRecord session : weekSessions) {
                if (session.availabilityId() != null
                        && session.availabilityId() == slot.getId()
                        && session.status() == SessionStatus.CONFIRMED) {
                    confirmed++;
                }
            }
            if (confirmed >= slot.getMaxCustomers()) {
                continue;
            }
            any = true;
            String trainerName = trainerName(slot.getTrainerId());
            menu.item(DAY_FMT.format(LocalDate.parse(slot.getDate())) + " " + slot.getStartTime()
                            + " · " + trainerName + " · " + confirmed + "/"
                            + slot.getMaxCustomers() + " booked",
                    () -> openCustomerMenu(slot.getTrainerId(), target, slot.getId(),
                            slot.getDate(), slot.getStartTime()));
        }
        if (!any) {
            menu.item("No open slots with free capacity this week", () -> { });
        }
        showMenuBelow(actionSplitButton, menu,
                "Booking opens inside the app window.");
    }

    /** One menu entry per coach; continues to the customer picker. */
    private void openTrainerPickerMenu(SessionStatus target, boolean useSlot) {
        M3Menu menu = new M3Menu();
        if (trainerRoster.isEmpty()) {
            menu.item("No coaches in the database yet", () -> { });
        }
        for (Trainer trainer : trainerRoster) {
            menu.item(trainer.getFullName() + " — " + displayValue(trainer.getSpecialization()),
                    () -> openCustomerMenu(trainer.getId(), target, null,
                            weekStart.plusDays(1).toString(), "10:00"));
        }
        showMenuBelow(actionSplitButton, menu, "Booking opens inside the app window.");
    }

    /** One menu entry per ACTIVE customer; continues to the booking dialog. */
    private void openCustomerMenu(int trainerId, SessionStatus target, Integer availabilityId,
                                  String date, String startTime) {
        M3Menu menu = new M3Menu();
        if (bookableCustomers.isEmpty()) {
            menu.item("No ACTIVE customers to book", () -> { });
        }
        for (Customer customer : bookableCustomers) {
            menu.item(customer.getFullName() + " · " + customer.getPhone(),
                    () -> openBookingDialog(customer.getId(), trainerId, target,
                            availabilityId, date, startTime));
        }
        showMenuBelow(actionSplitButton, menu, "Booking opens inside the app window.");
    }

    /**
     * The booking dialog: customer, coach and slot are already picked; the date/time and the
     * notes are editable. Submitting inserts the session through {@code SessionDao} with the
     * rule-1 status (CONFIRMED inside a slot or admin override, REQUESTED outside slots) and
     * the default 60-minute duration.
     */
    private void openBookingDialog(int customerId, int trainerId, SessionStatus target,
                                   Integer availabilityId, String date, String startTime) {
        M3TextField dateField = new M3TextField("Session date", io.m3fx.controls.textfield.TextFieldVariant.OUTLINED);
        dateField.setSupportingText("YYYY-MM-DD");
        dateField.setText(date);
        M3TextField timeField = new M3TextField("Start time", io.m3fx.controls.textfield.TextFieldVariant.OUTLINED);
        timeField.setSupportingText("HH:mm (24h)");
        timeField.setText(startTime);
        M3TextField notesField = new M3TextField("Notes", io.m3fx.controls.textfield.TextFieldVariant.OUTLINED);
        notesField.setSupportingText("Optional — what this session covers");

        M3Button cancel = M3Button.text("Cancel");
        M3Button book = M3Button.text(target == SessionStatus.REQUESTED
                ? "Request Session" : "Book Session");
        M3Dialog dialog = M3Dialog.basic(target == SessionStatus.REQUESTED
                        ? "Request a session outside the slots"
                        : "Book the session",
                new VBox(10, dateField, timeField, notesField), cancel, book);
        dialog.setDefaultAction(book::activate);
        book.setOnAction(e -> {
            String bookingDate = dateField.getText() == null ? "" : dateField.getText().trim();
            String bookingTime = timeField.getText() == null ? "" : timeField.getText().trim();
            boolean dateOk = bookingDate.matches("\\d{4}-\\d{2}-\\d{2}");
            boolean timeOk = bookingTime.matches("\\d{2}:\\d{2}");
            dateField.setError(!dateOk);
            timeField.setError(!timeOk);
            if (!dateOk || !timeOk) {
                return;
            }
            dialog.hide();
            String notes = notesField.getText() == null ? "" : notesField.getText().trim();
            Data.submit(
                    () -> {
                        Session session = new Session(0, customerId, trainerId, bookingDate,
                                bookingTime, 60, target, availabilityId, null,
                                java.math.BigDecimal.ZERO, null,
                                notes.isBlank() ? null : notes);
                        return new SessionDao().insert(session);
                    },
                    inserted -> {
                        feedback.showSnackbar((target == SessionStatus.REQUESTED
                                ? "Session requested" : "Session booked") + " — #"
                                + inserted.getId() + " on " + inserted.getDate()
                                + " at " + inserted.getStartTime() + ".");
                        loadSchedule();
                    },
                    error -> feedback.showSnackbar("Could not book session: " + error.getMessage()));
        });
        cancel.setOnAction(e -> dialog.hide());
        feedback.showDialog(dialog);
    }

    private String trainerName(int trainerId) {
        for (Trainer trainer : trainerRoster) {
            if (trainer.getId() == trainerId) {
                return trainer.getFullName();
            }
        }
        return "Coach #" + trainerId;
    }

    public void onConfirmClick() {
        applyLifecycle(SessionStatus.CONFIRMED, "Session confirmed");
    }

    public void onConfirm(int sessionId) {
        onConfirmClick();
    }

    public void onCompleteClick() {
        applyLifecycle(SessionStatus.COMPLETED, "Session completed");
    }

    public void onComplete(int sessionId) {
        onCompleteClick();
    }

    public void onNoShowClick() {
        applyLifecycle(SessionStatus.NO_SHOW, "Session marked as no-show");
    }

    public void onNoShow(int sessionId) {
        onNoShowClick();
    }

    public void onReserve() {
        openTrainerPickerMenu(SessionStatus.CONFIRMED, false);
    }

    /**
     * Rule-10 cancel: destructive, so it confirms first and records the reason. The notice
     * verdict mirrors the CLI — >= 2h before start is a free cancellation, later cancels are
     * still marked CANCELLED with the reason recorded.
     */
    public void onCancel() {
        SessionRecord session = selectedSession;
        if (session == null) {
            feedback.showSnackbar("Select a session on the agenda first.");
            return;
        }
        M3TextField reasonField = new M3TextField("Cancel reason", io.m3fx.controls.textfield.TextFieldVariant.OUTLINED);
        reasonField.setSupportingText("Required (rule 10) — recorded on the session");
        M3Button keep = M3Button.text("Keep session");
        M3Button confirm = M3Button.text("Cancel session");
        M3Dialog dialog = M3Dialog.basic("Cancel this session?",
                new VBox(10,
                        new Label(session.customerName() + " at " + session.start() + " on "
                                + session.date() + " will be marked CANCELLED."),
                        reasonField),
                keep, confirm);
        dialog.setDefaultAction(confirm::activate);
        keep.setOnAction(e -> dialog.hide());
        confirm.setOnAction(e -> {
            String reason = reasonField.getText() == null ? "" : reasonField.getText().trim();
            if (reason.isEmpty()) {
                reasonField.setError(true);
                return;
            }
            dialog.hide();
            int sessionId = session.sessionId();
            Data.submit(
                    () -> {
                        Session current = new SessionDao().findById(sessionId);
                        if (current == null) {
                            throw new com.gym.exception.ValidationException(
                                    "Session no longer exists in the database");
                        }
                        long minutesUntilStart = java.time.Duration.between(
                                LocalDateTime.now(),
                                LocalDateTime.of(LocalDate.parse(current.getDate()),
                                        LocalTime.parse(current.getStartTime()))).toMinutes();
                        current.setStatus(SessionStatus.CANCELLED);
                        current.setCancelReason(reason);
                        new SessionDao().update(current);
                        return minutesUntilStart >= 120
                                ? "Rule 10: Free cancellation (>= 2h notice, " + minutesUntilStart
                                        + " min remaining)."
                                : "Rule 10: Late cancellation (< 2h before start). Marked CANCELLED"
                                        + " with the reason recorded.";
                    },
                    verdict -> {
                        feedback.showSnackbar("Session cancelled. " + verdict);
                        loadSchedule();
                    },
                    error -> feedback.showSnackbar("Could not cancel session: " + error.getMessage()));
        });
        feedback.showDialog(dialog);
    }

    public void onSubscribe() {
        feedback.showSnackbar("Subscription flow starts from the athlete's profile (demo)");
    }

    public void onRenewSubscription() {
        feedback.showSnackbar("Renewal flow starts from the athlete's profile (demo)");
    }

    public void onCancelSubscription() {
        feedback.showSnackbar("Cancelling a subscription requires confirmation (demo)");
    }

    /**
     * Toggles the coach rail between the full roster and only the coaches with open slots this
     * week (real open-slot counts), answering with a snackbar either way.
     */
    public void showAvailableTrainers() {
        showingAvailableOnly = !showingAvailableOnly;
        applyCoachRail();
        btnShowAvailableTrainers.setText(showingAvailableOnly
                ? "Show All Coaches"
                : "Available Coaches Matrix");
        feedback.showSnackbar(showingAvailableOnly
                ? "Showing coaches with open slots this week."
                : "Showing the full coach roster.");
    }

    public void showTrainerSlots(int trainerId) {
        feedback.showSnackbar("Coach #" + trainerId + " slots are visible on the agenda for the "
                + "rendered week.");
    }

    /** Shows an anchored menu; without a window (headless preview) it answers via snackbar. */
    private void showMenuBelow(Node anchor, M3Menu menu, String fallback) {
        if (anchor.getScene() == null || anchor.getScene().getWindow() == null) {
            feedback.showSnackbar(fallback);
            return;
        }
        menu.showBelow(anchor);
    }

    /**
     * Hands the selected CONFIRMED session to the Live Workout HUD through the owning shell
     * (the same openScreenViaRail pathway as the roster handoff, so the rail selection stays
     * on Schedule). Standalone previews answer with a snackbar.
     */
    public void onRunLive() {
        if (selectedSession == null) {
            feedback.showSnackbar("Select a session on the agenda first.");
            return;
        }
        int sessionId = selectedSession.sessionId();
        if (viewRoot.getScene() == null
                || !(viewRoot.getScene().getRoot() instanceof javafx.scene.Parent sceneRoot)) {
            feedback.showSnackbar("Run Live opens in the app window.");
            return;
        }
        Object shell = sceneRoot.getProperties().get(AppShellController.SHELL_CONTROLLER_KEY);
        if (shell instanceof AppShellController controller) {
            controller.openScreenViaRail("navSchedule", Screen.LIVE_WORKOUT_HUD,
                    hud -> {
                        if (hud instanceof LiveWorkoutController liveHud) {
                            liveHud.startSession(sessionId);
                        }
                    });
            return;
        }
        feedback.showSnackbar("Run Live opens in the app window.");
    }

    public GridPane getAgendaGrid() { return agendaGrid; }
    public M3Button getBtnRunLive() { return btnRunLive; }
    public M3TextField getSearchTrainerField() { return searchTrainerField; }
    public ListView<String> getTrainerListView() { return trainerListView; }
    public Label getLblInspectorTitle() { return lblInspectorTitle; }
    public Label getLblSessionStatus() { return lblSessionStatus; }
    public Label getLblAthleteName() { return lblAthleteName; }
    public Label getLblCoachName() { return lblCoachName; }
    public Label getLblSessionTime() { return lblSessionTime; }
    public Label getLblPriceOrSub() { return lblPriceOrSub; }
    public M3DonutChart getSlotDonutChart() { return slotDonutChart; }
    public M3Timeline getSessionTimeline() { return sessionTimeline; }
    public M3Button getBtnConfirmAction() { return btnConfirmAction; }
    public M3Button getBtnCompleteAction() { return btnCompleteAction; }
    public M3Button getBtnNoShowAction() { return btnNoShowAction; }
    public M3Button getBtnCancelAction() { return btnCancelAction; }
    public M3SplitButton getActionSplitButton() { return actionSplitButton; }
    public M3IconButton getBtnPrevWeek() { return btnPrevWeek; }
    public Label getLblCurrentDateRange() { return lblCurrentDateRange; }
    public M3IconButton getBtnNextWeek() { return btnNextWeek; }
    public M3Button getBtnToday() { return btnToday; }
    public M3Button getBtnShowAvailableTrainers() { return btnShowAvailableTrainers; }
    public M3Chip getChipFilterConfirmed() { return chipFilterConfirmed; }
    public M3Chip getChipFilterRequested() { return chipFilterRequested; }
    public M3Chip getChipFilterCancelled() { return chipFilterCancelled; }
    public VBox getInspectorContainer() { return inspectorContainer; }
    public M3IconButton getBtnCloseInspector() { return btnCloseInspector; }
}
