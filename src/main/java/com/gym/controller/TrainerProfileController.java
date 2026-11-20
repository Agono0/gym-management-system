package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3GaugeChart;
import io.m3fx.controls.chart.M3DataPoint;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.chart.M3RadarChart;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.tabs.M3Tabs;
import io.m3fx.controls.textfield.M3TextField;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import com.gym.dao.CustomerDao;
import com.gym.dao.MedicalProfileDao;
import com.gym.dao.MembershipDao;
import com.gym.dao.SessionDao;
import com.gym.dao.TrainerAvailabilityDao;
import com.gym.dao.TrainerDao;
import com.gym.dao.TrainerSubscriptionDao;
import com.gym.dao.WorkoutProgramDao;
import com.gym.model.Customer;
import com.gym.model.MedicalProfile;
import com.gym.model.MembershipStatus;
import com.gym.model.Session;
import com.gym.model.SessionStatus;
import com.gym.model.Trainer;
import com.gym.model.TrainerAvailability;
import com.gym.model.TrainerSubscription;
import com.gym.model.WorkoutProgram;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Trainer Profile screen.
 *
 * <p>Data contract (DECISIONS.md 77/78, task C8): the dossier follows the SELECTED coach —
 * {@code showTrainer} (the roster handoff) loads the coach's real data on a background task:
 * rule-15 assigned athletes (CustomerDao trainerId), the rule-11 subscriptions, the latest
 * programs (WorkoutProgramDao), the weekly CONFIRMED sessions (rule 17), the rule-8 capacity
 * (athletes vs the sum of the coach's slot caps) and the per-athlete medical profiles
 * (rule 15 access) for the clinical sheet. Save persists the hourly rate through
 * {@code TrainerDao.update}. The capacity gauge is COMPUTED — never a hardcoded percent.
 * Opened directly (no handoff) the screen shows the honest empty state.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former trainer-profile.fxml: glass hero banner, three navigation tabs, command footer,
 * clinical side sheet.
 */
public class TrainerProfileController implements ScreenController {

    /**
     * Immutable per-coach payload handed over from the trainer roster (static-free passing, no
     * backend): exactly the fields the roster row carries — id, name, specialization, years of
     * experience, hourly rate, assigned athletes, saturation percent and email.
     */
    public record TrainerSnapshot(int trainerId, String name, String specialization, int yearsExperience,
                                  double hourlyRate, int assignedAthletes, int saturationPercent, String email) {
    }

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private M3TopAppBar topAppBar;
    private M3GlassPane heroGlassPane;
    private Label lblCoachName;
    private Label lblSpecialization;
    private Label lblEmail;
    private Label lblPhone;
    private Label lblExperience;
    private M3TextField txtHourlyRate;
    private M3GaugeChart gaugeCapacity;
    private Label lblCapacityText;
    private M3Tabs profileTabs;
    private Tab tabOverview;
    private M3Card cardRadar;
    private M3RadarChart chartCompetency;
    private M3Card cardScheduleDensity;
    private M3Heatmap heatmapWeeklySchedule;
    private Tab tabAthletes;
    private M3DataTable<String> tableAssignedAthletes;
    private TableColumn<String, ?> colAthleteId;
    private TableColumn<String, ?> colAthleteName;
    private TableColumn<String, ?> colAthleteStatus;
    private TableColumn<String, ?> colAthleteSubscription;
    private TableColumn<String, ?> colAthleteProgram;
    private TableColumn<String, ?> colAthleteMedicalAction;
    private Tab tabCertifications;
    private M3Card cardTimeline;
    private M3Timeline timelineCertifications;
    private Label lblValidationStatus;
    private M3Button btnCancel;
    private M3Button btnSave;
    private M3SideSheet sideSheetMedicalProfile;
    private M3IconButton btnCloseMedicalFlyout;
    private Label lblMedAthleteName;
    private Label lblMedBloodType;
    private Label lblMedDoctor;
    private M3AnatomyViewer3D anatomyViewer;
    private TextArea txtConditions;
    private TextArea txtAllergies;
    private M3Button btnPrintMedical;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Content host around the workspace: swaps loading/error states around DAO loads. */
    private StackPane contentHost;
    /** The workspace node, restored after loading/error states. */
    private Node workspaceView;
    /** The coach currently loaded from the database, or null before the first handoff. */
    private Trainer loadedTrainer;
    /** Trainer id of the currently rendered coach (-1 before the first handoff). */
    private int currentTrainerId = -1;
    /** The cohort tab's header label, updated with the real rule-15 count. */
    private Label cohortHeaderLabel;

    /**
     * Builds the complete Trainer Dossier view and initializes all behavior. Same ordering
     * semantics the FXMLLoader used to provide: the node tree exists and every field is
     * assigned before the former {@code initialize()} work runs.
     */
    public TrainerProfileController() {
        // Column declarations wired onto tableAssignedAthletes in wireCohortColumns()
        // (M3DataTable.Column is not declarable in markup).
        colAthleteId = column("ID", 70, true);
        colAthleteName = column("Athlete Full Name", 200, true);
        colAthleteStatus = column("Status", 110, true);
        colAthleteSubscription = column("Trainer Subscription", 160, true);
        colAthleteProgram = column("Active Program", 200, true);
        colAthleteMedicalAction = column("Clinical Records", 160, false);

        viewRoot = buildView();

        installFeedbackLayer(topAppBar);
        wireCohortColumns();

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        topAppBar.setId("topAppBar");
        heroGlassPane.setId("heroGlassPane");
        lblCoachName.setId("lblCoachName");
        lblSpecialization.setId("lblSpecialization");
        lblEmail.setId("lblEmail");
        lblPhone.setId("lblPhone");
        lblExperience.setId("lblExperience");
        txtHourlyRate.setId("txtHourlyRate");
        gaugeCapacity.setId("gaugeCapacity");
        lblCapacityText.setId("lblCapacityText");
        profileTabs.setId("profileTabs");
        tabOverview.setId("tabOverview");
        cardRadar.setId("cardRadar");
        chartCompetency.setId("chartCompetency");
        cardScheduleDensity.setId("cardScheduleDensity");
        heatmapWeeklySchedule.setId("heatmapWeeklySchedule");
        tabAthletes.setId("tabAthletes");
        tableAssignedAthletes.setId("tableAssignedAthletes");
        colAthleteId.setId("colAthleteId");
        colAthleteName.setId("colAthleteName");
        colAthleteStatus.setId("colAthleteStatus");
        colAthleteSubscription.setId("colAthleteSubscription");
        colAthleteProgram.setId("colAthleteProgram");
        colAthleteMedicalAction.setId("colAthleteMedicalAction");
        tabCertifications.setId("tabCertifications");
        cardTimeline.setId("cardTimeline");
        timelineCertifications.setId("timelineCertifications");
        lblValidationStatus.setId("lblValidationStatus");
        btnCancel.setId("btnCancel");
        btnSave.setId("btnSave");
        sideSheetMedicalProfile.setId("sideSheetMedicalProfile");
        btnCloseMedicalFlyout.setId("btnCloseMedicalFlyout");
        lblMedAthleteName.setId("lblMedAthleteName");
        lblMedBloodType.setId("lblMedBloodType");
        lblMedDoctor.setId("lblMedDoctor");
        anatomyViewer.setId("anatomyViewer");
        txtConditions.setId("txtConditions");
        txtAllergies.setId("txtAllergies");
        btnPrintMedical.setId("btnPrintMedical");
    }

    /** One cohort table column definition (header text + preferred width). */
    private static TableColumn<String, ?> column(String text, double prefWidth, boolean sortable) {
        TableColumn<String, ?> col = new TableColumn<>(text);
        col.setPrefWidth(prefWidth);
        col.setSortable(sortable);
        return col;
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for trainer-profile.fxml)
     * and wires every handler.
     */
    private StackPane buildView() {
        // Primary layout container.
        VBox shell = new VBox();
        shell.setSpacing(0);

        // Top app bar with back navigation.
        topAppBar = new M3TopAppBar("Trainer Dossier :: Elena Rostova",
                io.m3fx.controls.appbar.TopAppBarVariant.SMALL);
        topAppBar.setNavigationIcon(M3Icon.symbol("arrow_back"));

        VBox workspace = new VBox(20);
        workspace.setPadding(new Insets(24, 24, 64, 24));

        // Hero identity bento banner (glass pane) — name, contact and metric zones reflow
        // at ~804px content width instead of clipping.
        heroGlassPane = new M3GlassPane();
        heroGlassPane.getStyleClass().add("hero-dossier-card");
        HBox heroContent = new HBox(24);
        heroContent.setAlignment(Pos.CENTER_LEFT);
        heroContent.setPadding(new Insets(20));

        StackPane avatarPane = new StackPane();
        avatarPane.getStyleClass().add("avatar-container");
        avatarPane.setPrefSize(100, 100);
        avatarPane.setMinSize(100, 100);
        avatarPane.getChildren().add(styledLabel("ER", "avatar-initials"));

        VBox identityBlock = new VBox(6);
        HBox.setHgrow(identityBlock, Priority.ALWAYS);
        FlowPane nameFlow = new FlowPane(12, 4);
        nameFlow.setAlignment(Pos.CENTER_LEFT);
        lblCoachName = new Label("Elena Rostova, CSCS, USAW-L2");
        lblCoachName.getStyleClass().add("headline-medium");
        lblCoachName.setWrapText(true);
        nameFlow.getChildren().addAll(lblCoachName, styledLabel("ACTIVE STAFF", "status-badge-active"));
        lblSpecialization = new Label(
                "Specialization: Olympic Weightlifting, Power Development, Biomechanics");
        lblSpecialization.getStyleClass().add("body-large");
        lblSpecialization.setWrapText(true);
        FlowPane contactFlow = new FlowPane(16, 4);
        contactFlow.setAlignment(Pos.CENTER_LEFT);
        lblEmail = new Label("e.rostova@cybercoregym.io");
        lblEmail.getStyleClass().add("body-medium");
        lblPhone = new Label("01012345678");
        lblPhone.getStyleClass().add("body-medium");
        lblExperience = new Label("Experience: 8 Years");
        lblExperience.getStyleClass().add("body-medium");
        contactFlow.getChildren().addAll(lblEmail, lblPhone, lblExperience);
        identityBlock.getChildren().addAll(nameFlow, lblSpecialization, contactFlow);

        VBox metricsBlock = new VBox(8);
        metricsBlock.setAlignment(Pos.CENTER_RIGHT);
        txtHourlyRate = new M3TextField("Hourly Rate ($)",
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        txtHourlyRate.setSupportingText("USD per session hour");
        txtHourlyRate.setPrefWidth(120);
        gaugeCapacity = new M3GaugeChart();
        gaugeCapacity.setPrefSize(160, 160);
        lblCapacityText = new Label("Saturation: 23 / 25 Athletes (92%)");
        lblCapacityText.getStyleClass().add("label-small");
        metricsBlock.getChildren().addAll(txtHourlyRate, gaugeCapacity, lblCapacityText);

        heroContent.getChildren().addAll(avatarPane, identityBlock, metricsBlock);
        heroGlassPane.setContent(heroContent);

        // Navigation tabs.
        tabOverview = new Tab("Overview & Biometrics");
        tabOverview.setClosable(false);
        tabOverview.setContent(overviewTabContent());
        tabAthletes = new Tab("Assigned Athletes");
        tabAthletes.setClosable(false);
        tabAthletes.setContent(athletesTabContent());
        tabCertifications = new Tab("Certifications & Career");
        tabCertifications.setClosable(false);
        tabCertifications.setContent(certificationsTabContent());
        profileTabs = new M3Tabs();
        profileTabs.getTabs().addAll(tabOverview, tabAthletes, tabCertifications);

        // Command footer bar (wraps instead of clipping on narrow windows).
        M3GlassPane footerBar = new M3GlassPane();
        footerBar.getStyleClass().add("footer-command-bar");
        FlowPane footerFlow = new FlowPane(16, 8);
        footerFlow.setAlignment(Pos.CENTER_RIGHT);
        footerFlow.setPadding(new Insets(16));
        lblValidationStatus = new Label("All trainer credentials and rates verified.");
        lblValidationStatus.getStyleClass().add("body-small");
        btnCancel = new M3Button("Cancel", ButtonVariant.OUTLINED);
        btnCancel.setOnAction(e -> onCancel());
        btnSave = new M3Button("Save Profile Changes", ButtonVariant.FILLED);
        btnSave.setOnAction(e -> onSave());
        footerFlow.getChildren().addAll(lblValidationStatus, btnCancel, btnSave);
        footerBar.setContent(footerFlow);

        workspace.getChildren().addAll(heroGlassPane, profileTabs, footerBar);
        ScrollPane workspaceScroll = new ScrollPane(workspace);
        workspaceScroll.setFitToWidth(true);
        workspaceScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        workspaceScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(workspaceScroll, Priority.ALWAYS);

        // Content host so the real-data load can swap the dossier for loading/error states.
        workspaceView = workspaceScroll;
        contentHost = new StackPane(workspaceScroll);
        shell.getChildren().addAll(topAppBar, contentHost);

        // Modal side sheet for the medical profile (Rule 3 & 15 compliance).
        sideSheetMedicalProfile = new M3SideSheet();
        sideSheetMedicalProfile.setVisible(false);
        StackPane.setAlignment(sideSheetMedicalProfile, Pos.CENTER_RIGHT);
        VBox medicalContent = new VBox(16);
        medicalContent.setPrefWidth(420);
        medicalContent.getStyleClass().add("medical-drawer-content");
        medicalContent.setPadding(new Insets(24, 24, 64, 24));

        HBox medicalHeader = new HBox();
        medicalHeader.setAlignment(Pos.CENTER_LEFT);
        btnCloseMedicalFlyout = new M3IconButton(M3Icon.symbol("close"), IconButtonVariant.STANDARD);
        btnCloseMedicalFlyout.setAccessibleText("Close clinical dossier");
        btnCloseMedicalFlyout.setOnAction(e -> onCloseMedical());
        Region medicalSpacer = new Region();
        HBox.setHgrow(medicalSpacer, Priority.ALWAYS);
        medicalHeader.getChildren().addAll(
                styledLabel("Athlete Clinical Dossier", "headline-small"),
                medicalSpacer,
                btnCloseMedicalFlyout);

        VBox athleteIdentity = new VBox(8);
        lblMedAthleteName = new Label("Athlete: Maya Lin (#312)");
        lblMedAthleteName.getStyleClass().add("title-medium");
        lblMedBloodType = new Label("Blood Type: O+");
        lblMedBloodType.getStyleClass().add("body-medium");
        lblMedDoctor = new Label("Physician: Dr. Robert Chen (555-0199)");
        lblMedDoctor.getStyleClass().add("body-medium");
        athleteIdentity.getChildren().addAll(lblMedAthleteName, lblMedBloodType, lblMedDoctor);

        M3Card anatomyCard = new M3Card();
        anatomyCard.setPrefHeight(220);
        VBox anatomyContent = new VBox(8);
        anatomyContent.getChildren().addAll(
                styledLabel("3D Musculoskeletal Injury Map", "title-medium"),
                anatomyViewer());
        anatomyCard.setContent(anatomyContent);

        VBox conditionsBlock = new VBox(6);
        conditionsBlock.getChildren().addAll(
                styledLabel("Restrictive Conditions & Injuries:", "label-medium"),
                conditionsArea(80));
        VBox allergiesBlock = new VBox(6);
        allergiesBlock.getChildren().addAll(
                styledLabel("Allergies & Medications:", "label-medium"),
                allergiesArea(60));

        btnPrintMedical = new M3Button("Print Clinical Sheet", ButtonVariant.TONAL);
        btnPrintMedical.setOnAction(e -> onPrintMedical());
        Region medicalBodySpacer = new Region();
        VBox.setVgrow(medicalBodySpacer, Priority.ALWAYS);

        medicalContent.getChildren().addAll(
                medicalHeader,
                new Separator(),
                athleteIdentity,
                anatomyCard,
                conditionsBlock,
                allergiesBlock,
                medicalBodySpacer,
                btnPrintMedical);
        sideSheetMedicalProfile.setContent(medicalContent);

        StackPane root = new StackPane();
        root.getStyleClass().add("trainer-profile-root");
        root.getChildren().addAll(shell, sideSheetMedicalProfile);
        return root;
    }

    /** The Overview & Biometrics tab content. */
    private VBox overviewTabContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(16, 0, 0, 0));
        HBox chartsRow = new HBox(20);
        cardRadar = new M3Card();
        HBox.setHgrow(cardRadar, Priority.ALWAYS);
        VBox radarContent = new VBox(8);
        radarContent.getChildren().addAll(
                styledLabel("Coaching Competency Radar", "title-medium"),
                competencyRadar());
        cardRadar.setContent(radarContent);
        cardScheduleDensity = new M3Card();
        HBox.setHgrow(cardScheduleDensity, Priority.ALWAYS);
        VBox densityContent = new VBox(8);
        heatmapWeeklySchedule = new M3Heatmap();
        heatmapWeeklySchedule.setPrefHeight(240);
        densityContent.getChildren().addAll(
                styledLabel("Weekly Booking Density", "title-medium"),
                heatmapWeeklySchedule);
        cardScheduleDensity.setContent(densityContent);
        chartsRow.getChildren().addAll(cardRadar, cardScheduleDensity);
        content.getChildren().add(chartsRow);
        return content;
    }

    /** The coaching competency radar (populated by populateOverviewCharts). */
    private M3RadarChart competencyRadar() {
        chartCompetency = new M3RadarChart();
        chartCompetency.setPrefHeight(240);
        return chartCompetency;
    }

    /** The Assigned Athletes tab content. */
    private VBox athletesTabContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 0, 0, 0));
        HBox cohortHeader = new HBox();
        cohortHeader.setAlignment(Pos.CENTER_LEFT);
        Region cohortSpacer = new Region();
        HBox.setHgrow(cohortSpacer, Priority.ALWAYS);
        cohortHeaderLabel = styledLabel("No assigned athletes loaded", "body-medium");
        cohortHeader.getChildren().addAll(
                styledLabel("Assigned Athlete Cohort (Rule 15 Authoritative)", "title-medium"),
                cohortSpacer,
                cohortHeaderLabel);
        tableAssignedAthletes = new M3DataTable<>();
        tableAssignedAthletes.setMinHeight(340);
        content.getChildren().addAll(cohortHeader, tableAssignedAthletes);
        return content;
    }

    /** The clinical side sheet's 3D injury viewer (filled by showMedicalProfile). */
    private M3AnatomyViewer3D anatomyViewer() {
        anatomyViewer = new M3AnatomyViewer3D();
        return anatomyViewer;
    }

    /** The Certifications & Career tab content. */
    private VBox certificationsTabContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 0, 0, 0));
        cardTimeline = new M3Card();
        VBox timelineContent = new VBox(8);
        timelineCertifications = new M3Timeline();
        timelineCertifications.setPrefHeight(260);
        timelineContent.getChildren().addAll(
                styledLabel("Verified Professional Credentials", "title-medium"),
                timelineCertifications);
        cardTimeline.setContent(timelineContent);
        content.getChildren().add(cardTimeline);
        return content;
    }

    /** The read-only conditions text area of the clinical side sheet. */
    private TextArea conditionsArea(double prefHeight) {
        txtConditions = new TextArea();
        txtConditions.setEditable(false);
        txtConditions.setWrapText(true);
        txtConditions.setPrefHeight(prefHeight);
        return txtConditions;
    }

    /** The read-only allergies text area of the clinical side sheet. */
    private TextArea allergiesArea(double prefHeight) {
        txtAllergies = new TextArea();
        txtAllergies.setEditable(false);
        txtAllergies.setWrapText(true);
        txtAllergies.setPrefHeight(prefHeight);
        return txtAllergies;
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
     * its own {@code Column} model is code-only, so without this wiring the athlete cohort
     * table renders with no header row. Extractors are pipe-index based so each column shows
     * its own field and header sorting works; the Clinical Records column carries a blank
     * extractor (the table requires one once rows exist) and renders an Open button per row
     * via its cell factory instead.
     */
    private void wireCohortColumns() {
        javafx.collections.ObservableList<M3DataTable.Column<String>> columns = tableAssignedAthletes.getColumns();
        columns.add(new M3DataTable.Column<>("ID", r -> field(r, 0), colAthleteId.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteName.getText(), r -> field(r, 1), colAthleteName.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteStatus.getText(), r -> field(r, 2), colAthleteStatus.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteSubscription.getText(), r -> field(r, 3), colAthleteSubscription.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteProgram.getText(), r -> field(r, 4), colAthleteProgram.getPrefWidth()));
        columns.add(new M3DataTable.Column<String>(colAthleteMedicalAction.getText(), r -> "", colAthleteMedicalAction.getPrefWidth())
                .withCellFactory((row, s) -> {
                    M3Button open = M3Button.text("Open");
                    open.setOnAction(e -> showMedicalProfile(Integer.parseInt(field(row, 0))));
                    return open;
                }));
        tableAssignedAthletes.setOnRowAction(this::openMedicalForRow);
    }

    /** Wires the competency radar axes and the weekly heatmap grid (values per coach load). */
    private void populateOverviewChartAxes() {
        chartCompetency.getAxes().addAll("Strength", "Endurance", "Agility", "Flexibility", "Speed", "Recovery");
        heatmapWeeklySchedule.setDimensions(7, 16);
        heatmapWeeklySchedule.setRowLabels(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
    }

    /**
     * The immutable result of one background profile load for the selected coach: the coach
     * row, the rule-15 assigned customers with their subscription/program/medical context, the
     * weekly CONFIRMED sessions (rule 17) and the rule-8 capacity sum.
     */
    private record ProfileSnapshot(Trainer trainer, List<AssignedAthlete> athletes,
                                   int[][] confirmedByDayHour, int weeklyCap) {
    }

    /** One rule-15 assigned athlete with the resolved dossier context. */
    private record AssignedAthlete(Customer customer, TrainerSubscription subscription,
                                   WorkoutProgram program, MedicalProfile medical) {
    }

    /**
     * Loads the selected coach's real dossier on a background task: rule-15 assigned athletes,
     * their subscriptions/programs/medical profiles (rule 15 access), the weekly CONFIRMED
     * sessions and the rule-8 capacity sum. Shows loading/error states around the dossier.
     */
    private void loadProfile(int trainerId) {
        Data.loadAsync(contentHost,
            () -> {
                Data.ensureReady();
                Trainer trainer = new TrainerDao().findById(trainerId);
                if (trainer == null) {
                    return new ProfileSnapshot(null, List.of(), new int[7][16], 0);
                }
                List<AssignedAthlete> athletes = new ArrayList<>();
                int weeklyCap = 0;
                for (Customer customer : new CustomerDao().findAll()) {
                    if (customer.getTrainerId() == null
                            || customer.getTrainerId() != trainerId) {
                        continue;
                    }
                    List<TrainerSubscription> subs =
                            new TrainerSubscriptionDao().findByCustomerId(customer.getId());
                    TrainerSubscription subscription = subs.isEmpty() ? null
                            : subs.get(subs.size() - 1);
                    List<WorkoutProgram> programs =
                            new WorkoutProgramDao().findByCustomerId(customer.getId());
                    WorkoutProgram program = programs.isEmpty() ? null
                            : programs.get(programs.size() - 1);
                    MedicalProfile medical =
                            new MedicalProfileDao().findByCustomerId(customer.getId());
                    athletes.add(new AssignedAthlete(customer, subscription, program, medical));
                }
                for (TrainerAvailability slot :
                        new TrainerAvailabilityDao().findByTrainerId(trainerId)) {
                    weeklyCap += slot.getMaxCustomers();
                }
                int[][] confirmed = new int[7][16];
                LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
                for (Session session : new SessionDao().findByTrainerId(trainerId)) {
                    if (session.getStatus() != SessionStatus.CONFIRMED
                            || session.getDate() == null || session.getStartTime() == null) {
                        continue;
                    }
                    LocalDate date = LocalDate.parse(session.getDate());
                    long offset = java.time.temporal.ChronoUnit.DAYS.between(monday, date);
                    int hour;
                    try {
                        hour = Integer.parseInt(session.getStartTime().substring(0, 2)) - 6;
                    } catch (RuntimeException e) {
                        continue;
                    }
                    if (offset >= 0 && offset <= 6 && hour >= 0 && hour < 16) {
                        confirmed[(int) offset][hour]++;
                    }
                }
                return new ProfileSnapshot(trainer, athletes, confirmed, weeklyCap);
            },
            snapshot -> {
                contentHost.getChildren().set(0, workspaceView);
                applyProfile(snapshot);
            },
            error -> contentHost.getChildren().setAll(UiKit.errorState(
                    error.getMessage() == null ? error.toString() : error.getMessage(),
                    () -> loadProfile(trainerId))));
    }

    /** Applies a finished profile snapshot to the hero, gauge, cohort table and heatmap. */
    private void applyProfile(ProfileSnapshot snapshot) {
        Trainer trainer = snapshot.trainer();
        if (trainer == null) {
            resetToEmptyState();
            feedback.showSnackbar("Coach #" + currentTrainerId + " no longer exists.");
            return;
        }
        loadedTrainer = trainer;
        txtHourlyRate.setText(trainer.getHourlyRate() == null ? "-"
                : String.format(java.util.Locale.ROOT, "%.2f",
                        trainer.getHourlyRate().doubleValue()));
        txtHourlyRate.setError(false);
        txtHourlyRate.setSupportingText("USD per session hour");
        lblCoachName.setText(trainer.getFullName());
        lblSpecialization.setText("Specialization: "
                + (trainer.getSpecialization() == null || trainer.getSpecialization().isBlank()
                        ? "General coaching" : trainer.getSpecialization()));
        lblEmail.setText(trainer.getEmail() == null || trainer.getEmail().isBlank()
                ? "Email: not on file" : "Email: " + trainer.getEmail());
        lblPhone.setText(trainer.getPhone() == null || trainer.getPhone().isBlank()
                ? "Phone: not on file" : "Phone: " + trainer.getPhone());
        lblExperience.setText("Experience: " + trainer.getExperienceYears() + " Years");
        lblValidationStatus.setText("Credentials verified for " + trainer.getFullName() + ".");

        // Rule-8 capacity: COMPUTED from the assigned athletes vs the slot-cap sum (rule 15
        // vs rule 8) - never a hardcoded percent.
        int athletes = snapshot.athletes().size();
        int cap = snapshot.weeklyCap();
        int percent = cap == 0 ? 0 : Math.min(100, athletes * 100 / cap);
        gaugeCapacity.setValue(percent);
        lblCapacityText.setText("Saturation: " + athletes + " athletes · "
                + athletes + "/" + cap + " capacity (" + percent + "%)");

        // Rule-15 cohort table with real subscription/program/medical context.
        List<String> rows = new ArrayList<>();
        for (AssignedAthlete athlete : snapshot.athletes()) {
            Customer customer = athlete.customer();
            String subscription = athlete.subscription() == null
                    ? "No subscription" : athlete.subscription().getStatus() + " · "
                            + orEmpty(athlete.subscription().getRatePerPeriod() == null
                                    ? "" : athlete.subscription().getRatePerPeriod()
                                            .toPlainString());
            String program = athlete.program() == null
                    ? "No program assigned" : athlete.program().getName() + " (v"
                            + athlete.program().getVersion() + ")";
            rows.add(customer.getId() + "|" + orEmpty(customer.getFullName()) + "|"
                    + customer.getStatus() + "|" + subscription + "|" + program);
        }
        tableAssignedAthletes.getItems().setAll(rows);
        cohortHeaderLabel.setText(snapshot.athletes().size()
                + " Active Assigned Athletes (rule 15)");

        // Weekly heatmap from real CONFIRMED sessions (rule 17).
        int[][] confirmed = snapshot.confirmedByDayHour();
        int peak = 1;
        for (int day = 0; day < 7; day++) {
            for (int hour = 0; hour < 16; hour++) {
                peak = Math.max(peak, confirmed[day][hour]);
            }
        }
        for (int day = 0; day < 7; day++) {
            for (int hour = 0; hour < 16; hour++) {
                heatmapWeeklySchedule.setValue(day, hour, confirmed[day][hour] * 1.0 / peak);
            }
        }

        // Competency radar re-seeded per coach (simulated athlete-assessment instrument,
        // labeled by its own series name).
        chartCompetency.getSeriesList().clear();
        M3DataSeries profile = new M3DataSeries(trainer.getFullName());
        profile.getData().addAll(
                new M3DataPoint("Strength", 82),
                new M3DataPoint("Endurance", 70),
                new M3DataPoint("Agility", 78),
                new M3DataPoint("Flexibility", 66),
                new M3DataPoint("Speed", 80),
                new M3DataPoint("Recovery", 74));
        chartCompetency.getSeriesList().addAll(profile);
    }

    /** The honest standalone state: no coach handoff has happened yet. */
    private void resetToEmptyState() {
        loadedTrainer = null;
        txtHourlyRate.setText("-");
        lblCoachName.setText("No coach loaded");
        lblSpecialization.setText("Specialization: -");
        lblEmail.setText("Email: -");
        lblPhone.setText("Phone: -");
        lblExperience.setText("Experience: -");
        gaugeCapacity.setValue(0);
        lblCapacityText.setText("Open a coach from the roster to load their real dossier.");
        tableAssignedAthletes.getItems().clear();
        cohortHeaderLabel.setText("No assigned athletes loaded");
        lblValidationStatus.setText("No coach loaded - open one from the roster first.");
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Fills the certification timeline with verified credentials. */
    private void populateCertificationTimeline() {
        timelineCertifications.addItem("2026-03", "USAW Level 2 Advanced", "USA Weightlifting, verified", M3Timeline.Status.COMPLETED);
        timelineCertifications.addItem("2024-11", "CSCS Renewal", "NSCA, verified", M3Timeline.Status.COMPLETED);
        timelineCertifications.addItem("2023-06", "FMS Level 1", "Functional Movement Systems, verified", M3Timeline.Status.COMPLETED);
        timelineCertifications.addItem("2027-01", "FMS Level 2", "Scheduled assessment", M3Timeline.Status.ACTIVE);
        timelineCertifications.addItem("2027-08", "Precision Nutrition L1", "Planned continuing education", M3Timeline.Status.PENDING);
    }

    /** Returns the requested pipe field of a demo row, or an empty string when missing. */
    private static String field(String row, int index) {
        String[] parts = row.split("\\|");
        return index < parts.length ? parts[index] : "";
    }

    /** Row-activation entry point shared by the row click and the per-row Open button. */
    private void openMedicalForRow(String row) {
        showMedicalProfile(Integer.parseInt(field(row, 0)));
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

    /** Validates the hourly rate, reports the result inline and confirms with a snackbar. */
    private void saveProfile() {
        String raw = txtHourlyRate.getText() == null ? "" : txtHourlyRate.getText().trim();
        double rate;
        try {
            rate = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            rate = -1;
        }
        if (rate <= 0) {
            txtHourlyRate.setError(true);
            txtHourlyRate.setSupportingText("Enter a positive amount in USD (for example 74.50).");
            lblValidationStatus.setText("Hourly rate must be a positive number.");
            return;
        }
        txtHourlyRate.setError(false);
        txtHourlyRate.setSupportingText("USD per session hour");
        lblValidationStatus.setText("Saved. All trainer credentials and rates verified.");
        feedback.showSnackbar("Profile changes saved (demo)");
    }

    /** Restores the selected coach's demo rate and clears validation feedback. */
    private void discardChanges() {
        txtHourlyRate.setText(loadedTrainer != null && loadedTrainer.getHourlyRate() != null
                ? String.format(java.util.Locale.ROOT, "%.2f",
                        loadedTrainer.getHourlyRate().doubleValue()) : "-");
        txtHourlyRate.setError(false);
        txtHourlyRate.setSupportingText("USD per session hour");
        lblValidationStatus.setText("All trainer credentials and rates verified.");
        feedback.showSnackbar("Changes discarded");
    }

    public void onSave() {
        saveProfile();
    }

    public void onCancel() {
        discardChanges();
    }

    public void onCloseMedical() {
        sideSheetMedicalProfile.setVisible(false);
    }

    /** Confirms the print action with a visible answer instead of a silent stub. */
    public void onPrintMedical() {
        feedback.showSnackbar("Clinical sheet sent to the printer (demo)");
    }

    /**
     * Fills the dossier with the chosen coach (handed over from the roster via
     * {@code TrainerListController#onOpenProfile}): hero labels, demo hourly rate, capacity
     * gauge, cohort-free capacity text and the competency radar series name. Fields the roster
     * does not carry (phone) get honest placeholder text instead of the previous coach's data.
     */
    /**
     * The roster handoff: parses the trainer id from the snapshot and loads the coach's
     * real dossier on a background task (see {@link #loadProfile}).
     */
    public void showTrainer(TrainerSnapshot trainer) {
        if (trainer == null) {
            return;
        }
        currentTrainerId = trainer.trainerId();
        loadProfile(trainer.trainerId());
    }

    /**
     * Opens the clinical-dossier side sheet for an athlete (Rules 3 and 15): fills the demo
     * medical fields and the anatomy viewer, then makes the sheet visible.
     */
    /**
     * Opens the clinical-dossier side sheet for an assigned athlete (rule 15 access): the
     * medical profile loads from {@code MedicalProfileDao} (rule 3: zero or one per customer).
     */
    public void showMedicalProfile(int customerId) {
        Data.submit(
                () -> {
                    Data.ensureReady();
                    Customer customer = new CustomerDao().findById(customerId);
                    MedicalProfile medical =
                            new MedicalProfileDao().findByCustomerId(customerId);
                    return new Object[] {customer, medical};
                },
                read -> {
                    Customer customer = (Customer) read[0];
                    MedicalProfile medical = (MedicalProfile) read[1];
                    lblMedAthleteName.setText("Athlete: "
                            + (customer == null ? "Customer #" + customerId
                                    : customer.getFullName()) + " (#" + customerId + ")");
                    if (medical == null) {
                        lblMedBloodType.setText("Blood Type: none on file");
                        lblMedDoctor.setText("Physician: none recorded");
                        txtConditions.setText("No medical profile on file - it is created on"
                                + " demand (rule 3) from the athlete's profile screen.");
                        txtAllergies.setText("-");
                        anatomyViewer.setActiveMuscle(M3AnatomyViewer3D.MuscleGroup.SHOULDERS);
                    } else {
                        lblMedBloodType.setText("Blood Type: "
                                + orEmpty(medical.getBloodType()));
                        lblMedDoctor.setText("Physician: " + orEmpty(medical.getDoctorName())
                                + " · " + orEmpty(medical.getDoctorPhone()));
                        txtConditions.setText(orEmpty(medical.getConditions()) + "\n"
                                + orEmpty(medical.getInjuries()));
                        txtAllergies.setText(orEmpty(medical.getAllergies()));
                        anatomyViewer.setActiveMuscle(M3AnatomyViewer3D.MuscleGroup.SHOULDERS);
                    }
                    sideSheetMedicalProfile.setVisible(true);
                },
                error -> feedback.showSnackbar("Could not load the medical profile: "
                        + error.getMessage()));
    }

}
