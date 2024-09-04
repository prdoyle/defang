package works.bosk.defang.runtime.config;

import works.bosk.defang.runtime.EntitlementChecks;
import works.bosk.defang.runtime.StaticMethod;

import java.io.InputStream;
import java.io.PrintStream;

import static works.bosk.defang.api.FlagEntitlement.SET_SYSTEM_FILES;

public class SystemMethods {
    @StaticMethod
    public static void setIn(Class<?> callerClass, System system, InputStream in) {
        EntitlementChecks.checkFlagEntitlement(callerClass, SET_SYSTEM_FILES);
    }

    @StaticMethod
    public static void setOut(Class<?> callerClass, System system, PrintStream out) {
        EntitlementChecks.checkFlagEntitlement(callerClass, SET_SYSTEM_FILES);
    }

    @StaticMethod
    public static void setErr(Class<?> callerClass, System system, PrintStream err) {
        EntitlementChecks.checkFlagEntitlement(callerClass, SET_SYSTEM_FILES);
    }
}
