package io.m3fx.controls.icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Render-level catalog check: every bundled name must render its own glyph (accessible text is
 * the name, never "unknown icon: ...") and produce parseable SVGPath geometry. Complements
 * {@link M3IconsTest}, which checks the path strings without the FX toolkit. Also pins the B14
 * property contract: {@link M3Icon#symbol(String)} routes through {@code symbolNameProperty} so
 * property-based consumers observe the name.
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

    private static String svgContent(M3Icon icon) {
        return ((SVGPath) icon.getChildren().get(0)).getContent();
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

    @Test
    void symbolFactoryPublishesTheNameOnTheSymbolNameProperty() {
        M3Icon icon = M3Icon.symbol("check");
        assertEquals("check", icon.getSymbolName(),
                "M3Icon.symbol must set symbolNameProperty (B14 gap)");
        assertEquals(M3Icons.pathFor("check"), svgContent(icon));
    }

    @Test
    void propertyChangeRerendersTheGlyph() {
        M3Icon icon = new M3Icon();
        assertEquals(0, icon.getChildren().size(), "an empty icon renders no glyph");

        icon.setSymbolName("add");
        assertEquals(M3Icons.pathFor("add"), svgContent(icon));
        assertEquals("add", icon.getAccessibleText());

        icon.setSymbolName("star");
        assertEquals(M3Icons.pathFor("star"), svgContent(icon), "changing the name re-renders");
        assertEquals("star", icon.getAccessibleText());

        icon.setSymbolName(null);
        assertEquals("", svgContent(icon), "null clears the glyph (documented clear)");
        assertNull(icon.getAccessibleText());
    }

    @Test
    void unknownNameRendersPlaceholderAndWarnsExactlyOnce() {
        String unknown = "b14-unknown-probe-icon";
        PrintStream originalErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(captured, true));
            M3Icon icon = M3Icon.symbol(unknown);
            assertEquals("unknown icon: " + unknown, icon.getAccessibleText());
            assertEquals(M3Icon.placeholderPathForTesting(), svgContent(icon),
                    "unknown names render the neutral ring placeholder");
            // A second icon with the same unknown name must not warn again.
            M3Icon second = M3Icon.symbol(unknown);
            assertEquals("unknown icon: " + unknown, second.getAccessibleText());
        } finally {
            System.setErr(originalErr);
        }
        String err = captured.toString();
        assertEquals(1, err.split("unknown icon name", -1).length - 1,
                "exactly one warning per distinct unknown name, got: " + err);
    }

    @Test
    void ofSvgAndOfGlyphDoNotTouchTheSymbolNameProperty() {
        M3Icon svg = M3Icon.ofSvg("M4 4h10v10H4z");
        assertNull(svg.getSymbolName(), "ofSvg content is raw, not a catalog symbol");
        assertEquals("M4 4h10v10H4z", svgContent(svg));

        M3Icon glyph = M3Icon.ofGlyph("A");
        assertNull(glyph.getSymbolName(), "ofGlyph content is glyph text, not a catalog symbol");
        assertTrue(glyph.getChildren().get(0) instanceof javafx.scene.control.Label,
                "ofGlyph renders a glyph label");
    }
}
