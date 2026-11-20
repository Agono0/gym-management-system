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
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import com.gym.dao.CustomerDao;
import com.gym.dao.MedicalProfileDao;
import com.gym.model.Customer;
import com.gym.model.MedicalProfile;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.scene.layout.VBox;

/**
 * Controller for the Clinical Medical &amp; Safety Console on real data (C9).
 * Conforms to docs/ui/members/MEDICAL_PROFILE_UI.md Section 7 and the com.gym.ui foundation
 * contracts (app-ui-foundation.md section 11).
 *
 * <p>Rule 3: zero-or-one medical profile per customer, created on demand. A customer picker
 * (default: the most recently joined member) loads their record or the designed empty state;
 * Save maps the dossier fields onto {@code MedicalProfileDao.insert/update} (the DAO's
 * ValidationException surfaces verbatim, rule 3 duplicates included); allergy tags are
 * closable chips rendered from the record's comma-separated allergies field and persisted
 * back on save; the CRITICAL beacon reflects real allergy data; Delete Record confirms and
 * calls the DAO delete with a real Undo (the captured record is re-inserted).
 * The 3D anatomy viewer stays presentational and carries the record's injury notes.
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
    private M3Button btnCustomerPicker;

    /** Overlay host for snackbars and dialogs, added to the screen root in code (no fx:id). */
    private final M3Overlay overlay = new M3Overlay();

    // ---- Loaded state (C9) ----
    private StackPane mainStateHost;
    private List<Customer> customers = List.of();
    private Map<Integer, MedicalProfile> profileByCustomer = Map.of();
    private Customer selectedCustomer;
    /** The loaded record for the selected customer; {@code null} = rule-3 empty. */
    private MedicalProfile currentProfile;

    /**
     * Builds the complete Clinical Medical &amp; Safety Console and initializes all behavior.
     * Same ordering semantics the FXMLLoader used to provide: the node tree exists and every
     * field is assigned before the former {@code initialize()} work runs.
     */
    public MedicalProfileController() {
        buildView();
        rootPane.getChildren().add(overlay);

        labelClearanceStepper();
        makeAllergyChipsRemovable();
        loadMedical();
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
        btnCustomerPicker = customerPickerButton();
        appActions.getChildren().addAll(btnCustomerPicker, btnDeleteProfile, btnSaveProfile);

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

        // The console stays mounted through every load (loadAsync swaps inside this host);
        // the rule-3 empty state overlays it.
        mainStateHost = new StackPane(mainLayout);

        rootPane = new StackPane();
        rootPane.getStyleClass().add("medical-profile-root");
        rootPane.getChildren().addAll(mainStateHost, emptyStateContainer);
    }

    /** The customer selector: a button opening an M3Menu over the loaded members. */
    private M3Button customerPickerButton() {
        M3Button picker = new M3Button("Select member", ButtonVariant.OUTLINED);
        picker.getStyleClass().add("customer-picker-btn");
        picker.setOnAction(e -> {
            M3Menu menu = new M3Menu();
            for (Customer customer : customers) {
                menu.item(customer.getFullName(), () -> selectCustomer(customer));
            }
            if (customers.isEmpty()) {
                menu.item("No members on record", () -> { });
            }
            menu.showBelow(picker);
        });
        return picker;
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

    // ------------------------------------------------------------------
    // Data loading (CustomerDao + MedicalProfileDao)
    // ------------------------------------------------------------------

    /**
     * Loads members and their medical profiles on a background task; defaults the selection to
     * the most recently joined member (highest id) and renders the record or the rule-3 empty
     * state for them.
     */
    public void loadMedical() {
        Data.loadAsync(mainStateHost,
                () -> {
                    Data.ensureReady();
                    List<Customer> loadedCustomers = new CustomerDao().findAll();
                    Map<Integer, MedicalProfile> profiles = new MedicalProfileDao().findAll()
                            .stream().collect(Collectors.toMap(
                                    MedicalProfile::getCustomerId, Function.identity()));
                    return new MedicalSnapshot(loadedCustomers, profiles);
                },
                snapshot -> {
                    customers = snapshot.customers();
                    profileByCustomer = snapshot.profiles();
                    if (selectedCustomer != null) {
                        Customer kept = customers.stream()
                                .filter(c -> c.getId() == selectedCustomer.getId())
                                .findFirst().orElse(null);
                        selectCustomer(kept);
                        return;
                    }
                    // Default: the most recently joined member (highest id).
                    Customer defaultCustomer = customers.isEmpty()
                            ? null : customers.get(customers.size() - 1);
                    selectCustomer(defaultCustomer);
                },
                error -> {
                    swapToEmptyState();
                    emptyStateContainer.getChildren().setAll(
                            UiKit.errorState(error.getMessage(), this::loadMedical));
                });
    }

    /** One load's worth of DAO data. */
    private record MedicalSnapshot(List<Customer> customers,
                                   Map<Integer, MedicalProfile> profiles) {
    }

    /** Selects a member and renders their record or the rule-3 empty state. */
    private void selectCustomer(Customer customer) {
        selectedCustomer = customer;
        btnCustomerPicker.setText(customer == null
                ? "Select member" : "Member: " + customer.getFullName());
        if (customer == null) {
            swapToEmptyState();
            return;
        }
        currentProfile = profileByCustomer.get(customer.getId());
        if (currentProfile == null) {
            clearDossierFields();
            swapToEmptyState();
            return;
        }
        restoreConsole();
        applyProfileToFields(currentProfile);
        refreshEmergencyCards(currentProfile);
    }

    /** Clears the dossier fields for a fresh rule-3 profile. */
    private void clearDossierFields() {
        txtBloodType.setText("");
        txtConditions.setText("");
        txtAllergies.setText("");
        paneAllergyChips.getChildren().clear();
        txtMedications.setText("");
        txtInjuries.setText("");
        txtDoctorName.setText("");
        txtDoctorPhone.setText("");
        txtNotes.setText("");
    }

    /** Maps a loaded record onto the dossier fields, allergy chips and emergency cards. */
    private void applyProfileToFields(MedicalProfile profile) {
        txtBloodType.setText(profile.getBloodType() == null ? "" : profile.getBloodType());
        txtConditions.setText(nvl(profile.getConditions()));
        txtAllergies.setText("");
        paneAllergyChips.getChildren().clear();
        for (String tag : splitAllergies(profile.getAllergies())) {
            paneAllergyChips.getChildren().add(createAllergyChip(tag));
        }
        txtMedications.setText(nvl(profile.getMedications()));
        txtInjuries.setText(nvl(profile.getInjuries()));
        txtDoctorName.setText(nvl(profile.getDoctorName()));
        txtDoctorPhone.setText(nvl(profile.getDoctorPhone()));
        txtNotes.setText(nvl(profile.getNotes()));
    }

    /** Syncs the emergency HUD cards and the allergy beacon from real record data. */
    private void refreshEmergencyCards(MedicalProfile profile) {
        String bloodType = profile.getBloodType() == null ? "" : profile.getBloodType();
        statBloodType.setValue(bloodType.isEmpty() ? "—" : bloodType);
        statBloodType.setDeltaText(bloodType.endsWith("+") ? "Rh Positive"
                : bloodType.endsWith("-") ? "Rh Negative" : "Not recorded");
        String doctor = nvl(profile.getDoctorName());
        statPhysician.setValue(doctor.isEmpty() ? "—" : doctor);
        statPhysician.setDeltaText(nvl(profile.getDoctorPhone()));

        List<String> allergies = splitAllergies(profile.getAllergies());
        if (allergies.isEmpty() || (allergies.size() == 1
                && allergies.get(0).equalsIgnoreCase("None"))) {
            beaconAllergyAlert.setStatus(M3PulseBeacon.BeaconStatus.ONLINE);
            beaconAllergyAlert.setAccessibleText("Allergy alert status: normal");
            lblActiveInjuryDetail.setText(profile.getInjuries() == null
                    || profile.getInjuries().isBlank()
                    || "None".equalsIgnoreCase(profile.getInjuries())
                    ? "No injury notes on record"
                    : "Injury notes: " + profile.getInjuries());
        } else {
            beaconAllergyAlert.setStatus(M3PulseBeacon.BeaconStatus.CRITICAL);
            beaconAllergyAlert.setAccessibleText(
                    "Allergy alert status: critical — " + allergies.size() + " tagged");
            lblActiveInjuryDetail.setText(nvl(profile.getInjuries()));
        }
    }

    /** Splits the record's comma-separated allergy field into clean tag strings. */
    private static List<String> splitAllergies(String allergies) {
        if (allergies == null || allergies.isBlank()
                || "None".equalsIgnoreCase(allergies.trim())) {
            return List.of();
        }
        return List.of(allergies.split(",\s*"));
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    /**
     * Delete Record: destructive, so it confirms first and then calls the DAO delete; the
     * snackbar offers a real Undo (the captured record is re-inserted through the DAO).
     */
    public void onDelete() {
        if (currentProfile == null || selectedCustomer == null) {
            overlay.showSnackbar("No medical record to delete for this member (rule 3).");
            return;
        }
        MedicalProfile doomed = currentProfile;
        String memberName = selectedCustomer.getFullName();
        M3Button cancel = M3Button.text("Keep Record");
        M3Button delete = M3Button.text("Delete Record");
        M3Dialog dialog = M3Dialog.basic("Delete this medical record?",
                new Label(memberName + "'s clinical dossier, allergy tags and injury mapping "
                        + "are removed permanently. Undo stays available right after the delete."),
                cancel, delete);
        dialog.setDefaultAction(delete::activate);
        delete.setOnAction(e -> {
            dialog.hide();
            MedicalProfileDao dao = new MedicalProfileDao();
            Data.submit(
                    () -> {
                        dao.delete(doomed.getId());
                        return doomed;
                    },
                    deleted -> {
                        swapToEmptyState();
                        overlay.showSnackbar(
                                "Medical record deleted — the console is now empty.",
                                "Undo", () -> {
                                    MedicalProfileDao restoreDao = new MedicalProfileDao();
                                    Data.submit(
                                            () -> restoreDao.insert(deleted),
                                            restored -> {
                                                selectCustomer(selectedCustomer);
                                                overlay.showSnackbar(
                                                        "Deletion undone — the record is back.");
                                            },
                                            error -> overlay.showSnackbar(
                                                    "Could not undo: " + error.getMessage()));
                                });
                    },
                    error -> overlay.showSnackbar(
                            "Could not delete: " + error.getMessage()));
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    /**
     * Saves the clinical dossier as a real record: field errors name the problem, the DAO's
     * verdict (rule 3 duplicates, phone format) surfaces verbatim, and success syncs the
     * emergency cards from the freshly written row.
     */
    public void onSave() {
        if (selectedCustomer == null) {
            overlay.showSnackbar("Select a member first, then save their profile.");
            return;
        }
        String bloodType = txtBloodType.getText() == null ? "" : txtBloodType.getText().trim();
        String doctor = txtDoctorName.getText() == null ? "" : txtDoctorName.getText().trim();
        String doctorPhone = txtDoctorPhone.getText() == null
                ? "" : txtDoctorPhone.getText().trim();
        boolean bloodOk = bloodType.matches("(A|B|AB|O)[+-]");
        boolean doctorOk = !doctor.isEmpty();
        boolean phoneOk = !doctorPhone.isEmpty();
        txtBloodType.setError(!bloodOk);
        txtDoctorName.setError(!doctorOk);
        txtDoctorPhone.setError(!phoneOk);
        if (!bloodOk || !doctorOk || !phoneOk) {
            overlay.showSnackbar("Fix the highlighted fields, then save again.");
            return;
        }
        List<String> allergies = new java.util.ArrayList<>();
        for (Node node : paneAllergyChips.getChildren()) {
            if (node instanceof M3Chip chip) {
                allergies.add(chip.getText());
            }
        }
        boolean inserting = currentProfile == null;
        MedicalProfileDao dao = new MedicalProfileDao();
        MedicalProfile target = inserting
                ? new MedicalProfile(0, selectedCustomer.getId(), bloodType,
                        txtConditions.getText().trim(), String.join(", ", allergies),
                        txtMedications.getText().trim(), txtInjuries.getText().trim(),
                        doctor, doctorPhone, txtNotes.getText().trim(),
                        LocalDate.now().toString())
                : currentProfile;
        if (!inserting) {
            target.setBloodType(bloodType);
            target.setConditions(txtConditions.getText().trim());
            target.setAllergies(String.join(", ", allergies));
            target.setMedications(txtMedications.getText().trim());
            target.setInjuries(txtInjuries.getText().trim());
            target.setDoctorName(doctor);
            target.setDoctorPhone(doctorPhone);
            target.setNotes(txtNotes.getText().trim());
            target.setUpdatedDate(LocalDate.now().toString());
        }
        Data.submit(
                () -> {
                    if (inserting) {
                        return dao.insert(target);
                    }
                    dao.update(target);
                    return target;
                },
                saved -> {
                    currentProfile = saved;
                    profileByCustomer.put(selectedCustomer.getId(), saved);
                    refreshEmergencyCards(saved);
                    overlay.showSnackbar("Medical profile saved for " + memberLabel() + ".");
                },
                error -> overlay.showSnackbar(
                        "Could not save: " + error.getMessage()));
    }

    private String memberLabel() {
        return selectedCustomer == null ? "the member" : selectedCustomer.getFullName();
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
        clearDossierFields();
        restoreConsole();
        overlay.showSnackbar("Medical profile initialized for "
                + memberLabel() + " — fill the clinical dossier and save.");
    }

    // ------------------------------------------------------------------
    // Demo record, chips and layout swaps
    // ------------------------------------------------------------------

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
