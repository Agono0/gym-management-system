package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.sheet.M3BottomSheet;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

/**
 * Controller skeleton for Cashless POS Register & Split Billing screen (ACT-18-BILLING).
 * Declarative UI contract stubs matching Section 7 without backend, domain or service dependencies.
 */
public class PaymentController {

    @FXML private TextField txtSearchReceipt;
    @FXML private M3TextField txtCustomerLookup;
    @FXML private Label lblCustomerName;
    @FXML private Label lblCustomerBalance;
    @FXML private RadioButton rbMembership;
    @FXML private RadioButton rbSubscription;
    @FXML private RadioButton rbSession;
    @FXML private M3Chip chipMethodCard;
    @FXML private M3Chip chipMethodCash;
    @FXML private M3Chip chipMethodTransfer;
    @FXML private M3TextField txtPaymentAmount;
    @FXML private M3SplitButton actionSplitButton;
    @FXML private M3DataTable<String> paymentDataTable;
    @FXML private M3StatCard statRevenueCard;
    @FXML private M3StatCard statUnpaidCard;
    @FXML private M3Button btnShowReceipt;
    @FXML private M3Button btnVoidPayment;
    @FXML private M3BottomSheet receiptBottomSheet;
    @FXML private Label lblReceiptDetails;

    public PaymentController() {
        // Public no-arg constructor required by FXMLLoader
    }

    @FXML
    public void initialize() {
        keepPaymentMethodsExclusive();
        populateRecentTransactions();
    }

    /** Fills the recent-transactions register with demo rows so the screen is not blank. */
    private void populateRecentTransactions() {
        if (paymentDataTable == null || !paymentDataTable.getColumns().isEmpty()) {
            return;
        }
        paymentDataTable.getColumns().addAll(
                new M3DataTable.Column<>("Date", r -> r.split("\\|")[0], 100),
                new M3DataTable.Column<>("Member", r -> r.split("\\|")[1], 190),
                new M3DataTable.Column<>("Method", r -> r.split("\\|")[2], 100),
                new M3DataTable.Column<>("Amount", r -> r.split("\\|")[3], 90),
                new M3DataTable.Column<>("Status", r -> r.split("\\|")[4], 90));
        paymentDataTable.getItems().addAll(
                "2026-10-29|Priya Nair (#2075)|Card|45.00|Paid",
                "2026-10-29|Tom Becker (#1533)|Cash|120.00|Paid",
                "2026-10-28|Aisha Khan (#1902)|Transfer|45.00|Pending",
                "2026-10-28|Lena Fischer (#3110)|Card|480.00|Paid",
                "2026-10-27|Jonas Weber (#1254)|Cash|15.00|Paid",
                "2026-10-27|Diego Ramos (#2871)|Transfer|60.00|Voided");
    }

    /** Puts the three payment-method chips in one ToggleGroup so exactly one tender type is active. */
    private void keepPaymentMethodsExclusive() {
        if (chipMethodCard == null) {
            return;
        }
        javafx.scene.control.ToggleGroup methods = new javafx.scene.control.ToggleGroup();
        methods.getToggles().addAll(chipMethodCard, chipMethodCash, chipMethodTransfer);
    }

    @FXML
    public void onRecord() {
        // Stub for record payment action
    }

    @FXML
    public void onRecord(ActionEvent event) {
        onRecord();
    }

    @FXML
    public void onReceiptLookupClick() {
        // Stub for receipt lookup action
    }

    @FXML
    public void onReceiptLookupClick(ActionEvent event) {
        onReceiptLookupClick();
    }

    @FXML
    public void onCheckBalanceClick() {
        // Stub for checking customer balance
    }

    @FXML
    public void onCheckBalanceClick(ActionEvent event) {
        onCheckBalanceClick();
    }

    @FXML
    public void onVoidClick() {
        // Stub for void payment action
    }

    @FXML
    public void onVoidClick(ActionEvent event) {
        onVoidClick();
    }

    @FXML
    public void onShowReceiptClick() {
        if (receiptBottomSheet != null) {
            receiptBottomSheet.setVisible(true);
        }
    }

    @FXML
    public void onShowReceiptClick(ActionEvent event) {
        onShowReceiptClick();
    }

    @FXML
    public void onCloseReceiptClick() {
        if (receiptBottomSheet != null) {
            receiptBottomSheet.setVisible(false);
        }
    }

    @FXML
    public void onCloseReceiptClick(ActionEvent event) {
        onCloseReceiptClick();
    }

    @FXML
    public void onTender20() {
        if (txtPaymentAmount != null) {
            txtPaymentAmount.setText("20.00");
        }
    }

    @FXML
    public void onTender20(ActionEvent event) {
        onTender20();
    }

    @FXML
    public void onTender50() {
        if (txtPaymentAmount != null) {
            txtPaymentAmount.setText("50.00");
        }
    }

    @FXML
    public void onTender50(ActionEvent event) {
        onTender50();
    }

    @FXML
    public void onTender100() {
        if (txtPaymentAmount != null) {
            txtPaymentAmount.setText("100.00");
        }
    }

    @FXML
    public void onTender100(ActionEvent event) {
        onTender100();
    }

    @FXML
    public void onTenderExact() {
        // Stub for setting exact tender amount
    }

    @FXML
    public void onTenderExact(ActionEvent event) {
        onTenderExact();
    }

    public void onVoid(int paymentId) {
        // Stub for voiding a specific payment
    }

    public void showReceipt(String receiptNo) {
        if (lblReceiptDetails != null) {
            lblReceiptDetails.setText("Receipt: " + receiptNo);
        }
        if (receiptBottomSheet != null) {
            receiptBottomSheet.setVisible(true);
        }
    }
}

