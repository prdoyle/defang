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
            for (Method method : config.getDeclaredMethods()) {
                InstanceMethod annotation = method.getAnnotation(InstanceMethod.class);
                if (annotation == null) {
                    annotation = classAnnotation;
                }
                if (annotation == null) {
                    continue;
                }
                if (method.getParameterTypes().length < 2) {
                    throw new IllegalStateException("Instrumentation method's parameters should include at least the caller class and receiver object");
                }
                if (!method.getParameterTypes()[0].equals(Class.class)) {
                    throw new IllegalStateException("First parameter of instrumentation method should be the caller class");
                }
                methods.put(methodKey(method), method);
                classesToRetransform.add(method.getParameterTypes()[1]);
            }
        }
        return new ScanResults(methods, classesToRetransform);
    }

    static MethodKey methodKey(Method method) {
        Class<?> targetClass = method.getParameterTypes()[1];
        Type[] targetParameters = Stream.of(method.getParameterTypes())
                .skip(2)
                .map(Type::getType)
                .toArray(Type[]::new);
        String targetDescriptor = Type.getMethodDescriptor(
                Type.VOID_TYPE, // We ignore the return type
                targetParameters
        );
        return new MethodKey(
                Type.getInternalName(targetClass),
                method.getName(),
                targetDescriptor);
    }

    record ScanResults(Map<MethodKey, Method> instrumentationMethods, Set<Class<?>> classesToRetransform) {
    }
}
