import java.net.URL;

import org.junit.Test;

import us.kbase.userandjobstate.UserAndJobStateClient;

public class TestClientImport {
	
	@Test
	public void checkClientImport() throws Exception {
		UserAndJobStateClient c = new UserAndJobStateClient(
				new URL("http://johanngambolputtydevonausfernschplendenschlittercrasscrenbon.com"));
		c.isAuthAllowedForHttp();
		//ok all imports work
	}
}