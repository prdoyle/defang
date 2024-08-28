package works.bosk.defang.agent.config;

import works.bosk.defang.api.InstanceMethod;

import java.lang.reflect.Method;

import static works.bosk.defang.api.Entitlement.REFLECTION;

public interface ReflectionMethods {
	@InstanceMethod(REFLECTION)
	Method getDeclaredMethod(Class<?> c, String name, Class<?>... parameterTypes);

	@InstanceMethod(REFLECTION)
	Method[] getDeclaredMethods(Class<?> c);
}
