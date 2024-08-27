package works.bosk.defang;

import java.lang.reflect.Method;

import static works.bosk.defang.Entitlement.REFLECTION;

public interface Policies {
	@InstanceMethod(REFLECTION)
	Method getDeclaredMethod(Class<?> c, String name, Class<?>... parameterTypes);

}
