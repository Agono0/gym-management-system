package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.common.M3Divider;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.data.M3Stepper;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Clinical Medical &amp; Safety Console.
 * Conforms to docs/ui/members/MEDICAL_PROFILE_UI.md Section 7 contract.
 * Pure UI: strictly no backend service, DAO, or database logic.
 *
 * <p>UX contract: saving validates and syncs the emergency biometrics cards, Delete Record
 * is a two-step confirm whose snackbar offers Undo (which exercises the Rule 3 empty state),
 * allergy tags are real closable chips, and the 3D viewer has a working camera reset.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former medical-profile.fxml: emergency HUD, allergy alert card, clearance stepper, 3D
 * anatomy viewer, clinical dossier, on-demand empty state.
 */
public class MedicalProfileController implements ScreenController {

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane rootPane;
    private BorderPane mainLayout;
    private M3TopAppBar topAppBar;
    private M3IconButton btnBack;
    private M3Button btnDeleteProfile;
    private M3Button btnSaveProfile;
    private M3StatCard statBloodType;
    private M3StatCard statPhysician;
    private M3Card cardAllergiesAlert;
    private M3PulseBeacon beaconAllergyAlert;
    private FlowPane paneAllergyChips;
    private M3Button btnAddAllergy;
    private M3Stepper stepperClearance;
    private M3IconButton btnResetCamera;
    private M3AnatomyViewer3D anatomyViewer;
    private Label lblActiveInjuryDetail;
    private M3TextField txtBloodType;
    private M3TextField txtConditions;
    private M3TextField txtAllergies;
    private M3TextField txtMedications;
    private M3TextField txtInjuries;
    private M3TextField txtDoctorName;
    private M3TextField txtDoctorPhone;
    private TextArea txtNotes;
    private VBox emptyStateContainer;
    private M3Button btnCreateProfile;

    /** Overlay host for snackbars and dialogs, added to the screen root in code (no fx:id). */
    private final M3Overlay overlay = new M3Overlay();

    /**
     * Builds the complete Clinical Medical &amp; Safety Console and initializes all behavior.
     * Same ordering semantics the FXMLLoader used to provide: the node tree exists and every
     * field is assigned before the former {@code initialize()} work runs.
     */
    public MedicalProfileController() {
        buildView();
        rootPane.getChildren().add(overlay);

        prefillDemoRecord();
        labelClearanceStepper();
        makeAllergyChipsRemovable();
        // Pressing Enter in the allergies field tags the chip (no mouse detour needed).

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        rootPane.setId("rootPane");
        mainLayout.setId("mainLayout");
        topAppBar.setId("topAppBar");
        btnBack.setId("btnBack");
        btnDeleteProfile.setId("btnDeleteProfile");
        btnSaveProfile.setId("btnSaveProfile");
        statBloodType.setId("statBloodType");
        statPhysician.setId("statPhysician");
        cardAllergiesAlert.setId("cardAllergiesAlert");
        beaconAllergyAlert.setId("beaconAllergyAlert");
        paneAllergyChips.setId("paneAllergyChips");
        btnAddAllergy.setId("btnAddAllergy");
        stepperClearance.setId("stepperClearance");
        btnResetCamera.setId("btnResetCamera");
        anatomyViewer.setId("anatomyViewer");
        lblActiveInjuryDetail.setId("lblActiveInjuryDetail");
        txtBloodType.setId("txtBloodType");
        txtConditions.setId("txtConditions");
        txtAllergies.setId("txtAllergies");
        txtMedications.setId("txtMedications");
        txtInjuries.setId("txtInjuries");
        txtDoctorName.setId("txtDoctorName");
        txtDoctorPhone.setId("txtDoctorPhone");
        txtNotes.setId("txtNotes");
        emptyStateContainer.setId("emptyStateContainer");
        btnCreateProfile.setId("btnCreateProfile");
        txtAllergies.getInput().setOnAction(e -> onAddAllergyTag());
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for medical-profile.fxml)
     * and wires every handler.
     */
    private void buildView() {
        mainLayout = new BorderPane();

        // Top app bar.
        topAppBar = new M3TopAppBar("ATHLETIC MEDICAL & SAFETY CONSOLE",
                io.m3fx.controls.appbar.TopAppBarVariant.CENTER_ALIGNED);
        HBox.setHgrow(topAppBar, Priority.ALWAYS);
        btnBack = new M3IconButton(M3Icon.symbol("arrow_back"), IconButtonVariant.STANDARD);
        btnBack.setOnAction(e -> onNavigateBack());
        btnBack.setAccessibleText("Back to member roster");
        topAppBar.setNavigationIcon(btnBack);

        HBox appActions = new HBox(8);
        appActions.setAlignment(Pos.CENTER_RIGHT);
        appActions.setPadding(new Insets(0, 16, 0, 0));
        btnDeleteProfile = new M3Button("Delete Record", ButtonVariant.TEXT);
        btnDeleteProfile.setGraphic(M3Icon.symbol("delete"));
        btnDeleteProfile.setOnAction(e -> onDelete());
        btnSaveProfile = new M3Button("Save Profile", ButtonVariant.FILLED);
        btnSaveProfile.setGraphic(M3Icon.symbol("check"));
        btnSaveProfile.setOnAction(e -> onSave());
        appActions.getChildren().addAll(btnDeleteProfile, btnSaveProfile);

        HBox appBarRow = new HBox();
        appBarRow.setAlignment(Pos.CENTER_LEFT);
        appBarRow.getStyleClass().add("app-bar-container");
        appBarRow.getChildren().addAll(topAppBar, appActions);
        mainLayout.setTop(appBarRow);

        // Center scrollable console.
        HBox consoleRow = new HBox(20);
        consoleRow.setPadding(new Insets(20, 24, 64, 24));

        // Zone A: emergency HUD & allergies.
        VBox emergencyZone = new VBox(16);
        emergencyZone.setPrefWidth(380);
        emergencyZone.setMinWidth(360);

        M3Card biometricsCard = new M3Card(CardVariant.ELEVATED);
        VBox biometricsContent = new VBox(12);
        biometricsContent.setPadding(new Insets(16));
        biometricsContent.getChildren().add(styledLabel("EMERGENCY BIOMETRICS", "title-small"));
        HBox biometricsRow = new HBox(12);
        statBloodType = new M3StatCard("BLOOD TYPE", "O+", "Rh Positive");
        statPhysician = new M3StatCard("ATTENDING MD", "Dr. Vance", "01055443322");
        HBox.setHgrow(statBloodType, Priority.ALWAYS);
        HBox.setHgrow(statPhysician, Priority.ALWAYS);
        biometricsRow.getChildren().addAll(statBloodType, statPhysician);
        biometricsContent.getChildren().add(biometricsRow);
        biometricsCard.setContent(biometricsContent);

        // Critical allergies & warnings alert card.
        cardAllergiesAlert = new M3Card(CardVariant.OUTLINED);
        cardAllergiesAlert.getStyleClass().add("allergy-alert-card");
        VBox allergyContent = new VBox(12);
        allergyContent.setPadding(new Insets(16));
        HBox allergyHeader = new HBox(8);
        allergyHeader.setAlignment(Pos.CENTER_LEFT);
        beaconAllergyAlert = new M3PulseBeacon();
        beaconAllergyAlert.setStatus(M3PulseBeacon.BeaconStatus.CRITICAL);
        beaconAllergyAlert.setAccessibleText("Allergy alert status: critical");
        allergyHeader.getChildren().addAll(
                beaconAllergyAlert,
                styledLabel("CRITICAL ALLERGIES & WARNINGS", "title-small"));
        paneAllergyChips = new FlowPane(8, 8);
        paneAllergyChips.getChildren().addAll(
                new M3Chip("Penicillin", ChipVariant.INPUT),
                new M3Chip("NSAIDs (Mild)", ChipVariant.INPUT));
        btnAddAllergy = new M3Button("+ Add Allergy Tag", ButtonVariant.TONAL);
        btnAddAllergy.setOnAction(e -> onAddAllergyTag());
        allergyContent.getChildren().addAll(allergyHeader, paneAllergyChips, btnAddAllergy);
        cardAllergiesAlert.setContent(allergyContent);

        // Exercise clearance stepper card.
        M3Card clearanceCard = new M3Card(CardVariant.OUTLINED);
        VBox clearanceContent = new VBox(12);
        clearanceContent.setPadding(new Insets(16));
        clearanceContent.getChildren().add(styledLabel("EXERCISE CLEARANCE STAGE", "title-small"));
        stepperClearance = new M3Stepper();
        stepperClearance.setCurrentStep(3);
        clearanceContent.getChildren().add(stepperClearance);
        clearanceCard.setContent(clearanceContent);

        emergencyZone.getChildren().addAll(biometricsCard, cardAllergiesAlert, clearanceCard);

        // Zone B: 3D anatomy viewer.
        VBox anatomyZone = new VBox(16);
        anatomyZone.setPrefWidth(460);
        HBox.setHgrow(anatomyZone, Priority.ALWAYS);
        M3Card anatomyCard = new M3Card(CardVariant.ELEVATED);
        VBox anatomyContent = new VBox(12);
        anatomyContent.setAlignment(Pos.CENTER);
        anatomyContent.setPadding(new Insets(16));
        HBox anatomyHeader = new HBox();
        anatomyHeader.setAlignment(Pos.CENTER_LEFT);
        btnResetCamera = new M3IconButton(M3Icon.symbol("refresh"), IconButtonVariant.STANDARD);
        btnResetCamera.setOnAction(e -> onReset3DCamera());
        btnResetCamera.setAccessibleText("Reset 3D camera to standard pose");
        Region anatomySpacer = new Region();
        HBox.setHgrow(anatomySpacer, Priority.ALWAYS);
        anatomyHeader.getChildren().addAll(
                styledLabel("3D MUSCULOSKELETAL INJURY MAPPING", "title-small"),
                anatomySpacer,
                btnResetCamera);
        anatomyViewer = new M3AnatomyViewer3D();
        anatomyViewer.setPrefSize(420, 480);
        lblActiveInjuryDetail = new Label("Active Hotspot: Right Patellar Tendinitis");
        lblActiveInjuryDetail.getStyleClass().add("body-small");
        anatomyContent.getChildren().addAll(anatomyHeader, anatomyViewer, lblActiveInjuryDetail);
        anatomyCard.setContent(anatomyContent);
        anatomyZone.getChildren().add(anatomyCard);

        // Zone C: clinical dossier.
        VBox dossierZone = new VBox(16);
        dossierZone.setPrefWidth(420);
        dossierZone.setMinWidth(380);
        M3Card dossierCard = new M3Card(CardVariant.OUTLINED);
        VBox dossierContent = new VBox(14);
        dossierContent.setPadding(new Insets(16));
        dossierContent.getChildren().add(styledLabel("CLINICAL DOSSIER & PROTOCOLS", "title-small"));
        txtBloodType = outlinedField("Blood Type", "e.g., A+, O-, B+");
        txtConditions = outlinedField("Chronic Conditions", "Comma-separated");
        txtAllergies = outlinedField("Allergies & Contraindications",
                "Press Enter or + Add Allergy Tag to chip it");
        txtMedications = outlinedField("Active Medications & Dosages", "Drug, dose, frequency");
        txtInjuries = outlinedField("Musculoskeletal Injuries History",
                "Year + body area, most recent first");
        txtDoctorName = outlinedField("Doctor Full Name",
                "Shown on the emergency biometrics card");
        txtDoctorPhone = outlinedField("Doctor Emergency Phone", "Digits only, e.g. 01055443322");
        txtNotes = new TextArea();
        txtNotes.setPrefRowCount(4);
        txtNotes.setWrapText(true);
        txtNotes.setPromptText("Enter detailed physical rehabilitation notes...");
        dossierContent.getChildren().addAll(
                txtBloodType, txtConditions, txtAllergies, txtMedications, txtInjuries,
                new M3Divider(),
                styledLabel("ATTENDING PHYSICIAN", "label-large"),
                txtDoctorName, txtDoctorPhone,
                new M3Divider(),
                styledLabel("CLINICAL REHAB NOTES", "label-large"),
                txtNotes);
        dossierCard.setContent(dossierContent);
        dossierZone.getChildren().add(dossierCard);

        consoleRow.getChildren().addAll(emergencyZone, anatomyZone, dossierZone);
        ScrollPane consoleScroll = new ScrollPane(consoleRow);
        consoleScroll.setFitToWidth(true);
        consoleScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        consoleScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainLayout.setCenter(consoleScroll);

        // On-demand empty state overlay pane (Rule 3).
        emptyStateContainer = new VBox(16);
        emptyStateContainer.setAlignment(Pos.CENTER);
        emptyStateContainer.getStyleClass().add("empty-state-pane");
        M3Icon emptyHeroIcon = M3Icon.symbol("favorite");
        emptyHeroIcon.getStyleClass().add("large-hero-icon");
        btnCreateProfile = new M3Button("Initialize Medical Profile", ButtonVariant.FILLED);
        btnCreateProfile.setOnAction(e -> onCreateOnDemand());
        emptyStateContainer.getChildren().addAll(
                emptyHeroIcon,
                styledLabel("No Medical Profile Created Yet", "headline-medium"),
                styledLabel("Athletes do not have medical records by default (UML Rule 3). Create one on demand.",
                        "body-large"),
                btnCreateProfile);
        emptyStateContainer.setVisible(false);

        rootPane = new StackPane();
        rootPane.getStyleClass().add("medical-profile-root");
        rootPane.getChildren().addAll(mainLayout, emptyStateContainer);
    }

    /** One M3TextField with an outlined variant, label and supporting text. */
    private static M3TextField outlinedField(String labelText, String supportingText) {
        M3TextField field = new M3TextField(labelText, TextFieldVariant.OUTLINED);
        field.setSupportingText(supportingText);
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
     * Delete Record: destructive, so it confirms first. Confirming swaps to the Rule 3
     * "no medical profile" empty state and the snackbar offers Undo.
     */
    public void onDelete() {
        M3Button cancel = M3Button.text("Keep Record");
        M3Button delete = M3Button.text("Delete Record");
        M3Dialog dialog = M3Dialog.basic("Delete this medical record?",
                new Label("The clinical dossier, allergy tags and injury mapping are removed "
                        + "from the console. Undo stays available right after the delete."),
                cancel, delete);
        dialog.setDefaultAction(delete::activate);
        delete.setOnAction(e -> {
            dialog.hide();
            swapToEmptyState();
            overlay.showSnackbar("Medical record deleted — the console is now empty.", "Undo", () -> {
                restoreConsole();
                overlay.showSnackbar("Deletion undone — the medical record is back.");
            });
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    /**
     * Saves the clinical dossier: field errors name the problem, success syncs the
     * emergency biometrics cards and answers via snackbar.
     */
    public void onSave() {
        String bloodType = txtBloodType.getText() == null ? "" : txtBloodType.getText().trim();
        String doctor = txtDoctorName.getText() == null ? "" : txtDoctorName.getText().trim();
        boolean bloodOk = bloodType.matches("(A|B|AB|O)[+-]");
        boolean doctorOk = !doctor.isEmpty();
        txtBloodType.setError(!bloodOk);
        txtDoctorName.setError(!doctorOk);
        if (!bloodOk || !doctorOk) {
            overlay.showSnackbar("Fix the highlighted fields, then save again.");
            return;
        }
        statBloodType.setValue(bloodType);
        statBloodType.setDeltaText(bloodType.endsWith("+") ? "Rh Positive" : "Rh Negative");
        statPhysician.setValue(doctor);
        overlay.showSnackbar("Medical profile saved for " + doctor + "'s care.");
    }

    /** Adds an allergy tag chip from the field; duplicates and blanks are called out. */
    public void onAddAllergyTag() {
        String tag = txtAllergies.getText();
        if (tag == null || tag.isBlank()) {
            txtAllergies.setError(true);
            overlay.showSnackbar("Type an allergy first, then add the tag.");
            return;
        }
        String cleaned = tag.trim();
        if (containsTag(cleaned)) {
            overlay.showSnackbar("\"" + cleaned + "\" is already tagged.");
            return;
        }
        paneAllergyChips.getChildren().add(createAllergyChip(cleaned));
        txtAllergies.setError(false);
        txtAllergies.setText("");
    }

    /** Resets the 3D camera pose via the viewer's own API. */
    public void onReset3DCamera() {
        anatomyViewer.resetCamera();
        overlay.showSnackbar("3D view reset to the standard camera pose.");
    }

    /** Rule 3: profile is created on demand — swap the empty state for the console. */
    public void onCreateOnDemand() {
        restoreConsole();
        overlay.showSnackbar("Medical profile initialized — fill the clinical dossier.");
    }

    // ------------------------------------------------------------------
    // Demo record, chips and layout swaps
    // ------------------------------------------------------------------

    private void prefillDemoRecord() {
        txtBloodType.setText("O+");
        txtConditions.setText("Mild asthma");
        txtMedications.setText("Salbutamol inhaler (as needed)");
        txtInjuries.setText("Right patellar tendinitis (2026-03)");
        txtDoctorName.setText("Dr. Elena Vance");
        txtDoctorPhone.setText("01055443322");
        txtNotes.setText("Stage 3 rehab: pain-free knee-dominant loading only. "
                + "Re-assess patellar response after each session.");
    }

    private void labelClearanceStepper() {
        stepperClearance.getStepTitles().addAll(
                "Physician Screening", "Injury Assessment", "Rehab Plan", "Return to Play");
    }

    /** Allergy chips become closable with a working remove callback. */
    private void makeAllergyChipsRemovable() {
        for (Node node : paneAllergyChips.getChildren()) {
            if (node instanceof M3Chip chip) {
                chip.setOnClose(() -> paneAllergyChips.getChildren().remove(chip));
            }
        }
    }

    private boolean containsTag(String tag) {
        for (Node node : paneAllergyChips.getChildren()) {
            if (node instanceof M3Chip chip && tag.equalsIgnoreCase(chip.getText())) {
                return true;
            }
        }
        return false;
    }

    private M3Chip createAllergyChip(String tag) {
        M3Chip chip = new M3Chip(tag, ChipVariant.INPUT);
        chip.setClosable(true);
        chip.setOnClose(() -> paneAllergyChips.getChildren().remove(chip));
        return chip;
    }

    private void swapToEmptyState() {
        emptyStateContainer.setVisible(true);
        emptyStateContainer.setManaged(true);
        mainLayout.setVisible(false);
        mainLayout.setManaged(false);
    }

    private void restoreConsole() {
        emptyStateContainer.setVisible(false);
        emptyStateContainer.setManaged(false);
        mainLayout.setVisible(true);
        mainLayout.setManaged(true);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Navigates by firing the shell's own pinned rail button (same pathway as clicking the
     * rail). Outside the shell — standalone previews and tests — it answers with a
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
