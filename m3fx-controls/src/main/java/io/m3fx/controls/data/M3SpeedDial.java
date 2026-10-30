package io.m3fx.controls.data;

import io.m3fx.controls.fab.FabSize;
import io.m3fx.controls.fab.M3Fab;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.internal.SpringAnimation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

/**
 * Radial Speed Dial Action Wheel expanding outward with spring physics and rotating trigger FAB.
 */
public class M3SpeedDial extends StackPane {

    /** Side of the dial pane frame; dial items center on the FAB center. */
    private static final double DIAL_PANE_SIZE = 48.0;

    /** M3 default FAB trigger size (56dp, see the M3 FAB spec). */
    private static final double FAB_SIZE = 56.0;

    /** Delay between consecutive items in the stagger, from the M3 short-1 motion token. */
    private static final int STAGGER_MILLIS = io.m3fx.core.token.MotionTokens.DURATION_SHORT_1;

    /** Model for a speed dial action item. */
    public static class ActionItem {
        private final String label;
        private final Node icon;
        private final Runnable onAction;
        private boolean enabled = true;

        /**
         * Creates an action item.
         *
         * @param label action label (accessibility text)
         * @param icon icon node shown in the dial button
         * @param onAction callback run when the item is activated, may be {@code null}
         */
        public ActionItem(String label, Node icon, Runnable onAction) {
            this.label = label;
            this.icon = icon;
            this.onAction = onAction;
        }

        /** @return the action label */
        public String getLabel() { return label; }

        /** @return the icon node */
        public Node getIcon() { return icon; }

        /** @return the activation callback, or {@code null} */
        public Runnable getOnAction() { return onAction; }

        /** @return whether the action can be activated (default {@code true}) */
        public boolean isEnabled() { return enabled; }

        /**
         * Enables or disables the action; disabled items render as disabled buttons that
         * cannot be activated.
         *
         * @param enabled {@code false} to disable
         */
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    private final BooleanProperty expanded = new SimpleBooleanProperty(this, "expanded", false);
    private final DoubleProperty dialRadius = new SimpleDoubleProperty(this, "dialRadius", 80.0);

    private final List<ActionItem> actions = new ArrayList<>();
    private final List<javafx.animation.PauseTransition> pendingStaggers = new ArrayList<>();
    private final Pane dialPane = new Pane();
    private final M3Fab triggerButton;
    private final Rotate triggerRotate = new Rotate(0, FAB_SIZE / 2.0, FAB_SIZE / 2.0);

    /** Creates an empty speed dial. */
    public M3SpeedDial() {
        getStyleClass().add("m3-speed-dial");
        setAlignment(Pos.CENTER);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setPickOnBounds(false);

        // Pin the dial pane to a fixed square centered under the FAB so ring positions are
        // computed from the FAB center instead of the component's top-left corner.
        dialPane.setPickOnBounds(false);
        dialPane.setPrefSize(DIAL_PANE_SIZE, DIAL_PANE_SIZE);
        dialPane.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        triggerButton = new M3Fab(M3Icon.symbol("add"), FabSize.MEDIUM);
        triggerButton.setAccessibleText("Expand Speed Dial");
        triggerButton.getTransforms().add(triggerRotate);
        triggerButton.setOnAction(e -> setExpanded(!isExpanded()));

        getChildren().addAll(dialPane, triggerButton);

        expandedProperty().addListener((obs, o, exp) -> animateDial(exp));

        // Lifecycle: when the dial leaves the scene, kill pending stagger timers and transform
        // springs so nothing keeps animating a detached node, and snap items to the rest state
        // of the current expanded flag (nothing resumes half-open on re-add).
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                cancelDialMotion();
                snapDialToRest();
            }
        });
    }

    /** Stops every pending stagger and running dial spring. */
    private void cancelDialMotion() {
        for (javafx.animation.PauseTransition pending : pendingStaggers) {
            pending.stop();
        }
        pendingStaggers.clear();
        SpringAnimation.stop(triggerRotate.angleProperty());
        for (Node btn : dialPane.getChildren()) {
            SpringAnimation.stopTransforms(btn);
            SpringAnimation.stop(btn.opacityProperty());
        }
    }

    /** Instantly places every dial item and the trigger at their rest state (no animation). */
    private void snapDialToRest() {
        boolean open = isExpanded();
        int n = actions.size();
        double radius = getDialRadius();
        double startAngle = Math.PI;
        double endAngle = 2 * Math.PI;
        double step = n > 1 ? (endAngle - startAngle) / (n - 1) : 0;
        for (int i = 0; i < n && i < dialPane.getChildren().size(); i++) {
            Node btn = dialPane.getChildren().get(i);
            double angle = n == 1 ? (3 * Math.PI / 2) : (startAngle + i * step);
            btn.setTranslateX(open ? radius * Math.cos(angle) : 0);
            btn.setTranslateY(open ? radius * Math.sin(angle) : 0);
            btn.setScaleX(open ? 1.0 : 0.0);
            btn.setScaleY(open ? 1.0 : 0.0);
            btn.setOpacity(open ? 1.0 : 0.0);
            btn.setVisible(open);
        }
        triggerRotate.setAngle(open ? 45.0 : 0.0);
        triggerButton.setAccessibleText(open ? "Collapse Speed Dial" : "Expand Speed Dial");
    }

    /** Whether the dial is currently expanded. */
    public BooleanProperty expandedProperty() {
        return expanded;
    }

    public boolean isExpanded() {
        return expanded.get();
    }

    public void setExpanded(boolean expanded) {
        this.expanded.set(expanded);
    }

    /** Distance from the FAB center to the center of the outermost dial items, in px. */
    public DoubleProperty dialRadiusProperty() {
        return dialRadius;
    }

    public double getDialRadius() {
        return dialRadius.get();
    }

    public void setDialRadius(double radius) {
        this.dialRadius.set(radius);
    }

    /**
     * Adds an action item to the radial wheel.
     *
     * @param label action label
     * @param icon icon node
     * @param onAction action callback
     */
    public void addAction(String label, Node icon, Runnable onAction) {
        actions.add(new ActionItem(label, icon, onAction));
        rebuildActionButtons();
    }

    /**
     * Enables or disables the action at the given index; disabled actions render as disabled
     * buttons that cannot be activated.
     *
     * @param index action index
     * @param enabled {@code false} to disable
     */
    public void setActionEnabled(int index, boolean enabled) {
        if (index < 0 || index >= actions.size()) {
            return;
        }
        actions.get(index).setEnabled(enabled);
        if (index < dialPane.getChildren().size()) {
            dialPane.getChildren().get(index).setDisable(!enabled);
        }
    }

    private void rebuildActionButtons() {
        dialPane.getChildren().clear();
        int n = actions.size();
        if (n == 0) return;

        // 48px tonal icon buttons stacked dead-center behind the 48px trigger
        for (ActionItem item : actions) {
            M3IconButton btn = new M3IconButton(item.getIcon(), IconButtonVariant.TONAL);
            btn.setAccessibleText(item.getLabel());
            btn.setDisable(!item.isEnabled());
            btn.setOnAction(e -> {
                if (item.getOnAction() != null) {
                    item.getOnAction().run();
                }
                setExpanded(false);
            });

            btn.setTranslateX(0);
            btn.setTranslateY(0);
            btn.setScaleX(0.0);
            btn.setScaleY(0.0);
            btn.setOpacity(0.0);
            btn.setVisible(false);

            dialPane.getChildren().add(btn);
        }
    }

    private void animateDial(boolean open) {
        // Rotate trigger button (0 to 45 deg turning '+' into 'x')
        double targetAngle = open ? 45.0 : 0.0;
        triggerButton.setAccessibleText(open ? "Collapse Speed Dial" : "Expand Speed Dial");
        SpringAnimation.animate(triggerRotate.angleProperty(), targetAngle, SpringAnimation.BOUNCY, null);

        int n = actions.size();
        if (n == 0) return;

        // Cancel any stagger timers from a rapid re-toggle before scheduling new ones
        for (javafx.animation.PauseTransition pending : pendingStaggers) {
            pending.stop();
        }
        pendingStaggers.clear();

        double radius = getDialRadius();

        // Radial positions on the upper semi-circle arc (180°..360°: left -> top -> right)
        double startAngle = Math.PI;    // left
        double endAngle = 2 * Math.PI;  // right
        double step = n > 1 ? (endAngle - startAngle) / (n - 1) : 0;

        for (int i = 0; i < n && i < dialPane.getChildren().size(); i++) {
            Node btn = dialPane.getChildren().get(i);
            double angle = n == 1 ? (3 * Math.PI / 2) : (startAngle + i * step);

            // Translations are relative to the dial pane's top-left, i.e. the FAB frame
            double targetX = open ? radius * Math.cos(angle) : 0;
            double targetY = open ? radius * Math.sin(angle) : 0;

            btn.setVisible(true);

            // M3 stagger: items ripple outward on open, inward on close
            int order = open ? i : (n - 1 - i);
            if (order == 0) {
                animateItem(btn, targetX, targetY, open);
            } else {
                javafx.animation.PauseTransition stagger =
                        new javafx.animation.PauseTransition(javafx.util.Duration.millis(order * STAGGER_MILLIS));
                stagger.setOnFinished(e -> animateItem(btn, targetX, targetY, open));
                pendingStaggers.add(stagger);
                stagger.play();
            }
        }
    }

    private static void animateItem(Node btn, double targetX, double targetY, boolean open) {
        SpringAnimation.animate(btn.translateXProperty(), targetX, SpringAnimation.BOUNCY, null);
        SpringAnimation.animate(btn.translateYProperty(), targetY, SpringAnimation.BOUNCY, null);
        SpringAnimation.animate(btn.scaleXProperty(), open ? 1.0 : 0.0, SpringAnimation.BOUNCY, null);
        SpringAnimation.animate(btn.scaleYProperty(), open ? 1.0 : 0.0, SpringAnimation.BOUNCY, null);
        SpringAnimation.animate(btn.opacityProperty(), open ? 1.0 : 0.0, SpringAnimation.FAST, () -> {
            if (!open) btn.setVisible(false);
        });
    }
}

