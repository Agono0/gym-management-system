package com.gym.fxml;

import com.gym.controller.AppShellController;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.textfield.M3TextField;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Interactive smoke tests for the app flows being wired in this same round:
 * <ol>
 *   <li>trainer-list Open action navigates to trainer-profile and hands over the chosen coach;</li>
 *   <li>customer-list Open Full Profile navigates to customer-profile;</li>
 *   <li>dashboard End Shift leads back to the login gate;</li>
 *   <li>a payment action and an attendance action each produce a visible snackbar/row answer.</li>
 * </ol>
 *
 * <p>These tests verify the WIRING contract of the controller changes landing in parallel with
 * them: where a lookup target or behavior is not implemented yet, the assertions below document
 * the contract the wiring must satisfy, and they fail by design until the wiring lands.</p>
 *
 * <p>Pattern: fire the real rail buttons / row handlers / onAction targets on the FX thread and
 * assert the resulting shell state, exactly like {@link GymAppScreenViewerTest} does. Tests run
 * with {@code GymApp.SHOW_LAUNCH_GATES == false}, so "the login gate" is the in-shell
 * login.fxml Authentication Vault screen (the same target {@code AppShellController.onLogout}
 * uses when gates are disabled).</p>
 */
public class WiredFlowSmokeTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Platform already initialized
        }
        Platform.setImplicitExit(false);
    }

    // ------------------------------------------------------------------
    // (a) Trainer list open action -> trainer profile shows the chosen coach
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Trainer list Open action navigates to trainer-profile with the chosen coach")
    void trainerListOpenActionNavigatesToProfileWithChosenCoach() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Shell shell = loadShell();
                shell.controller().onNavTrainers();
                shell.root().applyCss();
                shell.root().layout();

                M3DataTable<String> roster = lookup(shell.root(), "#tableTrainers");
                assertNotNull(roster, "trainer roster #tableTrainers must exist in the shell");
                assertFalse(roster.getItems().isEmpty(), "roster must show the demo rows");

                // trainer-profile.fxml ships a static placeholder coach ("Elena Rostova, ...");
                // pick a roster row whose name differs from it so the shown-coach assertion
                // cannot pass off the FXML default. Degenerate rosters fall back to row 0.
                String row = null;
                String chosenName = null;
                for (String candidate : roster.getItems()) {
                    String candidateName = candidate.split("\\|")[1];
                    if (!"Elena Rostova".equals(candidateName)) {
                        row = candidate;
                        chosenName = candidateName;
                        break;
                    }
                }
                if (row == null) {
                    row = roster.getItems().get(0);
                    chosenName = row.split("\\|")[1];
                }
                int chosenId = Integer.parseInt(row.split("\\|")[0]);

                // Row activation is the entry point shared by the row click and the per-row
                // Open button (TrainerListController.wireRosterColumns).
                assertNotNull(roster.getOnRowAction(), "roster rows must have an open action");
                roster.getOnRowAction().accept(row);

                // WIRING CONTRACT: the open action must navigate the shell to trainer-profile.fxml
                // and hand the chosen coach over as a TrainerSnapshot —
                // TrainerProfileController.showTrainer fills #lblCoachName with the coach's name.
                assertNotNull(shell.root().lookup("#lblCoachName"),
                        "shell must navigate to trainer-profile.fxml (coach name label present)");
                assertNotNull(shell.root().lookup("#gaugeCapacity"),
                        "trainer-profile capacity gauge must be part of the shown screen");
                assertNull(shell.root().lookup("#tableTrainers"),
                        "trainer list must no longer be the visible screen");
                Label coachLabel = (Label) shell.root().lookup("#lblCoachName");
                assertEquals(chosenName, coachLabel.getText(),
                        "the trainer profile must show the chosen coach (" + chosenId + " / "
                                + chosenName + "), not the FXML placeholder");
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out executing on the JavaFX thread");
        if (err.get() != null) {
            throw new AssertionError("Trainer open-to-profile flow failed", err.get());
        }
    }

    // ------------------------------------------------------------------
    // (b) Customer list open-full-profile -> customer-profile screen
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Customer list Open Full Profile navigates to customer-profile")
    void customerListOpenFullProfileNavigatesToCustomerProfile() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Shell shell = loadShell();
                shell.controller().onNavCustomers();
                shell.root().applyCss();
                shell.root().layout();

                M3DataTable<Object[]> table = lookup(shell.root(), "#customerTable");
                assertNotNull(table, "roster table #customerTable must exist in the shell");
                assertFalse(table.getItems().isEmpty(), "roster table must show demo rows");

                // Activate the first row: peek selects the athlete in the quick-peek side sheet.
                assertNotNull(table.getOnRowAction(), "roster rows must have a row action");
                table.getOnRowAction().accept(table.getItems().get(0));

                Node openFullProfile = shell.root().lookup("#btnOpenFullProfile");
                assertNotNull(openFullProfile, "#btnOpenFullProfile must exist in the side sheet");
                openFullProfile.fireEvent(new ActionEvent());

                // WIRING CONTRACT: Open Full Profile must navigate the shell to customer-profile.fxml.
                assertNotNull(shell.root().lookup("#holographicPass"),
                        "shell must navigate to customer-profile.fxml (holographic pass present)");
                assertNull(shell.root().lookup("#customerTable"),
                        "customer list must no longer be the visible screen");
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out executing on the JavaFX thread");
        if (err.get() != null) {
            throw new AssertionError("Customer open-full-profile flow failed", err.get());
        }
    }

    // ------------------------------------------------------------------
    // (c) Dashboard End Shift -> login gate
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Dashboard End Shift confirm leads back to the login gate")
    void dashboardEndShiftLeadsBackToLoginGate() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Shell shell = loadShell(); // initialize() loads dashboard.fxml by default
                Node logoutButton = shell.root().lookup("#logoutButton");
                assertNotNull(logoutButton, "dashboard #logoutButton must exist");
                logoutButton.fireEvent(new ActionEvent());

                // The destructive action must confirm through a dialog first.
                M3Button confirm = findButtonByText(shell.root(), "End Shift");
                assertNotNull(confirm, "End Shift confirmation dialog must be showing");
                confirm.fireEvent(new ActionEvent());

                // WIRING CONTRACT: confirming End Shift hands the operator session back to the
                // login gate (with gates disabled in tests: the in-shell login.fxml screen).
                assertNotNull(shell.root().lookup("#authCard"),
                        "shell must return to the login gate (login.fxml auth card present)");
                assertNull(shell.root().lookup("#logoutButton"),
                        "dashboard must no longer be the visible screen");
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out executing on the JavaFX thread");
        if (err.get() != null) {
            throw new AssertionError("Dashboard end-shift flow failed", err.get());
        }
    }

    // ------------------------------------------------------------------
    // (d) Payment / attendance actions produce a visible snackbar/row answer
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Payment Record Payment action produces a visible snackbar/row answer")
    void paymentRecordActionProducesVisibleAnswer() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Shell shell = loadShell();
                shell.controller().onNavPayments();
                shell.root().applyCss();
                shell.root().layout();

                M3DataTable<String> register = lookup(shell.root(), "#paymentDataTable");
                assertNotNull(register, "transactions register #paymentDataTable must exist");
                int rowsBefore = register.getItems().size();

                M3TextField amountField = (M3TextField) shell.root().lookup("#txtPaymentAmount");
                assertNotNull(amountField, "#txtPaymentAmount must exist");
                amountField.setText("20.00");

                Node recordPayment = shell.root().lookup("#actionSplitButton");
                assertNotNull(recordPayment, "RECORD PAYMENT action #actionSplitButton must exist");
                recordPayment.fireEvent(new ActionEvent());

                // WIRING CONTRACT: the record action must answer visibly — either the
                // transactions register gains a row for the payment or a snackbar confirms it.
                assertTrue(register.getItems().size() > rowsBefore || snackbarVisibleIn(shell.root()),
                        "Record Payment must produce a visible answer: the register row count must grow "
                                + "(was " + rowsBefore + ") or a snackbar must show");
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out executing on the JavaFX thread");
        if (err.get() != null) {
            throw new AssertionError("Payment record flow failed", err.get());
        }
    }

    @Test
    @DisplayName("Attendance manual Check-In action produces a visible snackbar/row answer")
    void attendanceManualCheckInProducesVisibleAnswer() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Shell shell = loadShell();
                shell.controller().onNavAttendance();
                shell.root().applyCss();
                shell.root().layout();

                M3DataTable<String> stream = lookup(shell.root(), "#attendanceDataTable");
                assertNotNull(stream, "check-in stream #attendanceDataTable must exist");
                int rowsBefore = stream.getItems().size();

                M3TextField memberLookup = (M3TextField) shell.root().lookup("#txtMemberLookup");
                assertNotNull(memberLookup, "#txtMemberLookup must exist");
                memberLookup.setText("1042");

                Node checkIn = shell.root().lookup("#btnManualCheckIn");
                assertNotNull(checkIn, "#btnManualCheckIn must exist");
                checkIn.fireEvent(new ActionEvent());

                // WIRING CONTRACT: the manual check-in must answer visibly — either the check-in
                // stream gains a row for the member or a snackbar confirms the scan.
                assertTrue(stream.getItems().size() > rowsBefore || snackbarVisibleIn(shell.root()),
                        "Manual Check-In must produce a visible answer: the stream row count must grow "
                                + "(was " + rowsBefore + ") or a snackbar must show");
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out executing on the JavaFX thread");
        if (err.get() != null) {
            throw new AssertionError("Attendance manual check-in flow failed", err.get());
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Type-safe fx:id lookup. JavaFX 21 {@code Node.lookup} returns plain {@code Node}, so the
     * unchecked cast is unavoidable; it is a false positive here — the fx:id-to-type mapping is
     * fixed by the FXML contract this test file pins (see ControllerBindingContractTest).
     */
    @SuppressWarnings("unchecked")
    private static <N extends Node> N lookup(Parent root, String id) {
        return (N) root.lookup(id);
    }

    /** The loaded app shell: root node, controller and the #contentArea host pane. */
    private record Shell(Parent root, AppShellController controller) {}

    /** Loads app-shell.fxml into a scene (dashboard loads by default) and applies CSS/layout. */
    private Shell loadShell() throws Exception {
        URL shellUrl = getClass().getResource("/fxml/app-shell.fxml");
        assertNotNull(shellUrl, "Missing /fxml/app-shell.fxml");
        FXMLLoader loader = new FXMLLoader(shellUrl);
        Parent shellRoot = loader.load();
        AppShellController controller = loader.getController();
        assertNotNull(controller, "AppShellController must be injected");
        new Scene(shellRoot, 1280, 800);
        shellRoot.applyCss();
        shellRoot.layout();
        return new Shell(shellRoot, controller);
    }

    /** Depth-first search for an M3Button carrying the given text (e.g. a dialog confirm). */
    private static M3Button findButtonByText(Parent parent, String text) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof M3Button button && text.equals(button.getText())) {
                return button;
            }
            if (child instanceof Parent nested) {
                M3Button found = findButtonByText(nested, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** True when an M3 snackbar bar is present and visible under the given root. */
    private static boolean snackbarVisibleIn(Parent root) {
        Node bar = root.lookup(".m3-snackbar");
        return bar != null && bar.isVisible();
    }
}
