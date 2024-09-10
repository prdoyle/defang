package works.bosk.defang.runtime.config;

import works.bosk.defang.runtime.EntitlementChecks;
import works.bosk.defang.runtime.InstrumentationMethod;

public class ReflectionMethods {
    @InstrumentationMethod
    public static void getDeclaredMethod(Class<?> c, String name, Class<?>... parameterTypes) {
        EntitlementChecks.checkReflectionEntitlement();
    }

    @InstrumentationMethod
    public static void getDeclaredMethods(Class<?> c) {
        EntitlementChecks.checkReflectionEntitlement();
    }
}
