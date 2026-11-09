package com.gym.controller;

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
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.textfield.M3TextField;
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
import javafx.scene.paint.Color;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure UI controller for Master Schedule & Class Timetable screen.
 * Conforms to Section 7 of docs/ui/workouts/SCHEDULE_CALENDAR_UI.md and docs/UML.md.
 * Owns the week navigation, the demo agenda population, the status filtering, the coach rail,
 * the session inspector and the session lifecycle actions (confirm/complete/no-show/cancel).
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former schedule.fxml: week navigation bar, coach rail, agenda matrix, session inspector.
 */
public class ScheduleController implements ScreenController {

    /** Visible day columns of the demo agenda (Mon-Wed keeps the matrix readable at 1024px). */
    private static final int DEMO_DAY_COLUMNS = 3;
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

    /** Start (Monday) of the week shown in the header; refreshed by the navigation buttons. */
    private LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Demo coach roster: name plus whether the coach has open slots this week. */
    private record Coach(String name, String detail, boolean availableThisWeek) {}

    private final List<Coach> coaches = List.of(
            new Coach("Elena Rostova", "Olympic Lifting · 4 open slots", true),
            new Coach("Marcus Vance", "Powerbuilding · fully booked", false),
            new Coach("Kim Park", "Post-Rehab · 2 open slots", true),
            new Coach("Ana Silva", "VO2 & Conditioning · 6 open slots", true),
            new Coach("Dana Wolfe", "Strength Basics · fully booked", false),
            new Coach("Omar Haddad", "Circuits · 3 open slots", true));

    /** Demo session cards currently on the agenda grid, in placement order. */
    private final List<M3Card> agendaCards = new ArrayList<>();

    /** The session currently loaded into the inspector, or null before the first click. */
    private String[] selectedSession;

    /** Whether the coach rail currently shows only this week's available coaches. */
    private boolean showingAvailableOnly;

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
        VBox.setVgrow(agendaScroll, Priority.ALWAYS);
        calendarPane.getChildren().addAll(statusFilterStrip, agendaScroll);

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
        lifecycleButtons.getChildren().addAll(btnConfirmAction, btnCompleteAction,
                btnNoShowAction, btnCancelAction);

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
        populateCoachRail();
        if (agendaGrid.getChildren().isEmpty()) {
            buildDemoAgenda();
        }
        wireStatusFilters();
        // The inspector is contextual: it starts collapsed so the agenda (this screen's primary
        // answer) gets the full width, and opens over the session the user clicks.
        collapseInspector();
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

    /** Fills the coach rail and wires the search field to filter it live. */
    private void populateCoachRail() {
        ListView<String> list = trainerListView;
        list.setPlaceholder(new Label("No coaches match your search."));
        list.setItems(FXCollections.observableArrayList(coachSummaries(coaches)));
        searchTrainerField.getInput().textProperty().addListener((obs, o, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            List<String> visible = new ArrayList<>();
            for (Coach coach : coaches) {
                if (coach.name().toLowerCase(Locale.ROOT).contains(q)
                        || coach.detail().toLowerCase(Locale.ROOT).contains(q)) {
                    visible.add(coach.name() + " — " + coach.detail());
                }
            }
            list.getItems().setAll(visible);
        });
    }

    private static List<String> coachSummaries(List<Coach> source) {
        List<String> summaries = new ArrayList<>();
        for (Coach coach : source) {
            summaries.add(coach.name() + " — " + coach.detail());
        }
        return summaries;
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
        feedback.showSnackbar("Showing week " + lblCurrentDateRange.getText() + " (demo)");
    }

    public void onNextWeek() {
        weekStart = weekStart.plusWeeks(1);
        updateDateRangeLabel();
        feedback.showSnackbar("Showing week " + lblCurrentDateRange.getText() + " (demo)");
    }

    public void onToday() {
        weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        updateDateRangeLabel();
        feedback.showSnackbar("Back to the current week (demo)");
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
     * Fills the agenda matrix with hour rows, day headers and a few sample sessions so the
     * center pane is not an empty grid. Cards are tagged with their status (for the chips),
     * clickable (loads the inspector) and keyboard operable. Demo content only — replaced once
     * real bookings flow.
     */
    private void buildDemoAgenda() {
        agendaGrid.getChildren().clear();
        agendaCards.clear();
        ColumnConstraints timeCol = new ColumnConstraints(64);
        ColumnConstraints dayCol = new ColumnConstraints();
        dayCol.setHgrow(Priority.ALWAYS);
        agendaGrid.getColumnConstraints().setAll(timeCol, dayCol, dayCol, dayCol);
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

        for (int d = 0; d < DEMO_DAY_COLUMNS; d++) {
            Label header = new Label(DAY_FMT.format(weekStart.plusDays(d)));
            header.getStyleClass().add("m3-list-headline");
            GridPane.setHalignment(header, HPos.CENTER);
            agendaGrid.add(header, d + 1, 0);
        }

        String[][][] sessions = {
                {{"07:00", "John Doe · PT", "CONFIRMED"}, {"09:00", "Ana Silva · PT", "REQUESTED"}, {"17:00", "Group HIIT", "CONFIRMED"}},
                {{"08:00", "Leo Martins · PT", "CONFIRMED"}, {"10:00", "Zoe Chen · PT", "CANCELLED"}, {"18:00", "Mobility Class", "CONFIRMED"}},
                {{"07:30", "Kim Park · PT", "REQUESTED"}, {"16:00", "Powerlifting Club", "CONFIRMED"}, {"19:00", "Spin Class", "CONFIRMED"}}
        };
        for (int h = FIRST_HOUR; h <= LAST_HOUR; h++) {
            Label time = new Label(String.format("%02d:00", h));
            time.getStyleClass().add("m3-list-overline");
            GridPane.setValignment(time, VPos.TOP);
            agendaGrid.add(time, 0, h - FIRST_HOUR + 1);
        }
        // Place the sample sessions at plausible hours; row 0 is the header row.
        int[] hours = {7, 9, 17, 8, 10, 18, 7, 16, 19};
        for (int d = 0; d < DEMO_DAY_COLUMNS; d++) {
            for (int s = 0; s < sessions[d].length; s++) {
                String[] session = sessions[d][s];
                M3Card card = createSessionCard(session, weekStart.plusDays(d));
                agendaCards.add(card);
                agendaGrid.add(card, d + 1, hours[d * DEMO_DAY_COLUMNS + s] - FIRST_HOUR + 1);
            }
        }
    }

    /** Builds one agenda card: tagged, clickable, keyboard operable. */
    private M3Card createSessionCard(String[] session, LocalDate day) {
        Label title = new Label(session[1]);
        title.getStyleClass().add("m3-list-supporting");
        Label caption = new Label(session[0] + " · " + session[2]);
        caption.getStyleClass().add("m3-list-overline");
        VBox box = new VBox(2, title, caption);
        box.setPadding(new Insets(6));
        M3Card card = new M3Card(CardVariant.OUTLINED);
        card.setContent(box);
        card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        card.getProperties().put("sessionStatus", session[2]);
        card.getProperties().put("sessionTime", session[0]);
        card.getProperties().put("sessionDay", day);
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

    /** Loads a demo session into the inspector and shows it. */
    private void openInspector(String[] session) {
        selectedSession = session;
        String title = session[1];
        boolean isClass = !title.contains("·");
        String athlete = isClass ? "Group booking" : title.substring(0, title.indexOf('·')).trim();
        lblInspectorTitle.setText("Session Inspector");
        lblSessionStatus.setText(session[2]);
        lblAthleteName.setText("Athlete: " + athlete);
        lblCoachName.setText(isClass ? "Coach: Elena Rostova" : "Coach: Marcus Vance");
        int startHour = Integer.parseInt(session[0].substring(0, 2));
        lblSessionTime.setText("Time: " + session[0] + " - "
                + String.format("%02d:00", (startHour + 1) % 24));
        lblPriceOrSub.setText(isClass ? "Price: $15 drop-in" : "Price: Covered by Subscription");
        slotDonutChart.getRings().clear();
        slotDonutChart.getRings().addAll(
                new M3DonutChart.Ring("Booked", 4, 8, null),
                new M3DonutChart.Ring("Waitlist", 2, 8, Color.web("#00E5FF")));
        slotDonutChart.setCenterHeadline("4/8");
        sessionTimeline.getItems().clear();
        sessionTimeline.addItem(session[0], "Slot requested", "Booked online by the athlete", M3Timeline.Status.COMPLETED);
        sessionTimeline.addItem("+2m", "Coach review", "Coach confirmed the slot", M3Timeline.Status.ACTIVE);
        sessionTimeline.addItem("—", "Session start", "Pending", M3Timeline.Status.PENDING);
        expandInspector();
    }

    /** Applies a lifecycle change to the selected session and answers visibly. */
    private void applyLifecycle(String newStatus, String message) {
        if (selectedSession == null) {
            feedback.showSnackbar("Select a session on the agenda first.");
            return;
        }
        selectedSession[2] = newStatus;
        lblSessionStatus.setText(newStatus);
        for (M3Card card : agendaCards) {
            if (!selectedSession[0].equals(String.valueOf(card.getProperties().get("sessionTime")))) {
                continue;
            }
            if (sessionMatchesCard(card)) {
                card.getProperties().put("sessionStatus", newStatus);
                if (card.getContent() instanceof VBox box && box.getChildren().size() > 1
                        && box.getChildren().get(1) instanceof Label caption) {
                    caption.setText(selectedSession[0] + " · " + newStatus);
                }
                break;
            }
        }
        applyAgendaFilter();
        feedback.showSnackbar(message + " (demo)");
    }

    /**
     * Matches the selected session to its agenda card. The card is the right one when its status
     * tag and athlete label still correspond to the selection captured at open time.
     */
    private boolean sessionMatchesCard(M3Card card) {
        if (!(card.getContent() instanceof VBox box) || box.getChildren().isEmpty()
                || !(box.getChildren().get(0) instanceof Label title)) {
            return false;
        }
        return title.getText().equals(selectedSession[1]);
    }

    public void onBook() {
        feedback.showSnackbar("Pick an open slot on the agenda, then confirm it here (demo)");
    }

    public void onRequest() {
        applyLifecycle("REQUESTED", "Session requested");
    }

    public void onConfirmClick() {
        applyLifecycle("CONFIRMED", "Session confirmed");
    }

    public void onConfirm(int sessionId) {
        onConfirmClick();
    }

    public void onCompleteClick() {
        applyLifecycle("COMPLETED", "Session completed");
    }

    public void onComplete(int sessionId) {
        onCompleteClick();
    }

    public void onNoShowClick() {
        applyLifecycle("NO-SHOW", "Session marked as no-show");
    }

    public void onNoShow(int sessionId) {
        onNoShowClick();
    }

    public void onReserve() {
        applyLifecycle("REQUESTED", "Slot reserved");
    }

    /** Cancel is destructive: confirm in an M3Dialog, then apply with a snackbar answer. */
    public void onCancel() {
        if (selectedSession == null) {
            feedback.showSnackbar("Select a session on the agenda first.");
            return;
        }
        M3Button keep = M3Button.text("Keep session");
        M3Button confirm = M3Button.text("Cancel session");
        M3Dialog dialog = M3Dialog.basic("Cancel this session?",
                new Label(selectedSession[1] + " at " + selectedSession[0]
                        + " will be marked CANCELLED. The athlete is notified."),
                keep, confirm);
        dialog.setDefaultAction(confirm::activate);
        keep.setOnAction(e -> dialog.hide());
        confirm.setOnAction(e -> {
            dialog.hide();
            applyLifecycle("CANCELLED", "Session cancelled");
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
     * week, answering with a snackbar either way.
     */
    public void showAvailableTrainers() {
        showingAvailableOnly = !showingAvailableOnly;
        List<Coach> visible = showingAvailableOnly
                ? coaches.stream().filter(Coach::availableThisWeek).toList()
                : coaches;
        trainerListView.getItems().setAll(coachSummaries(visible));
        btnShowAvailableTrainers.setText(showingAvailableOnly
                ? "Show All Coaches"
                : "Available Coaches Matrix");
        feedback.showSnackbar(showingAvailableOnly
                ? "Showing " + visible.size() + " coaches with open slots this week (demo)"
                : "Showing the full coach roster (demo)");
    }

    public void showTrainerSlots(int trainerId) {
        feedback.showSnackbar("Showing slots for coach #" + trainerId + " (demo)");
    }

    public GridPane getAgendaGrid() { return agendaGrid; }
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
