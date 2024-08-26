package works.bosk.defang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AgentTest {

	public static class TestClass {
		@Override
		public int hashCode() {
			return 42; // This should be overridden by the agent
		}
	}

	@Test
	public void testHashCodeRewrite() {
		TestClass obj = new TestClass();
		int expected = System.identityHashCode(obj);
		int actual = obj.hashCode();
		assertEquals(expected, actual);
	}
}
