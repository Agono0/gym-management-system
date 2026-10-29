package com.gym.fxml;

import com.gym.GymApp;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GymAppDiagnosticsTest {

    @Test
    void testGymAppThreadStateDuringExecution() throws Exception {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
        Platform.setImplicitExit(false);

        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                stageRef.set(stage);
                GymApp app = new GymApp();
                app.start(stage);
                startLatch.countDown();
            } catch (Throwable t) {
                err.set(t);
                startLatch.countDown();
            }
        });

        startLatch.await(5, TimeUnit.SECONDS);
        if (err.get() != null) {
            err.get().printStackTrace();
            throw new AssertionError("app.start failed", err.get());
        }

        // Wait 2 seconds while the scene graph renders and pulses
        Thread.sleep(2000);

        // Dump threads to inspect where the FX thread or renderer is
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = mxBean.dumpAllThreads(true, true);
        System.out.println("=== THREAD DUMP START ===");
        for (ThreadInfo ti : threadInfos) {
            String name = ti.getThreadName();
            if (name.contains("JavaFX") || name.contains("Quantum") || name.contains("Prism") || name.contains("Main")) {
                System.out.println("Thread: " + name + " | State: " + ti.getThreadState());
                for (StackTraceElement ste : ti.getStackTrace()) {
                    System.out.println("    at " + ste);
                }
            }
        }
        System.out.println("=== THREAD DUMP END ===");

        // Hide stage
        CountDownLatch hideLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            if (stageRef.get() != null) {
                stageRef.get().hide();
            }
            hideLatch.countDown();
        });
        hideLatch.await(3, TimeUnit.SECONDS);
    }
}

