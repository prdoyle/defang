package works.bosk.defang;

import java.io.File;
import java.lang.reflect.Method;

import static works.bosk.defang.Entitlement.FILES;
import static works.bosk.defang.Entitlement.REFLECTION;

public interface Policies {
	@InstanceMethod(REFLECTION)
	Method getDeclaredMethod(Class<?> c, String name, Class<?>... parameterTypes);

	@InstanceMethod(FILES)
	boolean delete(File file);

}
