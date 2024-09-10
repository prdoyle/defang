package works.bosk.defang.instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.bosk.defang.runtime.InstrumentationMethod;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigScanner {
    public static ScanResults scanConfig(Iterable<Class<?>> configClasses) {
        var classesToInstrument = new HashSet<Class<?>>();
        var methods = new HashMap<MethodKey, Method>();
        for (var config : configClasses) {
            for (Method instrumentationMethod : config.getDeclaredMethods()) {
                InstrumentationMethod im = instrumentationMethod.getAnnotation(InstrumentationMethod.class);
                if (im == null) {
                    LOGGER.trace("Skipping method " + instrumentationMethod);
                    continue;
                }
                if (instrumentationMethod.getParameterTypes().length < 1) {
                    throw new IllegalStateException("Instrumentation method's parameters should include at least the "
                            + (im.isStatic()? "receiver object" : "declaring class"));
                }
                MethodKey key = MethodKey.forCorrespondingTargetMethod(instrumentationMethod, im.isStatic());
                methods.put(key, instrumentationMethod);
                Class<?> classToInstrument;
                try {
                    classToInstrument = Class.forName(key.className().replace('/','.'));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
                classesToInstrument.add(classToInstrument);
            }
        }
        return new ScanResults(methods, classesToInstrument);
    }

    public record ScanResults(Map<MethodKey, Method> instrumentationMethods, Set<Class<?>> classesToInstrument) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigScanner.class);
}
