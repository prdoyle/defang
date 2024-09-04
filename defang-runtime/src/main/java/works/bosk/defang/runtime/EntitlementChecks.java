package works.bosk.defang.runtime;

import works.bosk.defang.api.Entitlement;
import works.bosk.defang.api.FileEntitlement;
import works.bosk.defang.api.FlagEntitlement;
import works.bosk.defang.api.NotEntitledException;
import works.bosk.defang.api.OperationKind;
import works.bosk.defang.api.ReflectionEntitlement;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptySet;
import static java.util.Collections.newSetFromMap;
import static works.bosk.defang.runtime.internal.EntitlementInternals.isActive;

public class EntitlementChecks {
    private static final Map<Module, Set<FileEntitlement>> fileEntitlements = new ConcurrentHashMap<>();
    private static final Map<Module, Set<ReflectionEntitlement>> reflectionEntitlements = new ConcurrentHashMap<>();
    private static final Map<Module, Set<FlagEntitlement>> flagEntitlements = new ConcurrentHashMap<>();

    /**
     * Causes entitlements to be checked. Before this is called, entitlements are not enforced,
     * so there's no need for an application to set a lot of permissions which are required only during initialization.
     */
    public static void activate() {
        isActive = true;
    }

    public static void revokeAll() {
        fileEntitlements.clear();
        reflectionEntitlements.clear();
        flagEntitlements.clear();
    }

    public static boolean grant(Module module, Entitlement e) {
        return switch (e) {
            case FileEntitlement f -> fileEntitlements
                    .computeIfAbsent(module, k -> newSetFromMap(new ConcurrentHashMap<>()))
                    .add(f);
            case ReflectionEntitlement r -> reflectionEntitlements
                    .computeIfAbsent(module, k -> newSetFromMap(new ConcurrentHashMap<>()))
                    .add(r);
            case FlagEntitlement f -> flagEntitlements
                    .computeIfAbsent(module, k -> newSetFromMap(new ConcurrentHashMap<>())) // Yeesh... try to use EnumSet safely. Perhaps only allow grants when not active?
                    .add(f);
        };
    }

    private static boolean isTriviallyAllowed(Class<?> callingClass) {
        return !isActive || (callingClass.getClassLoader() == null);
    }

    public static void checkFileEntitlement(Class<?> callingClass, File file, OperationKind operation) {
        if (isTriviallyAllowed(callingClass)) {
            return;
        }
        for (var e: fileEntitlements.getOrDefault(callingClass.getModule(), emptySet())) {
            if (e.allows(file, operation)) {
                return;
            }
        }
        throw new NotEntitledException("Missing file entitlement for " + callingClass.getName() + " "
                + operation + " \"" + file + "\"");
    }

    public static void checkReflectionEntitlement(Class<?> callingClass) {
        if (isTriviallyAllowed(callingClass)) {
            return;
        }
        for (var e: reflectionEntitlements.getOrDefault(callingClass.getModule(), emptySet())) {
            if (e.allows()) {
                return;
            }
        }
        throw new NotEntitledException("Missing reflection entitlement for " + callingClass.getName());
    }

    public static void checkFlagEntitlement(Class<?> callingClass, FlagEntitlement flag) {
        if (isTriviallyAllowed(callingClass)) {
            return;
        }
        if (flagEntitlements.getOrDefault(callingClass.getModule(), emptySet()).contains(flag)) {
            return;
        }
        throw new NotEntitledException("Missing " + flag + " entitlement for " + callingClass.getName());
    }
}
