package io.m3fx.controls.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Breakpoint checks for {@link WindowSizeClass} (no FX toolkit needed). */
class WindowSizeClassTest {

    @Test
    void widthBreakpoints() {
        assertEquals(WindowSizeClass.COMPACT, WindowSizeClass.forWidth(0));
        assertEquals(WindowSizeClass.COMPACT, WindowSizeClass.forWidth(599));
        assertEquals(WindowSizeClass.MEDIUM, WindowSizeClass.forWidth(600));
        assertEquals(WindowSizeClass.MEDIUM, WindowSizeClass.forWidth(839));
        assertEquals(WindowSizeClass.EXPANDED, WindowSizeClass.forWidth(840));
        assertEquals(WindowSizeClass.EXPANDED, WindowSizeClass.forWidth(2560));
    }

    @Test
    void heightBreakpoints() {
        assertEquals(WindowSizeClass.COMPACT, WindowSizeClass.forHeight(479));
        assertEquals(WindowSizeClass.MEDIUM, WindowSizeClass.forHeight(480));
        assertEquals(WindowSizeClass.MEDIUM, WindowSizeClass.forHeight(899));
        assertEquals(WindowSizeClass.EXPANDED, WindowSizeClass.forHeight(900));
    }
}
