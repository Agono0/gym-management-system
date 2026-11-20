package io.m3fx.controls.iconbutton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.m3fx.controls.common.M3Tooltip;
import io.m3fx.controls.icon.M3Icon;
import javafx.application.Platform;
import javafx.scene.control.Tooltip;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the icon button accessibility behavior: the announced text is derived from the
 * {@link M3Icon} graphic's symbol name (caller-written text always wins) and the tooltip with the
 * announced text is installed automatically unless the caller already installed one.
 */
class M3IconButtonA11yTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already started
        }
    }

    @Test
    void accessibleTextIsDerivedFromTheIconName() {
        M3IconButton button = M3IconButton.standard(M3Icon.symbol("delete"));
        assertEquals("Delete", button.getAccessibleText(),
                "the symbol name announces capitalized");

        M3IconButton close = M3IconButton.filled(M3Icon.symbol("close"));
        assertEquals("Close", close.getAccessibleText());
    }

    @Test
    void accessibleTextFollowsIconChangesLive() {
        M3Icon icon = M3Icon.symbol("search");
        M3IconButton button = M3IconButton.standard(icon);
        assertEquals("Search", button.getAccessibleText());

        icon.setSymbolName("settings");
        assertEquals("Settings", button.getAccessibleText(),
                "renaming the icon re-derives the announcement");

        button.setGraphic(M3Icon.symbol("delete"));
        assertEquals("Delete", button.getAccessibleText(),
                "swapping the graphic re-derives from the new icon");
    }

    @Test
    void iconWithoutNameLeavesTheAccessibleTextEmpty() {
        M3IconButton button = M3IconButton.standard(M3Icon.symbol(null));
        assertNull(button.getAccessibleText(),
                "no generic label: unlabeled icon buttons are a caller-visible gap");
    }

    @Test
    void callerAccessibleTextAlwaysWins() {
        M3IconButton button = M3IconButton.standard(M3Icon.symbol("delete"));
        button.setAccessibleText("Remove item");
        assertEquals("Remove item", button.getAccessibleText(),
                "the caller label must not be overwritten by derivation");

        // A later icon change must not clobber the caller label either.
        button.setGraphic(M3Icon.symbol("close"));
        assertEquals("Remove item", button.getAccessibleText());
    }

    @Test
    void autoTooltipFollowsTheAnnouncedText() {
        M3IconButton button = M3IconButton.standard(M3Icon.symbol("delete"));
        Tooltip tooltip = button.getTooltip();
        assertEquals("Delete", tooltip == null ? null : tooltip.getText(),
                "the tooltip mirrors the derived label");
        assertSame(tooltip, button.getTooltip(), "one tooltip installed");

        button.setAccessibleText("Remove item");
        assertEquals("Remove item", button.getTooltip().getText(),
                "an overridden label updates the auto tooltip");
    }

    @Test
    void callerInstalledTooltipIsNeverDoubled() {
        M3IconButton button = M3IconButton.standard(M3Icon.symbol("delete"));
        Tooltip callerTooltip = M3Tooltip.plain("Custom tip");
        button.setTooltip(callerTooltip);
        // Trigger a re-derive cycle (icon rename) - the caller tooltip must survive untouched.
        button.setGraphic(M3Icon.symbol("close"));
        assertSame(callerTooltip, button.getTooltip(), "no double tooltips");
        assertEquals("Custom tip", button.getTooltip().getText());
        assertFalse(M3Tooltip.plain("x") == button.getTooltip());
    }

    @Test
    void installIfAbsentNeverDuplicates() {
        javafx.scene.control.Label plain = new javafx.scene.control.Label("target");
        M3Tooltip.installIfAbsent(plain, "First");
        assertEquals("First", plain.getTooltip().getText());
        M3Tooltip.installIfAbsent(plain, "Second");
        assertEquals("First", plain.getTooltip().getText(),
                "an existing install (ours) is not replaced");
        M3Tooltip.installIfAbsent(plain, "  ");
        assertEquals("First", plain.getTooltip().getText(), "blank text installs nothing");
    }
}
