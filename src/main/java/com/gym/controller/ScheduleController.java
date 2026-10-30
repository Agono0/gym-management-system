package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.chart.M3DonutChart;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.textfield.M3TextField;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
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
 */
public class ScheduleController {

    /** Visible day columns of the demo agenda (Mon-Wed keeps the matrix readable at 1024px). */
    private static final int DEMO_DAY_COLUMNS = 3;
    private static final int FIRST_HOUR = 7;
    private static final int LAST_HOUR = 20;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEE dd");
    private static final DateTimeFormatter RANGE_START_FMT = DateTimeFormatter.ofPattern("MMM dd");
    private static final DateTimeFormatter RANGE_END_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML private GridPane agendaGrid;
    @FXML private M3TextField searchTrainerField;
    @FXML private ListView<?> trainerListView;
    @FXML private Label lblInspectorTitle;
    @FXML private Label lblSessionStatus;
    @FXML private Label lblAthleteName;
    @FXML private Label lblCoachName;
    @FXML private Label lblSessionTime;
    @FXML private Label lblPriceOrSub;
    @FXML private M3DonutChart slotDonutChart;
    @FXML private M3Timeline sessionTimeline;
    @FXML private M3Button btnConfirmAction;
    @FXML private M3Button btnCompleteAction;
    @FXML private M3Button btnNoShowAction;
    @FXML private M3Button btnCancelAction;
    @FXML private M3SplitButton actionSplitButton;

    @FXML private M3IconButton btnPrevWeek;
    @FXML private Label lblCurrentDateRange;
    @FXML private M3IconButton btnNextWeek;
    @FXML private M3Button btnToday;
    @FXML private M3Button btnShowAvailableTrainers;
    @FXML private M3Chip chipFilterConfirmed;
    @FXML private M3Chip chipFilterRequested;
    @FXML private M3Chip chipFilterCancelled;
    @FXML private VBox inspectorContainer;
    @FXML private M3IconButton btnCloseInspector;

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

    public ScheduleController() {
    }

    @FXML
    public void initialize() {
        installFeedbackLayer(agendaGrid);
        updateDateRangeLabel();
        populateCoachRail();
        if (agendaGrid != null && agendaGrid.getChildren().isEmpty()) {
            buildDemoAgenda();
        }
        wireStatusFilters();
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
    @SuppressWarnings("unchecked")
    private void populateCoachRail() {
        if (trainerListView == null) {
            return;
        }
        ListView<String> list = (ListView<String>) trainerListView;
        list.setPlaceholder(new Label("No coaches match your search."));
        list.setItems(FXCollections.observableArrayList(coachSummaries(coaches)));
        if (searchTrainerField != null) {
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
        if (chipFilterConfirmed != null) {
            chipFilterConfirmed.setOnAction(e -> applyAgendaFilter());
        }
        if (chipFilterRequested != null) {
            chipFilterRequested.setOnAction(e -> applyAgendaFilter());
        }
        if (chipFilterCancelled != null) {
            chipFilterCancelled.setOnAction(e -> applyAgendaFilter());
        }
    }

    /** Shows only the agenda cards whose status chip is selected; an empty result is reachable. */
    private void applyAgendaFilter() {
        for (M3Card card : agendaCards) {
            Object status = card.getProperties().get("sessionStatus");
            boolean show = switch (String.valueOf(status)) {
                case "CONFIRMED" -> chipFilterConfirmed == null || chipFilterConfirmed.isSelected();
                case "REQUESTED" -> chipFilterRequested != null && chipFilterRequested.isSelected();
                case "CANCELLED" -> chipFilterCancelled != null && chipFilterCancelled.isSelected();
                default -> true;
            };
            card.setVisible(show);
            card.setManaged(show);
        }
    }

    private void updateDateRangeLabel() {
        if (lblCurrentDateRange != null) {
            lblCurrentDateRange.setText(RANGE_START_FMT.format(weekStart)
                    + " - " + RANGE_END_FMT.format(weekStart.plusDays(6)));
        }
    }

    @FXML
    public void onPrevWeek() {
        weekStart = weekStart.minusWeeks(1);
        updateDateRangeLabel();
        feedback.showSnackbar("Showing week " + lblCurrentDateRange.getText() + " (demo)");
    }

    @FXML
    public void onPrevWeek(ActionEvent event) {
        onPrevWeek();
    }

    @FXML
    public void onNextWeek() {
        weekStart = weekStart.plusWeeks(1);
        updateDateRangeLabel();
        feedback.showSnackbar("Showing week " + lblCurrentDateRange.getText() + " (demo)");
    }

    @FXML
    public void onNextWeek(ActionEvent event) {
        onNextWeek();
    }

    @FXML
    public void onToday() {
        weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        updateDateRangeLabel();
        feedback.showSnackbar("Back to the current week (demo)");
    }

    @FXML
    public void onToday(ActionEvent event) {
        onToday();
    }

    @FXML
    public void onCloseInspector() {
        if (inspectorContainer != null) {
            inspectorContainer.setVisible(false);
            inspectorContainer.setManaged(false);
        }
    }

    @FXML
    public void onCloseInspector(ActionEvent event) {
        onCloseInspector();
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
        RowConstraints row = new RowConstraints(56);
        agendaGrid.getRowConstraints().setAll(row);

        for (int d = 0; d < DEMO_DAY_COLUMNS; d++) {
            Label header = new Label(DAY_FMT.format(weekStart.plusDays(d)));
            header.getStyleClass().add("title-small");
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
            time.getStyleClass().add("body-small");
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
        VBox box = new VBox(2, new Label(session[1]), new Label(session[0] + " · " + session[2]));
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
        if (lblInspectorTitle != null) {
            lblInspectorTitle.setText("Session Inspector");
        }
        if (lblSessionStatus != null) {
            lblSessionStatus.setText(session[2]);
        }
        if (lblAthleteName != null) {
            lblAthleteName.setText("Athlete: " + athlete);
        }
        if (lblCoachName != null) {
            lblCoachName.setText(isClass ? "Coach: Elena Rostova" : "Coach: Marcus Vance");
        }
        if (lblSessionTime != null) {
            int startHour = Integer.parseInt(session[0].substring(0, 2));
            lblSessionTime.setText("Time: " + session[0] + " - "
                    + String.format("%02d:00", (startHour + 1) % 24));
        }
        if (lblPriceOrSub != null) {
            lblPriceOrSub.setText(isClass ? "Price: $15 drop-in" : "Price: Covered by Subscription");
        }
        if (slotDonutChart != null) {
            slotDonutChart.getRings().clear();
            slotDonutChart.getRings().addAll(
                    new M3DonutChart.Ring("Booked", 4, 8, null),
                    new M3DonutChart.Ring("Waitlist", 2, 8, Color.web("#00E5FF")));
            slotDonutChart.setCenterHeadline("4/8");
        }
        if (sessionTimeline != null) {
            sessionTimeline.getItems().clear();
            sessionTimeline.addItem(session[0], "Slot requested", "Booked online by the athlete", M3Timeline.Status.COMPLETED);
            sessionTimeline.addItem("+2m", "Coach review", "Coach confirmed the slot", M3Timeline.Status.ACTIVE);
            sessionTimeline.addItem("—", "Session start", "Pending", M3Timeline.Status.PENDING);
        }
        if (inspectorContainer != null) {
            inspectorContainer.setVisible(true);
            inspectorContainer.setManaged(true);
        }
    }

    /** Applies a lifecycle change to the selected session and answers visibly. */
    private void applyLifecycle(String newStatus, String message) {
        if (selectedSession == null) {
            feedback.showSnackbar("Select a session on the agenda first.");
            return;
        }
        selectedSession[2] = newStatus;
        if (lblSessionStatus != null) {
            lblSessionStatus.setText(newStatus);
        }
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

    @FXML
    public void onBook() {
        feedback.showSnackbar("Pick an open slot on the agenda, then confirm it here (demo)");
    }

    @FXML
    public void onBook(ActionEvent event) {
        onBook();
    }

    @FXML
    public void onRequest() {
        applyLifecycle("REQUESTED", "Session requested");
    }

    @FXML
    public void onRequest(ActionEvent event) {
        onRequest();
    }

    @FXML
    public void onConfirmClick() {
        applyLifecycle("CONFIRMED", "Session confirmed");
    }

    @FXML
    public void onConfirmClick(ActionEvent event) {
        onConfirmClick();
    }

    @FXML
    public void onConfirm(int sessionId) {
        onConfirmClick();
    }

    @FXML
    public void onCompleteClick() {
        applyLifecycle("COMPLETED", "Session completed");
    }

    @FXML
    public void onCompleteClick(ActionEvent event) {
        onCompleteClick();
    }

    @FXML
    public void onComplete(int sessionId) {
        onCompleteClick();
    }

    @FXML
    public void onNoShowClick() {
        applyLifecycle("NO-SHOW", "Session marked as no-show");
    }

    @FXML
    public void onNoShowClick(ActionEvent event) {
        onNoShowClick();
    }

    @FXML
    public void onNoShow(int sessionId) {
        onNoShowClick();
    }

    @FXML
    public void onReserve() {
        applyLifecycle("REQUESTED", "Slot reserved");
    }

    @FXML
    public void onReserve(ActionEvent event) {
        onReserve();
    }

    /** Cancel is destructive: confirm in an M3Dialog, then apply with a snackbar answer. */
    @FXML
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

    @FXML
    public void onCancel(ActionEvent event) {
        onCancel();
    }

    @FXML
    public void onSubscribe() {
        feedback.showSnackbar("Subscription flow starts from the athlete's profile (demo)");
    }

    @FXML
    public void onSubscribe(ActionEvent event) {
        onSubscribe();
    }

    @FXML
    public void onRenewSubscription() {
        feedback.showSnackbar("Renewal flow starts from the athlete's profile (demo)");
    }

    @FXML
    public void onRenewSubscription(ActionEvent event) {
        onRenewSubscription();
    }

    @FXML
    public void onCancelSubscription() {
        feedback.showSnackbar("Cancelling a subscription requires confirmation (demo)");
    }

    @FXML
    public void onCancelSubscription(ActionEvent event) {
        onCancelSubscription();
    }

    /**
     * Toggles the coach rail between the full roster and only the coaches with open slots this
     * week, answering with a snackbar either way.
     */
    @FXML
    public void showAvailableTrainers() {
        if (trainerListView == null) {
            return;
        }
        showingAvailableOnly = !showingAvailableOnly;
        List<Coach> visible = showingAvailableOnly
                ? coaches.stream().filter(Coach::availableThisWeek).toList()
                : coaches;
        ((ListView<String>) trainerListView).getItems().setAll(coachSummaries(visible));
        if (btnShowAvailableTrainers != null) {
            btnShowAvailableTrainers.setText(showingAvailableOnly
                    ? "Show All Coaches"
                    : "Available Coaches Matrix");
        }
        feedback.showSnackbar(showingAvailableOnly
                ? "Showing " + visible.size() + " coaches with open slots this week (demo)"
                : "Showing the full coach roster (demo)");
    }

    @FXML
    public void showAvailableTrainers(ActionEvent event) {
        showAvailableTrainers();
    }

    @FXML
    public void showTrainerSlots(int trainerId) {
        feedback.showSnackbar("Showing slots for coach #" + trainerId + " (demo)");
    }

    public GridPane getAgendaGrid() { return agendaGrid; }
    public M3TextField getSearchTrainerField() { return searchTrainerField; }
    public ListView<?> getTrainerListView() { return trainerListView; }
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
