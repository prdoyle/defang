
package works.bosk.defang.agent;

import org.objectweb.asm.Type;
import works.bosk.defang.api.Entitlement;
import works.bosk.defang.api.InstanceMethod;
import works.bosk.defang.runtime.MethodKey;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Agent {
	public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
		inst.addTransformer(new Transformer(scanPolicies(Policies.class)), true);
		inst.retransformClasses(File.class);
	}

	static Map<MethodKey, Entitlement> scanPolicies(Class<?> policyClass) {
		Map<MethodKey, Entitlement> result = new HashMap<>();
		for (Method m : policyClass.getDeclaredMethods()) {
			InstanceMethod im = m.getAnnotation(InstanceMethod.class);
			String targetClass = Type.getInternalName(m.getParameterTypes()[0]);
			Type[] targetParameters = Stream.of(m.getParameterTypes())
				.skip(1)
				.map(Type::getType)
				.toArray(Type[]::new);
			String targetDescriptor = Type.getMethodDescriptor(
				Type.getType(m.getReturnType()),
				targetParameters
			);
			if (im != null) {
				result.put(new MethodKey(
						targetClass,
						m.getName(),
						targetDescriptor),
					im.value()
				);
			}
		}
		return result;
	}

}
