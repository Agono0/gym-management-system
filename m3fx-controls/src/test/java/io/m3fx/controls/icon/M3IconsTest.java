package io.m3fx.controls.icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Catalog integrity for {@link M3Icons} (path strings only — no FX toolkit needed). */
class M3IconsTest {

    /** Upper bound for a single 24px glyph path; the largest bundled entry is ~1500 chars. */
    private static final int MAX_PATH_LENGTH = 4000;

    /** Path-data grammar: Google glyph paths use plain decimals and the SVG command letters. */
    private static final String PATH_GRAMMAR = "[MmZzLlHhVvCcSsQqTtAa0-9 ,.\\- ]+";

    @Test
    void catalogHasCoreIcons() {
        for (String name : new String[] {
            "menu", "close", "search", "add", "home", "check",
            "arrow_drop_down", "arrow_back", "arrow_forward",
            "settings", "keyboard_arrow_down", "chevron_left", "chevron_right"
        }) {
            assertNotNull(M3Icons.pathFor(name), "missing icon: " + name);
        }
        assertNull(M3Icons.pathFor("no-such-icon"));
    }

    @Test
    void everyPathIsNonBlank() {
        assertFalse(M3Icons.names().isEmpty());
        for (String name : M3Icons.names()) {
            String path = M3Icons.pathFor(name);
            if (path == null || path.isBlank() || !path.contains(" ")) {
                throw new AssertionError("bad path for icon: " + name);
            }
        }
    }

    /**
     * Every catalog entry resolves to well-formed path data: starts with a move command, uses
     * only SVG path-grammar characters, has no stray whitespace, and stays within a sane length
     * bound (a hand-invented or truncated path fails here).
     */
    @Test
    void everyNameResolvesToWellFormedPathData() {
        for (String name : M3Icons.names()) {
            String path = M3Icons.pathFor(name);
            assertNotNull(path, "path data missing for '" + name + "'");
            assertTrue(path.startsWith("M") || path.startsWith("m"),
                    "path for '" + name + "' must start with a move command");
            assertTrue(path.matches(PATH_GRAMMAR),
                    "path for '" + name + "' contains non-path characters");
            assertEquals(path.trim(), path, "path for '" + name + "' has stray whitespace");
            assertFalse(path.contains("\n"), "path for '" + name + "' contains a line break");
            assertTrue(path.length() <= MAX_PATH_LENGTH,
                    "path for '" + name + "' is unreasonably long: " + path.length());
            assertTrue(path.length() >= 4, "path for '" + name + "' is too short to be a glyph");
        }
    }

    /**
     * Pins every icon name the app audit (task B3) found in
     * {@code grep -rn "M3Icon.symbol(" src/main/java} plus the gym/common set, so the
     * app-critical names can never regress to placeholder rings again.
     */
    @Test
    void requiredAppAndGymIconNamesResolve() {
        String[] required = {
            // Names the gym app / gallery already pass to M3Icon.symbol(...):
            "fitness", "pan_tool", "play_arrow", "water_drop", "3d_rotation",
            "home", "person", "group", "check", "close", "add", "remove",
            "arrow_back", "arrow_forward", "arrow_drop_down", "chevron_left", "chevron_right",
            "menu", "refresh", "settings", "search", "star", "favorite", "delete", "edit",
            "stats", "info", "money", "dark", "light", "calendar", "bolt", "back",
            // Gym / common set (login eye toggle and lock replace raw SVG constants):
            "visibility", "visibility_off", "lock",
            "pause", "stop", "stop_circle", "timer",
            "monitor_heart", "scale", "monitor_weight",
            "directions_run", "sports_gymnastics", "logout", "help",
        };
        for (String name : required) {
            assertNotNull(M3Icons.pathFor(name), "required icon missing from catalog: " + name);
        }
    }

    @Test
    void fitnessAliasRendersTheFitnessCenterGlyph() {
        // "fitness" is not a Material icon name; the catalog maps it to fitness_center so the
        // gallery/shell request renders the dumbbell instead of a placeholder ring.
        assertEquals(M3Icons.pathFor("fitness_center"), M3Icons.pathFor("fitness"));
    }
}
