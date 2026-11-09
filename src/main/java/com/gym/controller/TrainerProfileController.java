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

import java.util.List;

/**
 * Controller for the Trainer Profile screen.
 * UI-contract only (no backend/service dependencies): owns the demo athlete cohort, the
 * certification timeline, the capacity gauge and the clinical-dossier side sheet. The dossier
 * is filled per coach by the roster handoff ({@code TrainerListController#onOpenProfile} via
 * {@link #showTrainer(TrainerSnapshot)}); opened directly it keeps the demo default coach.
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

    /** Demo hourly rate applied before a roster handoff; the Cancel action restores it. */
    private static final String DEFAULT_RATE = "74.50";

    /** Rate currently shown for the selected coach; Cancel/discards restore this value. */
    private String demoRate = DEFAULT_RATE;

    /** Demo cohort rows in "id|name|status|subscription|program" shape (matches hero counts). */
    private final List<String> demoAthletes = List.of(
            "312|Maya Lin|Active|Elite PT|Olympic Lifting Block",
            "314|John Doe|Active|Premium PT|Powerbuilding 5x5",
            "317|Ana Silva|Active|Elite PT|VO2 Max Builder",
            "319|Leo Martins|Active|Premium PT|Hypertrophy Split",
            "321|Zoe Chen|Active|Standard PT|Mobility Foundations",
            "323|Omar Haddad|Active|Elite PT|Conditioning Circuit",
            "325|Sofia Reyes|Active|Premium PT|Return-to-Play Phase 2",
            "328|Nils Berg|Active|Standard PT|Strength Basics",
            "330|Dana Wolfe|Active|Elite PT|Peaking Cycle",
            "332|Tomas Rivera|Active|Premium PT|Fat Loss Circuit",
            "334|Ava Kowalski|Active|Standard PT|Foundations On-Ramp",
            "336|Jonas Weber|Active|Elite PT|Powerlifting Meet Prep",
            "338|Priya Nair|Active|Premium PT|Rehab Shoulder Stability",
            "340|Igor Petrov|Active|Elite PT|Hypertrophy PPL",
            "342|Lucia Moretti|Active|Premium PT|Kettlebell Conditioning",
            "345|Ethan Brooks|Active|Standard PT|Cardio Base Builder",
            "347|Hana Sato|Active|Elite PT|Olympic Lifting Block",
            "349|Pavel Novak|Active|Premium PT|Strength Peaking",
            "351|Rita Almeida|Active|Standard PT|Mobility Foundations",
            "353|Yusuf Demir|Active|Premium PT|Powerbuilding 5x5",
            "355|Grace Okafor|Active|Elite PT|Conditioning Circuit",
            "357|Ben Carter|Active|Standard PT|Fat Loss Circuit",
            "359|Lena Fischer|Active|Premium PT|Return-to-Play Phase 1");

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
        populateHeroMetrics();
        populateOverviewCharts();
        populateCohortTable();

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
        populateCertificationTimeline();
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
        lblPhone = new Label("+1 (555) 392-8819");
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

        shell.getChildren().addAll(topAppBar, workspaceScroll);

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
        cohortHeader.getChildren().addAll(
                styledLabel("Assigned Athlete Cohort (Rule 15 Authoritative)", "title-medium"),
                cohortSpacer,
                styledLabel("23 Active Assigned Athletes", "body-medium"));
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

    /** Fills the capacity gauge, demo rate and overview charts so no hero/overview slot is empty. */
    private void populateHeroMetrics() {
        if (txtHourlyRate.getText() == null || txtHourlyRate.getText().isBlank()) {
            txtHourlyRate.setText(DEFAULT_RATE);
        }
        gaugeCapacity.setTitle("CAPACITY");
        gaugeCapacity.setValue(92);
    }

    /** Fills the competency radar and weekly booking heatmap on the Overview tab. */
    private void populateOverviewCharts() {
        chartCompetency.getAxes().addAll("Strength", "Endurance", "Agility", "Flexibility", "Speed", "Recovery");
        M3DataSeries profile = new M3DataSeries("Elena Rostova");
        profile.getData().addAll(
                new M3DataPoint("Strength", 90),
                new M3DataPoint("Endurance", 74),
                new M3DataPoint("Agility", 82),
                new M3DataPoint("Flexibility", 68),
                new M3DataPoint("Speed", 88),
                new M3DataPoint("Recovery", 79));
        chartCompetency.getSeriesList().addAll(profile);

        heatmapWeeklySchedule.setDimensions(7, 16);
        heatmapWeeklySchedule.setRowLabels(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
        for (int day = 0; day < 7; day++) {
            for (int slot = 0; slot < 16; slot++) {
                int hour = slot + 6;
                double intensity = 0.12;
                if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19)) {
                    intensity = 0.80 - (day % 2 == 0 ? 0.0 : 0.15);
                } else if (hour >= 12 && hour <= 14) {
                    intensity = 0.45;
                }
                heatmapWeeklySchedule.setValue(day, slot, intensity);
            }
        }
    }

    /** Fills the cohort table with the demo athletes so Rule 15 counts match the hero banner. */
    private void populateCohortTable() {
        tableAssignedAthletes.getItems().setAll(demoAthletes);
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
        txtHourlyRate.setText(demoRate);
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
    public void showTrainer(TrainerSnapshot trainer) {
        if (trainer == null) {
            return;
        }
        lblCoachName.setText(trainer.name());
        lblSpecialization.setText("Specialization: " + trainer.specialization());
        lblEmail.setText(trainer.email());
        lblPhone.setText("Phone: on file with the front desk (demo)");
        lblExperience.setText("Experience: " + trainer.yearsExperience() + " Years");
        demoRate = String.format(java.util.Locale.ROOT, "%.2f", trainer.hourlyRate());
        txtHourlyRate.setText(demoRate);
        txtHourlyRate.setError(false);
        txtHourlyRate.setSupportingText("USD per session hour");
        gaugeCapacity.setValue(trainer.saturationPercent());
        lblCapacityText.setText("Saturation: " + trainer.assignedAthletes() + " athletes · "
                + trainer.saturationPercent() + "% capacity");
        lblValidationStatus.setText("Credentials verified for " + trainer.name() + ".");
        if (!chartCompetency.getSeriesList().isEmpty()) {
            chartCompetency.getSeriesList().get(0).setName(trainer.name());
        }
    }

    /**
     * Opens the clinical-dossier side sheet for an athlete (Rules 3 and 15): fills the demo
     * medical fields and the anatomy viewer, then makes the sheet visible.
     */
    public void showMedicalProfile(int customerId) {
        lblMedAthleteName.setText("Athlete: " + athleteName(customerId) + " (#" + customerId + ")");
        lblMedBloodType.setText("Blood Type: O+");
        lblMedDoctor.setText("Physician: Dr. Robert Chen (555-0199)");
        txtConditions.setText("Right shoulder impingement (rehab phase 2). No loaded overhead work above 60% 1RM until cleared.");
        txtAllergies.setText("Penicillin. Uses inhaler before conditioning blocks (exercise-induced asthma).");
        anatomyViewer.setActiveMuscle(M3AnatomyViewer3D.MuscleGroup.SHOULDERS);
        sideSheetMedicalProfile.setVisible(true);
    }

    /** Looks up the demo athlete name by id, falling back to the cohort id itself. */
    private String athleteName(int customerId) {
        return demoAthletes.stream()
                .filter(r -> field(r, 0).equals(String.valueOf(customerId)))
                .map(r -> field(r, 1))
                .findFirst()
                .orElse("Athlete " + customerId);
    }
}
