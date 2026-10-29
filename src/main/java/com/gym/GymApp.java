package com.gym;

import io.m3fx.controls.M3Stylesheets;
import io.m3fx.core.theme.M3Theme;
import io.m3fx.core.theme.ThemeVariant;
import com.gym.controller.AppShellController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.URL;

/**
 * Dedicated application launcher for the complete Gym Management System (app-shell.fxml).
 * Provides the production 2026 Material Design 3 shell with live navigation across all 19 screens.
 */
public class GymApp extends Application {

    public static final Color SEED_PURPLE = Color.web("#6750A4");

    /**
     * Master switch for the Apple-style launch experience (splash screen → login gate →
     * fluid morph into the shell). {@link #main(String[])} sets it to {@code true} so a real
     * {@code gradlew runGym} gets the full choreography; every test calls {@link #start(Stage)}
     * directly with the flag left {@code false}, so the shell appears exactly as before and
     * no test contract (fx:id/@FXML counts, screen inspection) is affected.
     */
    public static boolean SHOW_LAUNCH_GATES = false;

    @Override
    public void start(Stage stage) throws Exception {
        // Register active M3 theme upfront so all custom controls resolve colors during FXML inflation
        M3Theme theme = M3Theme.fromSeed(SEED_PURPLE, ThemeVariant.TONAL_SPOT, true);
        M3Stylesheets.setLatestTheme(theme);

        URL url = getClass().getResource("/fxml/app-shell.fxml");
        if (url == null) {
            throw new IllegalStateException("Missing /fxml/app-shell.fxml resource");
        }
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();

        // Responsive desktop canvas (will cleanly fill the maximized work area without border clipping)
        Scene scene = new Scene(root, 1280, 800);
        M3Stylesheets.applyTo(scene, theme);

        stage.setTitle("GYM-OS // Enterprise Gym Management System (2026)");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);

        // Route window close through Platform.exit() so Application.stop() cleanup runs first;
        // stop() still force-exits the JVM afterwards so no thread can keep the process alive.
        stage.setOnCloseRequest(e -> Platform.exit());

        // Maximize BEFORE show so Windows calculates decorations and work area properly
        stage.setMaximized(true);
        stage.show();
        startFreezeWatchdog();

        // Launch gates (splash + login) only for the real run: start() is already on the FX
        // thread, and the gate controller defers its first overlay to a later pulse so the
        // freshly shown scene lays out first.
        if (SHOW_LAUNCH_GATES && loader.getController() instanceof AppShellController shellController) {
            shellController.beginLaunchGates();
        }
    }

    /**
     * Diagnostics only: samples the JavaFX Application Thread's CPU and, if it stays busy
     * (&gt;60% of a core) for four consecutive seconds, dumps the FX and QuantumRenderer stacks
     * to stderr so a UI freeze self-diagnoses in the console instead of hanging silently.
     * Read-only — never touches the scene graph.
     */
    private static void startFreezeWatchdog() {
        Thread watchdog = new Thread(() -> {
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            Thread fxThread = null;
            long lastCpu = 0, lastSample = 0, lastDump = 0;
            int busySeconds = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                if (fxThread == null) {
                    for (Thread t : Thread.getAllStackTraces().keySet()) {
                        if ("JavaFX Application Thread".equals(t.getName())) {
                            fxThread = t;
                        }
                    }
                    if (fxThread == null) {
                        continue;
                    }
                }
                long now = System.nanoTime();
                long cpu = mx.getThreadCpuTime(fxThread.getId());
                if (lastSample == 0) {
                    lastSample = now;
                    lastCpu = cpu;
                    continue;
                }
                double seconds = (now - lastSample) / 1e9;
                double coresUsed = seconds > 0 ? (cpu - lastCpu) / 1e9 / seconds : 0;
                lastSample = now;
                lastCpu = cpu;
                if (coresUsed > 0.6) {
                    busySeconds++;
                    if (busySeconds >= 4 && now - lastDump > 30_000_000_000L) {
                        lastDump = now;
                        System.err.println("=== FX-FREEZE WATCHDOG: FX thread busy at "
                                + String.format("%.0f%%", coresUsed * 100) + " for " + busySeconds
                                + "s — dumping stacks ===");
                        dumpThread(fxThread, "JavaFX Application Thread");
                        for (Thread t : Thread.getAllStackTraces().keySet()) {
                            if ("QuantumRenderer-0".equals(t.getName())) {
                                dumpThread(t, "QuantumRenderer-0");
                            }
                        }
                    }
                } else {
                    busySeconds = 0;
                }
            }
        }, "m3fx-freeze-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static void dumpThread(Thread t, String label) {
        StackTraceElement[] stack = t.getStackTrace();
        System.err.println("=== WATCHDOG STACK [" + label + "] depth " + stack.length + " ===");
        for (int i = 0; i < Math.min(stack.length, 50); i++) {
            System.err.println("    at " + stack[i]);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        // Real launches get the full Apple-style gate choreography; tests never go through main().
        SHOW_LAUNCH_GATES = true;
        launch(args);
    }
}
