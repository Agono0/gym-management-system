package io.m3fx.controls.command;

import io.m3fx.controls.icon.M3Icon;
import io.m3fx.core.token.TypeScale;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * Global Spotlight Command Palette (Ctrl+K / Cmd+K) for high-speed operator actions,
 * fuzzy keyword filtering, categorized navigation, and keyboard arrow execution.
 */
public class M3CommandPalette extends StackPane {

    /** Action command descriptor. */
    public static class CommandAction {
        public final String category;
        public final String title;
        public final String subtitle;
        public final String iconName;
        public final Runnable runAction;

        public CommandAction(String category, String title, String subtitle, String iconName, Runnable runAction) {
            this.category = category;
            this.title = title;
            this.subtitle = subtitle;
            this.iconName = iconName;
            this.runAction = runAction;
        }
    }

    private final ObservableList<CommandAction> masterActions = FXCollections.observableArrayList();
    private final List<CommandAction> filteredActions = new ArrayList<>();

    private final StackPane scrim = new StackPane();
    private final VBox card = new VBox(0);
    private final TextField searchField = new TextField();
    private final VBox resultsContainer = new VBox(2);
    private final ScrollPane scrollPane = new ScrollPane(resultsContainer);

    private int selectedIndex = 0;
    private final BooleanProperty open = new SimpleBooleanProperty(this, "open", false);
    private Node previousFocus;

    /** Creates a command palette overlay. */
    public M3CommandPalette() {
        getStyleClass().add("m3-command-palette-overlay");
        setAlignment(Pos.CENTER);
        setVisible(false);
        setManaged(false);

        // Scrim background: two stacked 32% scrim token layers (~54%) approximate the previous
        // hardcoded 55% black while staying on -md-sys-color lookups.
        scrim.setStyle("-fx-background-color: -md-sys-color-scrim, -md-sys-color-scrim;");
        scrim.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> close());

        // Card Container
        card.getStyleClass().add("m3-command-palette-card");
        card.setMaxWidth(580);
        card.setMaxHeight(420);

        // Header Search Field
        searchField.getStyleClass().add("m3-command-palette-input");
        searchField.setPromptText("Type a command, search members, or book equipment... (ESC to close)");
        searchField.setFont(Font.font(
                TypeScale.DEFAULT_FAMILY, FontWeight.NORMAL, TypeScale.BODY_MEDIUM.size()));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        M3Icon searchIcon = M3Icon.symbol("search");
        Label escBadge = new Label("ESC");
        escBadge.getStyleClass().add("m3-command-palette-badge");

        HBox searchRow = new HBox(12, searchIcon, searchField, escBadge);
        searchRow.getStyleClass().add("m3-command-palette-header");
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setPadding(new Insets(4, 16, 4, 16));

        // Results Scroll Container
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPadding(new Insets(8));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        card.getChildren().addAll(searchRow, scrollPane);
        getChildren().addAll(scrim, card);

        initKeyboardControls();

        searchField.textProperty().addListener((obs, o, n) -> filterResults(n));
        open.addListener((obs, o, n) -> {
            setVisible(n);
            setManaged(n);
            if (n) {
                previousFocus = getScene() != null ? getScene().getFocusOwner() : null;
                searchField.setText("");
                filterResults("");
                searchField.requestFocus();
            } else {
                Node owner = previousFocus;
                previousFocus = null;
                if (owner != null && owner.getScene() != null) {
                    owner.requestFocus();
                }
            }
        });
    }

    /** Registers a global accelerator on the scene for Ctrl+K / Cmd+K. */
    public static void attachGlobalShortcut(Scene scene, M3CommandPalette palette) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if ((e.isControlDown() || e.isMetaDown()) && e.getCode() == KeyCode.K) {
                e.consume();
                palette.toggle();
            }
        });
    }

    /** Toggles visibility of the command palette. */
    public void toggle() {
        if (isOpen()) {
            close();
        } else {
            show();
        }
    }

    /** Opens the command palette. */
    public void show() {
        open.set(true);
    }

    /** Closes the command palette. */
    public void close() {
        open.set(false);
    }

    /** Adds an action to the command palette catalog. */
    public void registerAction(String category, String title, String subtitle, String iconName, Runnable action) {
        masterActions.add(new CommandAction(category, title, subtitle, iconName, action));
    }

    private void initKeyboardControls() {
        searchField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                close();
            } else if (e.getCode() == KeyCode.DOWN) {
                e.consume();
                moveSelection(1);
            } else if (e.getCode() == KeyCode.UP) {
                e.consume();
                moveSelection(-1);
            } else if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                executeSelected();
            }
        });
    }

    private void moveSelection(int delta) {
        if (filteredActions.isEmpty()) return;
        selectedIndex = Math.max(0, Math.min(filteredActions.size() - 1, selectedIndex + delta));
        renderResults();
    }

    private void executeSelected() {
        if (selectedIndex >= 0 && selectedIndex < filteredActions.size()) {
            CommandAction act = filteredActions.get(selectedIndex);
            close();
            if (act.runAction != null) {
                act.runAction.run();
            }
        }
    }

    private void filterResults(String query) {
        filteredActions.clear();
        String q = (query == null) ? "" : query.trim().toLowerCase();

        for (CommandAction a : masterActions) {
            if (q.isEmpty()
                    || a.title.toLowerCase().contains(q)
                    || a.category.toLowerCase().contains(q)
                    || (a.subtitle != null && a.subtitle.toLowerCase().contains(q))) {
                filteredActions.add(a);
            }
        }
        selectedIndex = 0;
        renderResults();
    }

    private void renderResults() {
        resultsContainer.getChildren().clear();

        if (filteredActions.isEmpty()) {
            Label empty = new Label("No matching commands found");
            empty.setStyle("-fx-text-fill: -md-sys-color-on-surface-variant; -fx-padding: 20;");
            resultsContainer.getChildren().add(empty);
            return;
        }

        String currentCat = "";

        for (int i = 0; i < filteredActions.size(); i++) {
            CommandAction act = filteredActions.get(i);

            // Category Header
            if (!act.category.equalsIgnoreCase(currentCat)) {
                currentCat = act.category;
                Label catHeader = new Label(currentCat.toUpperCase());
                catHeader.getStyleClass().add("m3-command-palette-cat-header");
                resultsContainer.getChildren().add(catHeader);
            }

            boolean isSelected = (i == selectedIndex);
            final int actionIndex = i;

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.getStyleClass().add("m3-command-palette-row");
            if (isSelected) {
                row.getStyleClass().add("selected");
            }

            M3Icon icon = M3Icon.symbol(act.iconName != null ? act.iconName : "arrow_forward");

            VBox textCol = new VBox(2);
            Label title = new Label(act.title);
            title.getStyleClass().add("m3-command-palette-title");

            Label sub = new Label(act.subtitle);
            sub.getStyleClass().add("m3-command-palette-sub");

            textCol.getChildren().addAll(title, sub);
            HBox.setHgrow(textCol, Priority.ALWAYS);

            Label runHint = new Label(isSelected ? "↵" : "");
            runHint.getStyleClass().add("m3-command-palette-run-hint");

            row.getChildren().addAll(icon, textCol, runHint);

            row.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
                selectedIndex = actionIndex;
                renderResults();
            });

            row.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                selectedIndex = actionIndex;
                executeSelected();
            });

            resultsContainer.getChildren().add(row);
        }
    }

    public ObservableList<CommandAction> getMasterActions() {
        return masterActions;
    }

    public BooleanProperty openProperty() {
        return open;
    }

    public boolean isOpen() {
        return open.get();
    }
}

