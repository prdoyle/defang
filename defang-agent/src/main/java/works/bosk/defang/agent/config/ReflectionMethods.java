package works.bosk.defang.agent.config;

import works.bosk.defang.agent.InstanceMethod;

import java.lang.reflect.Method;

import static works.bosk.defang.api.Entitlement.REFLECTION;

@InstanceMethod(REFLECTION)
public interface ReflectionMethods {
    Method getDeclaredMethod(Class<?> c, String name, Class<?>... parameterTypes);
    Method[] getDeclaredMethods(Class<?> c);
}
