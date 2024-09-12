package works.bosk.defang.runtime.config;

import works.bosk.defang.runtime.EntitlementChecks;
import works.bosk.defang.runtime.InstrumentationMethod;

public class ReflectionMethods {
    @InstrumentationMethod
    public static void getDeclaredMethod(Class<?> callingClass, Class<?> c, String name, Class<?>... parameterTypes) {
        EntitlementChecks.checkReflectionEntitlement(callingClass);
    }

    @InstrumentationMethod
    public static void getDeclaredMethods(Class<?> callingClass, Class<?> c) {
        EntitlementChecks.checkReflectionEntitlement(callingClass);
    }
}
