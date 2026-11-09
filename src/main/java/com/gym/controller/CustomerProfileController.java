package com.gym.controller;

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

/**
 * Controller for the Athlete 360 VIP Dossier.
 * Conforms to docs/ui/members/CUSTOMER_PROFILE_UI.md Section 7 contract.
 * Pure UI: strictly no backend service, DAO, or database logic.
 *
 * <p>UX contract: saving validates with inline field errors plus a snackbar answer,
 * Freeze is a two-step confirm, plan/coach/program changes visibly update the dossier
 * (badges, labels, capacity radar), and the demo biometrics render in the stat cards and
 * radar chart instead of placeholder dashes.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former customer-profile.fxml: identity/biometrics zone, performance zone, lifecycle/dossier
 * zone.
 */
public class CustomerProfileController implements ScreenController {

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

    /** Overlay host for snackbars and dialogs, added to the screen root in code (no fx:id). */
    private final M3Overlay overlay = new M3Overlay();
    /** True while the membership badge shows FROZEN; drives the Freeze/Unfreeze pair. */
    private boolean membershipFrozen;
    /** Name of the currently assigned demo program, or {@code null} when unassigned. */
    private String assignedProgram;
    /** Member identity on the holographic pass ("M-1007" until the roster handoff replaces it). */
    private String currentMemberId = "M-1007";

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
        statWeight = new M3StatCard("WEIGHT", "-- kg", null);
        statBodyFat = new M3StatCard("BODY FAT", "-- %", null);
        statTarget = new M3StatCard("TARGET", "-- kg", null);
        statVbt = new M3StatCard("VBT 1RM", "-- kg", null);
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
        txtPhone = outlinedField("Phone Number", "Format: 01xxxxxxxxx");
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
        mainLayout.setCenter(cockpitScroll);

        rootPane = new StackPane();
        rootPane.getStyleClass().add("customer-profile-root");
        rootPane.getChildren().addAll(mainLayout, overlay);

        prefillDemoDossier();

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
     * Prefills the dossier from the roster selection (the "Open Full Profile" handoff in
     * {@code CustomerListController}): the name field and the holographic pass identity. Demo
     * facts the roster does not carry keep their dossier values.
     */
    public void showMember(String memberName, String memberId) {
        if (memberName != null && !memberName.isBlank()) {
            txtFullName.setText(memberName.trim());
        }
        if (memberId != null && !memberId.isBlank()) {
            currentMemberId = memberId.trim();
        }
        renderIdentityPass();
    }

    /**
     * Saves the dossier: inline field errors name the problem, a snackbar confirms success
     * and the identity pass re-renders with the saved name.
     */
    public void onSave() {
        String name = txtFullName.getText() == null ? "" : txtFullName.getText().trim();
        String phone = txtPhone.getText() == null ? "" : txtPhone.getText().trim();
        boolean nameOk = !name.isEmpty();
        boolean phoneOk = phone.matches("01\\d{9}");
        txtFullName.setError(!nameOk);
        txtPhone.setError(!phoneOk);
        if (!nameOk || !phoneOk) {
            overlay.showSnackbar("Fix the highlighted fields, then save again.");
            return;
        }
        renderIdentityPass();
        overlay.showSnackbar("Profile changes saved for " + name + ".");
    }

    /** Logs a new biometric reading through a dialog and updates the telemetry cards. */
    public void onLogBiometric() {
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
            statWeight.setValue(String.format("%.1f kg", weight));
            statBodyFat.setValue(String.format("%.1f %%", bodyFat));
            double lean = weight * (1 - bodyFat / 100.0);
            statTarget.setValue(String.format("%.1f kg", lean));
            overlay.showSnackbar(String.format(
                    "Reading saved — %.1f kg at %.1f%% body fat.", weight, bodyFat));
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

    /** Shows the assigned program's sessions; without a program it says exactly that. */
    public void onViewProgram() {
        if (assignedProgram == null) {
            overlay.showSnackbar("No program assigned yet — use Assign Program first.");
            return;
        }
        M3Button close = M3Button.text("Close");
        M3Dialog dialog = M3Dialog.basic(assignedProgram, programContent(assignedProgram), close);
        close.setOnAction(e -> dialog.hide());
        dialog.setDefaultAction(close::activate);
        overlay.showDialog(dialog);
    }

    /** Assigns one of the demo programs; the card and badge update in place. */
    public void onAssignProgram() {
        M3Menu menu = new M3Menu();
        menu.item("Hypertrophy Base (6 weeks)", () -> assignProgram("Hypertrophy Base"));
        menu.item("VBT Strength Peak (4 weeks)", () -> assignProgram("VBT Strength Peak"));
        menu.item("Return-to-Play Rehab (stage 3)", () -> assignProgram("Return-to-Play Rehab"));
        showMenuBelow(btnAssignProgram, menu, "Open this profile in the app window to pick a program.");
    }

    /** Freezing pauses billing and bookings — destructive, so it confirms first. */
    public void onFreezeMembership() {
        M3Button cancel = M3Button.text("Keep Active");
        M3Button freeze = M3Button.text("Freeze Plan");
        M3Dialog dialog = M3Dialog.basic("Freeze this membership?",
                new Label("Bookings are held, billing pauses, and the athlete keeps gym-floor "
                        + "access until the next billing date."),
                cancel, freeze);
        dialog.setDefaultAction(freeze::activate);
        freeze.setOnAction(e -> {
            dialog.hide();
            membershipFrozen = true;
            badgeMembershipStatus.setText("FROZEN");
            lblPlanType.setText("Plan: Monthly Standard (frozen)");
            btnFreezePlan.setDisable(true);
            btnUnfreezePlan.setDisable(false);
            overlay.showSnackbar("Membership frozen — billing and bookings paused.");
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    /** Unfreezing restores the plan immediately (non-destructive: no confirm needed). */
    public void onUnfreezeMembership() {
        membershipFrozen = false;
        badgeMembershipStatus.setText("ACTIVE");
        lblPlanType.setText("Plan: Monthly Standard");
        btnFreezePlan.setDisable(false);
        btnUnfreezePlan.setDisable(true);
        overlay.showSnackbar("Membership reactivated — billing resumes on the next cycle.");
    }

    /** Renews through a confirm dialog and visibly extends the validity window. */
    public void onRenewMembership() {
        M3Button cancel = M3Button.text("Not Now");
        M3Button renew = M3Button.text("Renew 12 Months");
        M3Dialog dialog = M3Dialog.basic("Renew for another 12 months?",
                new Label("The validity window extends by one year from the current end date "
                        + "at the Monthly Standard rate."),
                cancel, renew);
        dialog.setDefaultAction(renew::activate);
        renew.setOnAction(e -> {
            dialog.hide();
            lblPlanDates.setText("Valid: 2026-01-01 -> 2027-12-31");
            overlay.showSnackbar("Membership renewed through 2027-12-31.");
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    /** Picks a coach from the demo roster; the coach card updates in place. */
    public void onChangeTrainer() {
        M3Menu menu = new M3Menu();
        menu.item("Coach Sarah — HIIT & Conditioning", () -> changeTrainer("Coach Sarah", "HIIT & Conditioning"));
        menu.item("Coach Marcus — Strength & Powerlifting", () -> changeTrainer("Coach Marcus", "Strength & Powerlifting"));
        menu.item("Coach Elena — Mobility & Rehab", () -> changeTrainer("Coach Elena", "Mobility & Rehab"));
        menu.item("Unassign current coach", () -> changeTrainer("Unassigned", "No specialization"));
        showMenuBelow(btnChangeTrainer, menu, "Open this profile in the app window to pick a coach.");
    }

    // ------------------------------------------------------------------
    // Demo data rendering
    // ------------------------------------------------------------------

    private void prefillDemoDossier() {
        txtFullName.setText("Layla Hassan");
        txtPhone.setText("01005558877");
        txtEmail.setText("layla.h@mailbox.com");
        txtAddress.setText("14 El-Nasr St, Nasr City");
        txtDateOfBirth.setText("1998-04-12");
        txtEmergencyName.setText("Yara Hassan (sister)");
        txtEmergencyPhone.setText("01098761234");
        statWeight.setValue("84.2 kg");
        statBodyFat.setValue("13.8 %");
        statTarget.setValue("80.0 kg");
        statVbt.setValue("142.5 kg");
    }

    private void populateCapacityRadar() {
        radarAthleteCapacity.getAxes().addAll(
                "Strength", "Endurance", "Mobility", "Power", "Core", "Balance");
        radarAthleteCapacity.setMaxValue(100);
        radarAthleteCapacity.getSeriesList().add(
                M3DataSeries.of("Current capacity", 82, 64, 71, 88, 76, 69));
    }

    /** The holographic pass is the screen's hero: name, ID and status at a glance. */
    private void renderIdentityPass() {
        Label name = new Label(txtFullName.getText().isBlank() ? "Unnamed athlete" : txtFullName.getText().trim());
        name.getStyleClass().add("m3-list-headline");
        Label meta = new Label(currentMemberId + " · Membership ACTIVE · Monthly Standard");
        meta.getStyleClass().add("m3-list-supporting");
        meta.setWrapText(true);
        holographicPass.setContent(new VBox(6, name, meta));
    }

    private void assignProgram(String program) {
        assignedProgram = program;
        badgeProgramVersion.setText("v1");
        lblProgramName.setText(program);
        lblProgramDetails.setText("Assigned just now — 3 sessions per week, reviewed by the head coach.");
        overlay.showSnackbar(program + " assigned. View Workout now shows the sessions.");
    }

    private VBox programContent(String program) {
        VBox box = new VBox(10);
        box.setPrefWidth(380);
        String[][] sessions = switch (program) {
            case "Hypertrophy Base" -> new String[][] {
                    {"Day A — Upper", "Bench 4x8 @ RIR 2 · Row 4x8 · Pull-up 3xAMRAP"},
                    {"Day B — Lower", "Squat 4x8 @ RIR 2 · RDL 3x10 · Split squat 3x10"},
                    {"Day C — Full", "Deadlift 3x5 · Press 3x8 · Core circuit 3 rounds"}};
            case "VBT Strength Peak" -> new String[][] {
                    {"Day A — Velocity", "Back squat 5x3 @ 0.55 m/s · Auto-regulate by VBT"},
                    {"Day B — Peak", "Bench 5x2 @ 0.45 m/s · Trap-bar jump 4x3"},
                    {"Day C — Deload", "Squat 3x3 @ 0.65 m/s · Mobility flow"}};
            default -> new String[][] {
                    {"Stage 3 — Knee-dominant", "Leg press 3x12 pain-free · Step-down 3x10"},
                    {"Stage 3 — Posterior", "Hip hinge 3x12 · Nordic curl 2x6 assisted"},
                    {"Stage 3 — Capacity", "Bike 15 min Z2 · Stretching circuit"}};
        };
        for (String[] session : sessions) {
            box.getChildren().add(new M3ListItem(session[0], session[1], M3ListItem.TWO_LINES));
        }
        return box;
    }

    private VBox medicalSummary() {
        VBox box = new VBox(10);
        box.setPrefWidth(380);
        box.getChildren().addAll(
                listItem("favorite", "Contraindications", "None recorded — cleared for full training."),
                listItem("info", "Allergies", "Penicillin · NSAIDs (mild)"),
                listItem("person", "Attending physician", "Dr. Elena Vance · 01055443322"),
                listItem("stats", "Clearance stage", "Stage 3 of 4 — Return to sport conditioning"));
        return box;
    }

    private M3ListItem listItem(String symbol, String headline, String supporting) {
        M3ListItem item = new M3ListItem(headline, supporting, M3ListItem.TWO_LINES);
        item.setLeading(M3Icon.symbol(symbol));
        return item;
    }

    private void changeTrainer(String name, String specialization) {
        lblTrainerName.setText(name);
        lblTrainerSpecialization.setText(specialization);
        overlay.showSnackbar("Assigned coach updated to " + name + ".");
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
