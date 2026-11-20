package com.gym.controller;

import com.gym.dao.CustomerDao;
import com.gym.dao.MembershipDao;
import com.gym.dao.PaymentDao;
import com.gym.model.Customer;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.PlanType;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.carousel.M3Carousel;
import io.m3fx.controls.chart.M3BarChart;
import io.m3fx.controls.chart.M3DataPoint;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.common.M3Tooltip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.glass.GlassEffectParams;
import io.m3fx.controls.glass.GlassShape;
import io.m3fx.controls.glass.M3GlassPane;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for Membership Plans & Subscription Tiers screen (ACT-17-MEMBERSHIP).
 *
 * <p>Data contract (DECISIONS.md 77/78, task C6): the tier carousel, agreement funnel, roster
 * and inspector run on real data through one {@link Data#loadAsync} snapshot (MembershipDao +
 * PaymentDao + CustomerDao). The lifecycle actions enforce rule 7 via
 * {@code MembershipDao.update} mirroring the CLI's arithmetic — freeze stores the day counter
 * (ACTIVE-only), unfreeze shifts the end date and clears the counter, renew extends the plan
 * window and a CANCELLED renewal is refused; cancel is destructive and confirms with rule
 * copy. Sell inserts a real agreement for an ACTIVE customer at the plan price. The funnel is
 * an honest agreement history (sold -> paid -> active -> yearly) computed from Membership +
 * Payment rows — no invented conversion numbers. Every DAO {@code ValidationException}
 * surfaces verbatim in a snackbar.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former membership.fxml: tier carousel, agreement funnel, membership roster, inspector sheet.
 */
public class MembershipController implements ScreenController {

    /**
     * Corner radius of the floating inspector sheet's liquid glass surface — the same 28px the
     * bottom-sheet stylesheet uses, so both floating sheets read as one glass language (Apple
     * chrome is consistently rounded; the 20px sheet padding keeps content clear of the corners).
     */
    private static final double SHEET_GLASS_RADIUS = 28;

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private M3Carousel tierCarousel;
    private M3BarChart conversionFunnelChart;
    private M3DataTable<String> membershipDataTable;
    private M3TextField searchMemberField;
    private M3Chip chipExpiringFilter;
    private M3Button btnTopSell;
    private M3Card cardMonthly;
    private M3Card cardYearlyVip;
    private M3Card cardQuarterly;
    private M3Button btnRenewAction;
    private M3Button btnFreezeAction;
    private M3Button btnUnfreezeAction;
    private M3Button btnCancelAction;
    private M3SideSheet membershipDetailSheet;

    private final ObservableList<String> masterLedger = FXCollections.observableArrayList();
    /** Membership id of the row activated in the roster (the lifecycle target), or null. */
    private Integer selectedMembershipId;
    /** Rows of the current snapshot in table order; the id travels in column 5 of each row. */
    private final Map<String, Integer> rowMembershipIds = new HashMap<>();

    /** Content host around the workspace: swaps loading/error states around DAO loads. */
    private StackPane contentHost;
    /** The workspace node, restored after loading/error states. */
    private Node workspaceView;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Labels of the inspector sheet, filled from the selected agreement. */
    private Label lblInspectorCustomer;
    private Label lblInspectorStatus;
    private Label lblInspectorFrozen;
    private Label lblInspectorDates;
    private Label lblInspectorOutstanding;

    /**
     * Builds the complete Membership Plans view and initializes all behavior. Same ordering
     * semantics the FXMLLoader used to provide: the node tree exists and every field is
     * assigned before the former {@code initialize()} work runs.
     */
    public MembershipController() {
        viewRoot = buildView();

        applyDetailSheetGlass();
        wireRosterColumns();
        wireFilterListeners();
        installFeedbackLayer(viewRoot);
        loadAgreements();

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        tierCarousel.setId("tierCarousel");
        conversionFunnelChart.setId("conversionFunnelChart");
        membershipDataTable.setId("membershipDataTable");
        searchMemberField.setId("searchMemberField");
        chipExpiringFilter.setId("chipExpiringFilter");
        btnTopSell.setId("btnTopSell");
        cardMonthly.setId("cardMonthly");
        cardYearlyVip.setId("cardYearlyVip");
        cardQuarterly.setId("cardQuarterly");
        btnRenewAction.setId("btnRenewAction");
        btnFreezeAction.setId("btnFreezeAction");
        btnUnfreezeAction.setId("btnUnfreezeAction");
        btnCancelAction.setId("btnCancelAction");
        membershipDetailSheet.setId("membershipDetailSheet");
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for membership.fxml) and
     * wires every handler.
     */
    private StackPane buildView() {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("membership-base-pane");

        // Top app bar / command strip.
        VBox topBarContainer = new VBox(8);
        topBarContainer.getStyleClass().add("top-bar-container");
        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(16, 24, 16, 24));
        searchMemberField = new M3TextField("Search member agreements...",
                TextFieldVariant.FILLED);
        searchMemberField.setPrefWidth(280);
        btnTopSell = new M3Button("Sell Membership", ButtonVariant.FILLED);
        btnTopSell.getStyleClass().add("m3-button-filled");
        btnTopSell.setOnAction(e -> onSell());
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topRow.getChildren().addAll(
                styledLabel("MEMBERSHIP PLANS & SUBSCRIPTION TIERS", "title-large"),
                topSpacer,
                searchMemberField,
                btnTopSell);
        topBarContainer.getChildren().add(topRow);
        layout.setTop(topBarContainer);

        // Main workspace.
        VBox workspace = new VBox(16);
        workspace.setPadding(new Insets(16, 20, 64, 20));

        // Tier cards carousel (rebuilt from the snapshot in applyTierCards).
        VBox tierSection = new VBox(8);
        tierSection.getChildren().add(styledLabel("MEMBERSHIP TIERS & PACKAGES", "label-small"));
        tierCarousel = new M3Carousel();
        tierCarousel.setPrefHeight(270);
        tierCarousel.getItems().addAll(
                monthlyCard(), yearlyVipCard(), quarterlyCard());
        tierSection.getChildren().add(tierCarousel);

        // Split section: agreement funnel & roster table.
        GridPane splitGrid = new GridPane(16, 16);
        VBox.setVgrow(splitGrid, Priority.ALWAYS);
        splitGrid.setMinHeight(450);
        ColumnConstraints funnelColumn = new ColumnConstraints();
        funnelColumn.setPercentWidth(38);
        funnelColumn.setMinWidth(320);
        ColumnConstraints rosterColumn = new ColumnConstraints();
        rosterColumn.setPercentWidth(62);
        rosterColumn.setMinWidth(480);
        RowConstraints rowConstraint = new RowConstraints();
        rowConstraint.setVgrow(Priority.ALWAYS);
        rowConstraint.setMinHeight(430);
        rowConstraint.setPrefHeight(480);
        splitGrid.getColumnConstraints().addAll(funnelColumn, rosterColumn);
        splitGrid.getRowConstraints().add(rowConstraint);

        // Left column: agreement funnel.
        VBox funnelPane = new VBox(12);
        funnelPane.getStyleClass().add("card-surface");
        funnelPane.setMinHeight(430);
        funnelPane.setPadding(new Insets(16));
        HBox funnelHeader = new HBox(8);
        funnelHeader.setAlignment(Pos.CENTER_LEFT);
        Region funnelSpacer = new Region();
        HBox.setHgrow(funnelSpacer, Priority.ALWAYS);
        Label funnelBadge = new Label("ACTIVE BY PLAN");
        funnelBadge.getStyleClass().add("badge-featured");
        funnelHeader.getChildren().addAll(
                styledLabel("PLAN DISTRIBUTION", "title-medium"),
                funnelSpacer,
                funnelBadge);
        conversionFunnelChart = new M3BarChart();
        VBox.setVgrow(conversionFunnelChart, Priority.ALWAYS);
        conversionFunnelChart.setMinHeight(350);
        conversionFunnelChart.setPrefHeight(390);
        funnelPane.getChildren().addAll(funnelHeader, conversionFunnelChart);

        // Right column: active memberships table & action toolbar.
        VBox rosterPane = new VBox(12);
        rosterPane.getStyleClass().add("card-surface");
        rosterPane.setMinHeight(430);
        rosterPane.setPadding(new Insets(16));
        HBox rosterHeader = new HBox(12);
        rosterHeader.setAlignment(Pos.CENTER_LEFT);
        Region rosterSpacer = new Region();
        HBox.setHgrow(rosterSpacer, Priority.ALWAYS);
        chipExpiringFilter = new M3Chip("Expiring in 7 Days", ChipVariant.ASSIST);
        chipExpiringFilter.setSelected(false);
        rosterHeader.getChildren().addAll(
                styledLabel("MEMBERSHIP ROSTER", "title-medium"),
                rosterSpacer,
                chipExpiringFilter);
        membershipDataTable = new M3DataTable<>();
        VBox.setVgrow(membershipDataTable, Priority.ALWAYS);
        membershipDataTable.setMinHeight(310);
        membershipDataTable.setPrefHeight(350);
        HBox lifecycleBar = new HBox(10);
        lifecycleBar.setAlignment(Pos.CENTER_RIGHT);
        btnRenewAction = new M3Button("Renew Agreement", ButtonVariant.TONAL);
        btnRenewAction.getStyleClass().add("m3-button-tonal");
        btnRenewAction.setOnAction(e -> onRenew());
        btnFreezeAction = new M3Button("Freeze (Pause)", ButtonVariant.OUTLINED);
        btnFreezeAction.getStyleClass().add("m3-button-outlined");
        btnFreezeAction.setOnAction(e -> onFreeze());
        btnUnfreezeAction = new M3Button("Unfreeze (Resume)", ButtonVariant.OUTLINED);
        btnUnfreezeAction.getStyleClass().add("m3-button-outlined");
        btnUnfreezeAction.setOnAction(e -> onUnfreeze());
        btnCancelAction = new M3Button("Cancel Agreement", ButtonVariant.TEXT);
        btnCancelAction.getStyleClass().add("m3-button-text-danger");
        btnCancelAction.setOnAction(e -> onCancel());
        lifecycleBar.getChildren().addAll(btnRenewAction, btnFreezeAction, btnUnfreezeAction, btnCancelAction);
        rosterPane.getChildren().addAll(rosterHeader, membershipDataTable, lifecycleBar);

        splitGrid.add(funnelPane, 0, 0);
        splitGrid.add(rosterPane, 1, 0);
        GridPane.setHgrow(rosterPane, Priority.ALWAYS);

        workspace.getChildren().addAll(tierSection, splitGrid);

        // Contextual inspector side sheet.
        membershipDetailSheet = new M3SideSheet();
        membershipDetailSheet.setVisible(false);
        StackPane.setAlignment(membershipDetailSheet, Pos.CENTER_RIGHT);
        membershipDetailSheet.setPrefWidth(380);
        VBox sheetContent = new VBox(16);
        sheetContent.setPadding(new Insets(20));
        HBox sheetHeader = new HBox();
        sheetHeader.setAlignment(Pos.CENTER_LEFT);
        Region sheetSpacer = new Region();
        HBox.setHgrow(sheetSpacer, Priority.ALWAYS);
        M3IconButton closeBtn = new M3IconButton(M3Icon.symbol("close"), IconButtonVariant.STANDARD);
        closeBtn.setAccessibleText("Close the agreement inspector");
        Tooltip.install(closeBtn, M3Tooltip.plain("Close inspector"));
        closeBtn.setOnAction(e -> onCloseDetailSheet());
        sheetHeader.getChildren().addAll(
                styledLabel("Agreement Inspector", "headline-small"),
                sheetSpacer,
                closeBtn);
        VBox lifecycleSummary = new VBox(8);
        VBox.setVgrow(lifecycleSummary, Priority.ALWAYS);
        lblInspectorCustomer = styledLabel("Customer: -", "body-medium");
        lblInspectorStatus = styledLabel("Agreement Status: -", "body-medium");
        lblInspectorFrozen = styledLabel("Frozen Days Accrued: -", "body-medium");
        lblInspectorDates = styledLabel("Valid: -", "body-medium");
        lblInspectorOutstanding = styledLabel("Outstanding on this plan: -", "body-medium");
        lifecycleSummary.getChildren().addAll(
                styledLabel("LIFECYCLE SUMMARY", "label-small"),
                lblInspectorCustomer,
                lblInspectorStatus,
                lblInspectorFrozen,
                lblInspectorDates,
                lblInspectorOutstanding);
        sheetContent.getChildren().addAll(
                sheetHeader,
                styledLabel("Detailed contract audit trail, freeze logs, and financial association.", "body-small"),
                new Separator(),
                lifecycleSummary);
        membershipDetailSheet.setContent(sheetContent);

        // Content host so the real-data load can swap the workspace for loading/error states.
        workspaceView = workspace;
        contentHost = new StackPane(workspace);
        layout.setCenter(contentHost);

        viewRoot = new StackPane();
        viewRoot.getStyleClass().add("membership-root");
        viewRoot.getChildren().addAll(layout, membershipDetailSheet);
        return viewRoot;
    }

    /** The Monthly tier card (values applied from the snapshot in applyTierCards). */
    private M3Card monthlyCard() {
        cardMonthly = new M3Card(CardVariant.OUTLINED);
        cardMonthly.getStyleClass().add("tier-card-standard");
        cardMonthly.setPrefSize(310, 250);
        cardMonthly.setMaxHeight(Double.MAX_VALUE);
        cardMonthly.setPadding(new Insets(16));
        cardMonthly.setContent(new VBox(10));
        return cardMonthly;
    }

    /** The featured yearly tier card (values applied from the snapshot in applyTierCards). */
    private M3Card yearlyVipCard() {
        cardYearlyVip = new M3Card(CardVariant.ELEVATED);
        cardYearlyVip.getStyleClass().add("tier-card-vip");
        cardYearlyVip.setPrefSize(350, 250);
        cardYearlyVip.setMaxHeight(Double.MAX_VALUE);
        cardYearlyVip.setPadding(new Insets(16));
        cardYearlyVip.setContent(new VBox(10));
        return cardYearlyVip;
    }

    /** The quarterly tier card (values applied from the snapshot in applyTierCards). */
    private M3Card quarterlyCard() {
        cardQuarterly = new M3Card(CardVariant.OUTLINED);
        cardQuarterly.getStyleClass().add("tier-card-standard");
        cardQuarterly.setPrefSize(310, 250);
        cardQuarterly.setMaxHeight(Double.MAX_VALUE);
        cardQuarterly.setPadding(new Insets(16));
        cardQuarterly.setContent(new VBox(10));
        return cardQuarterly;
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
     * Moves the floating agreement-inspector side sheet onto a real liquid glass surface
     * (Apple iOS 26): a REGULAR interactive {@link M3GlassPane} takes the sheet's exact place in
     * the screen root and the sheet becomes its content, so the inspector reads as Apple floating
     * chrome over the ledger. The sheet's opaque surface fill is cleared (Apple: no opaque
     * backgrounds behind glass) and the glass mirrors the sheet's visibility, which keeps the
     * row-click reveal and the close handler working unchanged. The tier cards, funnel chart and
     * ledger table deliberately stay non-glass — glass is reserved for floating chrome, never
     * page content.
     */
    private void applyDetailSheetGlass() {
        wrapSheetInGlass(membershipDetailSheet, "Membership sheet glass skipped: ");
    }

    /**
     * Re-parents the inspector sheet into a REGULAR interactive liquid glass pane that takes
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

    /** Wires the roster columns once; the fifth (off-width) column carries the membership id. */
    private void wireRosterColumns() {
        membershipDataTable.getColumns().addAll(List.of(
                new M3DataTable.Column<>("Member", r -> rowField(r, 0), 190),
                new M3DataTable.Column<>("Plan", r -> rowField(r, 1), 130),
                new M3DataTable.Column<>("Status", r -> rowField(r, 2), 110),
                new M3DataTable.Column<>("Renews", r -> rowField(r, 3), 120)));
    }

    /** One pipe-separated roster field (the hidden 5th field carries the membership id). */
    private static String rowField(String row, int index) {
        String[] parts = row.split("\\|", -1);
        return index < parts.length ? parts[index] : "";
    }

    /**
     * Loads the real agreements on a background task: the bootstrap, Membership/Payment/
     * Customer queries and the snapshot computation run off the FX thread. Shows
     * {@link UiKit#loadingState()} while running and {@link UiKit#errorState()} with retry on
     * failure.
     */
    private void loadAgreements() {
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
                    this::loadAgreements)));
    }

    /**
     * Queries the agreements, payments and customers and computes the funnel, tier stats and
     * roster rows. Runs on the background thread — no JavaFX types touched here.
     */
    private MembershipSnapshot buildSnapshot() throws Exception {
        Data.ensureReady();
        LocalDate today = LocalDate.now();
        List<Membership> memberships = new MembershipDao().findAll();
        List<com.gym.model.Payment> payments = new PaymentDao().findAll();
        List<Customer> customers = new CustomerDao().findAll();
        Map<Integer, String> customerNames = new HashMap<>();
        for (Customer customer : customers) {
            customerNames.put(customer.getId(), customer.getFullName());
        }

        // Per-agreement non-voided payment sums (rule 6/19: voided money never counts).
        Map<Integer, Double> paidByMembership = new HashMap<>();
        double paidRevenue = 0;
        for (com.gym.model.Payment payment : payments) {
            if (payment.isVoided() || payment.getMembershipId() == null) {
                continue;
            }
            paidByMembership.merge(payment.getMembershipId(),
                    payment.getAmount().doubleValue(), Double::sum);
            paidRevenue += payment.getAmount().doubleValue();
        }

        List<AgreementRow> rows = new ArrayList<>();
        Map<PlanType, Integer> soldPlans = new HashMap<>();
        Map<PlanType, Integer> activePlans = new HashMap<>();
        Map<PlanType, List<Double>> pricesByPlan = new HashMap<>();
        int paidCount = 0;
        int activeCount = 0;
        int yearlyActive = 0;
        for (Membership membership : memberships) {
            PlanType plan = membership.getPlan() == null ? PlanType.MONTHLY : membership.getPlan();
            soldPlans.merge(plan, 1, Integer::sum);
            double price = membership.getPrice() == null ? 0 : membership.getPrice().doubleValue();
            pricesByPlan.computeIfAbsent(plan, k -> new ArrayList<>()).add(price);
            double paid = paidByMembership.getOrDefault(membership.getId(), 0.0);
            if (paid > 0) {
                paidCount++;
            }
            boolean active = membership.getStatus() == MembershipStatus.ACTIVE
                    && membership.getEndDate() != null
                    && !LocalDate.parse(membership.getEndDate()).isBefore(today);
            if (active) {
                activeCount++;
                activePlans.merge(plan, 1, Integer::sum);
                if (plan == PlanType.YEARLY) {
                    yearlyActive++;
                }
            }
            rows.add(new AgreementRow(membership.getId(),
                    customerNames.getOrDefault(membership.getCustomerId(), "Customer #"
                            + membership.getCustomerId()) + " (M-"
                            + String.format("%04d", membership.getCustomerId()) + ")",
                    planLabel(plan), statusLabel(membership.getStatus(),
                            membership.getEndDate(), today),
                    membership.getEndDate() == null ? "-" : membership.getEndDate()));
        }

        Map<PlanType, Double> avgPriceByPlan = new HashMap<>();
        for (Map.Entry<PlanType, List<Double>> entry : pricesByPlan.entrySet()) {
            double sum = 0;
            for (double value : entry.getValue()) {
                sum += value;
            }
            avgPriceByPlan.put(entry.getKey(),
                    entry.getValue().isEmpty() ? 0 : sum / entry.getValue().size());
        }
        return new MembershipSnapshot(rows, soldPlans, activePlans, avgPriceByPlan,
                memberships.size(), paidCount, paidRevenue, activeCount, paidRevenue,
                yearlyActive, paidRevenue);
    }

    /** User-facing status copy: the enum plus the derived expiring/expired state. */
    private static String statusLabel(MembershipStatus status, String endDate, LocalDate today) {
        if (status == MembershipStatus.CANCELLED) {
            return "Cancelled";
        }
        if (status == MembershipStatus.FROZEN) {
            return "Frozen";
        }
        if (endDate != null && !endDate.isBlank() && LocalDate.parse(endDate).isBefore(today)) {
            return "Expired";
        }
        if (endDate != null && !endDate.isBlank()
                && !LocalDate.parse(endDate).isAfter(today.plusDays(7))) {
            return "Expiring";
        }
        return "Active";
    }

    private static String planLabel(PlanType plan) {
        return switch (plan == null ? PlanType.MONTHLY : plan) {
            case MONTHLY -> "Monthly";
            case QUARTERLY -> "Quarterly";
            case YEARLY -> "Yearly";
        };
    }

    /** Plan list prices (the tier-card anchors, used when selling a new agreement). */
    private static java.math.BigDecimal planPrice(PlanType plan) {
        return switch (plan == null ? PlanType.MONTHLY : plan) {
            case MONTHLY -> new java.math.BigDecimal("59.00");
            case QUARTERLY -> new java.math.BigDecimal("149.00");
            case YEARLY -> new java.math.BigDecimal("499.00");
        };
    }

    /** Plan window length in days (mirrors the CLI's rule-7 arithmetic). */
    private static int planDays(PlanType plan) {
        return switch (plan == null ? PlanType.MONTHLY : plan) {
            case MONTHLY -> 30;
            case QUARTERLY -> 90;
            case YEARLY -> 365;
        };
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /** The funnel/tier snapshot values, computed off the FX thread. */
    private record AgreementRow(int membershipId, String memberLabel, String planLabel,
                                String statusLabel, String renewsLabel) {
    }

    private record MembershipSnapshot(List<AgreementRow> rows,
                                      Map<PlanType, Integer> soldPlans,
                                      Map<PlanType, Integer> activePlans,
                                      Map<PlanType, Double> avgPriceByPlan,
                                      int totalSold, int paidCount, double paidRevenue,
                                      int activeCount, double activeRevenue,
                                      int yearlyActive, double yearlyRevenue) {
    }

    /** Applies a finished snapshot: tier cards, funnel, roster rows and filters. */
    private void applySnapshot(MembershipSnapshot snapshot) {
        applyTierCards(snapshot);
        populateFunnelChart(snapshot);
        rebuildLedgerRows(snapshot);
        membershipDataTable.getItems().setAll(masterLedger);
        applyFilter();
    }

    /**
     * Tier cards from PlanType reality: active-count and average sold price per plan from the
     * loaded agreements (no invented marketing prices).
     */
    private void applyTierCards(MembershipSnapshot snapshot) {
        cardMonthly.setContent(tierCardContent("MONTHLY STANDARD", PlanType.MONTHLY, snapshot));
        cardQuarterly.setContent(tierCardContent("QUARTERLY PRO", PlanType.QUARTERLY, snapshot));
        cardYearlyVip.setContent(tierCardContent("YEARLY COMMITMENT", PlanType.YEARLY, snapshot));
    }

    /** One tier card body: active count and the plan's average sold price, real. */
    private VBox tierCardContent(String title, PlanType plan, MembershipSnapshot snapshot) {
        int active = snapshot.activePlans().getOrDefault(plan, 0);
        int sold = snapshot.soldPlans().getOrDefault(plan, 0);
        double avgPrice = snapshot.avgPriceByPlan().getOrDefault(plan, 0.0);
        Label headline = styledLabel(sold == 0 ? "No agreements sold yet"
                : String.format(Locale.ROOT, "$%,.2f average sold price", avgPrice),
                "headline-medium");
        Region bodySpacer = new Region();
        VBox.setVgrow(bodySpacer, Priority.ALWAYS);
        M3Button sell = new M3Button("Sell " + planLabel(plan), ButtonVariant.OUTLINED);
        sell.getStyleClass().add("m3-button-outlined");
        sell.setMaxWidth(Double.MAX_VALUE);
        sell.setOnAction(e -> onSell(plan));
        return new VBox(10,
                styledLabel(title, "title-medium"),
                headline,
                styledLabel(active + " active now · " + sold + " sold in total", "body-small"),
                bodySpacer,
                sell);
    }

    /**
     * Honest agreement-history funnel computed from Membership+Payment rows (replaces the
     * invented conversion telemetry): sold -> with a recorded payment -> active today ->
     * yearly commitments. Each stage is a subset of the previous one.
     */
    /**
     * Honest plan-distribution chart from MembershipDao rows (replaces the invented conversion
     * funnel): active agreements per PlanType. A funnel was rejected deliberately - paid and
     * active agreements are overlapping sets, not nested stages, so a funnel shape would
     * misrepresent them.
     */
    private void populateFunnelChart(MembershipSnapshot snapshot) {
        M3DataSeries plans = new M3DataSeries("Active agreements");
        plans.getData().addAll(List.of(
                new M3DataPoint("Monthly",
                        snapshot.activePlans().getOrDefault(PlanType.MONTHLY, 0)),
                new M3DataPoint("Quarterly",
                        snapshot.activePlans().getOrDefault(PlanType.QUARTERLY, 0)),
                new M3DataPoint("Yearly",
                        snapshot.activePlans().getOrDefault(PlanType.YEARLY, 0))));
        conversionFunnelChart.getSeriesList().setAll(plans);
    }

    /** Rebuilds the ledger rows from the snapshot (member label, plan, status, renews, id). */
    private void rebuildLedgerRows(MembershipSnapshot snapshot) {
        rowMembershipIds.clear();
        masterLedger.clear();
        for (AgreementRow row : snapshot.rows()) {
            String tableRow = row.memberLabel() + "|" + row.planLabel() + "|"
                    + row.statusLabel() + "|" + row.renewsLabel() + "|"
                    + row.membershipId();
            rowMembershipIds.put(tableRow, row.membershipId());
            masterLedger.add(tableRow);
        }
    }

    private void wireFilterListeners() {
        searchMemberField.textProperty().addListener((obs, oldV, newV) -> applyFilter());
        chipExpiringFilter.selectedProperty().addListener((obs, oldV, newV) -> applyFilter());
        membershipDataTable.setOnRowAction(row -> {
            selectedMembershipId = rowMembershipIds.get(row);
            applyDetailSheet();
            membershipDetailSheet.setVisible(true);
        });
    }

    private void applyFilter() {
        String query = searchMemberField.getText() != null
                ? searchMemberField.getText().trim().toLowerCase(Locale.ROOT) : "";
        boolean onlyExpiring = chipExpiringFilter.isSelected();

        List<String> filtered = new ArrayList<>();
        for (String row : masterLedger) {
            String status = rowField(row, 2);
            if (onlyExpiring && !"Expiring".equals(status) && !"Expired".equals(status)) {
                continue;
            }
            if (!query.isEmpty() && !row.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            filtered.add(row);
        }
        membershipDataTable.getItems().setAll(filtered);
    }

    /** Refreshes the inspector sheet from the selected agreement's real data. */
    private void applyDetailSheet() {
        Integer id = selectedMembershipId;
        if (id == null) {
            lblInspectorCustomer.setText("Customer: -");
            lblInspectorStatus.setText("Agreement Status: -");
            lblInspectorFrozen.setText("Frozen Days Accrued: -");
            lblInspectorDates.setText("Valid: -");
            lblInspectorOutstanding.setText("Outstanding on this plan: -");
            return;
        }
        Data.submit(
                () -> {
                    Data.ensureReady();
                    Membership membership = new MembershipDao().findById(id);
                    double outstanding = 0;
                    if (membership != null && membership.getPrice() != null) {
                        double paid = 0;
                        for (com.gym.model.Payment payment : new PaymentDao().findAll()) {
                            if (!payment.isVoided()
                                    && membership.getId() == payment.getMembershipId()) {
                                paid += payment.getAmount().doubleValue();
                            }
                        }
                        outstanding = Math.max(0,
                                membership.getPrice().doubleValue() - paid);
                    }
                    String customerName = null;
                    if (membership != null) {
                        Customer customer = new CustomerDao().findById(membership.getCustomerId());
                        customerName = customer == null ? null : customer.getFullName();
                    }
                    return new Object[] {membership, outstanding, customerName};
                },
                read -> {
                    Membership membership = (Membership) read[0];
                    double outstanding = (Double) read[1];
                    String customerName = (String) read[2];
                    if (membership == null) {
                        lblInspectorCustomer.setText("Customer: agreement no longer exists");
                        lblInspectorStatus.setText("Agreement Status: -");
                        lblInspectorFrozen.setText("Frozen Days Accrued: -");
                        lblInspectorDates.setText("Valid: -");
                        lblInspectorOutstanding.setText("Outstanding on this plan: -");
                        return;
                    }
                    lblInspectorCustomer.setText("Customer: " + (customerName == null
                            ? "Customer #" + membership.getCustomerId() : customerName));
                    lblInspectorStatus.setText("Agreement Status: "
                            + membership.getStatus() + " · " + planLabel(membership.getPlan()));
                    lblInspectorFrozen.setText("Frozen Days Accrued: "
                            + membership.getFrozenDays());
                    lblInspectorDates.setText("Valid: " + orEmpty(membership.getStartDate())
                            + " -> " + orEmpty(membership.getEndDate()));
                    lblInspectorOutstanding.setText(String.format(Locale.ROOT,
                            "Outstanding on this plan: $%,.2f (rule 6: price - non-voided payments)",
                            outstanding));
                },
                error -> lblInspectorCustomer.setText("Could not load the agreement: "
                        + error.getMessage()));
    }

    /**
     * Rule-7 lifecycle target resolution: re-reads the agreement through the DAO so the
     * arithmetic below works on the database state, not a stale copy.
     */
    private Membership currentMembership() throws Exception {
        Integer id = selectedMembershipId;
        if (id == null) {
            throw new com.gym.exception.ValidationException(
                    "Select an agreement row on the roster first.");
        }
        Membership membership = new MembershipDao().findById(id);
        if (membership == null) {
            throw new com.gym.exception.ValidationException(
                    "Agreement no longer exists in the database");
        }
        return membership;
    }

    /**
     * Rule 7 renew through {@code MembershipDao.update}: extends the end date by the plan
     * window from today (an expired plan reactivates) and refuses a CANCELLED agreement with
     * the rule copy.
     */
    public void onRenew() {
        try {
            Membership target = currentMembership();
            M3Button cancel = M3Button.text("Not Now");
            M3Button renew = M3Button.text("Renew " + planLabel(target.getPlan()));
            M3Dialog dialog = M3Dialog.basic("Renew for another "
                            + planLabel(target.getPlan()).toLowerCase(Locale.ROOT) + " cycle?",
                    new Label("The validity window extends by one plan cycle from today. "
                            + "Renewing a cancelled agreement is refused (rule 7)."),
                    cancel, renew);
            dialog.setDefaultAction(renew::activate);
            cancel.setOnAction(e -> dialog.hide());
            renew.setOnAction(e -> {
                dialog.hide();
                int id = target.getId();
                PlanType plan = target.getPlan();
                Data.submit(
                        () -> {
                            Membership current = new MembershipDao().findById(id);
                            if (current == null) {
                                throw new com.gym.exception.ValidationException(
                                        "Agreement no longer exists in the database");
                            }
                            if (current.getStatus() == MembershipStatus.CANCELLED) {
                                throw new com.gym.exception.ValidationException(
                                        "Rule 7: Renewing a CANCELLED membership is refused");
                            }
                            LocalDate baseDate = LocalDate.now();
                            if (current.getEndDate() != null
                                    && !current.getEndDate().isBlank()
                                    && !LocalDate.parse(current.getEndDate()).isBefore(baseDate)) {
                                baseDate = LocalDate.parse(current.getEndDate());
                            }
                            current.setEndDate(baseDate.plusDays(planDays(plan)).toString());
                            current.setStatus(MembershipStatus.ACTIVE);
                            new MembershipDao().update(current);
                            return current;
                        },
                        renewed -> {
                            feedback.showSnackbar("Agreement renewed through "
                                    + renewed.getEndDate() + ".");
                            loadAgreements();
                        },
                        error -> feedback.showSnackbar("Could not renew: " + error.getMessage()));
            });
            feedback.showDialog(dialog);
        } catch (Exception e) {
            // Bootstrap or lookup failures answer the same way as rule refusals: a snackbar
            // with the DAO's own message.
            feedback.showSnackbar(e.getMessage());
        }
    }

    /**
     * Rule 7 freeze through {@code MembershipDao.update}: only an ACTIVE agreement freezes and
     * the frozen-day counter is stored. Destructive, so it confirms with a day-count field.
     */
    public void onFreeze() {
        try {
            Membership target = currentMembership();
            M3TextField daysField = new M3TextField("Days to freeze", TextFieldVariant.OUTLINED);
            daysField.setSupportingText("1 - 365, e.g. 14");
            M3Button cancel = M3Button.text("Keep Active");
            M3Button freeze = M3Button.text("Freeze Plan");
            M3Dialog dialog = M3Dialog.basic("Freeze this agreement?",
                    new VBox(10,
                            new Label("Billing and bookings pause for the freeze window and the"
                                    + " day counter is stored (rule 7)."),
                            daysField),
                    cancel, freeze);
            dialog.setDefaultAction(freeze::activate);
            cancel.setOnAction(e -> dialog.hide());
            freeze.setOnAction(e -> {
                Double daysValue = parseDays(daysField);
                if (daysValue == null) {
                    return;
                }
                dialog.hide();
                int id = target.getId();
                int days = daysValue.intValue();
                Data.submit(
                        () -> {
                            Membership current = new MembershipDao().findById(id);
                            if (current == null) {
                                throw new com.gym.exception.ValidationException(
                                        "Agreement no longer exists in the database");
                            }
                            if (current.getStatus() != MembershipStatus.ACTIVE) {
                                throw new com.gym.exception.ValidationException(
                                        "Rule 7: Only ACTIVE memberships can be frozen (current: "
                                                + current.getStatus() + ")");
                            }
                            current.setStatus(MembershipStatus.FROZEN);
                            current.setFrozenDays(days);
                            new MembershipDao().update(current);
                            return current;
                        },
                        frozen -> {
                            feedback.showSnackbar("Agreement frozen for " + days
                                    + " days — billing and bookings paused.");
                            loadAgreements();
                        },
                        error -> feedback.showSnackbar("Could not freeze: " + error.getMessage()));
            });
            feedback.showDialog(dialog);
        } catch (Exception e) {
            // Bootstrap or lookup failures answer the same way as rule refusals: a snackbar
            // with the DAO's own message.
            feedback.showSnackbar(e.getMessage());
        }
    }

    /**
     * Rule 7 unfreeze through {@code MembershipDao.update}: the end date shifts forward by the
     * stored frozen days and the counter clears. Non-destructive, so no confirm.
     */
    public void onUnfreeze() {
        try {
            Membership target = currentMembership();
            int id = target.getId();
            Data.submit(
                    () -> {
                        Membership current = new MembershipDao().findById(id);
                        if (current == null) {
                            throw new com.gym.exception.ValidationException(
                                    "Agreement no longer exists in the database");
                        }
                        if (current.getStatus() != MembershipStatus.FROZEN) {
                            throw new com.gym.exception.ValidationException(
                                    "Rule 7: Only FROZEN memberships can be unfrozen");
                        }
                        LocalDate newEnd = LocalDate.parse(current.getEndDate())
                                .plusDays(current.getFrozenDays());
                        current.setEndDate(newEnd.toString());
                        current.setFrozenDays(0);
                        current.setStatus(MembershipStatus.ACTIVE);
                        new MembershipDao().update(current);
                        return current;
                    },
                    unfrozen -> {
                        feedback.showSnackbar("Agreement reactivated — valid through "
                                + unfrozen.getEndDate() + ".");
                        loadAgreements();
                    },
                    error -> feedback.showSnackbar("Could not unfreeze: " + error.getMessage()));
        } catch (Exception e) {
            // Bootstrap or lookup failures answer the same way as rule refusals: a snackbar
            // with the DAO's own message.
            feedback.showSnackbar(e.getMessage());
        }
    }

    /**
     * Rule 7 cancel: destructive, so it confirms with rule copy first, then sets the agreement
     * CANCELLED through {@code MembershipDao.update} (a later renewal is refused by the DAO).
     */
    public void onCancel() {
        try {
            Membership target = currentMembership();
            M3Button keep = M3Button.text("Keep agreement");
            M3Button confirm = M3Button.text("Cancel Agreement");
            M3Dialog dialog = M3Dialog.basic("Cancel this agreement?",
                    new Label("Agreement #" + target.getId() + " is marked CANCELLED. Rule 7:"
                            + " a cancelled agreement cannot be renewed or reactivated"
                            + " afterwards."),
                    keep, confirm);
            dialog.setDefaultAction(confirm::activate);
            keep.setOnAction(e -> dialog.hide());
            confirm.setOnAction(e -> {
                dialog.hide();
                int id = target.getId();
                Data.submit(
                        () -> {
                            Membership current = new MembershipDao().findById(id);
                            if (current == null) {
                                throw new com.gym.exception.ValidationException(
                                        "Agreement no longer exists in the database");
                            }
                            current.setStatus(MembershipStatus.CANCELLED);
                            new MembershipDao().update(current);
                            return current;
                        },
                        cancelled -> {
                            feedback.showSnackbar("Agreement cancelled — rule 7 blocks any later"
                                    + " renewal.");
                            loadAgreements();
                        },
                        error -> feedback.showSnackbar("Could not cancel: " + error.getMessage()));
            });
            feedback.showDialog(dialog);
        } catch (Exception e) {
            // Bootstrap or lookup failures answer the same way as rule refusals: a snackbar
            // with the DAO's own message.
            feedback.showSnackbar(e.getMessage());
        }
    }

    /**
     * Sell: inserts a real agreement for an ACTIVE customer (picked from a menu) starting
     * today at the plan price, through {@code MembershipDao.insert}.
     */
    public void onSell() {
        openCustomerPickerMenu(null);
    }

    /** Sells the given plan to an ACTIVE customer picked from a menu. */
    public void onSell(PlanType plan) {
        openCustomerPickerMenu(plan);
    }

    /** One menu entry per ACTIVE customer; continues to the plan picker when no plan is fixed. */
    private void openCustomerPickerMenu(PlanType fixedPlan) {
        Data.submit(
                () -> {
                    Data.ensureReady();
                    List<Customer> actives = new ArrayList<>();
                    for (Customer customer : new CustomerDao().findAll()) {
                        if (customer.getStatus() == com.gym.model.CustomerStatus.ACTIVE) {
                            actives.add(customer);
                        }
                    }
                    return actives;
                },
                actives -> {
                    M3Menu customerMenu = new M3Menu();
                    if (actives.isEmpty()) {
                        customerMenu.item("No ACTIVE customers to sell to", () -> { });
                    }
                    for (Customer customer : actives) {
                        if (fixedPlan != null) {
                            customerMenu.item(customer.getFullName() + " · " + planLabel(fixedPlan),
                                    () -> sellAgreement(customer.getId(), fixedPlan));
                        } else {
                            for (PlanType plan : PlanType.values()) {
                                customerMenu.item(customer.getFullName() + " · " + planLabel(plan),
                                        () -> sellAgreement(customer.getId(), plan));
                            }
                        }
                    }
                    showMenuBelow(btnTopSell, customerMenu, "Selling opens inside the app window.");
                },
                error -> feedback.showSnackbar("Could not load customers: " + error.getMessage()));
    }

    /** Inserts the agreement at the plan price, starting today (rule 7 window). */
    private void sellAgreement(int customerId, PlanType plan) {
        Data.submit(
                () -> {
                    Membership membership = new Membership(0, customerId, plan,
                            planPrice(plan), LocalDate.now().toString(),
                            LocalDate.now().plusDays(planDays(plan)).toString(),
                            MembershipStatus.ACTIVE, 0);
                    return new MembershipDao().insert(membership);
                },
                sold -> {
                    feedback.showSnackbar("Agreement sold — " + planLabel(sold.getPlan())
                            + " for member #" + sold.getCustomerId() + ", valid through "
                            + sold.getEndDate() + ".");
                    loadAgreements();
                },
                error -> feedback.showSnackbar("Could not sell: " + error.getMessage()));
    }

    private Double parseDays(M3TextField field) {
        try {
            double value = Double.parseDouble(field.getText() == null ? "" : field.getText().trim());
            if (value >= 1 && value <= 365) {
                field.setError(false);
                return value;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error state
        }
        field.setError(true);
        feedback.showSnackbar("Enter a freeze window between 1 and 365 days.");
        return null;
    }

    public void onCloseDetailSheet() {
        membershipDetailSheet.setVisible(false);
    }

    /** Shows an anchored menu; without a window (headless preview) it answers via snackbar. */
    private void showMenuBelow(Node anchor, M3Menu menu, String fallback) {
        if (anchor.getScene() == null || anchor.getScene().getWindow() == null) {
            feedback.showSnackbar(fallback);
            return;
        }
        menu.showBelow(anchor);
    }

    /**
     * Hosts the shared feedback overlay on the scene root while this screen is visible.
     * Attach/detach are deferred with {@link Platform#runLater(Runnable) Platform.runLater} so
     * the scene root children list is never mutated re-entrantly during a screen swap.
     */
    private void installFeedbackLayer(Node anchor) {
        anchor.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null && scene.getRoot() instanceof Pane root
                    && !root.getChildren().contains(feedback)) {
                root.getChildren().add(feedback);
            }
            if (oldScene != null && oldScene.getRoot() instanceof Pane oldRoot) {
                Platform.runLater(() -> oldRoot.getChildren().remove(feedback));
            }
        });
    }
}
