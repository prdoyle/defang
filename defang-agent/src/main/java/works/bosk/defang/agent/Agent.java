package works.bosk.defang.agent;

import org.objectweb.asm.Type;
import works.bosk.defang.runtime.InstanceMethod;
import works.bosk.defang.runtime.config.FilesystemMethods;
import works.bosk.defang.runtime.config.ReflectionMethods;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class Agent {
    public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException, IOException {
        var jarsString = System.getProperty("defang.runtimeJars");
        if (jarsString != null) {
            for (var jar : jarsString.split(File.pathSeparator)) {
                inst.appendToBootstrapClassLoaderSearch(new JarFile(jar));
            }
        }
        var scanResults = scanConfig(
                ReflectionMethods.class, FilesystemMethods.class
        );
        inst.addTransformer(new Transformer(
                scanResults.classesToRetransform.stream().map(Type::getInternalName).collect(toSet()),
                scanResults.instrumentationMethods),
                true);
        inst.retransformClasses(scanResults.classesToRetransform.toArray(new Class[0]));
    }

    record ScanResults(Map<MethodKey, Method> instrumentationMethods, Collection<Class<?>> classesToRetransform) {
    }

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
                Class<?> targetClass = method.getParameterTypes()[1];
                classesToRetransform.add(targetClass);
                Type[] targetParameters = Stream.of(method.getParameterTypes())
                        .skip(2)
                        .map(Type::getType)
                        .toArray(Type[]::new);
                String targetDescriptor = Type.getMethodDescriptor(
                        Type.VOID_TYPE, // We ignore the return type
                        targetParameters
                );
                methods.put(new MethodKey(
                                Type.getInternalName(targetClass),
                                method.getName(),
                                targetDescriptor),
                        method
                );
            }
        }
        return new ScanResults(methods, classesToRetransform);
    }

}
