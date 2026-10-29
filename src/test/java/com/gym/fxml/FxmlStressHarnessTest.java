package com.gym.fxml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Empirical Stress Harness and Scene-Graph Deep Validator.
 * Executes on the JavaFX Application Thread:
 * 1. Deep JavaFX Node tree depth measurement
 * 2. Total Node count measurement in scene graph
 * 3. Controller binding verification (checks @javafx.fxml.FXML field injection)
 * 4. Repetitive load stress test (5 iterations per screen)
 */
public class FxmlStressHarnessTest {

    public record ScreenMetrics(
            String screenName,
            String rootClassName,
            int nodeTreeDepth,
            int totalNodeCount,
            int controllerFieldCount,
            int boundFieldCount,
            long singleLoadNanos,
            long stress5LoadNanos
    ) {}

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    private static int computeNodeDepth(Node node, int currentDepth) {
        if (!(node instanceof Parent parent)) {
            return currentDepth;
        }
        int max = currentDepth;
        for (Node child : parent.getChildrenUnmodifiable()) {
            int d = computeNodeDepth(child, currentDepth + 1);
            if (d > max) {
                max = d;
            }
        }
        return max;
    }

    private static int countTotalNodes(Node node) {
        if (!(node instanceof Parent parent)) {
            return 1;
        }
        int count = 1;
        for (Node child : parent.getChildrenUnmodifiable()) {
            count += countTotalNodes(child);
        }
        return count;
    }

    private record FxmlLoadResult(Parent root, Object controller, long elapsedNanos) {}

    private static FxmlLoadResult loadFxmlWithTiming(URL resource) throws Exception {
        AtomicReference<Parent> rootRef = new AtomicReference<>();
        AtomicReference<Object> controllerRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        long startTime = System.nanoTime();

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(resource);
                Parent root = loader.load();
                rootRef.set(root);
                controllerRef.set(loader.getController());
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        long elapsed = System.nanoTime() - startTime;
        if (!finished) {
            throw new RuntimeException("Timeout loading " + resource);
        }
        if (errorRef.get() != null) {
            throw new RuntimeException("Error loading " + resource, errorRef.get());
        }
        return new FxmlLoadResult(rootRef.get(), controllerRef.get(), elapsed);
    }

    @Test
    @DisplayName("Execute Empirical Stress Harness across all 19 screens")
    void testEmpiricalStressAndSceneGraphMetrics() throws Exception {
        List<ScreenMetrics> metricsList = new ArrayList<>();

        for (String screenName : FxmlIntegrityTest.ALL_19_SCREENS) {
            URL url = getClass().getResource("/fxml/" + screenName);
            assertNotNull(url, "Screen must exist on classpath: " + screenName);

            // 1. Initial timed load
            FxmlLoadResult result = loadFxmlWithTiming(url);
            Parent root = result.root();
            Object controller = result.controller();

            assertNotNull(root, "Root must not be null for " + screenName);
            assertNotNull(controller, "Controller must be bound for " + screenName);

            int depth = computeNodeDepth(root, 1);
            int nodeCount = countTotalNodes(root);

            // Verify depth >= 3 requirement
            assertTrue(depth >= 3, "Node depth for " + screenName + " must be >= 3, but was " + depth);

            // Inspect @FXML fields
            int totalFxmlFields = 0;
            int boundFields = 0;
            for (Field field : controller.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(javafx.fxml.FXML.class)) {
                    totalFxmlFields++;
                    field.setAccessible(true);
                    Object val = field.get(controller);
                    if (val != null) {
                        boundFields++;
                    }
                }
            }

            // 2. Stress testing: 5 consecutive reloads
            long stressStart = System.nanoTime();
            for (int i = 0; i < 5; i++) {
                FxmlLoadResult stressRes = loadFxmlWithTiming(url);
                assertNotNull(stressRes.root());
            }
            long stressElapsed = System.nanoTime() - stressStart;

            metricsList.add(new ScreenMetrics(
                    screenName,
                    root.getClass().getSimpleName(),
                    depth,
                    nodeCount,
                    totalFxmlFields,
                    boundFields,
                    result.elapsedNanos(),
                    stressElapsed
            ));
        }

        // Print Empirical Summary Table
        System.out.println("\n==========================================================================================================");
        System.out.println("                         EMPIRICAL CHALLENGER STRESS & METRICS REPORT                             ");
        System.out.println("==========================================================================================================");
        System.out.printf("%-22s | %-12s | %-5s | %-6s | %-11s | %-10s | %-11s%n",
                "Screen Name", "Root Class", "Depth", "Nodes", "Bound/Total", "1st Load", "5-Reloads");
        System.out.println("----------------------------------------------------------------------------------------------------------");

        for (ScreenMetrics m : metricsList) {
            System.out.printf("%-22s | %-12s | %-5d | %-6d | %-2d / %-2d    | %6.2f ms | %6.2f ms%n",
                    m.screenName(),
                    m.rootClassName(),
                    m.nodeTreeDepth(),
                    m.totalNodeCount(),
                    m.boundFieldCount(),
                    m.controllerFieldCount(),
                    m.singleLoadNanos() / 1_000_000.0,
                    m.stress5LoadNanos() / 1_000_000.0
            );
        }
        System.out.println("==========================================================================================================\n");
    }
}
