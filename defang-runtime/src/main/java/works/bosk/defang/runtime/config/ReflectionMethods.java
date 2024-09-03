package works.bosk.defang.runtime.config;

import works.bosk.defang.runtime.InstanceMethod;

import java.lang.reflect.Method;

public interface ReflectionMethods {
    @InstanceMethod
    Method getDeclaredMethod(Class<?> callingClass, Class<?> c, String name, Class<?>... parameterTypes);

    @InstanceMethod
    Method[] getDeclaredMethods(Class<?> callingClass, Class<?> c);
}
