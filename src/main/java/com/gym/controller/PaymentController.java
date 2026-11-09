package com.gym.controller;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.ChipVariant;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
 * every answer surfaces through an {@link M3Overlay} snackbar.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former payment.fxml: register pane (lookup, charge item, tender pads), financial ledger,
 * digital receipt drawer on liquid glass.
 */
public class PaymentController implements ScreenController {

    /** Demo dues per charge item; the default amount (499.00) anchors the membership fee. */
    private static final double MEMBERSHIP_DUE = 499.00;
    private static final double SUBSCRIPTION_DUE = 240.00;
    private static final double SESSION_DUE = 45.00;

    /**
     * Corner radius of the floating receipt sheet's liquid glass surface — the same 28px the
     * {@code .m3-bottom-sheet} stylesheet uses, so the glass keeps the sheet's geometry.
     */
    private static final double SHEET_GLASS_RADIUS = 28;

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private TextField txtSearchReceipt;
    private M3TextField txtCustomerLookup;
    private Label lblCustomerName;
    private Label lblCustomerBalance;
    private RadioButton rbMembership;
    private RadioButton rbSubscription;
    private RadioButton rbSession;
    private M3Chip chipMethodCard;
    private M3Chip chipMethodCash;
    private M3Chip chipMethodTransfer;
    private M3TextField txtPaymentAmount;
    private M3SplitButton actionSplitButton;
    private M3DataTable<String> paymentDataTable;
    private M3StatCard statRevenueCard;
    private M3StatCard statUnpaidCard;
    private M3Button btnShowReceipt;
    private M3Button btnVoidPayment;
    private M3BottomSheet receiptBottomSheet;
    private Label lblReceiptDetails;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Exclusive single-selection group behind the three tender-method chips. */
    private ToggleGroup methodGroup;

    /** Today's revenue behind the KPI card, seeded from the register value ($4,850.00). */
    private double todayRevenue = 4850.00;
    /** Outstanding balance behind the KPI card, seeded from the register value ($320.00). */
    private double todayUnpaid = 320.00;

    /** Demo receipt register: R-8801… map to the rows shown in the register table. */
    private final Map<String, String> demoReceipts = new LinkedHashMap<>();
    /** Next receipt number issued by the Record action. */
    private int nextReceiptNumber = 8801;
    /** Most recent register row activated by a click — the Void button's first target. */
    private String selectedPaymentRow;

    /**
     * Builds the complete Cashless POS &amp; Billing view and initializes all behavior. Same
     * ordering semantics the FXMLLoader used to provide: the node tree exists and every field
     * is assigned before the former {@code initialize()} work runs.
     */
    public PaymentController() {
        viewRoot = buildView();

        keepPaymentMethodsExclusive();
        populateRecentTransactions();
        applyReceiptSheetGlass();
        installFeedbackLayer(paymentDataTable);
        // Row activation marks the payment as the Void button's target (hand cursor affordance).

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        txtSearchReceipt.setId("txtSearchReceipt");
        txtCustomerLookup.setId("txtCustomerLookup");
        lblCustomerName.setId("lblCustomerName");
        lblCustomerBalance.setId("lblCustomerBalance");
        rbMembership.setId("rbMembership");
        rbSubscription.setId("rbSubscription");
        rbSession.setId("rbSession");
        chipMethodCard.setId("chipMethodCard");
        chipMethodCash.setId("chipMethodCash");
        chipMethodTransfer.setId("chipMethodTransfer");
        txtPaymentAmount.setId("txtPaymentAmount");
        actionSplitButton.setId("actionSplitButton");
        paymentDataTable.setId("paymentDataTable");
        statRevenueCard.setId("statRevenueCard");
        statUnpaidCard.setId("statUnpaidCard");
        btnShowReceipt.setId("btnShowReceipt");
        btnVoidPayment.setId("btnVoidPayment");
        receiptBottomSheet.setId("receiptBottomSheet");
        lblReceiptDetails.setId("lblReceiptDetails");
        paymentDataTable.setOnRowAction(this::selectPaymentRow);
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for payment.fxml) and wires
     * every handler.
     */
    private StackPane buildView() {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("billing-base-pane");

        // Top app bar.
        VBox topBarContainer = new VBox(8);
        topBarContainer.getStyleClass().add("top-bar-container");
        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(16, 24, 16, 24));
        txtSearchReceipt = new TextField();
        txtSearchReceipt.setPromptText("Search Receipt # (e.g. R-8801)");
        txtSearchReceipt.setPrefWidth(250);
        M3Button lookupBtn = new M3Button("Lookup Receipt", ButtonVariant.TONAL);
        lookupBtn.getStyleClass().add("m3-button-tonal");
        lookupBtn.setOnAction(e -> onReceiptLookupClick());
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topRow.getChildren().addAll(
                styledLabel("CASHLESS POS REGISTER & BILLING", "title-large"),
                topSpacer,
                txtSearchReceipt,
                lookupBtn);
        topBarContainer.getChildren().add(topRow);
        layout.setTop(topBarContainer);

        // Left half: cash register & tender engine.
        VBox registerPane = new VBox(16);
        registerPane.getStyleClass().add("register-pane");
        registerPane.setPadding(new Insets(16, 16, 64, 16));

        // Member lookup & balance card.
        M3Card balanceCard = new M3Card(CardVariant.OUTLINED);
        balanceCard.getStyleClass().add("customer-balance-card");
        balanceCard.setPadding(new Insets(12));
        VBox balanceContent = new VBox(8);
        balanceContent.getChildren().add(styledLabel("CUSTOMER LOOKUP", "label-small"));
        HBox lookupRow = new HBox(8);
        txtCustomerLookup = new M3TextField("Enter Customer ID or Phone",
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        HBox.setHgrow(txtCustomerLookup, Priority.ALWAYS);
        M3Button balanceBtn = new M3Button("Check Balance", ButtonVariant.OUTLINED);
        balanceBtn.getStyleClass().add("m3-button-outlined");
        balanceBtn.setOnAction(e -> onCheckBalanceClick());
        lookupRow.getChildren().addAll(txtCustomerLookup, balanceBtn);
        HBox balanceRow = new HBox();
        balanceRow.setAlignment(Pos.CENTER_LEFT);
        lblCustomerName = new Label("Athlete: Not Selected");
        lblCustomerName.getStyleClass().add("title-medium");
        lblCustomerBalance = new Label("Unpaid Balance: $0.00");
        lblCustomerBalance.getStyleClass().add("headline-small");
        Region balanceSpacer = new Region();
        HBox.setHgrow(balanceSpacer, Priority.ALWAYS);
        balanceRow.getChildren().addAll(lblCustomerName, balanceSpacer, lblCustomerBalance);
        balanceContent.getChildren().addAll(lookupRow, balanceRow);
        balanceCard.setContent(balanceContent);

        // Single item link selector (Rule 12).
        VBox itemSelector = new VBox(8);
        itemSelector.getChildren().add(styledLabel("SELECT ITEM TO SETTLE (EXACTLY ONE)", "label-small"));
        ToggleGroup itemGroup = new ToggleGroup();
        rbMembership = radio("Membership Agreement Due", itemGroup, true);
        rbSubscription = radio("Trainer Subscription Due", itemGroup, false);
        rbSession = radio("Single Ad-hoc Session Fee", itemGroup, false);
        itemSelector.getChildren().addAll(rbMembership, rbSubscription, rbSession);

        // Payment method selector.
        VBox methodSelector = new VBox(8);
        methodSelector.getChildren().add(styledLabel("PAYMENT METHOD", "label-small"));
        HBox methodRow = new HBox(8);
        chipMethodCard = new M3Chip("Card / NFC", ChipVariant.ASSIST);
        chipMethodCard.setSelected(true);
        chipMethodCash = new M3Chip("Cash", ChipVariant.ASSIST);
        chipMethodCash.setSelected(false);
        chipMethodTransfer = new M3Chip("Wire Transfer", ChipVariant.ASSIST);
        chipMethodTransfer.setSelected(false);
        methodRow.getChildren().addAll(chipMethodCard, chipMethodCash, chipMethodTransfer);
        methodSelector.getChildren().add(methodRow);

        // Amount field & tender pads.
        VBox amountSection = new VBox(10);
        amountSection.getChildren().add(styledLabel("CHARGE AMOUNT (BIGDECIMAL)", "label-small"));
        txtPaymentAmount = new M3TextField();
        txtPaymentAmount.setText("499.00");
        txtPaymentAmount.getStyleClass().add("currency-hero-field");
        HBox tenderPads = new HBox(8);
        M3Button tender20 = new M3Button("$20", ButtonVariant.TONAL);
        tender20.getStyleClass().add("m3-button-tonal");
        tender20.setOnAction(e -> onTender20());
        M3Button tender50 = new M3Button("$50", ButtonVariant.TONAL);
        tender50.getStyleClass().add("m3-button-tonal");
        tender50.setOnAction(e -> onTender50());
        M3Button tender100 = new M3Button("$100", ButtonVariant.TONAL);
        tender100.getStyleClass().add("m3-button-tonal");
        tender100.setOnAction(e -> onTender100());
        M3Button tenderExact = new M3Button("Exact Amount", ButtonVariant.OUTLINED);
        tenderExact.getStyleClass().add("m3-button-outlined");
        tenderExact.setOnAction(e -> onTenderExact());
        tenderPads.getChildren().addAll(tender20, tender50, tender100, tenderExact);
        amountSection.getChildren().addAll(txtPaymentAmount, tenderPads);

        Region registerSpacer = new Region();
        VBox.setVgrow(registerSpacer, Priority.ALWAYS);

        // Primary record split button.
        actionSplitButton = new M3SplitButton("RECORD PAYMENT (ENTER)", ButtonVariant.FILLED, null);
        actionSplitButton.getStyleClass().add("m3-split-button-filled");
        actionSplitButton.setMaxWidth(Double.MAX_VALUE);
        actionSplitButton.setOnAction(e -> onRecord());

        registerPane.getChildren().addAll(balanceCard, itemSelector, methodSelector,
                amountSection, registerSpacer, actionSplitButton);

        // Right half: financial ledger table.
        VBox ledgerPane = new VBox(16);
        ledgerPane.setPadding(new Insets(16, 16, 64, 16));

        // Telemetry row.
        HBox telemetryRow = new HBox(16);
        statRevenueCard = new M3StatCard("Today's Revenue", "$4,850.00", "Voided excluded");
        statUnpaidCard = new M3StatCard("Accounts Receivable", "$320.00", "Pending settlement");
        HBox.setHgrow(statRevenueCard, Priority.ALWAYS);
        HBox.setHgrow(statUnpaidCard, Priority.ALWAYS);
        telemetryRow.getChildren().addAll(statRevenueCard, statUnpaidCard);

        // Ledger data table.
        paymentDataTable = new M3DataTable<>();
        VBox.setVgrow(paymentDataTable, Priority.ALWAYS);

        // Action toolbar.
        HBox ledgerToolbar = new HBox(12);
        ledgerToolbar.setAlignment(Pos.CENTER_RIGHT);
        btnShowReceipt = new M3Button("View / Print Receipt", ButtonVariant.TONAL);
        btnShowReceipt.getStyleClass().add("m3-button-tonal");
        btnShowReceipt.setOnAction(e -> onShowReceiptClick());
        btnVoidPayment = new M3Button("Void Payment", ButtonVariant.TEXT);
        btnVoidPayment.getStyleClass().add("m3-button-text-danger");
        btnVoidPayment.setOnAction(e -> onVoidClick());
        ledgerToolbar.getChildren().addAll(btnShowReceipt, btnVoidPayment);

        ledgerPane.getChildren().addAll(telemetryRow, paymentDataTable, ledgerToolbar);

        SplitPane workspaceSplit = new SplitPane(registerPane, ledgerPane);
        workspaceSplit.setDividerPositions(0.44);
        layout.setCenter(workspaceSplit);

        // Bottom digital receipt drawer (M3BottomSheet).
        receiptBottomSheet = new M3BottomSheet();
        receiptBottomSheet.setVisible(false);
        StackPane.setAlignment(receiptBottomSheet, Pos.BOTTOM_CENTER);
        VBox receiptContent = new VBox(12);
        receiptContent.getStyleClass().add("receipt-paper-surface");
        receiptContent.setPadding(new Insets(20, 24, 20, 24));
        lblReceiptDetails = new Label("Receipt Details...");
        lblReceiptDetails.getStyleClass().add("body-medium");
        HBox receiptActions = new HBox(12);
        receiptActions.setAlignment(Pos.CENTER_RIGHT);
        M3Button printBtn = new M3Button("Print Thermal Copy", ButtonVariant.FILLED);
        printBtn.getStyleClass().add("m3-button-filled");
        M3Button closeBtn = new M3Button("Close", ButtonVariant.OUTLINED);
        closeBtn.getStyleClass().add("m3-button-outlined");
        closeBtn.setOnAction(e -> onCloseReceiptClick());
        receiptActions.getChildren().addAll(printBtn, closeBtn);
        receiptContent.getChildren().addAll(
                styledLabel("DIGITAL FISCAL RECEIPT", "title-large"),
                lblReceiptDetails,
                receiptActions);
        receiptBottomSheet.setContent(receiptContent);

        viewRoot = new StackPane();
        viewRoot.getStyleClass().add("billing-root");
        viewRoot.getChildren().addAll(layout, receiptBottomSheet);
        return viewRoot;
    }

    /** Builds one radio button inside the given group. */
    private static RadioButton radio(String text, ToggleGroup group, boolean selected) {
        RadioButton button = new RadioButton(text);
        button.setToggleGroup(group);
        button.setSelected(selected);
        return button;
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
     * Re-parents the receipt sheet into a REGULAR interactive liquid glass pane that takes
     * over the sheet's position, constraints and visibility. Defensive by construction: any
     * failure leaves the plain sheet in place, because glass chrome must never break a screen.
     *
     * @param sheet the sheet node shown/hidden by the action handlers
     * @param errorPrefix stderr prefix identifying the caller in the log
     */
    private static void wrapSheetInGlass(Region sheet, String errorPrefix) {
        try {
            if (!(sheet.getParent() instanceof Pane parent)) {
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
            // The view flow toggles the sheet node directly; the glass chrome mirrors it so the
            // existing show/hide handlers keep working unchanged.
            glass.visibleProperty().bind(sheet.visibleProperty());
            // Preserve the placement constraints (they live on the child node).
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
        if (!paymentDataTable.getColumns().isEmpty()) {
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
        methodGroup = new ToggleGroup();
        methodGroup.getToggles().addAll(chipMethodCard, chipMethodCash, chipMethodTransfer);
    }

    /**
     * Records the current charge: validates the amount inline (M3TextField error), reads the
     * active tender chip and charge item, appends a Paid row to the register, advances the
     * revenue KPI and confirms with a snackbar carrying the new receipt number.
     */
    public void onRecord() {
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

    /**
     * Receipt lookup: validates the receipt number, answers the demo register R-8801… and opens
     * the receipt sheet with the stored row; unknown numbers say so instead of failing silently.
     */
    public void onReceiptLookupClick() {
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

    /**
     * Balance lookup: validates the input inline (M3TextField error), finds the member in the
     * visible register and sums their Pending rows into the balance readout.
     */
    public void onCheckBalanceClick() {
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

    /**
     * Two-step void (destructive action): an M3Dialog confirms first, then the target row
     * visibly flips to Voided, the revenue/unpaid KPIs adjust and a snackbar confirms.
     */
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

    public void onShowReceiptClick() {
        receiptBottomSheet.setVisible(true);
    }

    public void onCloseReceiptClick() {
        receiptBottomSheet.setVisible(false);
    }

    public void onTender20() {
        txtPaymentAmount.setText("20.00");
    }

    public void onTender50() {
        txtPaymentAmount.setText("50.00");
    }

    public void onTender100() {
        txtPaymentAmount.setText("100.00");
    }

    /** Loads the exact due amount of the currently selected charge item into the amount field. */
    public void onTenderExact() {
        txtPaymentAmount.setText(String.format(Locale.ROOT, "%.2f", selectedChargeDue()));
        txtPaymentAmount.setError(false);
        feedback.showSnackbar("Exact due amount loaded for " + selectedChargeItem() + " (demo).");
    }

    /** Voids the payment row of the given member id (demo register lookup), if present. */
    public void onVoid(int paymentId) {
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
        String row = demoReceipts.get(receiptNo);
        lblReceiptDetails.setText(row == null ? "Receipt: " + receiptNo : receiptText(receiptNo, row));
        receiptBottomSheet.setVisible(true);
    }

    // ------------------------------------------------------------------
    // Demo register helpers
    // ------------------------------------------------------------------

    /** Marks the register row Voided, adjusts the revenue/unpaid KPIs and confirms. */
    private void voidRow(String row) {
        if (!paymentDataTable.getItems().contains(row)) {
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
            Toggle selected = methodGroup.getSelectedToggle();
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
        if (rbSubscription.isSelected()) {
            return "trainer subscription";
        }
        if (rbSession.isSelected()) {
            return "single session";
        }
        return "membership";
    }

    /** Demo dues for the selected charge item (see the constants above for the anchors). */
    private double selectedChargeDue() {
        if (rbSubscription.isSelected()) {
            return SUBSCRIPTION_DUE;
        }
        if (rbSession.isSelected()) {
            return SESSION_DUE;
        }
        return MEMBERSHIP_DUE;
    }

    /**
     * The charge recipient: the athlete set by the balance lookup, or the walk-in default when
     * no lookup answered yet.
     */
    private String currentCustomerLabel() {
        String label = lblCustomerName.getText() == null ? "" : lblCustomerName.getText().trim();
        if (label.startsWith("Athlete: ") && !label.equals("Athlete: Not Selected")
                && !label.startsWith("Athlete: Not found")) {
            return label.substring("Athlete: ".length());
        }
        return "Walk-in member";
    }

    private void updateRevenueKpi() {
        statRevenueCard.setValue(String.format(Locale.ROOT, "$%,.2f", todayRevenue));
    }

    private void updateUnpaidKpi() {
        statUnpaidCard.setValue(String.format(Locale.ROOT, "$%,.2f", todayUnpaid));
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
