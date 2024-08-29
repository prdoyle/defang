package works.bosk.defang.agent;

import org.junit.jupiter.api.AfterEach;
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
import static works.bosk.defang.runtime.EntitlementChecking.doEntitled;

public class AgentTest {
    File file = new File("nonexistent");

    @AfterEach
    void deactivate() {
        EntitlementInternals.isActive = false;
    }

    @Test
    public void notEntitled_throws() {
        EntitlementChecking.activate();
        assertThrows(NotEntitledException.class, file::delete);
        assertThrows(NotEntitledException.class, () -> assertNotNull(getClass().getDeclaredMethod("entitled_works")));
        assertThrows(NotEntitledException.class, getClass()::getDeclaredMethods);
        assertThrows(NotEntitledException.class, () -> doEntitled(REFLECTION, () -> assertFalse(file.delete())), "Wrong entitlement should throw");
    }

    @Test
    public void entitled_works() throws NoSuchMethodException {
        EntitlementChecking.activate();
        doEntitled(FILES, () -> assertFalse(file.delete()));
        doEntitled(REFLECTION, () -> assertNotNull(getClass().getDeclaredMethod("entitled_works")));
        doEntitled(REFLECTION, () -> assertNotNull(getClass().getDeclaredMethods()));
    }
}
