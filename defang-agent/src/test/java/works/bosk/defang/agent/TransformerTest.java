package works.bosk.defang.agent;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import works.bosk.defang.api.NotEntitledException;
import works.bosk.defang.runtime.InstanceMethod;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static works.bosk.defang.agent.ASMUtils.bytecode2text;
import static works.bosk.defang.agent.ConfigScanner.methodKey;

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
        public void hello(Class<?> callerClass, ClassToInstrument receiver) {
            throw new NotEntitledException("nope");
        }

    }

    @Test
    void test() throws NoSuchMethodException, IOException {
        var helloMethod = Config.class.getMethod("hello", Class.class, ClassToInstrument.class);
        var transformer = new Transformer(
                Set.of(Type.getInternalName(ClassToInstrument.class)),
                Map.of(methodKey(helloMethod), helloMethod),
                "_NEW");
        var classFileName = "/" + Type.getInternalName(ClassToInstrument.class) + ".class";
        byte[] oldBytecode;
        try (var stream = ClassToInstrument.class.getResourceAsStream(classFileName)) {
            assert stream != null;
            oldBytecode = stream.readAllBytes();
        }
        System.out.println("Before transformation: \n" + bytecode2text(oldBytecode));

        byte[] newBytecode = transformer.transform(
                ClassToInstrument.class.getClassLoader(),
                Type.getInternalName(ClassToInstrument.class),
                ClassToInstrument.class,
                null,
                oldBytecode
        );
        System.out.println("After transformation: \n" + bytecode2text(newBytecode));

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
}
