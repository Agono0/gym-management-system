package com.gym.ui;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.progress.M3CircularProgress;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * Shared UI plumbing for the Java-built screens — the single home of the helpers that used
 * to be copy-pasted across the controllers: id-based node lookup, the feedback overlay
 * install, the confirm-dialog factory, the loading/empty/error state builders, the form-row
 * builder and the theme looked-up-color resolver.
 *
 * <p>Layer contract (docs/SPEC_NOTES/app-ui-foundation.md): pure UI plumbing only — no SQL,
 * no business rules. Screens call the DAOs themselves (DECISIONS.md 77) and render the DAOs'
 * {@code ValidationException} messages (DECISIONS.md 78); this class never decides either.
 *
 * <p>Thread discipline: every method here touches the scene graph, so every method must be
 * called on the JavaFX Application Thread. DAO work belongs in {@link Data} background tasks.
 */
public final class UiKit {

    private UiKit() {
    }

    /**
     * Id-based tree search (no CSS dependency, unlike {@link Node#lookup}, which cannot reach
     * across unrelated sub-graphs such as the shell's rail). Returns the first node with the
     * given JavaFX id, or {@code null} when no node matches.
     */
    public static Node findById(Parent parent, String id) {
        if (id.equals(parent.getId())) {
            return parent;
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (id.equals(child.getId())) {
                return child;
            }
            if (child instanceof Parent childParent) {
                Node found = findById(childParent, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Creates the screen's shared {@link M3Overlay} feedback layer and attaches it to the
     * given root pane so snackbars and dialogs cover the whole screen. Replaces the
     * per-controller {@code installFeedbackLayer} copies; pass the screen's root
     * {@code StackPane} (the overlay fills it). Returns the installed overlay for the screen
     * to show snackbars/dialogs through.
     */
    public static M3Overlay installFeedback(Pane root) {
        M3Overlay overlay = new M3Overlay();
        root.getChildren().add(overlay);
        return overlay;
    }

    /**
     * Shows the destructive-action contract in one call: a confirm dialog with the two
     * standard buttons — the confirm action is the dialog default — plus a cancel that just
     * hides. {@code onConfirm} runs only after the user activates the confirm button.
     */
    public static void confirmDialog(M3Overlay overlay, String title, String body,
                                     String confirmLabel, String cancelLabel, Runnable onConfirm) {
        M3Button cancel = M3Button.text(cancelLabel);
        M3Button confirm = M3Button.text(confirmLabel);
        Label bodyLabel = new Label(body);
        bodyLabel.setWrapText(true);
        M3Dialog dialog = M3Dialog.basic(title, bodyLabel, cancel, confirm);
        dialog.setDefaultAction(confirm::activate);
        confirm.setOnAction(e -> {
            dialog.hide();
            onConfirm.run();
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    /**
     * Real empty state for a content region (anti-slop: a screen never shows a silent blank
     * pane): the message, an optional action button ({@code actionLabel == null} skips it)
     * and a muted icon, centered and sized to fill its container.
     */
    public static VBox emptyState(String message, String actionLabel, Runnable action) {
        M3Button actionButton = actionLabel == null ? null : M3Button.text(actionLabel);
        if (actionButton != null) {
            actionButton.setOnAction(e -> action.run());
        }
        return statePane(M3Icon.symbol("group"), message, actionButton);
    }

    /**
     * Real error state for a content region: the failure cause (usually a DAO
     * {@code ValidationException}/{@code DatabaseException} message, rendered verbatim) and a
     * Retry button wired to {@code retry}, typically the screen's own load method.
     */
    public static VBox errorState(String cause, Runnable retry) {
        M3Button retryButton = new M3Button("Retry", ButtonVariant.FILLED);
        retryButton.setGraphic(M3Icon.symbol("refresh"));
        retryButton.setOnAction(e -> retry.run());
        return statePane(M3Icon.symbol("info"),
                "Something went wrong: " + cause, retryButton);
    }

    /**
     * Real loading state for a content region: an indeterminate circular progress indicator
     * and a "Loading..." caption, centered and sized to fill its container. Uses
     * {@link M3CircularProgress} rather than {@code M3LinearProgress}: the linear skin's
     * indeterminate timeline is currently broken (SPLINE control points outside [0,1],
     * IllegalArgumentException during layout — see the BUGS FOUND report for task C0).
     */
    public static VBox loadingState() {
        M3CircularProgress spinner = new M3CircularProgress();
        Label caption = new Label("Loading...");
        caption.getStyleClass().add("m3-list-supporting");
        VBox pane = new VBox(16, spinner, caption);
        pane.setAlignment(Pos.CENTER);
        return pane;
    }

    /** Shared skeleton of the three state builders above. */
    private static VBox statePane(Node icon, String message, M3Button action) {
        Label text = new Label(message);
        text.setWrapText(true);
        text.getStyleClass().add("m3-list-supporting");
        text.setTextAlignment(TextAlignment.CENTER);
        VBox pane = new VBox(12, icon, text);
        if (action != null) {
            pane.getChildren().add(action);
        }
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(24));
        VBox.setVgrow(pane, Priority.ALWAYS);
        pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return pane;
    }

    /**
     * One labeled form row for entry dialogs: fixed-width field label on the left, the given
     * control growing on the right.
     */
    public static HBox formRow(String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("m3-list-supporting");
        label.setMinWidth(120);
        label.setMaxWidth(120);
        if (field instanceof Region region) {
            HBox.setHgrow(region, Priority.ALWAYS);
        }
        HBox row = new HBox(12, label, field);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * Resolves a looked-up {@code -md-sys-color-*} CSS variable to a concrete {@link Color},
     * so canvas drawing and pseudo-node tinting can use theme roles instead of hardcoded hex
     * ({@code Color.web("#...")}). Returns {@code null} — never a guessed color — when the
     * context has no scene, the variable is malformed, or the CSS engine cannot resolve it.
     *
     * <p>The trick: the CSS engine only expands {@code lookup(...)} chains while processing a
     * real node, so we style a scratch {@code Region} with the variable as its background,
     * attach it to the scene root, run {@code applyCss()} and read back the resolved
     * background fill, then detach the probe. Must run on the FX thread with a shown scene.
     */
    public static Color lookedUpColor(Node context, String cssVariable) {
        if (context.getScene() == null
                || context.getScene().getRoot() == null
                || !(context.getScene().getRoot() instanceof Pane sceneRoot)
                || !Platform.isFxApplicationThread()) {
            return null;
        }
        Region probe = new Region();
        probe.setStyle("-fx-background-color: " + cssVariable + ";");
        try {
            sceneRoot.getChildren().add(probe);
            probe.applyCss();
            if (probe.getBackground() == null || probe.getBackground().getFills().isEmpty()
                    || !(probe.getBackground().getFills().get(0).getFill() instanceof Color color)) {
                return null;
            }
            return color;
        } finally {
            sceneRoot.getChildren().remove(probe);
        }
    }
}
