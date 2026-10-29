package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.data.M3Stepper;
import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller skeleton for Clinical Medical & Safety Console (medical-profile.fxml).
 * Conforms to docs/ui/members/MEDICAL_PROFILE_UI.md Section 7 contract.
 * Pure UI stub: strictly no backend service, DAO, or database logic.
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

    public MedicalProfileController() {
        // Public no-arg constructor for FXMLLoader
    }

    @FXML
    public void initialize() {
        // UI initialization stub
    }

    @FXML
    public void onNavigateBack() {
        // UI event handler stub
    }

    @FXML
    public void onDelete() {
        // UI event handler stub
    }

    @FXML
    public void onSave() {
        // UI event handler stub
    }

    @FXML
    public void onAddAllergyTag() {
        String tag = txtAllergies != null ? txtAllergies.getText() : null;
        if (tag == null || tag.isBlank()) {
            if (txtAllergies != null) {
                txtAllergies.setError(true);
            }
            return;
        }
        if (paneAllergyChips != null) {
            M3Chip chip = new M3Chip(tag.trim(), ChipVariant.INPUT);
            chip.setClosable(true);
            chip.setOnClose(() -> paneAllergyChips.getChildren().remove(chip));
            paneAllergyChips.getChildren().add(chip);
        }
        if (txtAllergies != null) {
            txtAllergies.setError(false);
            txtAllergies.setText("");
        }
    }

    @FXML
    public void onReset3DCamera() {
        // UI event handler stub
    }

    @FXML
    public void onCreateOnDemand() {
        // Rule 3: profile is created on demand — swap the empty state for the console
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisible(false);
            emptyStateContainer.setManaged(false);
        }
        if (mainLayout != null) {
            mainLayout.setVisible(true);
            mainLayout.setManaged(true);
        }
    }
}
