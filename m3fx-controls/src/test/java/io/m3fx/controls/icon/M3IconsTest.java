package io.m3fx.controls.icon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** Catalog integrity for {@link M3Icons} (path strings only — no FX toolkit needed). */
class M3IconsTest {

    @Test
    void catalogHasCoreIcons() {
        for (String name : new String[] {"menu", "close", "search", "add", "home", "check"}) {
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
}
