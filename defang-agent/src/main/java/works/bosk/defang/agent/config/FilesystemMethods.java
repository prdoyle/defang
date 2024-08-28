package works.bosk.defang.agent.config;

import works.bosk.defang.api.InstanceMethod;

import java.io.File;

import static works.bosk.defang.api.Entitlement.FILES;

public interface FilesystemMethods {
    @InstanceMethod(FILES)
    boolean delete(File file);
}
