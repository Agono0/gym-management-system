package com.gym.controller;

import javafx.scene.Parent;

/**
 * Contract for Gym-OS screen controllers that build their own UI in Java code.
 *
 * <p>Historically every screen was a pair: an {@code .fxml} markup file describing the node
 * tree plus a controller whose {@code @FXML} fields were injected by {@code FXMLLoader}.
 * Every screen now constructs that node tree programmatically in its constructor instead —
 * same nodes, same styling, same handler wiring, no markup resource — and this interface
 * exposes the finished root so the shell ({@link AppShellController}) and the samples gallery
 * can mount a screen without touching any FXML file.
 */
public interface ScreenController {

    /**
     * The fully constructed root node of this screen's UI.
     * Available immediately after the constructor returns; never {@code null}.
     */
    Parent getRoot();
}
