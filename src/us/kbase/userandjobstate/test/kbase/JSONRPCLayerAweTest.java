package us.kbase.userandjobstate.test.kbase;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.userandjobstate.UserAndJobStateClient;
import us.kbase.userandjobstate.UserAndJobStateServer;
import us.kbase.userandjobstate.test.UserJobStateTestCommon;
import us.kbase.userandjobstate.test.awe.controller.AweController;
import us.kbase.userandjobstate.test.awe.controller.AweController.TestAweJob;

//TODO note about this only covering server ops, main tests cover all ops
public class JSONRPCLayerAweTest extends JSONRPCLayerTestUtils {
	
	private static final boolean DELETE_TEMP_FILES_ON_EXIT = false;

	//TODO deal with duplicate code here & in reg jrpc test
	
	private static UserAndJobStateServer SERVER = null;
	private static UserAndJobStateClient CLIENT1 = null;
	private static String USER1 = null;
	private static UserAndJobStateClient CLIENT2 = null;
	private static String USER2 = null;
	
	private static AweController aweC;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		UserJobStateTestCommon.destroyAndSetupAweDB();
		aweC = new AweController(
				new URL(UserJobStateTestCommon.getShockUrl()),
				UserJobStateTestCommon.getAweExe(),
				UserJobStateTestCommon.getAweClientExe(),
				UserJobStateTestCommon.getHost(),
				UserJobStateTestCommon.getAweDB(),
				UserJobStateTestCommon.getMongoUser(),
				UserJobStateTestCommon.getMongoPwd(),
				DELETE_TEMP_FILES_ON_EXIT);
		System.out.println("Awe temp dir is " + aweC.getTempDir());
		
		USER1 = System.getProperty("test.user1");
		USER2 = System.getProperty("test.user2");
		String p1 = System.getProperty("test.pwd1");
		String p2 = System.getProperty("test.pwd2");
		UserJobStateTestCommon.destroyAndSetupDB();
		
		//write the server config file:
		File iniFile = File.createTempFile("test", ".cfg", new File("./"));
		iniFile.deleteOnExit();
		System.out.println("Created temporary config file: " + iniFile.getAbsolutePath());
		Ini ini = new Ini();
		Section ws = ini.add("UserAndJobState");
		ws.add("mongodb-host", UserJobStateTestCommon.getHost());
		ws.add("mongodb-database", UserJobStateTestCommon.getDB());
		ws.add("mongodb-user", UserJobStateTestCommon.getMongoUser());
		ws.add("mongodb-pwd", UserJobStateTestCommon.getMongoPwd());
		ws.add("awe-url", "http://localhost:" + aweC.getServerPort());
		ini.store(iniFile);
		
		//set up env
		Map<String, String> env = getenv();
		env.put("KB_DEPLOYMENT_CONFIG", iniFile.getAbsolutePath());
		env.put("KB_SERVICE_NAME", "UserAndJobState");

		SERVER = new UserAndJobStateServer();
		new ServerThread(SERVER).start();
		System.out.println("Main thread waiting for server to start up");
		while(SERVER.getServerPort() == null) {
			Thread.sleep(1000);
		}
		int port = SERVER.getServerPort();
		System.out.println("Started test server on port " + port);
		System.out.println("Talking to awe server on port " + aweC.getServerPort());
		System.out.println("logging in users");
		CLIENT1 = new UserAndJobStateClient(new URL("http://localhost:" + port), USER1, p1);
		CLIENT2 = new UserAndJobStateClient(new URL("http://localhost:" + port), USER2, p2);
		CLIENT1.setIsInsecureHttpConnectionAllowed(true);
		CLIENT2.setIsInsecureHttpConnectionAllowed(true);
		System.out.println("Starting tests");
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		if (SERVER != null) {
			System.out.print("Killing server... ");
			SERVER.stopServer();
			System.out.println("Done");
		}
		if (aweC != null) {
			aweC.destroy();
		}
	}
	
	@Test
	public void getJob() throws Exception {
		TestAweJob j = aweC.createJob("myserv", "some desc");
		j.addTask();
		String jobid = aweC.submitJob(j, CLIENT1.getToken());
		System.out.println(CLIENT1.getJobInfo(jobid));
	}
	
}
