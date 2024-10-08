package works.bosk.defang.agent;

import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.bosk.defang.instrumentation.ConfigScanner;
import works.bosk.defang.instrumentation.Instrumenter;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarFile;

import static java.util.stream.Collectors.toSet;
import static works.bosk.defang.runtime.Config.CONFIG_CLASSES;

public class Agent {
    public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException, IOException {
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
            scanResults.classesToInstrument().stream().map(Type::getInternalName).collect(toSet())),
            true);
        LOGGER.trace("Starting retransformClasses");
        inst.retransformClasses(scanResults.classesToInstrument().toArray(new Class[0]));
        LOGGER.trace("All done!");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);
}
