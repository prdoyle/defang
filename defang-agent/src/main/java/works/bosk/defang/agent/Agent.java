package works.bosk.defang.agent;

import org.objectweb.asm.Type;
import works.bosk.defang.agent.config.FilesystemMethods;
import works.bosk.defang.agent.config.ReflectionMethods;
import works.bosk.defang.api.Entitlement;
import works.bosk.defang.api.InstanceMethod;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;

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
        inst.addTransformer(new Transformer(scanResults.entitlements), true);
        inst.retransformClasses(scanResults.classesToRetransform.toArray(new Class[0]));
    }

    record ScanResults(Map<MethodKey, Entitlement> entitlements, Collection<Class<?>> classesToRetransform) {
    }

    static ScanResults scanConfig(Class<?>... configClasses) {
        var classesToRetransform = new HashSet<Class<?>>();
        var entitlements = new HashMap<MethodKey, Entitlement>();
        for (var config : configClasses) {
            for (Method m : config.getDeclaredMethods()) {
                InstanceMethod im = m.getAnnotation(InstanceMethod.class);
                Class<?> targetClass = m.getParameterTypes()[0];
                classesToRetransform.add(targetClass);
                Type[] targetParameters = Stream.of(m.getParameterTypes())
                        .skip(1)
                        .map(Type::getType)
                        .toArray(Type[]::new);
                String targetDescriptor = Type.getMethodDescriptor(
                        Type.getType(m.getReturnType()),
                        targetParameters
                );
                if (im != null) {
                    entitlements.put(new MethodKey(
                                    Type.getInternalName(targetClass),
                                    m.getName(),
                                    targetDescriptor),
                            im.value()
                    );
                }
            }
        }
        return new ScanResults(entitlements, classesToRetransform);
    }

}
