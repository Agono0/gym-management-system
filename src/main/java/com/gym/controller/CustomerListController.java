package com.gym.controller;

import com.gym.dao.AttendanceDao;
import com.gym.dao.CustomerDao;
import com.gym.dao.MembershipDao;
import com.gym.dao.TrainerDao;
import com.gym.model.Attendance;
import com.gym.model.Customer;
import com.gym.model.CustomerStatus;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.Trainer;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
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

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Controller for the Customer List Roster & Athlete Directory.
 * Conforms to docs/ui/members/CUSTOMER_LIST_UI.md Section 7 contract.
 *
 * <p>Data contract (DECISIONS.md 77/78): the roster is loaded from the real SQLite database
 * through {@code CustomerDao} on a background task via {@link Data#loadAsync} — no service
 * layer, no business rules in the UI; DAO {@code ValidationException} messages surface
 * verbatim in snackbars. The KPI strip is computed from the same loaded rows, so it always
 * agrees with the table. Freeze/renew/assign (rules 6/7/15) and the profile flows are
 * Milestone-3 (C3) scope.
 *
 * <p>UX contract: the search bar and filter chips filter the live roster (so the table's
 * empty states are reachable), Add Member opens a real entry dialog backed by
 * {@code CustomerDao}, delete implements rule 4 (status change to INACTIVE) with Undo,
 * export copies a CSV to the clipboard, and every answer surfaces through an
 * {@link M3Overlay} snackbar.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former customer-list.fxml: app bar, KPI cards, search + filter chips, master table,
 * quick-peek side sheet, floating speed dial.
 */
public class CustomerListController implements ScreenController {

    /** Row layout: id, name, phone, email, status, trainer, 8-wk activity %, joined, raw status, db id. */
    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_PHONE = 2;
    private static final int COL_EMAIL = 3;
    private static final int COL_STATUS = 4;
    private static final int COL_TRAINER = 5;
    private static final int COL_ACTIVITY = 6;
    private static final int COL_JOINED = 7;
    private static final int COL_RAW_STATUS = 8;
    private static final int COL_CUSTOMER_ID = 9;

    /** A customer whose latest ACTIVE membership ends within this horizon counts as expiring. */
    private static final int EXPIRING_DAYS = 7;

    /** The activity column counts attendance weeks within this many past days. */
    private static final int ACTIVITY_WINDOW_DAYS = 56;

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
    /** Host of the master table; swaps to loading/empty/error states around DAO loads. */
    private StackPane contentHost;
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
    /** The full roster loaded from CustomerDao; the table shows the chip/search-filtered subset. */
    private final List<Object[]> roster = new ArrayList<>();
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
        contentHost.setId("contentHost");
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

        wireRosterColumns();
        keepFilterChipsExclusive();
        hideTableBuiltInSearch();
        hideSideSheet();
        wireSpeedDial();
        loadRoster();
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

        // Main bento workspace. The bottom inset reserves the floating speed dial's full
        // footprint (56px FAB + 32px edge margins + clearance), so the dial never covers the
        // table's pagination or the last row's action icons.
        VBox workspace = new VBox(16);
        workspace.setPadding(new Insets(16, 24, 112, 24));

        // Bento KPI summary cards (filled from the loaded roster in applyKpis()).
        HBox kpiRow = new HBox(16);
        kpiRow.setFillHeight(true);
        cardTotalActive = new M3StatCard("TOTAL ACTIVE", "-", "of the loaded roster");
        // Short delta copy: the badge rides beside the title on these narrow cards, so a
        // wordy badge squeezed the title into mid-word slivers ("EXPIRING (< …" at 1280).
        cardExpiringSoon = new M3StatCard("EXPIRING (< 7 DAYS)", "-", "Follow-Up");
        cardSuspended = new M3StatCard("SUSPENDED / HOLD", "-", "Frozen");
        cardNewThisMonth = new M3StatCard("NEW THIS MONTH", "-", "Joined this month");
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

        // Virtualized master data table: rows open the quick-peek sheet, headers sort. It lives
        // in a host pane so the load/empty/error states can swap around it.
        customerTable = new M3DataTable<>();
        VBox.setVgrow(customerTable, Priority.ALWAYS);
        contentHost = new StackPane(customerTable);

        workspace.getChildren().addAll(kpiRow, searchRow, chipFlow, contentHost);
        VBox.setVgrow(contentHost, Priority.ALWAYS);

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

        // Floating action speed dial. It pins to the workspace host's bottom edge — the host
        // is exactly the scroll content's height, so the FAB always sits in the reserved
        // band below the roster (a root-pinned FAB would land mid-table wherever the scene
        // clips the root).
        speedDial = new M3SpeedDial();
        speedDial.setMaxSize(56, 56);
        speedDial.setPickOnBounds(false);
        StackPane.setAlignment(speedDial, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(speedDial, new Insets(0, 32, 32, 0));

        // The workspace rides in a host pane so the speed dial can pin to the scroll
        // content's bottom edge (below the last row + the reserved bottom inset).
        StackPane workspaceHost = new StackPane(workspace, speedDial);
        mainLayout.setCenter(workspaceHost);

        rootContainer = new StackPane();
        rootContainer.getStyleClass().add("customer-list-root");
        rootContainer.getChildren().addAll(mainLayout, sideSheetQuickPeek);

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

    /**
     * Opens the Add Member dialog; confirming inserts a real customer through
     * {@code CustomerDao} on a background task ({@link Data#submit}). The inline checks
     * mirror the DAO's rules for instant field feedback; a DAO {@code ValidationException}
     * surfaces verbatim in a snackbar and the dialog stays open so the values can be fixed.
     */
    public void onAdd() {
        M3TextField nameField = new M3TextField("Full name", TextFieldVariant.OUTLINED);
        M3TextField phoneField = new M3TextField("Phone (010/011/012/015 + 8 digits)", TextFieldVariant.OUTLINED);
        phoneField.setSupportingText("11 digits, e.g. 01005558877");
        M3TextField emergencyNameField = new M3TextField("Emergency contact name", TextFieldVariant.OUTLINED);
        M3TextField emergencyPhoneField = new M3TextField("Emergency contact phone", TextFieldVariant.OUTLINED);
        emergencyPhoneField.setSupportingText("11 digits, e.g. 01005558877");
        Label hint = new Label("New members join with Active status; a coach can be assigned later.");
        hint.setWrapText(true);

        M3Button cancel = M3Button.text("Cancel");
        M3Button add = M3Button.text("Add Member");
        M3Dialog dialog = M3Dialog.basic("Add member", new VBox(10,
                UiKit.formRow("Member", nameField),
                UiKit.formRow("Phone", phoneField),
                UiKit.formRow("Emergency", emergencyNameField),
                UiKit.formRow("Contact no.", emergencyPhoneField),
                hint),
                cancel, add);
        dialog.setDefaultAction(add::activate);
        add.setOnAction(e -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String emergencyName = emergencyNameField.getText().trim();
            String emergencyPhone = emergencyPhoneField.getText().trim();
            boolean nameOk = !name.isEmpty();
            boolean phoneOk = PhoneUtil.isValid(phone);
            boolean emergencyNameOk = !emergencyName.isEmpty();
            boolean emergencyPhoneOk = PhoneUtil.isValid(emergencyPhone);
            nameField.setError(!nameOk);
            phoneField.setError(!phoneOk);
            emergencyNameField.setError(!emergencyNameOk);
            emergencyPhoneField.setError(!emergencyPhoneOk);
            if (!nameOk || !phoneOk || !emergencyNameOk || !emergencyPhoneOk) {
                return;
            }
            CustomerDao customerDao = new CustomerDao();
            Data.submit(
                    () -> customerDao.insert(new Customer(0, name, phone, "", "", "", "",
                            emergencyName, emergencyPhone, CustomerStatus.ACTIVE,
                            LocalDate.now().toString(), null)),
                    inserted -> {
                        dialog.hide();
                        chipFilterAll.setSelected(true);
                        loadRoster();
                        overlay.showSnackbar(inserted.getFullName() + " added to the roster (M-"
                                + String.format("%04d", inserted.getId()) + ").");
                    },
                    error -> overlay.showSnackbar(
                            "Could not add member: " + error.getMessage()));
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
    // Roster loading (CustomerDao on a background task), columns, filtering
    // ------------------------------------------------------------------

    /**
     * Loads the roster from the database on a background task: the bootstrap
     * ({@link Data#ensureReady()}), the four queries and the row assembly run off the FX
     * thread; the result lands in {@link #roster} and the KPI strip on the FX thread. Shows
     * {@link UiKit#loadingState()} while running, {@link UiKit#emptyState()} when the database
     * has no customers and {@link UiKit#errorState()} with retry on failure.
     */
    private void loadRoster() {
        Data.loadAsync(contentHost,
            () -> {
                Data.ensureReady();
                return buildRows(new CustomerDao().findAll(), new TrainerDao().findAll(),
                        new MembershipDao().findAll(), new AttendanceDao().findAll());
            },
            rows -> {
                roster.clear();
                roster.addAll(rows);
                contentHost.getChildren().setAll(roster.isEmpty()
                        ? UiKit.emptyState("No members in the database yet.",
                                "Add Member", this::onAdd)
                        : customerTable);
                applyKpis();
                fillSearchSuggestions();
                applyRosterView();
            },
            error -> contentHost.getChildren().setAll(UiKit.errorState(
                    error.getMessage() == null ? error.toString() : error.getMessage(),
                    this::loadRoster)));
    }

    /**
     * Assembles the table rows off the FX thread (no JavaFX types touched here).
     * Presentation-only derivations, owned by C3 to revisit: display status "Expiring" means
     * an ACTIVE customer whose latest ACTIVE membership ends within {@link #EXPIRING_DAYS}
     * days; "8-Wk Activity %" is the share of the past 8 weeks (of 8) with at least one
     * attendance check-in. These are UI metrics, not business rules.
     */
    private static List<Object[]> buildRows(List<Customer> customers, List<Trainer> trainers,
                                            List<Membership> memberships,
                                            List<Attendance> attendance) {
        LocalDate today = LocalDate.now();
        Map<Integer, String> trainerNames = new HashMap<>();
        for (Trainer trainer : trainers) {
            trainerNames.put(trainer.getId(), trainer.getFullName());
        }
        Map<Integer, Membership> activeMembership = new HashMap<>();
        for (Membership membership : memberships) {
            if (membership.getStatus() != MembershipStatus.ACTIVE) {
                continue;
            }
            Membership current = activeMembership.get(membership.getCustomerId());
            if (current == null
                    || membership.getEndDate().compareTo(current.getEndDate()) > 0) {
                activeMembership.put(membership.getCustomerId(), membership);
            }
        }
        Map<Integer, Set<Integer>> attendanceWeeks = new HashMap<>();
        for (Attendance record : attendance) {
            long daysAgo = ChronoUnit.DAYS.between(LocalDate.parse(record.getDate()), today);
            if (daysAgo < 0 || daysAgo >= ACTIVITY_WINDOW_DAYS) {
                continue;
            }
            attendanceWeeks.computeIfAbsent(record.getCustomerId(), id -> new HashSet<>())
                    .add((int) (daysAgo / 7));
        }

        List<Object[]> rows = new ArrayList<>();
        for (Customer customer : customers) {
            CustomerStatus rawStatus = customer.getStatus() == null
                    ? CustomerStatus.ACTIVE : customer.getStatus();
            Membership membership = activeMembership.get(customer.getId());
            boolean expiring = membership != null && membership.getEndDate() != null
                    && isExpiringSoon(LocalDate.parse(membership.getEndDate()), today);
            String displayStatus;
            if (rawStatus == CustomerStatus.SUSPENDED) {
                displayStatus = "Suspended";
            } else if (rawStatus == CustomerStatus.INACTIVE) {
                displayStatus = "Inactive";
            } else {
                displayStatus = expiring ? "Expiring" : "Active";
            }
            String trainerName = customer.getTrainerId() == null ? "Unassigned"
                    : trainerNames.getOrDefault(customer.getTrainerId(),
                            "Coach #" + customer.getTrainerId());
            Set<Integer> weeks = attendanceWeeks.get(customer.getId());
            int activityPct = weeks == null ? 0
                    : Math.min(100, weeks.size() * 100 / (ACTIVITY_WINDOW_DAYS / 7));

            rows.add(new Object[] {
                    "M-" + String.format("%04d", customer.getId()),
                    customer.getFullName(),
                    customer.getPhone(),
                    customer.getEmail() == null ? "" : customer.getEmail(),
                    displayStatus,
                    trainerName,
                    activityPct,
                    customer.getJoinDate() == null ? "" : customer.getJoinDate(),
                    rawStatus,
                    customer.getId()});
        }
        return rows;
    }

    /** True when the membership end date falls within the expiring horizon from today. */
    private static boolean isExpiringSoon(LocalDate endDate, LocalDate today) {
        return !endDate.isBefore(today) && !endDate.isAfter(today.plusDays(EXPIRING_DAYS));
    }

    /** Recomputes the KPI strip from the loaded roster so it always agrees with the table. */
    private void applyKpis() {
        int active = 0;
        int suspended = 0;
        int expiring = 0;
        int newThisMonth = 0;
        LocalDate today = LocalDate.now();
        String monthPrefix = today.getYear() + "-"
                + String.format("%02d", today.getMonthValue());
        for (Object[] row : roster) {
            CustomerStatus status = (CustomerStatus) row[COL_RAW_STATUS];
            if (status == CustomerStatus.ACTIVE) {
                active++;
                if ("Expiring".equals(row[COL_STATUS])) {
                    expiring++;
                }
            } else if (status == CustomerStatus.SUSPENDED) {
                suspended++;
            }
            if (String.valueOf(row[COL_JOINED]).startsWith(monthPrefix)) {
                newThisMonth++;
            }
        }
        cardTotalActive.setValue(String.valueOf(active));
        cardTotalActive.setDeltaText("of " + roster.size() + " members on roster");
        cardExpiringSoon.setValue(String.valueOf(expiring));
        cardSuspended.setValue(String.valueOf(suspended));
        cardNewThisMonth.setValue(String.valueOf(newThisMonth));
        cardNewThisMonth.setDeltaText("Joined "
                + today.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
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

    /** Search suggestions: athlete names from the loaded roster for quick jump-in. */
    private void fillSearchSuggestions() {
        searchBar.getSuggestions().clear();
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

    /**
     * Rule 4 delete for the selected athlete: confirms first (the destructive-action
     * contract), then flips the customer's status to INACTIVE through {@code CustomerDao} on
     * a background task — no record is hard-deleted — and offers Undo restoring the previous
     * status. Any DAO {@code ValidationException} surfaces verbatim in a snackbar.
     */
    private void deleteRow(Object[] row) {
        if (row == null) {
            overlay.showSnackbar("Select an athlete row first.");
            return;
        }
        String name = String.valueOf(row[COL_NAME]);
        String memberId = String.valueOf(row[COL_ID]);
        CustomerStatus previousStatus = (CustomerStatus) row[COL_RAW_STATUS];
        int customerId = (Integer) row[COL_CUSTOMER_ID];
        UiKit.confirmDialog(overlay, "Delete " + name + "?",
                "The member is marked Inactive (rule 4) — nothing is hard-deleted, and Undo "
                        + "stays available right after.",
                "Delete", "Keep Member",
                () -> Data.submit(
                        () -> {
                            CustomerDao customerDao = new CustomerDao();
                            Customer customer = customerDao.findById(customerId);
                            if (customer != null) {
                                customer.setStatus(CustomerStatus.INACTIVE);
                                customerDao.update(customer);
                            }
                            return customer;
                        },
                        updated -> {
                            if (selectedRow == row) {
                                hideSideSheet();
                            }
                            loadRoster();
                            overlay.showSnackbar(name + " (" + memberId + ") marked Inactive.",
                                    "Undo", () -> restoreStatus(customerId, name, previousStatus));
                        },
                        error -> overlay.showSnackbar(
                                "Could not delete member: " + error.getMessage())));
    }

    /** Undo for a rule-4 delete: restores the member's previous status through the DAO. */
    private void restoreStatus(int customerId, String name, CustomerStatus previousStatus) {
        Data.submit(
                () -> {
                    CustomerDao customerDao = new CustomerDao();
                    Customer customer = customerDao.findById(customerId);
                    if (customer != null) {
                        customer.setStatus(previousStatus);
                        customerDao.update(customer);
                    }
                    return customer;
                },
                restored -> {
                    loadRoster();
                    overlay.showSnackbar(name + " restored to the roster.");
                },
                error -> overlay.showSnackbar("Could not undo: " + error.getMessage()));
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
