package works.bosk.defang.instrumentation;

import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.bosk.defang.runtime.InstrumentedParameter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.stream.Stream;

public record MethodKey(
        String className,
        String methodName,
        String voidDescriptor,
        boolean isStatic
) {
    public static MethodKey forTargetMethod(Method targetMethod) {
        Type actualType = Type.getMethodType(Type.getMethodDescriptor(targetMethod));
        String voidDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, actualType.getArgumentTypes());
        return new MethodKey(
                Type.getInternalName(targetMethod.getDeclaringClass()),
                targetMethod.getName(),
                voidDescriptor,
                Modifier.isStatic(targetMethod.getModifiers()));
    }

    public static MethodKey forCorrespondingTargetMethod(Method instrumentationMethod, boolean isStatic) {
        LOGGER.trace("Instrumentation method " + instrumentationMethod);
        Type declaringType = parameterType(instrumentationMethod.getParameters()[1]);
        Type[] targetParameters = Stream.of(instrumentationMethod.getParameters())
                .skip(2)
                .map(MethodKey::parameterType)
                .toArray(Type[]::new);
        String targetDescriptor = Type.getMethodDescriptor(
                Type.VOID_TYPE, // We ignore the return type
                targetParameters
        );
        return new MethodKey(
                declaringType.getInternalName(),
                instrumentationMethod.getName(),
                targetDescriptor,
                isStatic);
    }

    private static Type parameterType(Parameter p) {
        var annotation = p.getAnnotation(InstrumentedParameter.class);
        if (annotation == null || "".equals(annotation.className())) {
            LOGGER.trace("Using actual type for " + p);
            return Type.getType(p.getType());
        } else {
            if (p.getType().isPrimitive()) {
                throw new IllegalStateException("Primitive parameters must not be annotated with @InstrumentedParameter: " + p);
            }
            LOGGER.trace("Using class name \"" + annotation.className() + "\" for " + p);
            return Type.getObjectType(annotation.className().replace('.', '/'));
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodKey.class);
}
