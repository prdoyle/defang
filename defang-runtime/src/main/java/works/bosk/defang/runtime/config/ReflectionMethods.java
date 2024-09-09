package works.bosk.defang.runtime.config;

import works.bosk.defang.runtime.EntitlementChecks;
import works.bosk.defang.runtime.InstanceMethod;

import java.lang.reflect.Method;

public class ReflectionMethods {
    @InstanceMethod
    public static void getDeclaredMethod(Class<?> c, String name, Class<?>... parameterTypes) {
        EntitlementChecks.checkReflectionEntitlement();
    }

    @InstanceMethod
    public static void getDeclaredMethods(Class<?> c) {
        EntitlementChecks.checkReflectionEntitlement();
    }
}
