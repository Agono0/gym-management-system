package io.m3fx.controls.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class M3LinearProgressTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
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

    private static M3LinearProgress.LinearSkin skinOf(M3LinearProgress bar) {
        return (M3LinearProgress.LinearSkin) bar.getSkin();
    }

    @Test
    void indeterminateBarsAreVisibleOnceTheSkinIsCreatedInAScene() throws Throwable {
        runOnFx(() -> {
            M3LinearProgress bar = new M3LinearProgress(); // indeterminate
            StackPane holder = new StackPane(bar);
            new Scene(holder);
            bar.applyCss(); // the skin is created here, i.e. after the scene attach
            long visibleActive = bar.lookupAll(".m3-linear-active").stream()
                    .filter(Node::isVisible)
                    .count();
            assertEquals(1, visibleActive,
                    "the primary indeterminate bar must be visible once the skin is in a scene");
            long visibleSecondary = bar.lookupAll(".m3-linear-secondary").stream()
                    .filter(Node::isVisible)
                    .count();
            assertEquals(1, visibleSecondary, "the secondary bar must be visible too");
        });
    }

    @Test
    void indeterminateLoopIsCreatedOnceAndSurvivesWidthChanges() throws Throwable {
        runOnFx(() -> {
            M3LinearProgress bar = new M3LinearProgress();
            StackPane holder = new StackPane(bar);
            new Scene(holder);
            bar.applyCss();
            bar.resize(160, 4); // first real width starts the loop
            M3LinearProgress.LinearSkin skin = skinOf(bar);
            Timeline loop = skin.indeterminateLoopForTesting();
            assertNotNull(loop, "the indeterminate loop runs once the control has a width");
            assertEquals(Animation.Status.RUNNING, loop.getStatus());

            bar.resize(90, 4);
            assertSame(loop, skin.indeterminateLoopForTesting(),
                    "a width change must not rebuild the running timeline");
            assertEquals(Animation.Status.RUNNING, loop.getStatus());

            bar.resize(240, 4);
            assertSame(loop, skin.indeterminateLoopForTesting(),
                    "still the same timeline after several width changes");
            assertEquals(Animation.Status.RUNNING, loop.getStatus());
        });
    }

    @Test
    void loopStopsInDeterminateModeAndRestartsWhenIndeterminate() throws Throwable {
        runOnFx(() -> {
            M3LinearProgress bar = new M3LinearProgress();
            StackPane holder = new StackPane(bar);
            new Scene(holder);
            bar.applyCss();
            bar.resize(160, 4);
            M3LinearProgress.LinearSkin skin = skinOf(bar);
            assertNotNull(skin.indeterminateLoopForTesting());

            bar.setProgress(0.5);
            assertNull(skin.indeterminateLoopForTesting(), "determinate mode stops the loop");

            bar.setProgress(M3LinearProgress.INDETERMINATE);
            Timeline restarted = skin.indeterminateLoopForTesting();
            assertNotNull(restarted, "indeterminate mode restarts the loop");
            assertEquals(Animation.Status.RUNNING, restarted.getStatus());
        });
    }

    @Test
    void disposeIsIdempotentAndStopsTheLoop() throws Throwable {
        runOnFx(() -> {
            M3LinearProgress bar = new M3LinearProgress();
            StackPane holder = new StackPane(bar);
            new Scene(holder);
            bar.applyCss();
            bar.resize(160, 4);
            M3LinearProgress.LinearSkin skin = skinOf(bar);
            skin.dispose();
            skin.dispose(); // must be idempotent
            assertNull(skin.indeterminateLoopForTesting(), "dispose stops the loop");
        });
    }

    @Test
    void overshootSplineMatchesSplineForInRangeControlPoints() {
        M3LinearProgress.LinearSkin.OvershootSpline mine =
                new M3LinearProgress.LinearSkin.OvershootSpline(0.06, 0.11, 0.6, 1);
        Interpolator reference = Interpolator.SPLINE(0.06, 0.11, 0.6, 1);
        for (double t = 0; t <= 1.0; t += 0.05) {
            assertEquals(reference.interpolate(0.0, 1.0, t), mine.interpolate(0.0, 1.0, t), 1e-3,
                    "same easing as Interpolator.SPLINE for in-range points at t=" + t);
        }
    }

    @Test
    void overshootSplineKeepsEndpointsAndOvershootsLikeTheWebBezier() {
        // sW3 from material-web _linear-progress.scss: y1 dips below 0, y2 overshoots above 1.
        M3LinearProgress.LinearSkin.OvershootSpline sW3 =
                new M3LinearProgress.LinearSkin.OvershootSpline(0.257759, -0.003163, 0.211762, 1.38179);
        assertEquals(0.0, sW3.interpolate(0.0, 1.0, 0.0), 1e-9, "starts at 0");
        assertEquals(1.0, sW3.interpolate(0.0, 1.0, 1.0), 1e-9, "ends at 1");

        double max = 0;
        double min = 1;
        for (double t = 0; t <= 1.0; t += 0.002) {
            double v = sW3.interpolate(0.0, 1.0, t);
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        assertTrue(max > 1.0, "y2=1.38179 must overshoot above 1, max=" + max);
        assertTrue(min < 1.0, "the curve passes below 1 before overshooting, min=" + min);
    }
}
