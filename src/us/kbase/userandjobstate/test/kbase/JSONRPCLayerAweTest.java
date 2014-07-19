package us.kbase.userandjobstate.test.kbase;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.userandjobstate.Result;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;
import us.kbase.userandjobstate.UserAndJobStateServer;
import us.kbase.userandjobstate.test.UserJobStateTestCommon;
import us.kbase.userandjobstate.test.awe.controller.AweController;
import us.kbase.userandjobstate.test.awe.controller.AweController.TestAweJob;

//TODO note about this only covering server ops, main tests cover all ops
public class JSONRPCLayerAweTest extends JSONRPCLayerTestUtils {
	
	private static final boolean DELETE_TEMP_FILES_ON_EXIT = true;

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
		
		UserAndJobStateServer.clearConfigForTests();
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
			System.out.println("Deleting Awe temporary directory");
			aweC.destroy();
		}
	}
	
	@Test
	public void getJob() throws Exception {
		TestAweJob j = aweC.createJob("myserv", "some desc");
		j.addTask();
		String jobid1 = aweC.submitJob(j, CLIENT1.getToken());
		j = aweC.createJob("myserv 2", "some desc 2");
		j.addTask();
		j.addTask();
		j.addTask();
		String jobid2 = aweC.submitJob(j, CLIENT1.getToken());
		j = aweC.createJob("myserv err", "some desc err");
		j.addTask();
		j.addErrorTask();
		j.addTask();
		String jobiderr = aweC.submitJob(j, CLIENT1.getToken());
		
		j = aweC.createJobWithNoClient("myserv q", "some desc q");
		j.addTask();
		String jobidq = aweC.submitJob(j, CLIENT1.getToken());
		
		System.out.println("Waiting 10s for jobs to run");
		Thread.sleep(10000);
		Results mtres = new Results().withResults(new LinkedList<Result>());
		checkJob(CLIENT1, jobid1, "complete", "", "myserv", "some desc", "task",
				1L, 1L, null, 1L, 0L, null, mtres);
		checkJob(CLIENT1, jobid2, "complete", "", "myserv 2", "some desc 2", "task",
				3L, 3L, null, 1L, 0L, null, mtres);
		String err = "workunit " + jobiderr + "_1_0 failed 1 time(s).";
		checkJob(CLIENT1, jobiderr, "error", err, "myserv err", "some desc err", "task",
				1L, 3L, null, 0L, 1L, err, mtres);
		checkJob(CLIENT1, jobidq, "created", "", "myserv q", "some desc q", "task",
				0L, 1L, null, 0L, 0L, null, mtres);
		//TODO 1 results
		//TODO bad job ids
	}
	
	@Test
	public void shareJob() throws Exception {
		TestAweJob j = aweC.createJob("share serv", "share desc");
		j.addTask();
		String jobid = aweC.submitJob(j, CLIENT1.getToken());
		System.out.println("Waiting 10s for job to run");
		Thread.sleep(10000);
		CLIENT1.getJobInfo(jobid); //should work
		
		failGetJob(CLIENT2, jobid, String.format(
				"There is no job %s viewable by user %s", jobid, USER2));
		CLIENT1.shareJob(jobid, Arrays.asList(USER2)); //TODO 1 need a lib level test that checks multiple users
		
		CLIENT2.getJobInfo(jobid); //should work
		CLIENT1.unshareJob(jobid, Arrays.asList(USER2));
		failGetJob(CLIENT2, jobid, String.format(
				"There is no job %s viewable by user %s", jobid, USER2));
		//TODO bad job ids, bad users
		//TODO illegal sharing
	}
	
	//TODO 1 mix awe and ujs jobs
	//TODO 1 failing and unavailable operations
	//TODO go through the awe job state class and check for tests
	
	@Test
	public void delayedJob() throws Exception {
		TestAweJob j = aweC.createJob("delay serv", "delay desc");
		j.addDelayTask(20);
		j.addDelayTask(20);
		String jobid = aweC.submitJob(j, CLIENT1.getToken());
		System.out.println(jobid);
		
		System.out.println("Waiting 10s for job to run");
		Thread.sleep(10000);
		Results mtres = new Results().withResults(new LinkedList<Result>());
		checkJob(CLIENT1, jobid, "started", "", "delay serv", "delay desc", "task",
				0L, 2L, null, 0L, 0L, null, mtres);
		
		System.out.println("Waiting 20s for next task");
		Thread.sleep(20000);
		checkJob(CLIENT1, jobid, "started", "", "delay serv", "delay desc", "task",
				1L, 2L, null, 0L, 0L, null, mtres);
		
		System.out.println("Waiting 20s for job complete");
		Thread.sleep(20000);
		checkJob(CLIENT1, jobid, "complete", "", "delay serv", "delay desc", "task",
				2L, 2L, null, 1L, 0L, null, mtres);
	}
}
