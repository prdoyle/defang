
package works.bosk.defang.agent;

import java.lang.instrument.Instrumentation;
import works.bosk.defang.transformer.Transformer;

public class Agent {
	public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(new Transformer(), true);
	}
}
