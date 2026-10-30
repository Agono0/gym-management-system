package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.search.M3SearchBar;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller for Staff RBAC Matrix & Security Administration screen (ACT-19-STAFF).
 * UI-contract only (no backend, domain or service dependencies): owns the demo roster, its
 * search filtering, the audit timeline, the override side sheet forms and the destructive
 * confirmations (delete, execute override).
 */
public class StaffController {

    @FXML private M3TextField txtSearchStaff;
    @FXML private M3DataTable<String> staffDataTable;
    @FXML private M3Timeline auditTimeline;
    @FXML private M3SideSheet overrideSideSheet;
    @FXML private Label lblSideSheetTitle;
    @FXML private VBox sideSheetFormContainer;
    @FXML private M3Button btnTopAddStaff;
    @FXML private M3Button btnEditStaffAction;
    @FXML private M3Button btnResetPasswordAction;
    @FXML private M3Button btnDeleteStaffAction;
    @FXML private M3Button btnReassignAction;
    @FXML private M3Button btnAdjustPaymentAction;

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

    public StaffController() {
        // Public no-arg constructor required by FXMLLoader
    }

    @FXML
    public void initialize() {
        installFeedbackLayer(txtSearchStaff);
        populateStaffTable();
        wireSearch();
        populateAuditTimeline();
    }

    /**
     * Fills the roster table with demo rows (pipe-separated "id|name|role|status|lastLogin") so
     * the screen is not blank, and wires the per-row edit action.
     */
    private void populateStaffTable() {
        if (staffDataTable == null || !staffDataTable.getColumns().isEmpty()) {
            return;
        }
        staffDataTable.getColumns().addAll(
                new M3DataTable.Column<>("ID", r -> field(r, 0), 60),
                new M3DataTable.Column<>("Name", r -> field(r, 1), 150),
                new M3DataTable.Column<>("Role", r -> field(r, 2), 110),
                new M3DataTable.Column<>("Status", r -> field(r, 3), 100),
                new M3DataTable.Column<>("Last Login", r -> field(r, 4), 110));
        staffDataTable.getItems().setAll(demoStaff);
    }

    /** Wires the labeled search field to filter the visible roster live. */
    private void wireSearch() {
        if (staffDataTable == null) {
            return;
        }
        // txtSearchStaff is this screen's labeled search affordance; the table's built-in
        // unlabeled search bar would duplicate it, so it is not shown here.
        staffDataTable.getChildren().removeIf(n -> n instanceof M3SearchBar);
        if (txtSearchStaff != null) {
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
    }

    /** Fills the audit timeline so the right pane is not an empty feed. */
    private void populateAuditTimeline() {
        if (auditTimeline == null) {
            return;
        }
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
        if (lblSideSheetTitle != null) {
            lblSideSheetTitle.setText(title);
        }
        if (sideSheetFormContainer != null) {
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
        }
        if (overrideSideSheet != null) {
            overrideSideSheet.setVisible(true);
        }
    }

    @FXML
    public void onAddStaff() {
        showOverrideForm("Add Staff Member", "Full name", "", "Role (ADMIN, TRAINER, RECEPTION)",
                "TRAINER", "add");
    }

    @FXML
    public void onAddStaff(ActionEvent event) {
        onAddStaff();
    }

    @FXML
    public void onEditStaffClick() {
        String current = selectedStaff();
        showOverrideForm("Edit Staff Profile", "Full name", field(current, 1),
                "Status (Active, Suspended)", field(current, 3), "edit");
    }

    @FXML
    public void onEditStaffClick(ActionEvent event) {
        onEditStaffClick();
    }

    /** Destructive: confirms the delete in an M3Dialog, then answers with an Undo snackbar. */
    @FXML
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

    @FXML
    public void onDeleteStaffClick(ActionEvent event) {
        onDeleteStaffClick();
    }

    /** Answers the password reset with a visible, non-blocking confirmation. */
    @FXML
    public void onResetPasswordClick() {
        String current = selectedStaff();
        if (current.isEmpty()) {
            return;
        }
        feedback.showSnackbar("Temporary password issued to " + field(current, 1) + " (demo)");
    }

    @FXML
    public void onResetPasswordClick(ActionEvent event) {
        onResetPasswordClick();
    }

    @FXML
    public void onReassign() {
        showOverrideForm("Reassign Athlete Coach (Rule 15)", "Athlete ID", "", "New coach ID", "", "reassign");
    }

    @FXML
    public void onReassign(ActionEvent event) {
        onReassign();
    }

    @FXML
    public void onAdjustPaymentClick() {
        showOverrideForm("Adjust Payment Record (Rule 19)", "Payment ID", "", "New amount (USD)", "", "payment");
    }

    @FXML
    public void onAdjustPaymentClick(ActionEvent event) {
        onAdjustPaymentClick();
    }

    @FXML
    public void onCloseSideSheetClick() {
        if (overrideSideSheet != null) {
            overrideSideSheet.setVisible(false);
        }
        pendingOverride = null;
    }

    /** Destructive override: confirms in an M3Dialog, closes the sheet and answers visibly. */
    @FXML
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
            if (auditTimeline != null) {
                auditTimeline.addItem("just now", "Override executed", who + " — " + kindLabel, M3Timeline.Status.ACTIVE);
            }
            onCloseSideSheetClick();
            feedback.showSnackbar("Override executed: " + who + " " + kindLabel + " (demo)");
        });
        feedback.showDialog(dialog);
    }

    /** Re-applies the current search query after the roster data changed. */
    private void refreshRoster() {
        if (staffDataTable == null) {
            return;
        }
        String q = txtSearchStaff != null && txtSearchStaff.getText() != null
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

    @FXML
    public void onExecuteOverride(ActionEvent event) {
        onExecuteOverride();
    }

    @FXML
    public void onCloseSideSheetClick(ActionEvent event) {
        onCloseSideSheetClick();
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
