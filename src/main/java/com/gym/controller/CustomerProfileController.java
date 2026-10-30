package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.chart.M3RadarChart;
import io.m3fx.controls.common.M3Badge;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.list.M3ListItem;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.scene3d.M3TiltCard3D;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Athlete 360 VIP Dossier (customer-profile.fxml).
 * Conforms to docs/ui/members/CUSTOMER_PROFILE_UI.md Section 7 contract.
 * Pure UI: strictly no backend service, DAO, or database logic.
 *
 * <p>UX contract: saving validates with inline field errors plus a snackbar answer,
 * Freeze is a two-step confirm, plan/coach/program changes visibly update the dossier
 * (badges, labels, capacity radar), and the demo biometrics render in the stat cards and
 * radar chart instead of placeholder dashes.
 */
public class CustomerProfileController {

    @FXML private StackPane rootPane;
    @FXML private BorderPane mainLayout;
    @FXML private M3TopAppBar topAppBar;
    @FXML private M3IconButton btnBack;
    @FXML private M3Button btnSaveProfile;

    @FXML private M3TiltCard3D holographicPass;
    @FXML private M3StatCard statWeight;
    @FXML private M3StatCard statBodyFat;
    @FXML private M3StatCard statTarget;
    @FXML private M3StatCard statVbt;
    @FXML private M3Button btnLogBiometric;

    @FXML private M3Card cardMedicalAlert;
    @FXML private Label lblMedicalSummary;
    @FXML private M3Button btnOpenMedical;

    @FXML private M3RadarChart radarAthleteCapacity;
    @FXML private M3Badge badgeProgramVersion;
    @FXML private Label lblProgramName;
    @FXML private Label lblProgramDetails;
    @FXML private M3Button btnViewProgram;
    @FXML private M3Button btnAssignProgram;

    @FXML private M3Badge badgeMembershipStatus;
    @FXML private Label lblPlanType;
    @FXML private Label lblPlanDates;
    @FXML private M3Button btnFreezePlan;
    @FXML private M3Button btnUnfreezePlan;
    @FXML private M3Button btnRenewPlan;

    @FXML private Label lblTrainerName;
    @FXML private Label lblTrainerSpecialization;
    @FXML private M3Button btnChangeTrainer;

    @FXML private M3TextField txtFullName;
    @FXML private M3TextField txtPhone;
    @FXML private M3TextField txtEmail;
    @FXML private M3TextField txtAddress;
    @FXML private M3TextField txtDateOfBirth;
    @FXML private M3TextField txtEmergencyName;
    @FXML private M3TextField txtEmergencyPhone;

    /** Overlay host for snackbars and dialogs, added to the screen root in code (no fx:id). */
    private M3Overlay overlay;
    /** True while the membership badge shows FROZEN; drives the Freeze/Unfreeze pair. */
    private boolean membershipFrozen;
    /** Name of the currently assigned demo program, or {@code null} when unassigned. */
    private String assignedProgram;

    public CustomerProfileController() {
        // Public no-arg constructor for FXMLLoader
    }

    @FXML
    public void initialize() {
        overlay = new M3Overlay();
        rootPane.getChildren().add(overlay);

        prefillDemoDossier();
        populateCapacityRadar();
        renderIdentityPass();
        // The plan starts ACTIVE, so Unfreeze has nothing to do yet (Hick's law: only the
        // meaningful half of the pair is offered).
        btnUnfreezePlan.setDisable(true);
    }

    // ------------------------------------------------------------------
    // Event handlers (answers for every visible action)
    // ------------------------------------------------------------------

    /** Returns to the member roster via the shell rail; answers with a hint if standalone. */
    @FXML
    public void onNavigateBack() {
        navigateViaShellRail("navCustomers",
                "Open the member roster from the navigation rail (this preview is standalone).");
    }

    /**
     * Saves the dossier: inline field errors name the problem, a snackbar confirms success
     * and the identity pass re-renders with the saved name.
     */
    @FXML
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
    @FXML
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
    @FXML
    public void onOpenMedicalProfile() {
        M3Button close = M3Button.text("Close");
        M3Dialog dialog = M3Dialog.basic("Medical & safety summary", medicalSummary(), close);
        close.setOnAction(e -> dialog.hide());
        dialog.setDefaultAction(close::activate);
        overlay.showDialog(dialog);
    }

    /** Shows the assigned program's sessions; without a program it says exactly that. */
    @FXML
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
    @FXML
    public void onAssignProgram() {
        M3Menu menu = new M3Menu();
        menu.item("Hypertrophy Base (6 weeks)", () -> assignProgram("Hypertrophy Base"));
        menu.item("VBT Strength Peak (4 weeks)", () -> assignProgram("VBT Strength Peak"));
        menu.item("Return-to-Play Rehab (stage 3)", () -> assignProgram("Return-to-Play Rehab"));
        showMenuBelow(btnAssignProgram, menu, "Open this profile in the app window to pick a program.");
    }

    /** Freezing pauses billing and bookings — destructive, so it confirms first. */
    @FXML
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
    @FXML
    public void onUnfreezeMembership() {
        membershipFrozen = false;
        badgeMembershipStatus.setText("ACTIVE");
        lblPlanType.setText("Plan: Monthly Standard");
        btnFreezePlan.setDisable(false);
        btnUnfreezePlan.setDisable(true);
        overlay.showSnackbar("Membership reactivated — billing resumes on the next cycle.");
    }

    /** Renews through a confirm dialog and visibly extends the validity window. */
    @FXML
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
    @FXML
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
        Label meta = new Label("M-1007 · Membership ACTIVE · Monthly Standard");
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
    private void showMenuBelow(javafx.scene.Node anchor, M3Menu menu, String fallback) {
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
     * rail). Outside the shell — standalone previews and FXML tests — it answers with a
     * snackbar instead of pretending to navigate.
     */
    private void navigateViaShellRail(String railButtonId, String fallbackText) {
        Node railButton = findInScene(railButtonId);
        if (railButton instanceof ButtonBase button && !button.isDisabled()) {
            button.fire();
            return;
        }
        overlay.showSnackbar(fallbackText);
    }

    private Node findInScene(String id) {
        if (rootPane == null || rootPane.getScene() == null) {
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
