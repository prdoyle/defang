package works.bosk.defang.agent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.bosk.defang.api.Entitlement;
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

    @BeforeEach
    void activate() {
        EntitlementChecking.activate();
    }

    @AfterEach
    void deactivate() {
        EntitlementInternals.isActive = false;
    }

    @Test
    public void notEntitled_throws() {
        assertThrows(NotEntitledException.class, file::delete);
        assertThrows(NotEntitledException.class, () -> assertNotNull(getClass().getDeclaredMethod("entitled_works")));
        assertThrows(NotEntitledException.class, getClass()::getDeclaredMethods);
        assertThrows(NotEntitledException.class, () -> doEntitled(REFLECTION, () -> assertFalse(file.delete())), "Wrong entitlement should throw");
    }

    @Test
    public void entitled_works() throws NoSuchMethodException {
        doEntitled(FILES, () -> assertFalse(file.delete()));
        doEntitled(REFLECTION, () -> assertNotNull(getClass().getDeclaredMethod("entitled_works")));
        doEntitled(REFLECTION, () -> assertNotNull(getClass().getDeclaredMethods()));
    }

    /**
     * Note that {@link works.bosk.defang.runtime.permission.Permission#checkPermission}
     * currently looks for the string "{@code NOT_PERMITTED}" in the method name.
     */
    @Test
    public void NOT_PERMITTED_throws() {
        assertThrows(NotEntitledException.class, () ->
                doEntitled(REFLECTION, () -> assertNotNull(getClass().getDeclaredMethods())));
    }
}
