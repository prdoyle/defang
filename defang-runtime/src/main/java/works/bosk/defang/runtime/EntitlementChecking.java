package works.bosk.defang.runtime;

import works.bosk.defang.api.Entitlement;
import works.bosk.defang.api.NotEntitledException;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class EntitlementChecking {

    private static final ThreadLocal<Set<Entitlement>> ACTIVE_ENTITLEMENTS = ThreadLocal.withInitial(() -> EnumSet.noneOf(Entitlement.class));
    private static final AtomicBoolean isActive = new AtomicBoolean(false);

    public static void activate() {
        isActive.set(true);
    }

    public static void deactivate() {
        isActive.set(false);
    }

    public interface PrivilegedRunnable<X extends Throwable> {
        void run() throws X;
    }

    public static <X extends Throwable> void doEntitled(Entitlement p, PrivilegedRunnable<X> action) throws X {
        // TODO: Check that caller is allowed to ask for this
        Set<Entitlement> existingEntitlements = ACTIVE_ENTITLEMENTS.get();
        Set<Entitlement> newEntitlements = EnumSet.copyOf(existingEntitlements);
        newEntitlements.add(p);
        ACTIVE_ENTITLEMENTS.set(newEntitlements);
        try {
            action.run();
        } finally {
            ACTIVE_ENTITLEMENTS.set(existingEntitlements);
        }
    }

    public static void checkEntitlement(String entitlement) {
        if (isActive.get() && !ACTIVE_ENTITLEMENTS.get().contains(Entitlement.valueOf(entitlement))) {
            throw new NotEntitledException("Missing entitlement: " + entitlement);
        }
    }
}
