package io.m3fx.controls.glass;

import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for the immutable {@link GlassEffectParams} bundle: the documented defaults, the
 * fluent builder (each method returns a NEW bundle and never mutates the receiver), and the
 * documented null handling.
 *
 * <p>Expectations come from the class Javadoc ("Creates the default bundle: regular variant, no
 * tint, not interactive, capsule, radius 16") and {@code docs/SPEC_NOTES/liquid-glass.md}.
 */
class GlassEffectParamsTest {

    private static final double EPS = 1e-9;

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
    void defaultsAreRegularUntintedNonInteractiveCapsule() throws Throwable {
        runOnFx(() -> {
            GlassEffectParams params = new GlassEffectParams();
            assertEquals(GlassVariant.REGULAR, params.variant(), "regular is the default variant");
            assertNull(params.tint(), "no tint by default");
            assertFalse(params.isInteractive(), "interactivity is opt-in");
            assertEquals(GlassShape.CAPSULE, params.shape(), "capsule is the default shape");
            assertEquals(16.0, params.cornerRadius(), EPS, "documented default radius 16");
        });
    }

    @Test
    void fluentMethodsReturnNewBundlesAndNeverMutateTheReceiver() throws Throwable {
        runOnFx(() -> {
            GlassEffectParams base = new GlassEffectParams();

            GlassEffectParams shaped = base.in(GlassShape.RECT, 28);
            assertNotSame(base, shaped, "in() must return a new immutable bundle");
            assertEquals(GlassShape.RECT, shaped.shape());
            assertEquals(28.0, shaped.cornerRadius(), EPS);
            assertEquals(GlassShape.CAPSULE, base.shape(), "receiver stays capsule");
            assertEquals(16.0, base.cornerRadius(), EPS, "receiver keeps its radius");

            GlassEffectParams interactive = base.interactive();
            assertNotSame(base, interactive, "interactive() must return a new bundle");
            assertTrue(interactive.isInteractive(), "copy is interactive");
            assertFalse(base.isInteractive(), "receiver stays non-interactive");

            Color tint = Color.web("#FF8A00");
            GlassEffectParams tinted = base.tint(tint);
            assertNotSame(base, tinted, "tint() must return a new bundle");
            assertEquals(tint, tinted.tint());
            assertNull(base.tint(), "receiver stays untinted");

            GlassEffectParams cleared = base.variant(GlassVariant.CLEAR);
            assertNotSame(base, cleared, "variant() must return a new bundle");
            assertEquals(GlassVariant.CLEAR, cleared.variant());
            assertEquals(GlassVariant.REGULAR, base.variant(), "receiver stays regular");
        });
    }

    @Test
    void fluentChainComposesEveryDimension() throws Throwable {
        runOnFx(() -> {
            Color tint = Color.web("#6750A4");
            GlassEffectParams params = new GlassEffectParams()
                    .in(GlassShape.RECT, 28)
                    .interactive()
                    .tint(tint)
                    .variant(GlassVariant.CLEAR);
            assertEquals(GlassVariant.CLEAR, params.variant());
            assertEquals(tint, params.tint());
            assertTrue(params.isInteractive());
            assertEquals(GlassShape.RECT, params.shape());
            assertEquals(28.0, params.cornerRadius(), EPS);
        });
    }

    @Test
    void bundlesAreIndependentOfEachOther() throws Throwable {
        runOnFx(() -> {
            GlassEffectParams base = new GlassEffectParams();
            GlassEffectParams a = base.in(GlassShape.RECT, 10).interactive();
            GlassEffectParams b = base.in(GlassShape.CIRCLE, 0);
            assertEquals(GlassShape.RECT, a.shape(), "first copy keeps its shape");
            assertTrue(a.isInteractive());
            assertEquals(GlassShape.CIRCLE, b.shape(), "second copy is independent");
            assertFalse(b.isInteractive(), "second copy does not inherit the first copy's flag");
            assertEquals(GlassShape.CAPSULE, base.shape(), "base never changes");
        });
    }

    @Test
    void nullArgumentsFallBackToDocumentedDefaults() throws Throwable {
        runOnFx(() -> {
            GlassEffectParams shaped = new GlassEffectParams().in(null, 5);
            assertEquals(GlassShape.CAPSULE, shaped.shape(), "null shape resets to capsule");
            GlassEffectParams variant = new GlassEffectParams().variant(null);
            assertEquals(GlassVariant.REGULAR, variant.variant(), "null variant resets to regular");
            GlassEffectParams untinted = new GlassEffectParams().tint(Color.RED).tint(null);
            assertNull(untinted.tint(), "null tint clears the tint");
        });
    }
}
