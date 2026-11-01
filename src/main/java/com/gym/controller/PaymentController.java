package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.glass.GlassEffectParams;
import io.m3fx.controls.glass.GlassShape;
import io.m3fx.controls.glass.M3GlassPane;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.sheet.M3BottomSheet;
import io.m3fx.controls.textfield.M3TextField;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for Cashless POS Register &amp; Split Billing screen (ACT-18-BILLING).
 * UI-contract only (no backend, domain or service dependencies): the register is a working
 * demo — recording validates the amount and appends a Paid row, the receipt lookup opens the
 * demo receipts sheet, the balance lookup reads unpaid rows from the register, Void confirms
 * through an {@link M3Dialog} and then visibly updates the table row and the revenue KPI, and
 * every answer surfaces through an {@link io.m3fx.controls.snackbar.M3Snackbar}.
 */
public class PaymentController {

    /** Demo dues per charge item; the FXML default amount (499.00) anchors the membership fee. */
    private static final double MEMBERSHIP_DUE = 499.00;
    private static final double SUBSCRIPTION_DUE = 240.00;
    private static final double SESSION_DUE = 45.00;

    /**
     * Corner radius of the floating receipt sheet's liquid glass surface — the same 28px the
     * {@code .m3-bottom-sheet} stylesheet uses, so the glass keeps the sheet's geometry.
     */
    private static final double SHEET_GLASS_RADIUS = 28;

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

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Exclusive single-selection group behind the three tender-method chips. */
    private javafx.scene.control.ToggleGroup methodGroup;

    /** Today's revenue behind the KPI card, seeded from the FXML value ($4,850.00). */
    private double todayRevenue = 4850.00;
    /** Outstanding balance behind the KPI card, seeded from the FXML value ($320.00). */
    private double todayUnpaid = 320.00;

    /** Demo receipt register: R-8801… map to the rows shown in the register table. */
    private final Map<String, String> demoReceipts = new LinkedHashMap<>();
    /** Next receipt number issued by the Record action. */
    private int nextReceiptNumber = 8801;
    /** Most recent register row activated by a click — the Void button's first target. */
    private String selectedPaymentRow;

    public PaymentController() {
        // Public no-arg constructor required by FXMLLoader
    }

    @FXML
    public void initialize() {
        keepPaymentMethodsExclusive();
        populateRecentTransactions();
        applyReceiptSheetGlass();
        installFeedbackLayer(paymentDataTable);
        if (paymentDataTable != null) {
            // Row activation marks the payment as the Void button's target (hand cursor affordance).
            paymentDataTable.setOnRowAction(this::selectPaymentRow);
        }
    }

    /**
     * Moves the floating digital-receipt drawer onto a real liquid glass surface (Apple iOS 26):
     * a REGULAR interactive {@link M3GlassPane} takes the sheet's exact place in the screen root
     * and the sheet becomes its content, so the drawer reads as Apple floating chrome over the
     * register. The sheet's opaque surface fill is cleared (Apple: no opaque backgrounds behind
     * glass) and the glass mirrors the sheet's visibility, which keeps every existing
     * {@code receiptBottomSheet.setVisible(...)} toggle, the snackbar and the dialog flows
     * byte-identical. Page cards (balance card, KPI cards, register table) deliberately stay
     * non-glass — glass is reserved for floating chrome, never page content.
     */
    private void applyReceiptSheetGlass() {
        wrapSheetInGlass(receiptBottomSheet, "Receipt sheet glass skipped: ");
    }

    /**
     * Re-parents an FXML-declared sheet into a REGULAR interactive liquid glass pane that takes
     * over the sheet's position, constraints and visibility. Defensive by construction: any
     * failure leaves the plain sheet in place, because glass chrome must never break a screen.
     *
     * @param sheet the sheet node shown/hidden by the FXML action handlers
     * @param errorPrefix stderr prefix identifying the caller in the log
     */
    private static void wrapSheetInGlass(Region sheet, String errorPrefix) {
        try {
            if (sheet == null || !(sheet.getParent() instanceof Pane parent)) {
                return;
            }
            int index = parent.getChildren().indexOf(sheet);
            if (index < 0) {
                return;
            }
            M3GlassPane glass = M3GlassPane.of(new GlassEffectParams()
                    .in(GlassShape.RECT, SHEET_GLASS_RADIUS)
                    .interactive());
            // Replace-then-reparent: the glass takes the sheet's slot first (so no child index
            // shifts underneath us), then the sheet moves into the glass's content box.
            parent.getChildren().set(index, glass);
            glass.setContent(sheet);
            // The sheet's own paint moves onto the glass (inline style outranks the
            // .m3-bottom-sheet / .m3-side-sheet stylesheet fill).
            sheet.setStyle("-fx-background-color: transparent;");
            // The FXML flow toggles the sheet node directly; the glass chrome mirrors it so the
            // existing show/hide handlers keep working unchanged.
            glass.visibleProperty().bind(sheet.visibleProperty());
            // Preserve the FXML placement constraints (they live on the child node).
            if (StackPane.getAlignment(sheet) != null) {
                StackPane.setAlignment(glass, StackPane.getAlignment(sheet));
            }
            if (StackPane.getMargin(sheet) != null) {
                StackPane.setMargin(glass, StackPane.getMargin(sheet));
            }
            glass.setMaxSize(sheet.getMaxWidth(), sheet.getMaxHeight());
        } catch (Throwable t) {
            System.err.println(errorPrefix + t);
        }
    }

    /** Fills the recent-transactions register with demo rows and issues their receipt numbers. */
    private void populateRecentTransactions() {
        if (paymentDataTable == null || !paymentDataTable.getColumns().isEmpty()) {
            return;
        }
        paymentDataTable.getColumns().addAll(List.of(
                new M3DataTable.Column<>("Date", r -> r.split("\\|")[0], 100),
                new M3DataTable.Column<>("Member", r -> r.split("\\|")[1], 190),
                new M3DataTable.Column<>("Method", r -> r.split("\\|")[2], 100),
                new M3DataTable.Column<>("Amount", r -> r.split("\\|")[3], 90),
                new M3DataTable.Column<>("Status", r -> r.split("\\|")[4], 90)));
        for (String row : new String[] {
                "2026-10-29|Priya Nair (#2075)|Card|45.00|Paid",
                "2026-10-29|Tom Becker (#1533)|Cash|120.00|Paid",
                "2026-10-28|Aisha Khan (#1902)|Transfer|45.00|Pending",
                "2026-10-28|Lena Fischer (#3110)|Card|480.00|Paid",
                "2026-10-27|Jonas Weber (#1254)|Cash|15.00|Paid",
                "2026-10-27|Diego Ramos (#2871)|Transfer|60.00|Voided"}) {
            paymentDataTable.getItems().addAll(row);
            demoReceipts.put("R-" + nextReceiptNumber++, row);
        }
    }

    /** Puts the three payment-method chips in one ToggleGroup so exactly one tender type is active. */
    private void keepPaymentMethodsExclusive() {
        if (chipMethodCard == null) {
            return;
        }
        methodGroup = new javafx.scene.control.ToggleGroup();
        methodGroup.getToggles().addAll(chipMethodCard, chipMethodCash, chipMethodTransfer);
    }

    /**
     * Records the current charge: validates the amount inline (M3TextField error), reads the
     * active tender chip and charge item, appends a Paid row to the register, advances the
     * revenue KPI and confirms with a snackbar carrying the new receipt number.
     */
    @FXML
    public void onRecord() {
        if (txtPaymentAmount == null || paymentDataTable == null) {
            return;
        }
        double amount = parseAmount(txtPaymentAmount.getText());
        if (amount <= 0) {
            txtPaymentAmount.setError(true);
            txtPaymentAmount.setSupportingText("Enter a positive amount in USD (for example 45.00).");
            feedback.showSnackbar("Fix the highlighted amount, then record again.");
            return;
        }
        txtPaymentAmount.setError(false);
        txtPaymentAmount.setSupportingText("");

        String method = selectedMethod();
        String charge = selectedChargeItem();
        String member = currentCustomerLabel();
        String receiptNo = "R-" + nextReceiptNumber++;
        String row = LocalDate.now() + "|" + member + "|" + method + "|"
                + String.format(Locale.ROOT, "%.2f", amount) + "|Paid";
        paymentDataTable.getItems().add(0, row);
        demoReceipts.put(receiptNo, row);
        todayRevenue += amount;
        updateRevenueKpi();
        feedback.showSnackbar("Recorded " + charge + " — $" + String.format(Locale.ROOT, "%.2f", amount)
                + " via " + method + " for " + member + " (receipt " + receiptNo + ", demo).");
    }

    @FXML
    public void onRecord(ActionEvent event) {
        onRecord();
    }

    /**
     * Receipt lookup: validates the receipt number, answers the demo register R-8801… and opens
     * the receipt sheet with the stored row; unknown numbers say so instead of failing silently.
     */
    @FXML
    public void onReceiptLookupClick() {
        if (txtSearchReceipt == null) {
            return;
        }
        String query = txtSearchReceipt.getText() == null ? "" : txtSearchReceipt.getText().trim();
        if (query.isEmpty()) {
            feedback.showSnackbar("Enter a receipt number first, for example R-8801 (demo).");
            return;
        }
        String receiptNo = query.toUpperCase(Locale.ROOT);
        if (!demoReceipts.containsKey(receiptNo)) {
            feedback.showSnackbar("No receipt matches \"" + query + "\" — the demo register covers R-8801 to R-"
                    + (nextReceiptNumber - 1) + ".");
            return;
        }
        showReceipt(receiptNo);
        feedback.showSnackbar("Receipt " + receiptNo + " opened (demo).");
    }

    @FXML
    public void onReceiptLookupClick(ActionEvent event) {
        onReceiptLookupClick();
    }

    /**
     * Balance lookup: validates the input inline (M3TextField error), finds the member in the
     * visible register and sums their Pending rows into the balance readout.
     */
    @FXML
    public void onCheckBalanceClick() {
        if (txtCustomerLookup == null || lblCustomerName == null || lblCustomerBalance == null) {
            return;
        }
        String query = txtCustomerLookup.getText() == null ? "" : txtCustomerLookup.getText().trim();
        if (query.isEmpty()) {
            txtCustomerLookup.setError(true);
            txtCustomerLookup.setSupportingText("Enter the customer ID, phone or name shown in the register.");
            feedback.showSnackbar("Enter a customer to look up first.");
            return;
        }
        txtCustomerLookup.setError(false);
        txtCustomerLookup.setSupportingText("");

        String lower = query.toLowerCase(Locale.ROOT);
        String match = null;
        double unpaid = 0;
        if (paymentDataTable != null) {
            for (String row : paymentDataTable.getItems()) {
                String[] parts = row.split("\\|");
                if (parts.length > 4 && (parts[0] + " " + parts[1]).toLowerCase(Locale.ROOT).contains(lower)) {
                    if (match == null) {
                        match = parts[1];
                    }
                    if ("Pending".equals(parts[4])) {
                        unpaid += parseAmount(parts[3]);
                    }
                }
            }
        }
        if (match == null) {
            lblCustomerName.setText("Athlete: Not found for \"" + query + "\"");
            lblCustomerBalance.setText("Unpaid Balance: $0.00");
            feedback.showSnackbar("No register row matches \"" + query + "\" (demo).");
            return;
        }
        lblCustomerName.setText("Athlete: " + match);
        lblCustomerBalance.setText("Unpaid Balance: $" + String.format(Locale.ROOT, "%.2f", unpaid));
        feedback.showSnackbar(match + " has $" + String.format(Locale.ROOT, "%.2f", unpaid)
                + " outstanding (demo).");
    }

    @FXML
    public void onCheckBalanceClick(ActionEvent event) {
        onCheckBalanceClick();
    }

    /**
     * Two-step void (destructive action): an M3Dialog confirms first, then the target row
     * visibly flips to Voided, the revenue/unpaid KPIs adjust and a snackbar confirms.
     */
    @FXML
    public void onVoidClick() {
        String row = voidTargetRow();
        if (row == null) {
            feedback.showSnackbar("Nothing to void — every register row is already voided (demo).");
            return;
        }
        String[] parts = row.split("\\|");
        M3Button cancel = M3Button.text("Keep Payment");
        M3Button voidButton = M3Button.text("Void Payment");
        M3Dialog dialog = M3Dialog.basic("Void this payment?",
                new Label("Payment " + parts[1] + " (" + parts[2] + ", $" + parts[3] + ") is reversed"
                        + (parts[4].equals("Paid") ? " and drops out of today's revenue." : ".")),
                cancel, voidButton);
        dialog.setDefaultAction(voidButton::activate);
        cancel.setOnAction(e -> dialog.hide());
        voidButton.setOnAction(e -> {
            dialog.hide();
            voidRow(row);
        });
        feedback.showDialog(dialog);
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

    /** Loads the exact due amount of the currently selected charge item into the amount field. */
    @FXML
    public void onTenderExact() {
        if (txtPaymentAmount != null) {
            txtPaymentAmount.setText(String.format(Locale.ROOT, "%.2f", selectedChargeDue()));
            txtPaymentAmount.setError(false);
        }
        feedback.showSnackbar("Exact due amount loaded for " + selectedChargeItem() + " (demo).");
    }

    @FXML
    public void onTenderExact(ActionEvent event) {
        onTenderExact();
    }

    /** Voids the payment row of the given member id (demo register lookup), if present. */
    public void onVoid(int paymentId) {
        if (paymentDataTable == null) {
            return;
        }
        String row = paymentDataTable.getItems().stream()
                .filter(r -> r.contains("#" + paymentId + ")") && !r.endsWith("Voided"))
                .findFirst().orElse(null);
        if (row == null) {
            feedback.showSnackbar("No voidable payment for member #" + paymentId + " (demo).");
            return;
        }
        voidRow(row);
    }

    /**
     * Renders the stored register row of the given receipt number into the receipt sheet; when
     * the number is not in the demo register, the sheet shows the bare number instead.
     */
    public void showReceipt(String receiptNo) {
        if (lblReceiptDetails != null) {
            String row = demoReceipts.get(receiptNo);
            lblReceiptDetails.setText(row == null ? "Receipt: " + receiptNo : receiptText(receiptNo, row));
        }
        if (receiptBottomSheet != null) {
            receiptBottomSheet.setVisible(true);
        }
    }

    // ------------------------------------------------------------------
    // Demo register helpers
    // ------------------------------------------------------------------

    /** Marks the register row Voided, adjusts the revenue/unpaid KPIs and confirms. */
    private void voidRow(String row) {
        if (paymentDataTable == null || !paymentDataTable.getItems().contains(row)) {
            return;
        }
        String[] parts = row.split("\\|");
        int index = paymentDataTable.getItems().indexOf(row);
        paymentDataTable.getItems().set(index, parts[0] + "|" + parts[1] + "|" + parts[2]
                + "|" + parts[3] + "|Voided");
        double amount = parseAmount(parts[3]);
        if ("Paid".equals(parts[4])) {
            todayRevenue = Math.max(0, todayRevenue - amount);
            updateRevenueKpi();
        } else if ("Pending".equals(parts[4])) {
            todayUnpaid = Math.max(0, todayUnpaid - amount);
            updateUnpaidKpi();
        }
        if (row.equals(selectedPaymentRow)) {
            selectedPaymentRow = null;
        }
        feedback.showSnackbar("Payment voided: " + parts[1] + " — $" + parts[3] + " reversed (demo).");
    }

    /** The Void target: the activated register row, else the most recent voidable row. */
    private String voidTargetRow() {
        if (paymentDataTable == null) {
            return null;
        }
        if (selectedPaymentRow != null && paymentDataTable.getItems().contains(selectedPaymentRow)
                && !selectedPaymentRow.endsWith("Voided")) {
            return selectedPaymentRow;
        }
        return paymentDataTable.getItems().stream()
                .filter(r -> !r.endsWith("Voided"))
                .findFirst().orElse(null);
    }

    private void selectPaymentRow(String row) {
        selectedPaymentRow = row;
    }

    /** Tender chip selection mapped to the register's method vocabulary; Card is the default. */
    private String selectedMethod() {
        if (methodGroup != null) {
            javafx.scene.control.Toggle selected = methodGroup.getSelectedToggle();
            if (selected == chipMethodCash) {
                return "Cash";
            }
            if (selected == chipMethodTransfer) {
                return "Transfer";
            }
        }
        return "Card";
    }

    /** Charge radio selection mapped to a human label; Membership is the default. */
    private String selectedChargeItem() {
        if (rbSubscription != null && rbSubscription.isSelected()) {
            return "trainer subscription";
        }
        if (rbSession != null && rbSession.isSelected()) {
            return "single session";
        }
        return "membership";
    }

    /** Demo dues for the selected charge item (see the constants above for the anchors). */
    private double selectedChargeDue() {
        if (rbSubscription != null && rbSubscription.isSelected()) {
            return SUBSCRIPTION_DUE;
        }
        if (rbSession != null && rbSession.isSelected()) {
            return SESSION_DUE;
        }
        return MEMBERSHIP_DUE;
    }

    /**
     * The charge recipient: the athlete set by the balance lookup, or the walk-in default when
     * no lookup answered yet.
     */
    private String currentCustomerLabel() {
        String label = lblCustomerName == null || lblCustomerName.getText() == null
                ? "" : lblCustomerName.getText().trim();
        if (label.startsWith("Athlete: ") && !label.equals("Athlete: Not Selected")
                && !label.startsWith("Athlete: Not found")) {
            return label.substring("Athlete: ".length());
        }
        return "Walk-in member";
    }

    private void updateRevenueKpi() {
        if (statRevenueCard != null) {
            statRevenueCard.setValue(String.format(Locale.ROOT, "$%,.2f", todayRevenue));
        }
    }

    private void updateUnpaidKpi() {
        if (statUnpaidCard != null) {
            statUnpaidCard.setValue(String.format(Locale.ROOT, "$%,.2f", todayUnpaid));
        }
    }

    /** Multi-line receipt text rendered into the sheet from a register row. */
    private static String receiptText(String receiptNo, String row) {
        String[] parts = row.split("\\|");
        return "Receipt " + receiptNo + "\nDate: " + parts[0] + "\nMember: " + parts[1]
                + "\nMethod: " + parts[2] + "\nAmount: $" + parts[3] + "\nStatus: " + parts[4];
    }

    /** Tolerant demo amount parsing ("45.00" → 45.0; anything else → -1). */
    private static double parseAmount(String raw) {
        try {
            return Double.parseDouble(raw == null ? "" : raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
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
