package com.gym.controller;

import com.gym.util.PhoneUtil;
import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.chip.ChipVariant;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for the Customer List Roster & Athlete Directory.
 * Conforms to docs/ui/members/CUSTOMER_LIST_UI.md Section 7 contract.
 * Pure UI: strictly no backend service, DAO, or database logic.
 *
 * <p>UX contract: the roster is a working demo data set — the search bar and filter chips
 * actually filter it (so the table's empty states are reachable), Add Member opens a real
 * entry dialog, export copies a CSV to the clipboard, delete confirms first and offers
 * Undo, and every answer surfaces through an {@link M3Overlay} snackbar.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former customer-list.fxml: app bar, KPI cards, search + filter chips, master table,
 * quick-peek side sheet, floating speed dial.
 */
public class CustomerListController implements ScreenController {

    /** Row layout: id, name, phone, email, status, trainer, 8-wk activity %, joined. */
    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_PHONE = 2;
    private static final int COL_EMAIL = 3;
    private static final int COL_STATUS = 4;
    private static final int COL_TRAINER = 5;
    private static final int COL_ACTIVITY = 6;
    private static final int COL_JOINED = 7;

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane rootContainer;
    private BorderPane mainLayout;
    private M3TopAppBar topAppBar;
    private M3IconButton btnNavMenu;
    private M3IconButton btnExport;
    private M3IconButton btnHelp;
    private M3LinearProgress loadingBar;
    private M3StatCard cardTotalActive;
    private M3StatCard cardExpiringSoon;
    private M3StatCard cardSuspended;
    private M3StatCard cardNewThisMonth;
    private M3SearchBar searchBar;
    private M3Button btnAddMember;
    private M3Chip chipFilterAll;
    private M3Chip chipFilterActive;
    private M3Chip chipFilterSuspended;
    private M3Chip chipFilterInactive;
    private M3Chip chipFilterExpiring;
    private M3DataTable<Object[]> customerTable;
    private TableColumn<Object[], ?> colId;
    private TableColumn<Object[], ?> colFullName;
    private TableColumn<Object[], ?> colPhone;
    private TableColumn<Object[], ?> colEmail;
    private TableColumn<Object[], ?> colStatus;
    private TableColumn<Object[], ?> colTrainer;
    private TableColumn<Object[], ?> colAttendanceSpark;
    private TableColumn<Object[], ?> colActions;
    private M3SideSheet sideSheetQuickPeek;
    private M3IconButton btnCloseSideSheet;
    private M3TiltCard3D holographicPassCard;
    private VBox biometricHudBox;
    private M3Button btnOpenFullProfile;
    private M3IconButton btnDeleteCustomer;
    private M3SpeedDial speedDial;

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

    /**
     * Builds the complete Member Roster view and initializes all behavior. Same ordering
     * semantics the FXMLLoader used to provide: the node tree exists and every field is
     * assigned before the former {@code initialize()} work runs.
     */
    public CustomerListController() {
        // Column declarations wired onto the M3DataTable in wireRosterColumns()
        // (M3DataTable.Column is not declarable in markup).
        colId = column("ID", 70);
        colFullName = column("Athlete Full Name", 220);
        colPhone = column("Phone Number", 140);
        colEmail = column("Email Address", 200);
        colStatus = column("Status", 120);
        colTrainer = column("Assigned Coach", 160);
        colAttendanceSpark = column("8-Wk Activity", 110);
        colActions = column("Actions", 120);

        buildView();

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        rootContainer.setId("rootContainer");
        mainLayout.setId("mainLayout");
        topAppBar.setId("topAppBar");
        btnNavMenu.setId("btnNavMenu");
        btnExport.setId("btnExport");
        btnHelp.setId("btnHelp");
        loadingBar.setId("loadingBar");
        cardTotalActive.setId("cardTotalActive");
        cardExpiringSoon.setId("cardExpiringSoon");
        cardSuspended.setId("cardSuspended");
        cardNewThisMonth.setId("cardNewThisMonth");
        searchBar.setId("searchBar");
        btnAddMember.setId("btnAddMember");
        chipFilterAll.setId("chipFilterAll");
        chipFilterActive.setId("chipFilterActive");
        chipFilterSuspended.setId("chipFilterSuspended");
        chipFilterInactive.setId("chipFilterInactive");
        chipFilterExpiring.setId("chipFilterExpiring");
        customerTable.setId("customerTable");
        colId.setId("colId");
        colFullName.setId("colFullName");
        colPhone.setId("colPhone");
        colEmail.setId("colEmail");
        colStatus.setId("colStatus");
        colTrainer.setId("colTrainer");
        colAttendanceSpark.setId("colAttendanceSpark");
        colActions.setId("colActions");
        sideSheetQuickPeek.setId("sideSheetQuickPeek");
        btnCloseSideSheet.setId("btnCloseSideSheet");
        holographicPassCard.setId("holographicPassCard");
        biometricHudBox.setId("biometricHudBox");
        btnOpenFullProfile.setId("btnOpenFullProfile");
        btnDeleteCustomer.setId("btnDeleteCustomer");
        speedDial.setId("speedDial");
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

    /** One roster table column definition (header text + preferred width). */
    private static TableColumn<Object[], ?> column(String text, double prefWidth) {
        TableColumn<Object[], ?> col = new TableColumn<>(text);
        col.setPrefWidth(prefWidth);
        return col;
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for customer-list.fxml) and
     * wires every handler.
     */
    private StackPane buildView() {
        mainLayout = new BorderPane();

        // Top app bar.
        topAppBar = new M3TopAppBar("ATHLETE & MEMBER ROSTER",
                io.m3fx.controls.appbar.TopAppBarVariant.CENTER_ALIGNED);
        HBox.setHgrow(topAppBar, Priority.ALWAYS);

        btnNavMenu = new M3IconButton(M3Icon.symbol("menu"), io.m3fx.controls.iconbutton.IconButtonVariant.STANDARD);
        btnNavMenu.setOnAction(e -> onToggleNavRail());
        btnNavMenu.setAccessibleText("Roster menu: focus search or clear filters");
        topAppBar.setNavigationIcon(btnNavMenu);

        HBox appActions = new HBox(8);
        appActions.setAlignment(Pos.CENTER_RIGHT);
        appActions.setPadding(new Insets(0, 16, 0, 0));
        btnExport = new M3IconButton(M3Icon.symbol("arrow_drop_down"), io.m3fx.controls.iconbutton.IconButtonVariant.STANDARD);
        btnExport.setOnAction(e -> onExportData());
        btnExport.setAccessibleText("Export visible athletes as CSV to clipboard");
        btnHelp = new M3IconButton(M3Icon.symbol("info"), io.m3fx.controls.iconbutton.IconButtonVariant.STANDARD);
        btnHelp.setOnAction(e -> onShowHelp());
        btnHelp.setAccessibleText("How the roster works");
        appActions.getChildren().addAll(btnExport, btnHelp);

        HBox appBarRow = new HBox();
        appBarRow.setAlignment(Pos.CENTER_LEFT);
        appBarRow.getStyleClass().add("app-bar-container");
        appBarRow.getChildren().addAll(topAppBar, appActions);

        loadingBar = new M3LinearProgress();
        loadingBar.setVisible(false);
        loadingBar.setManaged(false);

        VBox topBlock = new VBox(appBarRow, loadingBar);
        mainLayout.setTop(topBlock);

        // Main bento workspace.
        VBox workspace = new VBox(16);
        workspace.setPadding(new Insets(16, 24, 64, 24));

        // Bento KPI summary cards.
        HBox kpiRow = new HBox(16);
        kpiRow.setFillHeight(true);
        cardTotalActive = new M3StatCard("TOTAL ACTIVE", "1,428", "84% Operating Capacity");
        cardExpiringSoon = new M3StatCard("EXPIRING (< 7 DAYS)", "38", "Requires Follow-Up");
        cardSuspended = new M3StatCard("SUSPENDED / HOLD", "14", "Memberships Frozen");
        cardNewThisMonth = new M3StatCard("NEW THIS MONTH", "+86", "+12% vs Previous Month");
        for (M3StatCard card : List.of(cardTotalActive, cardExpiringSoon, cardSuspended, cardNewThisMonth)) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        kpiRow.getChildren().addAll(cardTotalActive, cardExpiringSoon, cardSuspended, cardNewThisMonth);

        // Search and filter bar.
        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchBar = new M3SearchBar();
        HBox.setHgrow(searchBar, Priority.ALWAYS);
        searchBar.setOnKeyReleased(e -> onSearch());
        btnAddMember = new M3Button("Add Member", ButtonVariant.FILLED);
        btnAddMember.setGraphic(M3Icon.symbol("add"));
        btnAddMember.setOnAction(e -> onAdd());
        searchRow.getChildren().addAll(searchBar, btnAddMember);

        // Filter chips flow.
        FlowPane chipFlow = new FlowPane(8, 8);
        chipFilterAll = new M3Chip("All Members", ChipVariant.FILTER);
        chipFilterAll.setSelected(true);
        chipFilterActive = new M3Chip("Active Only", ChipVariant.FILTER);
        chipFilterSuspended = new M3Chip("Suspended", ChipVariant.FILTER);
        chipFilterInactive = new M3Chip("Inactive", ChipVariant.FILTER);
        chipFilterExpiring = new M3Chip("Expiring Soon", ChipVariant.FILTER);
        for (M3Chip chip : List.of(chipFilterAll, chipFilterActive, chipFilterSuspended,
                chipFilterInactive, chipFilterExpiring)) {
            chip.setOnAction(e -> onFilterChanged());
        }
        chipFlow.getChildren().addAll(chipFilterAll, chipFilterActive, chipFilterSuspended,
                chipFilterInactive, chipFilterExpiring);

        // Virtualized master data table: rows open the quick-peek sheet, headers sort.
        customerTable = new M3DataTable<>();
        VBox.setVgrow(customerTable, Priority.ALWAYS);

        workspace.getChildren().addAll(kpiRow, searchRow, chipFlow, customerTable);
        mainLayout.setCenter(workspace);

        // Contextual quick-peek side sheet (collapsible drawer).
        sideSheetQuickPeek = new M3SideSheet();
        sideSheetQuickPeek.setVisible(false);
        StackPane.setAlignment(sideSheetQuickPeek, Pos.CENTER_RIGHT);

        VBox sheetContent = new VBox(16);
        sheetContent.setPrefWidth(380);
        sheetContent.setPadding(new Insets(20));

        HBox sheetHeader = new HBox();
        sheetHeader.setAlignment(Pos.CENTER_LEFT);
        Region sheetSpacer = new Region();
        HBox.setHgrow(sheetSpacer, Priority.ALWAYS);
        btnCloseSideSheet = new M3IconButton(M3Icon.symbol("close"), io.m3fx.controls.iconbutton.IconButtonVariant.STANDARD);
        btnCloseSideSheet.setOnAction(e -> onCloseSideSheet());
        btnCloseSideSheet.setAccessibleText("Close quick peek");
        sheetHeader.getChildren().addAll(
                styledLabel("ATHLETE QUICK PEEK", "title-medium"),
                sheetSpacer,
                btnCloseSideSheet);

        // 3D holographic identity pass.
        holographicPassCard = new M3TiltCard3D();

        // Fast biometric HUD.
        biometricHudBox = new VBox(8);
        biometricHudBox.getStyleClass().add("biometric-hud-pane");

        // Action triggers.
        HBox sheetActions = new HBox(8);
        btnOpenFullProfile = new M3Button("Open Full Profile", ButtonVariant.FILLED);
        HBox.setHgrow(btnOpenFullProfile, Priority.ALWAYS);
        btnOpenFullProfile.setOnAction(e -> onOpenSelectedProfile());
        btnDeleteCustomer = new M3IconButton(M3Icon.symbol("delete"), io.m3fx.controls.iconbutton.IconButtonVariant.STANDARD);
        btnDeleteCustomer.setOnAction(e -> onDeleteSelectedCustomer());
        btnDeleteCustomer.setAccessibleText("Delete selected athlete");
        sheetActions.getChildren().addAll(btnOpenFullProfile, btnDeleteCustomer);

        sheetContent.getChildren().addAll(sheetHeader, holographicPassCard, biometricHudBox, sheetActions);
        sideSheetQuickPeek.setContent(sheetContent);

        // Floating action speed dial.
        speedDial = new M3SpeedDial();
        speedDial.setMaxSize(56, 56);
        speedDial.setPickOnBounds(false);
        StackPane.setAlignment(speedDial, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(speedDial, new Insets(0, 32, 32, 0));

        rootContainer = new StackPane();
        rootContainer.getStyleClass().add("customer-list-root");
        rootContainer.getChildren().addAll(mainLayout, sideSheetQuickPeek, speedDial);

        overlay = new M3Overlay();
        return rootContainer;
    }

    /** Small label with one style class. */
    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    @Override
    public Parent getRoot() {
        return rootContainer;
    }

    // ------------------------------------------------------------------
    // Event handlers (answers for every visible action)
    // ------------------------------------------------------------------

    /** Opens a small in-screen menu: jump to the search box or clear search + filters. */
    public void onToggleNavRail() {
        M3Menu menu = new M3Menu();
        menu.item("Focus search box", searchBar::requestFocus);
        menu.item("Clear search & filters", this::clearSearchAndFilters);
        showMenuBelow(btnNavMenu, menu,
                "Search tip: type a name, phone, email, ID or coach in the bar above.");
    }

    /** Exports the visible roster rows as CSV to the clipboard and confirms the count. */
    public void onExportData() {
        List<Object[]> visible = customerTable.getItems();
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
    public void onShowHelp() {
        M3Button gotIt = M3Button.text("Got it");
        M3Dialog dialog = M3Dialog.basic("How the roster works", helpContent(), gotIt);
        gotIt.setOnAction(e -> dialog.hide());
        dialog.setDefaultAction(gotIt::activate);
        overlay.showDialog(dialog);
    }

    /** Live search: combines the query with the active filter chip and re-renders. */
    public void onSearch() {
        applyRosterView();
    }

    /** Opens the Add Member dialog; confirming inserts the athlete into the roster. */
    public void onAdd() {
        M3TextField nameField = new M3TextField("Full name", TextFieldVariant.OUTLINED);
        M3TextField phoneField = new M3TextField("Phone (010/011/012/015 + 8 digits)", TextFieldVariant.OUTLINED);
        phoneField.setSupportingText("11 digits, e.g. 01005558877");
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
            boolean phoneOk = PhoneUtil.isValid(phone);
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
    public void onFilterChanged() {
        applyRosterView();
    }

    /** Hides the quick-peek side sheet. */
    public void onCloseSideSheet() {
        hideSideSheet();
    }

    /**
     * Navigates the shell to the athlete's full dossier (customer-profile): fires the pinned
     * {@code navCustomers} rail destination on the way, exactly like a rail click, and passes
     * the selected member identity so {@link CustomerProfileController} prefills the name. The
     * quick-peek side sheet stays the answer for the peek action (row click / row icon).
     * Standalone previews and tests without the owning shell answer with a snackbar.
     */
    public void onOpenSelectedProfile() {
        if (selectedRow == null) {
            overlay.showSnackbar("Select an athlete row first.");
            return;
        }
        String name = String.valueOf(selectedRow[COL_NAME]);
        String memberId = String.valueOf(selectedRow[COL_ID]);
        AppShellController shell = shellFromScene();
        if (shell != null) {
            shell.openScreenViaRail("navCustomers", Screen.CUSTOMER_PROFILE, controller -> {
                if (controller instanceof CustomerProfileController profile) {
                    profile.showMember(name, memberId);
                }
            });
            return;
        }
        overlay.showSnackbar("Full profile opens in the app window — " + name + " (" + memberId + ").");
    }

    /**
     * The owning shell controller, published in the scene-root properties by
     * {@link AppShellController} ({@code AppShellController.SHELL_CONTROLLER_KEY}); null in
     * standalone previews and tests.
     */
    private AppShellController shellFromScene() {
        if (rootContainer.getScene() == null
                || !(rootContainer.getScene().getRoot() instanceof javafx.scene.Parent sceneRoot)) {
            return null;
        }
        Object shell = sceneRoot.getProperties().get(AppShellController.SHELL_CONTROLLER_KEY);
        return shell instanceof AppShellController controller ? controller : null;
    }

    /** Two-step delete of the selected athlete with an Undo snackbar answer. */
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

    /**
     * Maps the column definitions (header text + preferred width) onto the M3DataTable with
     * real per-column extractors, a status badge cell and per-row actions.
     */
    private void wireRosterColumns() {
        customerTable.getColumns().add(new M3DataTable.Column<>(colId.getText(), row -> (String) row[COL_ID], colId.getPrefWidth()));
        customerTable.getColumns().add(new M3DataTable.Column<>(colFullName.getText(), row -> (String) row[COL_NAME], colFullName.getPrefWidth()));
        customerTable.getColumns().add(new M3DataTable.Column<>(colPhone.getText(), row -> (String) row[COL_PHONE], colPhone.getPrefWidth()));
        customerTable.getColumns().add(new M3DataTable.Column<>(colEmail.getText(), row -> (String) row[COL_EMAIL], colEmail.getPrefWidth()));
        M3DataTable.Column<Object[]> statusColumn = new M3DataTable.Column<>(
                colStatus.getText(), row -> (String) row[COL_STATUS], colStatus.getPrefWidth());
        statusColumn.withCellFactory((row, value) -> statusBadge(value));
        customerTable.getColumns().add(statusColumn);
        customerTable.getColumns().add(new M3DataTable.Column<>(colTrainer.getText(), row -> (String) row[COL_TRAINER], colTrainer.getPrefWidth()));
        customerTable.getColumns().add(new M3DataTable.Column<>(colAttendanceSpark.getText(),
                row -> String.valueOf(row[COL_ACTIVITY]) + "%", colAttendanceSpark.getPrefWidth()));
        // The actions column renders through its cell factory only; the extractor returns an
        // empty string because M3DataTable calls the extractor for every populated row.
        M3DataTable.Column<Object[]> actionsColumn = new M3DataTable.Column<>(
                colActions.getText(), row -> "", colActions.getPrefWidth());
        actionsColumn.withCellFactory((row, value) -> rowActions(row));
        customerTable.getColumns().add(actionsColumn);

        // Row activation opens the quick-peek side sheet (click or Enter/Space).
        customerTable.setOnRowAction(this::openSideSheet);
    }

    /** Puts the five filter chips in one ToggleGroup so exactly one filter is active at a time. */
    private void keepFilterChipsExclusive() {
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
        customerTable.getItems().setAll(view);
    }

    /** Chip label -> roster status. Called from the FX thread only. */
    private String selectedStatusFilter() {
        if (filterGroup == null) {
            return "All";
        }
        Toggle selected = filterGroup.getSelectedToggle();
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
        chipFilterAll.setSelected(true);
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
        fillSideSheet(row);
        sideSheetQuickPeek.setVisible(true);
        sideSheetQuickPeek.setManaged(true);
    }

    private void hideSideSheet() {
        selectedRow = null;
        sideSheetQuickPeek.setVisible(false);
        sideSheetQuickPeek.setManaged(false);
    }

    private void fillSideSheet(Object[] row) {
        holographicPassCard.setContent(identityPass(row));
        biometricHudBox.getChildren().setAll(sheetItems(row));
    }

    private VBox identityPass(Object[] row) {
        Label name = new Label((String) row[COL_NAME]);
        name.getStyleClass().add("m3-list-headline");
        Label meta = new Label(row[COL_ID] + " · " + row[COL_STATUS] + " · joined " + row[COL_JOINED]);
        meta.getStyleClass().add("m3-list-supporting");
        meta.setWrapText(true);
        return new VBox(6, name, meta);
    }

    /** The peek sheet shows the three headline facts; the full dossier lives in the profile screen. */
    private List<M3ListItem> sheetItems(Object[] row) {
        List<M3ListItem> items = new ArrayList<>();
        items.add(listItem("person", "Status", row[COL_STATUS] + " · 8-week activity " + row[COL_ACTIVITY] + "%"));
        items.add(listItem("group", "Assigned coach", String.valueOf(row[COL_TRAINER])));
        items.add(listItem("calendar", "Member since", String.valueOf(row[COL_JOINED])));
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
    private void showMenuBelow(Node anchor, M3Menu menu, String fallback) {
        if (anchor.getScene() == null || anchor.getScene().getWindow() == null) {
            overlay.showSnackbar(fallback);
            return;
        }
        menu.showBelow(anchor);
    }
}
