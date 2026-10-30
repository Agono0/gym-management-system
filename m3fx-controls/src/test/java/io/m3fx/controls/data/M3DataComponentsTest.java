package io.m3fx.controls.data;

import static org.junit.jupiter.api.Assertions.*;

import io.m3fx.controls.data.M3Timeline.Status;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.search.M3SearchBar;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for Phase 4 Advanced Enterprise & Telemetry Components.
 */
class M3DataComponentsTest {

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX failed to initialize");
    }

    private static void runOnFx(Runnable action) throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    @Test
    void statCardPropertiesAndSparkline() throws Throwable {
        runOnFx(() -> {
            M3StatCard card = new M3StatCard("Monthly Revenue", "$48,250", "+14.5%");
            assertEquals("Monthly Revenue", card.getTitle());
            assertEquals("$48,250", card.getValue());
            assertEquals("+14.5%", card.getDeltaText());
            assertTrue(card.isDeltaPositive());

            card.setDeltaPositive(false);
            assertFalse(card.isDeltaPositive());

            assertDoesNotThrow(() -> card.setSparkline(10, 20, 15, 30, 42));
        });
    }

    @Test
    void dataTableColumnsAndPagination() throws Throwable {
        record Member(String name, String tier, String status) {}

        runOnFx(() -> {
            M3DataTable<Member> table = new M3DataTable<>();
            table.getColumns().add(new M3DataTable.Column<>("Name", Member::name, 140));
            table.getColumns().add(new M3DataTable.Column<>("Tier", Member::tier, 100));
            table.getColumns().add(new M3DataTable.Column<>("Status", Member::status, 80));

            table.setPageSize(2);

            table.getItems().addAll(
                    new Member("Alice", "VIP", "Active"),
                    new Member("Bob", "Standard", "Active"),
                    new Member("Charlie", "Pro", "Pending")
            );

            assertEquals(0, table.getCurrentPage());
            table.nextPage();
            assertEquals(1, table.getCurrentPage());
            table.prevPage();
            assertEquals(0, table.getCurrentPage());
        });
    }

    @Test
    void dataTableHeaderClickSortsRows() throws Throwable {
        record Member(String name, String tier) {}

        runOnFx(() -> {
            M3DataTable<Member> table = new M3DataTable<>();
            table.getColumns().add(new M3DataTable.Column<>("Name", Member::name, 140));
            table.getItems().addAll(
                    new Member("Cara", "Pro"),
                    new Member("Alice", "VIP"),
                    new Member("Bob", "Standard")
            );

            javafx.scene.layout.HBox headerRow = (javafx.scene.layout.HBox) table.getChildren().get(1);
            javafx.scene.layout.VBox rows = (javafx.scene.layout.VBox) table.getChildren().get(2);

            // Click the "Name" header -> ascending
            javafx.scene.control.Label header = (javafx.scene.control.Label) headerRow.getChildren().get(0);
            header.fireEvent(mouseClick());
            assertTrue(header.getText().contains("\u25B2"), "Ascending sort arrow shown");
            assertEquals("Alice", firstCellText(rows));
            assertEquals("Cara", cellText(rows, 2));

            // Second click -> descending
            header.fireEvent(mouseClick());
            assertTrue(header.getText().contains("\u25BC"), "Descending sort arrow shown");
            assertEquals("Cara", firstCellText(rows));

            // Third click -> sort cleared, back to insertion order
            header.fireEvent(mouseClick());
            assertEquals("Name", header.getText());
            assertEquals("Cara", firstCellText(rows));
        });
    }

    @Test
    void dataTableZebraStripingAlternatesWithinPage() throws Throwable {
        record Member(String name) {}

        runOnFx(() -> {
            M3DataTable<Member> table = new M3DataTable<>();
            table.getColumns().add(new M3DataTable.Column<>("Name", Member::name, 140));
            table.setPageSize(5);
            for (int i = 1; i <= 6; i++) {
                table.getItems().add(new Member("M" + i));
            }

            javafx.scene.layout.VBox rows = (javafx.scene.layout.VBox) table.getChildren().get(2);
            // Page 1: rows at positions 0..4 -> second and fourth rows striped
            // (StyleClassList prints space-separated, so assert membership instead of toString)
            assertTrue(rows.getChildren().get(1).getStyleClass().contains("alt"),
                    "Second visible row must be striped");
            assertTrue(rows.getChildren().get(3).getStyleClass().contains("alt"),
                    "Fourth visible row must be striped");
            assertFalse(rows.getChildren().get(0).getStyleClass().contains("alt"));

            // Page 2 (single row): striping restarts at unstriped regardless of absolute index
            table.nextPage();
            rows = (javafx.scene.layout.VBox) table.getChildren().get(2);
            assertFalse(rows.getChildren().get(0).getStyleClass().contains("alt"));
        });
    }

    private static javafx.scene.input.MouseEvent mouseClick() {
        return new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, false, true, false, false, false, false, null);
    }

    private static String firstCellText(javafx.scene.layout.VBox rows) {
        return cellText(rows, 0);
    }

    private static String cellText(javafx.scene.layout.VBox rows, int row) {
        javafx.scene.layout.HBox rowBox = (javafx.scene.layout.HBox) rows.getChildren().get(row);
        javafx.scene.control.Label cell = (javafx.scene.control.Label) rowBox.getChildren().get(0);
        return cell.getText();
    }

    @Test
    void timelineItemsAndRebuild() throws Throwable {
        runOnFx(() -> {
            M3Timeline timeline = new M3Timeline();
            timeline.addItem("09:00 AM", "Gym Opened", "Staff checked equipment", Status.COMPLETED);
            timeline.addItem("10:30 AM", "HIIT Training Class", "Coach Sarah - Studio A", Status.ACTIVE);
            timeline.addItem("12:00 PM", "Spa Maintenance", "Scheduled sanitation", Status.PENDING);

            assertEquals(3, timeline.getItems().size());
            assertEquals(3, timeline.getChildren().size());

            // The connector below a completed entry is colored as completed
            javafx.scene.layout.HBox firstRow = (javafx.scene.layout.HBox) timeline.getChildren().get(0);
            javafx.scene.layout.VBox nodeCol = (javafx.scene.layout.VBox) firstRow.getChildren().get(0);
            javafx.scene.layout.Region line = (javafx.scene.layout.Region) nodeCol.getChildren().get(1);
            assertTrue(line.getStyleClass().contains("completed"));
        });
    }

    @Test
    void stepperStepNavigation() throws Throwable {
        runOnFx(() -> {
            M3Stepper stepper = new M3Stepper("Profile", "Plan", "Payment", "Confirm");
            assertEquals(4, stepper.getStepTitles().size());
            assertEquals(0, stepper.getCurrentStep());

            stepper.nextStep();
            assertEquals(1, stepper.getCurrentStep());

            stepper.nextStep();
            stepper.nextStep();
            assertEquals(3, stepper.getCurrentStep());

            // Check upper bound clamp
            stepper.nextStep();
            assertEquals(3, stepper.getCurrentStep());

            stepper.prevStep();
            assertEquals(2, stepper.getCurrentStep());
        });
    }

    @Test
    void stepperErrorStateRendersAndRebuilds() throws Throwable {
        runOnFx(() -> {
            M3Stepper stepper = new M3Stepper("Profile", "Plan", "Payment", "Confirm");
            stepper.setCurrentStep(2);

            // 4 steps + 3 connector bands
            assertEquals(7, stepper.getChildren().size());

            stepper.getErrorSteps().add(1);
            assertEquals(7, stepper.getChildren().size()); // same structure, rebuilt

            // Children alternate [stepBox, connectorBand, stepBox, ...]; step 1 is index 2.
            // Errored step: the styled disc is the inner Circle (fx-fill); clickability is on the wrapper.
            javafx.scene.layout.VBox stepBox = (javafx.scene.layout.VBox) stepper.getChildren().get(2);
            javafx.scene.layout.StackPane circle = (javafx.scene.layout.StackPane) stepBox.getChildren().get(0);
            javafx.scene.shape.Circle disc = (javafx.scene.shape.Circle) circle.getChildren().get(0);
            assertTrue(disc.getStyleClass().contains("m3-stepper-circle-error"));
            assertTrue(circle.getStyleClass().contains("m3-stepper-clickable"));

            // Connector after the errored step (index 3) is not "completed"-colored
            javafx.scene.layout.StackPane band = (javafx.scene.layout.StackPane) stepper.getChildren().get(3);
            javafx.scene.layout.Region line = (javafx.scene.layout.Region) band.getChildren().get(0);
            assertFalse(line.getStyleClass().contains("completed"));
        });
    }

    @Test
    void speedDialActionsAndExpand() throws Throwable {
        runOnFx(() -> {
            M3SpeedDial dial = new M3SpeedDial();
            assertFalse(dial.isExpanded());

            AtomicBoolean actionRun = new AtomicBoolean(false);
            dial.addAction("Add Member", M3Icon.symbol("person"), () -> actionRun.set(true));
            dial.addAction("Add Workout", M3Icon.symbol("star"), () -> {});

            dial.setExpanded(true);
            assertTrue(dial.isExpanded());

            dial.setExpanded(false);
            assertFalse(dial.isExpanded());
        });
    }

    // =========================================================================
    // UX polish: empty states, pagination clamping, row actions, header sort a11y
    // =========================================================================

    @Test
    void dataTableEmptyStateShowsQuietPlaceholderAndClearsWhenItemsArrive() throws Throwable {
        runOnFx(() -> {
            M3DataTable<String> table = new M3DataTable<>();
            table.getColumns().add(new M3DataTable.Column<>("Name", String::toString, 140));

            VBox rows = (VBox) table.getChildren().get(2);
            assertEquals(1, rows.getChildren().size(), "Empty table shows one placeholder row");
            Node empty = rows.getChildren().get(0);
            assertTrue(empty.getStyleClass().contains("m3-data-table-empty"),
                    "Placeholder carries the m3-data-table-empty style class");
            assertEquals("No items", ((Label) empty).getText());

            table.getItems().addAll("Alice", "Bob");

            rows = (VBox) table.getChildren().get(2);
            assertEquals(2, rows.getChildren().size(), "Placeholder is replaced by real rows");
            assertEquals("Alice", cellText(rows, 0));
        });
    }

    @Test
    void dataTableFilteredEmptyStateSaysNoMatchingItems() throws Throwable {
        runOnFx(() -> {
            M3DataTable<String> table = new M3DataTable<>();
            table.getColumns().add(new M3DataTable.Column<>("Name", String::toString, 140));
            table.getItems().addAll("Alice", "Bob");

            M3SearchBar search = (M3SearchBar) table.getChildren().get(0);
            search.setQuery("zzz-no-match");

            VBox rows = (VBox) table.getChildren().get(2);
            assertEquals(1, rows.getChildren().size());
            assertEquals("No matching items", ((Label) rows.getChildren().get(0)).getText());

            search.setQuery("");
            rows = (VBox) table.getChildren().get(2);
            assertEquals(2, rows.getChildren().size(), "Rows return when the filter clears");
        });
    }

    @Test
    void dataTablePageSizeChangeClampsCurrentPage() throws Throwable {
        runOnFx(() -> {
            M3DataTable<String> table = new M3DataTable<>();
            table.getColumns().add(new M3DataTable.Column<>("Name", String::toString, 140));
            table.setPageSize(2);
            for (int i = 1; i <= 5; i++) {
                table.getItems().add("M" + i);
            }

            table.nextPage();
            table.nextPage();
            assertEquals(2, table.getCurrentPage(), "Last page of three reached");

            // Shrinking the page set (5 items / 4 per page = 2 pages) must pull the page back
            // in range instead of rendering a stale out-of-range page.
            table.setPageSize(4);
            assertEquals(1, table.getCurrentPage(), "currentPage clamped to the new last page");

            table.setPageSize(10);
            assertEquals(0, table.getCurrentPage(), "Single-page table clamps to page 0");
        });
    }

    @Test
    void dataTableRowActionActivatesByMouseAndKeyboard() throws Throwable {
        runOnFx(() -> {
            M3DataTable<String> table = new M3DataTable<>();
            table.getColumns().add(new M3DataTable.Column<>("Name", String::toString, 140));
            table.getItems().addAll("Alice", "Bob");

            // Without a handler rows are presentational: default cursor, no Tab focus.
            VBox rows = (VBox) table.getChildren().get(2);
            HBox passiveRow = (HBox) rows.getChildren().get(0);
            assertEquals(Cursor.DEFAULT, passiveRow.getCursor(),
                    "No hand cursor without a row action");
            assertFalse(passiveRow.isFocusTraversable());

            AtomicReference<String> activated = new AtomicReference<>();
            table.setOnRowAction(activated::set);

            rows = (VBox) table.getChildren().get(2);
            HBox secondRow = (HBox) rows.getChildren().get(1);
            assertEquals(Cursor.HAND, secondRow.getCursor(), "Hand cursor signals interactivity");
            assertTrue(secondRow.isFocusTraversable(), "Row is Tab-reachable");
            assertEquals("Bob", secondRow.getAccessibleText(),
                    "Row exposes its leading value to screen readers");

            secondRow.fireEvent(keyPress(KeyCode.ENTER));
            assertEquals("Bob", activated.get(), "Enter activates the focused row");

            secondRow.fireEvent(keyPress(KeyCode.SPACE));
            assertEquals("Bob", activated.get(), "Space activates the focused row");
        });
    }

    @Test
    void dataTableHeaderSortIsKeyboardOperableAndAnnouncesDirection() throws Throwable {
        runOnFx(() -> {
            M3DataTable<String> table = new M3DataTable<>();
            table.getColumns().add(new M3DataTable.Column<>("Name", String::toString, 140));
            table.getItems().addAll("Cara", "Alice", "Bob");

            HBox headerRow = (HBox) table.getChildren().get(1);
            Label header = (Label) headerRow.getChildren().get(0);
            assertTrue(header.isFocusTraversable(), "Sortable header is Tab-reachable");

            header.fireEvent(keyPress(KeyCode.ENTER));
            assertTrue(header.getText().contains("\u25B2"), "Ascending arrow after Enter");
            assertEquals("Sorted ascending by Name", header.getAccessibleText());

            header.fireEvent(keyPress(KeyCode.ENTER));
            assertTrue(header.getText().contains("\u25BC"), "Descending arrow after second Enter");
            assertEquals("Sorted descending by Name", header.getAccessibleText());

            header.fireEvent(keyPress(KeyCode.ENTER));
            assertEquals("Name", header.getText(), "Third Enter clears the sort");
            assertEquals("Sort by Name", header.getAccessibleText());
        });
    }

    @Test
    void stepperOnlyClickableStepsAdvertiseTheButtonRole() throws Throwable {
        runOnFx(() -> {
            M3Stepper stepper = new M3Stepper("Profile", "Plan", "Payment", "Confirm");
            stepper.setCurrentStep(2); // steps 0,1 completed (clickable); 2 active; 3 pending

            // Children alternate [stepBox, connectorBand, stepBox, ...]
            javafx.scene.layout.StackPane circle0 =
                    circleOf((VBox) stepper.getChildren().get(0));
            javafx.scene.layout.StackPane circle2 =
                    circleOf((VBox) stepper.getChildren().get(4));
            javafx.scene.layout.StackPane circle3 =
                    circleOf((VBox) stepper.getChildren().get(6));

            assertEquals(javafx.scene.AccessibleRole.BUTTON, circle0.getAccessibleRole(),
                    "Completed step is a button");
            assertTrue(circle0.getAccessibleText().contains(", completed"));

            assertNotEquals(javafx.scene.AccessibleRole.BUTTON, circle2.getAccessibleRole(),
                    "Active step is not a fake button");
            assertTrue(circle2.getAccessibleText().contains(", current"));

            assertNotEquals(javafx.scene.AccessibleRole.BUTTON, circle3.getAccessibleRole(),
                    "Pending step is not a fake button");
            assertTrue(circle3.getAccessibleText().startsWith("Step 4: Confirm"));
        });
    }

    @Test
    void speedDialDisabledActionItemCannotActivate() throws Throwable {
        runOnFx(() -> {
            M3SpeedDial dial = new M3SpeedDial();
            AtomicBoolean firstRun = new AtomicBoolean(false);
            AtomicBoolean secondRun = new AtomicBoolean(false);
            dial.addAction("Add Member", M3Icon.symbol("person"), () -> firstRun.set(true));
            dial.addAction("Add Workout", M3Icon.symbol("star"), () -> secondRun.set(true));

            // dial children: [dialPane, triggerButton]; dial items live in the pane
            javafx.scene.layout.Pane dialPane = (javafx.scene.layout.Pane) dial.getChildren().get(0);
            javafx.scene.Node firstItem = dialPane.getChildren().get(0);
            assertFalse(firstItem.isDisabled(), "Actions start enabled");

            dial.setActionEnabled(0, false);
            assertTrue(firstItem.isDisabled(), "Disabled action renders a disabled button");
            assertFalse(dial.isExpanded());

            dial.setActionEnabled(1, false);
            dial.setActionEnabled(1, true);
            assertFalse(dialPane.getChildren().get(1).isDisabled(),
                    "Re-enabled action is clickable again");
        });
    }

    private static javafx.scene.layout.StackPane circleOf(VBox stepBox) {
        return (javafx.scene.layout.StackPane) stepBox.getChildren().get(0);
    }

    private static KeyEvent keyPress(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, KeyEvent.CHAR_UNDEFINED, "", code,
                false, false, false, false);
    }
}

