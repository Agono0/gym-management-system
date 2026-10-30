package io.m3fx.controls.carousel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import javafx.application.Platform;
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
    void testCarouselNavVisibilityAndClamping() {
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
    }
}

