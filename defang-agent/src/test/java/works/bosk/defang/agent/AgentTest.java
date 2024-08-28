package works.bosk.defang.agent;

import java.io.File;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AgentTest {
	@Test
	public void notEntitled_throws() {
		File file = new File("nonexistent");
		assertThrows(IllegalAccessError.class, file::delete);
	}
}
