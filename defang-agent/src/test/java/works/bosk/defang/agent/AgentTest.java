package works.bosk.defang.agent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.bosk.defang.api.NotEntitledException;
import works.bosk.defang.runtime.EntitlementChecking;
import works.bosk.defang.runtime.internal.EntitlementInternals;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static works.bosk.defang.api.Entitlement.FILES;
import static works.bosk.defang.api.Entitlement.REFLECTION;

public class AgentTest {
    File file = new File("nonexistent");

    @BeforeEach
    void activate() {
        EntitlementChecking.revokeAll();
        EntitlementChecking.activate();
    }

    @AfterEach
    void deactivate() {
        EntitlementInternals.isActive = false;
        EntitlementChecking.revokeAll();
    }

    @Test
    public void notEntitled_throws() {
        assertThrows(NotEntitledException.class, file::delete);
        assertThrows(NotEntitledException.class, () -> assertNotNull(getClass().getDeclaredMethod("entitled_works")));
        assertThrows(NotEntitledException.class, getClass()::getDeclaredMethods);
        EntitlementChecking.grant(REFLECTION, getClass().getClassLoader());
        assertThrows(NotEntitledException.class, file::delete, "Wrong permission");
    }

    @Test
    public void entitled_works() throws NoSuchMethodException {
        EntitlementChecking.grant(FILES, getClass().getClassLoader());
        assertFalse(file.delete());
        EntitlementChecking.revokeAll();
        EntitlementChecking.grant(REFLECTION, getClass().getClassLoader());
        assertNotNull(getClass().getDeclaredMethod("entitled_works"));
        assertNotNull(getClass().getDeclaredMethods());
    }
}
