package works.bosk.defang.agent;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.bosk.defang.api.NotEntitledException;
import works.bosk.defang.instrumentation.Instrumenter;
import works.bosk.defang.instrumentation.MethodKey;
import works.bosk.defang.runtime.InstrumentationMethod;
import works.bosk.defang.runtime.InstrumentedParameter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static works.bosk.defang.agent.ASMUtils.bytecode2text;

public class TransformerTest {
    public interface Helloable {
        String hello();
        String hello2();
    }

    public static class ClassToInstrument implements Helloable {
        public String hello() {
            return "world";
        }

        public String hello2() {
            return "world2";
        }

        public static String staticHello() {
            return "static world";
        }

        public static native String hello0();
    }

    public static class Config {
        @InstrumentationMethod
        public static void hello(Helloable receiver) {
            throw new NotEntitledException("nope");
        }

        @InstrumentationMethod
        public static void hello2(@InstrumentedParameter(className = "works.bosk.defang.agent.TransformerTest$ClassToInstrument") Object receiver) {
            throw new NotEntitledException("nope2");
        }

        @InstrumentationMethod(isStatic = true)
        public static void staticHello(Helloable declaringClass) {
            throw new NotEntitledException("nuh uh");
        }

        @InstrumentationMethod(isStatic = true)
        public static void hello0(Helloable declaringClass) {
            throw new NotEntitledException("denied");
        }
    }

    @Test
    void test() throws NoSuchMethodException, IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // This test doesn't replace ClassToInstrument in-place but instead loads a separate
        // class ClassToInstrument_NEW that contains the instrumentation. Because of this,
        // we need to configure the Transformer to use a MethodKey and instrumentationMethod
        // with slightly different signatures (using the common interface Helloable) which
        // is not what would happen when it's run by the agent.

        assertEquals(
                ClassToInstrument.class.getName(),
                Config.class.getDeclaredMethod("hello2", Object.class).getParameters()[0].getAnnotation(InstrumentedParameter.class).className(),
                "Bad test! @InstrumentedParameter annotation must specify the right class name!");

        var transformer = new Transformer(new Instrumenter("_NEW", Map.of(
                MethodKey.forTargetMethod(ClassToInstrument.class.getMethod("hello")), Config.class.getMethod("hello", Helloable.class),
                MethodKey.forTargetMethod(ClassToInstrument.class.getMethod("hello2")), Config.class.getMethod("hello2", Object.class),
                MethodKey.forTargetMethod(ClassToInstrument.class.getMethod("staticHello")), Config.class.getMethod("staticHello", Helloable.class),
                MethodKey.forTargetMethod(ClassToInstrument.class.getMethod("hello0")), Config.class.getMethod("hello0", Helloable.class)
        )), Set.of(Type.getInternalName(ClassToInstrument.class)));
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
        assertEquals("world2", new ClassToInstrument().hello2());
        assertEquals("static world", ClassToInstrument.staticHello());
        assertEquals("static world", callStaticHello(ClassToInstrument.class));
        Helloable newInstance = (Helloable) newClass.getConstructor().newInstance();
        assertThrows(NotEntitledException.class, newInstance::hello);
        assertThrows(NotEntitledException.class, newInstance::hello2);
        assertThrows(NotEntitledException.class, () -> callStaticHello(newClass));
        assertThrows(NotEntitledException.class, () -> callHello0(newClass));
    }

    private static String callStaticHello(Class<?> c) throws NoSuchMethodException, IllegalAccessException {
        try {
            return (String) c
                    .getMethod("staticHello")
                    .invoke(null);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NotEntitledException n) {
                // Sometimes we're expecting this one!
                throw n;
            } else {
                throw new AssertionError(cause);
            }
        }
    }

    private static String callHello0(Class<?> c) throws NoSuchMethodException, IllegalAccessException {
        try {
            return (String) c
                    .getMethod("hello0")
                    .invoke(null);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NotEntitledException n) {
                // Sometimes we're expecting this one!
                throw n;
            } else {
                throw new AssertionError(cause);
            }
        }
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
