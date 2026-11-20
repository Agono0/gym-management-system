package com.gym.ui;

import com.gym.dao.AdministratorDao;
import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Administrator;
import com.gym.model.StaffRole;
import com.gym.util.DatabaseConnection;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Data bootstrap and async plumbing for the screens — the bridge between the JavaFX
 * Application Thread and the blocking SQLite DAOs (DECISIONS.md 77: controllers call DAOs
 * directly; there is no service layer).
 *
 * <p>Thread discipline (the reason this class exists): every DAO call opens a JDBC
 * connection and blocks on SQLite I/O, so no DAO call may ever run on the FX thread — it
 * would freeze layout and trip the app's freeze watchdog. {@link #ensureReady()} and the
 * {@code work} suppliers given to {@link #loadAsync}/{@link #submit} run on
 * {@link javafx.concurrent.Task} background threads and may touch the database and models
 * freely; the success/error callbacks run on the FX thread and are the only place allowed
 * to touch the scene graph.
 *
 * <p>Layer contract (docs/SPEC_NOTES/app-ui-foundation.md): no SQL here — SQL lives in
 * {@code com.gym.dao}; this class only moves calls between threads and surfaces the DAOs'
 * {@code ValidationException}/{@code DatabaseException} to screen callbacks.
 */
public final class Data {

    /** Supplier variant that may throw the DAOs' checked exceptions (work side). */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /** Property key on the content host: generation counter of the newest issued load. */
    private static final String LOAD_GENERATION_KEY = "com.gym.ui.Data.loadGeneration";

    private Data() {
    }

    /**
     * First-run bootstrap: creates the 13-table SQLite schema
     * ({@link DatabaseConnection#initializeDatabase()}) and seeds a realistic demo dataset
     * through the DAOs when the database is still empty ({@link DemoSeeder#seedIfEmpty()}).
     *
     * <p>Must be called off the FX thread (it opens JDBC connections). The
     * {@code synchronized} guard plus the seeders' emptiness checks make repeated calls
     * cheap and idempotent, so every screen's load task may start with it.
     */
    public static synchronized void ensureReady() throws DatabaseException, ValidationException {
        DatabaseConnection.initializeDatabase();
        DemoSeeder.seedIfEmpty();
    }

    /**
     * The standard screen-load pattern: swaps {@code contentHost} to
     * {@link UiKit#loadingState()}, runs {@code work} on a background {@link Task}, restores
     * the original content and applies the result on the FX thread, and routes any exception
     * (typically {@code ValidationException}/{@code DatabaseException}) to {@code onError}
     * on the FX thread.
     *
     * <p>Contract details: {@code contentHost} must be called on the FX thread (enforced);
     * the current children of {@code contentHost} are saved and restored before either
     * callback runs, so the screen never loses its node tree; a per-host generation counter
     * makes a stale task (screen re-loaded before it finished) a silent no-op; the callbacks
     * own the final state — on failure the usual answer is
     * {@code UiKit.errorState(cause, retry)} with {@code retry} re-running the same load.
     */
    public static <T> void loadAsync(Pane contentHost, ThrowingSupplier<T> work,
                                     Consumer<T> onSuccess, Consumer<Exception> onError) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "Data.loadAsync must be called on the JavaFX Application Thread");
        }
        int generation = nextGeneration(contentHost);
        ObservableList<Node> children = contentHost.getChildren();
        List<Node> saved = new ArrayList<>(children);
        children.setAll(UiKit.loadingState());

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.get();
            }
        };
        task.setOnSucceeded(event -> {
            if (isStale(contentHost, generation)) {
                return;
            }
            children.setAll(saved);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            if (isStale(contentHost, generation)) {
                return;
            }
            children.setAll(saved);
            Throwable failure = task.getException();
            onError.accept(failure instanceof Exception exception
                    ? exception : new IllegalStateException(failure));
        });
        Thread thread = new Thread(task, "gym-data-load");
        // Non-daemon on purpose: Platform.exit() must wait for in-flight database work (e.g. a
        // first-run seeding inside this task) instead of killing it mid-write and leaving a
        // half-seeded SQLite file behind.
        thread.start();
    }

    /**
     * The write-path counterpart of {@link #loadAsync} for DAO inserts/updates that must not
     * blank a content region: runs {@code work} on a background {@link Task} and delivers
     * the result or the exception to the callbacks on the FX thread. Must be called on the
     * FX thread; the callbacks own the UI answer (usually a snackbar or dialog).
     */
    public static <T> void submit(ThrowingSupplier<T> work, Consumer<T> onSuccess,
                                  Consumer<Exception> onError) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "Data.submit must be called on the JavaFX Application Thread");
        }
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.get();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            onError.accept(failure instanceof Exception exception
                    ? exception : new IllegalStateException(failure));
        });
        Thread thread = new Thread(task, "gym-data-write");
        // Non-daemon for the same reason as loadAsync: writes must never die mid-transaction.
        thread.start();
    }

    /**
     * Outcome of one credential check against the seeded administrators (rule 16). {@code kind}
     * lets the caller give each failure its own copy; {@code fullName}/{@code role} are the real
     * signed-in identity when {@code kind} is {@link Status#OK}.
     */
    public record AuthResult(Status kind, String fullName, StaffRole role, String error) {

        /** Why a sign-in attempt failed (or did not). */
        public enum Status { OK, INVALID_CREDENTIALS, INACTIVE }
    }

    /**
     * Credential check for the login gate (rule 16: inactive accounts cannot log in).
     * Mirrors the CLI's {@code GymRulesHelper.authenticate} semantics — username lookup via
     * {@link AdministratorDao#findByUsername}, the stored password compared to the entered one,
     * then the active-flag guard — but reports the three outcomes distinctly so the UI can give
     * each failure its own copy. Headless-testable: no JavaFX, synchronous, caller runs it off
     * the FX thread (the gate uses {@link #submit}).
     *
     * @return {@link AuthResult} with {@code OK} (carrying the real identity) or the failure
     *         kind plus short human-facing copy
     * @throws DatabaseException when the bootstrap or the lookup itself fails
     */
    public static AuthResult authenticate(String username, String password)
            throws DatabaseException, ValidationException {
        ensureReady();
        String trimmed = username == null ? "" : username.trim();
        if (trimmed.isEmpty() || password == null || password.isEmpty()) {
            return new AuthResult(AuthResult.Status.INVALID_CREDENTIALS, null, null,
                    "Enter your username and password to continue.");
        }
        Administrator admin = new AdministratorDao().findByUsername(trimmed);
        if (admin == null || admin.getPasswordHash() == null
                || !admin.getPasswordHash().equals(password)) {
            return new AuthResult(AuthResult.Status.INVALID_CREDENTIALS, null, null,
                    "Wrong username or password.");
        }
        if (!admin.isActive()) {
            return new AuthResult(AuthResult.Status.INACTIVE, null, admin.getRole(),
                    "This account is inactive, so it cannot sign in (rule 16).");
        }
        return new AuthResult(AuthResult.Status.OK, admin.getFullName(), admin.getRole(), null);
    }

    /** Issues the next load generation for the host and returns it (FX thread only). */
    private static int nextGeneration(Pane contentHost) {
        Object current = contentHost.getProperties().get(LOAD_GENERATION_KEY);
        int generation = current instanceof Integer integer ? integer : 0;
        generation++;
        contentHost.getProperties().put(LOAD_GENERATION_KEY, generation);
        return generation;
    }

    /** True when a newer load has been issued for the host, making this callback stale. */
    private static boolean isStale(Pane contentHost, int generation) {
        Object current = contentHost.getProperties().get(LOAD_GENERATION_KEY);
        return !(current instanceof Integer integer) || integer != generation;
    }
}
