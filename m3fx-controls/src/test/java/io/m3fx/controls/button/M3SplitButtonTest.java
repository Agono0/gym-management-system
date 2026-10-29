package io.m3fx.controls.button;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.controls.button.M3SplitButton.SplitButtonSize;
import io.m3fx.controls.menu.M3Menu;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

class M3SplitButtonTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
    }

    @Test
    void createsWithProperties() {
        M3Menu menu = new M3Menu().item("Export", () -> {});
        M3SplitButton split = M3SplitButton.filled("Send", menu);

        assertEquals("Send", split.getText());
        assertEquals(ButtonVariant.FILLED, split.getVariant());
        assertEquals(SplitButtonSize.MEDIUM, split.getSize());
        assertNotNull(split.getMenu());
        assertTrue(split.getStyleClass().contains("m3-split-button"));
        assertTrue(split.getStyleClass().contains("m3-split-button-filled"));
    }

    @Test
    void updatesVariantAndSize() {
        M3SplitButton split = new M3SplitButton("Action", ButtonVariant.TONAL, null);
        split.setVariant(ButtonVariant.OUTLINED);
        split.setSize(SplitButtonSize.LARGE);

        assertEquals(ButtonVariant.OUTLINED, split.getVariant());
        assertEquals(SplitButtonSize.LARGE, split.getSize());
        assertEquals(48.0, split.getPrefHeight());
    }

    @Test
    void triggersPrimaryAction() {
        M3SplitButton split = new M3SplitButton("Save", ButtonVariant.FILLED, null);
        AtomicBoolean clicked = new AtomicBoolean(false);
        split.setOnAction(e -> clicked.set(true));

        split.getLeadingButton().activate();
        assertTrue(clicked.get(), "Primary button activate should trigger onAction handler");
    }
}
