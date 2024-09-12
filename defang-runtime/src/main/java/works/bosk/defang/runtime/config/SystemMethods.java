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
    public static void setIn(Class<?> callerClass, System system, InputStream in) {
        EntitlementChecks.checkFlagEntitlement(callerClass, SET_SYSTEM_FILES);
    }

    @InstrumentationMethod(isStatic = true)
    public static void setOut(Class<?> callerClass, System system, PrintStream out) {
        EntitlementChecks.checkFlagEntitlement(callerClass, SET_SYSTEM_FILES);
    }

    @InstrumentationMethod(isStatic = true)
    public static void setErr(Class<?> callerClass, System system, PrintStream err) {
        EntitlementChecks.checkFlagEntitlement(callerClass, SET_SYSTEM_FILES);
    }

    @InstrumentationMethod(isStatic = true)
    public static void halt(Class<?> callerCLass, @InstrumentedParameter(className = "java.lang.Shutdown") Object shutdown, int status) {
        EntitlementChecks.checkFlagEntitlement(callerCLass, EXIT);
    }
}
