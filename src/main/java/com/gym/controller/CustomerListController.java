package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3SpeedDial;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.list.M3ListItem;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.progress.M3LinearProgress;
import io.m3fx.controls.scene3d.M3TiltCard3D;
import io.m3fx.controls.search.M3SearchBar;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for the Customer List Roster & Athlete Directory (customer-list.fxml).
 * Conforms to docs/ui/members/CUSTOMER_LIST_UI.md Section 7 contract.
 * Pure UI: strictly no backend service, DAO, or database logic.
 *
 * <p>UX contract: the roster is a working demo data set — the search bar and filter chips
 * actually filter it (so the table's empty states are reachable), Add Member opens a real
 * entry dialog, export copies a CSV to the clipboard, delete confirms first and offers
 * Undo, and every answer surfaces through an {@link M3Snackbar}.
 */
public class CustomerListController {

    /** Row layout: id, name, phone, email, status, trainer, 8-wk activity %, joined. */
    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_PHONE = 2;
    private static final int COL_EMAIL = 3;
    private static final int COL_STATUS = 4;
    private static final int COL_TRAINER = 5;
    private static final int COL_ACTIVITY = 6;
    private static final int COL_JOINED = 7;

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

    /** Overlay host for snackbars and dialogs, added to the screen root in code (no fx:id). */
    private M3Overlay overlay;
    /** Exclusive single-selection group behind the five filter chips. */
    private ToggleGroup filterGroup;
    /** The full demo roster; the table shows the chip/search-filtered subset. */
    private final List<Object[]> roster = new ArrayList<>();
    /** Next membership number issued by the Add Member dialog. */
    private int nextMemberNumber = 1043;
    /** Row currently shown in the quick-peek side sheet, or {@code null}. */
    private Object[] selectedRow;
    /** Whether the side sheet shows the short peek or the full dossier. */
    private boolean fullDossier;

    public CustomerListController() {
        // Public no-arg constructor for FXMLLoader
    }

    @FXML
    public void initialize() {
        overlay = new M3Overlay();
        rootContainer.getChildren().add(overlay);

        loadDemoRoster();
        wireRosterColumns();
        keepFilterChipsExclusive();
        hideTableBuiltInSearch();
        applyRosterView();
        fillSearchSuggestions();
        hideSideSheet();
        wireSpeedDial();
    }

    // ------------------------------------------------------------------
    // Event handlers (answers for every visible action)
    // ------------------------------------------------------------------

    /** Opens a small in-screen menu: jump to the search box or clear search + filters. */
    @FXML
    public void onToggleNavRail() {
        M3Menu menu = new M3Menu();
        menu.item("Focus search box", searchBar::requestFocus);
        menu.item("Clear search & filters", this::clearSearchAndFilters);
        showMenuBelow(btnNavMenu, menu,
                "Search tip: type a name, phone, email, ID or coach in the bar above.");
    }

    /** Exports the visible roster rows as CSV to the clipboard and confirms the count. */
    @FXML
    public void onExportData() {
        List<Object[]> visible = table().getItems();
        if (visible.isEmpty()) {
            overlay.showSnackbar("Nothing to export — the current view has no athletes.");
            return;
        }
        StringBuilder csv = new StringBuilder("ID,Name,Phone,Email,Status,Trainer,8-Wk Activity,Joined\n");
        for (Object[] row : visible) {
            csv.append(row[COL_ID]).append(',')
                    .append(row[COL_NAME]).append(',')
                    .append(row[COL_PHONE]).append(',')
                    .append(row[COL_EMAIL]).append(',')
                    .append(row[COL_STATUS]).append(',')
                    .append(row[COL_TRAINER]).append(',')
                    .append(row[COL_ACTIVITY]).append('%').append(',')
                    .append(row[COL_JOINED]).append('\n');
        }
        try {
            Clipboard.getSystemClipboard()
                    .setContent(Map.of(DataFormat.PLAIN_TEXT, csv.toString()));
            overlay.showSnackbar("Copied " + visible.size() + " athletes as CSV to the clipboard.");
        } catch (RuntimeException clipboardUnavailable) {
            overlay.showSnackbar("Clipboard unavailable in this environment.");
        }
    }

    /** Explains the roster screen in a dismissible dialog (real content, not a stub). */
    @FXML
    public void onShowHelp() {
        M3Button gotIt = M3Button.text("Got it");
        M3Dialog dialog = M3Dialog.basic("How the roster works", helpContent(), gotIt);
        gotIt.setOnAction(e -> dialog.hide());
        dialog.setDefaultAction(gotIt::activate);
        overlay.showDialog(dialog);
    }

    /** Live search: combines the query with the active filter chip and re-renders. */
    @FXML
    public void onSearch() {
        applyRosterView();
    }

    /** Opens the Add Member dialog; confirming inserts the athlete into the roster. */
    @FXML
    public void onAdd() {
        M3TextField nameField = new M3TextField("Full name", TextFieldVariant.OUTLINED);
        M3TextField phoneField = new M3TextField("Phone (01xxxxxxxxx)", TextFieldVariant.OUTLINED);
        phoneField.setSupportingText("Digits only, e.g. 01005558877");
        Label hint = new Label("New members start on the Monthly Standard plan with Active status.");
        hint.setWrapText(true);

        M3Button cancel = M3Button.text("Cancel");
        M3Button add = M3Button.text("Add Member");
        M3Dialog dialog = M3Dialog.basic("Add member", new VBox(12, nameField, phoneField, hint),
                cancel, add);
        dialog.setDefaultAction(add::activate);
        add.setOnAction(e -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            boolean nameOk = !name.isEmpty();
            boolean phoneOk = phone.matches("01\\d{9}");
            nameField.setError(!nameOk);
            phoneField.setError(!phoneOk);
            if (!nameOk || !phoneOk) {
                return;
            }
            dialog.hide();
            Object[] row = {"M-" + nextMemberNumber, name, phone, "", "Active",
                    "Unassigned", 0, "New"};
            nextMemberNumber++;
            roster.add(0, row);
            chipFilterAll.setSelected(true);
            applyRosterView();
            overlay.showSnackbar(name + " added to the roster (" + row[COL_ID] + ").");
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    /** Applies the selected filter chip (exactly one active) to the roster view. */
    @FXML
    public void onFilterChanged() {
        applyRosterView();
    }

    /** Hides the quick-peek side sheet. */
    @FXML
    public void onCloseSideSheet() {
        hideSideSheet();
    }

    /**
     * Toggles between the compact peek and the full dossier for the selected athlete;
     * the button relabels so the state change is visible without any popup.
     */
    @FXML
    public void onOpenSelectedProfile() {
        if (selectedRow == null) {
            overlay.showSnackbar("Select an athlete row first.");
            return;
        }
        fullDossier = !fullDossier;
        fillSideSheet(selectedRow);
        btnOpenFullProfile.setText(fullDossier ? "Collapse Dossier" : "Open Full Profile");
    }

    /** Two-step delete of the selected athlete with an Undo snackbar answer. */
    @FXML
    public void onDeleteSelectedCustomer() {
        deleteRow(selectedRow);
    }

    // ------------------------------------------------------------------
    // Demo roster, columns, filtering and side sheet
    // ------------------------------------------------------------------

    private void loadDemoRoster() {
        roster.add(new Object[] {"M-1007", "Layla Hassan", "01005558877", "layla.h@mailbox.com",
                "Active", "Coach Sarah", 92, "2025-02-11"});
        roster.add(new Object[] {"M-1012", "Omar Fathi", "01012244668", "omar.fathi@gym.io",
                "Expiring", "Coach Marcus", 74, "2025-03-02"});
        roster.add(new Object[] {"M-1015", "Nour El-Din", "01109876543", "nour.eldin@mailbox.com",
                "Active", "Coach Elena", 81, "2025-03-19"});
        roster.add(new Object[] {"M-1021", "Sara Adel", "01233445566", "sara.adel@gym.io",
                "Suspended", "Unassigned", 18, "2025-04-27"});
        roster.add(new Object[] {"M-1026", "Kareem Aziz", "01066778899", "kareem.aziz@mailbox.com",
                "Active", "Coach Marcus", 88, "2025-05-14"});
        roster.add(new Object[] {"M-1029", "Hana Mostafa", "01122334455", "hana.m@gym.io",
                "Expiring", "Coach Sarah", 63, "2025-05-30"});
        roster.add(new Object[] {"M-1031", "Youssef Kamal", "01099887766", "y.kamal@mailbox.com",
                "Active", "Coach Yusuf", 57, "2025-06-08"});
        roster.add(new Object[] {"M-1033", "Mariam Lotfy", "01288776655", "mariam.l@gym.io",
                "Inactive", "Unassigned", 5, "2025-06-21"});
        roster.add(new Object[] {"M-1035", "Tarek Nabil", "01011445599", "tarek.nabil@gym.io",
                "Active", "Coach Elena", 79, "2025-07-03"});
        roster.add(new Object[] {"M-1037", "Farida Samir", "01155667788", "farida.s@gym.io",
                "Expiring", "Coach Sarah", 45, "2025-07-15"});
        roster.add(new Object[] {"M-1040", "Ahmed Sobhy", "01077889900", "a.sobhy@mailbox.com",
                "Suspended", "Coach Yusuf", 22, "2025-08-01"});
        roster.add(new Object[] {"M-1042", "Dina Raouf", "01244556677", "dina.raouf@gym.io",
                "Active", "Coach Marcus", 96, "2025-08-18"});
    }

    @SuppressWarnings("unchecked")
    private M3DataTable<Object[]> table() {
        return (M3DataTable<Object[]>) (M3DataTable<?>) customerTable;
    }

    /**
     * Maps the FXML-declared TableColumn definitions (header text + preferred width) onto the
     * M3DataTable with real per-column extractors, a status badge cell and per-row actions.
     */
    private void wireRosterColumns() {
        if (customerTable == null) {
            return;
        }
        M3DataTable<Object[]> table = table();
        table.getColumns().add(new M3DataTable.Column<>(colId.getText(), row -> (String) row[COL_ID], colId.getPrefWidth()));
        table.getColumns().add(new M3DataTable.Column<>(colFullName.getText(), row -> (String) row[COL_NAME], colFullName.getPrefWidth()));
        table.getColumns().add(new M3DataTable.Column<>(colPhone.getText(), row -> (String) row[COL_PHONE], colPhone.getPrefWidth()));
        table.getColumns().add(new M3DataTable.Column<>(colEmail.getText(), row -> (String) row[COL_EMAIL], colEmail.getPrefWidth()));
        M3DataTable.Column<Object[]> statusColumn = new M3DataTable.Column<>(
                colStatus.getText(), row -> (String) row[COL_STATUS], colStatus.getPrefWidth());
        statusColumn.withCellFactory((row, value) -> statusBadge(value));
        table.getColumns().add(statusColumn);
        table.getColumns().add(new M3DataTable.Column<>(colTrainer.getText(), row -> (String) row[COL_TRAINER], colTrainer.getPrefWidth()));
        table.getColumns().add(new M3DataTable.Column<>(colAttendanceSpark.getText(),
                row -> String.valueOf(row[COL_ACTIVITY]) + "%", colAttendanceSpark.getPrefWidth()));
        // The actions column renders through its cell factory only; the extractor returns an
        // empty string because M3DataTable calls the extractor for every populated row.
        M3DataTable.Column<Object[]> actionsColumn = new M3DataTable.Column<>(
                colActions.getText(), row -> "", colActions.getPrefWidth());
        actionsColumn.withCellFactory((row, value) -> rowActions(row));
        table.getColumns().add(actionsColumn);

        // Row activation opens the quick-peek side sheet (click or Enter/Space).
        table.setOnRowAction(this::openSideSheet);
    }

    /** Puts the five filter chips in one ToggleGroup so exactly one filter is active at a time. */
    private void keepFilterChipsExclusive() {
        if (chipFilterAll == null) {
            return;
        }
        filterGroup = new ToggleGroup();
        filterGroup.getToggles().addAll(
                chipFilterAll, chipFilterActive, chipFilterSuspended, chipFilterInactive, chipFilterExpiring);
    }

    /** The table ships its own search field; this screen drives filtering from the app bar one. */
    private void hideTableBuiltInSearch() {
        customerTable.getChildren().removeIf(node -> node instanceof M3SearchBar);
    }

    /** Search suggestions: athlete names for quick jump-in. */
    private void fillSearchSuggestions() {
        for (Object[] row : roster) {
            searchBar.getSuggestions().add((String) row[COL_NAME]);
        }
    }

    /** Applies the active chip filter + search query to the visible table rows. */
    private void applyRosterView() {
        String query = searchBar.getQuery() == null ? "" : searchBar.getQuery().toLowerCase(Locale.ROOT).trim();
        String statusFilter = selectedStatusFilter();
        List<Object[]> view = new ArrayList<>();
        for (Object[] row : roster) {
            boolean matchesStatus = "All".equals(statusFilter) || statusFilter.equals(row[COL_STATUS]);
            boolean matchesQuery = query.isEmpty() || rowMatches(row, query);
            if (matchesStatus && matchesQuery) {
                view.add(row);
            }
        }
        table().getItems().setAll(view);
    }

    /** Chip label -> roster status. Called from the FX thread only. */
    private String selectedStatusFilter() {
        if (filterGroup == null) {
            return "All";
        }
        javafx.scene.control.Toggle selected = filterGroup.getSelectedToggle();
        if (selected == chipFilterActive) {
            return "Active";
        }
        if (selected == chipFilterSuspended) {
            return "Suspended";
        }
        if (selected == chipFilterInactive) {
            return "Inactive";
        }
        if (selected == chipFilterExpiring) {
            return "Expiring";
        }
        return "All";
    }

    private boolean rowMatches(Object[] row, String query) {
        for (int i = COL_ID; i <= COL_TRAINER; i++) {
            String value = String.valueOf(row[i]).toLowerCase(Locale.ROOT);
            if (value.contains(query)) {
                return true;
            }
        }
        return false;
    }

    private void clearSearchAndFilters() {
        searchBar.setQuery("");
        if (chipFilterAll != null) {
            chipFilterAll.setSelected(true);
        }
        applyRosterView();
        overlay.showSnackbar("Search cleared — showing all " + roster.size() + " athletes.");
    }

    // ------------------------------------------------------------------
    // Row cells, side sheet and destructive actions
    // ------------------------------------------------------------------

    /** Status cell: a themed label matching the table's cell typography. */
    private Label statusBadge(String status) {
        Label cell = new Label(status == null ? "" : status);
        cell.getStyleClass().add("m3-data-table-cell");
        return cell;
    }

    /** Per-row actions: peek the athlete in the side sheet, or delete with confirmation. */
    private HBox rowActions(Object[] row) {
        M3IconButton peek = M3IconButton.standard(M3Icon.symbol("person"));
        peek.setAccessibleText("Quick peek " + row[COL_NAME]);
        peek.setOnAction(e -> openSideSheet(row));

        M3IconButton delete = M3IconButton.standard(M3Icon.symbol("delete"));
        delete.setAccessibleText("Delete " + row[COL_NAME]);
        delete.setOnAction(e -> deleteRow(row));

        HBox actions = new HBox(4, peek, delete);
        actions.setMaxHeight(40);
        return actions;
    }

    private void openSideSheet(Object[] row) {
        selectedRow = row;
        fullDossier = false;
        btnOpenFullProfile.setText("Open Full Profile");
        fillSideSheet(row);
        sideSheetQuickPeek.setVisible(true);
        sideSheetQuickPeek.setManaged(true);
    }

    private void hideSideSheet() {
        selectedRow = null;
        fullDossier = false;
        if (sideSheetQuickPeek != null) {
            sideSheetQuickPeek.setVisible(false);
            sideSheetQuickPeek.setManaged(false);
        }
    }

    private void fillSideSheet(Object[] row) {
        holographicPassCard.setContent(identityPass(row));
        biometricHudBox.getChildren().setAll(sheetItems(row, fullDossier));
    }

    private VBox identityPass(Object[] row) {
        Label name = new Label((String) row[COL_NAME]);
        name.getStyleClass().add("m3-list-headline");
        Label meta = new Label(row[COL_ID] + " · " + row[COL_STATUS] + " · joined " + row[COL_JOINED]);
        meta.getStyleClass().add("m3-list-supporting");
        meta.setWrapText(true);
        return new VBox(6, name, meta);
    }

    /** Peek shows the three headline facts; the full dossier lists everything on record. */
    private List<M3ListItem> sheetItems(Object[] row, boolean full) {
        List<M3ListItem> items = new ArrayList<>();
        items.add(listItem("person", "Status", row[COL_STATUS] + " · 8-week activity " + row[COL_ACTIVITY] + "%"));
        items.add(listItem("group", "Assigned coach", String.valueOf(row[COL_TRAINER])));
        items.add(listItem("calendar", "Member since", String.valueOf(row[COL_JOINED])));
        if (full) {
            items.add(listItem("search", "Phone", String.valueOf(row[COL_PHONE])));
            items.add(listItem("info", "Email", String.valueOf(row[COL_EMAIL])));
            items.add(listItem("stats", "Membership", "Monthly Standard · renews monthly"));
            items.add(listItem("favorite", "Emergency contact", "On file with the front desk"));
        }
        return items;
    }

    private M3ListItem listItem(String symbol, String headline, String supporting) {
        M3ListItem item = new M3ListItem(headline, supporting, M3ListItem.TWO_LINES);
        item.setLeading(M3Icon.symbol(symbol));
        return item;
    }

    /** Confirm-then-delete with an Undo snackbar (the destructive-action contract). */
    private void deleteRow(Object[] row) {
        if (row == null) {
            overlay.showSnackbar("Select an athlete row first.");
            return;
        }
        String name = String.valueOf(row[COL_NAME]);
        M3Button cancel = M3Button.text("Keep Member");
        M3Button delete = M3Button.text("Delete");
        M3Dialog dialog = M3Dialog.basic("Delete " + name + "?",
                new Label("This removes " + row[COL_ID] + " from the roster view. Undo stays "
                        + "available right after the delete."),
                cancel, delete);
        dialog.setDefaultAction(delete::activate);
        delete.setOnAction(e -> {
            dialog.hide();
            int index = roster.indexOf(row);
            roster.remove(row);
            if (selectedRow == row) {
                hideSideSheet();
            }
            applyRosterView();
            overlay.showSnackbar(name + " removed from the roster.", "Undo", () -> {
                roster.add(Math.min(index, roster.size()), row);
                applyRosterView();
                overlay.showSnackbar(name + " restored to the roster.");
            });
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    private VBox helpContent() {
        Label intro = new Label("Three ways to find an athlete:");
        intro.setWrapText(true);
        M3ListItem search = listItem("search", "Search", "Type a name, phone, email, ID or coach in the bar above.");
        M3ListItem filter = listItem("check", "Filter chips", "Exactly one chip is active — All, Active, Suspended, Inactive or Expiring.");
        M3ListItem row = listItem("person", "Rows", "Click a row to peek the athlete; header cells sort; actions peek or delete.");
        VBox box = new VBox(10, intro, search, filter, row);
        box.setPrefWidth(380);
        return box;
    }

    /** The floating speed dial mirrors the two real roster answers. */
    private void wireSpeedDial() {
        speedDial.addAction("Add Member", M3Icon.symbol("add"), this::onAdd);
        speedDial.addAction("Export CSV", M3Icon.symbol("stats"), this::onExportData);
    }

    /** Shows an anchored menu; without a window (headless preview) it answers via snackbar. */
    private void showMenuBelow(javafx.scene.Node anchor, M3Menu menu, String fallback) {
        if (anchor.getScene() == null || anchor.getScene().getWindow() == null) {
            overlay.showSnackbar(fallback);
            return;
        }
        menu.showBelow(anchor);
    }
}
