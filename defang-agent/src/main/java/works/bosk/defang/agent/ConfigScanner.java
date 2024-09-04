package works.bosk.defang.agent;

import org.objectweb.asm.Type;
import works.bosk.defang.runtime.InstanceMethod;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

class ConfigScanner {
    static ScanResults scanConfig(Class<?>... configClasses) {
        var classesToRetransform = new HashSet<Class<?>>();
        var methods = new HashMap<MethodKey, Method>();
        for (var config : configClasses) {
            InstanceMethod classAnnotation = config.getAnnotation(InstanceMethod.class);
            for (Method instrumentationMethod : config.getDeclaredMethods()) {
                InstanceMethod annotation = instrumentationMethod.getAnnotation(InstanceMethod.class);
                if (annotation == null) {
                    annotation = classAnnotation;
                }
                if (annotation == null) {
                    continue;
                }
                if (instrumentationMethod.getParameterTypes().length < 2) {
                    throw new IllegalStateException("Instrumentation method's parameters should include at least the caller class and receiver object");
                }
                if (!instrumentationMethod.getParameterTypes()[0].equals(Class.class)) {
                    throw new IllegalStateException("First parameter of instrumentation method should be the caller class");
                }
                methods.put(MethodKey.forCorrespondingTargetMethod(instrumentationMethod), instrumentationMethod);
                classesToRetransform.add(instrumentationMethod.getParameterTypes()[1]);
            }
        }
        return new ScanResults(methods, classesToRetransform);
    }

    record ScanResults(Map<MethodKey, Method> instrumentationMethods, Set<Class<?>> classesToRetransform) {
    }
}
