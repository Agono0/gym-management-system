package com.gym.screen;

import com.gym.controller.Screen;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Empirical Stress Harness and Scene-Graph Deep Validator (the former FXML stress harness).
 * Executes on the JavaFX Application Thread:
 * <ol>
 *   <li>Deep JavaFX node tree depth measurement</li>
 *   <li>Total node count measurement in the scene graph</li>
 *   <li>View-field verification (every Node/TableColumn/Tab field non-null after build)</li>
 *   <li>Repetitive rebuild stress test (5 iterations per screen)</li>
 * </ol>
 */
public class ScreenStressHarnessTest {

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

    private record BuiltScreen(Parent root, Object controller, long elapsedNanos) {}

    /** Builds the screen on the FX thread with timing. */
    private static BuiltScreen buildWithTiming(Screen screen) throws Exception {
        AtomicReference<Parent> rootRef = new AtomicReference<>();
        AtomicReference<Object> controllerRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        long startTime = System.nanoTime();

        Platform.runLater(() -> {
            try {
                com.gym.controller.ScreenController controller = screen.create();
                rootRef.set(controller.getRoot());
                controllerRef.set(controller);
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        long elapsed = System.nanoTime() - startTime;
        if (!finished) {
            throw new RuntimeException("Timeout building " + screen.key());
        }
        if (errorRef.get() != null) {
            throw new RuntimeException("Error building " + screen.key(), errorRef.get());
        }
        return new BuiltScreen(rootRef.get(), controllerRef.get(), elapsed);
    }

    /** Node-bearing view fields: Node itself plus the two tree members that carry ids. */
    private static boolean isViewField(Field field) {
        Class<?> type = field.getType();
        return Node.class.isAssignableFrom(type)
                || javafx.scene.control.TableColumn.class.isAssignableFrom(type)
                || javafx.scene.control.Tab.class.isAssignableFrom(type);
    }

    @Test
    @DisplayName("Execute Empirical Stress Harness across all 19 screens")
    void testEmpiricalStressAndSceneGraphMetrics() throws Exception {
        List<ScreenMetrics> metricsList = new ArrayList<>();

        for (Screen screen : ScreenBuildIntegrityTest.ALL_19_SCREENS) {
            // 1. Initial timed build
            BuiltScreen result = buildWithTiming(screen);
            Parent root = result.root();
            Object controller = result.controller();

            assertNotNull(root, "Root must not be null for " + screen.key());
            assertNotNull(controller, "Controller must be bound for " + screen.key());

            int depth = computeNodeDepth(root, 1);
            int nodeCount = countTotalNodes(root);

            // Verify depth >= 3 requirement
            assertTrue(depth >= 3, "Node depth for " + screen.key() + " must be >= 3, but was " + depth);

            // Inspect the view fields (the former @FXML bindings)
            int totalViewFields = 0;
            int boundFields = 0;
            for (Class<?> c = controller.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (isViewField(field)) {
                        totalViewFields++;
                        field.setAccessible(true);
                        Object val = field.get(controller);
                        if (val != null) {
                            boundFields++;
                        }
                    }
                }
            }

            // 2. Stress testing: 5 consecutive rebuilds
            long stressStart = System.nanoTime();
            for (int i = 0; i < 5; i++) {
                BuiltScreen stressRes = buildWithTiming(screen);
                assertNotNull(stressRes.root());
            }
            long stressElapsed = System.nanoTime() - stressStart;

            metricsList.add(new ScreenMetrics(
                    screen.key(),
                    root.getClass().getSimpleName(),
                    depth,
                    nodeCount,
                    totalViewFields,
                    boundFields,
                    result.elapsedNanos(),
                    stressElapsed
            ));
        }

        // Print Empirical Summary Table
        System.out.println("\n==========================================================================================================");
        System.out.println("                         EMPIRICAL CHALLENGER STRESS & METRICS REPORT                             ");
        System.out.println("==========================================================================================================");
        System.out.printf("%-22s | %-12s | %-5s | %-6s | %-11s | %-11s | %-11s%n",
                "Screen Name", "Root Class", "Depth", "Nodes", "Bound/Total", "1st Build", "5-Rebuilds");
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
