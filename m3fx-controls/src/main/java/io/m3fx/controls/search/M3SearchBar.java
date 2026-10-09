package io.m3fx.controls.search;

import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.internal.OverlayLayer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/**
 * M3 search bar with a suggestions view.
 *
 * <p>See <a href="https://m3.material.io/components/search/overview">M3 search</a>. Collapsed bar
 * 56dp, full shape, surface-container-high; expanded suggestions appear in a popup with matching
 * width. Suggestions filter as the user types.
 */
public class M3SearchBar extends VBox {

    private final TextField input = new TextField();
    private final HBox bar = new HBox(8);
    private final ObservableList<String> suggestions = FXCollections.observableArrayList();
    private final Popup suggestionsPopup = new Popup();
    private final ListView<String> suggestionsView = new ListView<>(suggestions);
    private final VBox suggestionsPanel = new VBox();
    private final ObjectProperty<OverlayLayer> overlay =
            new SimpleObjectProperty<>(this, "overlay");

    /** Creates an empty search bar. */
    public M3SearchBar() {
        this("Search");
    }

    /**
     * Creates a search bar.
     *
     * @param prompt prompt text
     */
    public M3SearchBar(String prompt) {
        getStyleClass().add("m3-search-bar");
        bar.getStyleClass().add("m3-search-field");
        bar.setAlignment(Pos.CENTER_LEFT);
        M3Icon searchIcon = M3Icon.symbol("search");
        input.setPromptText(prompt == null ? "" : prompt);
        input.getStyleClass().add("m3-search-input");
        HBox.setHgrow(input, Priority.ALWAYS);
        bar.getChildren().addAll(searchIcon, input);
        getChildren().add(bar);
        suggestionsView.getStyleClass().add("m3-search-suggestions");
        suggestionsView.setPrefHeight(240);
        suggestionsPanel.getStyleClass().add("m3-search-panel");
        suggestionsPanel.getChildren().add(suggestionsView);
        suggestionsPopup.getContent().add(suggestionsPanel);
        suggestionsPopup.setAutoHide(true);
        input.textProperty().addListener((obs, oldV, newV) -> showSuggestions(newV));
        input.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                hideSuggestions();
            }
        });
        suggestionsView.setOnMouseClicked(e -> {
            String selected = suggestionsView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                input.setText(selected);
                hideSuggestions();
            }
        });
        setFocusTraversable(false);
    }

    /** Returns the overlay layer property (anchored suggestions with scrim when set). */
    public ObjectProperty<OverlayLayer> overlayProperty() {
        return overlay;
    }

    /** Returns the overlay layer, or {@code null} for popup suggestions. */
    public OverlayLayer getOverlay() {
        return overlay.get();
    }

    /**
     * Sets the overlay layer used to show suggestions anchored under the bar with a scrim.
     * When {@code null} (default), a plain popup is used instead.
     *
     * @param overlay the overlay layer, may be {@code null}
     */
    public void setOverlay(OverlayLayer overlay) {
        this.overlay.set(overlay);
    }

    /** Returns the query text. */
    public String getQuery() {
        return input.getText();
    }

    /**
     * Sets the query text.
     *
     * @param query query text
     */
    public void setQuery(String query) {
        input.setText(query);
    }

    /** Returns the editable suggestions list. */
    public ObservableList<String> getSuggestions() {
        return suggestions;
    }

    /**
     * Sets the trailing node (avatar, close button, filter icon).
     *
     * @param trailing trailing node, may be {@code null}
     */
    public void setTrailing(Node trailing) {
        bar.getChildren().removeIf(n -> "m3-search-trailing".equals(n.getId()));
        if (trailing != null) {
            trailing.setId("m3-search-trailing");
            bar.getChildren().add(trailing);
        }
    }

    private void showSuggestions(String query) {
        if (suggestions.isEmpty() || !input.isFocused()) {
            hideSuggestions();
            return;
        }
        if (query != null && !query.isEmpty()) {
            suggestionsView.getItems().setAll(
                    suggestions.stream()
                            .filter(s -> s.toLowerCase().contains(query.toLowerCase()))
                            .toList());
        } else {
            suggestionsView.getItems().setAll(suggestions);
        }
        if (getOverlay() != null) {
            getOverlay().showAnchored(bar, suggestionsPanel, null);
            return;
        }
        if (!suggestionsPopup.isShowing() && getScene() != null && getScene().getWindow() != null) {
            if (!suggestionsPopup.getContent().contains(suggestionsPanel)) {
                suggestionsPopup.getContent().setAll(suggestionsPanel);
            }
            javafx.geometry.Bounds bounds = bar.localToScreen(bar.getBoundsInLocal());
            if (bounds != null) {
                suggestionsPopup.show(
                        input, bounds.getMinX(), bounds.getMaxY() + 4);
            }
        }
    }

    private void hideSuggestions() {
        suggestionsPopup.hide();
        if (getOverlay() != null) {
            getOverlay().hideAnchored();
        }
    }
}
