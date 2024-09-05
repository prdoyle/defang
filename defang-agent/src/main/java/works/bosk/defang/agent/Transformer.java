package works.bosk.defang.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.bosk.defang.instrumentation.Instrumenter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Set;

/**
 * A {@link ClassFileTransformer} that applies {@link Instrumenter} to the appropriate classes.
 */
public class Transformer implements ClassFileTransformer {
    private final Instrumenter instrumenter;
    private final Set<String> classesToTransform;

    public Transformer(Instrumenter instrumenter, Set<String> classesToTransform) {
        this.instrumenter = instrumenter;
        this.classesToTransform = classesToTransform;
        // TODO: Should warn if any MethodKey doesn't match any methods
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (classesToTransform.contains(className)) {
            return instrumenter.instrumentClass(className, classfileBuffer);
        } else {
            LOGGER.trace("Not transforming {}", className);
            return classfileBuffer;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Transformer.class);
}
