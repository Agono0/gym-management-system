package io.m3fx.controls.carousel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class M3CarouselTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    /**
     * Runs the body on the JavaFX Application Thread: skins, CSS passes and the shared state
     * layer are FX-thread-bound (StateLayer.bind enforces it), so UI work must not run on the
     * plain test thread. Assertions are unchanged; only the execution thread moves.
     */
    private static void onFx(Runnable body) throws Exception {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> err = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out on the JavaFX Application Thread");
        }
        Throwable t = err.get();
        if (t instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (t instanceof Error error) {
            throw error;
        }
        if (t != null) {
            throw new IllegalStateException(t);
        }
    }

    @Test
    void managesItemsAndIndex() {
        M3Carousel carousel = new M3Carousel(M3Carousel.CarouselLayout.MULTI_BROWSE);
        M3Card card1 = M3Card.of(CardVariant.ELEVATED, new Label("Item 1"));
        M3Card card2 = M3Card.of(CardVariant.ELEVATED, new Label("Item 2"));
        M3Card card3 = M3Card.of(CardVariant.ELEVATED, new Label("Item 3"));
        carousel.getItems().addAll(card1, card2, card3);

        assertEquals(3, carousel.getItems().size());
        assertEquals(0, carousel.getActiveIndex());

        carousel.scrollNext();
        assertEquals(1, carousel.getActiveIndex());

        carousel.scrollPrev();
        assertEquals(0, carousel.getActiveIndex());
    }

    @Test
    void testCarouselNavVisibilityAndClamping() throws Exception {
        onFx(() -> {
        M3Carousel carousel = new M3Carousel(M3Carousel.CarouselLayout.MULTI_BROWSE);
        carousel.resize(800, 300);

        // Add 2 narrow cards: total width is small, fits within 800px viewport
        javafx.scene.layout.Region smallCard1 = new javafx.scene.layout.Region();
        smallCard1.setPrefWidth(200);
        javafx.scene.layout.Region smallCard2 = new javafx.scene.layout.Region();
        smallCard2.setPrefWidth(200);
        carousel.getItems().addAll(smallCard1, smallCard2);

        javafx.scene.Scene scene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(carousel), 800, 300);
        carousel.applyCss();
        carousel.layout();

        javafx.scene.Node prevBtn = carousel.lookup(".m3-carousel-nav-prev");
        javafx.scene.Node nextBtn = carousel.lookup(".m3-carousel-nav-next");

        // When all cards fit, nav buttons are hidden to avoid overlapping cards
        assertTrue(prevBtn == null || !prevBtn.isVisible());
        assertTrue(nextBtn == null || !nextBtn.isVisible());

        // Now add multiple cards so total width overflows 800px
        for (int i = 0; i < 6; i++) {
            javafx.scene.layout.Region wideCard = new javafx.scene.layout.Region();
            wideCard.setPrefWidth(260);
            carousel.getItems().add(wideCard);
        }

        carousel.applyCss();
        carousel.layout();

        prevBtn = carousel.lookup(".m3-carousel-nav-prev");
        nextBtn = carousel.lookup(".m3-carousel-nav-next");

        // Now navigation buttons should be present and visible
        assertTrue(prevBtn != null && prevBtn.isVisible());
        assertTrue(nextBtn != null && nextBtn.isVisible());

        // Initially at index 0: prev is disabled
        assertTrue(prevBtn.isDisable());
        assertTrue(!nextBtn.isDisable());

        // Scroll to end
        for (int i = 0; i < 10; i++) {
            carousel.scrollNext();
        }
        assertEquals(7, carousel.getActiveIndex()); // Clamped to last item index (0..7)
        });
    }

    private static KeyEvent keyPressed(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }

    @Test
    void arrowKeysMoveTheActiveItem() throws Exception {
        onFx(() -> {
            M3Carousel carousel = buildCarousel(3);
            carousel.fireEvent(keyPressed(KeyCode.RIGHT));
            assertEquals(1, carousel.getActiveIndex(), "Right moves forward");
            carousel.fireEvent(keyPressed(KeyCode.RIGHT));
            carousel.fireEvent(keyPressed(KeyCode.RIGHT));
            assertEquals(2, carousel.getActiveIndex(), "Right clamps at the last item");
            carousel.fireEvent(keyPressed(KeyCode.LEFT));
            assertEquals(1, carousel.getActiveIndex(), "Left moves back");
        });
    }

    @Test
    void homeAndEndJumpToTheEnds() throws Exception {
        onFx(() -> {
            M3Carousel carousel = buildCarousel(4);
            carousel.setActiveIndex(2);
            carousel.fireEvent(keyPressed(KeyCode.HOME));
            assertEquals(0, carousel.getActiveIndex(), "Home jumps to the first item");
            carousel.fireEvent(keyPressed(KeyCode.END));
            assertEquals(3, carousel.getActiveIndex(), "End jumps to the last item");
        });
    }

    @Test
    void accessibleTextAnnouncesThePosition() throws Exception {
        onFx(() -> {
            M3Carousel carousel = buildCarousel(5);
            assertEquals("Slide 1 of 5", carousel.getAccessibleText());
            carousel.setActiveIndex(3);
            assertEquals("Slide 4 of 5", carousel.getAccessibleText());
            carousel.getItems().remove(4);
            assertEquals("Slide 4 of 4", carousel.getAccessibleText(),
                    "item removal re-announces with the clamped index");
            carousel.getItems().clear();
            assertEquals("Empty carousel", carousel.getAccessibleText());
        });
    }

    @Test
    void carouselIsFocusTraversableWithAPaginationRole() {
        M3Carousel carousel = new M3Carousel(M3Carousel.CarouselLayout.MULTI_BROWSE);
        assertTrue(carousel.isFocusTraversable(), "keyboard navigation needs focus");
        assertEquals(javafx.scene.AccessibleRole.PAGINATION, carousel.getAccessibleRole());
    }

    private static M3Carousel buildCarousel(int count) {
        M3Carousel carousel = new M3Carousel(M3Carousel.CarouselLayout.MULTI_BROWSE);
        for (int i = 0; i < count; i++) {
            carousel.getItems().add(M3Card.of(CardVariant.ELEVATED, new Label("Item " + (i + 1))));
        }
        return carousel;
    }
}

