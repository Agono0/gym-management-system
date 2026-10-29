package io.m3fx.controls.segmented;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

class M3SegmentedButtonTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
    }

    @Test
    void buildsSegmentsWithSingleSelection() {
        M3SegmentedButton segmented = new M3SegmentedButton(List.of("Day", "Week", "Month"));

        assertEquals(3, segmented.getSegments().size());
        assertEquals(-1, segmented.getSelectedIndex(), "No segment selected initially");

        segmented.setSelectedIndex(1);
        assertEquals(1, segmented.getSelectedIndex());
    }

    @Test
    void selectionRestoresAfterRebuild() {
        M3SegmentedButton segmented = new M3SegmentedButton(List.of("Day", "Week"));
        segmented.setSelectedIndex(0);

        segmented.setSegments(List.of("Day", "Week", "Month"));

        // The previously selected label stays selected after the segments are rebuilt.
        assertEquals(0, segmented.getSelectedIndex());
        assertTrue(getSegment(segmented, 0).isSelected(), "First segment re-selected after rebuild");
        assertFalse(getSegment(segmented, 2).isSelected());
    }

    @Test
    void checkIconOnlyOnSelectedSegment() {
        M3SegmentedButton segmented = new M3SegmentedButton(List.of("Day", "Week", "Month"));
        segmented.setSelectedIndex(1);

        assertTrue(getSegment(segmented, 1).isSelected());
        assertNull(getSegment(segmented, 0).getGraphic(), "Unselected segment has no check icon");
        assertNull(getSegment(segmented, 2).getGraphic(), "Unselected segment has no check icon");
        assertTrue(getSegment(segmented, 1).getGraphic() != null, "Selected segment shows the check icon");

        segmented.setSelectedIndex(-1);
        assertNull(getSegment(segmented, 1).getGraphic(), "Clearing selection removes the check icon");
    }

    @Test
    void clickingSegmentSelectsIt() {
        M3SegmentedButton segmented = new M3SegmentedButton(List.of("Day", "Week"));
        getSegment(segmented, 1).fire();

        assertEquals(1, segmented.getSelectedIndex(), "Segment action updates selectedIndex");
    }

    private static ToggleButton getSegment(M3SegmentedButton segmented, int index) {
        Node node = segmented.getChildren().get(index);
        return (ToggleButton) node;
    }
}
