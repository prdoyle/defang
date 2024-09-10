package works.bosk.defang.agent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import works.bosk.defang.api.FileEntitlement;
import works.bosk.defang.api.NotEntitledException;
import works.bosk.defang.api.ReflectionEntitlement;
import works.bosk.defang.runtime.EntitlementChecks;
import works.bosk.defang.runtime.internal.EntitlementInternals;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static works.bosk.defang.api.FlagEntitlement.SET_SYSTEM_FILES;
import static works.bosk.defang.api.OperationKind.WRITE;

/**
 * This is an end-to-end test that runs with the javaagent installed.
 * It should exhaustively test every instrumented method to make sure it passes with the entitlement
 * and fails without it.
 * See {@code build.gradle} for how we set the command line arguments for this test.
 */
public class AgentTest {
    File file = new File("nonexistent");
    private Method[] escape;

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
        EntitlementChecks.grant(getClass().getModule(), new FileEntitlement(new File("wrong-file"), WRITE));
        assertThrows(NotEntitledException.class, file::delete);
        assertThrows(NotEntitledException.class, () -> assertNotNull(getClass().getDeclaredMethod("entitled_works")));
        assertThrows(NotEntitledException.class, getClass()::getDeclaredMethods);
        assertThrows(NotEntitledException.class, () -> System.setIn(System.in));
        assertThrows(NotEntitledException.class, () -> System.setOut(System.out));
        assertThrows(NotEntitledException.class, () -> System.setErr(System.err));
        EntitlementChecks.grant(getClass().getModule(), new ReflectionEntitlement());
        assertThrows(NotEntitledException.class, file::delete, "Wrong permission");
        assertThrows(NotEntitledException.class, () -> System.exit(123));
    }

    @Test
    public void entitled_works() throws NoSuchMethodException {
        EntitlementChecks.grant(getClass().getModule(), new FileEntitlement(file, WRITE));
        assertFalse(file.delete());
        EntitlementChecks.revokeAll();
        EntitlementChecks.grant(getClass().getModule(), new ReflectionEntitlement());
        assertNotNull(getClass().getDeclaredMethod("entitled_works"));
        assertNotNull(getClass().getDeclaredMethods());
        EntitlementChecks.revokeAll();
        EntitlementChecks.grant(getClass().getModule(), SET_SYSTEM_FILES);
        assertDoesNotThrow(() -> System.setIn(System.in));
        assertDoesNotThrow(() -> System.setOut(System.out));
        assertDoesNotThrow(() -> System.setErr(System.err));
    }

    @Disabled
    @Test
    public void benchmark() {
        EntitlementInternals.isActive = false;
        doBenchmark("Warmup");
        for (int i = 0; i < 3; i++) {
            doBenchmark("Measurement");
        }
    }

    private void doBenchmark(String which) {
        long start = System.currentTimeMillis();
        int iterations = 20_000_000;
        for (int i = 0; i < iterations; i++) {
            this.escape = getClass().getDeclaredMethods();
        }
        double duration = System.currentTimeMillis() - start;
        double nsPerIteration = duration * 1e6 / iterations;
        System.out.println(which + ": " + nsPerIteration + "ns per call");
    }

}
