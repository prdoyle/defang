package works.bosk.defang.runtime.config;

import works.bosk.defang.runtime.EntitlementChecks;
import works.bosk.defang.runtime.InstrumentationMethod;

import java.io.File;

import static works.bosk.defang.api.OperationKind.WRITE;

public class FilesystemMethods {
    @InstrumentationMethod
    public static void delete(File file) {
        EntitlementChecks.checkFileEntitlement(file, WRITE);
    }
}
