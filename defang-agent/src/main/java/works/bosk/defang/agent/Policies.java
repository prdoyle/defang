package works.bosk.defang.agent;

import works.bosk.defang.runtime.InstanceMethod;

import java.io.File;

import static works.bosk.defang.runtime.Entitlement.FILES;

public interface Policies {
//	@InstanceMethod(REFLECTION)
//	Method getDeclaredMethod(Class<?> c, String name, Class<?>... parameterTypes);

	@InstanceMethod(FILES)
	boolean delete(File file);

}
