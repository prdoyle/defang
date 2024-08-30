package works.bosk.defang.runtime;

import works.bosk.defang.api.Entitlement;
import works.bosk.defang.api.NotEntitledException;
import works.bosk.defang.runtime.permission.Permission;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.util.Collections.emptySet;
import static works.bosk.defang.runtime.internal.EntitlementInternals.isActive;
import static works.bosk.defang.runtime.permission.Permission.checkPermission;

public class EntitlementChecking {
    private static final Map<Entitlement, Set<ClassLoader>> entitledClassLoaders = new ConcurrentHashMap<>();

    public static void revokeAll() {
        entitledClassLoaders.clear();
    }

    public static boolean grant(Entitlement entitlement, ClassLoader classLoader) {
        return entitledClassLoaders.computeIfAbsent(entitlement, k -> new HashSet<>()).add(classLoader);
    }

    /**
     * Causes entitlements to be checked. Before this is called, entitlements are not enforced,
     * so there's no need for an application to set a lot of permissions which are required only during initialization.
     */
    public static void activate() {
        isActive = true;
    }

    public static void checkEntitlement(Entitlement entitlement, Class<?> callerClass) {
        if (isActive && !entitledClassLoaders.getOrDefault(entitlement, emptySet()).contains(callerClass.getClassLoader())) {
            throw new NotEntitledException("Missing entitlement: " + entitlement);
        }
    }
}
