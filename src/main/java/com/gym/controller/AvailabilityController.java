package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3ActivityCalendar;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.data.M3Stepper;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.selection.M3Switch;
import io.m3fx.controls.sheet.M3BottomSheet;
import io.m3fx.controls.slider.M3Slider;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * Controller for the Trainer Availability &amp; Slot Capacity Matrix screen.
 * UI-contract only (no backend/service dependencies): owns the week navigation, the demo
 * density heatmap and activity calendar, the daily/slot cap controls and the batch publisher.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former availability.fxml: control strip, density banner, activity calendar, bottom command
 * bar, batch publisher sheet.
 */
public class AvailabilityController implements ScreenController {

    private static final DateTimeFormatter RANGE_START_FMT = DateTimeFormatter.ofPattern("MMM dd");
    private static final DateTimeFormatter RANGE_END_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final List<String> DAY_LABELS = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private M3TopAppBar topAppBar;
    private M3GlassPane controlStrip;
    private M3SplitButton btnCoachSelector;
    private M3IconButton btnPrevWeek;
    private Label lblDateRange;
    private M3IconButton btnNextWeek;
    private M3Button btnToday;
    private M3Stepper stepperDailyCap;
    private M3Button btnApplyDailyCap;
    private M3Button btnBatchPublish;
    private M3Card cardDensityBanner;
    private Label lblDensityStats;
    private M3Heatmap heatmapDensity;
    private M3ActivityCalendar calendarGrid;
    private M3GlassPane bottomCommandBar;
    private Label lblSelectedSlotTitle;
    private Label lblSlotOccupancy;
    private M3Switch switchSlotOpen;
    private M3Slider sliderSlotCap;
    private Label lblSlotCapValue;
    private M3Button btnSaveSlotCap;
    private M3BottomSheet bottomSheetBatchPublish;
    private M3IconButton btnCloseBottomSheet;
    private M3TextField txtBatchStartDate;
    private M3TextField txtBatchEndDate;
    private M3TextField txtBatchStartTime;
    private M3TextField txtBatchEndTime;
    private M3Stepper stepperBatchCap;
    private Label lblBatchValidation;
    private M3Button btnCancelBatch;
    private M3Button btnConfirmBatchPublish;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Monday of the week shown in the header; refreshed by the navigation buttons. */
    private LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

    /** Runtime replacement for the wizard {@code M3Stepper}: the daily cap picker. */
    private M3Slider dailyCapSlider;

    /** Runtime replacement for the wizard {@code M3Stepper}: the batch cap picker. */
    private M3Slider batchCapSlider;

    /**
     * Builds the complete Trainer Availability view and initializes all behavior. Same ordering
     * semantics the FXMLLoader used to provide: the node tree exists and every field is
     * assigned before the former {@code initialize()} work runs.
     */
    public AvailabilityController() {
        viewRoot = buildView();


    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        topAppBar.setId("topAppBar");
        controlStrip.setId("controlStrip");
        btnCoachSelector.setId("btnCoachSelector");
        btnPrevWeek.setId("btnPrevWeek");
        lblDateRange.setId("lblDateRange");
        btnNextWeek.setId("btnNextWeek");
        btnToday.setId("btnToday");
        stepperDailyCap.setId("stepperDailyCap");
        btnApplyDailyCap.setId("btnApplyDailyCap");
        btnBatchPublish.setId("btnBatchPublish");
        cardDensityBanner.setId("cardDensityBanner");
        lblDensityStats.setId("lblDensityStats");
        heatmapDensity.setId("heatmapDensity");
        calendarGrid.setId("calendarGrid");
        bottomCommandBar.setId("bottomCommandBar");
        lblSelectedSlotTitle.setId("lblSelectedSlotTitle");
        lblSlotOccupancy.setId("lblSlotOccupancy");
        switchSlotOpen.setId("switchSlotOpen");
        sliderSlotCap.setId("sliderSlotCap");
        lblSlotCapValue.setId("lblSlotCapValue");
        btnSaveSlotCap.setId("btnSaveSlotCap");
        bottomSheetBatchPublish.setId("bottomSheetBatchPublish");
        btnCloseBottomSheet.setId("btnCloseBottomSheet");
        txtBatchStartDate.setId("txtBatchStartDate");
        txtBatchEndDate.setId("txtBatchEndDate");
        txtBatchStartTime.setId("txtBatchStartTime");
        txtBatchEndTime.setId("txtBatchEndTime");
        stepperBatchCap.setId("stepperBatchCap");
        lblBatchValidation.setId("lblBatchValidation");
        btnCancelBatch.setId("btnCancelBatch");
        btnConfirmBatchPublish.setId("btnConfirmBatchPublish");
        installFeedbackLayer(topAppBar);
        installDailyCapSlider();
        installBatchCapSlider();
        wireCoachSelector();
        wireSlotControls();
        populateActivityCalendar();
        refreshWeekView();
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for availability.fxml) and
     * wires every handler.
     */
    private StackPane buildView() {
        // Primary vertical shell.
        VBox shell = new VBox();
        shell.setSpacing(0);

        // Top app bar.
        topAppBar = new M3TopAppBar("Trainer Scheduling :: Availability & Slot Capacity",
                io.m3fx.controls.appbar.TopAppBarVariant.SMALL);
        topAppBar.setNavigationIcon(M3Icon.symbol("arrow_back"));

        // Control strip glass pane (wraps instead of clipping on narrow windows).
        controlStrip = new M3GlassPane();
        controlStrip.getStyleClass().add("control-strip");
        FlowPane controlFlow = new FlowPane(16, 8);
        controlFlow.setAlignment(Pos.CENTER_LEFT);
        controlFlow.setPadding(new Insets(12, 24, 12, 24));
        btnCoachSelector = new M3SplitButton("Elena Rostova (#101)", ButtonVariant.FILLED, null);
        HBox weekNavRow = new HBox(8);
        weekNavRow.setAlignment(Pos.CENTER_LEFT);
        btnPrevWeek = new M3IconButton(M3Icon.symbol("chevron_left"), IconButtonVariant.STANDARD);
        btnPrevWeek.setAccessibleText("Previous Week");
        btnPrevWeek.setOnAction(e -> onPrevWeek());
        lblDateRange = new Label("Oct 26 - Nov 01, 2026 (Week 44)");
        lblDateRange.getStyleClass().add("title-small");
        btnNextWeek = new M3IconButton(M3Icon.symbol("chevron_right"), IconButtonVariant.STANDARD);
        btnNextWeek.setAccessibleText("Next Week");
        btnNextWeek.setOnAction(e -> onNextWeek());
        btnToday = new M3Button("Today", ButtonVariant.OUTLINED);
        btnToday.setOnAction(e -> onToday());
        weekNavRow.getChildren().addAll(btnPrevWeek, lblDateRange, btnNextWeek, btnToday);
        HBox dailyCapRow = new HBox(8);
        dailyCapRow.setAlignment(Pos.CENTER_LEFT);
        stepperDailyCap = new M3Stepper();
        btnApplyDailyCap = new M3Button("Apply to Day", ButtonVariant.TONAL);
        btnApplyDailyCap.setOnAction(e -> onApplyDailyCap());
        dailyCapRow.getChildren().addAll(
                styledLabel("Daily Cap:", "label-medium"),
                stepperDailyCap,
                btnApplyDailyCap);
        btnBatchPublish = new M3Button("+ Batch Publish Slots", ButtonVariant.FILLED);
        btnBatchPublish.setOnAction(e -> onBatchPublish());
        controlFlow.getChildren().addAll(
                styledLabel("Coach:", "label-medium"),
                btnCoachSelector,
                new Separator(Orientation.VERTICAL),
                weekNavRow,
                new Separator(Orientation.VERTICAL),
                dailyCapRow,
                btnBatchPublish);
        controlStrip.getGlassChildren().add(controlFlow);

        // Main workspace.
        VBox workspace = new VBox(16);
        workspace.setPadding(new Insets(20, 20, 64, 20));

        // Weekly density HUD banner.
        cardDensityBanner = new M3Card();
        VBox densityContent = new VBox(12);
        HBox densityHeader = new HBox();
        densityHeader.setAlignment(Pos.CENTER_LEFT);
        lblDensityStats = new Label(
                "Confirmed Sessions: 38 / 52 Capacity (73.1% Saturation) | Peak: Wed 17:00");
        lblDensityStats.getStyleClass().add("body-medium");
        Region densitySpacer = new Region();
        HBox.setHgrow(densitySpacer, Priority.ALWAYS);
        densityHeader.getChildren().addAll(lblDensityStats, densitySpacer,
                styledLabel("Derived Statuses Active (Rule 13)", "label-small"));
        heatmapDensity = new M3Heatmap();
        heatmapDensity.setPrefHeight(120);
        densityContent.getChildren().addAll(densityHeader, heatmapDensity);
        cardDensityBanner.setContent(densityContent);

        // Interactive annual booking calendar.
        VBox calendarSection = new VBox(8);
        VBox.setVgrow(calendarSection, Priority.ALWAYS);
        HBox calendarHeader = new HBox(12);
        calendarHeader.setAlignment(Pos.CENTER_LEFT);
        calendarHeader.getChildren().addAll(
                styledLabel("Booking Activity Calendar", "title-medium"),
                styledLabel("One cell per day; darker cells mean more booked slots", "body-small"));
        calendarGrid = new M3ActivityCalendar();
        calendarGrid.setMinHeight(540);
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);
        calendarSection.getChildren().addAll(calendarHeader, calendarGrid);

        workspace.getChildren().addAll(cardDensityBanner, calendarSection);
        ScrollPane workspaceScroll = new ScrollPane(workspace);
        workspaceScroll.setFitToWidth(true);
        workspaceScroll.setFitToHeight(false);
        workspaceScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        workspaceScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(workspaceScroll, Priority.ALWAYS);

        // Bottom action & cap modulation command bar (wraps instead of clipping).
        bottomCommandBar = new M3GlassPane();
        bottomCommandBar.getStyleClass().add("bottom-command-bar");
        FlowPane commandFlow = new FlowPane(20, 8);
        commandFlow.setAlignment(Pos.CENTER_LEFT);
        commandFlow.setPadding(new Insets(16, 24, 16, 24));
        VBox slotIdentity = new VBox(4);
        lblSelectedSlotTitle = new Label("Selected Slot: #SLOT-882 (Wed 2026-10-28 @ 07:00)");
        lblSelectedSlotTitle.getStyleClass().add("title-small");
        lblSlotOccupancy = new Label("Occupancy: 4 Confirmed Athletes (Derived State: FULL)");
        lblSlotOccupancy.getStyleClass().add("body-small");
        slotIdentity.getChildren().addAll(lblSelectedSlotTitle, lblSlotOccupancy);
        HBox openSwitchRow = new HBox(12);
        openSwitchRow.setAlignment(Pos.CENTER_LEFT);
        switchSlotOpen = new M3Switch(true);
        openSwitchRow.getChildren().addAll(styledLabel("Slot Status:", "label-medium"), switchSlotOpen);
        HBox capRow = new HBox(12);
        capRow.setAlignment(Pos.CENTER_LEFT);
        sliderSlotCap = new M3Slider(1, 10, 4);
        sliderSlotCap.setDiscrete(true);
        sliderSlotCap.setMajorTickUnit(1);
        sliderSlotCap.setMinorTickCount(0);
        sliderSlotCap.setShowTickLabels(true);
        sliderSlotCap.setPrefWidth(260);
        lblSlotCapValue = new Label("4 Athletes");
        lblSlotCapValue.getStyleClass().add("title-small");
        btnSaveSlotCap = new M3Button("Save Slot Cap", ButtonVariant.FILLED);
        btnSaveSlotCap.setOnAction(e -> onSaveSlotCap());
        capRow.getChildren().addAll(
                styledLabel("Per-Slot Cap (Rule 8):", "label-medium"),
                sliderSlotCap,
                lblSlotCapValue,
                btnSaveSlotCap);
        commandFlow.getChildren().addAll(
                slotIdentity,
                new Separator(Orientation.VERTICAL),
                openSwitchRow,
                new Separator(Orientation.VERTICAL),
                capRow);
        bottomCommandBar.getGlassChildren().add(commandFlow);

        shell.getChildren().addAll(topAppBar, controlStrip, workspaceScroll, bottomCommandBar);

        // Batch slot publisher slide-up bottom sheet.
        bottomSheetBatchPublish = new M3BottomSheet();
        bottomSheetBatchPublish.setVisible(false);
        StackPane.setAlignment(bottomSheetBatchPublish, Pos.BOTTOM_CENTER);
        VBox publisherContent = new VBox(20);
        publisherContent.setPrefWidth(640);
        publisherContent.getStyleClass().add("batch-publisher-card");
        publisherContent.setPadding(new Insets(24));
        HBox publisherHeader = new HBox();
        publisherHeader.setAlignment(Pos.CENTER_LEFT);
        btnCloseBottomSheet = new M3IconButton(M3Icon.symbol("close"), IconButtonVariant.STANDARD);
        btnCloseBottomSheet.setAccessibleText("Close batch publisher");
        btnCloseBottomSheet.setOnAction(e -> onCloseBottomSheet());
        Region publisherSpacer = new Region();
        HBox.setHgrow(publisherSpacer, Priority.ALWAYS);
        publisherHeader.getChildren().addAll(
                styledLabel("Batch Publish Hourly Availability Slots", "headline-small"),
                publisherSpacer,
                btnCloseBottomSheet);
        VBox dateRangeSection = new VBox(12);
        dateRangeSection.getChildren().add(styledLabel(
                "1. Select Date Range (ISO-8601 yyyy-MM-dd per Rule 9):", "label-medium"));
        HBox dateRow = new HBox(12);
        txtBatchStartDate = new M3TextField("Start Date (yyyy-MM-dd)",
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        txtBatchEndDate = new M3TextField("End Date (yyyy-MM-dd)",
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        HBox.setHgrow(txtBatchStartDate, Priority.ALWAYS);
        HBox.setHgrow(txtBatchEndDate, Priority.ALWAYS);
        dateRow.getChildren().addAll(txtBatchStartDate, txtBatchEndDate);
        dateRangeSection.getChildren().add(dateRow);
        VBox hourWindowSection = new VBox(12);
        hourWindowSection.getChildren().add(styledLabel(
                "2. Daily Hourly Window (Rule 8: startTime < endTime):", "label-medium"));
        HBox timeRow = new HBox(12);
        txtBatchStartTime = new M3TextField("Start (e.g. 07:00)",
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        txtBatchEndTime = new M3TextField("End (e.g. 11:00)",
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        HBox.setHgrow(txtBatchStartTime, Priority.ALWAYS);
        HBox.setHgrow(txtBatchEndTime, Priority.ALWAYS);
        timeRow.getChildren().addAll(txtBatchStartTime, txtBatchEndTime);
        hourWindowSection.getChildren().add(timeRow);
        HBox batchCapRow = new HBox(16);
        batchCapRow.setAlignment(Pos.CENTER_LEFT);
        stepperBatchCap = new M3Stepper();
        batchCapRow.getChildren().addAll(
                styledLabel("3. Default Slot Capacity:", "label-medium"),
                stepperBatchCap,
                styledLabel("Athletes per 60-min slot", "body-small"));
        lblBatchValidation = new Label("");
        lblBatchValidation.getStyleClass().add("error-message-label");
        HBox publisherActions = new HBox(16);
        publisherActions.setAlignment(Pos.CENTER_RIGHT);
        btnCancelBatch = new M3Button("Cancel", ButtonVariant.OUTLINED);
        btnCancelBatch.setOnAction(e -> onCancelBatch());
        btnConfirmBatchPublish = new M3Button("Publish Slots", ButtonVariant.FILLED);
        btnConfirmBatchPublish.setOnAction(e -> onConfirmBatchPublish());
        publisherActions.getChildren().addAll(btnCancelBatch, btnConfirmBatchPublish);
        publisherContent.getChildren().addAll(
                publisherHeader,
                new Separator(),
                dateRangeSection,
                hourWindowSection,
                batchCapRow,
                lblBatchValidation,
                publisherActions);
        bottomSheetBatchPublish.setContent(publisherContent);

        StackPane root = new StackPane();
        root.getStyleClass().add("trainer-availability-root");
        root.getChildren().addAll(shell, bottomSheetBatchPublish);
        return root;
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
     * The screen previously pinned {@code stepperDailyCap} as an {@link M3Stepper}, but a wizard
     * stepper cannot express a numeric daily cap and renders empty without step titles. Swap in
     * the same slider pattern the per-slot cap already uses (bottom command bar) and retire the
     * stepper.
     */
    private void installDailyCapSlider() {
        dailyCapSlider = M3Slider.discrete(1, 10, 4, 1);
        dailyCapSlider.setShowTickLabels(true);
        dailyCapSlider.setPrefWidth(200);
        if (stepperDailyCap.getParent() instanceof Pane parent) {
            int index = parent.getChildren().indexOf(stepperDailyCap);
            parent.getChildren().set(index, dailyCapSlider);
        }
        stepperDailyCap.setVisible(false);
        stepperDailyCap.setManaged(false);
    }

    /** Same treatment for the batch publisher's default slot capacity. */
    private void installBatchCapSlider() {
        batchCapSlider = M3Slider.discrete(1, 10, 4, 1);
        batchCapSlider.setShowTickLabels(true);
        batchCapSlider.setPrefWidth(200);
        if (stepperBatchCap.getParent() instanceof Pane parent) {
            int index = parent.getChildren().indexOf(stepperBatchCap);
            parent.getChildren().set(index, batchCapSlider);
        }
        stepperBatchCap.setVisible(false);
        stepperBatchCap.setManaged(false);
    }

    /** Turns the coach split button into a working selector over the demo coaching staff. */
    private void wireCoachSelector() {
        M3Menu coachMenu = new M3Menu()
                .item("Elena Rostova (#101)", () -> selectCoach("Elena Rostova (#101)"))
                .item("Marcus Vance (#102)", () -> selectCoach("Marcus Vance (#102)"))
                .item("Kim Park (#103)", () -> selectCoach("Kim Park (#103)"));
        btnCoachSelector.setMenu(coachMenu);
    }

    /** Switches the visible week to the chosen demo coach and confirms with a snackbar. */
    private void selectCoach(String coach) {
        btnCoachSelector.setText(coach);
        feedback.showSnackbar("Showing availability for " + coach + " (demo)");
        refreshWeekView();
    }

    /** Wires the per-slot cap caption, the open/closed switch and the save action. */
    private void wireSlotControls() {
        lblSlotCapValue.setText((int) sliderSlotCap.getValue() + " Athletes");
        sliderSlotCap.valueProperty().addListener((obs, o, v) ->
                lblSlotCapValue.setText(v.intValue() + " Athletes"));
        switchSlotOpen.selectedProperty().addListener((obs, wasOpen, isOpen) ->
                feedback.showSnackbar(isOpen
                        ? "Slot " + currentSlotLabel() + " opened for booking (demo)"
                        : "Slot " + currentSlotLabel() + " closed (demo)"));
    }

    /** The demo slot identity shown in the bottom command bar. */
    private String currentSlotLabel() {
        return lblSelectedSlotTitle.getText() != null
                ? lblSelectedSlotTitle.getText().replace("Selected Slot: ", "")
                : "#SLOT-882";
    }

    /** Fills the annual activity calendar with a deterministic demo booking pattern. */
    private void populateActivityCalendar() {
        LocalDate day = LocalDate.of(calendarGrid.getYear(), 1, 1);
        Random random = new Random(42);
        while (day.getYear() == calendarGrid.getYear()) {
            DayOfWeek dow = day.getDayOfWeek();
            boolean booked = dow != DayOfWeek.SUNDAY && random.nextDouble() > 0.25;
            if (booked) {
                calendarGrid.setCount(day, 2 + random.nextInt(8));
            }
            day = day.plusDays(1);
        }
    }

    /** Re-renders the week header, the density heatmap and the banner stats for weekStart. */
    private void refreshWeekView() {
        int weekNumber = weekStart.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
        lblDateRange.setText(RANGE_START_FMT.format(weekStart) + " - "
                + RANGE_END_FMT.format(weekStart.plusDays(6)) + " (Week " + weekNumber + ")");
        heatmapDensity.setDimensions(7, 16);
        heatmapDensity.setRowLabels(DAY_LABELS);
        Random random = new Random(weekStart.toEpochDay());
        int confirmed = 0;
        int peakRow = 0;
        int peakCol = 0;
        double peakValue = -1;
        for (int day = 0; day < 7; day++) {
            for (int slot = 0; slot < 16; slot++) {
                int hour = slot + 6;
                double intensity;
                if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 20)) {
                    intensity = 0.70 + random.nextDouble() * 0.25;
                } else if (hour >= 12 && hour <= 14) {
                    intensity = 0.40 + random.nextDouble() * 0.20;
                } else if (day >= 5) {
                    intensity = 0.20 + random.nextDouble() * 0.15;
                } else {
                    intensity = 0.10 + random.nextDouble() * 0.15;
                }
                heatmapDensity.setValue(day, slot, intensity);
                confirmed += (int) Math.round(intensity * 5);
                if (intensity > peakValue) {
                    peakValue = intensity;
                    peakRow = day;
                    peakCol = slot;
                }
            }
        }
        int capacity = 7 * 16 * 5;
        double saturation = 100.0 * confirmed / capacity;
        String peak = DAY_LABELS.get(peakRow) + " " + String.format("%02d:00", peakCol + 6);
        lblDensityStats.setText(String.format(
                "Confirmed Sessions: %d / %d Capacity (%.1f%% Saturation) | Peak: %s",
                confirmed, capacity, saturation, peak));
    }

    /** Validates the batch publisher form (Rules 8 & 9) and reports problems inline. */
    private boolean validateBatchForm() {
        LocalDate start = parseIsoDate(txtBatchStartDate);
        LocalDate end = parseIsoDate(txtBatchEndDate);
        LocalTime startTime = parseTime(txtBatchStartTime);
        LocalTime endTime = parseTime(txtBatchEndTime);

        lblBatchValidation.setText("");
        boolean valid = true;
        if (start == null) {
            txtBatchStartDate.setError(true);
            valid = false;
        }
        if (end == null) {
            txtBatchEndDate.setError(true);
            valid = false;
        }
        if (start != null && end != null && end.isBefore(start)) {
            lblBatchValidation.setText("End date must be on or after the start date (Rule 9).");
            valid = false;
        }
        if (startTime == null) {
            txtBatchStartTime.setError(true);
            valid = false;
        }
        if (endTime == null) {
            txtBatchEndTime.setError(true);
            valid = false;
        }
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            lblBatchValidation.setText("End time must be after start time (Rule 8: startTime < endTime).");
            valid = false;
        }
        if (!valid && lblBatchValidation.getText().isEmpty()) {
            lblBatchValidation.setText("Enter dates as yyyy-MM-dd and times as HH:mm.");
        }
        return valid;
    }

    private LocalDate parseIsoDate(M3TextField field) {
        try {
            return LocalDate.parse(field.getText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseTime(M3TextField field) {
        try {
            return LocalTime.parse(field.getText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    public void setCalendar(int trainerId, String date) {
        // UI presentation stub
    }

    public void setSlots(int trainerId, String date, String startTime) {
        // UI action stub
    }

    public void setSlotCap(int availabilityId, int maxCustomers) {
        // UI action stub
    }

    /** Applies the chosen daily cap to every day of the visible week. */
    public void onApplyDailyCap() {
        int cap = dailyCapSlider != null ? (int) dailyCapSlider.getValue() : 4;
        feedback.showSnackbar("Daily cap of " + cap + " athletes applied to " + weekButtonText() + " (demo)");
    }

    /** Opens the batch publisher sheet with prefilled demo dates for the visible week. */
    public void onBatchPublish() {
        txtBatchStartDate.setText(weekStart.toString());
        txtBatchEndDate.setText(weekStart.plusDays(6).toString());
        bottomSheetBatchPublish.setVisible(true);
    }

    /** Saves the per-slot cap and confirms with a snackbar. */
    public void onSaveSlotCap() {
        int cap = sliderSlotCap != null ? (int) sliderSlotCap.getValue() : 4;
        feedback.showSnackbar("Slot cap of " + cap + " athletes saved for " + currentSlotLabel() + " (demo)");
    }

    public void onCloseBottomSheet() {
        bottomSheetBatchPublish.setVisible(false);
    }

    public void onCancelBatch() {
        onCloseBottomSheet();
    }

    /** Publishes the batch after validation and answers with the computed slot count. */
    public void onConfirmBatchPublish() {
        if (!validateBatchForm()) {
            return;
        }
        LocalDate start = parseIsoDate(txtBatchStartDate);
        LocalDate end = parseIsoDate(txtBatchEndDate);
        LocalTime startTime = parseTime(txtBatchStartTime);
        LocalTime endTime = parseTime(txtBatchEndTime);
        long days = start != null && end != null ? ChronoUnit.DAYS.between(start, end) + 1 : 1;
        int hours = startTime != null && endTime != null ? endTime.getHour() - startTime.getHour() : 1;
        int cap = batchCapSlider != null ? (int) batchCapSlider.getValue() : 4;
        long slots = Math.max(1, days * Math.max(1, hours));
        txtBatchStartDate.setError(false);
        txtBatchEndDate.setError(false);
        txtBatchStartTime.setError(false);
        txtBatchEndTime.setError(false);
        bottomSheetBatchPublish.setVisible(false);
        feedback.showSnackbar(slots + " slots published for " + days + " day(s), " + cap
                + " athletes each (demo)");
    }

    public void onPrevWeek() {
        weekStart = weekStart.minusWeeks(1);
        refreshWeekView();
    }

    public void onNextWeek() {
        weekStart = weekStart.plusWeeks(1);
        refreshWeekView();
    }

    public void onToday() {
        weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        refreshWeekView();
    }

    /** Short week label used by the daily-cap snackbar. */
    private String weekButtonText() {
        return "the week of " + RANGE_START_FMT.format(weekStart);
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
}
