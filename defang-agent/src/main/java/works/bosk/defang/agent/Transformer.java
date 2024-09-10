package works.bosk.defang.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.bosk.defang.instrumentation.Instrumenter;

import java.io.PrintWriter;
import java.io.StringWriter;
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
                            ProtectionDomain protectionDomain, byte[] existingBytes) {
        if (classesToTransform.contains(className)) {
            byte[] newBytes = instrumenter.instrumentClass(className, existingBytes);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Instrumented bytecodes for {}:\n{}", className, generateBytecodeString(newBytes));
            }
            return newBytes;
        } else {
            LOGGER.trace("Not transforming {}", className);
            return existingBytes;
        }
    }

    public static String generateBytecodeString(byte[] classBytes) {
        StringWriter stringWriter = new StringWriter();
        new ClassReader(classBytes).accept(new TraceClassVisitor(null, new Textifier(), new PrintWriter(stringWriter)), 0);
        return stringWriter.toString();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Transformer.class);
}
