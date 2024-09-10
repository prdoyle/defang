package works.bosk.defang.runtime.config;

import works.bosk.defang.runtime.EntitlementChecks;
import works.bosk.defang.runtime.InstrumentationMethod;
import works.bosk.defang.runtime.InstrumentedParameter;

import java.io.InputStream;
import java.io.PrintStream;

import static works.bosk.defang.api.FlagEntitlement.EXIT;
import static works.bosk.defang.api.FlagEntitlement.SET_SYSTEM_FILES;

public class SystemMethods {
    @InstrumentationMethod(isStatic = true)
    public static void setIn(System system, InputStream in) {
        EntitlementChecks.checkFlagEntitlement(SET_SYSTEM_FILES);
    }

    @InstrumentationMethod(isStatic = true)
    public static void setOut(System system, PrintStream out) {
        EntitlementChecks.checkFlagEntitlement(SET_SYSTEM_FILES);
    }

    @InstrumentationMethod(isStatic = true)
    public static void setErr(System system, PrintStream err) {
        EntitlementChecks.checkFlagEntitlement(SET_SYSTEM_FILES);
    }

    @InstrumentationMethod(isStatic = true)
    public static void halt(@InstrumentedParameter(className = "java.lang.Shutdown") Object shutdown, int status) {
        EntitlementChecks.checkFlagEntitlement(EXIT);
    }
}
