package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3ActivityCalendar;
import io.m3fx.controls.chart.M3Heatmap;

import com.gym.dao.SessionDao;
import com.gym.dao.TrainerAvailabilityDao;
import com.gym.dao.TrainerDao;
import com.gym.model.AvailabilityStatus;
import com.gym.model.Session;
import com.gym.model.SessionStatus;
import com.gym.model.Trainer;
import com.gym.model.TrainerAvailability;
import com.gym.ui.Data;
import com.gym.ui.UiKit;

import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.dialog.M3Dialog;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Controller for the Trainer Availability &amp; Slot Capacity Matrix screen.
 * Data contract (DECISIONS.md 77/78, task C8): the coach selector, density heatmap, occupancy
 * line and slot controls run on real {@code TrainerAvailabilityDao} slots and rule-17
 * CONFIRMED occupancy per slot ({@code SessionDao}); the batch publisher really creates the
 * week's slots via {@code TrainerAvailabilityDao.insert} (rule 8: startTime < endTime,
 * maxCustomers > 0 - the DAO owns the validation), the per-slot cap persists via
 * {@code TrainerAvailabilityDao.update}, and the app-bar arrow navigates back to the schedule
 * screen through the owning shell. The activity calendar counts real CONFIRMED sessions.
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
    private Label lblBatchValidation;
    private M3Button btnCancelBatch;
    private M3Button btnConfirmBatchPublish;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Monday of the week shown in the header; refreshed by the navigation buttons. */
    private LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

    /** Real coach roster for the selector (TrainerDao). */
    private final List<Trainer> trainerRoster = new ArrayList<>();
    /** The currently selected coach (-1 before the first snapshot). */
    private int selectedTrainerId = -1;
    /** The week's real slots for the selected coach (TrainerAvailabilityDao). */
    private final List<TrainerAvailability> weekSlots = new ArrayList<>();
    /** CONFIRMED session count per availability id (rule 17 occupancy). */
    private final Map<Integer, Integer> confirmedBySlot = new HashMap<>();
    /** The slot currently selected in the bottom command bar, or null. */
    private TrainerAvailability selectedSlot;
    /** Content host around the workspace: swaps loading/error states around DAO loads. */
    private StackPane contentHost;
    /** The workspace node, restored after loading/error states. */
    private Node workspaceView;
    /** Shown between the banner and the calendar when the week has no published slots. */
    private VBox emptyWeekState;

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
        dailyCapSlider.setId("stepperDailyCap"); // replaces the former wizard stepper
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
        batchCapSlider.setId("stepperBatchCap"); // replaces the former wizard stepper
        lblBatchValidation.setId("lblBatchValidation");
        btnCancelBatch.setId("btnCancelBatch");
        btnConfirmBatchPublish.setId("btnConfirmBatchPublish");
        installFeedbackLayer(topAppBar);
        wireCoachSelector();
        wireSlotControls();
        loadSchedule();
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
        M3IconButton backIcon = new M3IconButton(M3Icon.symbol("arrow_back"),
                IconButtonVariant.STANDARD);
        backIcon.setAccessibleText("Back to the schedule screen");
        backIcon.setOnAction(e -> onNavigateBack());
        topAppBar.setNavigationIcon(backIcon);

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
        btnApplyDailyCap = new M3Button("Apply to Day", ButtonVariant.TONAL);
        btnApplyDailyCap.setOnAction(e -> onApplyDailyCap());
        dailyCapSlider = M3Slider.discrete(1, 10, 4, 1);
        dailyCapSlider.setShowTickLabels(true);
        dailyCapSlider.setPrefWidth(200);
        dailyCapRow.getChildren().addAll(
                styledLabel("Daily Cap:", "label-medium"),
                dailyCapSlider,
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

        // Main workspace. The scroll wraps the CONTENT HOST (not the workspace itself): the
        // load/empty/error swap in Data.loadAsync drains and refills the host's child list, so
        // the host — not the workspace — is the node that must sit in the scene (wrapping the
        // workspace directly left the visible pane empty while loads ran).
        VBox workspace = new VBox(16);
        workspace.setPadding(new Insets(20, 20, 64, 20));
        workspaceView = workspace;
        contentHost = new StackPane(workspace);

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

        // Week slot empty state (shown between the banner and the calendar when the selected
        // coach has no published slots for the rendered week; the batch publisher is the
        // designed answer, so the action opens it prefilled).
        emptyWeekState = UiKit.emptyState("No slots published for this week.",
                "+ Batch Publish Slots", this::onBatchPublish);
        emptyWeekState.getStyleClass().add("week-empty-state");
        emptyWeekState.setVisible(false);
        emptyWeekState.setManaged(false);

        workspace.getChildren().addAll(cardDensityBanner, emptyWeekState, calendarSection);
        ScrollPane workspaceScroll = new ScrollPane(contentHost);
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
        lblSelectedSlotTitle = new Label("Selected Slot: none - use the slot picker");
        lblSelectedSlotTitle.getStyleClass().add("title-small");
        lblSlotOccupancy = new Label("Occupancy: -");
        lblSlotOccupancy.getStyleClass().add("body-small");
        slotIdentity.getChildren().addAll(lblSelectedSlotTitle, lblSlotOccupancy);
        HBox openSwitchRow = new HBox(12);
        openSwitchRow.setAlignment(Pos.CENTER_LEFT);
        switchSlotOpen = new M3Switch(true);
        switchSlotOpen.selectedProperty().addListener((obs, wasOpen, isOpen) ->
                onToggleSlotOpen(isOpen));
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
        HBox editorRow = new HBox(12);
        editorRow.setAlignment(Pos.CENTER_LEFT);
        M3Button slotPickerBtn = new M3Button("Pick Slot", ButtonVariant.TONAL);
        slotPickerBtn.getStyleClass().add("m3-button-tonal");
        slotPickerBtn.setOnAction(e -> openSlotPickerMenu());
        M3Button deleteSlotBtn = new M3Button("Delete Slot", ButtonVariant.TEXT);
        deleteSlotBtn.getStyleClass().add("m3-button-text-danger");
        deleteSlotBtn.setOnAction(e -> onDeleteSlot());
        editorRow.getChildren().addAll(slotPickerBtn, deleteSlotBtn);
        commandFlow.getChildren().addAll(
                slotIdentity,
                new Separator(Orientation.VERTICAL),
                openSwitchRow,
                new Separator(Orientation.VERTICAL),
                capRow,
                new Separator(Orientation.VERTICAL),
                editorRow);
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
        batchCapSlider = M3Slider.discrete(1, 10, 4, 1);
        batchCapSlider.setShowTickLabels(true);
        batchCapSlider.setPrefWidth(200);
        batchCapRow.getChildren().addAll(
                styledLabel("3. Default Slot Capacity:", "label-medium"),
                batchCapSlider,
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
    /** Turns the coach split button into a working selector over the real coaching staff. */
    private void wireCoachSelector() {
        M3Menu coachMenu = new M3Menu();
        if (trainerRoster.isEmpty()) {
            coachMenu.item("No coaches in the database yet", () -> { });
        }
        for (Trainer trainer : trainerRoster) {
            long id = trainer.getId();
            coachMenu.item(trainer.getFullName() + " (#" + trainer.getId() + ")",
                    () -> selectCoach((int) id));
        }
        btnCoachSelector.setMenu(coachMenu);
    }

    /** Switches the visible week to the chosen real coach and reloads the slots. */
    private void selectCoach(int trainerId) {
        selectedTrainerId = trainerId;
        for (Trainer trainer : trainerRoster) {
            if (trainer.getId() == trainerId) {
                btnCoachSelector.setText(trainer.getFullName() + " (#" + trainerId + ")");
            }
        }
        loadSchedule();
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

    /**
     * Loads the selected coach's real slots for the rendered week on a background task:
     * {@code TrainerAvailabilityDao} rows plus rule-17 CONFIRMED occupancy per slot
     * ({@code SessionDao}). Shows loading/error states around the workspace.
     */
    private void loadSchedule() {
        Data.loadAsync(contentHost,
            () -> {
                Data.ensureReady();
                LocalDate weekEnd = weekStart.plusDays(6);
                List<TrainerAvailability> slots = new ArrayList<>();
                Map<Integer, Integer> confirmed = new HashMap<>();
                if (selectedTrainerId > 0) {
                    for (TrainerAvailability slot :
                            new TrainerAvailabilityDao().findByTrainerId(selectedTrainerId)) {
                        if (slot.getDate() == null || slot.getDate().isBlank()) {
                            continue;
                        }
                        LocalDate date = LocalDate.parse(slot.getDate());
                        if (date.isBefore(weekStart) || date.isAfter(weekEnd)) {
                            continue;
                        }
                        slots.add(slot);
                    }
                }
                for (Session session : new SessionDao().findAll()) {
                    if (session.getStatus() == SessionStatus.CONFIRMED
                            && session.getAvailabilityId() != null) {
                        confirmed.merge(session.getAvailabilityId(), 1, Integer::sum);
                    }
                }
                List<Trainer> trainers = new TrainerDao().findAll();
                return new Object[] {slots, confirmed, trainers};
            },
            read -> {
                contentHost.getChildren().set(0, workspaceView);
                weekSlots.clear();
                @SuppressWarnings("unchecked")
                List<TrainerAvailability> slots = (List<TrainerAvailability>) read[0];
                weekSlots.addAll(slots);
                confirmedBySlot.clear();
                confirmedBySlot.putAll((Map<Integer, Integer>) read[1]);
                trainerRoster.clear();
                trainerRoster.addAll((List<Trainer>) read[2]);
                if (selectedTrainerId <= 0 && !trainerRoster.isEmpty()) {
                    selectedTrainerId = trainerRoster.get(0).getId();
                }
                for (Trainer trainer : trainerRoster) {
                    if (trainer.getId() == selectedTrainerId) {
                        btnCoachSelector.setText(trainer.getFullName() + " (#"
                                + trainer.getId() + ")");
                    }
                }
                selectedSlot = null;
                lblSelectedSlotTitle.setText("Selected Slot: none - pick one below");
                lblSlotOccupancy.setText("Occupancy: -");
                wireCoachSelector();
                refreshWeekView();
            },
            error -> contentHost.getChildren().setAll(UiKit.errorState(
                    error.getMessage() == null ? error.toString() : error.getMessage(),
                    this::loadSchedule)));
    }

    /** Rule-17 CONFIRMED occupancy of one slot. */
    private int confirmedInSlot(int availabilityId) {
        return confirmedBySlot.getOrDefault(availabilityId, 0);
    }

    /** The real slot list of the rendered week, sorted by date and start time. */
    private List<TrainerAvailability> sortedWeekSlots() {
        List<TrainerAvailability> sorted = new ArrayList<>(weekSlots);
        sorted.sort((a, b) -> {
            int byDate = a.getDate().compareTo(b.getDate());
            return byDate != 0 ? byDate : a.getStartTime().compareTo(b.getStartTime());
        });
        return sorted;
    }

    /** Fills the annual activity calendar from the real CONFIRMED sessions of the year. */
    private void populateActivityCalendar() {
        LocalDate day = LocalDate.of(calendarGrid.getYear(), 1, 1);
        Map<LocalDate, Integer> confirmedByDate = new HashMap<>();
        try {
            for (Session session : new SessionDao().findAll()) {
                if (session.getStatus() == SessionStatus.CONFIRMED
                        && session.getDate() != null && !session.getDate().isBlank()) {
                    confirmedByDate.merge(LocalDate.parse(session.getDate()), 1, Integer::sum);
                }
            }
        } catch (com.gym.exception.DatabaseException e) {
            // A failed session read leaves the calendar empty rather than inventing bookings.
            return;
        }
        while (day.getYear() == calendarGrid.getYear()) {
            int count = confirmedByDate.getOrDefault(day, 0);
            if (count > 0) {
                calendarGrid.setCount(day, Math.min(9, count));
            }
            day = day.plusDays(1);
        }
    }

    /** Re-renders the week header, the density heatmap and the banner stats for weekStart.
     *  The heatmap intensity is the REAL rule-17 CONFIRMED occupancy per slot hour (0 when a
     *  slot has no cap or no session). */
    private void refreshWeekView() {
        int weekNumber = weekStart.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
        lblDateRange.setText(RANGE_START_FMT.format(weekStart) + " - "
                + RANGE_END_FMT.format(weekStart.plusDays(6)) + " (Week " + weekNumber + ")");
        heatmapDensity.setDimensions(7, 16);
        heatmapDensity.setRowLabels(DAY_LABELS);
        int confirmed = 0;
        int capacity = 0;
        int peakRow = 0;
        int peakCol = 0;
        double peakValue = -1;
        for (TrainerAvailability slot : weekSlots) {
            LocalDate date = LocalDate.parse(slot.getDate());
            int day = date.getDayOfWeek().getValue() - 1;
            int hour;
            try {
                hour = Integer.parseInt(slot.getStartTime().substring(0, 2));
            } catch (RuntimeException e) {
                continue;
            }
            if (day < 0 || day > 6 || hour < 6 || hour > 21) {
                continue;
            }
            int booked = confirmedBySlot.getOrDefault(slot.getId(), 0);
            if (slot.getStatus() != AvailabilityStatus.CLOSED) {
                capacity += slot.getMaxCustomers();
                confirmed += Math.min(booked, slot.getMaxCustomers());
            }
            double intensity = slot.getMaxCustomers() == 0 ? 0
                    : Math.min(1.0, booked * 1.0 / slot.getMaxCustomers());
            heatmapDensity.setValue(day, hour - 6, intensity);
            if (intensity > peakValue) {
                peakValue = intensity;
                peakRow = day;
                peakCol = hour - 6;
            }
        }
        double saturation = capacity == 0 ? 0 : 100.0 * confirmed / capacity;
        String peak = peakValue < 0 ? "none" : DAY_LABELS.get(peakRow) + " "
                + String.format("%02d:00", peakCol + 6);
        lblDensityStats.setText(String.format(
                "Confirmed Sessions: %d / %d Capacity (%.1f%% Saturation) | Peak: %s",
                confirmed, capacity, saturation, peak));
        emptyWeekState.setVisible(weekSlots.isEmpty());
        emptyWeekState.setManaged(weekSlots.isEmpty());
        refreshSlotBar();
    }

    /** Refreshes the bottom command bar against the currently selected real slot. */
    private void refreshSlotBar() {
        if (selectedSlot == null) {
            lblSelectedSlotTitle.setText("Selected Slot: none - pick one from the list");
            lblSlotOccupancy.setText("Occupancy: -");
            return;
        }
        int booked = confirmedInSlot(selectedSlot.getId());
        lblSelectedSlotTitle.setText("Selected Slot: " + selectedSlot.getDate()
                + " " + selectedSlot.getStartTime() + "-"
                + selectedSlot.getEndTime() + " (#" + selectedSlot.getId() + ")");
        lblSlotOccupancy.setText("Occupancy: " + booked + " / "
                + selectedSlot.getMaxCustomers() + " CONFIRMED (rule 17)");
        sliderSlotCap.setValue(Math.max(sliderSlotCap.getMin(),
                Math.min(sliderSlotCap.getMax(), selectedSlot.getMaxCustomers())));
        lblSlotCapValue.setText(selectedSlot.getMaxCustomers() + " Athletes");
        switchSlotOpen.setSelected(selectedSlot.getStatus() != AvailabilityStatus.CLOSED);
    }

    /** Selects the next real slot in date/time order (used by the slot navigation). */
    private void selectFirstSlot() {
        List<TrainerAvailability> sorted = sortedWeekSlots();
        selectedSlot = sorted.isEmpty() ? null : sorted.get(0);
        refreshSlotBar();
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

    /**
     * Applies the slider cap to every slot of the selected coach on the selected slot's day
     * (a real per-day rule-8 cap batch via {@code TrainerAvailabilityDao.update}).
     */
    public void onApplyDailyCap() {
        if (selectedTrainerId <= 0) {
            feedback.showSnackbar("Select a coach first.");
            return;
        }
        LocalDate day = selectedSlot != null ? LocalDate.parse(selectedSlot.getDate())
                : LocalDate.now();
        int cap = dailyCapSlider != null ? (int) dailyCapSlider.getValue() : 4;
        Data.submit(
                () -> {
                    int updated = 0;
                    for (TrainerAvailability slot : sortedWeekSlots()) {
                        if (day.toString().equals(slot.getDate())) {
                            slot.setMaxCustomers(cap);
                            new TrainerAvailabilityDao().update(slot);
                            updated++;
                        }
                    }
                    return updated;
                },
                updated -> {
                    feedback.showSnackbar("Cap of " + cap + " athletes applied to " + updated
                            + " slots on " + day + " (rule 8).");
                    loadSchedule();
                },
                error -> feedback.showSnackbar("Could not apply the cap: " + error.getMessage()));
    }

    /** Opens the batch publisher sheet prefilled for the visible week. */
    public void onBatchPublish() {
        txtBatchStartDate.setText(weekStart.toString());
        txtBatchEndDate.setText(weekStart.plusDays(6).toString());
        bottomSheetBatchPublish.setVisible(true);
    }

    /**
     * Saves the per-slot cap through {@code TrainerAvailabilityDao.update} (rule 8:
     * maxCustomers > 0 is the DAO's floor). Requires a selected real slot.
     */
    public void onSaveSlotCap() {
        if (selectedSlot == null) {
            feedback.showSnackbar("Pick a slot first - the cap applies to a real slot row.");
            return;
        }
        int cap = sliderSlotCap != null ? (int) sliderSlotCap.getValue() : 4;
        int slotId = selectedSlot.getId();
        Data.submit(
                () -> {
                    TrainerAvailability current = new TrainerAvailabilityDao().findById(slotId);
                    if (current == null) {
                        throw new com.gym.exception.ValidationException(
                                "Slot no longer exists in the database");
                    }
                    current.setMaxCustomers(cap);
                    new TrainerAvailabilityDao().update(current);
                    return current;
                },
                saved -> {
                    feedback.showSnackbar("Slot cap of " + cap + " athletes saved for "
                            + saved.getDate() + " " + saved.getStartTime() + " (rule 8).");
                    loadSchedule();
                },
                error -> feedback.showSnackbar("Could not save the cap: " + error.getMessage()));
    }

    public void onCloseBottomSheet() {
        bottomSheetBatchPublish.setVisible(false);
    }

    public void onCancelBatch() {
        onCloseBottomSheet();
    }

    /**
     * Rule 8 batch publish: REALLY creates the slots for every day in the range, one
     * TrainerAvailabilityDao.insert per hour between start and end with the chosen cap (the
     * DAO validates start < end and maxCustomers > 0). Requires a selected coach.
     */
    public void onConfirmBatchPublish() {
        if (selectedTrainerId <= 0) {
            feedback.showSnackbar("Select a coach first - slots belong to a coach (rule 8).");
            return;
        }
        if (!validateBatchForm()) {
            return;
        }
        LocalDate start = parseIsoDate(txtBatchStartDate);
        LocalDate end = parseIsoDate(txtBatchEndDate);
        LocalTime startTime = parseTime(txtBatchStartTime);
        LocalTime endTime = parseTime(txtBatchEndTime);
        int cap = batchCapSlider != null ? (int) batchCapSlider.getValue() : 4;
        txtBatchStartDate.setError(false);
        txtBatchEndDate.setError(false);
        txtBatchStartTime.setError(false);
        txtBatchEndTime.setError(false);
        Data.submit(
                () -> {
                    int created = 0;
                    LocalDate day = start;
                    while (!day.isAfter(end)) {
                        LocalTime time = startTime;
                        while (time.isBefore(endTime)) {
                            TrainerAvailability slot = new TrainerAvailability(0,
                                    selectedTrainerId, day.toString(), time.toString(),
                                    time.plusHours(1).toString(), cap,
                                    AvailabilityStatus.OPEN);
                            new TrainerAvailabilityDao().insert(slot);
                            created++;
                            time = time.plusHours(1);
                        }
                        day = day.plusDays(1);
                    }
                    return created;
                },
                created -> {
                    bottomSheetBatchPublish.setVisible(false);
                    feedback.showSnackbar(created + " slots published for coach #"
                            + selectedTrainerId + " (rule 8).");
                    loadSchedule();
                },
                error -> {
                    feedback.showSnackbar("Could not publish slots: " + error.getMessage());
                    loadSchedule();
                });
    }

    /** Opens the slot picker over the selected coach's real slots for the rendered week. */
    private void openSlotPickerMenu() {
        M3Menu menu = new M3Menu();
        List<TrainerAvailability> sorted = sortedWeekSlots();
        if (sorted.isEmpty()) {
            menu.item("No slots published for this coach this week - use Batch Publish", () -> { });
        }
        for (TrainerAvailability slot : sorted) {
            int booked = confirmedInSlot(slot.getId());
            menu.item(slot.getDate() + " " + slot.getStartTime() + "-" + slot.getEndTime()
                    + " · " + booked + "/" + slot.getMaxCustomers() + " · " + slot.getStatus(),
                    () -> {
                        selectedSlot = slot;
                        refreshSlotBar();
                    });
        }
        showMenuBelow(lblSelectedSlotTitle, menu, "The slot picker opens in the app window.");
    }

    /** Rule 17 guard + rule 8 delete: refuses while CONFIRMED sessions occupy the slot. */
    private void onDeleteSlot() {
        if (selectedSlot == null) {
            feedback.showSnackbar("Pick a slot first.");
            return;
        }
        if (confirmedInSlot(selectedSlot.getId()) > 0) {
            feedback.showSnackbar("Rule 17: the slot still has CONFIRMED sessions - confirm or"
                    + " cancel them before deleting the slot.");
            return;
        }
        M3Button keep = M3Button.text("Keep Slot");
        M3Button delete = M3Button.text("Delete Slot");
        M3Dialog dialog = M3Dialog.basic("Delete this slot?",
                new Label("Slot #" + selectedSlot.getId() + " ("
                        + selectedSlot.getDate() + " " + selectedSlot.getStartTime() + ") is"
                        + " removed from the schedule."),
                keep, delete);
        dialog.setDefaultAction(delete::activate);
        keep.setOnAction(e -> dialog.hide());
        delete.setOnAction(e -> {
            dialog.hide();
            int slotId = selectedSlot.getId();
            Data.submit(
                    () -> {
                        new TrainerAvailabilityDao().delete(slotId);
                        return slotId;
                    },
                    deleted -> {
                        feedback.showSnackbar("Slot #" + deleted + " deleted.");
                        selectedSlot = null;
                        loadSchedule();
                    },
                    error -> feedback.showSnackbar("Could not delete the slot: "
                            + error.getMessage()));
        });
        feedback.showDialog(dialog);
    }

    /** Rule 8 open/close: persists the slot status through {@code TrainerAvailabilityDao.update}. */
    private void onToggleSlotOpen(boolean isOpen) {
        if (selectedSlot == null) {
            feedback.showSnackbar("Pick a slot first - the status belongs to a real slot.");
            refreshSlotBar();
            return;
        }
        int slotId = selectedSlot.getId();
        Data.submit(
                () -> {
                    TrainerAvailability current = new TrainerAvailabilityDao().findById(slotId);
                    if (current == null) {
                        throw new com.gym.exception.ValidationException(
                                "Slot no longer exists in the database");
                    }
                    current.setStatus(isOpen ? AvailabilityStatus.OPEN : AvailabilityStatus.CLOSED);
                    new TrainerAvailabilityDao().update(current);
                    return current;
                },
                saved -> feedback.showSnackbar("Slot #" + saved.getId() + " is now "
                        + saved.getStatus() + "."),
                error -> {
                    feedback.showSnackbar("Could not update the slot status: " + error.getMessage());
                    refreshSlotBar();
                });
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
     * The app-bar arrow navigates back to the schedule screen through the owning shell (the
     * same openScreenViaRail pathway as the roster handoff); standalone previews answer with a
     * snackbar.
     */
    public void onNavigateBack() {
        if (viewRoot.getScene() == null
                || !(viewRoot.getScene().getRoot() instanceof javafx.scene.Parent sceneRoot)) {
            feedback.showSnackbar("Back navigation opens in the app window.");
            return;
        }
        Object shell = sceneRoot.getProperties().get(AppShellController.SHELL_CONTROLLER_KEY);
        if (shell instanceof AppShellController controller) {
            controller.openScreenViaRail("navSchedule", Screen.SCHEDULE, null);
            return;
        }
        feedback.showSnackbar("Back navigation opens in the app window.");
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
