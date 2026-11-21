package com.gym.controller;

import com.gym.dao.AdministratorDao;
import com.gym.model.Administrator;
import com.gym.model.StaffRole;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.common.M3Tooltip;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.search.M3SearchBar;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.textfield.M3TextField;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Controller for Staff RBAC Matrix &amp; Security Administration screen (ACT-19-STAFF), running
 * on {@link AdministratorDao} (rule 16): the roster table is a real {@code findAll} snapshot,
 * row activation selects the administrator the toolbar acts on (edit profile, password reset,
 * disable/enable, delete), Add Staff inserts through the DAO with the role from the
 * {@link StaffRole} enum, and Disable/Enable flips the active flag with the rule-16 copy
 * (inactive accounts cannot sign in — the same guard the login gate's
 * {@code Data.authenticate} applies). Passwords are stored as the DAO stores them and are
 * never displayed. The audit timeline is an honest empty state: the current data model does
 * not record administrator actions, and none are invented.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former staff.fxml: top app bar, roster/workspace split, override side sheet.
 */
public class StaffController implements ScreenController {

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private M3TextField txtSearchStaff;
    private M3DataTable<Administrator> staffDataTable;
    private M3Timeline auditTimeline;
    private M3SideSheet overrideSideSheet;
    private Label lblSideSheetTitle;
    private VBox sideSheetFormContainer;
    private M3Button btnTopAddStaff;
    private M3Button btnEditStaffAction;
    private M3Button btnResetPasswordAction;
    private M3Button btnToggleActiveAction;
    private M3Button btnDeleteStaffAction;
    private M3Button btnReassignAction;
    private M3Button btnAdjustPaymentAction;
    private Label lblSelectedStaff;
    /** The viewer badge: real signed-in identity once the shell publishes it. */
    private Label roleBadge;
    /** Content host around the roster table for the Data.loadAsync loading/error states. */
    private StackPane rosterHost;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** The administrator the toolbar currently acts on (set by activating a roster row). */
    private Administrator selectedAdmin;

    /** Fields of the currently open override form, read by Execute Override. */
    private M3TextField overrideFieldA;
    private M3TextField overrideFieldB;

    /** What the open override sheet is for (add / edit / reassign / payment). */
    private String pendingOverride;

    /** The administrators from the last successful load (search operates on this list). */
    private final List<Administrator> admins = new ArrayList<>();

    /**
     * Builds the complete Staff &amp; Access Governance view and initializes all behavior. Same
     * ordering semantics the FXMLLoader used to provide: the node tree exists and every field is
     * assigned before the former {@code initialize()} work runs.
     */
    public StaffController() {
        viewRoot = buildView();

        installFeedbackLayer(txtSearchStaff);
        populateStaffTable();
        loadStaff();
        wireSearch();
        // B/C7: the viewer badge shows the real signed-in identity once the shell publishes
        // itself on the scene root (SHELL_CONTROLLER_KEY); standalone previews (no gate
        // sign-in) keep the honest "not signed in" fallback.
        txtSearchStaff.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null && scene.getRoot().getProperties()
                    .get(AppShellController.SHELL_CONTROLLER_KEY)
                    instanceof AppShellController shell) {
                String name = shell.getSignedInName();
                if (name == null || name.isBlank()) {
                    roleBadge.setText("ROLE: NOT SIGNED IN");
                } else {
                    StaffRole role = shell.getSignedInRole() == null
                            ? StaffRole.STAFF : shell.getSignedInRole();
                    roleBadge.setText("ROLE: " + role + " (" + name + ")");
                }
            }
        });
    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        txtSearchStaff.setId("txtSearchStaff");
        staffDataTable.setId("staffDataTable");
        auditTimeline.setId("auditTimeline");
        overrideSideSheet.setId("overrideSideSheet");
        lblSideSheetTitle.setId("lblSideSheetTitle");
        sideSheetFormContainer.setId("sideSheetFormContainer");
        btnTopAddStaff.setId("btnTopAddStaff");
        btnEditStaffAction.setId("btnEditStaffAction");
        btnResetPasswordAction.setId("btnResetPasswordAction");
        btnToggleActiveAction.setId("btnToggleActiveAction");
        btnDeleteStaffAction.setId("btnDeleteStaffAction");
        btnReassignAction.setId("btnReassignAction");
        btnAdjustPaymentAction.setId("btnAdjustPaymentAction");

        // B/C7: the audit timeline is an honest empty state — the current data model does not
        // record administrator actions, and none are invented here.
        auditTimeline.addItem("—", "No audit trail recorded yet",
                "Administrator actions are not captured by the current data model.",
                M3Timeline.Status.PENDING);
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for staff.fxml) and wires
     * every handler.
     */
    private StackPane buildView() {
        // Primary base layout.
        BorderPane basePane = new BorderPane();
        basePane.getStyleClass().add("staff-base-pane");

        // Top app bar block.
        VBox topBarContainer = new VBox(8);
        topBarContainer.getStyleClass().add("top-bar-container");

        M3TopAppBar topAppBar = new M3TopAppBar("Staff & Access Governance",
                io.m3fx.controls.appbar.TopAppBarVariant.SMALL);
        topAppBar.setNavigationIcon(M3Icon.symbol("arrow_back"));

        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(4, 24, 12, 24));
        btnTopAddStaff = new M3Button("Add Staff", io.m3fx.controls.button.ButtonVariant.FILLED);
        btnTopAddStaff.setOnAction(e -> onAddStaff());
        Label topCaption = new Label("Role-based access, audit trail and administrative overrides");
        topCaption.getStyleClass().add("body-medium");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        roleBadge = new Label("ROLE: NOT SIGNED IN");
        roleBadge.getStyleClass().add("badge-admin");
        topRow.getChildren().addAll(topCaption, topSpacer, roleBadge, btnTopAddStaff);
        topBarContainer.getChildren().addAll(topAppBar, topRow);
        basePane.setTop(topBarContainer);

        // Main workspace split: left roster pane, right overrides & audit feed pane.
        VBox rosterPane = new VBox(16);
        rosterPane.setPadding(new Insets(16, 16, 64, 16));

        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        txtSearchStaff = new M3TextField("Search staff members...", io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        HBox.setHgrow(txtSearchStaff, Priority.ALWAYS);
        searchRow.getChildren().add(txtSearchStaff);

        rosterHost = new StackPane();
        VBox.setVgrow(rosterHost, Priority.ALWAYS);
        staffDataTable = new M3DataTable<>();
        // B/C7: activating a roster row selects the administrator the toolbar acts on.
        staffDataTable.setOnRowAction(this::selectStaffRow);
        rosterHost.getChildren().add(staffDataTable);

        lblSelectedStaff = new Label("Select a staff row to enable the actions below.");
        lblSelectedStaff.getStyleClass().add("body-small");
        lblSelectedStaff.setWrapText(true);

        HBox rosterToolbar = new HBox(10);
        rosterToolbar.setAlignment(Pos.CENTER_RIGHT);
        btnEditStaffAction = new M3Button("Edit Profile", ButtonVariant.TONAL);
        btnEditStaffAction.getStyleClass().add("m3-button-tonal");
        btnEditStaffAction.setOnAction(e -> onEditStaffClick());
        btnResetPasswordAction = new M3Button("Reset Password", ButtonVariant.OUTLINED);
        btnResetPasswordAction.getStyleClass().add("m3-button-outlined");
        btnResetPasswordAction.setOnAction(e -> onResetPasswordClick());
        btnToggleActiveAction = new M3Button("Disable", ButtonVariant.OUTLINED);
        btnToggleActiveAction.getStyleClass().add("m3-button-outlined");
        btnToggleActiveAction.setOnAction(e -> onToggleActiveClick());
        btnDeleteStaffAction = new M3Button("Delete Staff", ButtonVariant.TEXT);
        btnDeleteStaffAction.getStyleClass().add("m3-button-text-danger");
        btnDeleteStaffAction.setOnAction(e -> onDeleteStaffClick());
        // Honest disabled state: without a selected row the toolbar cannot act on anything,
        // so the buttons start disabled (the library's disabled styling makes the hint above
        // true) and every button carries a tooltip explaining why.
        for (M3Button action : List.of(btnEditStaffAction, btnResetPasswordAction,
                btnToggleActiveAction, btnDeleteStaffAction)) {
            action.setDisable(true);
            Tooltip.install(action, M3Tooltip.plain("Select a staff row first — the action"
                    + " targets the selected row."));
        }
        rosterToolbar.getChildren().addAll(
                btnEditStaffAction, btnResetPasswordAction, btnToggleActiveAction, btnDeleteStaffAction);
        rosterPane.getChildren().addAll(searchRow, rosterHost, lblSelectedStaff, rosterToolbar);

        VBox feedPane = new VBox(16);
        feedPane.setPadding(new Insets(16, 16, 64, 16));

        VBox overrideCommandCard = new VBox(12);
        overrideCommandCard.getStyleClass().add("override-command-card");
        overrideCommandCard.getChildren().add(
                styledLabel("ADMINISTRATIVE OVERRIDE COMMANDS", "title-medium"));
        HBox overrideButtons = new HBox(10);
        btnReassignAction = new M3Button("Reassign Athlete Coach", ButtonVariant.TONAL);
        btnReassignAction.getStyleClass().add("m3-button-tonal");
        btnReassignAction.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnReassignAction, Priority.ALWAYS);
        btnReassignAction.setOnAction(e -> onReassign());
        btnAdjustPaymentAction = new M3Button("Adjust Payment Record", ButtonVariant.OUTLINED);
        btnAdjustPaymentAction.getStyleClass().add("m3-button-outlined");
        btnAdjustPaymentAction.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnAdjustPaymentAction, Priority.ALWAYS);
        btnAdjustPaymentAction.setOnAction(e -> onAdjustPaymentClick());
        overrideButtons.getChildren().addAll(btnReassignAction, btnAdjustPaymentAction);
        overrideCommandCard.getChildren().add(overrideButtons);

        VBox auditFeed = new VBox(8);
        auditFeed.getChildren().add(
                styledLabel("CHRONOLOGICAL AUDIT & SECURITY TIMELINE", "label-small"));
        auditTimeline = new M3Timeline();
        VBox.setVgrow(auditTimeline, Priority.ALWAYS);
        auditFeed.getChildren().add(auditTimeline);
        VBox.setVgrow(auditFeed, Priority.ALWAYS);

        feedPane.getChildren().addAll(overrideCommandCard, auditFeed);

        SplitPane workspaceSplit = new SplitPane(rosterPane, feedPane);
        workspaceSplit.setDividerPositions(0.55);
        basePane.setCenter(workspaceSplit);

        // Contextual override side sheet.
        overrideSideSheet = new M3SideSheet();
        overrideSideSheet.setVisible(false);
        overrideSideSheet.setPrefWidth(380);
        StackPane.setAlignment(overrideSideSheet, Pos.CENTER_RIGHT);

        VBox sheetContent = new VBox(16);
        sheetContent.setPadding(new Insets(20));

        HBox sheetHeader = new HBox();
        sheetHeader.setAlignment(Pos.CENTER_LEFT);
        lblSideSheetTitle = new Label("Administrative Override");
        lblSideSheetTitle.getStyleClass().add("headline-small");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        M3Button closeBtn = new M3Button("Close", ButtonVariant.TEXT);
        closeBtn.setOnAction(e -> onCloseSideSheetClick());
        sheetHeader.getChildren().addAll(lblSideSheetTitle, headerSpacer, closeBtn);

        sideSheetFormContainer = new VBox(12);
        VBox.setVgrow(sideSheetFormContainer, Priority.ALWAYS);

        HBox sheetActions = new HBox(10);
        sheetActions.setAlignment(Pos.CENTER_RIGHT);
        M3Button cancelBtn = new M3Button("Cancel", ButtonVariant.OUTLINED);
        cancelBtn.getStyleClass().add("m3-button-outlined");
        cancelBtn.setOnAction(e -> onCloseSideSheetClick());
        M3Button executeBtn = new M3Button("Execute Override", ButtonVariant.FILLED);
        executeBtn.getStyleClass().add("m3-button-filled");
        executeBtn.setOnAction(e -> onExecuteOverride());
        sheetActions.getChildren().addAll(cancelBtn, executeBtn);

        sheetContent.getChildren().addAll(
                sheetHeader,
                styledLabel("Configure entity parameters and review audit implications.", "body-small"),
                sideSheetFormContainer,
                sheetActions);
        overrideSideSheet.setContent(sheetContent);

        StackPane root = new StackPane();
        root.getStyleClass().add("staff-root");
        root.getChildren().addAll(basePane, overrideSideSheet);
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

    /** Builds the roster columns from the real administrator fields. */
    private void populateStaffTable() {
        if (!staffDataTable.getColumns().isEmpty()) {
            return;
        }
        staffDataTable.getColumns().addAll(List.of(
                new M3DataTable.Column<>("ID", a -> String.valueOf(a.getId()), 60),
                new M3DataTable.Column<>("Name", Administrator::getFullName, 170),
                new M3DataTable.Column<>("Username", Administrator::getUsername, 130),
                new M3DataTable.Column<>("Role", a -> roleName(a), 110),
                new M3DataTable.Column<>("Status",
                        a -> a.isActive() ? "ACTIVE" : "INACTIVE", 100),
                new M3DataTable.Column<>("Phone",
                        a -> a.getPhone() == null ? "" : a.getPhone(), 140)));
    }

    // ------------------------------------------------------------------
    // Snapshot (the single FX-touching step is applyStaffSnapshot)
    // ------------------------------------------------------------------

    /** Re-queries the administrator roster from {@link AdministratorDao} and applies it. */
    private void loadStaff() {
        Data.loadAsync(rosterHost,
                () -> {
                    Data.ensureReady();
                    return new AdministratorDao().findAll();
                },
                loaded -> {
                    admins.clear();
                    admins.addAll(loaded);
                    if (selectedAdmin != null) {
                        for (Administrator admin : loaded) {
                            if (admin.getId() == selectedAdmin.getId()) {
                                selectedAdmin = admin;
                                break;
                            }
                        }
                    }
                    applyStaffSnapshot();
                },
                error -> rosterHost.getChildren().setAll(UiKit.errorState(
                        error.getMessage() == null ? error.toString() : error.getMessage(),
                        this::loadStaff)));
    }

    /** The only FX-touching step of a load: fills the roster and the selection hint. */
    private void applyStaffSnapshot() {
        refreshRoster();
        updateSelectionHint();
        refreshToggleLabel();
        refreshToolbarEnabled();
    }

    /** Row activation: selects the administrator the toolbar acts on. */
    private void selectStaffRow(Administrator admin) {
        selectedAdmin = admin;
        updateSelectionHint();
        refreshToggleLabel();
        refreshToolbarEnabled();
    }

    /** The selection hint under the roster, so the acting target is never a guess. */
    private void updateSelectionHint() {
        if (selectedAdmin == null) {
            lblSelectedStaff.setText("Select a staff row to enable the actions below.");
        } else {
            lblSelectedStaff.setText("Selected: #" + selectedAdmin.getId() + " "
                    + selectedAdmin.getFullName() + " (" + roleName(selectedAdmin) + ", "
                    + (selectedAdmin.isActive() ? "ACTIVE" : "INACTIVE") + ")");
        }
    }

    /** The disable/enable button follows the selected administrator's active flag. */
    private void refreshToggleLabel() {
        if (selectedAdmin != null) {
            btnToggleActiveAction.setText(selectedAdmin.isActive() ? "Disable" : "Enable");
        } else {
            btnToggleActiveAction.setText("Disable");
        }
    }

    /** The roster toolbar only acts on a selected row: disabled when none is selected. */
    private void refreshToolbarEnabled() {
        boolean hasSelection = selectedAdmin != null;
        btnEditStaffAction.setDisable(!hasSelection);
        btnResetPasswordAction.setDisable(!hasSelection);
        btnToggleActiveAction.setDisable(!hasSelection);
        btnDeleteStaffAction.setDisable(!hasSelection);
    }

    private static String roleName(Administrator admin) {
        return admin.getRole() == null ? "—" : admin.getRole().name();
    }

    /** Wires the labeled search field to filter the visible roster live. */
    private void wireSearch() {
        // txtSearchStaff is this screen's labeled search affordance; the table's built-in
        // unlabeled search bar would duplicate it, so it is not shown here.
        staffDataTable.getChildren().removeIf(n -> n instanceof M3SearchBar);
        txtSearchStaff.getInput().textProperty().addListener((obs, o, query) -> refreshRoster());
    }

    /** Re-applies the current search query after the roster data changed. */
    private void refreshRoster() {
        String q = txtSearchStaff.getText() != null
                ? txtSearchStaff.getText().trim().toLowerCase(Locale.ROOT)
                : "";
        List<Administrator> visible = new ArrayList<>();
        for (Administrator admin : admins) {
            String haystack = (admin.getFullName() + " " + admin.getUsername() + " "
                    + roleName(admin) + " " + admin.getPhone()).toLowerCase(Locale.ROOT);
            if (q.isEmpty() || haystack.contains(q)) {
                visible.add(admin);
            }
        }
        staffDataTable.getItems().setAll(visible);
    }

    // ------------------------------------------------------------------
    // Event handlers (administrator CRUD via AdministratorDao, rule 16)
    // ------------------------------------------------------------------

    /** Opens the Add Staff dialog (full name, username, password, phone, role). */
    public void onAddStaff() {
        Label title = new Label("New staff account (rule 16): the account signs in with the "
                + "username and password below.");
        title.setWrapText(true);
        M3TextField nameField = new M3TextField("Full name", io.m3fx.controls.textfield.TextFieldVariant.OUTLINED);
        M3TextField usernameField = new M3TextField("Username (3+ chars, no spaces)",
                io.m3fx.controls.textfield.TextFieldVariant.OUTLINED);
        M3TextField passwordField = new M3TextField("Password", io.m3fx.controls.textfield.TextFieldVariant.OUTLINED);
        M3TextField phoneField = new M3TextField("Phone (01xxxxxxxxx)", io.m3fx.controls.textfield.TextFieldVariant.OUTLINED);
        M3TextField roleField = new M3TextField("Role: ADMIN, MANAGER or STAFF",
                io.m3fx.controls.textfield.TextFieldVariant.OUTLINED);
        roleField.setText("STAFF");
        VBox form = new VBox(10, title,
                UiKit.formRow("Full name", nameField),
                UiKit.formRow("Username", usernameField),
                UiKit.formRow("Password", passwordField),
                UiKit.formRow("Phone", phoneField),
                UiKit.formRow("Role", roleField));
        form.setPrefWidth(420);
        M3Button cancel = M3Button.text("Cancel");
        M3Button create = M3Button.text("Create Account");
        M3Dialog dialog = M3Dialog.basic("Add Staff Member", form, cancel, create);
        dialog.setDefaultAction(create::activate);
        cancel.setOnAction(e -> dialog.hide());
        create.setOnAction(e -> {
            Administrator admin = new Administrator(0,
                    nameField.getText() == null ? "" : nameField.getText().trim(),
                    usernameField.getText() == null ? "" : usernameField.getText().trim(),
                    passwordField.getText() == null ? "" : passwordField.getText(),
                    parseRole(roleField.getText()),
                    phoneField.getText() == null ? "" : phoneField.getText().trim(),
                    "", true, LocalDate.now().toString());
            Data.submit(
                    () -> new AdministratorDao().insert(admin),
                    inserted -> {
                        dialog.hide();
                        selectedAdmin = inserted;
                        loadStaff();
                        feedback.showSnackbar("Staff account created: " + inserted.getFullName()
                                + " (" + roleName(inserted) + ")");
                    },
                    error -> {
                        String message = error.getMessage() == null
                                ? error.toString() : error.getMessage();
                        markFieldError(nameField, usernameField, passwordField, phoneField,
                                roleField, message);
                        feedback.showSnackbar(message);
                    });
        });
        feedback.showDialog(dialog);
    }

    /** Maps the role text onto the {@link StaffRole} enum, defaulting to STAFF. */
    private static StaffRole parseRole(String text) {
        String trimmed = text == null ? "" : text.trim().toUpperCase(Locale.ROOT);
        for (StaffRole role : StaffRole.values()) {
            if (role.name().equals(trimmed)) {
                return role;
            }
        }
        return StaffRole.STAFF;
    }

    /** Surfaces a DAO validation message on the field whose label the message names. */
    private static void markFieldError(M3TextField nameField, M3TextField usernameField,
                                       M3TextField passwordField, M3TextField phoneField,
                                       M3TextField roleField, String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        M3TextField target = lower.contains("username") ? usernameField
                : lower.contains("password") ? passwordField
                : lower.contains("phone") ? phoneField
                : lower.contains("role") ? roleField
                : nameField;
        target.setError(true);
        target.setSupportingText(message);
    }

    /** Opens the edit side sheet for the selected administrator (name/phone/email). */
    public void onEditStaffClick() {
        Administrator admin = requireSelection();
        if (admin == null) {
            return;
        }
        showOverrideForm("Edit Staff Profile #" + admin.getId(),
                "Full name", admin.getFullName(),
                "Phone", admin.getPhone(), "edit");
    }

    /** Opens the password reset side sheet for the selected administrator. */
    public void onResetPasswordClick() {
        Administrator admin = requireSelection();
        if (admin == null) {
            return;
        }
        showOverrideForm("Reset Password — " + admin.getFullName(),
                "New password", "", "Repeat new password", "", "reset");
    }

    /** Disables or enables the selected administrator (rule 16), through the DAO. */
    public void onToggleActiveClick() {
        Administrator admin = requireSelection();
        if (admin == null) {
            return;
        }
        boolean disabling = admin.isActive();
        Data.submit(
                () -> {
                    Administrator updated = new AdministratorDao().findById(admin.getId());
                    if (updated == null) {
                        throw new com.gym.exception.ValidationException(
                                "Staff account #" + admin.getId() + " no longer exists.");
                    }
                    updated.setActive(!disabling);
                    new AdministratorDao().update(updated);
                    return updated;
                },
                updated -> {
                    selectedAdmin = updated;
                    loadStaff();
                    feedback.showSnackbar(updated.getFullName()
                            + (updated.isActive()
                            ? " re-enabled — the account can sign in again."
                            : " disabled — inactive accounts cannot sign in (rule 16)."));
                },
                error -> feedback.showSnackbar("Could not update the account: " + error.getMessage()));
    }

    /** Destructive: confirms the delete in an M3Dialog, then removes the account via the DAO. */
    public void onDeleteStaffClick() {
        Administrator admin = requireSelection();
        if (admin == null) {
            return;
        }
        UiKit.confirmDialog(feedback, "Delete " + admin.getFullName() + "?",
                "The staff account is removed immediately and can no longer sign in.",
                "Delete account", "Keep account",
                () -> {
                    selectedAdmin = null;
                    Data.submit(
                            () -> {
                                new AdministratorDao().delete(admin.getId());
                                return admin;
                            },
                            removed -> {
                                loadStaff();
                                feedback.showSnackbar(removed.getFullName()
                                        + " removed from the staff roster.");
                            },
                            error -> {
                                loadStaff();
                                feedback.showSnackbar("Could not delete: " + error.getMessage());
                            });
                });
    }

    /** Answers the password reset with a visible, non-blocking confirmation. */
    public void onResetPassword(int adminId) {
        for (Administrator admin : admins) {
            if (admin.getId() == adminId) {
                selectStaffRow(admin);
                break;
            }
        }
        onResetPasswordClick();
    }

    public void onEditStaff(int adminId) {
        for (Administrator admin : admins) {
            if (admin.getId() == adminId) {
                selectStaffRow(admin);
                break;
            }
        }
        onEditStaffClick();
    }

    public void onDeleteStaff(int adminId) {
        for (Administrator admin : admins) {
            if (admin.getId() == adminId) {
                selectStaffRow(admin);
                break;
            }
        }
        onDeleteStaffClick();
    }

    public void onAdjustPayment(int paymentId) {
        feedback.showSnackbar("Adjusting payment #" + paymentId
                + " happens in the payment register (rule 19).");
    }

    /** Requires a selected row; answers with the selection hint when none is active. */
    private Administrator requireSelection() {
        if (selectedAdmin == null) {
            feedback.showSnackbar("Select a staff row first — the actions act on the selected row.");
            return null;
        }
        return selectedAdmin;
    }

    public void onReassign() {
        showOverrideForm("Reassign Athlete Coach (Rule 15)", "Athlete ID", "", "New coach ID", "", "reassign");
    }

    public void onAdjustPaymentClick() {
        showOverrideForm("Adjust Payment Record (Rule 19)", "Payment ID", "", "New amount (USD)", "", "payment");
    }

    public void onCloseSideSheetClick() {
        overrideSideSheet.setVisible(false);
        pendingOverride = null;
    }

    /** Replaces the side sheet form area with the fields for the pending override. */
    private void showOverrideForm(String title, String fieldALabel, String fieldAValue,
                                  String fieldBLabel, String fieldBValue, String kind) {
        pendingOverride = kind;
        lblSideSheetTitle.setText(title);
        overrideFieldA = new M3TextField();
        overrideFieldA.setLabelText(fieldALabel);
        overrideFieldA.setText(fieldAValue);
        overrideFieldB = new M3TextField();
        overrideFieldB.setLabelText(fieldBLabel);
        overrideFieldB.setText(fieldBValue);
        Label hint = new Label("Committing runs a confirmation dialog before the change applies.");
        hint.getStyleClass().add("body-small");
        hint.setWrapText(true);
        sideSheetFormContainer.getChildren().setAll(overrideFieldA, overrideFieldB, hint);
        overrideSideSheet.setVisible(true);
    }

    /** Destructive override: confirms in an M3Dialog, closes the sheet and answers visibly. */
    public void onExecuteOverride() {
        if (overrideFieldA != null && (overrideFieldA.getText() == null
                || overrideFieldA.getText().isBlank())) {
            overrideFieldA.setError(true);
            overrideFieldA.setSupportingText("This field is required.");
            return;
        }
        String kind = pendingOverride == null ? "" : pendingOverride;
        // B/C7: these overrides belong to their own screens (rule 15 customer profile, rule 19
        // payment register) and no audit trail is invented here.
        if ("reassign".equals(kind)) {
            onCloseSideSheetClick();
            feedback.showSnackbar("Coach reassignment happens on the customer profile (rule 15).");
            return;
        }
        if ("payment".equals(kind)) {
            onCloseSideSheetClick();
            feedback.showSnackbar("Payment adjustments happen in the payment register (rule 19).");
            return;
        }
        M3Button keep = M3Button.text("Review again");
        M3Button confirm = M3Button.text("Execute");
        M3Dialog dialog = M3Dialog.basic("Apply this change?",
                new Label("The change is written to the staff roster immediately."),
                keep, confirm);
        dialog.setDefaultAction(confirm::activate);
        keep.setOnAction(e -> dialog.hide());
        confirm.setOnAction(e -> {
            dialog.hide();
            String name = overrideFieldA != null ? overrideFieldA.getText().trim() : "";
            if ("add".equals(kind)) {
                onAddStaff();
                onCloseSideSheetClick();
                return;
            }
            if ("edit".equals(kind) && selectedAdmin != null) {
                Administrator admin = selectedAdmin;
                String phone = overrideFieldB != null && overrideFieldB.getText() != null
                        ? overrideFieldB.getText().trim() : admin.getPhone();
                Data.submit(
                        () -> {
                            Administrator updated = new AdministratorDao().findById(admin.getId());
                            if (updated == null) {
                                throw new com.gym.exception.ValidationException(
                                        "Staff account #" + admin.getId() + " no longer exists.");
                            }
                            updated.setFullName(name);
                            updated.setPhone(phone);
                            new AdministratorDao().update(updated);
                            return updated;
                        },
                        updated -> {
                            selectedAdmin = updated;
                            loadStaff();
                            feedback.showSnackbar("Profile updated for " + updated.getFullName() + ".");
                        },
                        error -> {
                            loadStaff();
                            feedback.showSnackbar("Could not update the profile: " + error.getMessage());
                        });
            }
            onCloseSideSheetClick();
        });
        feedback.showDialog(dialog);
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
