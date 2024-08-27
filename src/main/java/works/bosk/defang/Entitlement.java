package works.bosk.defang;

import java.util.EnumSet;
import java.util.Set;

public enum Entitlement {
	REFLECTION;

	private static final ThreadLocal<Set<Entitlement>> ACTIVE_PERMISSIONS = ThreadLocal.withInitial(() -> EnumSet.noneOf(Entitlement.class));

	public interface PrivilegedRunnable<X extends Throwable> {
		void run() throws X;
	}

	public static <X extends Throwable> void doEntitled(Entitlement p, PrivilegedRunnable<X> action) throws X {
		// TODO: Check that caller is allowed to ask for this
		Set<Entitlement> existingEntitlements = ACTIVE_PERMISSIONS.get();
		Set<Entitlement> newEntitlements = EnumSet.copyOf(existingEntitlements);
		newEntitlements.add(p);
		ACTIVE_PERMISSIONS.set(newEntitlements);
		try {
			action.run();
		} finally {
			ACTIVE_PERMISSIONS.set(existingEntitlements);
		}
	}

	public static void checkEntitlement(String entitlement) {
		if (!ACTIVE_PERMISSIONS.get().contains(Entitlement.valueOf(entitlement))) {
			throw new IllegalArgumentException("Missing entitlement: " + entitlement);
		}
	}
}
