package works.bosk.defang.runtime;

import works.bosk.defang.runtime.config.FilesystemMethods;
import works.bosk.defang.runtime.config.ReflectionMethods;
import works.bosk.defang.runtime.config.SystemMethods;

import java.util.List;

public class Config {
    // It would be nice if this just picked up all the classes under config
    public static final List<Class<?>> CONFIG_CLASSES = List.of(
            FilesystemMethods.class,
            ReflectionMethods.class,
            SystemMethods.class
    );
}
