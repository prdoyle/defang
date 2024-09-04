package works.bosk.defang.agent;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.bosk.defang.api.NotEntitledException;
import works.bosk.defang.runtime.InstanceMethod;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static works.bosk.defang.agent.ASMUtils.bytecode2text;

public class TransformerTest {
    public interface Helloable {
        String hello();
    }

    public static class ClassToInstrument implements Helloable {
        public String hello() {
            return "world";
        }
    }

    public static class Config {
        @InstanceMethod
        public static void hello(Class<?> callerClass, Helloable receiver) {
            throw new NotEntitledException("nope");
        }

    }

    @Test
    void test() throws NoSuchMethodException, IOException {
        // This test doesn't replace ClassToInstrument in-place but instead loads a separate
        // class ClassToInstrument_NEW that contains the instrumentation. Because of this,
        // we need to configure the Transformer to use a MethodKey and instrumentationMethod
        // with slightly different signatures (using the common interface Helloable) which
        // is not what would happen when it's run by the agent.
        var targetMethod = ClassToInstrument.class.getMethod("hello");
        var instrumentationMethod = Config.class.getMethod("hello", Class.class, Helloable.class);

        var transformer = new Transformer(
                Set.of(Type.getInternalName(ClassToInstrument.class)),
                Map.of(MethodKey.forTargetMethod(targetMethod), instrumentationMethod),
                "_NEW");
        var classFileName = "/" + Type.getInternalName(ClassToInstrument.class) + ".class";
        byte[] oldBytecode;
        try (var stream = ClassToInstrument.class.getResourceAsStream(classFileName)) {
            assert stream != null;
            oldBytecode = stream.readAllBytes();
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Before transformation: \n{}", bytecode2text(oldBytecode));
        }

        byte[] newBytecode = transformer.transform(
                ClassToInstrument.class.getClassLoader(),
                Type.getInternalName(ClassToInstrument.class),
                ClassToInstrument.class,
                null,
                oldBytecode
        );
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("After transformation: \n{}", bytecode2text(newBytecode));
        }

        Class<?> newClass = new TestLoader(Helloable.class.getClassLoader()).defineClassFromBytes(ClassToInstrument.class.getName() + "_NEW", newBytecode);

        assertEquals("world", new ClassToInstrument().hello());
        assertThrows(NotEntitledException.class, () -> ((Helloable)newClass.getConstructor().newInstance()).hello());
    }

    static class TestLoader extends ClassLoader {
        public TestLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> defineClassFromBytes(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformerTest.class);
}
