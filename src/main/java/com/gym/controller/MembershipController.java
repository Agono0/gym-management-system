package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.carousel.M3Carousel;
import io.m3fx.controls.chart.M3FunnelChart;
import io.m3fx.controls.chart.M3FunnelStage;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.glass.GlassEffectParams;
import io.m3fx.controls.glass.GlassShape;
import io.m3fx.controls.glass.M3GlassPane;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.textfield.M3TextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import java.util.List;

/**
 * Controller skeleton for Membership Plans & Subscription Tiers screen (ACT-17-MEMBERSHIP).
 * Declarative UI contract stubs matching Section 7 without backend, domain or service dependencies.
 */
public class MembershipController {

    /**
     * Corner radius of the floating inspector sheet's liquid glass surface — the same 28px the
     * bottom-sheet stylesheet uses, so both floating sheets read as one glass language (Apple
     * chrome is consistently rounded; the 20px sheet padding keeps content clear of the corners).
     */
    private static final double SHEET_GLASS_RADIUS = 28;

    @FXML private M3Carousel tierCarousel;
    @FXML private M3FunnelChart conversionFunnelChart;
    @FXML private M3DataTable<String> membershipDataTable;
    @FXML private M3TextField searchMemberField;
    @FXML private M3Chip chipExpiringFilter;
    @FXML private M3Button btnTopSell;
    @FXML private M3Card cardMonthly;
    @FXML private M3Card cardYearlyVip;
    @FXML private M3Card cardQuarterly;
    @FXML private M3Button btnRenewAction;
    @FXML private M3Button btnFreezeAction;
    @FXML private M3Button btnUnfreezeAction;
    @FXML private M3Button btnCancelAction;
    @FXML private M3SideSheet membershipDetailSheet;

    private final ObservableList<String> masterLedger = FXCollections.observableArrayList();
    private String selectedAgreement;

    public MembershipController() {
        // Public no-arg constructor required by FXMLLoader
    }

    @FXML
    public void initialize() {
        populateMembershipLedger();
        populateFunnelChart();
        applyDetailSheetGlass();
        setupFilterListeners();
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

    /** Fills the subscription ledger with demo rows so the screen is not blank. */
    private void populateMembershipLedger() {
        if (membershipDataTable == null || !membershipDataTable.getColumns().isEmpty()) {
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
        if (conversionFunnelChart == null || !conversionFunnelChart.getStages().isEmpty()) {
            return;
        }
        conversionFunnelChart.getStages().addAll(
                new M3FunnelStage("1. Inquiries / Leads", 1200, 0.0),
                new M3FunnelStage("2. Free Day Passes", 900, 4500.0),
                new M3FunnelStage("3. Monthly Memberships", 600, 35400.0),
                new M3FunnelStage("4. VIP / Yearly Conv.", 300, 149700.0));
    }

    private void setupFilterListeners() {
        if (searchMemberField != null) {
            searchMemberField.textProperty().addListener((obs, oldV, newV) -> applyFilter());
        }
        if (chipExpiringFilter != null) {
            chipExpiringFilter.selectedProperty().addListener((obs, oldV, newV) -> applyFilter());
        }
        if (membershipDataTable != null) {
            membershipDataTable.setOnRowAction(row -> {
                selectedAgreement = row;
                if (membershipDetailSheet != null) {
                    membershipDetailSheet.setVisible(true);
                }
            });
        }
    }

    private void applyFilter() {
        if (membershipDataTable == null) return;
        String query = searchMemberField != null && searchMemberField.getText() != null
                ? searchMemberField.getText().trim().toLowerCase() : "";
        boolean onlyExpiring = chipExpiringFilter != null && chipExpiringFilter.isSelected();

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

    @FXML
    public void onSell() {
        int id = 1050 + (int)(Math.random() * 8000);
        String newRecord = "New Athlete (#" + id + ")|Yearly VIP|Active|2027-10-30";
        masterLedger.add(0, newRecord);
        selectedAgreement = newRecord;
        applyFilter();
    }

    @FXML
    public void onSell(ActionEvent event) {
        onSell();
    }

    @FXML
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

    @FXML
    public void onRenew(ActionEvent event) {
        onRenew();
    }

    @FXML
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

    @FXML
    public void onFreeze(ActionEvent event) {
        onFreeze();
    }

    @FXML
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

    @FXML
    public void onUnfreeze(ActionEvent event) {
        onUnfreeze();
    }

    @FXML
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

    @FXML
    public void onCancel(ActionEvent event) {
        onCancel();
    }

    @FXML
    public void onCloseDetailSheet() {
        if (membershipDetailSheet != null) {
            membershipDetailSheet.setVisible(false);
        }
    }

    @FXML
    public void onCloseDetailSheet(ActionEvent event) {
        onCloseDetailSheet();
    }
}

