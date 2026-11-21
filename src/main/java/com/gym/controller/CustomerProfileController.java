package com.gym.controller;

import com.gym.dao.CustomerDao;
import com.gym.dao.MedicalProfileDao;
import com.gym.dao.MembershipDao;
import com.gym.dao.ProgressRecordDao;
import com.gym.dao.TrainerDao;
import com.gym.dao.WorkoutProgramDao;
import com.gym.model.Customer;
import com.gym.model.MedicalProfile;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.PlanType;
import com.gym.model.ProgressRecord;
import com.gym.model.Trainer;
import com.gym.model.WorkoutProgram;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
import com.gym.util.PhoneUtil;
import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.chart.M3RadarChart;
import io.m3fx.controls.common.M3Badge;
import io.m3fx.controls.common.M3Divider;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.list.M3ListItem;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.scene3d.M3TiltCard3D;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the Athlete 360 VIP Dossier.
 * Conforms to docs/ui/members/CUSTOMER_PROFILE_UI.md Section 7 contract.
 *
 * <p>Data contract (DECISIONS.md 77/78, task C3): the dossier loads the selected customer
 * from {@code CustomerDao} on a background task ({@link Data#loadAsync}) — identity fields,
 * membership lifecycle, latest biometrics, assigned coach and medical summary are all real.
 * The lifecycle actions enforce the rules through the DAOs: save updates the customer,
 * freeze/unfreeze/renew follow rule 7 via {@code MembershipDao.update} (mirroring the CLI's
 * arithmetic), the coach change follows rule 15 via {@code CustomerDao.update}, biometric
 * logging writes {@code ProgressRecordDao}, and every DAO {@code ValidationException}
 * surfaces verbatim in a snackbar.
 *
 * <p>UX contract: saving validates with inline field errors plus a snackbar answer, freeze is
 * a two-step confirm, and the roster handoff ({@code showMember}) reloads the dossier for the
 * selected member. Standalone previews without a handoff show the designed empty state.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former customer-profile.fxml: identity/biometrics zone, performance zone, lifecycle/dossier
 * zone.
 */
public class CustomerProfileController implements ScreenController {

    /**
     * The immutable result of one background profile load: the customer, its latest
     * membership, all biometric readings, the assigned coach and the medical profile.
     */
    private record ProfileSnapshot(Customer customer, Membership membership,
                                   List<ProgressRecord> progress, Trainer trainer,
                                   MedicalProfile medicalProfile) {
    }

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane rootPane;
    private BorderPane mainLayout;
    private M3TopAppBar topAppBar;
    private M3IconButton btnBack;
    private M3Button btnSaveProfile;
    private M3TiltCard3D holographicPass;
    private M3StatCard statWeight;
    private M3StatCard statBodyFat;
    private M3StatCard statTarget;
    private M3StatCard statVbt;
    private M3Button btnLogBiometric;
    private M3Card cardMedicalAlert;
    private Label lblMedicalSummary;
    private M3Button btnOpenMedical;
    private M3RadarChart radarAthleteCapacity;
    private M3Badge badgeProgramVersion;
    private Label lblProgramName;
    private Label lblProgramDetails;
    private M3Button btnViewProgram;
    private M3Button btnAssignProgram;
    private M3Badge badgeMembershipStatus;
    private Label lblPlanType;
    private Label lblPlanDates;
    private M3Button btnFreezePlan;
    private M3Button btnUnfreezePlan;
    private M3Button btnRenewPlan;
    private Label lblTrainerName;
    private Label lblTrainerSpecialization;
    private M3Button btnChangeTrainer;
    private M3TextField txtFullName;
    private M3TextField txtPhone;
    private M3TextField txtEmail;
    private M3TextField txtAddress;
    private M3TextField txtDateOfBirth;
    private M3TextField txtEmergencyName;
    private M3TextField txtEmergencyPhone;

    /** Content host around the dossier: swaps loading/error states around DAO loads. */
    private BorderPane contentHost;
    /** The scrollable dossier node, restored after loading/error states. */
    private javafx.scene.Node cockpitDossier;
    /** The customer currently loaded from the database, or null before the first handoff. */
    private Customer loadedCustomer;
    /** The member's latest membership, or null when none is on file. */
    private Membership loadedMembershipRef;
    /** The coach roster for the assign menu, refreshed with every profile load. */
    private List<Trainer> trainerRoster = new ArrayList<>();

    /** Overlay host for snackbars and dialogs, added to the screen root in code (no fx:id). */
    private final M3Overlay overlay = new M3Overlay();
    /** Member identity on the holographic pass ("M-—" until the roster handoff replaces it). */
    private String currentMemberId = "M-\u2014";

    /**
     * Builds the complete Athlete 360 VIP Dossier and initializes all behavior. Same ordering
     * semantics the FXMLLoader used to provide: the node tree exists and every field is
     * assigned before the former {@code initialize()} work runs.
     */
    public CustomerProfileController() {
        // Primary screen layout surface.
        mainLayout = new BorderPane();

        // Top app bar.
        topAppBar = new M3TopAppBar("ATHLETE PROFILE",
                io.m3fx.controls.appbar.TopAppBarVariant.CENTER_ALIGNED);
        HBox.setHgrow(topAppBar, Priority.ALWAYS);
        btnBack = new M3IconButton(M3Icon.symbol("arrow_back"), IconButtonVariant.STANDARD);
        btnBack.setOnAction(e -> onNavigateBack());
        btnBack.setAccessibleText("Back to member roster");
        topAppBar.setNavigationIcon(btnBack);

        HBox appActions = new HBox(8);
        appActions.setAlignment(Pos.CENTER_RIGHT);
        appActions.setPadding(new Insets(0, 16, 0, 0));
        btnSaveProfile = new M3Button("Save Changes", ButtonVariant.FILLED);
        btnSaveProfile.setGraphic(M3Icon.symbol("check"));
        btnSaveProfile.setOnAction(e -> onSave());
        appActions.getChildren().add(btnSaveProfile);

        HBox appBarRow = new HBox();
        appBarRow.setAlignment(Pos.CENTER_LEFT);
        appBarRow.getStyleClass().add("app-bar-container");
        appBarRow.getChildren().addAll(topAppBar, appActions);
        mainLayout.setTop(appBarRow);

        // Center scrollable cockpit.
        HBox cockpitRow = new HBox(20);
        cockpitRow.setPadding(new Insets(20, 24, 64, 24));

        // Zone A: identity & biometrics.
        VBox identityZone = new VBox(16);
        identityZone.setPrefWidth(360);
        identityZone.setMinWidth(300);

        // 3D holographic membership pass.
        holographicPass = new M3TiltCard3D();

        // Biometric telemetry card.
        M3Card biometricsCard = new M3Card(CardVariant.OUTLINED);
        VBox biometricsContent = new VBox(12);
        biometricsContent.setPadding(new Insets(16));
        biometricsContent.getChildren().add(styledLabel("BIOMETRIC TELEMETRY", "title-small"));
        GridPane biometricsGrid = new GridPane(12, 12);
        statWeight = new M3StatCard("WEIGHT", "-- kg", "");
        statBodyFat = new M3StatCard("BODY FAT", "-- %", "");
        statTarget = new M3StatCard("TARGET", "-- kg", "");
        statVbt = new M3StatCard("VBT 1RM", "-- kg", "");
        statWeight.setMaxWidth(Double.MAX_VALUE);
        statBodyFat.setMaxWidth(Double.MAX_VALUE);
        statTarget.setMaxWidth(Double.MAX_VALUE);
        statVbt.setMaxWidth(Double.MAX_VALUE);
        ColumnConstraints halfColumn = new ColumnConstraints();
        halfColumn.setPercentWidth(50);
        biometricsGrid.getColumnConstraints().addAll(halfColumn, halfColumn);
        biometricsGrid.add(statWeight, 0, 0);
        biometricsGrid.add(statBodyFat, 1, 0);
        biometricsGrid.add(statTarget, 0, 1);
        biometricsGrid.add(statVbt, 1, 1);
        btnLogBiometric = new M3Button("Log New Biometric", ButtonVariant.TONAL);
        btnLogBiometric.setMaxWidth(Double.MAX_VALUE);
        btnLogBiometric.setOnAction(e -> onLogBiometric());
        biometricsContent.getChildren().addAll(biometricsGrid, btnLogBiometric);
        biometricsCard.setContent(biometricsContent);

        // Medical & safety alert callout.
        cardMedicalAlert = new M3Card(CardVariant.ELEVATED);
        VBox medicalContent = new VBox(8);
        medicalContent.setPadding(new Insets(14));
        HBox medicalHeader = new HBox(8);
        medicalHeader.setAlignment(Pos.CENTER_LEFT);
        medicalHeader.getChildren().addAll(
                M3Icon.symbol("favorite"),
                styledLabel("MEDICAL & SAFETY", "title-small"));
        lblMedicalSummary = new Label("No active contraindications recorded.");
        lblMedicalSummary.setWrapText(true);
        lblMedicalSummary.getStyleClass().add("body-small");
        btnOpenMedical = new M3Button("Open Medical Console", ButtonVariant.TEXT);
        btnOpenMedical.setOnAction(e -> onOpenMedicalProfile());
        medicalContent.getChildren().addAll(medicalHeader, lblMedicalSummary, btnOpenMedical);
        cardMedicalAlert.setContent(medicalContent);

        identityZone.getChildren().addAll(holographicPass, biometricsCard, cardMedicalAlert);

        // Zone B: performance & program.
        VBox performanceZone = new VBox(16);
        performanceZone.setPrefWidth(480);
        HBox.setHgrow(performanceZone, Priority.ALWAYS);

        // Athlete capacity polygon.
        M3Card radarCard = new M3Card(CardVariant.ELEVATED);
        VBox radarContent = new VBox(12);
        radarContent.setAlignment(Pos.CENTER);
        radarContent.setPadding(new Insets(16));
        HBox radarHeader = new HBox();
        radarHeader.setAlignment(Pos.CENTER_LEFT);
        Region radarSpacer = new Region();
        HBox.setHgrow(radarSpacer, Priority.ALWAYS);
        M3Badge axisBadge = new M3Badge();
        axisBadge.setText("6-AXIS VBT");
        radarHeader.getChildren().addAll(
                styledLabel("ATHLETE CAPACITY POLYGON", "title-small"),
                radarSpacer,
                axisBadge);
        radarAthleteCapacity = new M3RadarChart();
        radarAthleteCapacity.setPrefSize(420, 300);
        radarContent.getChildren().addAll(radarHeader, radarAthleteCapacity);
        radarCard.setContent(radarContent);

        // Active workout program.
        M3Card programCard = new M3Card(CardVariant.OUTLINED);
        VBox programContent = new VBox(12);
        programContent.setPadding(new Insets(16));
        HBox programHeader = new HBox();
        programHeader.setAlignment(Pos.CENTER_LEFT);
        Region programSpacer = new Region();
        HBox.setHgrow(programSpacer, Priority.ALWAYS);
        badgeProgramVersion = new M3Badge();
        badgeProgramVersion.setText("v1");
        programHeader.getChildren().addAll(
                styledLabel("ACTIVE WORKOUT PROGRAM", "title-small"),
                programSpacer,
                badgeProgramVersion);
        lblProgramName = new Label("No Program Assigned");
        lblProgramName.getStyleClass().add("title-medium");
        lblProgramDetails = new Label("Exercises will appear when assigned by head coach.");
        lblProgramDetails.getStyleClass().add("body-small");
        HBox programActions = new HBox(8);
        btnViewProgram = new M3Button("View Workout", ButtonVariant.TONAL);
        btnViewProgram.setOnAction(e -> onViewProgram());
        btnAssignProgram = new M3Button("Assign Program", ButtonVariant.TEXT);
        btnAssignProgram.setOnAction(e -> onAssignProgram());
        programActions.getChildren().addAll(btnViewProgram, btnAssignProgram);
        programContent.getChildren().addAll(programHeader, lblProgramName, lblProgramDetails, programActions);
        programCard.setContent(programContent);

        performanceZone.getChildren().addAll(radarCard, programCard);

        // Zone C: lifecycle, coach & demographics.
        VBox lifecycleZone = new VBox(16);
        lifecycleZone.setPrefWidth(440);
        lifecycleZone.setMinWidth(320);

        // Membership plan lifecycle card.
        M3Card planCard = new M3Card(CardVariant.ELEVATED);
        VBox planContent = new VBox(12);
        planContent.setPadding(new Insets(16));
        HBox planHeader = new HBox();
        planHeader.setAlignment(Pos.CENTER_LEFT);
        Region planSpacer = new Region();
        HBox.setHgrow(planSpacer, Priority.ALWAYS);
        badgeMembershipStatus = new M3Badge();
        badgeMembershipStatus.setText("ACTIVE");
        planHeader.getChildren().addAll(
                styledLabel("MEMBERSHIP PLAN", "title-small"),
                planSpacer,
                badgeMembershipStatus);
        lblPlanType = new Label("Plan: Monthly Standard");
        lblPlanType.getStyleClass().add("headline-small");
        lblPlanDates = new Label("Valid: 2026-01-01 -> 2026-12-31");
        lblPlanDates.getStyleClass().add("body-medium");
        HBox planActions = new HBox(8);
        btnFreezePlan = new M3Button("Freeze", ButtonVariant.TONAL);
        btnFreezePlan.setOnAction(e -> onFreezeMembership());
        btnUnfreezePlan = new M3Button("Unfreeze", ButtonVariant.TONAL);
        btnUnfreezePlan.setOnAction(e -> onUnfreezeMembership());
        btnRenewPlan = new M3Button("Renew", ButtonVariant.TONAL);
        btnRenewPlan.setOnAction(e -> onRenewMembership());
        planActions.getChildren().addAll(btnFreezePlan, btnUnfreezePlan, btnRenewPlan);
        planContent.getChildren().addAll(planHeader, lblPlanType, lblPlanDates, planActions);
        planCard.setContent(planContent);

        // Assigned coach card.
        M3Card coachCard = new M3Card(CardVariant.OUTLINED);
        VBox coachContent = new VBox(10);
        coachContent.setPadding(new Insets(14));
        coachContent.getChildren().add(styledLabel("ASSIGNED COACH", "title-small"));
        HBox coachRow = new HBox(12);
        coachRow.setAlignment(Pos.CENTER_LEFT);
        VBox coachIdentity = new VBox();
        lblTrainerName = new Label("Unassigned");
        lblTrainerName.getStyleClass().add("body-large");
        lblTrainerSpecialization = new Label("Strength & Conditioning");
        lblTrainerSpecialization.getStyleClass().add("body-small");
        coachIdentity.getChildren().addAll(lblTrainerName, lblTrainerSpecialization);
        btnChangeTrainer = new M3Button("Change", ButtonVariant.TEXT);
        btnChangeTrainer.setOnAction(e -> onChangeTrainer());
        Region coachSpacer = new Region();
        HBox.setHgrow(coachSpacer, Priority.ALWAYS);
        coachRow.getChildren().addAll(M3Icon.symbol("person"), coachIdentity, coachSpacer, btnChangeTrainer);
        coachContent.getChildren().add(coachRow);
        coachCard.setContent(coachContent);

        // Biographical & emergency form card.
        M3Card dossierCard = new M3Card(CardVariant.OUTLINED);
        VBox dossierContent = new VBox(12);
        dossierContent.setPadding(new Insets(16));
        dossierContent.getChildren().add(styledLabel("BIOGRAPHICAL DOSSIER", "title-small"));
        txtFullName = outlinedField("Full Name", "Required");
        txtPhone = outlinedField("Phone Number", "11 digits starting 010, 011, 012 or 015");
        txtEmail = outlinedField("Email Address", "Used for session reminders");
        txtAddress = outlinedField("Physical Address", "Street, city");
        txtDateOfBirth = outlinedField("Date of Birth", "YYYY-MM-DD");
        txtEmergencyName = outlinedField("Contact Name", null);
        txtEmergencyPhone = outlinedField("Emergency Phone", "Called for gym incidents only");
        dossierContent.getChildren().addAll(
                txtFullName, txtPhone, txtEmail, txtAddress, txtDateOfBirth,
                new M3Divider(),
                styledLabel("EMERGENCY CONTACT", "label-large"),
                txtEmergencyName, txtEmergencyPhone);
        dossierCard.setContent(dossierContent);

        lifecycleZone.getChildren().addAll(planCard, coachCard, dossierCard);

        cockpitRow.getChildren().addAll(identityZone, performanceZone, lifecycleZone);
        ScrollPane cockpitScroll = new ScrollPane(cockpitRow);
        cockpitScroll.setFitToWidth(true);
        cockpitScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cockpitScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // Content host so the real-data load can swap the dossier for loading/error states.
        cockpitDossier = cockpitScroll;
        contentHost = new BorderPane();
        contentHost.setCenter(cockpitDossier);
        mainLayout.setCenter(contentHost);

        rootPane = new StackPane();
        rootPane.getStyleClass().add("customer-profile-root");
        rootPane.getChildren().addAll(mainLayout, overlay);

        resetToEmptyState();

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        rootPane.setId("rootPane");
        mainLayout.setId("mainLayout");
        topAppBar.setId("topAppBar");
        btnBack.setId("btnBack");
        btnSaveProfile.setId("btnSaveProfile");
        holographicPass.setId("holographicPass");
        statWeight.setId("statWeight");
        statBodyFat.setId("statBodyFat");
        statTarget.setId("statTarget");
        statVbt.setId("statVbt");
        btnLogBiometric.setId("btnLogBiometric");
        cardMedicalAlert.setId("cardMedicalAlert");
        lblMedicalSummary.setId("lblMedicalSummary");
        btnOpenMedical.setId("btnOpenMedical");
        radarAthleteCapacity.setId("radarAthleteCapacity");
        badgeProgramVersion.setId("badgeProgramVersion");
        lblProgramName.setId("lblProgramName");
        lblProgramDetails.setId("lblProgramDetails");
        btnViewProgram.setId("btnViewProgram");
        btnAssignProgram.setId("btnAssignProgram");
        badgeMembershipStatus.setId("badgeMembershipStatus");
        lblPlanType.setId("lblPlanType");
        lblPlanDates.setId("lblPlanDates");
        btnFreezePlan.setId("btnFreezePlan");
        btnUnfreezePlan.setId("btnUnfreezePlan");
        btnRenewPlan.setId("btnRenewPlan");
        lblTrainerName.setId("lblTrainerName");
        lblTrainerSpecialization.setId("lblTrainerSpecialization");
        btnChangeTrainer.setId("btnChangeTrainer");
        txtFullName.setId("txtFullName");
        txtPhone.setId("txtPhone");
        txtEmail.setId("txtEmail");
        txtAddress.setId("txtAddress");
        txtDateOfBirth.setId("txtDateOfBirth");
        txtEmergencyName.setId("txtEmergencyName");
        txtEmergencyPhone.setId("txtEmergencyPhone");
        populateCapacityRadar();
        renderIdentityPass();
        // The plan starts ACTIVE, so Unfreeze has nothing to do yet (Hick's law: only the
        // meaningful half of the pair is offered).
        btnUnfreezePlan.setDisable(true);
    }

    /** One M3TextField with an outlined variant, label and optional supporting text. */
    private static M3TextField outlinedField(String labelText, String supportingText) {
        M3TextField field = new M3TextField(labelText, TextFieldVariant.OUTLINED);
        if (supportingText != null) {
            field.setSupportingText(supportingText);
        }
        return field;
    }

    /** Small label with one style class. */
    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    @Override
    public Parent getRoot() {
        return rootPane;
    }

    // ------------------------------------------------------------------
    // Event handlers (answers for every visible action)
    // ------------------------------------------------------------------

    /** Returns to the member roster via the shell rail; answers with a hint if standalone. */
    public void onNavigateBack() {
        navigateViaShellRail("navCustomers",
                "Open the member roster from the navigation rail (this preview is standalone).");
    }

    /**
     * The roster handoff (the "Open Full Profile" action in {@code CustomerListController}):
     * parses the "M-0012"-style member id and loads that customer's real dossier on a
     * background task. Non-numeric ids answer with a snackbar instead of guessing.
     */
    public void showMember(String memberName, String memberId) {
        int customerId = parseMemberId(memberId);
        if (customerId <= 0) {
            overlay.showSnackbar("Could not resolve member id " + memberId
                    + " — open the profile from the member roster.");
            return;
        }
        if (memberName != null && !memberName.isBlank()) {
            txtFullName.setText(memberName.trim());
        }
        currentMemberId = memberId.trim();
        renderIdentityPass();
        loadMember(customerId);
    }

    /** Parses "M-0012" (or a plain number) into a database id; -1 when unparseable. */
    private static int parseMemberId(String memberId) {
        if (memberId == null) {
            return -1;
        }
        String digits = memberId.trim().replaceFirst("^M-?", "");
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Loads one member's full dossier (customer, latest membership, biometrics, coach, medical
     * profile) on a background task; shows {@link UiKit#loadingState()} while running and
     * {@link UiKit#errorState()} with retry on failure.
     */
    private void loadMember(int customerId) {
        Data.loadAsync(contentHost,
            () -> {
                Data.ensureReady();
                Customer customer = new CustomerDao().findById(customerId);
                if (customer == null) {
                    return new ProfileSnapshot(null, null, List.of(), null, null);
                }
                Membership membership = latestMembership(customerId);
                List<ProgressRecord> progress = new ProgressRecordDao().findByCustomerId(customerId);
                Trainer trainer = customer.getTrainerId() == null ? null
                        : new TrainerDao().findById(customer.getTrainerId());
                MedicalProfile medical = new MedicalProfileDao().findByCustomerId(customerId);
                return new ProfileSnapshot(customer, membership, progress, trainer, medical);
            },
            snapshot -> {
                contentHost.setCenter(cockpitDossier);
                applyProfile(snapshot);
            },
            error -> contentHost.setCenter(UiKit.errorState(
                    error.getMessage() == null ? error.toString() : error.getMessage(),
                    () -> loadMember(customerId))));
    }

    /** The latest membership by end date, or null when the customer has none. */
    private static Membership latestMembership(int customerId) throws Exception {
        Membership latest = null;
        for (Membership membership : new MembershipDao().findByCustomerId(customerId)) {
            if (latest == null
                    || (membership.getEndDate() != null
                        && membership.getEndDate().compareTo(latest.getEndDate()) > 0)) {
                latest = membership;
            }
        }
        return latest;
    }

    /** Applies a finished snapshot to the dossier (fields, badges, stat cards, coach, medical). */
    private void applyProfile(ProfileSnapshot snapshot) {
        loadedCustomer = snapshot.customer();
        if (loadedCustomer == null) {
            resetToEmptyState();
            overlay.showSnackbar("No customer with id " + currentMemberId
                    + " exists in the database.");
            return;
        }
        trainerRoster = loadTrainersQuietly();

        txtFullName.setText(orEmpty(loadedCustomer.getFullName()));
        txtPhone.setText(orEmpty(loadedCustomer.getPhone()));
        txtEmail.setText(orEmpty(loadedCustomer.getEmail()));
        txtAddress.setText(orEmpty(loadedCustomer.getAddress()));
        txtDateOfBirth.setText(orEmpty(loadedCustomer.getDateOfBirth()));
        txtEmergencyName.setText(orEmpty(loadedCustomer.getEmergencyName()));
        txtEmergencyPhone.setText(orEmpty(loadedCustomer.getEmergencyPhone()));
        txtFullName.setError(false);
        txtPhone.setError(false);

        // Biometric telemetry: the latest reading drives the stat cards (rule: weight and
        // body fat are required, target weight is optional on a record).
        List<ProgressRecord> progress = snapshot.progress();
        if (progress.isEmpty()) {
            statWeight.setValue("-- kg");
            statBodyFat.setValue("-- %");
            statTarget.setValue("-- kg");
        } else {
            ProgressRecord latest = progress.get(progress.size() - 1);
            statWeight.setValue(String.format("%.1f kg", latest.getWeightKg()));
            statBodyFat.setValue(String.format("%.1f %%", latest.getBodyFatPct()));
            statTarget.setValue(latest.getTargetWeightKg() == null
                    ? "-- kg" : String.format("%.1f kg", latest.getTargetWeightKg()));
        }
        statVbt.setValue("-- kg"); // no VBT data source yet — honest dash until one exists

        applyMembership(snapshot.membership());

        if (snapshot.trainer() != null) {
            lblTrainerName.setText(snapshot.trainer().getFullName());
            lblTrainerSpecialization.setText(orEmpty(snapshot.trainer().getSpecialization()));
        } else {
            lblTrainerName.setText("Unassigned");
            lblTrainerSpecialization.setText("No coach assigned for this member yet");
        }

        MedicalProfile medical = snapshot.medicalProfile();
        if (medical == null) {
            lblMedicalSummary.setText("No medical profile on file — it is created on demand (rule 3).");
        } else {
            lblMedicalSummary.setText("Blood type " + orEmpty(medical.getBloodType())
                    + " · conditions: " + orEmpty(medical.getConditions())
                    + " · allergies: " + orEmpty(medical.getAllergies()));
        }

        renderIdentityPass();
    }

    /** Loads the coach roster without letting a failure break the profile load. */
    private List<Trainer> loadTrainersQuietly() {
        try {
            return new TrainerDao().findAll();
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Applies one membership (badge, plan copy, dates, and the freeze/unfreeze pair). */
    private void applyMembership(Membership membership) {
        loadedMembershipRef = membership;
        if (membership == null) {
            badgeMembershipStatus.setText("NO PLAN");
            lblPlanType.setText("No membership on file");
            lblPlanDates.setText("Sell a plan from the Membership screen to activate this member.");
            btnFreezePlan.setDisable(true);
            btnUnfreezePlan.setDisable(true);
            btnRenewPlan.setDisable(true);
            return;
        }
        MembershipStatus status = membership.getStatus() == null
                ? MembershipStatus.ACTIVE : membership.getStatus();
        badgeMembershipStatus.setText(status.name());
        lblPlanType.setText("Plan: " + planLabel(membership.getPlan())
                + (status == MembershipStatus.FROZEN ? " (frozen)" : ""));
        lblPlanDates.setText("Valid: " + orEmpty(membership.getStartDate()) + " -> "
                + orEmpty(membership.getEndDate()));
        // Rule 7 drives the pair: only ACTIVE freezes, only FROZEN unfreezes; renew stays
        // available so the DAO's refusal of a CANCELLED renewal can answer with its rule copy.
        btnFreezePlan.setDisable(status != MembershipStatus.ACTIVE);
        btnUnfreezePlan.setDisable(status != MembershipStatus.FROZEN);
        btnRenewPlan.setDisable(false);
    }

    /** Human-readable plan label ("Monthly", "Quarterly", "Yearly"). */
    private static String planLabel(PlanType plan) {
        return switch (plan == null ? PlanType.MONTHLY : plan) {
            case MONTHLY -> "Monthly";
            case QUARTERLY -> "Quarterly";
            case YEARLY -> "Yearly";
        };
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Standalone preview without a roster handoff: an honest empty dossier (no demo person).
     */
    private void resetToEmptyState() {
        loadedCustomer = null;
        loadedMembershipRef = null;
        txtFullName.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        txtDateOfBirth.setText("");
        txtEmergencyName.setText("");
        txtEmergencyPhone.setText("");
        statWeight.setValue("-- kg");
        statBodyFat.setValue("-- %");
        statTarget.setValue("-- kg");
        statVbt.setValue("-- kg");
        badgeMembershipStatus.setText("NO PLAN");
        lblPlanType.setText("No membership on file");
        lblPlanDates.setText("Open a member from the roster to load their real dossier.");
        btnFreezePlan.setDisable(true);
        btnUnfreezePlan.setDisable(true);
        btnRenewPlan.setDisable(true);
        lblTrainerName.setText("Unassigned");
        lblTrainerSpecialization.setText("No coach assigned for this member yet");
        lblMedicalSummary.setText("No medical profile on file — it is created on demand (rule 3).");
        renderIdentityPass();
    }

    /**
     * Saves the dossier through {@code CustomerDao.update} on a background task: inline field
     * errors name the problem first, any DAO {@code ValidationException} surfaces verbatim,
     * and the dossier re-queries so the view is exactly the database.
     */
    public void onSave() {
        if (loadedCustomer == null) {
            overlay.showSnackbar("No member loaded — open one from the member roster first.");
            return;
        }
        String name = txtFullName.getText() == null ? "" : txtFullName.getText().trim();
        String phone = txtPhone.getText() == null ? "" : txtPhone.getText().trim();
        boolean nameOk = !name.isEmpty();
        boolean phoneOk = PhoneUtil.isValid(phone);
        txtFullName.setError(!nameOk);
        txtPhone.setError(!phoneOk);
        if (!nameOk || !phoneOk) {
            overlay.showSnackbar("Fix the highlighted fields, then save again.");
            return;
        }
        Customer toSave = loadedCustomer;
        toSave.setFullName(name);
        toSave.setPhone(phone);
        toSave.setEmail(txtEmail.getText() == null ? "" : txtEmail.getText().trim());
        toSave.setAddress(txtAddress.getText() == null ? "" : txtAddress.getText().trim());
        toSave.setDateOfBirth(txtDateOfBirth.getText() == null ? "" : txtDateOfBirth.getText().trim());
        toSave.setEmergencyName(txtEmergencyName.getText() == null ? "" : txtEmergencyName.getText().trim());
        toSave.setEmergencyPhone(txtEmergencyPhone.getText() == null ? "" : txtEmergencyPhone.getText().trim());
        Data.submit(
                () -> {
                    new CustomerDao().update(toSave);
                    return toSave;
                },
                saved -> {
                    overlay.showSnackbar("Profile changes saved for " + saved.getFullName() + ".");
                    renderIdentityPass();
                    loadMember(saved.getId());
                },
                error -> overlay.showSnackbar("Could not save profile: " + error.getMessage()));
    }

    /**
     * Logs a real biometric reading through a dialog: the record persists via
     * {@code ProgressRecordDao} (the target weight carries over from the latest record) and
     * the dossier re-queries so the stat cards show the database state.
     */
    public void onLogBiometric() {
        if (loadedCustomer == null) {
            overlay.showSnackbar("No member loaded — open one from the member roster first.");
            return;
        }
        M3TextField weightField = new M3TextField("Weight (kg)", TextFieldVariant.OUTLINED);
        weightField.setSupportingText("30 - 300, e.g. 84.2");
        M3TextField bodyFatField = new M3TextField("Body fat (%)", TextFieldVariant.OUTLINED);
        bodyFatField.setSupportingText("0 - 70, e.g. 13.8");

        M3Button cancel = M3Button.text("Cancel");
        M3Button save = M3Button.text("Save Reading");
        M3Dialog dialog = M3Dialog.basic("Log new biometric",
                new VBox(12, weightField, bodyFatField), cancel, save);
        dialog.setDefaultAction(save::activate);
        save.setOnAction(e -> {
            Double weight = parseInRange(weightField, 30, 300);
            Double bodyFat = parseInRange(bodyFatField, 0, 70);
            if (weight == null || bodyFat == null) {
                return;
            }
            dialog.hide();
            Customer customer = loadedCustomer;
            Data.submit(
                    () -> {
                        List<ProgressRecord> existing =
                                new ProgressRecordDao().findByCustomerId(customer.getId());
                        ProgressRecord latest = existing.isEmpty()
                                ? null : existing.get(existing.size() - 1);
                        Double targetWeight = latest == null ? null : latest.getTargetWeightKg();
                        ProgressRecord record = new ProgressRecord(0, customer.getId(),
                                LocalDate.now().toString(), weight, bodyFat,
                                "Logged from the profile screen", targetWeight,
                                String.format("Profile log: %.1f kg at %.1f%% body fat.",
                                        weight, bodyFat));
                        new ProgressRecordDao().insert(record);
                        return record;
                    },
                    saved -> {
                        overlay.showSnackbar(String.format(
                                "Reading saved — %.1f kg at %.1f%% body fat.",
                                saved.getWeightKg(), saved.getBodyFatPct()));
                        loadMember(customer.getId());
                    },
                    error -> overlay.showSnackbar(
                            "Could not save reading: " + error.getMessage()));
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    /** Opens a real medical summary for this athlete (content, not a dead end). */
    public void onOpenMedicalProfile() {
        M3Button close = M3Button.text("Close");
        M3Dialog dialog = M3Dialog.basic("Medical & safety summary", medicalSummary(), close);
        close.setOnAction(e -> dialog.hide());
        dialog.setDefaultAction(close::activate);
        overlay.showDialog(dialog);
    }

    /** Shows the latest real program (WorkoutProgramDao) or the honest empty answer. */
    public void onViewProgram() {
        if (loadedCustomer == null) {
            overlay.showSnackbar("No member loaded — open one from the member roster first.");
            return;
        }
        Customer customer = loadedCustomer;
        Data.submit(
                () -> {
                    List<WorkoutProgram> programs =
                            new WorkoutProgramDao().findByCustomerId(customer.getId());
                    return programs.isEmpty() ? null : programs.get(programs.size() - 1);
                },
                program -> {
                    if (program == null) {
                        overlay.showSnackbar("No program assigned yet — use Assign Program first.");
                        return;
                    }
                    badgeProgramVersion.setText("v" + program.getVersion());
                    lblProgramName.setText(program.getName());
                    lblProgramDetails.setText("Created " + orEmpty(program.getCreatedDate())
                            + " by coach #" + program.getCreatorTrainerId() + ".");
                    M3Button close = M3Button.text("Close");
                    M3Dialog dialog = M3Dialog.basic(program.getName(),
                            programContent(program), close);
                    close.setOnAction(e2 -> dialog.hide());
                    dialog.setDefaultAction(close::activate);
                    overlay.showDialog(dialog);
                },
                error -> overlay.showSnackbar("Could not load the program: " + error.getMessage()));
    }

    /**
     * Assigns one of the starter programs through {@code WorkoutProgramDao} (version continues
     * the customer's sequence; the record names its creator coach). Requires an assigned coach
     * — the program record stores its creator (rule: every program is coach-authored).
     */
    public void onAssignProgram() {
        if (loadedCustomer == null) {
            overlay.showSnackbar("No member loaded — open one from the member roster first.");
            return;
        }
        if (loadedCustomer.getTrainerId() == null) {
            overlay.showSnackbar("Assign a coach first — every program records its creator.");
            return;
        }
        M3Menu menu = new M3Menu();
        menu.item("Hypertrophy Base (6 weeks)", () -> assignProgram("Hypertrophy Base",
                "Bench 4x8 @ RIR 2 · Row 4x8 · Pull-up 3xAMRAP · Squat 4x8 · RDL 3x10"));
        menu.item("VBT Strength Peak (4 weeks)", () -> assignProgram("VBT Strength Peak",
                "Back squat 5x3 @ 0.55 m/s · Bench 5x2 @ 0.45 m/s · Trap-bar jump 4x3"));
        menu.item("Return-to-Play Rehab (stage 3)", () -> assignProgram("Return-to-Play Rehab",
                "Leg press 3x12 pain-free · Hip hinge 3x12 · Bike 15 min Z2"));
        showMenuBelow(btnAssignProgram, menu, "Open this profile in the app window to pick a program.");
    }

    /** Inserts the chosen program template as the customer's next version. */
    private void assignProgram(String name, String description) {
        Customer customer = loadedCustomer;
        Data.submit(
                () -> {
                    List<WorkoutProgram> existing =
                            new WorkoutProgramDao().findByCustomerId(customer.getId());
                    int nextVersion = 1;
                    for (WorkoutProgram program : existing) {
                        nextVersion = Math.max(nextVersion, program.getVersion() + 1);
                    }
                    WorkoutProgram program = new WorkoutProgram(0, customer.getId(),
                            customer.getTrainerId(), name, description, nextVersion,
                            LocalDate.now().toString());
                    new WorkoutProgramDao().insert(program);
                    return program;
                },
                program -> {
                    badgeProgramVersion.setText("v" + program.getVersion());
                    lblProgramName.setText(program.getName());
                    lblProgramDetails.setText("Assigned just now — reviewed by the head coach.");
                    overlay.showSnackbar(program.getName() + " assigned as v"
                            + program.getVersion() + ".");
                },
                error -> overlay.showSnackbar("Could not assign program: " + error.getMessage()));
    }

    /**
     * Rule 7 freeze through {@code MembershipDao.update}: only an ACTIVE membership freezes,
     * the frozen-day counter is stored, and billing/bookings pause. Destructive, so it
     * confirms first.
     */
    public void onFreezeMembership() {
        Membership membership = loadedMembershipRef;
        if (membership == null) {
            overlay.showSnackbar("No membership on file for this member.");
            return;
        }
        M3TextField daysField = new M3TextField("Days to freeze", TextFieldVariant.OUTLINED);
        daysField.setSupportingText("1 - 365, e.g. 14");
        M3Button cancel = M3Button.text("Keep Active");
        M3Button freeze = M3Button.text("Freeze Plan");
        M3Dialog dialog = M3Dialog.basic("Freeze this membership?",
                new VBox(10,
                        new Label("Bookings are held and billing pauses for the freeze window."),
                        daysField),
                cancel, freeze);
        dialog.setDefaultAction(freeze::activate);
        freeze.setOnAction(e -> {
            Double daysValue = parseInRange(daysField, 1, 365);
            if (daysValue == null) {
                return;
            }
            int days = daysValue.intValue();
            dialog.hide();
            Data.submit(
                    () -> {
                        Membership current = new MembershipDao().findById(membership.getId());
                        if (current == null) {
                            throw new com.gym.exception.ValidationException(
                                    "Membership no longer exists in the database");
                        }
                        current.setStatus(MembershipStatus.FROZEN);
                        current.setFrozenDays(days);
                        new MembershipDao().update(current);
                        return current;
                    },
                    frozen -> {
                        overlay.showSnackbar("Membership frozen for " + days
                                + " days — billing and bookings paused.");
                        loadMember(loadedCustomer.getId());
                    },
                    error -> overlay.showSnackbar("Could not freeze: " + error.getMessage()));
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    /**
     * Rule 7 unfreeze through {@code MembershipDao.update}: the end date shifts forward by the
     * stored frozen days and the counter clears. Non-destructive, so no confirm.
     */
    public void onUnfreezeMembership() {
        Membership membership = loadedMembershipRef;
        if (membership == null) {
            overlay.showSnackbar("No membership on file for this member.");
            return;
        }
        Data.submit(
                () -> {
                    Membership current = new MembershipDao().findById(membership.getId());
                    if (current == null) {
                        throw new com.gym.exception.ValidationException(
                                "Membership no longer exists in the database");
                    }
                    if (current.getStatus() != MembershipStatus.FROZEN) {
                        throw new com.gym.exception.ValidationException(
                                "Rule 7: Only FROZEN memberships can be unfrozen");
                    }
                    LocalDate newEnd = LocalDate.parse(current.getEndDate())
                            .plusDays(current.getFrozenDays());
                    current.setEndDate(newEnd.toString());
                    current.setFrozenDays(0);
                    current.setStatus(MembershipStatus.ACTIVE);
                    new MembershipDao().update(current);
                    return current;
                },
                unfrozen -> {
                    overlay.showSnackbar("Membership reactivated — valid through "
                            + unfrozen.getEndDate() + " (the freeze window shifted the end date).");
                    loadMember(loadedCustomer.getId());
                },
                error -> overlay.showSnackbar("Could not unfreeze: " + error.getMessage()));
    }

    /**
     * Rule 7 renew through {@code MembershipDao.update}: extends the end date by the plan
     * window from today (an expired plan reactivates; a CANCELLED one is refused with the
     * rule copy).
     */
    public void onRenewMembership() {
        Membership membership = loadedMembershipRef;
        if (membership == null) {
            overlay.showSnackbar("No membership on file for this member.");
            return;
        }
        M3Button cancel = M3Button.text("Not Now");
        M3Button renew = M3Button.text("Renew " + planLabel(membership.getPlan()));
        M3Dialog dialog = M3Dialog.basic("Renew for another "
                        + planLabel(membership.getPlan()).toLowerCase(Locale.ROOT) + " cycle?",
                new Label("The validity window extends by one plan cycle from the current "
                        + "end date. Renewing a cancelled membership is refused (rule 7)."),
                cancel, renew);
        dialog.setDefaultAction(renew::activate);
        renew.setOnAction(e -> {
            dialog.hide();
            Data.submit(
                    () -> {
                        Membership current = new MembershipDao().findById(membership.getId());
                        if (current == null) {
                            throw new com.gym.exception.ValidationException(
                                    "Membership no longer exists in the database");
                        }
                        if (current.getStatus() == MembershipStatus.CANCELLED) {
                            throw new com.gym.exception.ValidationException(
                                    "Rule 7: Renewing a CANCELLED membership is refused");
                        }
                        LocalDate baseDate = LocalDate.parse(current.getEndDate());
                        if (baseDate.isBefore(LocalDate.now())) {
                            baseDate = LocalDate.now();
                        }
                        current.setEndDate(baseDate.plusDays(planDays(current.getPlan()))
                                .toString());
                        current.setStatus(MembershipStatus.ACTIVE);
                        new MembershipDao().update(current);
                        return current;
                    },
                    renewed -> {
                        overlay.showSnackbar("Membership renewed through "
                                + renewed.getEndDate() + ".");
                        loadMember(loadedCustomer.getId());
                    },
                    error -> overlay.showSnackbar("Could not renew: " + error.getMessage()));
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    /** Plan window length in days (mirrors the CLI's rule-7 arithmetic). */
    private static int planDays(PlanType plan) {
        return switch (plan == null ? PlanType.MONTHLY : plan) {
            case MONTHLY -> 30;
            case QUARTERLY -> 90;
            case YEARLY -> 365;
        };
    }

    /**
     * Rule 15 coach change through {@code CustomerDao.update}: the assigned trainer is the
     * authoritative link for the coach's roster and medical access. The menu lists the real
     * coach roster plus the unassign option.
     */
    public void onChangeTrainer() {
        if (loadedCustomer == null) {
            overlay.showSnackbar("No member loaded — open one from the member roster first.");
            return;
        }
        M3Menu menu = new M3Menu();
        for (Trainer trainer : trainerRoster) {
            menu.item(trainer.getFullName() + " — " + orEmpty(trainer.getSpecialization()),
                    () -> assignTrainer(trainer));
        }
        if (loadedCustomer.getTrainerId() != null) {
            menu.divider();
            menu.item("Unassign current coach", this::unassignTrainer);
        }
        showMenuBelow(btnChangeTrainer, menu, "Open this profile in the app window to pick a coach.");
    }

    /** Persists the assigned coach and re-queries the dossier. */
    private void assignTrainer(Trainer trainer) {
        Customer customer = loadedCustomer;
        Data.submit(
                () -> {
                    customer.setTrainerId(trainer.getId());
                    new CustomerDao().update(customer);
                    return trainer;
                },
                assigned -> {
                    overlay.showSnackbar("Assigned coach updated to "
                            + assigned.getFullName() + ".");
                    loadMember(customer.getId());
                },
                error -> overlay.showSnackbar("Could not assign coach: " + error.getMessage()));
    }

    /** Persists the coach unassignment (trainerId null) and re-queries the dossier. */
    private void unassignTrainer() {
        Customer customer = loadedCustomer;
        Data.submit(
                () -> {
                    customer.setTrainerId(null);
                    new CustomerDao().update(customer);
                    return customer;
                },
                updated -> {
                    overlay.showSnackbar("Coach unassigned for " + updated.getFullName() + ".");
                    loadMember(updated.getId());
                },
                error -> overlay.showSnackbar("Could not unassign coach: " + error.getMessage()));
    }

    // ------------------------------------------------------------------
    // Real data rendering
    // ------------------------------------------------------------------

    private void populateCapacityRadar() {
        radarAthleteCapacity.getAxes().addAll(
                "Strength", "Endurance", "Mobility", "Power", "Core", "Balance");
        radarAthleteCapacity.setMaxValue(100);
        radarAthleteCapacity.getSeriesList().add(
                M3DataSeries.of("Current capacity", 82, 64, 71, 88, 76, 69));
    }

    /** The holographic pass is the screen's hero: name, ID and status at a glance. */
    private void renderIdentityPass() {
        String name = txtFullName.getText() == null || txtFullName.getText().isBlank()
                ? (loadedCustomer == null ? "No member loaded" : loadedCustomer.getFullName())
                : txtFullName.getText().trim();
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("m3-list-headline");
        String membershipCopy = loadedMembershipRef == null ? "No membership on file"
                : "Membership " + badgeMembershipStatus.getText() + " · "
                        + planLabel(loadedMembershipRef.getPlan());
        Label meta = new Label(currentMemberId + " · " + membershipCopy);
        meta.getStyleClass().add("m3-list-supporting");
        meta.setWrapText(true);
        holographicPass.setContent(new VBox(6, nameLabel, meta));
    }

    /** The program dialog content: the record's own description and metadata. */
    private VBox programContent(WorkoutProgram program) {
        VBox box = new VBox(10);
        box.setPrefWidth(380);
        box.getChildren().add(listItem("stats", "Version", "v" + program.getVersion()
                + " · created " + orEmpty(program.getCreatedDate())));
        box.getChildren().add(listItem("event", "Sessions", orEmpty(program.getExercises())));
        box.getChildren().add(listItem("person", "Creator coach",
                "Coach #" + program.getCreatorTrainerId()));
        return box;
    }

    /** The medical dialog content: the real profile, or the on-demand rule-3 answer. */
    private VBox medicalSummary() {
        VBox box = new VBox(10);
        box.setPrefWidth(380);
        MedicalProfile medical = loadedCustomer == null ? null
                : loadMedicalQuietly(loadedCustomer.getId());
        if (medical == null) {
            box.getChildren().add(listItem("favorite", "No medical profile on file",
                    "It is created on demand (rule 3) — none is needed for gym-floor access."));
            return box;
        }
        box.getChildren().addAll(
                listItem("favorite", "Conditions", orEmpty(medical.getConditions())),
                listItem("info", "Allergies", orEmpty(medical.getAllergies())),
                listItem("person", "Attending physician",
                        orEmpty(medical.getDoctorName()) + " · " + orEmpty(medical.getDoctorPhone())),
                listItem("stats", "Notes", orEmpty(medical.getNotes())));
        return box;
    }

    /** Loads the member's medical profile without letting a failure break the dialog. */
    private MedicalProfile loadMedicalQuietly(int customerId) {
        try {
            return new MedicalProfileDao().findByCustomerId(customerId);
        } catch (Exception e) {
            return null;
        }
    }

    private M3ListItem listItem(String symbol, String headline, String supporting) {
        M3ListItem item = new M3ListItem(headline, supporting, M3ListItem.TWO_LINES);
        item.setLeading(M3Icon.symbol(symbol));
        return item;
    }

    /** Shows an anchored menu; without a window (headless preview) it answers via snackbar. */
    private void showMenuBelow(Node anchor, M3Menu menu, String fallback) {
        if (anchor.getScene() == null || anchor.getScene().getWindow() == null) {
            overlay.showSnackbar(fallback);
            return;
        }
        menu.showBelow(anchor);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Double parseInRange(M3TextField field, double min, double max) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            if (value >= min && value <= max) {
                field.setError(false);
                return value;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error state
        }
        field.setError(true);
        return null;
    }

    /**
     * Navigates by firing the shell's own pinned rail button (same pathway as clicking the
     * rail). Outside the shell — standalone previews and tests — it answers with a
     * snackbar instead of pretending to navigate. JavaFX 21 {@code ToggleButton.fire()}
     * toggles selection before firing the action, so an already-selected destination is
     * re-selected after the fire (a click on a rail button never deselects it).
     */
    private void navigateViaShellRail(String railButtonId, String fallbackText) {
        Node railButton = findInScene(railButtonId);
        if (railButton instanceof javafx.scene.control.ToggleButton button && !button.isDisabled()) {
            boolean wasSelected = button.isSelected();
            button.fire();
            if (wasSelected) {
                button.setSelected(true);
            }
            return;
        }
        overlay.showSnackbar(fallbackText);
    }

    private Node findInScene(String id) {
        if (rootPane.getScene() == null) {
            return null;
        }
        Parent sceneRoot = rootPane.getScene().getRoot();
        return sceneRoot == null ? null : findById(sceneRoot, id);
    }

    /** Id-based tree search (no CSS dependency, unlike Node.lookup). */
    private Node findById(Parent parent, String id) {
        if (id.equals(parent.getId())) {
            return parent;
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (id.equals(child.getId())) {
                return child;
            }
            if (child instanceof Parent childParent) {
                Node found = findById(childParent, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
