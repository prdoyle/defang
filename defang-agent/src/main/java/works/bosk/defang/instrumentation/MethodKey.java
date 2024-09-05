package works.bosk.defang.instrumentation;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
        Class<?> targetClass = instrumentationMethod.getParameterTypes()[1];
        Type[] targetParameters = Stream.of(instrumentationMethod.getParameterTypes())
                .skip(2)
                .map(Type::getType)
                .toArray(Type[]::new);
        String targetDescriptor = Type.getMethodDescriptor(
                Type.VOID_TYPE, // We ignore the return type
                targetParameters
        );
        return new MethodKey(
                Type.getInternalName(targetClass),
                instrumentationMethod.getName(),
                targetDescriptor,
                isStatic);
    }

}
