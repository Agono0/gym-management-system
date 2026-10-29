package io.m3fx.controls.effect;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.InvalidationListener;
import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

/**
 * Neon and Colored Glow Elevation utility, transforming flat shadows into glowing
 * cyber atmospheric accents using dynamic M3 primary and tertiary theme roles.
 */
public final class M3GlowShadow {

    private static final String GLOW_TIMER_KEY = "m3fx.glow.timer";
    private static final String GLOW_VISIBILITY_KEY = "m3fx.glow.visibility-listener";

    private M3GlowShadow() {}

    /**
     * Applies a glowing neon shadow using a theme color role.
     *
     * @param node the target node
     * @param role theme color role (e.g. PRIMARY, TERTIARY)
     * @param radius blur radius in px (e.g. 16.0)
     * @param spread spread factor 0.0 to 1.0 (e.g. 0.35)
     */
    public static void apply(Node node, ColorRole role, double radius, double spread) {
        if (node == null) return;
        Color color = ThemeColors.resolve(node, role);
        apply(node, color, radius, spread);
    }

    /**
     * Applies a glowing neon shadow with a specific color.
     *
     * @param node target node
     * @param color glow color
     * @param radius blur radius in px
     * @param spread spread factor 0.0 to 1.0
     */
    public static void apply(Node node, Color color, double radius, double spread) {
        if (node == null) return;
        DropShadow glow = new DropShadow();
        glow.setBlurType(BlurType.GAUSSIAN);
        glow.setColor(color != null ? color : Color.CYAN);
        glow.setRadius(radius);
        glow.setSpread(Math.max(0.0, Math.min(1.0, spread)));
        glow.setOffsetX(0);
        glow.setOffsetY(0);
        node.setEffect(glow);
    }

    /**
     * Starts a subtle breathing neon glow animation on a node. The pulse pauses automatically
     * while the node is not attached to a showing scene, hidden, or disabled, and resumes when
     * it becomes visible again.
     *
     * @param node target node
     * @param color glow color
     * @param minRadius min radius in px (e.g. 10)
     * @param maxRadius max radius in px (e.g. 26)
     */
    public static void animatePulse(Node node, Color color, double minRadius, double maxRadius) {
        if (node == null) return;
        stopPulse(node);
        double lo = Math.min(minRadius, maxRadius);
        double hi = Math.max(minRadius, maxRadius);
        DropShadow glow = new DropShadow(BlurType.GAUSSIAN,
                color != null ? color : Color.CYAN, lo, 0.3, 0, 0);
        node.setEffect(glow);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = (now / 1_000_000_000.0) * 2.2;
                double sin = (Math.sin(t) + 1.0) / 2.0; // 0.0 to 1.0
                glow.setRadius(lo + sin * (hi - lo));
            }
        };
        node.getProperties().put(GLOW_TIMER_KEY, timer);

        // One shared listener guards the run state against scene/visibility/disable changes.
        InvalidationListener runStateListener = obs -> syncPulseRunState(node);
        node.sceneProperty().addListener(runStateListener);
        node.visibleProperty().addListener(runStateListener);
        node.disabledProperty().addListener(runStateListener);
        node.getProperties().put(GLOW_VISIBILITY_KEY, runStateListener);

        syncPulseRunState(node);
    }

    /** Starts or stops the registered pulse timer based on whether the node is showing. */
    private static void syncPulseRunState(Node node) {
        Object obj = node.getProperties().get(GLOW_TIMER_KEY);
        if (!(obj instanceof AnimationTimer timer)) {
            return;
        }
        boolean showing = node.getScene() != null && node.isVisible() && !node.isDisable();
        if (showing) {
            timer.start();
        } else {
            timer.stop();
        }
    }

    /** Stops any active pulse animation on the node and clears its effect. */
    public static void stopPulse(Node node) {
        if (node == null) return;
        Object obj = node.getProperties().remove(GLOW_TIMER_KEY);
        if (obj instanceof AnimationTimer timer) {
            timer.stop();
        }
        Object listener = node.getProperties().remove(GLOW_VISIBILITY_KEY);
        if (listener instanceof InvalidationListener runStateListener) {
            node.sceneProperty().removeListener(runStateListener);
            node.visibleProperty().removeListener(runStateListener);
            node.disabledProperty().removeListener(runStateListener);
        }
        node.setEffect(null);
    }
}

