package io.m3fx.controls.glass;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for interactive 3D perspective tilt, draggable lenses, and Apple button properties.
 */
class GlassTiltAndDragTest {

    private static final double EPS = 1e-6;

    @Test
    void tiltPropertiesDefaultAndChange() {
        M3GlassPane pane = new M3GlassPane();
        assertFalse(pane.isTilt(), "tilt defaults to false");
        assertEquals(5.0, pane.getTiltFactor(), EPS, "tilt factor defaults to 5 degrees");
        assertEquals(400.0, pane.getTiltEase(), EPS, "tilt ease defaults to 400 ms");

        pane.setTilt(true);
        pane.setTiltFactor(8.5);
        pane.setTiltEase(250.0);

        assertTrue(pane.isTilt());
        assertEquals(8.5, pane.getTiltFactor(), EPS);
        assertEquals(250.0, pane.getTiltEase(), EPS);
    }

    @Test
    void draggablePropertiesDefaultAndChange() {
        M3GlassPane pane = new M3GlassPane();
        assertFalse(pane.isDraggable(), "draggable defaults to false");

        pane.setDraggable(true);
        assertTrue(pane.isDraggable());
    }

    @Test
    void buttonModePropertiesDefaultAndChange() {
        M3GlassPane pane = new M3GlassPane();
        assertFalse(pane.isButtonMode(), "buttonMode defaults to false");

        pane.setButtonMode(true);
        assertTrue(pane.isButtonMode());

        M3GlassPane btn = M3GlassPane.button("Click Me");
        assertTrue(btn.isButtonMode(), "button factory activates buttonMode");
        assertTrue(btn.isInteractive(), "button factory activates interactive");
    }

    @Test
    void glassEffectParamsWithTiltAndDrag() {
        GlassEffectParams params = new GlassEffectParams()
                .tilt(true)
                .tilt(10.0, 300.0)
                .draggable(true)
                .asButton();

        assertTrue(params.isTilt());
        assertEquals(10.0, params.tiltFactor(), EPS);
        assertEquals(300.0, params.tiltEase(), EPS);
        assertTrue(params.isDraggable());
        assertTrue(params.isButtonMode());
        assertTrue(params.isInteractive());

        M3GlassPane pane = M3GlassPane.of(params);
        assertTrue(pane.isTilt());
        assertEquals(10.0, pane.getTiltFactor(), EPS);
        assertEquals(300.0, pane.getTiltEase(), EPS);
        assertTrue(pane.isDraggable());
        assertTrue(pane.isButtonMode());
    }
}

