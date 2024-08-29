package works.bosk.defang.runtime.permission;

import works.bosk.defang.api.Entitlement;
import works.bosk.defang.api.NotEntitledException;

public class Permission {
    public static void checkPermission(Entitlement e, StackWalker.StackFrame caller) {
        // For testing
        if (caller.getMethodName().contains("NOT_PERMITTED")) {
            throw new NotEntitledException("Method is not entitled to " + e + ": " + caller);
        }
    }
}
