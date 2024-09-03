package works.bosk.defang.runtime;

import works.bosk.defang.api.Entitlement;
import works.bosk.defang.api.FileEntitlement;
import works.bosk.defang.api.NotEntitledException;
import works.bosk.defang.api.OperationKind;
import works.bosk.defang.api.ReflectionEntitlement;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.newSetFromMap;
import static works.bosk.defang.runtime.internal.EntitlementInternals.isActive;

public class EntitlementChecks {
    private static final Map<ClassLoader, Set<FileEntitlement>> fileEntitlements = new ConcurrentHashMap<>();
    private static final Map<ClassLoader, Set<ReflectionEntitlement>> reflectionEntitlements = new ConcurrentHashMap<>();

    /**
     * Causes entitlements to be checked. Before this is called, entitlements are not enforced,
     * so there's no need for an application to set a lot of permissions which are required only during initialization.
     */
    public static void activate() {
        isActive = true;
    }

    public static void revokeAll() {
        fileEntitlements.clear();
    }

    public static boolean grant(ClassLoader classLoader, Entitlement e) {
        return switch (e) {
            case FileEntitlement f -> fileEntitlements
                    .computeIfAbsent(classLoader, k -> newSetFromMap(new ConcurrentHashMap<>()))
                    .add(f);
            case ReflectionEntitlement r -> reflectionEntitlements
                    .computeIfAbsent(classLoader, k -> newSetFromMap(new ConcurrentHashMap<>()))
                    .add(r);
        };
    }

    public static void checkFileEntitlement(Class<?> callingClass, File file, OperationKind operation) {
        for (var e: fileEntitlements.getOrDefault(callingClass.getClassLoader(), emptySet())) {
            if (e.allows(file, operation)) {
                return;
            }
        }
        throw new NotEntitledException("Missing file entitlement for " + callingClass.getName() + " "
                + operation + " \"" + file + "\"");
    }

    public static void checkReflectionEntitlement(Class<?> callingClass) {
        for (var e: reflectionEntitlements.getOrDefault(callingClass.getClassLoader(), emptySet())) {
            if (e.allows()) {
                return;
            }
        }
        throw new NotEntitledException("Missing reflection entitlement for " + callingClass.getName());
    }
}
