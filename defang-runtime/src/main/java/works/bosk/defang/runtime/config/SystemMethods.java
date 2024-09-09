package works.bosk.defang.runtime.config;

import works.bosk.defang.api.FlagEntitlement;
import works.bosk.defang.runtime.EntitlementChecks;
import works.bosk.defang.runtime.StaticMethod;

import java.io.InputStream;
import java.io.PrintStream;

import static works.bosk.defang.api.FlagEntitlement.SET_SYSTEM_FILES;

public class SystemMethods {
    @StaticMethod
    public static void setIn(System system, InputStream in) {
        EntitlementChecks.checkFlagEntitlement(SET_SYSTEM_FILES);
    }

    @StaticMethod
    public static void setOut(System system, PrintStream out) {
        EntitlementChecks.checkFlagEntitlement(SET_SYSTEM_FILES);
    }

    @StaticMethod
    public static void setErr(System system, PrintStream err) {
        EntitlementChecks.checkFlagEntitlement(SET_SYSTEM_FILES);
    }
}
