package io.m3fx.controls.segmented;

import io.m3fx.controls.icon.M3Icon;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import java.util.ArrayList;
import java.util.List;

/**
 * M3 segmented button: a connected row of 2-5 single-select segments.
 *
 * <p>See
 * <a href="https://m3.material.io/components/segmented-buttons/overview">M3 segmented
 * buttons</a>. Height 40dp, full-shape container with per-segment dividers, check icon on the
 * selected segment. Includes the Expressive button-group look via the same connected styling.
 */
public class M3SegmentedButton extends HBox {

    private final IntegerProperty selectedIndex = new SimpleIntegerProperty(this, "selectedIndex", -1);
    private final ToggleGroup group = new ToggleGroup();
    private final ObservableList<String> segments = FXCollections.observableArrayList();
    private final List<ToggleButton> segmentButtons = new ArrayList<>();

    /** Creates an empty segmented button. */
    public M3SegmentedButton() {
        this(List.of());
    }

    /**
     * Creates a segmented button.
     *
     * @param segments segment labels
     */
    public M3SegmentedButton(List<String> segments) {
        getStyleClass().add("m3-segmented");
        setAlignment(Pos.CENTER);
        setSegments(segments);
        selectedIndex.addListener((obs, oldV, newV) -> selectInGroup(newV.intValue()));
    }

    /**
     * Sets the segment labels (rebuilds segments, 2-5 expected).
     *
     * @param segments labels
     */
    public void setSegments(List<String> segments) {
        getChildren().clear();
        segmentButtons.clear();
        this.segments.setAll(segments == null ? List.of() : segments);
        for (int i = 0; i < this.segments.size(); i++) {
            final int index = i;
            ToggleButton segment = new ToggleButton(this.segments.get(i));
            segment.getStyleClass().add("m3-segmented-segment");
            if (i == 0) {
                segment.getStyleClass().add("m3-segmented-first");
            }
            if (i == this.segments.size() - 1) {
                segment.getStyleClass().add("m3-segmented-last");
            }
            segment.setToggleGroup(group);
            segment.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(segment, Priority.ALWAYS);
            segment.setOnAction(e -> setSelectedIndex(index));
            segmentButtons.add(segment);
            getChildren().add(segment);
        }
        refreshCheckmarks();
    }

    /** Returns the segment labels. */
    public ObservableList<String> getSegments() {
        return segments;
    }

    /** Returns the selected index property (-1 = none). */
    public IntegerProperty selectedIndexProperty() {
        return selectedIndex;
    }

    /** Returns the selected index. */
    public int getSelectedIndex() {
        return selectedIndex.get();
    }

    /**
     * Sets the selected index.
     *
     * @param selectedIndex the new index, or -1 to clear
     */
    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex.set(selectedIndex);
    }

    private void selectInGroup(int index) {
        if (index >= 0 && index < group.getToggles().size()) {
            group.selectToggle(group.getToggles().get(index));
        } else {
            group.selectToggle(null);
        }
        refreshCheckmarks();
    }

    /** Shows the M3 check icon on the selected segment only. */
    private void refreshCheckmarks() {
        for (int i = 0; i < segmentButtons.size(); i++) {
            ToggleButton segment = segmentButtons.get(i);
            if (i == getSelectedIndex()) {
                M3Icon check = M3Icon.symbol("check");
                check.setMinSize(18, 18);
                check.setPrefSize(18, 18);
                check.setMaxSize(18, 18);
                segment.setGraphic(check);
            } else {
                segment.setGraphic(null);
            }
        }
    }
}
