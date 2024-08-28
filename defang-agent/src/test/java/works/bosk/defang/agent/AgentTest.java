package works.bosk.defang.agent;

import java.io.File;
import org.junit.jupiter.api.Test;
import works.bosk.defang.runtime.EntitlementChecking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static works.bosk.defang.api.Entitlement.FILES;

public class AgentTest {
	File file = new File("nonexistent");

	@Test
	public void notEntitled_throws() {
		assertThrows(IllegalStateException.class, file::delete);
	}

	@Test
	public void entitled_works() {
		EntitlementChecking.doEntitled(FILES, () -> assertFalse(file.delete()));
	}
}
