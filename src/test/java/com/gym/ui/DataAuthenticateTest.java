package com.gym.ui;

import com.gym.model.StaffRole;
import com.gym.util.DatabaseConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for the real login credential check (rule 16) behind
 * {@link Data#authenticate}. Runs against an isolated temp SQLite file — the app's live
 * {@code gym.db} is never touched — bootstrapped through the same {@link Data#ensureReady()}
 * path the login gate uses.
 */
class DataAuthenticateTest {

    private static Path tempDir;

    @BeforeAll
    static void bootstrapSeededDatabase() throws Exception {
        tempDir = Files.createTempDirectory("gym-c1-auth-test");
        DatabaseConnection.setDbUrl("jdbc:sqlite:"
                + tempDir.resolve("gym.db").toAbsolutePath().toString().replace('\\', '/'));
        Data.ensureReady(); // initializeDatabase + DemoSeeder (4 admins, roster, ...)
    }

    @AfterAll
    static void releaseDatabase() throws Exception {
        DatabaseConnection.resetDbUrl();
        try (var files = Files.walk(tempDir)) {
            files.sorted(Comparator.reverseOrder()).forEach(file -> file.toFile().delete());
        }
    }

    @Test
    void adminSignsInWithRealIdentityAndRole() throws Exception {
        Data.AuthResult result = Data.authenticate("admin", "admin123");
        assertEquals(Data.AuthResult.Status.OK, result.kind());
        assertEquals("Alex Carter", result.fullName());
        assertEquals(StaffRole.ADMIN, result.role());
        assertNull(result.error());
    }

    @Test
    void managerSignsIn() throws Exception {
        Data.AuthResult result = Data.authenticate("manager", "mgr123");
        assertEquals(Data.AuthResult.Status.OK, result.kind());
        assertEquals(StaffRole.MANAGER, result.role());
    }

    @Test
    void staffSignsIn() throws Exception {
        Data.AuthResult result = Data.authenticate("staff", "staff123");
        assertEquals(Data.AuthResult.Status.OK, result.kind());
        assertEquals(StaffRole.STAFF, result.role());
    }

    @Test
    void wrongPasswordIsRefused() throws Exception {
        Data.AuthResult result = Data.authenticate("admin", "nope");
        assertEquals(Data.AuthResult.Status.INVALID_CREDENTIALS, result.kind());
        assertEquals("Wrong username or password.", result.error());
    }

    @Test
    void unknownUsernameIsRefused() throws Exception {
        Data.AuthResult result = Data.authenticate("ghost", "whatever123");
        assertEquals(Data.AuthResult.Status.INVALID_CREDENTIALS, result.kind());
    }

    @Test
    void blankFieldsAreRefusedAsInvalid() throws Exception {
        assertEquals(Data.AuthResult.Status.INVALID_CREDENTIALS,
                Data.authenticate("", "admin123").kind());
        assertEquals(Data.AuthResult.Status.INVALID_CREDENTIALS,
                Data.authenticate("admin", "").kind());
    }

    @Test
    void inactiveAccountIsRefusedWithRule16Copy() throws Exception {
        Data.AuthResult result = Data.authenticate("oldstaff", "old123");
        assertEquals(Data.AuthResult.Status.INACTIVE, result.kind());
        assertTrue(result.error().contains("rule 16"));
        assertNull(result.fullName()); // an inactive account must not hand out an identity
    }
}
