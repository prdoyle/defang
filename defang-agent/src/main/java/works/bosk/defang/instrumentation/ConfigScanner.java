package works.bosk.defang.instrumentation;

import works.bosk.defang.runtime.InstanceMethod;
import works.bosk.defang.runtime.StaticMethod;

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
                InstanceMethod im = instrumentationMethod.getAnnotation(InstanceMethod.class);
                StaticMethod sm = instrumentationMethod.getAnnotation(StaticMethod.class);
                if (im == null && sm == null) {
                    continue;
                }
                if (instrumentationMethod.getParameterTypes().length < 2) {
                    throw new IllegalStateException("Instrumentation method's parameters should include at least the caller class and "
                            + ((im != null)? "receiver object" : "declaring class"));
                }
                if (!instrumentationMethod.getParameterTypes()[0].equals(Class.class)) {
                    throw new IllegalStateException("First parameter of instrumentation method should be the caller class");
                }
                methods.put(MethodKey.forCorrespondingTargetMethod(instrumentationMethod, sm != null), instrumentationMethod);
                classesToInstrument.add(instrumentationMethod.getParameterTypes()[1]);
            }
        }
        return new ScanResults(methods, classesToInstrument);
    }

    public record ScanResults(Map<MethodKey, Method> instrumentationMethods, Set<Class<?>> classesToInstrument) {
    }
}
