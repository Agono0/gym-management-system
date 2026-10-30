package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.data.M3Stepper;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D;
import io.m3fx.controls.textfield.M3TextField;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Clinical Medical & Safety Console (medical-profile.fxml).
 * Conforms to docs/ui/members/MEDICAL_PROFILE_UI.md Section 7 contract.
 * Pure UI: strictly no backend service, DAO, or database logic.
 *
 * <p>UX contract: saving validates and syncs the emergency biometrics cards, Delete Record
 * is a two-step confirm whose snackbar offers Undo (which exercises the Rule 3 empty state),
 * allergy tags are real closable chips, and the 3D viewer has a working camera reset.
 */
public class MedicalProfileController {

    @FXML private StackPane rootPane;
    @FXML private BorderPane mainLayout;
    @FXML private M3TopAppBar topAppBar;
    @FXML private M3IconButton btnBack;
    @FXML private M3Button btnDeleteProfile;
    @FXML private M3Button btnSaveProfile;

    @FXML private M3StatCard statBloodType;
    @FXML private M3StatCard statPhysician;
    @FXML private M3Card cardAllergiesAlert;
    @FXML private M3PulseBeacon beaconAllergyAlert;
    @FXML private FlowPane paneAllergyChips;
    @FXML private M3Button btnAddAllergy;
    @FXML private M3Stepper stepperClearance;

    @FXML private M3IconButton btnResetCamera;
    @FXML private M3AnatomyViewer3D anatomyViewer;
    @FXML private Label lblActiveInjuryDetail;

    @FXML private M3TextField txtBloodType;
    @FXML private M3TextField txtConditions;
    @FXML private M3TextField txtAllergies;
    @FXML private M3TextField txtMedications;
    @FXML private M3TextField txtInjuries;
    @FXML private M3TextField txtDoctorName;
    @FXML private M3TextField txtDoctorPhone;
    @FXML private TextArea txtNotes;

    @FXML private VBox emptyStateContainer;
    @FXML private M3Button btnCreateProfile;

    /** Overlay host for snackbars and dialogs, added to the screen root in code (no fx:id). */
    private M3Overlay overlay;

    public MedicalProfileController() {
        // Public no-arg constructor for FXMLLoader
    }

    @FXML
    public void initialize() {
        overlay = new M3Overlay();
        rootPane.getChildren().add(overlay);

        prefillDemoRecord();
        labelClearanceStepper();
        makeAllergyChipsRemovable();
        // Pressing Enter in the allergies field tags the chip (no mouse detour needed).
        if (txtAllergies != null) {
            txtAllergies.getInput().setOnAction(e -> onAddAllergyTag());
        }
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
     * Delete Record: destructive, so it confirms first. Confirming swaps to the Rule 3
     * "no medical profile" empty state and the snackbar offers Undo.
     */
    @FXML
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
    @FXML
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
    @FXML
    public void onAddAllergyTag() {
        String tag = txtAllergies != null ? txtAllergies.getText() : null;
        if (tag == null || tag.isBlank()) {
            if (txtAllergies != null) {
                txtAllergies.setError(true);
                overlay.showSnackbar("Type an allergy first, then add the tag.");
            }
            return;
        }
        String cleaned = tag.trim();
        if (paneAllergyChips != null && containsTag(cleaned)) {
            overlay.showSnackbar("\"" + cleaned + "\" is already tagged.");
            return;
        }
        if (paneAllergyChips != null) {
            paneAllergyChips.getChildren().add(createAllergyChip(cleaned));
        }
        if (txtAllergies != null) {
            txtAllergies.setError(false);
            txtAllergies.setText("");
        }
    }

    /** Resets the 3D camera pose via the viewer's own API. */
    @FXML
    public void onReset3DCamera() {
        if (anatomyViewer != null) {
            anatomyViewer.resetCamera();
        }
        overlay.showSnackbar("3D view reset to the standard camera pose.");
    }

    /** Rule 3: profile is created on demand — swap the empty state for the console. */
    @FXML
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

    /** FXML allergy chips become closable with a working remove callback. */
    private void makeAllergyChipsRemovable() {
        if (paneAllergyChips == null) {
            return;
        }
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
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisible(true);
            emptyStateContainer.setManaged(true);
        }
        if (mainLayout != null) {
            mainLayout.setVisible(false);
            mainLayout.setManaged(false);
        }
    }

    private void restoreConsole() {
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisible(false);
            emptyStateContainer.setManaged(false);
        }
        if (mainLayout != null) {
            mainLayout.setVisible(true);
            mainLayout.setManaged(true);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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
