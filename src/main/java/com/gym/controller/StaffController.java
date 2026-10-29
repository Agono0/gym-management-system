package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Controller skeleton for Staff RBAC Matrix & Security Administration screen (ACT-19-STAFF).
 * Declarative UI contract stubs matching Section 7 without backend, domain or service dependencies.
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

    public StaffController() {
        // Public no-arg constructor required by FXMLLoader
    }

    @FXML
    public void initialize() {
        // Pure UI initialization stub
        populateStaffTable();
    }

    /** Fills the roster table with demo rows (pipe-separated "id|name|role|status") so the screen is not blank. */
    private void populateStaffTable() {
        if (staffDataTable == null || !staffDataTable.getColumns().isEmpty()) {
            return;
        }
        staffDataTable.getColumns().addAll(
                new M3DataTable.Column<>("ID", r -> r.split("\\|")[0], 60),
                new M3DataTable.Column<>("Name", r -> r.split("\\|")[1], 150),
                new M3DataTable.Column<>("Role", r -> r.split("\\|")[2], 110),
                new M3DataTable.Column<>("Status", r -> r.split("\\|")[3], 100),
                new M3DataTable.Column<>("Last Login", r -> r.split("\\|")[4], 110));
        staffDataTable.getItems().addAll(
                "1|Alex Vance|ADMIN|Active|2026-10-29 07:55",
                "2|Marcus Vance|TRAINER|Active|2026-10-29 06:30",
                "3|Elena Rostova|TRAINER|Active|2026-10-28 19:12",
                "4|Sofia Reyes|RECEPTION|Active|2026-10-29 07:58",
                "5|Dana Wolfe|RECEPTION|Suspended|2026-10-21 14:03",
                "6|Omar Haddad|TRAINER|Active|2026-10-29 08:01");
    }

    @FXML
    public void onAddStaff() {
        // Stub for adding staff member
    }

    @FXML
    public void onAddStaff(ActionEvent event) {
        onAddStaff();
    }

    @FXML
    public void onEditStaffClick() {
        // Stub for edit staff action
    }

    @FXML
    public void onEditStaffClick(ActionEvent event) {
        onEditStaffClick();
    }

    @FXML
    public void onDeleteStaffClick() {
        // Stub for delete staff action
    }

    @FXML
    public void onDeleteStaffClick(ActionEvent event) {
        onDeleteStaffClick();
    }

    @FXML
    public void onResetPasswordClick() {
        // Stub for reset password action
    }

    @FXML
    public void onResetPasswordClick(ActionEvent event) {
        onResetPasswordClick();
    }

    @FXML
    public void onReassign() {
        if (lblSideSheetTitle != null) {
            lblSideSheetTitle.setText("Reassign Athlete Coach (Rule 15)");
        }
        if (overrideSideSheet != null) {
            overrideSideSheet.setVisible(true);
        }
    }

    @FXML
    public void onReassign(ActionEvent event) {
        onReassign();
    }

    @FXML
    public void onAdjustPaymentClick() {
        if (lblSideSheetTitle != null) {
            lblSideSheetTitle.setText("Adjust Payment Record (Rule 19)");
        }
        if (overrideSideSheet != null) {
            overrideSideSheet.setVisible(true);
        }
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
    }

    @FXML
    public void onExecuteOverride() {
        // The override form is populated per action (reassign / payment); committing closes the sheet
        onCloseSideSheetClick();
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
        // Stub for editing specific staff ID
    }

    public void onDeleteStaff(int adminId) {
        // Stub for deleting specific staff ID
    }

    public void onResetPassword(int adminId) {
        // Stub for resetting password for specific staff ID
    }

    public void onAdjustPayment(int paymentId) {
        // Stub for adjusting payment for specific payment ID
    }
}

