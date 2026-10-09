package io.m3fx.controls.fab;

import io.m3fx.controls.icon.M3Icon;
import io.m3fx.core.token.MotionTokens;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * M3 Expressive FAB menu: a FAB that expands into a vertical stack of small action FABs with
 * labels, staggered in with emphasized motion.
 *
 * <p>See <a href="https://m3.material.io/components/floating-action-button/overview">M3 FAB</a>
 * (menu guidance). The main icon rotates 45 degrees when open; Escape or re-pressing the main FAB
 * collapses the menu; each action collapses after running its handler. The main FAB is anchored
 * and never moves — only the action rows rise from behind it with a stagger.
 */
public class M3FabMenu extends StackPane {

    private final M3Fab mainFab;
    private final VBox actionsBox = new VBox(12);
    private final BooleanProperty expanded = new SimpleBooleanProperty(this, "expanded", false);

    /**
     * Creates a collapsed FAB menu with a default "+" icon (FXML/Scene Builder friendly).
     */
    public M3FabMenu() {
        this(M3Icon.symbol("add"));
    }

    /**
     * Creates a collapsed FAB menu.
     *
     * @param mainIcon main FAB icon (rotates when open)
     */
    public M3FabMenu(Node mainIcon) {
        mainFab = new M3Fab(mainIcon, FabSize.MEDIUM);
        mainFab.getStyleClass().add("m3-fab-menu-main");
        actionsBox.getStyleClass().add("m3-fab-menu-actions");
        actionsBox.setAlignment(Pos.BOTTOM_RIGHT);
        actionsBox.setSpacing(12);
        // Managed but invisible when collapsed: the menu keeps its full width so the main FAB
        // never shifts when the menu opens.
        actionsBox.setVisible(false);
        getStyleClass().add("m3-fab-menu");
        StackPane.setAlignment(mainFab, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(actionsBox, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(actionsBox, new Insets(0, 0, 68, 0));
        getChildren().addAll(actionsBox, mainFab);
        mainFab.setOnAction(e -> toggle());
        expandedProperty().addListener((obs, oldV, isOpen) -> animate(isOpen));
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && isExpanded()) {
                collapse();
                e.consume();
            }
        });
    }

    /**
     * Adds an action row (small FAB + label).
     *
     * @param label action label
     * @param icon action icon, may be {@code null} for a text-only dot
     * @param handler handler run on click, may be {@code null}
     * @return this menu, for chaining
     */
    public M3FabMenu addAction(String label, Node icon, Runnable handler) {
        M3Fab actionFab = new M3Fab(icon == null ? M3Icon.symbol("add") : icon, FabSize.SMALL);
        actionFab.getStyleClass().add("m3-fab-menu-action");
        actionFab.setAccessibleText(label);
        Label text = new Label(label == null ? "" : label);
        text.getStyleClass().add("m3-fab-menu-label");
        HBox row = new HBox(8, text, actionFab);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.getStyleClass().add("m3-fab-menu-row");
        actionFab.setOnAction(e -> {
            if (handler != null) {
                handler.run();
            }
            collapse();
        });
        actionsBox.getChildren().add(row);
        return this;
    }

    /** Returns the expanded property. */
    public BooleanProperty expandedProperty() {
        return expanded;
    }

    /** Returns whether the menu is open. */
    public boolean isExpanded() {
        return expanded.get();
    }

    /**
     * Sets the expanded state.
     *
     * @param expanded {@code true} to open
     */
    public void setExpanded(boolean expanded) {
        this.expanded.set(expanded);
    }

    /** Toggles the menu. */
    public void toggle() {
        setExpanded(!isExpanded());
    }

    /** Opens the menu. */
    public void expand() {
        setExpanded(true);
    }

    /** Closes the menu. */
    public void collapse() {
        setExpanded(false);
    }

    private void animate(boolean open) {
        RotateTransition rotate =
                new RotateTransition(Duration.millis(MotionTokens.DURATION_SHORT_3), mainFab.getGraphic());
        if (mainFab.getGraphic() != null) {
            rotate.setToAngle(open ? 45 : 0);
            rotate.setInterpolator(MotionTokens.emphasized());
            rotate.play();
        }
        int count = actionsBox.getChildren().size();
        if (open) {
            actionsBox.setVisible(true);
        }
        for (int i = 0; i < count; i++) {
            Node row = actionsBox.getChildren().get(i);
            Object oldF = row.getProperties().get("m3fx.fab-fade");
            if (oldF instanceof FadeTransition running) running.stop();
            Object oldS = row.getProperties().get("m3fx.fab-slide");
            if (oldS instanceof TranslateTransition running) running.stop();

            // Nearest row first on open (bottom-up), top row first on close; wider stagger.
            int delay = open ? (count - 1 - i) * 45 : i * 20;
            int millis = open ? MotionTokens.DURATION_SHORT_4 : MotionTokens.DURATION_SHORT_2;
            FadeTransition fade = new FadeTransition(Duration.millis(millis), row);
            fade.setDelay(Duration.millis(delay));
            fade.setFromValue(open ? 0 : row.getOpacity());
            fade.setToValue(open ? 1 : 0);
            fade.setInterpolator(MotionTokens.emphasizedDecelerate());
            row.getProperties().put("m3fx.fab-fade", fade);

            TranslateTransition slide = new TranslateTransition(Duration.millis(millis), row);
            slide.setDelay(Duration.millis(delay));
            slide.setInterpolator(MotionTokens.emphasizedDecelerate());
            if (open) {
                slide.setFromY(20);
                slide.setToY(0);
            } else {
                slide.setFromY(row.getTranslateY());
                slide.setToY(12);
            }
            row.getProperties().put("m3fx.fab-slide", slide);

            if (!open && i == count - 1) {
                fade.setOnFinished(e -> actionsBox.setVisible(false));
            }
            fade.play();
            slide.play();
        }
        if (!open && count == 0) {
            actionsBox.setVisible(false);
        }
    }
}
