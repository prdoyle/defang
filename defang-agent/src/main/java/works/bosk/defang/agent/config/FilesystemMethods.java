package works.bosk.defang.agent.config;

import works.bosk.defang.agent.InstanceMethod;

import java.io.File;

import static works.bosk.defang.api.Entitlement.FILES;

@InstanceMethod(FILES)
public interface FilesystemMethods {
    boolean delete(File file);
}
