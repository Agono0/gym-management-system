package com.gym.controller;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.carousel.M3Carousel;
import io.m3fx.controls.chart.M3FunnelChart;
import io.m3fx.controls.chart.M3FunnelStage;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.glass.GlassEffectParams;
import io.m3fx.controls.glass.GlassShape;
import io.m3fx.controls.glass.M3GlassPane;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.textfield.M3TextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.ColumnConstraints;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Controller skeleton for Membership Plans & Subscription Tiers screen (ACT-17-MEMBERSHIP).
 * Declarative UI contract stubs matching Section 7 without backend, domain or service dependencies.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former membership.fxml: tier carousel, conversion funnel, membership roster, inspector sheet.
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
    private M3FunnelChart conversionFunnelChart;
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
    private String selectedAgreement;

    /**
     * Builds the complete Membership Plans view and initializes all behavior. Same ordering
     * semantics the FXMLLoader used to provide: the node tree exists and every field is
     * assigned before the former {@code initialize()} work runs.
     */
    public MembershipController() {
        viewRoot = buildView();

        populateMembershipLedger();
        populateFunnelChart();
        applyDetailSheetGlass();

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
        setupFilterListeners();
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
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        searchMemberField.setPrefWidth(280);
        btnTopSell = new M3Button("+ Sell Membership", ButtonVariant.FILLED);
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

        // Tier cards carousel.
        VBox tierSection = new VBox(8);
        tierSection.getChildren().add(styledLabel("MEMBERSHIP TIERS & PACKAGES", "label-small"));
        tierCarousel = new M3Carousel();
        tierCarousel.setPrefHeight(270);
        tierCarousel.getItems().addAll(
                monthlyCard(), yearlyVipCard(), quarterlyCard());
        tierSection.getChildren().add(tierCarousel);

        // Split section: conversion funnel & fleet data table.
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

        // Left column: conversion funnel.
        VBox funnelPane = new VBox(12);
        funnelPane.getStyleClass().add("card-surface");
        funnelPane.setMinHeight(430);
        funnelPane.setPadding(new Insets(16));
        HBox funnelHeader = new HBox(8);
        funnelHeader.setAlignment(Pos.CENTER_LEFT);
        Region funnelSpacer = new Region();
        HBox.setHgrow(funnelSpacer, Priority.ALWAYS);
        Label funnelBadge = new Label("4-STAGE TELEMETRY");
        funnelBadge.getStyleClass().add("badge-featured");
        funnelHeader.getChildren().addAll(
                styledLabel("MEMBER CONVERSION FUNNEL", "title-medium"),
                funnelSpacer,
                funnelBadge);
        conversionFunnelChart = new M3FunnelChart();
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
        layout.setCenter(workspace);

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
        M3Button closeBtn = new M3Button("✕", ButtonVariant.TEXT);
        closeBtn.setOnAction(e -> onCloseDetailSheet());
        sheetHeader.getChildren().addAll(
                styledLabel("Agreement Inspector", "headline-small"),
                sheetSpacer,
                closeBtn);
        VBox lifecycleSummary = new VBox(8);
        VBox.setVgrow(lifecycleSummary, Priority.ALWAYS);
        lifecycleSummary.getChildren().addAll(
                styledLabel("LIFECYCLE SUMMARY", "label-small"),
                styledLabel("Customer: Selected Athlete", "body-medium"),
                styledLabel("Agreement Status: ACTIVE", "body-medium"),
                styledLabel("Frozen Days Accrued: 0", "body-medium"));
        sheetContent.getChildren().addAll(
                sheetHeader,
                styledLabel("Detailed contract audit trail, freeze logs, and financial association.", "body-small"),
                new Separator(),
                lifecycleSummary);
        membershipDetailSheet.setContent(sheetContent);

        viewRoot = new StackPane();
        viewRoot.getStyleClass().add("membership-root");
        viewRoot.getChildren().addAll(layout, membershipDetailSheet);
        return viewRoot;
    }

    /** The Monthly Standard tier card. */
    private M3Card monthlyCard() {
        cardMonthly = new M3Card(CardVariant.OUTLINED);
        cardMonthly.getStyleClass().add("tier-card-standard");
        cardMonthly.setPrefSize(310, 250);
        cardMonthly.setMaxHeight(Double.MAX_VALUE);
        cardMonthly.setPadding(new Insets(16));
        VBox content = new VBox(10);
        content.getChildren().addAll(
                styledLabel("MONTHLY STANDARD", "title-medium"),
                styledLabel("$59.00 / Month", "headline-medium"),
                multiLine("• Access 06:00 - 23:00\n• Full Gym Floor & Lockers\n• 1 Guest Pass / Mo", "body-small"));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        content.getChildren().add(spacer);
        M3Button select = new M3Button("Select Monthly", ButtonVariant.OUTLINED);
        select.getStyleClass().add("m3-button-outlined");
        select.setMaxWidth(Double.MAX_VALUE);
        select.setOnAction(e -> onSell());
        content.getChildren().add(select);
        cardMonthly.setContent(content);
        return cardMonthly;
    }

    /** The featured Yearly Cyber VIP tier card. */
    private M3Card yearlyVipCard() {
        cardYearlyVip = new M3Card(CardVariant.ELEVATED);
        cardYearlyVip.getStyleClass().add("tier-card-vip");
        cardYearlyVip.setPrefSize(350, 250);
        cardYearlyVip.setMaxHeight(Double.MAX_VALUE);
        cardYearlyVip.setPadding(new Insets(16));
        VBox content = new VBox(10);
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label featured = new Label("BEST VALUE");
        featured.getStyleClass().add("badge-featured");
        header.getChildren().addAll(
                styledLabel("YEARLY CYBER VIP", "title-large"),
                spacer,
                featured);
        content.getChildren().addAll(
                header,
                styledLabel("$499.00 / Year", "headline-large"),
                multiLine("• 24/7 Unlimited Turnstile Access\n• 2 Personal Training Sessions / Mo"
                        + "\n• Sauna & Recovery Lounge Pass\n• 60 Days Freeze Privilege", "body-small"));
        Region bodySpacer = new Region();
        VBox.setVgrow(bodySpacer, Priority.ALWAYS);
        content.getChildren().add(bodySpacer);
        M3Button sellVip = new M3Button("Sell VIP Agreement", ButtonVariant.FILLED);
        sellVip.getStyleClass().add("m3-button-filled");
        sellVip.setMaxWidth(Double.MAX_VALUE);
        sellVip.setOnAction(e -> onSell());
        content.getChildren().add(sellVip);
        cardYearlyVip.setContent(content);
        return cardYearlyVip;
    }

    /** The Quarterly Pro tier card. */
    private M3Card quarterlyCard() {
        cardQuarterly = new M3Card(CardVariant.OUTLINED);
        cardQuarterly.getStyleClass().add("tier-card-standard");
        cardQuarterly.setPrefSize(310, 250);
        cardQuarterly.setMaxHeight(Double.MAX_VALUE);
        cardQuarterly.setPadding(new Insets(16));
        VBox content = new VBox(10);
        content.getChildren().addAll(
                styledLabel("QUARTERLY PRO", "title-medium"),
                styledLabel("$149.00 / Quarter", "headline-medium"),
                multiLine("• Full Gym Floor + Pool Access\n• 1 PT Diagnostic Session / Qtr"
                        + "\n• 14 Days Freeze Privilege", "body-small"));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        content.getChildren().add(spacer);
        M3Button select = new M3Button("Select Quarterly", ButtonVariant.OUTLINED);
        select.getStyleClass().add("m3-button-outlined");
        select.setMaxWidth(Double.MAX_VALUE);
        select.setOnAction(e -> onSell());
        content.getChildren().add(select);
        cardQuarterly.setContent(content);
        return cardQuarterly;
    }

    /** Small label with one style class. */
    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    /** Multi-line body label (the tier bullet lists). */
    private static Label multiLine(String text, String styleClass) {
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

    /** Fills the subscription ledger with demo rows so the screen is not blank. */
    private void populateMembershipLedger() {
        if (!membershipDataTable.getColumns().isEmpty()) {
            return;
        }
        membershipDataTable.getColumns().addAll(List.of(
                new M3DataTable.Column<>("Member", r -> r.split("\\|")[0], 190),
                new M3DataTable.Column<>("Plan", r -> r.split("\\|")[1], 130),
                new M3DataTable.Column<>("Status", r -> r.split("\\|")[2], 110),
                new M3DataTable.Column<>("Renews", r -> r.split("\\|")[3], 120)));

        masterLedger.setAll(
                "Sarah Connor (#1042)|Yearly VIP|Active|2027-03-14",
                "Marcus Vance (#101)|Staff|Active|—",
                "Priya Nair (#2075)|Monthly|Expiring|2026-11-02",
                "Tom Becker (#1533)|10-Pack|Active|6 left",
                "Lena Fischer (#3110)|Yearly|Active|2027-05-30",
                "Diego Ramos (#2871)|Trainer|Active|—",
                "Aisha Khan (#1902)|Monthly|Frozen|2026-12-18",
                "Jonas Weber (#1254)|Day Pass|Expired|2026-10-27");
        membershipDataTable.getItems().setAll(masterLedger);
    }

    /** Fills the acquisition conversion funnel chart with telemetry stages. */
    private void populateFunnelChart() {
        if (!conversionFunnelChart.getStages().isEmpty()) {
            return;
        }
        conversionFunnelChart.getStages().addAll(
                new M3FunnelStage("1. Inquiries / Leads", 1200, 0.0),
                new M3FunnelStage("2. Free Day Passes", 900, 4500.0),
                new M3FunnelStage("3. Monthly Memberships", 600, 35400.0),
                new M3FunnelStage("4. VIP / Yearly Conv.", 300, 149700.0));
    }

    private void setupFilterListeners() {
        searchMemberField.textProperty().addListener((obs, oldV, newV) -> applyFilter());
        chipExpiringFilter.selectedProperty().addListener((obs, oldV, newV) -> applyFilter());
        membershipDataTable.setOnRowAction(row -> {
            selectedAgreement = row;
            membershipDetailSheet.setVisible(true);
        });
    }

    private void applyFilter() {
        String query = searchMemberField.getText() != null
                ? searchMemberField.getText().trim().toLowerCase() : "";
        boolean onlyExpiring = chipExpiringFilter.isSelected();

        List<String> filtered = masterLedger.stream()
                .filter(row -> {
                    if (onlyExpiring && !row.toLowerCase().contains("expiring")) {
                        return false;
                    }
                    if (!query.isEmpty() && !row.toLowerCase().contains(query)) {
                        return false;
                    }
                    return true;
                })
                .toList();
        membershipDataTable.getItems().setAll(filtered);
    }

    private int getSelectedIndex() {
        if (selectedAgreement != null) {
            int idx = masterLedger.indexOf(selectedAgreement);
            if (idx >= 0) return idx;
        }
        return masterLedger.isEmpty() ? -1 : 0;
    }

    public void onSell() {
        int id = 1050 + (int)(Math.random() * 8000);
        String newRecord = "New Athlete (#" + id + ")|Yearly VIP|Active|2027-10-30";
        masterLedger.add(0, newRecord);
        selectedAgreement = newRecord;
        applyFilter();
    }

    public void onRenew() {
        int idx = getSelectedIndex();
        if (idx >= 0 && idx < masterLedger.size()) {
            String row = masterLedger.get(idx);
            String[] parts = row.split("\\|");
            if (parts.length >= 4) {
                masterLedger.set(idx, parts[0] + "|" + parts[1] + "|Active|2027-12-31");
                selectedAgreement = masterLedger.get(idx);
                applyFilter();
            }
        }
    }

    public void onFreeze() {
        int idx = getSelectedIndex();
        if (idx >= 0 && idx < masterLedger.size()) {
            String row = masterLedger.get(idx);
            String[] parts = row.split("\\|");
            if (parts.length >= 4) {
                masterLedger.set(idx, parts[0] + "|" + parts[1] + "|Frozen|" + parts[3]);
                selectedAgreement = masterLedger.get(idx);
                applyFilter();
            }
        }
    }

    public void onUnfreeze() {
        int idx = getSelectedIndex();
        if (idx >= 0 && idx < masterLedger.size()) {
            String row = masterLedger.get(idx);
            String[] parts = row.split("\\|");
            if (parts.length >= 4) {
                masterLedger.set(idx, parts[0] + "|" + parts[1] + "|Active|" + parts[3]);
                selectedAgreement = masterLedger.get(idx);
                applyFilter();
            }
        }
    }

    public void onCancel() {
        int idx = getSelectedIndex();
        if (idx >= 0 && idx < masterLedger.size()) {
            String row = masterLedger.get(idx);
            String[] parts = row.split("\\|");
            if (parts.length >= 4) {
                masterLedger.set(idx, parts[0] + "|" + parts[1] + "|Cancelled|" + parts[3]);
                selectedAgreement = masterLedger.get(idx);
                applyFilter();
            }
        }
    }

    public void onCloseDetailSheet() {
        membershipDetailSheet.setVisible(false);
    }
}
