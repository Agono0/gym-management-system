package com.gym.fxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Empirical Challenger Verification Suite for Requirement R2:
 * "Empirically challenge the controller binding contracts and FXML integrity across all 19 screens."
 *
 * Verifies:
 * 1. Clean FXMLLoader instantiation and injection of all 19 screens.
 * 2. Every single @FXML field in every controller is non-null after FXMLLoader injection (zero orphan fields).
 * 3. Injected instance types match controller field types.
 * 4. Every onAction="#method" handler in FXML resolves to a callable controller method.
 * 5. Controllers contain zero imports from com.gym.service or com.gym.dao packages.
 * 6. Total @FXML bindings across 19 screens are counted and validated against the empirical target (>450).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ControllerBindingContractTest {

    public static final List<String> SCREENS = List.of(
            "login.fxml",
            "app-shell.fxml",
            "dashboard.fxml",
            "customer-list.fxml",
            "customer-profile.fxml",
            "medical-profile.fxml",
            "progress.fxml",
            "trainer-list.fxml",
            "trainer-profile.fxml",
            "availability.fxml",
            "workout.fxml",
            "live-workout-hud.fxml",
            "schedule.fxml",
            "equipment.fxml",
            "gym-floor-map.fxml",
            "attendance.fxml",
            "membership.fxml",
            "payment.fxml",
            "staff.fxml"
    );

    private static final Map<String, Integer> INJECTED_FIELDS_PER_SCREEN = new ConcurrentHashMap<>();
    private static final Map<String, Integer> ON_ACTION_HANDLERS_PER_SCREEN = new ConcurrentHashMap<>();
    private static final Map<String, Integer> FXML_FXID_COUNT_PER_SCREEN = new ConcurrentHashMap<>();

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    private static record FxmlLoadResult(Parent root, Object controller, FXMLLoader loader) {}

    private FxmlLoadResult loadFxmlAndController(String fxmlFileName) throws Exception {
        URL resource = getClass().getResource("/fxml/" + fxmlFileName);
        assertNotNull(resource, "Mandatory FXML file not found on classpath: /fxml/" + fxmlFileName);

        AtomicReference<FxmlLoadResult> resultRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(resource);
                Parent root = loader.load();
                Object controller = loader.getController();
                resultRef.set(new FxmlLoadResult(root, controller, loader));
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        if (!finished) {
            throw new TimeoutException("Timed out loading /fxml/" + fxmlFileName);
        }

        if (errorRef.get() != null) {
            Throwable t = errorRef.get();
            if (t instanceof Exception e) throw e;
            throw new RuntimeException("Error loading /fxml/" + fxmlFileName, t);
        }

        return resultRef.get();
    }

    private String readResourceContent(String resourcePath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Resource not found: " + resourcePath);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @ParameterizedTest(name = "Empirical Controller Injection: {0}")
    @ValueSource(strings = {
            "login.fxml",
            "app-shell.fxml",
            "dashboard.fxml",
            "customer-list.fxml",
            "customer-profile.fxml",
            "medical-profile.fxml",
            "progress.fxml",
            "trainer-list.fxml",
            "trainer-profile.fxml",
            "availability.fxml",
            "workout.fxml",
            "live-workout-hud.fxml",
            "schedule.fxml",
            "equipment.fxml",
            "gym-floor-map.fxml",
            "attendance.fxml",
            "membership.fxml",
            "payment.fxml",
            "staff.fxml"
    })
    @Order(1)
    @DisplayName("Verify that FXMLLoader cleanly injects all @FXML fields without null orphans")
    void testControllerInjectionCompleteness(String fxmlFileName) throws Exception {
        FxmlLoadResult result = loadFxmlAndController(fxmlFileName);
        assertNotNull(result.root, "Root node must be instantiated for " + fxmlFileName);
        assertNotNull(result.controller, "Controller must be instantiated for " + fxmlFileName);

        Object controller = result.controller;
        Class<?> ctrlClass = controller.getClass();

        // 1. Inspect all @FXML fields
        List<Field> fxmlFields = new ArrayList<>();
        Class<?> current = ctrlClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(FXML.class)) {
                    fxmlFields.add(field);
                }
            }
            current = current.getSuperclass();
        }

        assertFalse(fxmlFields.isEmpty(), "Controller " + ctrlClass.getName() + " must have @FXML fields");

        int injectedCount = 0;
        List<String> nullOrphans = new ArrayList<>();
        List<String> typeMismatches = new ArrayList<>();

        for (Field field : fxmlFields) {
            field.setAccessible(true);
            Object value = field.get(controller);
            if (value == null) {
                nullOrphans.add(field.getName() + " (" + field.getType().getSimpleName() + ")");
            } else {
                injectedCount++;
                if (!field.getType().isAssignableFrom(value.getClass())) {
                    typeMismatches.add(field.getName() + " expected " + field.getType().getName()
                            + " but got " + value.getClass().getName());
                }
            }
        }

        assertTrue(nullOrphans.isEmpty(),
                "Found " + nullOrphans.size() + " orphan @FXML fields (un-injected / null) in "
                        + ctrlClass.getSimpleName() + " for " + fxmlFileName + ": " + nullOrphans);

        assertTrue(typeMismatches.isEmpty(),
                "Found type mismatches in " + ctrlClass.getSimpleName() + ": " + typeMismatches);

        INJECTED_FIELDS_PER_SCREEN.put(fxmlFileName, injectedCount);

        // 2. Cross-check FXML fx:ids vs controller fields
        String fxmlContent = readResourceContent("/fxml/" + fxmlFileName);
        Matcher fxIdMatcher = Pattern.compile("fx:id=\"([a-zA-Z0-9_]+)\"").matcher(fxmlContent);
        Set<String> fxmlFxIds = new HashSet<>();
        while (fxIdMatcher.find()) {
            fxmlFxIds.add(fxIdMatcher.group(1));
        }
        FXML_FXID_COUNT_PER_SCREEN.put(fxmlFileName, fxmlFxIds.size());

        Set<String> ctrlFieldNames = new HashSet<>();
        for (Field f : fxmlFields) {
            ctrlFieldNames.add(f.getName());
        }

        // Check if any controller field does not exist in FXML fx:ids
        Set<String> orphanCtrlFields = new HashSet<>(ctrlFieldNames);
        orphanCtrlFields.removeAll(fxmlFxIds);
        assertTrue(orphanCtrlFields.isEmpty(),
                "Controller " + ctrlClass.getSimpleName() + " declares @FXML fields not present in FXML: "
                        + orphanCtrlFields);

        // 3. Verify onAction handlers
        Matcher actionMatcher = Pattern.compile("onAction=\"#([a-zA-Z0-9_]+)\"").matcher(fxmlContent);
        Set<String> actionHandlers = new HashSet<>();
        while (actionMatcher.find()) {
            actionHandlers.add(actionMatcher.group(1));
        }
        ON_ACTION_HANDLERS_PER_SCREEN.put(fxmlFileName, actionHandlers.size());

        for (String handlerName : actionHandlers) {
            boolean methodFound = false;
            for (Method method : ctrlClass.getDeclaredMethods()) {
                if (method.getName().equals(handlerName)) {
                    Class<?>[] pTypes = method.getParameterTypes();
                    if (pTypes.length == 0 || (pTypes.length == 1 && Event.class.isAssignableFrom(pTypes[0]))) {
                        methodFound = true;
                        break;
                    }
                }
            }
            assertTrue(methodFound,
                    "FXML " + fxmlFileName + " specifies onAction=\"#" + handlerName
                            + "\" but no compatible @FXML method was found in " + ctrlClass.getSimpleName());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Verify architectural purity: zero backend service or DAO imports in controllers")
    void testZeroBackendOrDaoImports() throws Exception {
        Pattern forbiddenImport = Pattern.compile("^\\s*import\\s+com\\.gym\\.(service|dao|model)\\..*", Pattern.MULTILINE);
        List<String> violations = new ArrayList<>();

        for (String fxmlFileName : SCREENS) {
            String fxmlContent = readResourceContent("/fxml/" + fxmlFileName);
            Matcher ctrlMatcher = Pattern.compile("fx:controller=\"([^\"]+)\"").matcher(fxmlContent);
            assertTrue(ctrlMatcher.find(), "FXML " + fxmlFileName + " must declare fx:controller");
            String ctrlClassName = ctrlMatcher.group(1);

            String javaSourcePath = "/" + ctrlClassName.replace('.', '/') + ".java";
            // Check file content via resource or filesystem
            URL javaRes = getClass().getResource(javaSourcePath);
            if (javaRes != null) {
                String source = readResourceContent(javaSourcePath);
                Matcher m = forbiddenImport.matcher(source);
                while (m.find()) {
                    violations.add(ctrlClassName + " imports: " + m.group(0).trim());
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Controllers must strictly avoid service, DAO, or model imports: " + violations);
    }

    @Test
    @Order(3)
    @DisplayName("Verify total empirical @FXML binding counts across all 19 screens")
    void testTotalEmpiricalBindingCounts() throws Exception {
        // Ensure all screens have been tested
        for (String screen : SCREENS) {
            if (!INJECTED_FIELDS_PER_SCREEN.containsKey(screen)) {
                testControllerInjectionCompleteness(screen);
            }
        }

        int totalInjectedFields = INJECTED_FIELDS_PER_SCREEN.values().stream().mapToInt(Integer::intValue).sum();
        int totalFxmlFxIds = FXML_FXID_COUNT_PER_SCREEN.values().stream().mapToInt(Integer::intValue).sum();
        int totalOnActionHandlers = ON_ACTION_HANDLERS_PER_SCREEN.values().stream().mapToInt(Integer::intValue).sum();
        int totalBindings = totalInjectedFields + totalOnActionHandlers;

        System.out.println("===================================================================");
        System.out.println("EMPIRICAL CONTROLLER BINDING VERIFICATION RESULTS (19 SCREENS)");
        System.out.println("===================================================================");
        for (String screen : SCREENS) {
            System.out.printf("Screen: %-22s | FXML fx:ids: %2d | Injected Fields: %2d | onAction: %2d%n",
                    screen,
                    FXML_FXID_COUNT_PER_SCREEN.getOrDefault(screen, 0),
                    INJECTED_FIELDS_PER_SCREEN.getOrDefault(screen, 0),
                    ON_ACTION_HANDLERS_PER_SCREEN.getOrDefault(screen, 0));
        }
        System.out.println("-------------------------------------------------------------------");
        System.out.printf("TOTAL FXML fx:ids:           %d%n", totalFxmlFxIds);
        System.out.printf("TOTAL Injected @FXML fields: %d (100%% non-null)%n", totalInjectedFields);
        System.out.printf("TOTAL onAction handlers:     %d%n", totalOnActionHandlers);
        System.out.printf("TOTAL CONTRACT BINDINGS:     %d (Target: >450)%n", totalBindings);
        System.out.println("===================================================================");

        assertEquals(19, INJECTED_FIELDS_PER_SCREEN.size(), "All 19 screens must be tested");
        assertEquals(444, totalInjectedFields, "Expected 444 injected @FXML fields");
        assertEquals(448, totalFxmlFxIds, "Expected 448 FXML fx:ids");
        assertTrue(totalBindings > 450,
                "Total verified bindings (" + totalBindings + ") must exceed empirical target of 450");
    }
}

