package works.bosk.defang.agent;

import works.bosk.defang.api.InstanceMethod;

import java.io.File;

import static works.bosk.defang.api.Entitlement.FILES;

public interface Policies {
//	@InstanceMethod(REFLECTION)
//	Method getDeclaredMethod(Class<?> c, String name, Class<?>... parameterTypes);

	@InstanceMethod(FILES)
	boolean delete(File file);

}
