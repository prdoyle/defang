package works.bosk.defang.agent;

import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.bosk.defang.instrumentation.ConfigScanner;
import works.bosk.defang.instrumentation.Instrumenter;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static works.bosk.defang.runtime.Config.CONFIG_CLASSES;

public class Agent {
    public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException, IOException, ClassNotFoundException {
        LOGGER.trace("Starting premain");
        var jarsString = System.getProperty("defang.runtimeJars");
        if (jarsString != null) {
            for (var jar : jarsString.split(File.pathSeparator)) {
                inst.appendToBootstrapClassLoaderSearch(new JarFile(jar));
            }
        }
        var scanResults = ConfigScanner.scanConfig(CONFIG_CLASSES);
        inst.addTransformer(new Transformer(
            new Instrumenter("", scanResults.instrumentationMethods()),
            scanResults.classesToInstrument().stream().map(Type::getInternalName).collect(toSet())));
        LOGGER.trace("Starting redefineClasses");
        var classesToInstrument = scanResults.classesToInstrument().toArray(new Class[0]);
        ClassDefinition[] classDefinitions = Stream.of(classesToInstrument)
                .map(c -> new ClassDefinition(c, originalBytecodes(c)))
                .toArray(ClassDefinition[]::new);
        inst.redefineClasses(classDefinitions);
        LOGGER.trace("All done!");
    }

    private static byte[] originalBytecodes(Class<?> c) {
        try {
            return Instrumenter.getClassFileInfo(c).bytecodes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);
}
