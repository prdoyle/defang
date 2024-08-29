package works.bosk.defang.runtime;

import works.bosk.defang.api.Entitlement;
import works.bosk.defang.api.NotEntitledException;
import works.bosk.defang.runtime.permission.Permission;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static works.bosk.defang.runtime.internal.EntitlementInternals.isActive;
import static works.bosk.defang.runtime.permission.Permission.checkPermission;

public class EntitlementChecking {

    private static final ThreadLocal<Set<Entitlement>> ACTIVE_ENTITLEMENTS = ThreadLocal.withInitial(() -> EnumSet.noneOf(Entitlement.class));

    /**
     * Causes entitlements to be checked. Before this is called, entitlements are not enforced,
     * so there's no need for an application to set a lot of permissions which are required only during initialization.
     */
    public static void activate() {
        isActive = true;
    }

    public interface PrivilegedRunnable<X extends Throwable> {
        void run() throws X;
    }

    public static <X extends Throwable> void doEntitled(Entitlement e, PrivilegedRunnable<X> action) throws X {
        checkPermission(e, StackWalker
                .getInstance(RETAIN_CLASS_REFERENCE)
                .walk(s -> s.skip(1).findFirst().orElseThrow()));
        Set<Entitlement> entitlements = ACTIVE_ENTITLEMENTS.get();
        boolean shouldRemove = entitlements.add(e);
        try {
            action.run();
        } finally {
            if (shouldRemove) {
                entitlements.remove(e);
            }
        }
    }

    public static void checkEntitlement(Entitlement entitlement) {
        if (isActive && !ACTIVE_ENTITLEMENTS.get().contains(entitlement)) {
            throw new NotEntitledException("Missing entitlement: " + entitlement);
        }
    }
}
