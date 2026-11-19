package com.gym.tools;

import com.gym.GymApp;
import com.gym.controller.AppShellController;
import com.gym.controller.Screen;
import io.m3fx.controls.M3Stylesheets;
import io.m3fx.core.theme.M3Theme;
import io.m3fx.core.theme.ThemeVariant;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only visual regression tool (QA lane): snapshots every Java-built screen and the app shell
 * into PNGs so the multi-agent review loop can compare renders against the M3 spec notes and the
 * anti-slop checklist without manually clicking through the app.
 *
 * <p>Run with {@code ./gradlew screenshots}; output lands in
 * {@code build/screens/<screen>_<theme>_w<width>.png}. Optional flags:
 * {@code --widths=1280}, {@code --themes=dark,light}, {@code --screens=dashboard,customer-list},
 * plus an unprefixed argument to override the output directory.
 *
 * <p>Implementation notes: the stage is shown at opacity 0 purely so JavaFX runs real layout and
 * CSS pulses — node snapshots ignore stage opacity. Pixels transfer through the
 * {@link WritableImage} reader (no javafx.swing dependency). Every capture is isolated in
 * try/catch so one broken screen cannot stop the rest of the matrix; failures are summarized on
 * stderr when the run finishes.
 */
public final class ScreenShooter extends Application {

    private static final long SCREEN_SETTLE_MS = 400;
    private static final long SHELL_SETTLE_MS = 1_000; // shell-with-dashboard runs an enter animation

    private static final String SHELL_DASHBOARD_KEY = "shell-dashboard";

    private final List<String> failures = new ArrayList<>();
    private final List<Path> written = new ArrayList<>();
    private Path outDir = Path.of("build", "screens");
    private int[] widths = {1024, 1280, 1600};
    private boolean[] darkFlags = {true, false};
    private List<String> screenFilter = List.of();

    /** One render: a screen key at one theme/width combination. */
    private record Capture(String shot, String key, boolean dark, int width) {}

    @Override
    public void start(Stage stage) throws Exception {
        parseArgs(getParameters().getRaw());
        Files.createDirectories(outDir);

        List<Capture> captures = buildCaptures();
        System.out.println("[ScreenShooter] rendering " + captures.size()
                + " captures into " + outDir.toAbsolutePath());

        stage.setOpacity(0);
        stage.show();
        renderNext(stage, captures, 0);
    }

    /** Parses the optional {@code --key=value} flags; an unprefixed arg overrides the output dir. */
    private void parseArgs(List<String> raw) {
        for (String arg : raw) {
            if (arg.startsWith("--widths=")) {
                widths = parseIntList(arg.substring("--widths=".length()));
            } else if (arg.startsWith("--themes=")) {
                String[] parts = arg.substring("--themes=".length()).split(",");
                darkFlags = new boolean[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    darkFlags[i] = parts[i].trim().equalsIgnoreCase("dark");
                }
            } else if (arg.startsWith("--screens=")) {
                screenFilter = List.of(arg.substring("--screens=".length()).split(","));
            } else if (!arg.startsWith("--")) {
                outDir = Path.of(arg);
            }
        }
    }

    private int[] parseIntList(String csv) {
        String[] parts = csv.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        return result;
    }

    /**
     * Builds the full matrix: every registry screen plus the shell with the dashboard preloaded
     * (the state reviewers actually care about), across every theme × width combination.
     */
    private List<Capture> buildCaptures() {
        List<Capture> captures = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (Screen screen : Screen.values()) {
            keys.add(screen.key());
        }
        keys.add(SHELL_DASHBOARD_KEY);

        for (boolean dark : darkFlags) {
            for (int width : widths) {
                for (String key : keys) {
                    if (!screenFilter.isEmpty() && !screenFilter.contains(key)) {
                        continue;
                    }
                    captures.add(new Capture(shotName(key, dark, width), key, dark, width));
                }
            }
        }
        return captures;
    }

    private String shotName(String key, boolean dark, int width) {
        return key + "_" + (dark ? "dark" : "light") + "_w" + width;
    }

    /** Renders captures sequentially, giving each one settle pulse so CSS/layout/glass settle. */
    private void renderNext(Stage stage, List<Capture> captures, int index) {
        if (index >= captures.size()) {
            finish(stage);
            return;
        }
        Capture capture = captures.get(index);
        try {
            M3Theme theme = M3Theme.fromSeed(GymApp.SEED_PURPLE, ThemeVariant.TONAL_SPOT, capture.dark());
            M3Stylesheets.setLatestTheme(theme);

            Parent root = buildRoot(capture);
            Scene scene = new Scene(root, capture.width(), Math.round(capture.width() * 0.625f));
            M3Stylesheets.applyTo(scene, theme);
            stage.setScene(scene);
            stage.setWidth(capture.width());
            stage.setHeight(scene.getHeight());
        } catch (Exception ex) {
            failures.add(capture.shot() + " — build failed: " + ex);
            renderNext(stage, captures, index + 1);
            return;
        }

        long settleMs = SHELL_DASHBOARD_KEY.equals(capture.key()) ? SHELL_SETTLE_MS : SCREEN_SETTLE_MS;
        PauseTransition settle = new PauseTransition(Duration.millis(settleMs));
        settle.setOnFinished(e -> {
            try {
                writeSnapshot(stage, capture);
            } catch (Exception ex) {
                failures.add(capture.shot() + " — snapshot failed: " + ex);
            }
            renderNext(stage, captures, index + 1);
        });
        settle.play();
    }

    /** Instantiates the screen controller for a capture key (registry keys or shell-dashboard). */
    private Parent buildRoot(Capture capture) throws Exception {
        if (SHELL_DASHBOARD_KEY.equals(capture.key())) {
            AppShellController shell = new AppShellController();
            shell.loadScreen(Screen.DASHBOARD);
            return shell.getRoot();
        }
        Screen screen = Screen.byKey(capture.key());
        if (screen == null) {
            throw new IllegalArgumentException("unknown screen key: " + capture.key());
        }
        return screen.create().getRoot();
    }

    /** Renders the scene root into a PNG via node snapshot (stage opacity does not affect it). */
    private void writeSnapshot(Stage stage, Capture capture) throws Exception {
        Parent root = stage.getScene().getRoot();
        WritableImage image = root.snapshot(null, null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height,
                PixelFormat.getIntArgbInstance(), pixels, 0, width);
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        buffered.setRGB(0, 0, width, height, pixels, 0, width);
        Path file = outDir.resolve(capture.shot() + ".png");
        ImageIO.write(buffered, "png", file.toFile());
        written.add(file);
    }

    private void finish(Stage stage) {
        stage.close();
        System.out.println("[ScreenShooter] wrote " + written.size() + " captures to "
                + outDir.toAbsolutePath());
        for (String failure : failures) {
            System.err.println("[ScreenShooter] FAILURE " + failure);
        }
        if (!failures.isEmpty()) {
            System.err.println("[ScreenShooter] " + failures.size() + " capture(s) failed — "
                    + "review the list above before trusting the matrix.");
        }
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
