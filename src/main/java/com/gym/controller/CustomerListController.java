package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3SpeedDial;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.progress.M3LinearProgress;
import io.m3fx.controls.scene3d.M3TiltCard3D;
import io.m3fx.controls.search.M3SearchBar;
import io.m3fx.controls.sheet.M3SideSheet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller skeleton for Customer List Roster & Athlete Directory (customer-list.fxml).
 * Conforms to docs/ui/members/CUSTOMER_LIST_UI.md Section 7 contract.
 * Pure UI stub: strictly no backend service, DAO, or database logic.
 */
public class CustomerListController {

    @FXML private StackPane rootContainer;
    @FXML private BorderPane mainLayout;
    @FXML private M3TopAppBar topAppBar;
    @FXML private M3IconButton btnNavMenu;
    @FXML private M3IconButton btnExport;
    @FXML private M3IconButton btnHelp;
    @FXML private M3LinearProgress loadingBar;

    @FXML private M3StatCard cardTotalActive;
    @FXML private M3StatCard cardExpiringSoon;
    @FXML private M3StatCard cardSuspended;
    @FXML private M3StatCard cardNewThisMonth;

    @FXML private M3SearchBar searchBar;
    @FXML private M3Button btnAddMember;

    @FXML private M3Chip chipFilterAll;
    @FXML private M3Chip chipFilterActive;
    @FXML private M3Chip chipFilterSuspended;
    @FXML private M3Chip chipFilterInactive;
    @FXML private M3Chip chipFilterExpiring;

    @FXML private M3DataTable<?> customerTable;
    @FXML private TableColumn<?, ?> colId;
    @FXML private TableColumn<?, ?> colFullName;
    @FXML private TableColumn<?, ?> colPhone;
    @FXML private TableColumn<?, ?> colEmail;
    @FXML private TableColumn<?, ?> colStatus;
    @FXML private TableColumn<?, ?> colTrainer;
    @FXML private TableColumn<?, ?> colAttendanceSpark;
    @FXML private TableColumn<?, ?> colActions;

    @FXML private M3SideSheet sideSheetQuickPeek;
    @FXML private M3IconButton btnCloseSideSheet;
    @FXML private M3TiltCard3D holographicPassCard;
    @FXML private VBox biometricHudBox;
    @FXML private M3Button btnOpenFullProfile;
    @FXML private M3IconButton btnDeleteCustomer;
    @FXML private M3SpeedDial speedDial;

    public CustomerListController() {
        // Public no-arg constructor for FXMLLoader
    }

    @FXML
    public void initialize() {
        wireRosterColumns();
        keepFilterChipsExclusive();
    }

    /**
     * Maps the FXML-declared TableColumn definitions (header text + preferred width) onto the
     * M3DataTable: its own {@code Column} model has no FXML-constructible constructor, so without
     * this wiring the roster table renders with no header row at all. Extractors enable header
     * sorting; the Actions column passes no extractor, matching its {@code sortable="false"}.
     */
    @SuppressWarnings("unchecked")
    private void wireRosterColumns() {
        if (customerTable == null) {
            return;
        }
        javafx.collections.ObservableList<M3DataTable.Column<String>> columns =
                ((M3DataTable<String>) customerTable).getColumns();
        columns.add(new M3DataTable.Column<>(colId.getText(), String::toString, colId.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colFullName.getText(), String::toString, colFullName.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colPhone.getText(), String::toString, colPhone.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colEmail.getText(), String::toString, colEmail.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colStatus.getText(), String::toString, colStatus.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colTrainer.getText(), String::toString, colTrainer.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAttendanceSpark.getText(), String::toString, colAttendanceSpark.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colActions.getText(), null, colActions.getPrefWidth()));
    }

    /** Puts the five filter chips in one ToggleGroup so exactly one filter is active at a time. */
    private void keepFilterChipsExclusive() {
        if (chipFilterAll == null) {
            return;
        }
        ToggleGroup filterGroup = new ToggleGroup();
        filterGroup.getToggles().addAll(
                chipFilterAll, chipFilterActive, chipFilterSuspended, chipFilterInactive, chipFilterExpiring);
    }

    @FXML
    public void onToggleNavRail() {
        // UI event handler stub
    }

    @FXML
    public void onExportData() {
        // UI event handler stub
    }

    @FXML
    public void onShowHelp() {
        // UI event handler stub
    }

    @FXML
    public void onSearch() {
        // UI event handler stub
    }

    @FXML
    public void onAdd() {
        // UI event handler stub
    }

    @FXML
    public void onFilterChanged() {
        // UI event handler stub
    }

    @FXML
    public void onCloseSideSheet() {
        // UI event handler stub
    }

    @FXML
    public void onOpenSelectedProfile() {
        // UI event handler stub
    }

    @FXML
    public void onDeleteSelectedCustomer() {
        // UI event handler stub
    }
}
