package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.chart.M3DonutChart;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Pure UI skeleton controller for Master Schedule & Class Timetable screen.
 * Strictly conforms to Section 7 of docs/ui/workouts/SCHEDULE_CALENDAR_UI.md and docs/UML.md.
 * Owns the week navigation, the inspector visibility and the demo agenda population.
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

    public ScheduleController() {
    }

    @FXML
    public void initialize() {
        updateDateRangeLabel();
        if (agendaGrid != null && agendaGrid.getChildren().isEmpty()) {
            buildDemoAgenda();
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
    }

    @FXML
    public void onPrevWeek(ActionEvent event) {
        onPrevWeek();
    }

    @FXML
    public void onNextWeek() {
        weekStart = weekStart.plusWeeks(1);
        updateDateRangeLabel();
    }

    @FXML
    public void onNextWeek(ActionEvent event) {
        onNextWeek();
    }

    @FXML
    public void onToday() {
        weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        updateDateRangeLabel();
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
     * center pane is not an empty grid. Demo content only — replaced once real bookings flow.
     */
    private void buildDemoAgenda() {
        agendaGrid.getChildren().clear();
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
                M3Card card = new M3Card(CardVariant.OUTLINED);
                VBox box = new VBox(2, new Label(session[1]), new Label(session[0] + " · " + session[2]));
                box.setPadding(new Insets(6));
                card.setContent(box);
                card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                GridPane.setHgrow(card, Priority.ALWAYS);
                GridPane.setVgrow(card, Priority.ALWAYS);
                agendaGrid.add(card, d + 1, hours[d * DEMO_DAY_COLUMNS + s] - FIRST_HOUR + 1);
            }
        }
    }

    @FXML
    public void onBook() {
    }

    @FXML
    public void onBook(ActionEvent event) {
    }

    @FXML
    public void onRequest() {
    }

    @FXML
    public void onRequest(ActionEvent event) {
    }

    @FXML
    public void onConfirmClick() {
    }

    @FXML
    public void onConfirmClick(ActionEvent event) {
    }

    @FXML
    public void onConfirm(int sessionId) {
    }

    @FXML
    public void onCompleteClick() {
    }

    @FXML
    public void onCompleteClick(ActionEvent event) {
    }

    @FXML
    public void onComplete(int sessionId) {
    }

    @FXML
    public void onNoShowClick() {
    }

    @FXML
    public void onNoShowClick(ActionEvent event) {
    }

    @FXML
    public void onNoShow(int sessionId) {
    }

    @FXML
    public void onReserve() {
    }

    @FXML
    public void onReserve(ActionEvent event) {
    }

    @FXML
    public void onCancel() {
    }

    @FXML
    public void onCancel(ActionEvent event) {
    }

    @FXML
    public void onSubscribe() {
    }

    @FXML
    public void onSubscribe(ActionEvent event) {
    }

    @FXML
    public void onRenewSubscription() {
    }

    @FXML
    public void onRenewSubscription(ActionEvent event) {
    }

    @FXML
    public void onCancelSubscription() {
    }

    @FXML
    public void onCancelSubscription(ActionEvent event) {
    }

    @FXML
    public void showAvailableTrainers() {
    }

    @FXML
    public void showAvailableTrainers(ActionEvent event) {
    }

    @FXML
    public void showTrainerSlots(int trainerId) {
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
