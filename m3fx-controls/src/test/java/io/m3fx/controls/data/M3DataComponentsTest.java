package io.m3fx.controls.data;

import static org.junit.jupiter.api.Assertions.*;

import io.m3fx.controls.data.M3Timeline.Status;
import io.m3fx.controls.icon.M3Icon;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
}

