package works.bosk.defang.agent;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import static java.lang.System.identityHashCode;

@Disabled("Not actually a test")
public class ModuleTest {
    @Test
    void printLayers() {
        PrivilegedAction<List<Frame>> action = () -> StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(s -> s.map(f -> new Frame(f.getDeclaringClass(), f.getMethodName())).toList());
        var frames = AccessController.doPrivileged(action);
        frames.forEach(System.out::println);
    }

    record Frame(
            int layer,
            Module module,
            Class<?> class_,
            String methodName
    ){
        Frame(Class<?> class_, String methodName) {
            this(identityHashCode(class_.getModule().getLayer()), class_.getModule(), class_, methodName);
        }
    }
}
