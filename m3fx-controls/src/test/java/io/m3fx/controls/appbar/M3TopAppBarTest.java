package io.m3fx.controls.appbar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Locks the M3 top app bar contract: the four variants with their spec container heights
 * (small/center-aligned 64dp, medium 112dp, large 152dp), the headline row only on medium/large,
 * the center-aligned spacer behavior, title binding into both labels, and the nav/action wiring.
 *
 * <p>A11y (B15): TOOL_BAR role, a live "title toolbar, n navigation items, m actions"
 * announcement, and the bar itself excluded from the tab-focus cycle.
 */
class M3TopAppBarTest {

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown(); // Toolkit already started
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to initialize");
    }

    @Test
    void variantHeightsMatchTheSpecTokens() {
        assertEquals(64, M3TopAppBar.HEIGHT_SMALL, 1e-9, "md.comp.top-app-bar.small height");
        assertEquals(112, M3TopAppBar.HEIGHT_MEDIUM, 1e-9, "md.comp.top-app-bar.medium height");
        assertEquals(152, M3TopAppBar.HEIGHT_LARGE, 1e-9, "md.comp.top-app-bar.large height");

        M3TopAppBar small = new M3TopAppBar("Title", TopAppBarVariant.SMALL);
        assertEquals(64, small.getMinHeight(), 1e-9);
        assertEquals(64, small.getPrefHeight(), 1e-9);

        M3TopAppBar center = new M3TopAppBar("Title", TopAppBarVariant.CENTER_ALIGNED);
        assertEquals(64, center.getMinHeight(), 1e-9, "center-aligned shares the small height");

        M3TopAppBar medium = new M3TopAppBar("Title", TopAppBarVariant.MEDIUM);
        assertEquals(112, medium.getMinHeight(), 1e-9);
        assertEquals(112, medium.getPrefHeight(), 1e-9);

        M3TopAppBar large = new M3TopAppBar("Title", TopAppBarVariant.LARGE);
        assertEquals(152, large.getMinHeight(), 1e-9);
        assertEquals(152, large.getPrefHeight(), 1e-9);
    }

    @Test
    void variantStyleClassesAndHeadlineRowFollowTheVariant() {
        M3TopAppBar bar = new M3TopAppBar("Title", TopAppBarVariant.SMALL);
        assertTrue(bar.getStyleClass().contains("m3-top-app-bar-small"));
        // Small bars show only the single row (no headline).
        assertEquals(1, bar.getChildren().size());

        bar.setVariant(TopAppBarVariant.MEDIUM);
        assertTrue(bar.getStyleClass().contains("m3-top-app-bar-medium"));
        assertFalse(bar.getStyleClass().contains("m3-top-app-bar-small"));
        assertEquals(2, bar.getChildren().size(), "medium adds the headline row");
        assertTrue(bar.getChildren().get(1).getStyleClass()
                .contains("m3-top-app-bar-headline"));

        bar.setVariant(TopAppBarVariant.LARGE);
        assertTrue(bar.getStyleClass().contains("m3-top-app-bar-large"));
        assertEquals(2, bar.getChildren().size(), "large keeps the headline row");

        bar.setVariant(TopAppBarVariant.CENTER_ALIGNED);
        assertTrue(bar.getStyleClass().contains("m3-top-app-bar-center-aligned"));
        assertEquals(1, bar.getChildren().size(), "center-aligned is single-row again");
    }

    @Test
    void centerAlignedCentersTheTitleWhileOthersLeadAlign() {
        M3TopAppBar center = new M3TopAppBar("Title", TopAppBarVariant.CENTER_ALIGNED);
        HBox row = (HBox) center.getChildren().get(0);
        assertEquals(Pos.CENTER, row.getAlignment(), "center-aligned centers the row");
        // Both spacers grow so the title lands in the middle regardless of nav/action widths.
        Region leftSpacer = (Region) row.getChildren().get(1);
        Region rightSpacer = (Region) row.getChildren().get(3);
        assertTrue(leftSpacer.getMaxWidth() == Double.MAX_VALUE);
        assertTrue(rightSpacer.getMaxWidth() == Double.MAX_VALUE);

        M3TopAppBar small = new M3TopAppBar("Title", TopAppBarVariant.SMALL);
        HBox smallRow = (HBox) small.getChildren().get(0);
        assertEquals(Pos.CENTER_LEFT, smallRow.getAlignment());
        Region smallLeftSpacer = (Region) smallRow.getChildren().get(1);
        assertEquals(0, smallLeftSpacer.getMaxWidth(), 1e-9,
                "non-centered variants collapse the left spacer");
    }

    @Test
    void titleBindingUpdatesTheVisibleLabel() {
        M3TopAppBar bar = new M3TopAppBar("Members", TopAppBarVariant.SMALL);
        HBox row = (HBox) bar.getChildren().get(0);
        Label titleLabel = (Label) row.getChildren().get(2);
        assertEquals("Members", titleLabel.getText());
        bar.setTitle("Trainers");
        assertEquals("Trainers", titleLabel.getText(), "the visible label follows the title");
    }

    @Test
    void navigationIconAndActionsWireIntoTheirBoxes() {
        M3TopAppBar bar = new M3TopAppBar("Title", TopAppBarVariant.SMALL);
        HBox row = (HBox) bar.getChildren().get(0);
        HBox leadingBox = (HBox) row.getChildren().get(0);
        HBox trailingBox = (HBox) row.getChildren().get(4);

        Label nav = new Label();
        bar.setNavigationIcon(nav);
        assertEquals(1, leadingBox.getChildren().size(), "nav icon lands in the leading box");
        assertEquals("m3-appbar-nav", leadingBox.getChildren().get(0).getId(),
                "nav icon carries the documented id");

        Label actionA = new Label();
        Label actionB = new Label();
        bar.setActions(actionA, actionB);
        assertEquals(2, trailingBox.getChildren().size(), "actions land in the trailing box");
        assertEquals("m3-appbar-action", trailingBox.getChildren().get(0).getId());
        assertEquals("m3-appbar-action", trailingBox.getChildren().get(1).getId());

        bar.setNavigationIcon(null);
        assertEquals(0, leadingBox.getChildren().size(), "null removes the nav icon");
        bar.setActions();
        assertEquals(0, trailingBox.getChildren().size(), "no actions clears the box");
    }

    @Test
    void barAnnouncesAToolbarSummaryAndStaysOutOfTheFocusCycle() {
        // B15: the B9 a11y gap is closed - TOOL_BAR role, a summary announcement, and the bar
        // itself out of the tab-focus cycle (nav icon + actions are the keyboard targets).
        M3TopAppBar bar = new M3TopAppBar("Members", TopAppBarVariant.SMALL);
        bar.setNavigationIcon(new Label());
        bar.setActions(new Label());
        assertEquals(javafx.scene.AccessibleRole.TOOL_BAR, bar.getAccessibleRole());
        assertFalse(bar.isFocusTraversable(), "presentational container is not a tab stop");
        assertEquals("Members toolbar, 1 navigation item, 1 action", bar.getAccessibleText());
    }

    @Test
    void accessibleTextUpdatesLiveWithChanges() {
        M3TopAppBar bar = new M3TopAppBar("Title", TopAppBarVariant.SMALL);
        assertEquals("Title toolbar", bar.getAccessibleText(),
                "no clauses while the bar carries no controls");

        bar.setNavigationIcon(new Label());
        assertEquals("Title toolbar, 1 navigation item", bar.getAccessibleText());

        bar.setActions(new Label(), new Label());
        assertEquals("Title toolbar, 1 navigation item, 2 actions", bar.getAccessibleText());

        bar.setActions(new Label());
        assertEquals("Title toolbar, 1 navigation item, 1 action", bar.getAccessibleText());

        bar.setTitle("Schedule & Timetable");
        assertEquals("Schedule & Timetable toolbar, 1 navigation item, 1 action",
                bar.getAccessibleText(), "the summary follows a title change live");

        bar.setNavigationIcon(null);
        bar.setActions();
        assertEquals("Schedule & Timetable toolbar", bar.getAccessibleText(),
                "empty clauses drop out");

        bar.setTitle("   ");
        assertEquals("Toolbar", bar.getAccessibleText(), "a blank title falls back to Toolbar");
    }
}
