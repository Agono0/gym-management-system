package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3Timeline;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller for Staff RBAC Matrix & Security Administration screen (ACT-19-STAFF).
 * UI-contract only (no backend, domain or service dependencies): owns the demo roster, its
 * search filtering, the audit timeline, the override side sheet forms and the destructive
 * confirmations (delete, execute override).
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former staff.fxml: top app bar, roster/workspace split, override side sheet.
 */
public class StaffController implements ScreenController {

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private M3TextField txtSearchStaff;
    private M3DataTable<String> staffDataTable;
    private M3Timeline auditTimeline;
    private M3SideSheet overrideSideSheet;
    private Label lblSideSheetTitle;
    private VBox sideSheetFormContainer;
    private M3Button btnTopAddStaff;
    private M3Button btnEditStaffAction;
    private M3Button btnResetPasswordAction;
    private M3Button btnDeleteStaffAction;
    private M3Button btnReassignAction;
    private M3Button btnAdjustPaymentAction;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Demo roster rows in "id|name|role|status|lastLogin" shape. */
    private final List<String> demoStaff = new ArrayList<>(List.of(
            "1|Alex Vance|ADMIN|Active|2026-10-29 07:55",
            "2|Marcus Vance|TRAINER|Active|2026-10-29 06:30",
            "3|Elena Rostova|TRAINER|Active|2026-10-28 19:12",
            "4|Sofia Reyes|RECEPTION|Active|2026-10-29 07:58",
            "5|Dana Wolfe|RECEPTION|Suspended|2026-10-21 14:03",
            "6|Omar Haddad|TRAINER|Active|2026-10-29 08:01"));

    /** Fields of the currently open override form, read by Execute Override. */
    private M3TextField overrideFieldA;
    private M3TextField overrideFieldB;

    /** What the open override sheet is for (add / edit / reassign / payment). */
    private String pendingOverride;

    /**
     * Builds the complete Staff &amp; Access Governance view and initializes all behavior. Same
     * ordering semantics the FXMLLoader used to provide: the node tree exists and every field is
     * assigned before the former {@code initialize()} work runs.
     */
    public StaffController() {
        viewRoot = buildView();

        installFeedbackLayer(txtSearchStaff);
        populateStaffTable();
        wireSearch();

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
        btnDeleteStaffAction.setId("btnDeleteStaffAction");
        btnReassignAction.setId("btnReassignAction");
        btnAdjustPaymentAction.setId("btnAdjustPaymentAction");
        populateAuditTimeline();
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
        Label topCaption = new Label("Role-based access, audit trail and administrative overrides");
        topCaption.getStyleClass().add("body-medium");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        Label roleBadge = new Label("ROLE: ADMIN (Alex Vance)");
        roleBadge.getStyleClass().add("badge-admin");
        btnTopAddStaff = new M3Button("+ Add Staff Member", ButtonVariant.FILLED);
        btnTopAddStaff.getStyleClass().add("m3-button-filled");
        btnTopAddStaff.setOnAction(e -> onAddStaff());
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

        staffDataTable = new M3DataTable<>();
        VBox.setVgrow(staffDataTable, Priority.ALWAYS);

        HBox rosterToolbar = new HBox(10);
        rosterToolbar.setAlignment(Pos.CENTER_RIGHT);
        btnEditStaffAction = new M3Button("Edit Profile", ButtonVariant.TONAL);
        btnEditStaffAction.getStyleClass().add("m3-button-tonal");
        btnEditStaffAction.setOnAction(e -> onEditStaffClick());
        btnResetPasswordAction = new M3Button("Reset Password", ButtonVariant.OUTLINED);
        btnResetPasswordAction.getStyleClass().add("m3-button-outlined");
        btnResetPasswordAction.setOnAction(e -> onResetPasswordClick());
        btnDeleteStaffAction = new M3Button("Delete Staff", ButtonVariant.TEXT);
        btnDeleteStaffAction.getStyleClass().add("m3-button-text-danger");
        btnDeleteStaffAction.setOnAction(e -> onDeleteStaffClick());
        rosterToolbar.getChildren().addAll(btnEditStaffAction, btnResetPasswordAction, btnDeleteStaffAction);
        rosterPane.getChildren().addAll(searchRow, staffDataTable, rosterToolbar);

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

    /**
     * Fills the roster table with demo rows (pipe-separated "id|name|role|status|lastLogin") so
     * the screen is not blank, and wires the per-row edit action.
     */
    private void populateStaffTable() {
        if (!staffDataTable.getColumns().isEmpty()) {
            return;
        }
        staffDataTable.getColumns().addAll(List.of(
                new M3DataTable.Column<>("ID", r -> field(r, 0), 60),
                new M3DataTable.Column<>("Name", r -> field(r, 1), 150),
                new M3DataTable.Column<>("Role", r -> field(r, 2), 110),
                new M3DataTable.Column<>("Status", r -> field(r, 3), 100),
                new M3DataTable.Column<>("Last Login", r -> field(r, 4), 110)));
        staffDataTable.getItems().setAll(demoStaff);
    }

    /** Wires the labeled search field to filter the visible roster live. */
    private void wireSearch() {
        // txtSearchStaff is this screen's labeled search affordance; the table's built-in
        // unlabeled search bar would duplicate it, so it is not shown here.
        staffDataTable.getChildren().removeIf(n -> n instanceof M3SearchBar);
        txtSearchStaff.getInput().textProperty().addListener((obs, o, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            List<String> visible = new ArrayList<>();
            for (String row : demoStaff) {
                if (q.isEmpty() || row.toLowerCase(Locale.ROOT).contains(q)) {
                    visible.add(row);
                }
            }
            staffDataTable.getItems().setAll(visible);
        });
    }

    /** Fills the audit timeline so the right pane is not an empty feed. */
    private void populateAuditTimeline() {
        auditTimeline.addItem("07:58", "Gate check-in scanned", "Sofia Reyes opened the front desk terminal", M3Timeline.Status.COMPLETED);
        auditTimeline.addItem("07:55", "Role session started", "Alex Vance authenticated as ADMIN", M3Timeline.Status.COMPLETED);
        auditTimeline.addItem("06:30", "Schedule override", "Marcus Vance swapped two PT slots (Rule 13)", M3Timeline.Status.ACTIVE);
        auditTimeline.addItem("2026-10-28", "Password reset", "Temporary password issued to Dana Wolfe", M3Timeline.Status.COMPLETED);
        auditTimeline.addItem("2026-10-21", "Account suspended", "Dana Wolfe suspended after failed logins", M3Timeline.Status.PENDING);
    }

    /** Returns the requested pipe field of a demo row, or an empty string when missing. */
    private static String field(String row, int index) {
        String[] parts = row.split("\\|");
        return index < parts.length ? parts[index] : "";
    }

    /** The demo staff member the row-level actions act on (first roster entry). */
    private String selectedStaff() {
        return demoStaff.isEmpty() ? "" : demoStaff.get(0);
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
        Label hint = new Label("Committing runs a confirmation dialog and logs an audit entry.");
        hint.getStyleClass().add("body-small");
        hint.setWrapText(true);
        sideSheetFormContainer.getChildren().setAll(overrideFieldA, overrideFieldB, hint);
        overrideSideSheet.setVisible(true);
    }

    public void onAddStaff() {
        showOverrideForm("Add Staff Member", "Full name", "", "Role (ADMIN, TRAINER, RECEPTION)",
                "TRAINER", "add");
    }

    public void onEditStaffClick() {
        String current = selectedStaff();
        showOverrideForm("Edit Staff Profile", "Full name", field(current, 1),
                "Status (Active, Suspended)", field(current, 3), "edit");
    }

    /** Destructive: confirms the delete in an M3Dialog, then answers with an Undo snackbar. */
    public void onDeleteStaffClick() {
        String current = selectedStaff();
        if (current.isEmpty()) {
            return;
        }
        M3Button keep = M3Button.text("Keep member");
        M3Button confirm = M3Button.text("Delete member");
        M3Dialog dialog = M3Dialog.basic("Delete " + field(current, 1) + "?",
                new Label("The staff member loses access immediately and the removal is audited."),
                keep, confirm);
        dialog.setDefaultAction(confirm::activate);
        keep.setOnAction(e -> dialog.hide());
        confirm.setOnAction(e -> {
            dialog.hide();
            int index = demoStaff.indexOf(current);
            demoStaff.remove(current);
            refreshRoster();
            feedback.showSnackbar(field(current, 1) + " removed from the roster (demo)", "Undo",
                    () -> {
                        demoStaff.add(Math.min(index, demoStaff.size()), current);
                        refreshRoster();
                    });
        });
        feedback.showDialog(dialog);
    }

    /** Answers the password reset with a visible, non-blocking confirmation. */
    public void onResetPasswordClick() {
        String current = selectedStaff();
        if (current.isEmpty()) {
            return;
        }
        feedback.showSnackbar("Temporary password issued to " + field(current, 1) + " (demo)");
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

    /** Destructive override: confirms in an M3Dialog, closes the sheet and answers visibly. */
    public void onExecuteOverride() {
        if (overrideFieldA != null && (overrideFieldA.getText() == null
                || overrideFieldA.getText().isBlank())) {
            overrideFieldA.setError(true);
            overrideFieldA.setSupportingText("This field is required.");
            return;
        }
        M3Button keep = M3Button.text("Review again");
        M3Button confirm = M3Button.text("Execute");
        M3Dialog dialog = M3Dialog.basic("Execute this override?",
                new Label("The change applies immediately and a new entry is added to the audit timeline."),
                keep, confirm);
        dialog.setDefaultAction(confirm::activate);
        keep.setOnAction(e -> dialog.hide());
        confirm.setOnAction(e -> {
            dialog.hide();
            String who = overrideFieldA != null ? overrideFieldA.getText().trim() : "";
            String kindLabel = switch (pendingOverride == null ? "" : pendingOverride) {
                case "add" -> "added to the roster";
                case "edit" -> "profile updated";
                case "reassign" -> "athlete reassigned";
                case "payment" -> "payment record adjusted";
                default -> "override executed";
            };
            if ("add".equals(pendingOverride)) {
                int nextId = demoStaff.size() + 1;
                String role = overrideFieldB != null && overrideFieldB.getText() != null
                        && !overrideFieldB.getText().isBlank()
                        ? overrideFieldB.getText().trim().toUpperCase(Locale.ROOT)
                        : "TRAINER";
                demoStaff.add(nextId + "|" + who + "|" + role + "|Active|just now");
                refreshRoster();
            } else if ("edit".equals(pendingOverride)) {
                String current = selectedStaff();
                if (!current.isEmpty()) {
                    String status = overrideFieldB != null && overrideFieldB.getText() != null
                            ? overrideFieldB.getText().trim()
                            : field(current, 3);
                    demoStaff.set(demoStaff.indexOf(current),
                            field(current, 0) + "|" + who + "|" + field(current, 2) + "|" + status + "|just now");
                    refreshRoster();
                }
            }
            auditTimeline.addItem("just now", "Override executed", who + " — " + kindLabel, M3Timeline.Status.ACTIVE);
            onCloseSideSheetClick();
            feedback.showSnackbar("Override executed: " + who + " " + kindLabel + " (demo)");
        });
        feedback.showDialog(dialog);
    }

    /** Re-applies the current search query after the roster data changed. */
    private void refreshRoster() {
        String q = txtSearchStaff.getText() != null
                ? txtSearchStaff.getText().trim().toLowerCase(Locale.ROOT)
                : "";
        List<String> visible = new ArrayList<>();
        for (String row : demoStaff) {
            if (q.isEmpty() || row.toLowerCase(Locale.ROOT).contains(q)) {
                visible.add(row);
            }
        }
        staffDataTable.getItems().setAll(visible);
    }

    public void onEditStaff(int adminId) {
        feedback.showSnackbar("Editing staff #" + adminId + " (demo)");
    }

    public void onDeleteStaff(int adminId) {
        onDeleteStaffClick();
    }

    public void onResetPassword(int adminId) {
        feedback.showSnackbar("Temporary password issued for staff #" + adminId + " (demo)");
    }

    public void onAdjustPayment(int paymentId) {
        feedback.showSnackbar("Adjusting payment #" + paymentId + " (demo)");
    }
}
