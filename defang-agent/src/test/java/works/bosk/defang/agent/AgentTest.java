package works.bosk.defang.agent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.bosk.defang.api.FileEntitlement;
import works.bosk.defang.api.NotEntitledException;
import works.bosk.defang.api.ReflectionEntitlement;
import works.bosk.defang.runtime.EntitlementChecks;
import works.bosk.defang.runtime.internal.EntitlementInternals;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static works.bosk.defang.api.OperationKind.WRITE;

/**
 * This is an end-to-end test that runs with the javaagent installed.
 * See {@code build.gradle}.
 */
public class AgentTest {
    File file = new File("nonexistent");

    @BeforeEach
    void activate() {
        EntitlementChecks.revokeAll();
        EntitlementChecks.activate();
    }

    @AfterEach
    void deactivate() {
        EntitlementInternals.isActive = false;
        EntitlementChecks.revokeAll();
    }

    @Test
    public void notEntitled_throws() {
        assertThrows(NotEntitledException.class, file::delete);
        assertThrows(NotEntitledException.class, () -> assertNotNull(getClass().getDeclaredMethod("entitled_works")));
        assertThrows(NotEntitledException.class, getClass()::getDeclaredMethods);
        EntitlementChecks.grant(getClass().getClassLoader(), new ReflectionEntitlement());
        assertThrows(NotEntitledException.class, file::delete, "Wrong permission");
    }

    @Test
    public void entitled_works() throws NoSuchMethodException {
        EntitlementChecks.grant(getClass().getClassLoader(), new FileEntitlement(file, WRITE));
        assertFalse(file.delete());
        EntitlementChecks.revokeAll();
        EntitlementChecks.grant(getClass().getClassLoader(), new ReflectionEntitlement());
        assertNotNull(getClass().getDeclaredMethod("entitled_works"));
        assertNotNull(getClass().getDeclaredMethods());
    }
}
