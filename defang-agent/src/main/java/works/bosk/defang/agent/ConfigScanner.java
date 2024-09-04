package works.bosk.defang.agent;

import works.bosk.defang.runtime.InstanceMethod;
import works.bosk.defang.runtime.StaticMethod;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ConfigScanner {
    static ScanResults scanConfig(Class<?>... configClasses) {
        var classesToRetransform = new HashSet<Class<?>>();
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
                classesToRetransform.add(instrumentationMethod.getParameterTypes()[1]);
            }
        }
        return new ScanResults(methods, classesToRetransform);
    }

    record ScanResults(Map<MethodKey, Method> instrumentationMethods, Set<Class<?>> classesToRetransform) {
    }
}
