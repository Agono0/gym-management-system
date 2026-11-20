package io.m3fx.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.core.theme.M3Theme;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the theme temp-file lifecycle: one shared file per theme content, kept while any
 * scene uses it, released when scenes are released or re-themed, and swept on shutdown.
 *
 * <p>File observations are made by listing the temp dir before/after, per the A5 test note.
 */
class M3StylesheetsThemeFileTest {

    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir"));

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            javafx.application.Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown(); // already started
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to initialize");
    }

    @BeforeEach
    void resetTempDir() throws IOException {
        // Old runs left m3fx-theme-*.css files behind (the pre-fix leak), so every test starts
        // from an empty theme-file dir for deterministic counts.
        M3Stylesheets.deleteStragglerThemeFiles();
        try (Stream<Path> files = Files.list(TEMP_DIR)) {
            for (Path file : files.filter(this::isThemeFile).toList()) {
                Files.deleteIfExists(file);
            }
        }
        assertTrue(themeFileNames().isEmpty(), "temp dir must start without theme files");
    }

    @AfterEach
    void sweepStragglers() {
        M3Stylesheets.deleteStragglerThemeFiles();
    }

    private boolean isThemeFile(Path path) {
        return path.getFileName().toString().startsWith("m3fx-theme-");
    }

    private static void runOnFx(Runnable action) throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = new Throwable[1];
        javafx.application.Platform.runLater(() -> {
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

    /** Returns the file names of the theme temp CSS files currently in the temp dir. */
    private static Set<String> themeFileNames() throws IOException {
        Set<String> names = new HashSet<>();
        try (Stream<Path> files = Files.list(TEMP_DIR)) {
            files.filter(p -> p.getFileName().toString().startsWith("m3fx-theme-"))
                    .forEach(p -> names.add(p.getFileName().toString()));
        }
        return names;
    }

    @Test
    void themeFileIsSharedKeptWhileUsedAndDeletedWhenReleased() throws Throwable {
        AtomicReference<Scene> sceneA = new AtomicReference<>();
        AtomicReference<Scene> sceneB = new AtomicReference<>();
        M3Theme dark = M3Theme.baseline(true);
        runOnFx(() -> {
            sceneA.set(new Scene(new StackPane()));
            sceneB.set(new Scene(new StackPane()));
            M3Stylesheets.applyTo(sceneA.get(), dark);
            M3Stylesheets.applyTo(sceneB.get(), dark);
        });
        Set<String> withTwoScenes = themeFileNames();
        assertEquals(1, withTwoScenes.size(),
                "two scenes sharing one theme must produce exactly one temp file");

        // Releasing one scene must keep the file for the other.
        M3Stylesheets.releaseScene(sceneA.get());
        assertEquals(withTwoScenes, themeFileNames(), "file must survive while scene B lives");
        // Releasing the last scene deletes the file.
        M3Stylesheets.releaseScene(sceneB.get());
        assertTrue(themeFileNames().isEmpty(), "file must be deleted when unused");
    }

    @Test
    void reapplyingANewThemeReleasesTheOldFile() throws Throwable {
        AtomicReference<Scene> scene = new AtomicReference<>();
        runOnFx(() -> {
            scene.set(new Scene(new StackPane()));
            M3Stylesheets.applyTo(scene.get(), M3Theme.baseline(true));
        });
        Set<String> withDark = themeFileNames();
        assertEquals(1, withDark.size(), "dark theme file created");

        runOnFx(() -> M3Stylesheets.applyTo(scene.get(), M3Theme.baseline(false)));
        Set<String> withLight = themeFileNames();
        assertEquals(1, withLight.size(), "one theme file at a time per scene");
        assertFalse(withLight.equals(withDark), "the new theme must use a new file");
        for (String name : withDark) {
            assertFalse(withLight.contains(name), "old dark file " + name + " must be released");
        }

        M3Stylesheets.releaseScene(scene.get());
        assertTrue(themeFileNames().isEmpty(), "light file deleted on release");
    }
}
