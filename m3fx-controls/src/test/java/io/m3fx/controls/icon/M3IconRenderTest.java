package io.m3fx.controls.icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Render-level catalog check: every bundled name must render its own glyph (accessible text is
 * the name, never "unknown icon: ...") and produce parseable SVGPath geometry. Complements
 * {@link M3IconsTest}, which checks the path strings without the FX toolkit.
 */
class M3IconRenderTest {

    @BeforeAll
    static void initFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
    }

    @Test
    void everyCatalogNameRendersItsOwnGlyphWithParsableGeometry() {
        for (String name : M3Icons.names()) {
            M3Icon icon = M3Icon.symbol(name);
            assertEquals(name, icon.getAccessibleText(),
                    "'" + name + "' must not announce as an unknown icon");
            SVGPath svg = (SVGPath) icon.getChildren().get(0);
            assertEquals(M3Icons.pathFor(name), svg.getContent());
            // Forces SVGPath to parse the content into geometry; malformed path data fails here.
            assertNotNull(svg.getBoundsInLocal(), "'" + name + "' produced no geometry");
            assertTrue(svg.getBoundsInLocal().getWidth() > 0 && svg.getBoundsInLocal().getHeight() > 0,
                    "'" + name + "' rendered an empty glyph");
        }
    }
}
