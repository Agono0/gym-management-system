package io.m3fx.controls.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * Verifies the Apple-flavored MotionUtil primitives ({@code splashIn}, {@code splashOut},
 * {@code shake}, {@code morphFromBounds}) on a live FX toolkit: end states, callback
 * exactly-once semantics, the morph start-bounds math, the no-op guard paths, and
 * re-entrancy (at most one tracked timeline per node).
 */
class MotionUtilTest {

    /** Mirrors MotionUtil's private property key; asserted to keep the tracking contract. */
    private static final String TRACKED_TIMELINE_KEY = "m3fx.motion.tracked-timeline";

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Already started
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to initialize");
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

    /** Polls an FX-thread condition until it holds or the timeout elapses. */
    private static boolean awaitFxCondition(BooleanSupplier condition, long timeoutMs) throws Throwable {
        long deadline = System.currentTimeMillis() + timeoutMs;
        AtomicBoolean satisfied = new AtomicBoolean(false);
        while (System.currentTimeMillis() < deadline) {
            satisfied.set(false);
            runOnFx(() -> satisfied.set(condition.getAsBoolean()));
            if (satisfied.get()) {
                return true;
            }
            Thread.sleep(25);
        }
        return false;
    }

    private static Rectangle nodeInScene() {
        Rectangle rect = new Rectangle(120, 80);
        new Scene(new Group(rect));
        return rect;
    }

    private static boolean isIdentity(Rectangle rect) {
        return rect.getOpacity() == 1.0
                && rect.getTranslateX() == 0
                && rect.getTranslateY() == 0
                && rect.getScaleX() == 1.0
                && rect.getScaleY() == 1.0;
    }

    private static int countTrackedTimelines(Rectangle rect) {
        int count = 0;
        for (Object value : rect.getProperties().values()) {
            if (value instanceof Timeline) {
                count++;
            }
        }
        return count;
    }

    private static void assertBoundsClose(Bounds expected, Bounds actual, double tolerance) {
        assertEquals(expected.getMinX(), actual.getMinX(), tolerance, "minX");
        assertEquals(expected.getMinY(), actual.getMinY(), tolerance, "minY");
        assertEquals(expected.getWidth(), actual.getWidth(), tolerance, "width");
        assertEquals(expected.getHeight(), actual.getHeight(), tolerance, "height");
    }

    @Test
    void shakeStartsLeftAndEndsAtTranslateZeroWithin2s() throws Throwable {
        AtomicReference<Rectangle> holder = new AtomicReference<>();
        runOnFx(() -> {
            Rectangle rect = nodeInScene();
            MotionUtil.shake(rect);
            assertEquals(-12.0, rect.getTranslateX(), 1e-9, "shake kicks the node 12px left");
            holder.set(rect);
        });
        Rectangle rect = holder.get();
        // The spring sets the property to exactly 0 at settle; require the value to hold at 0
        // across two polls spaced apart so a mid-flight zero-crossing cannot satisfy this.
        assertTrue(awaitFxCondition(() -> rect.getTranslateX() == 0.0, 1000),
                "translateX must reach 0 within the spring's settle window");
        assertTrue(awaitFxCondition(() -> rect.getTranslateX() == 0.0, 300),
                "translateX must remain 0 (spring settled, not mid-oscillation)");
        runOnFx(() -> assertEquals(0.0, rect.getTranslateX(), 1e-9,
                "shake must end exactly at translateX 0"));
    }

    @Test
    void splashInStartsOversizedAndFadedAndSettlesToIdentity() throws Throwable {
        AtomicReference<Rectangle> holder = new AtomicReference<>();
        runOnFx(() -> {
            Rectangle rect = nodeInScene();
            rect.setScaleX(0.5);
            MotionUtil.splashIn(rect);
            assertEquals(0.0, rect.getOpacity(), 1e-9, "splashIn starts fully transparent");
            assertEquals(1.06, rect.getScaleX(), 1e-9, "splashIn starts oversized");
            assertEquals(1.06, rect.getScaleY(), 1e-9);
            assertEquals(0, rect.getTranslateX(), 1e-9);
            holder.set(rect);
        });
        Rectangle rect = holder.get();
        assertTrue(awaitFxCondition(() -> isIdentity(rect), 2000),
                "splashIn must settle to opacity 1 + identity transforms within 2s");
        runOnFx(() -> assertFalse(rect.getProperties().containsKey(TRACKED_TIMELINE_KEY),
                "tracked timeline must be cleared after splashIn"));
    }

    @Test
    void splashOutInvokesOnDoneExactlyOnceAndResets() throws Throwable {
        AtomicReference<Rectangle> holder = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        runOnFx(() -> {
            Rectangle rect = nodeInScene();
            MotionUtil.splashOut(rect, () -> {
                calls.incrementAndGet();
                done.countDown();
            });
            holder.set(rect);
        });
        assertTrue(done.await(2, TimeUnit.SECONDS), "splashOut onDone must fire within 2s");
        runOnFx(() -> {
            Rectangle rect = holder.get();
            assertTrue(isIdentity(rect), "splashOut must reset transforms + opacity to identity");
            assertEquals(1, calls.get(), "onDone must have run exactly once");
            assertFalse(rect.getProperties().containsKey(TRACKED_TIMELINE_KEY),
                    "tracked timeline must be cleared after splashOut");
        });
        Thread.sleep(400);
        runOnFx(() -> assertEquals(1, calls.get(), "onDone must never fire a second time"));
    }

    @Test
    void morphFromBoundsStartsAtGivenBoundsAndSettlesToNaturalPlace() throws Throwable {
        AtomicReference<Rectangle> holder = new AtomicReference<>();
        AtomicReference<Bounds> natural = new AtomicReference<>();
        AtomicReference<Bounds> start = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        runOnFx(() -> {
            Rectangle rect = nodeInScene();
            rect.setX(40);
            rect.setY(25);
            Bounds naturalRect = rect.localToScene(rect.getLayoutBounds());
            // Start rect: shifted up-left and shrunk, like a smaller card morphing to the shell.
            Bounds startBounds = new BoundingBox(
                    naturalRect.getMinX() - 30,
                    naturalRect.getMinY() - 15,
                    naturalRect.getWidth() * 0.8,
                    naturalRect.getHeight() * 0.8);
            MotionUtil.morphFromBounds(rect, startBounds, true, () -> {
                calls.incrementAndGet();
                done.countDown();
            });
            // Immediately after the call the node's scene bounds must equal the start rect.
            assertBoundsClose(startBounds, rect.localToScene(rect.getLayoutBounds()), 1e-4);
            holder.set(rect);
            natural.set(naturalRect);
            start.set(startBounds);
        });
        assertTrue(done.await(3, TimeUnit.SECONDS), "morph must settle within 3s");
        runOnFx(() -> {
            Rectangle rect = holder.get();
            assertEquals(1.0, rect.getScaleX(), 1e-9, "morph must end at scale 1");
            assertEquals(1.0, rect.getScaleY(), 1e-9);
            assertEquals(0, rect.getTranslateX(), 1e-9, "morph must end at translate 0");
            assertEquals(0, rect.getTranslateY(), 1e-9);
            assertEquals(1, calls.get(), "onDone must run exactly once after settle");
            assertFalse(rect.getProperties().containsKey(TRACKED_TIMELINE_KEY),
                    "tracked timeline must be cleared after the morph");
            assertBoundsClose(natural.get(), rect.localToScene(rect.getLayoutBounds()), 1e-4);
        });
    }

    @Test
    void morphFromBoundsNoOpPathsInvokeOnDoneWithoutTransformChanges() throws Throwable {
        AtomicReference<Rectangle> holder = new AtomicReference<>();
        AtomicReference<Rectangle> detachedHolder = new AtomicReference<>();
        runOnFx(() -> {
            Rectangle rect = nodeInScene();
            rect.setScaleX(1.5);
            rect.setTranslateX(7);
            Rectangle detached = new Rectangle(50, 50); // never added to a scene
            AtomicInteger calls = new AtomicInteger();
            Bounds valid = new BoundingBox(10, 10, 100, 60);

            MotionUtil.morphFromBounds(rect, valid, false, calls::incrementAndGet);
            assertEquals(1, calls.get(), "animate=false must invoke onDone synchronously");

            MotionUtil.morphFromBounds(rect, null, true, calls::incrementAndGet);
            assertEquals(2, calls.get(), "null bounds must invoke onDone");

            MotionUtil.morphFromBounds(rect, new BoundingBox(0, 0, 0, 0), true, calls::incrementAndGet);
            assertEquals(3, calls.get(), "zero-area bounds must invoke onDone");

            MotionUtil.morphFromBounds(rect, new BoundingBox(Double.NaN, 0, 100, 60), true,
                    calls::incrementAndGet);
            assertEquals(4, calls.get(), "non-finite bounds must invoke onDone");

            MotionUtil.morphFromBounds(detached, valid, true, calls::incrementAndGet);
            assertEquals(5, calls.get(), "node outside a scene must invoke onDone");

            assertDoesNotThrow(() -> MotionUtil.morphFromBounds(rect, null, true, null),
                    "null onDone on a no-op path must not throw");

            assertEquals(1.5, rect.getScaleX(), 1e-9, "no-op paths must not touch scale");
            assertEquals(7, rect.getTranslateX(), 1e-9, "no-op paths must not touch translate");
            assertEquals(1.0, detached.getScaleX(), 1e-9);
            assertEquals(0, detached.getTranslateX(), 1e-9);
            holder.set(rect);
            detachedHolder.set(detached);
        });
    }

    @Test
    void repeatedInvocationLeavesExactlyOneTrackedTimelineAndIdentityEndState() throws Throwable {
        AtomicReference<Rectangle> holder = new AtomicReference<>();
        runOnFx(() -> {
            Rectangle rect = nodeInScene();
            MotionUtil.enter(rect);
            MotionUtil.enter(rect);
            assertEquals(1, countTrackedTimelines(rect),
                    "re-entering must replace, not stack, the tracked timeline");
            assertTrue(rect.getProperties().containsKey(TRACKED_TIMELINE_KEY));

            MotionUtil.splashIn(rect);
            assertEquals(1, countTrackedTimelines(rect),
                    "splashIn over enter must still leave exactly one tracked timeline");
            MotionUtil.splashIn(rect);
            assertEquals(1, countTrackedTimelines(rect),
                    "double splashIn must leave exactly one tracked timeline");
            holder.set(rect);
        });
        Rectangle rect = holder.get();
        assertTrue(awaitFxCondition(() -> isIdentity(rect) && rect.getOpacity() == 1.0, 2000),
                "the surviving timeline must still settle to identity + opacity 1");
        runOnFx(() -> assertFalse(rect.getProperties().containsKey(TRACKED_TIMELINE_KEY),
                "tracked timeline must be cleared after the final splashIn"));
    }
}
