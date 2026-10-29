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
}

