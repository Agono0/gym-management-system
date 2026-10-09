package io.m3fx.controls.internal;

import io.m3fx.core.theme.ColorRole;
import io.m3fx.core.theme.M3Theme;
import io.m3fx.core.token.ElevationLevel;
import io.m3fx.core.token.MotionTokens;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Shared M3 elevation helper: maps {@link ElevationLevel} 0-5 to drop shadows and animates level
 * changes (hover raise, press lower) with {@link MotionTokens} timing.
 */
public final class ElevationShadow {

    private ElevationShadow() {}

    /**
     * Applies the shadow for a level to a node.
     *
     * @param node the node to shadow
     * @param level the elevation level
     * @param theme the current theme (for the shadow color)
     */
    public static void applyTo(Node node, ElevationLevel level, M3Theme theme) {
        if (level == ElevationLevel.LEVEL0) {
            node.setEffect(null);
            return;
        }
        node.setEffect(level.toEffect(theme == null ? null : theme.get(ColorRole.SHADOW)));
    }

    /**
     * Animates a node from one elevation level to another by cross-fading shadow opacity.
     *
     * @param node the node
     * @param from start level
     * @param to end level
     * @param theme the current theme (may be {@code null} for a black shadow)
     */
    public static void animateTo(Node node, ElevationLevel from, ElevationLevel to, M3Theme theme) {
        animateTo(node, from, to, theme == null ? null : theme.get(ColorRole.SHADOW));
    }

    /**
     * Animates a node from one elevation level to another with an explicit shadow color.
     *
     * @param node the node
     * @param from start level
     * @param to end level
     * @param shadowColor shadow color, may be {@code null} for black
     */
    public static void animateTo(
            Node node, ElevationLevel from, ElevationLevel to, javafx.scene.paint.Color shadowColor) {
        Object existing = node.getProperties().get("m3fx.elevation-timeline");
        if (existing instanceof Timeline running) {
            running.stop();
        }
        if (from == to) {
            applyLevel(node, to, shadowColor);
            return;
        }
        applyLevel(node, to, shadowColor);
        if (node.getEffect() instanceof javafx.scene.effect.DropShadow shadow) {
            javafx.scene.paint.Color target = (javafx.scene.paint.Color) shadow.getColor();
            javafx.scene.paint.Color start =
                    new javafx.scene.paint.Color(
                            target.getRed(), target.getGreen(), target.getBlue(), 0);
            shadow.setColor(start);
            Timeline timeline =
                    new Timeline(
                            new KeyFrame(
                                    Duration.millis(MotionTokens.DURATION_SHORT_3),
                                    new KeyValue(
                                            shadow.colorProperty(),
                                            target,
                                            MotionTokens.standard())));
            node.getProperties().put("m3fx.elevation-timeline", timeline);
            timeline.setOnFinished(e -> node.getProperties().remove("m3fx.elevation-timeline"));
            timeline.play();
        }
    }

    private static void applyLevel(
            Node node, ElevationLevel level, javafx.scene.paint.Color shadowColor) {
        if (level == ElevationLevel.LEVEL0) {
            node.setEffect(null);
            return;
        }
        node.setEffect(level.toEffect(shadowColor));
    }
}
