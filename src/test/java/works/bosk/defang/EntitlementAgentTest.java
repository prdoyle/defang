package works.bosk.defang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntitlementAgentTest {
	@Test
	public void notEntitled_throws() {
		assertThrows(IllegalAccessError.class, getClass()::getDeclaredMethods);
	}
}
