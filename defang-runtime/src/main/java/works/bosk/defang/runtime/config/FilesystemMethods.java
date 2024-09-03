package works.bosk.defang.runtime.config;

import works.bosk.defang.runtime.EntitlementChecks;
import works.bosk.defang.runtime.InstanceMethod;

import java.io.File;

import static works.bosk.defang.api.OperationKind.WRITE;

public class FilesystemMethods {
    @InstanceMethod
    public static void delete(Class<?> callerClass, File file) {
        EntitlementChecks.checkFileEntitlement(callerClass, file, WRITE);
    }
}
