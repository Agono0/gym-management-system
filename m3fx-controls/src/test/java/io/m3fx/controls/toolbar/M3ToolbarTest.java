package io.m3fx.controls.toolbar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.toolbar.M3Toolbar.ToolbarStyle;
import io.m3fx.controls.toolbar.M3Toolbar.ToolbarVariant;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class M3ToolbarTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void createsToolbarWithDefaults() {
        M3Toolbar toolbar = new M3Toolbar();
        assertEquals(ToolbarVariant.FLOATING, toolbar.getVariant());
        assertEquals(ToolbarStyle.STANDARD, toolbar.getToolbarStyle());
        assertEquals(Orientation.HORIZONTAL, toolbar.getOrientation());
        assertTrue(toolbar.getStyleClass().contains("m3-toolbar"));
    }

    @Test
    void managesItemsAndOrientation() {
        M3Toolbar toolbar = new M3Toolbar(ToolbarVariant.DOCKED, ToolbarStyle.VIBRANT, Orientation.VERTICAL);
        M3Button btn = M3Button.filled("Action");
        toolbar.getItems().add(btn);

        assertEquals(1, toolbar.getItems().size());
        assertEquals(ToolbarVariant.DOCKED, toolbar.getVariant());
        assertEquals(ToolbarStyle.VIBRANT, toolbar.getToolbarStyle());
        assertEquals(Orientation.VERTICAL, toolbar.getOrientation());
    }
}

