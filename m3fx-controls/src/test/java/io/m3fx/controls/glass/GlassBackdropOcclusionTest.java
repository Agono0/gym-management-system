package io.m3fx.controls.glass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * B12 ghost-text root cause: {@link GlassPaneSkin#captureBackdrop()} used to snapshot the
 * capture ancestor flat, so sibling content that paints ABOVE the glass pane (app-bar titles,
 * overlay labels, scanline overlays) was baked into the backdrop image and drawn back
 * refracted — text appeared twice with a slight offset. The backdrop must only contain what is
 * actually behind the pane, photographed at settled layout (the capture is therefore deferred
 * out of the layout pass).
 */
class GlassBackdropOcclusionTest {

    private static final Color BACKDROP_COLOR = Color.web("#203040");
    private static final Color MARKER_COLOR = Color.web("#C040C0");
    private static final double WIDTH = 400;
    private static final double HEIGHT = 300;

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
        Platform.setImplicitExit(false);
    }

    private static void runOnFx(Runnable action) throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(30, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    /**
     * The capture is deferred until layout settles (B12), so poll until the backdrop image
     * exists; each hop is one runLater, i.e. at least one further pulse attempt.
     */
    private static WritableImage awaitBackdropCapture(GlassPaneSkin skin) throws Throwable {
        WritableImage[] holder = new WritableImage[1];
        for (int i = 0; i < 100; i++) {
            runOnFx(() -> holder[0] = skin.backdropImageForTesting());
            if (holder[0] != null) {
                return holder[0];
            }
        }
        return null;
    }

    @Test
    void backdropCaptureMustNotBakeContentPaintedAboveThePane() throws Throwable {
        Stage[] stageHolder = new Stage[1];
        GlassPaneSkin[] skinHolder = new GlassPaneSkin[1];
        runOnFx(() -> {
            StackPane root = new StackPane();
            root.getStyleClass().add("glass-backdrop"); // makes it the backdrop capture source
            Region backdrop = new Region();
            backdrop.setBackground(new Background(new BackgroundFill(
                    BACKDROP_COLOR, CornerRadii.EMPTY, null)));
            M3GlassPane pane = new M3GlassPane();
            pane.setPrefSize(200, 120);
            pane.setMaxSize(200, 120);
            // A sibling text painted AFTER the pane: it must never land in the backdrop image.
            Text ghost = new Text("GHOST TEXT");
            ghost.setFont(Font.font(20));
            ghost.setFill(Color.WHITE);
            root.getChildren().addAll(backdrop, pane, ghost);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, WIDTH, HEIGHT));
            stage.show();
            stageHolder[0] = stage;
            skinHolder[0] = (GlassPaneSkin) pane.getSkin();
        });
        try {
            // The skin is created on the first pulse; if it was still null in the first hop,
            // fetch it again now.
            if (skinHolder[0] == null) {
                runOnFx(() -> skinHolder[0] = (GlassPaneSkin) paneOf(stageHolder[0]).getSkin());
            }
            assertNotNull(skinHolder[0], "skin created during the shown stage's first pulse");
            WritableImage captured = awaitBackdropCapture(skinHolder[0]);
            assertNotNull(captured, "backdrop was captured after layout settled");
            PixelReader px = captured.getPixelReader();
            int nonBackdropPixels = 0;
            for (int x = 0; x < (int) captured.getWidth(); x++) {
                for (int y = 0; y < (int) captured.getHeight(); y++) {
                    if (differsFromBackdrop(px.getColor(x, y))) {
                        nonBackdropPixels++;
                    }
                }
            }
            assertEquals(0, nonBackdropPixels,
                    "backdrop capture baked " + nonBackdropPixels + " pixels of sibling content "
                            + "painted above the pane - this is the B12 ghost-text mechanism");
        } finally {
            runOnFx(() -> {
                stageHolder[0].hide();
                stageHolder[0].close();
            });
        }
    }

    @Test
    void backdropCaptureStillShowsGenuinelyBehindContent() throws Throwable {
        Stage[] stageHolder = new Stage[1];
        GlassPaneSkin[] skinHolder = new GlassPaneSkin[1];
        runOnFx(() -> {
            StackPane root = new StackPane();
            root.getStyleClass().add("glass-backdrop");
            Region backdrop = new Region();
            backdrop.setBackground(new Background(new BackgroundFill(
                    BACKDROP_COLOR, CornerRadii.EMPTY, null)));
            // A marker painted BEFORE the pane: genuinely behind, must stay in the backdrop.
            javafx.scene.shape.Rectangle marker =
                    new javafx.scene.shape.Rectangle(60, 40, MARKER_COLOR);
            M3GlassPane pane = new M3GlassPane();
            pane.setPrefSize(200, 120);
            pane.setMaxSize(200, 120);
            root.getChildren().addAll(backdrop, marker, pane);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, WIDTH, HEIGHT));
            stage.show();
            stageHolder[0] = stage;
            skinHolder[0] = (GlassPaneSkin) pane.getSkin();
        });
        try {
            if (skinHolder[0] == null) {
                runOnFx(() -> skinHolder[0] = (GlassPaneSkin) paneOf(stageHolder[0]).getSkin());
            }
            assertNotNull(skinHolder[0]);
            WritableImage captured = awaitBackdropCapture(skinHolder[0]);
            assertNotNull(captured);
            PixelReader px = captured.getPixelReader();
            int markerPixels = 0;
            for (int x = 0; x < (int) captured.getWidth(); x++) {
                for (int y = 0; y < (int) captured.getHeight(); y++) {
                    Color c = px.getColor(x, y);
                    if (c.getOpacity() > 0.5 && c.getRed() > 0.6 && c.getBlue() > 0.6
                            && c.getGreen() < 0.4) {
                        markerPixels++;
                    }
                }
            }
            assertTrue(markerPixels > 1000,
                    "genuinely-behind content must remain in the backdrop capture, found "
                            + markerPixels + " marker pixels");
        } finally {
            runOnFx(() -> {
                stageHolder[0].hide();
                stageHolder[0].close();
            });
        }
    }

    private static boolean differsFromBackdrop(Color c) {
        return c.getOpacity() > 0.5
                && (Math.abs(c.getRed() - BACKDROP_COLOR.getRed()) > 0.15
                || Math.abs(c.getGreen() - BACKDROP_COLOR.getGreen()) > 0.15
                || Math.abs(c.getBlue() - BACKDROP_COLOR.getBlue()) > 0.15);
    }

    private static M3GlassPane paneOf(Stage stage) {
        return (M3GlassPane) ((StackPane) stage.getScene().getRoot()).getChildren().get(1);
    }
}
