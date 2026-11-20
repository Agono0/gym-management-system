package com.gym.controller;

import com.gym.dao.CustomerDao;
import com.gym.dao.MembershipDao;
import com.gym.dao.PaymentDao;
import com.gym.dao.SessionDao;
import com.gym.dao.TrainerSubscriptionDao;
import com.gym.model.Customer;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.Payment;
import com.gym.model.PaymentMethod;
import com.gym.model.Session;
import com.gym.model.SessionStatus;
import com.gym.model.TrainerSubscription;
import com.gym.model.SubscriptionStatus;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
import com.gym.util.PhoneUtil;
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
import io.m3fx.controls.textfield.TextFieldVariant;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for Cashless POS Register &amp; Split Billing screen (ACT-18-BILLING).
 *
 * <p>Data contract (DECISIONS.md 77/78, task C6): the ledger, the KPIs and the balance lookup
 * run on real {@code PaymentDao} rows through one {@link Data#loadAsync} snapshot, with the
 * rule-6 outstanding balance mirrored exactly from the CLI's {@code GymRulesHelper
 * .computeOutstandingBalance} (non-CANCELLED memberships/subscriptions priced minus their
 * non-voided payments, non-cancelled non-subscription sessions with a positive price, voided
 * money never counting). Record inserts through {@code PaymentDao} with rule 12 (exactly one
 * linked item; the DAO surfaces any customer-match refusal); the target item is the
 * customer's oldest unpaid item of the selected type, resolved by the balance lookup. Void
 * follows rule 19 — void fields on the row (previous amount, reason, date), no hard delete —
 * and offers a same-item replacement payment so receipts reconcile. The receipt lookup uses
 * {@code PaymentDao.findByReceiptNo} and the receipt sheet renders the real, itemized row.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former payment.fxml: register pane (lookup, charge item, tender pads), financial ledger,
 * digital receipt drawer on liquid glass.
 */
public class PaymentController implements ScreenController {

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
    /** Most recent ledger row activated by a click — the Void button's first target. */
    private String selectedPaymentRow;

    /** Exclusive single-selection group behind the three tender-method chips. */
    private ToggleGroup methodGroup;

    /** Content host around the workspace: swaps loading/error states around DAO loads. */
    private StackPane contentHost;
    /** The workspace node, restored after loading/error states. */
    private Node workspaceView;

    /** Rows of the current snapshot in table order; the payment id travels in column 5. */
    private final Map<String, Integer> rowPaymentIds = new HashMap<>();
    /** Customers of the current snapshot, kept for the lookup flow. */
    private final List<Customer> customerDirectory = new ArrayList<>();
    /** The customer resolved by the last balance lookup, or null. */
    private Customer resolvedCustomer;
    /** The resolved customer's unpaid items by type (rule 6), refreshed with every lookup. */
    private final Map<String, List<UnpaidItem>> unpaidByType = new HashMap<>();

    /** One unpaid rule-6 item (the record-payment target). */
    private record UnpaidItem(String type, int itemId, double outstanding) {
    }

    /** The immutable result of one background snapshot. */
    private record PaymentSnapshot(List<String> ledgerRows, Map<String, Integer> rowIds,
                                   double revenueToday, double totalOutstanding) {
    }

    /**
     * Builds the complete Cashless POS &amp; Billing view and initializes all behavior. Same
     * ordering semantics the FXMLLoader used to provide: the node tree exists and every field
     * is assigned before the former {@code initialize()} work runs.
     */
    public PaymentController() {
        viewRoot = buildView();

        keepPaymentMethodsExclusive();
        wireLedgerColumns();
        applyReceiptSheetGlass();
        installFeedbackLayer(paymentDataTable);
        loadPayments();

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
        txtSearchReceipt.setPromptText("Search receipt # (e.g. REC-1002)");
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
                TextFieldVariant.FILLED);
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
        amountSection.getChildren().add(styledLabel("CHARGE AMOUNT", "label-small"));
        txtPaymentAmount = new M3TextField();
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
        statRevenueCard = new M3StatCard("Today's Revenue", "-", "Voided excluded");
        statUnpaidCard = new M3StatCard("Accounts Receivable", "-", "Rule-6 outstanding, all members");
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
        M3Button previewBtn = new M3Button("Receipt Preview", ButtonVariant.FILLED);
        previewBtn.getStyleClass().add("m3-button-filled");
        previewBtn.setOnAction(e -> onShowReceiptClick());
        M3Button closeBtn = new M3Button("Close", ButtonVariant.OUTLINED);
        closeBtn.getStyleClass().add("m3-button-outlined");
        closeBtn.setOnAction(e -> onCloseReceiptClick());
        receiptActions.getChildren().addAll(previewBtn, closeBtn);
        receiptContent.getChildren().addAll(
                styledLabel("DIGITAL FISCAL RECEIPT", "title-large"),
                lblReceiptDetails,
                receiptActions);
        receiptBottomSheet.setContent(receiptContent);

        // Content host so the real-data load can swap the workspace for loading/error states.
        workspaceView = workspaceSplit;
        contentHost = new StackPane(workspaceSplit);
        layout.setCenter(contentHost);

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

    /** Wires the ledger columns once; the fifth (off-width) column carries the payment id. */
    private void wireLedgerColumns() {
        paymentDataTable.getColumns().addAll(List.of(
                new M3DataTable.Column<>("Date", r -> rowField(r, 0), 100),
                new M3DataTable.Column<>("Member", r -> rowField(r, 1), 190),
                new M3DataTable.Column<>("Method", r -> rowField(r, 2), 100),
                new M3DataTable.Column<>("Amount", r -> rowField(r, 3), 90),
                new M3DataTable.Column<>("Status", r -> rowField(r, 4), 90)));
    }

    /** One pipe-separated ledger field (the hidden 5th field carries the payment id). */
    private static String rowField(String row, int index) {
        String[] parts = row.split("\\|", -1);
        return index < parts.length ? parts[index] : "";
    }

    /**
     * Loads the real payments on a background task: the bootstrap, the DAO queries and the
     * snapshot computation run off the FX thread; the ledger and KPIs land on the FX thread.
     * Shows {@link UiKit#loadingState()} while running and {@link UiKit#errorState()} with
     * retry on failure.
     */
    private void loadPayments() {
        Data.loadAsync(contentHost,
            () -> {
                Data.ensureReady();
                return buildSnapshot();
            },
            snapshot -> {
                contentHost.getChildren().set(0, workspaceView);
                applySnapshot(snapshot);
            },
            error -> contentHost.getChildren().setAll(UiKit.errorState(
                    error.getMessage() == null ? error.toString() : error.getMessage(),
                    this::loadPayments)));
    }

    /**
     * Queries the payments, customers, memberships, subscriptions and sessions; computes the
     * ledger rows, today's revenue (voided excluded) and the total rule-6 outstanding across
     * every customer (mirroring the CLI's {@code GymRulesHelper.computeOutstandingBalance}).
     * Runs on the background thread — no JavaFX types touched here.
     */
    private PaymentSnapshot buildSnapshot() throws Exception {
        Data.ensureReady();
        LocalDate today = LocalDate.now();
        List<Payment> payments = new PaymentDao().findAll();
        List<Customer> customers = new CustomerDao().findAll();
        List<Membership> memberships = new MembershipDao().findAll();
        List<TrainerSubscription> subscriptions = new TrainerSubscriptionDao().findAll();
        List<Session> sessions = new SessionDao().findAll();

        Map<Integer, String> customerNames = new HashMap<>();
        for (Customer customer : customers) {
            customerNames.put(customer.getId(), customer.getFullName());
        }

        // Ledger rows, newest first; the payment id travels in the hidden 5th column.
        List<String> rows = new ArrayList<>();
        Map<String, Integer> rowIds = new HashMap<>();
        double revenueToday = 0;
        for (int i = payments.size() - 1; i >= 0; i--) {
            Payment payment = payments.get(i);
            String statusLabel = payment.isVoided() ? "Voided" : "Paid";
            if (!payment.isVoided() && today.toString().equals(payment.getDate())) {
                revenueToday += payment.getAmount().doubleValue();
            }
            String row = orEmpty(payment.getDate()) + "|"
                    + customerNames.getOrDefault(payment.getCustomerId(), "Customer #"
                            + payment.getCustomerId()) + "|"
                    + (payment.getMethod() == null ? "-" : payment.getMethod().name()) + "|"
                    + String.format(Locale.ROOT, "%.2f",
                            payment.getAmount() == null ? 0 : payment.getAmount().doubleValue())
                    + "|" + statusLabel + "|" + payment.getId();
            rowIds.put(row, payment.getId());
            rows.add(row);
        }

        // Rule 6, mirrored from the CLI: unpaid = price - non-voided payments, per item,
        // across memberships, subscriptions and non-subscription sessions.
        double totalOutstanding = 0;
        for (Customer customer : customers) {
            totalOutstanding += computeOutstandingBalance(customer.getId(), payments,
                    memberships, subscriptions, sessions);
        }

        return new PaymentSnapshot(rows, rowIds, revenueToday, totalOutstanding);
    }

    /**
     * Rule 6 outstanding balance for one customer — mirrors the CLI's
     * {@code GymRulesHelper.computeOutstandingBalance}: non-CANCELLED memberships and
     * subscriptions owed their price minus non-voided payments; non-cancelled,
     * non-subscription-covered sessions with a positive price owed the same; voided payments
     * never count.
     */
    private static double computeOutstandingBalance(int customerId, List<Payment> payments,
                                                   List<Membership> memberships,
                                                   List<TrainerSubscription> subscriptions,
                                                   List<Session> sessions) {
        double total = 0;
        for (Membership membership : memberships) {
            if (membership.getCustomerId() != customerId
                    || membership.getStatus() == MembershipStatus.CANCELLED
                    || membership.getPrice() == null) {
                continue;
            }
            double owed = membership.getPrice().doubleValue()
                    - sumNonVoidedForItem(payments, "membership", membership.getId());
            total += Math.max(0, owed);
        }
        for (TrainerSubscription subscription : subscriptions) {
            if (subscription.getCustomerId() != customerId
                    || subscription.getStatus() == SubscriptionStatus.CANCELLED
                    || subscription.getRatePerPeriod() == null) {
                continue;
            }
            double owed = subscription.getRatePerPeriod().doubleValue()
                    - sumNonVoidedForItem(payments, "subscription", subscription.getId());
            total += Math.max(0, owed);
        }
        for (Session session : sessions) {
            if (session.getCustomerId() != customerId
                    || session.getSubscriptionId() != null
                    || session.getStatus() == SessionStatus.CANCELLED
                    || session.getPrice() == null
                    || session.getPrice().doubleValue() <= 0) {
                continue;
            }
            double owed = session.getPrice().doubleValue()
                    - sumNonVoidedForItem(payments, "session", session.getId());
            total += Math.max(0, owed);
        }
        return total;
    }

    /** Sums the non-voided payments linked to one item (rule 12 target types). */
    private static double sumNonVoidedForItem(List<Payment> payments, String type, int itemId) {
        double sum = 0;
        for (Payment payment : payments) {
            if (payment.isVoided()) {
                continue;
            }
            Integer target = switch (type) {
                case "membership" -> payment.getMembershipId();
                case "subscription" -> payment.getSubscriptionId();
                default -> payment.getSessionId();
            };
            if (target != null && target == itemId) {
                sum += payment.getAmount().doubleValue();
            }
        }
        return sum;
    }

    /** The unpaid rule-6 items of one customer, keyed by the charge-item radio type. */
    private Map<String, List<UnpaidItem>> unpaidItemsFor(int customerId, List<Payment> payments,
                                                         List<Membership> memberships,
                                                         List<TrainerSubscription> subscriptions,
                                                         List<Session> sessions) {
        Map<String, List<UnpaidItem>> unpaid = new HashMap<>();
        unpaid.put("membership", new ArrayList<>());
        unpaid.put("subscription", new ArrayList<>());
        unpaid.put("session", new ArrayList<>());
        for (Membership membership : memberships) {
            if (membership.getCustomerId() != customerId
                    || membership.getStatus() == MembershipStatus.CANCELLED
                    || membership.getPrice() == null) {
                continue;
            }
            double outstanding = membership.getPrice().doubleValue()
                    - sumNonVoidedForItem(payments, "membership", membership.getId());
            if (outstanding > 0.005) {
                unpaid.get("membership").add(new UnpaidItem("membership",
                        membership.getId(), outstanding));
            }
        }
        for (TrainerSubscription subscription : subscriptions) {
            if (subscription.getCustomerId() != customerId
                    || subscription.getStatus() == SubscriptionStatus.CANCELLED
                    || subscription.getRatePerPeriod() == null) {
                continue;
            }
            double outstanding = subscription.getRatePerPeriod().doubleValue()
                    - sumNonVoidedForItem(payments, "subscription", subscription.getId());
            if (outstanding > 0.005) {
                unpaid.get("subscription").add(new UnpaidItem("subscription",
                        subscription.getId(), outstanding));
            }
        }
        for (Session session : sessions) {
            if (session.getCustomerId() != customerId
                    || session.getSubscriptionId() != null
                    || session.getStatus() == SessionStatus.CANCELLED
                    || session.getPrice() == null
                    || session.getPrice().doubleValue() <= 0) {
                continue;
            }
            double outstanding = session.getPrice().doubleValue()
                    - sumNonVoidedForItem(payments, "session", session.getId());
            if (outstanding > 0.005) {
                unpaid.get("session").add(new UnpaidItem("session",
                        session.getId(), outstanding));
            }
        }
        return unpaid;
    }

    /** Applies a finished snapshot: ledger rows and KPI values. */
    private void applySnapshot(PaymentSnapshot snapshot) {
        rowPaymentIds.clear();
        rowPaymentIds.putAll(snapshot.rowIds());
        paymentDataTable.getItems().setAll(snapshot.ledgerRows());
        statRevenueCard.setValue(String.format(Locale.ROOT, "$%,.2f",
                snapshot.revenueToday()));
        statUnpaidCard.setValue(String.format(Locale.ROOT, "$%,.2f",
                snapshot.totalOutstanding()));
    }

    /** Puts the three payment-method chips in one ToggleGroup so exactly one tender type is active. */
    private void keepPaymentMethodsExclusive() {
        methodGroup = new ToggleGroup();
        methodGroup.getToggles().addAll(chipMethodCard, chipMethodCash, chipMethodTransfer);
    }

    /**
     * Rule-5-style customer resolution: the query matches a customer id ("6" or "M-0006"), an
     * Egyptian phone (rule: 11 digits starting 010/011/012/015) or a name fragment. On a hit,
     * the customer's real rule-6 outstanding and unpaid items load on a background task.
     */
    public void onCheckBalanceClick() {
        String query = txtCustomerLookup.getText() == null ? "" : txtCustomerLookup.getText().trim();
        if (query.isEmpty()) {
            txtCustomerLookup.setError(true);
            txtCustomerLookup.setSupportingText("Enter the customer id (M-0006), phone or name.");
            feedback.showSnackbar("Enter a customer to look up first.");
            return;
        }
        txtCustomerLookup.setError(false);
        txtCustomerLookup.setSupportingText("");
        String normalized = query;
        Data.submit(
                () -> {
                    Data.ensureReady();
                    List<Customer> customers = new CustomerDao().findAll();
                    Customer hit = null;
                    if (normalized.matches("(?i)M-?\\d{1,6}")) {
                        int id = Integer.parseInt(normalized.replaceAll("(?i)M-?", ""));
                        for (Customer customer : customers) {
                            if (customer.getId() == id) {
                                hit = customer;
                            }
                        }
                    } else if (PhoneUtil.isValid(normalized)) {
                        for (Customer customer : customers) {
                            if (normalized.equals(customer.getPhone())) {
                                hit = customer;
                            }
                        }
                    } else {
                        for (Customer customer : customers) {
                            if (customer.getFullName() != null && customer.getFullName()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(normalized.toLowerCase(Locale.ROOT))) {
                                hit = customer;
                            }
                        }
                    }
                    if (hit == null) {
                        return new Object[] {null, 0.0, new HashMap<String, List<UnpaidItem>>()};
                    }
                    List<Payment> payments = new PaymentDao().findAll();
                    List<Membership> memberships = new MembershipDao().findAll();
                    List<TrainerSubscription> subscriptions = new TrainerSubscriptionDao().findAll();
                    List<Session> sessions = new SessionDao().findAll();
                    double outstanding = computeOutstandingBalance(hit.getId(), payments,
                            memberships, subscriptions, sessions);
                    Map<String, List<UnpaidItem>> unpaid =
                            unpaidItemsFor(hit.getId(), payments, memberships, subscriptions, sessions);
                    return new Object[] {hit, outstanding, unpaid};
                },
                read -> {
                    Customer hit = (Customer) read[0];
                    double outstanding = (Double) read[1];
                    @SuppressWarnings("unchecked")
                    Map<String, List<UnpaidItem>> unpaid =
                            (Map<String, List<UnpaidItem>>) read[2];
                    if (hit == null) {
                        resolvedCustomer = null;
                        unpaidByType.clear();
                        lblCustomerName.setText("Athlete: Not found for \"" + query + "\"");
                        lblCustomerBalance.setText("Unpaid Balance: $0.00");
                        feedback.showSnackbar("No customer matches \"" + query + "\".");
                        return;
                    }
                    resolvedCustomer = hit;
                    unpaidByType.clear();
                    unpaidByType.putAll(unpaid);
                    lblCustomerName.setText("Athlete: " + hit.getFullName() + " (M-"
                            + String.format("%04d", hit.getId()) + ")");
                    lblCustomerBalance.setText("Unpaid Balance: $"
                            + String.format(Locale.ROOT, "%.2f", outstanding)
                            + " (rule 6)");
                    feedback.showSnackbar(hit.getFullName() + " has $"
                            + String.format(Locale.ROOT, "%.2f", outstanding)
                            + " outstanding across unpaid items.");
                },
                error -> feedback.showSnackbar("Could not look up the customer: "
                        + error.getMessage()));
    }

    /**
     * Rule-12 record: the resolved customer's oldest unpaid item of the selected type is the
     * payment's single link (exactly one of membershipId/subscriptionId/sessionId), the amount
     * defaults to that item's outstanding, and the DAO surfaces any rule-12 customer-match
     * refusal verbatim.
     */
    public void onRecord() {
        if (resolvedCustomer == null) {
            feedback.showSnackbar("Run a customer lookup first — a payment must link to a"
                    + " customer and exactly one item (rule 12).");
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

        String type = selectedChargeItem();
        List<UnpaidItem> candidates = unpaidByType.getOrDefault(type, List.of());
        if (candidates.isEmpty()) {
            feedback.showSnackbar("No unpaid " + type + " items on file for "
                    + resolvedCustomer.getFullName() + " — nothing to settle (rule 6).");
            return;
        }
        UnpaidItem item = candidates.get(0); // oldest unpaid item of the selected type
        PaymentMethod method = selectedMethod();

        Data.submit(
                () -> {
                    String receiptNo = nextReceiptNumber(new PaymentDao().findAll());
                    Payment payment = switch (type) {
                        case "membership" -> Payment.forMembership(resolvedCustomer.getId(),
                                item.itemId(), java.math.BigDecimal.valueOf(amount), method,
                                LocalDate.now().toString(), receiptNo);
                        case "subscription" -> Payment.forSubscription(resolvedCustomer.getId(),
                                item.itemId(), java.math.BigDecimal.valueOf(amount), method,
                                LocalDate.now().toString(), receiptNo);
                        default -> Payment.forSession(resolvedCustomer.getId(),
                                item.itemId(), java.math.BigDecimal.valueOf(amount), method,
                                LocalDate.now().toString(), receiptNo);
                    };
                    return new PaymentDao().insert(payment);
                },
                recorded -> {
                    feedback.showSnackbar("Recorded " + type + " payment — $"
                            + String.format(Locale.ROOT, "%.2f", amount) + " via " + method
                            + " for " + resolvedCustomer.getFullName() + " (receipt "
                            + recorded.getReceiptNo() + ").");
                    txtPaymentAmount.setText("");
                    resolvedCustomer = null;
                    unpaidByType.clear();
                    lblCustomerName.setText("Athlete: Not Selected");
                    lblCustomerBalance.setText("Unpaid Balance: $0.00");
                    loadPayments();
                },
                error -> feedback.showSnackbar("Could not record payment: " + error.getMessage()));
    }

    /** Issues the next unique receipt number from the existing rows (REC-####). */
    private static String nextReceiptNumber(List<Payment> payments) {
        int max = 1000;
        for (Payment payment : payments) {
            String receipt = payment.getReceiptNo() == null ? "" : payment.getReceiptNo().trim();
            if (receipt.matches("REC-\\d+")) {
                max = Math.max(max, Integer.parseInt(receipt.substring(4)));
            }
        }
        return "REC-" + (max + 1);
    }

    /**
     * Rule-19 void: two-step confirm with a reason field, then the void fields are written on
     * the row (previous amount, reason, date — no hard delete) and a same-item replacement
     * payment is offered so receipts reconcile.
     */
    public void onVoidClick() {
        String row = voidTargetRow();
        if (row == null) {
            feedback.showSnackbar("Nothing to void — every ledger row is already voided.");
            return;
        }
        int paymentId = rowPaymentIds.getOrDefault(row, -1);
        if (paymentId < 0) {
            feedback.showSnackbar("Select a ledger row first.");
            return;
        }
        String[] parts = row.split("\\|", -1);
        TextField reasonField = new TextField();
        reasonField.setPromptText("Required (rule 19) — recorded on the payment");
        M3Button cancel = M3Button.text("Keep Payment");
        M3Button voidButton = M3Button.text("Void Payment");
        M3Dialog dialog = M3Dialog.basic("Void this payment?",
                new VBox(10,
                        new Label("Payment " + parts[1] + " (" + parts[2] + ", $"
                                + String.format(Locale.ROOT, "%.2f",
                                        parseAmount(parts[3]))
                                + ") is reversed with a recorded reason — the row stays in the"
                                + " ledger (rule 19, no hard delete)."),
                        reasonField),
                cancel, voidButton);
        dialog.setDefaultAction(voidButton::activate);
        cancel.setOnAction(e -> dialog.hide());
        voidButton.setOnAction(e -> {
            String reason = reasonField.getText() == null ? "" : reasonField.getText().trim();
            if (reason.isEmpty()) {
                reasonField.setStyle("-fx-border-color: -md-sys-color-error;");
                return;
            }
            dialog.hide();
            Data.submit(
                    () -> {
                        Payment current = new PaymentDao().findById(paymentId);
                        if (current == null) {
                            throw new com.gym.exception.ValidationException(
                                    "Payment no longer exists in the database");
                        }
                        if (current.isVoided()) {
                            throw new com.gym.exception.ValidationException(
                                    "Rule 19: Payment #" + paymentId + " is already voided");
                        }
                        current.setPreviousAmount(current.getAmount());
                        current.setVoided(true);
                        current.setVoidReason(reason);
                        current.setVoidedDate(LocalDate.now().toString());
                        new PaymentDao().update(current);
                        return current;
                    },
                    voided -> {
                        feedback.showSnackbar("Payment " + voided.getReceiptNo() + " voided — $"
                                + String.format(Locale.ROOT, "%.2f",
                                        voided.getPreviousAmount() == null
                                                ? 0 : voided.getPreviousAmount().doubleValue())
                                + " reversed; the linked item is unpaid again (rule 6).");
                        offerReplacementPayment(voided);
                        loadPayments();
                    },
                    error -> feedback.showSnackbar("Could not void payment: " + error.getMessage()));
        });
        feedback.showDialog(dialog);
    }

    /**
     * Rule 19: after a void, offers the same-item replacement payment so receipts reconcile;
     * the replacement links to the very same membership/subscription/session id.
     */
    private void offerReplacementPayment(Payment voided) {
        if (voided.getPreviousAmount() == null
                || voided.getPreviousAmount().doubleValue() <= 0) {
            return;
        }
        M3Button later = M3Button.text("Not Now");
        M3Button replace = M3Button.text("Record Replacement");
        M3Dialog dialog = M3Dialog.basic("Record a replacement payment?",
                new Label("The linked item is unpaid again. Record a replacement of $"
                        + String.format(Locale.ROOT, "%.2f",
                                voided.getPreviousAmount().doubleValue())
                        + " for the same item so the receipts reconcile (rule 19)?"),
                later, replace);
        dialog.setDefaultAction(replace::activate);
        later.setOnAction(e -> dialog.hide());
        replace.setOnAction(e -> {
            dialog.hide();
            Data.submit(
                    () -> {
                        String receiptNo = nextReceiptNumber(new PaymentDao().findAll());
                        Payment replacement = new Payment(0, voided.getCustomerId(),
                                voided.getMembershipId(), voided.getSubscriptionId(),
                                voided.getSessionId(), voided.getPreviousAmount(),
                                voided.getMethod(), LocalDate.now().toString(),
                                receiptNo, false);
                        return new PaymentDao().insert(replacement);
                    },
                    replacement -> {
                        feedback.showSnackbar("Replacement payment recorded — receipt "
                                + replacement.getReceiptNo() + ".");
                        loadPayments();
                    },
                    error -> feedback.showSnackbar("Could not record the replacement: "
                            + error.getMessage()));
        });
        feedback.showDialog(dialog);
    }

    /**
     * Receipt lookup through {@code PaymentDao.findByReceiptNo}: opens the receipt sheet with
     * the real, itemized row; unknown numbers say so instead of failing silently.
     */
    public void onReceiptLookupClick() {
        String query = txtSearchReceipt.getText() == null ? "" : txtSearchReceipt.getText().trim();
        if (query.isEmpty()) {
            feedback.showSnackbar("Enter a receipt number first, for example REC-1002.");
            return;
        }
        Data.submit(
                () -> {
                    Data.ensureReady();
                    return new PaymentDao().findByReceiptNo(query.toUpperCase(Locale.ROOT));
                },
                payment -> {
                    if (payment == null) {
                        feedback.showSnackbar("No receipt matches \"" + query + "\".");
                        return;
                    }
                    showReceipt(payment);
                    feedback.showSnackbar("Receipt " + payment.getReceiptNo() + " opened.");
                },
                error -> feedback.showSnackbar("Could not look up the receipt: "
                        + error.getMessage()));
    }

    public void onShowReceiptClick() {
        if (selectedPaymentRow == null) {
            feedback.showSnackbar("Select a ledger row, or look up a receipt number first.");
            return;
        }
        Integer paymentId = rowPaymentIds.get(selectedPaymentRow);
        if (paymentId == null) {
            feedback.showSnackbar("Select a ledger row first.");
            return;
        }
        Data.submit(
                () -> new PaymentDao().findById(paymentId),
                payment -> {
                    if (payment == null) {
                        notice("The selected payment no longer exists.");
                        return;
                    }
                    showReceipt(payment);
                },
                error -> notice("Could not load the receipt: " + error.getMessage()));
    }

    /** Renders the real, itemized payment row into the receipt sheet. */
    private void showReceipt(Payment payment) {
        lblReceiptDetails.setText(receiptText(payment));
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

    /** Loads the exact outstanding amount of the currently selected charge item type. */
    public void onTenderExact() {
        if (resolvedCustomer == null) {
            feedback.showSnackbar("Run a customer lookup first — the exact amount comes from"
                    + " their rule-6 balance.");
            return;
        }
        String type = selectedChargeItem();
        List<UnpaidItem> candidates = unpaidByType.getOrDefault(type, List.of());
        double exact = candidates.isEmpty() ? 0 : candidates.get(0).outstanding();
        txtPaymentAmount.setText(String.format(Locale.ROOT, "%.2f", exact));
        txtPaymentAmount.setError(false);
        feedback.showSnackbar("Exact outstanding loaded for the " + type + " item.");
    }

    /** Voids the payment row of the given member id (ledger lookup), if present. */
    public void onVoid(int paymentId) {
        for (String row : paymentDataTable.getItems()) {
            Integer id = rowPaymentIds.get(row);
            if (id != null && id == paymentId && !"Voided".equals(rowField(row, 4))) {
                selectedPaymentRow = row;
                onVoidClick();
                return;
            }
        }
        feedback.showSnackbar("No voidable payment for member #" + paymentId + ".");
    }

    /**
     * Renders the real, itemized payment into the receipt sheet text (a thermal-copy preview:
     * receipt number, date, member, method, linked item, amount, status and void fields).
     */
    private String receiptText(Payment payment) {
        return "Receipt " + orEmpty(payment.getReceiptNo()) + "\n"
                + "Date: " + orEmpty(payment.getDate()) + "\n"
                + "Customer: #" + payment.getCustomerId() + "\n"
                + "Method: " + (payment.getMethod() == null ? "-" : payment.getMethod().name())
                + "\nAmount: $" + String.format(Locale.ROOT, "%.2f",
                        payment.getAmount() == null ? 0 : payment.getAmount().doubleValue()) + "\n"
                + "Status: " + (payment.isVoided() ? "VOIDED" : "PAID") + "\n"
                + (payment.getVoidReason() == null ? "" : "Void reason: "
                        + payment.getVoidReason() + "\n")
                + (payment.getVoidedDate() == null ? "" : "Voided on: "
                        + payment.getVoidedDate() + "\n")
                + "Linked to: "
                + (payment.getMembershipId() != null ? "membership #" + payment.getMembershipId()
                : payment.getSubscriptionId() != null ? "subscription #"
                        + payment.getSubscriptionId()
                : payment.getSessionId() != null ? "session #" + payment.getSessionId()
                : "nothing (rule 12 violation)");
    }

    /** A headless-safe snackbar answer (the feedback layer attaches once a scene exists). */
    private void notice(String message) {
        feedback.showSnackbar(message);
    }

    /** The Void target: the activated ledger row, else the most recent voidable row. */
    private String voidTargetRow() {
        if (selectedPaymentRow != null && paymentDataTable.getItems().contains(selectedPaymentRow)
                && !"Voided".equals(rowField(selectedPaymentRow, 4))) {
            return selectedPaymentRow;
        }
        return paymentDataTable.getItems().stream()
                .filter(r -> !"Voided".equals(rowField(r, 4)))
                .findFirst().orElse(null);
    }

    private void selectPaymentRow(String row) {
        selectedPaymentRow = row;
    }

    /** Tender chip selection mapped to the PaymentMethod vocabulary; Card is the default. */
    private PaymentMethod selectedMethod() {
        if (methodGroup != null) {
            Toggle selected = methodGroup.getSelectedToggle();
            if (selected == chipMethodCash) {
                return PaymentMethod.CASH;
            }
            if (selected == chipMethodTransfer) {
                return PaymentMethod.TRANSFER;
            }
        }
        return PaymentMethod.CARD;
    }

    /** Charge radio selection mapped to the rule-12 item type key. */
    private String selectedChargeItem() {
        if (rbSubscription.isSelected()) {
            return "subscription";
        }
        if (rbSession.isSelected()) {
            return "session";
        }
        return "membership";
    }

    /** Tolerant amount parsing ("45.00" -> 45.0; anything else -> -1). */
    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

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
                // Deferred so a screen swap that is mid-mutation of the pane hosting this screen
                // never re-enters a JavaFX children list from inside this listener.
                Platform.runLater(() -> oldRoot.getChildren().remove(feedback));
            }
        });
    }
}
