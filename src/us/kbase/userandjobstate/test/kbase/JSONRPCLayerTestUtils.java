package us.kbase.userandjobstate.test.kbase;

import java.lang.reflect.Field;
import java.util.Map;

import org.slf4j.LoggerFactory;

import us.kbase.userandjobstate.UserAndJobStateServer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class JSONRPCLayerTestUtils {

	protected static String CHAR101 = "";
	protected static String CHAR1001 = "";
	static {
		String hundred = "";
		for (int i = 0; i < 10; i++) {
			hundred += "0123456789";
		}
		CHAR101 = hundred + "a";
		String thousand = "";
		for (int i = 0; i < 10; i++) {
			thousand += hundred;
		}
		CHAR1001 = thousand + "a";
	}
	
	static {
		//stfu Jetty
		((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME))
				.setLevel(Level.OFF);
	}
	
	protected static class ServerThread extends Thread {

		private final UserAndJobStateServer server;
		
		public ServerThread(UserAndJobStateServer server) {
			this.server = server;
		}
		
		public void run() {
			try {
				server.startupServer();
			} catch (Exception e) {
				System.err.println("Can't start server:");
				e.printStackTrace();
			}
		}
	}
	
	//http://quirkygba.blogspot.com/2009/11/setting-environment-variables-in-java.html
	@SuppressWarnings("unchecked")
	public static Map<String, String> getenv() throws NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		Map<String, String> unmodifiable = System.getenv();
		Class<?> cu = unmodifiable.getClass();
		Field m = cu.getDeclaredField("m");
		m.setAccessible(true);
		return (Map<String, String>) m.get(unmodifiable);
	}
	
}
