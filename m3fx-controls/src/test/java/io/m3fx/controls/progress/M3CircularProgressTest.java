package io.m3fx.controls.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class M3CircularProgressTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void indeterminateByDefault() {
        M3CircularProgress cp = new M3CircularProgress();
        assertTrue(cp.isIndeterminate());
        assertEquals(M3CircularProgress.INDETERMINATE, cp.getProgress());
    }

    @Test
    void determinateProgress() {
        M3CircularProgress cp = new M3CircularProgress(0.75);
        assertFalse(cp.isIndeterminate());
        assertEquals(0.75, cp.getProgress(), 1e-4);

        cp.setProgress(1.0);
        assertEquals(1.0, cp.getProgress(), 1e-4);
        assertFalse(cp.isIndeterminate());

        cp.setProgress(M3CircularProgress.INDETERMINATE);
        assertTrue(cp.isIndeterminate());
    }
}

