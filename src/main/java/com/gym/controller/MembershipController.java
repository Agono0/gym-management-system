package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.carousel.M3Carousel;
import io.m3fx.controls.chart.M3FunnelChart;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

/**
 * Controller skeleton for Membership Plans & Subscription Tiers screen (ACT-17-MEMBERSHIP).
 * Declarative UI contract stubs matching Section 7 without backend, domain or service dependencies.
 */
public class MembershipController {

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

    public MembershipController() {
        // Public no-arg constructor required by FXMLLoader
    }

    @FXML
    public void initialize() {
        populateMembershipLedger();
    }

    /** Fills the subscription ledger with demo rows so the screen is not blank. */
    private void populateMembershipLedger() {
        if (membershipDataTable == null || !membershipDataTable.getColumns().isEmpty()) {
            return;
        }
        membershipDataTable.getColumns().addAll(
                new M3DataTable.Column<>("Member", r -> r.split("\\|")[0], 190),
                new M3DataTable.Column<>("Plan", r -> r.split("\\|")[1], 130),
                new M3DataTable.Column<>("Status", r -> r.split("\\|")[2], 110),
                new M3DataTable.Column<>("Renews", r -> r.split("\\|")[3], 120));
        membershipDataTable.getItems().addAll(
                "Sarah Connor (#1042)|Yearly VIP|Active|2027-03-14",
                "Marcus Vance (#101)|Staff|Active|—",
                "Priya Nair (#2075)|Monthly|Expiring|2026-11-02",
                "Tom Becker (#1533)|10-Pack|Active|6 left",
                "Lena Fischer (#3110)|Yearly|Active|2027-05-30",
                "Diego Ramos (#2871)|Trainer|Active|—",
                "Aisha Khan (#1902)|Monthly|Frozen|2026-12-18",
                "Jonas Weber (#1254)|Day Pass|Expired|2026-10-27");
    }

    @FXML
    public void onSell() {
        // Stub for membership sale flow
    }

    @FXML
    public void onSell(ActionEvent event) {
        onSell();
    }

    @FXML
    public void onRenew() {
        // Stub for membership renewal flow
    }

    @FXML
    public void onRenew(ActionEvent event) {
        onRenew();
    }

    @FXML
    public void onFreeze() {
        // Stub for membership freeze flow
    }

    @FXML
    public void onFreeze(ActionEvent event) {
        onFreeze();
    }

    @FXML
    public void onUnfreeze() {
        // Stub for membership unfreeze flow
    }

    @FXML
    public void onUnfreeze(ActionEvent event) {
        onUnfreeze();
    }

    @FXML
    public void onCancel() {
        // Stub for membership cancellation flow
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

