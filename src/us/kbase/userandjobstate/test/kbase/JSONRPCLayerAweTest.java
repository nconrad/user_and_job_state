package us.kbase.userandjobstate.test.kbase;

import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
//import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
//import java.util.Set;


import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.common.service.ServerException;
import us.kbase.common.test.controllers.mongo.MongoController;
import us.kbase.common.test.controllers.shock.ShockController;
import us.kbase.userandjobstate.InitProgress;
import us.kbase.userandjobstate.Result;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;
import us.kbase.userandjobstate.UserAndJobStateServer;
//import us.kbase.userandjobstate.test.FakeJob;
import us.kbase.userandjobstate.test.UserJobStateTestCommon;
import us.kbase.userandjobstate.test.awe.controller.AweController;
import us.kbase.userandjobstate.test.awe.controller.AweController.TestAweJob;

//TODO note about this only covering server ops, main tests cover all ops
public class JSONRPCLayerAweTest extends JSONRPCLayerTestUtils {
	
	private static UserAndJobStateServer SERVER = null;
	private static UserAndJobStateClient CLIENT1 = null;
	private static String USER1 = null;
	private static UserAndJobStateClient CLIENT2 = null;
	private static String USER2 = null;
	private static UserAndJobStateClient CLIENT3 = null;
	private static String USER3 = null;
	
	private static MongoController mongo;
	private static ShockController shock;
	private static AweController aweC;
	
	private static String shockURL;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		mongo = new MongoController(
				UserJobStateTestCommon.getMongoExe(),
				Paths.get(UserJobStateTestCommon.getTempDir()));
		System.out.println("Using Mongo temp dir " + mongo.getTempDir());
		
		shock = new ShockController(
				UserJobStateTestCommon.getShockExe(),
				Paths.get(UserJobStateTestCommon.getTempDir()),
				"***---fakeuser---***",
				"localhost:" + mongo.getServerPort(),
				"AweClientTests_ShockDB",
				"foo",
				"foo");
		System.out.println("Using Shock temp dir " + shock.getTempDir());
		
		shockURL = "http://localhost:" + shock.getServerPort();
		
		aweC = new AweController(
				new URL(shockURL),
				UserJobStateTestCommon.getAweExe(),
				UserJobStateTestCommon.getAweClientExe(),
				"localhost:" + mongo.getServerPort(),
				"AweClientTests_AweDB",
				"foo",
				"foo",
				Paths.get(UserJobStateTestCommon.getTempDir()));
		System.out.println("Awe temp dir is " + aweC.getTempDir());
		
		
		USER1 = System.getProperty("test.user1");
		USER2 = System.getProperty("test.user2");
		USER3 = System.getProperty("test.user3");
		String p1 = System.getProperty("test.pwd1");
		String p2 = System.getProperty("test.pwd2");
		String p3 = System.getProperty("test.pwd3");
		
		//write the server config file:
		File iniFile = File.createTempFile("test", ".cfg", new File("./"));
		iniFile.deleteOnExit();
		System.out.println("Created temporary config file: " + iniFile.getAbsolutePath());
		Ini ini = new Ini();
		Section ws = ini.add("UserAndJobState");
		ws.add("mongodb-host", "localhost:" + mongo.getServerPort());
		ws.add("mongodb-database", "JSONRPCLayerTest_DB");
		ws.add("mongodb-user", "foo");
		ws.add("mongodb-pwd", "foo");
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
		CLIENT3 = new UserAndJobStateClient(new URL("http://localhost:" + port), USER3, p3);
		CLIENT1.setIsInsecureHttpConnectionAllowed(true);
		CLIENT2.setIsInsecureHttpConnectionAllowed(true);
		CLIENT3.setIsInsecureHttpConnectionAllowed(true);
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
			aweC.destroy(UserJobStateTestCommon.getDeleteTempFiles());
		}
		if (shock != null) {
			shock.destroy(UserJobStateTestCommon.getDeleteTempFiles());
		}
		if (mongo != null) {
			mongo.destroy(UserJobStateTestCommon.getDeleteTempFiles());
		}
	}
	
	@Test
	public void getJob() throws Exception {
		TestAweJob j = aweC.createJob("myserv", "some desc");
		j.addTask();
		String jobidComplete = aweC.submitJob(j, CLIENT1.getToken());
		
		j = aweC.createJob("myserv 2", "some desc 2");
		j.addTask();
		j.addTask();
		j.addTask();
		String jobidComplete3 = aweC.submitJob(j, CLIENT1.getToken());
		
		j = aweC.createJob("myserv err", "some desc err");
		j.addTask();
		j.addErrorTask();
		j.addTask();
		String jobiderr = aweC.submitJob(j, CLIENT1.getToken());
		
		j = aweC.createJobWithNoClient("myserv q", "some desc q");
		j.addTask();
		String jobidq = aweC.submitJob(j, CLIENT1.getToken());
		
		//TODO restore when AWE fixed https://github.com/MG-RAST/AWE/issues/325
//		j = aweC.createJob("results", "res desc");
//		j.addIOTask(null, Arrays.asList("foo", "bar"), Arrays.asList(true, false));
//		j.addIOTask(Arrays.asList("bar"), Arrays.asList("baz", "boo"), Arrays.asList(false, true));
//		j.addIOTask(Arrays.asList("baz", "boo"), Arrays.asList("wugga"), Arrays.asList(false));
//		String jobres = aweC.submitJob(j, CLIENT1.getToken());
		
		System.out.println("Waiting 70s for jobs to run");
		Thread.sleep(70000);
		
		Results mtres = new Results().withResults(new LinkedList<Result>());
		checkJob(CLIENT1, jobidComplete, "complete", "", "myserv", "some desc", "task",
				1L, 1L, null, 1L, 0L, null, mtres);
		checkJob(CLIENT1, jobidComplete3, "complete", "", "myserv 2", "some desc 2", "task",
				3L, 3L, null, 1L, 0L, null, mtres);
		String err = "workunit " + jobiderr + "_1_0 failed 1 time(s).";
		checkJob(CLIENT1, jobiderr, "error", err, "myserv err", "some desc err", "task",
				1L, 3L, null, 0L, 1L, err, mtres);
		checkJob(CLIENT1, jobidq, "created", "", "myserv q", "some desc q", "task",
				0L, 1L, null, 0L, 0L, null, mtres);
		mtres.getResults().add(new Result().withServerType("Shock").withUrl(shockURL)
				.withDescription("bar")); //leave out id
		mtres.getResults().add(new Result().withServerType("Shock").withUrl(shockURL)
				.withDescription("baz")); //leave out id
		mtres.getResults().add(new Result().withServerType("Shock").withUrl(shockURL)
				.withDescription("wugga")); //leave out id
		//TODO restore when AWE fixed (see TD above)
//		checkJob(CLIENT1, jobres, "complete", "", "results", "res desc", "task",
//				3L, 3L, null, 1L, 0L, null, mtres, true);
		
		failGetJob(CLIENT1, "a0c44010-9ad6-4714-8280-9f05b1ae8bcc",
				String.format("There is no job %s viewable by user %s",
						"a0c44010-9ad6-4714-8280-9f05b1ae8bcc", USER1));
		failGetJob(CLIENT1, "a0c44010-9ad6-4714-8280-9f05b1ae8bc",
				String.format("Job ID %s is not a legal ID",
						"a0c44010-9ad6-4714-8280-9f05b1ae8bc"));
		failGetJob(CLIENT1, "", "id cannot be null or the empty string");
		failGetJob(CLIENT1, null, "id cannot be null or the empty string");
	}
	
	@Test
	public void shareJob() throws Exception {
		TestAweJob j = aweC.createJob("share serv", "share desc");
		j.addTask();
		String jobid = aweC.submitJob(j, CLIENT1.getToken());
		System.out.println("Waiting 20s for job to run");
		Thread.sleep(20000);
		CLIENT1.getJobInfo(jobid); //should work
		failShareJob(CLIENT2, jobid, Arrays.asList(USER1), String.format(
				"There is no job %s owned by user %s", jobid, USER2));
		failUnshareJob(CLIENT2, jobid, Arrays.asList(USER1), String.format(
				"User %s may only stop sharing job %s for themselves", USER2, jobid));
		
		failGetJob(CLIENT2, jobid, String.format(
				"There is no job %s viewable by user %s", jobid, USER2));
		CLIENT1.shareJob(jobid, Arrays.asList(USER2));
		
		failShareJob(CLIENT2, jobid, Arrays.asList(USER1), String.format(
				"There is no job %s owned by user %s", jobid, USER2));
		failUnshareJob(CLIENT2, jobid, Arrays.asList(USER1), String.format(
				"User %s may only stop sharing job %s for themselves", USER2, jobid));
		
		Results mtres = new Results().withResults(new LinkedList<Result>());
		checkJob(CLIENT2, jobid, "complete", "", "share serv", "share desc", "task",
				1L, 1L, null, 1L, 0L, null, mtres);
		CLIENT2.unshareJob(jobid, Arrays.asList(USER2));
		failGetJob(CLIENT2, jobid, String.format(
				"There is no job %s viewable by user %s", jobid, USER2));
		failShareJob(CLIENT2, jobid, Arrays.asList(USER1), String.format(
				"There is no job %s owned by user %s", jobid, USER2));
		failUnshareJob(CLIENT2, jobid, Arrays.asList(USER1), String.format(
				"User %s may only stop sharing job %s for themselves", USER2, jobid));
		
		CLIENT1.shareJob(jobid, Arrays.asList(USER2));
		CLIENT2.getJobInfo(jobid); //should work
		CLIENT1.unshareJob(jobid, Arrays.asList(USER2));
		failGetJob(CLIENT2, jobid, String.format(
				"There is no job %s viewable by user %s", jobid, USER2));
		
		failShareUnshareJob(CLIENT1, "foo", Arrays.asList(USER2),
				"Job ID foo is not a legal ID");
		failShareUnshareJob(CLIENT1, null, Arrays.asList(USER2),
				"id cannot be null or the empty string");
		failShareUnshareJob(CLIENT1, "", Arrays.asList(USER2),
				"id cannot be null or the empty string");
		String badid = "f47460e1-dbaa-46d2-840c-7b5f34604af9";
		failShareUnshareJob(CLIENT1, badid, Arrays.asList(USER2),
				 String.format("There is no job %s viewable by user %s", badid, USER1));
		failShareUnshareJob(CLIENT1, jobid, Arrays.asList("thishadbetterbeafakeuserorthistestwillfail"),
				"User thishadbetterbeafakeuserorthistestwillfail is not a valid user");
		
		failShareUnshareJob(CLIENT1, jobid, null, "The user list may not be null or empty");
		
		List<String> users = new ArrayList<>();
		failShareUnshareJob(CLIENT1, jobid, users, "The user list may not be null or empty");
		users.add(null);
		failShareUnshareJob(CLIENT1, jobid, users, "A user name cannot be null or the empty string");
		users.set(0, "");
		failShareUnshareJob(CLIENT1, jobid, users, "A user name cannot be null or the empty string");
		
		
		//test multiple users
		failGetJob(CLIENT3, jobid, String.format(
				"There is no job %s viewable by user %s", jobid, USER3));
		failGetJob(CLIENT3, jobid, String.format(
				"There is no job %s viewable by user %s", jobid, USER3));
		CLIENT1.shareJob(jobid, Arrays.asList(USER2, USER3));
		CLIENT2.getJobInfo(jobid);
		CLIENT3.getJobInfo(jobid);
		CLIENT1.unshareJob(jobid, Arrays.asList(USER2, USER3));
		failGetJob(CLIENT3, jobid, String.format(
				"There is no job %s viewable by user %s", jobid, USER3));
		failGetJob(CLIENT3, jobid, String.format(
				"There is no job %s viewable by user %s", jobid, USER3));
	}
	
	@Test
	public void delayedJob() throws Exception {
		TestAweJob j = aweC.createJob("delay serv", "delay desc");
		j.addDelayTask(20);
		j.addDelayTask(20);
		String jobid = aweC.submitJob(j, CLIENT1.getToken());
		
		System.out.println("Waiting 20s for job to run");
		Thread.sleep(20000);
		Results mtres = new Results().withResults(new LinkedList<Result>());
		checkJob(CLIENT1, jobid, "started", "", "delay serv", "delay desc", "task",
				0L, 2L, null, 0L, 0L, null, mtres);
		
		System.out.println("Waiting 20s for next task");
		Thread.sleep(20000);
		checkJob(CLIENT1, jobid, "started", "", "delay serv", "delay desc", "task",
				1L, 2L, null, 0L, 0L, null, mtres);
		
		System.out.println("Waiting 40s for job complete");
		Thread.sleep(40000);
		checkJob(CLIENT1, jobid, "complete", "", "delay serv", "delay desc", "task",
				2L, 2L, null, 1L, 0L, null, mtres);
	}
	
/*	@Test
	public void listJobs() throws Exception {
		//TODO 1 mix awe and ujs jobs in list jobs test
		//TODO 1 list jobs tests
		
		//TODO 1 start long running job and test
		Set<FakeJob> empty = new HashSet<FakeJob>();
		
		//this is just getting too fecking big. Just to some random Qs.
		checkListJobs(CLIENT2, USER1, null, empty);
		checkListJobs(CLIENT2, USER1, "", empty);
		checkListJobs(CLIENT2, USER1, "Q", empty); 
		checkListJobs(CLIENT2, USER1, "S", empty);
		checkListJobs(CLIENT2, USER1, "R", empty);
		checkListJobs(CLIENT2, USER1, "RQ", empty);
		checkListJobs(CLIENT2, USER1, "RS", empty);
		checkListJobs(CLIENT2, USER1, "C", empty);
		checkListJobs(CLIENT2, USER1, "CS", empty);
		checkListJobs(CLIENT2, USER1, "E", empty);
		checkListJobs(CLIENT2, USER1, "ES", empty);
		checkListJobs(CLIENT2, USER1, "ESQ", empty);
		checkListJobs(CLIENT2, USER1, "RC", empty);
		checkListJobs(CLIENT2, USER1, "RCS", empty);
		checkListJobs(CLIENT2, USER1, "CE", empty);
		checkListJobs(CLIENT2, USER1, "CES", empty);
		checkListJobs(CLIENT2, USER1, "RE", empty);
		checkListJobs(CLIENT2, USER1, "REQ", empty);
		checkListJobs(CLIENT2, USER1, "RES", empty);
		checkListJobs(CLIENT2, USER1, "RCE", empty);
		checkListJobs(CLIENT2, USER1, "RCES", empty);
		checkListJobs(CLIENT2, USER1, "RCEX", empty);
		checkListJobs(CLIENT2, USER1, "RCEXQ", empty);
		checkListJobs(CLIENT2, USER1, "RCEXS", empty);
		
		TestAweJob j = aweC.createJob("list serv", "some desc");
		j.addTask();
		String jobidComplete = aweC.submitJob(j, CLIENT2.getToken());
		
		j = aweC.createJob("list serv 2", "some desc 2");
		j.addTask();
		j.addTask();
		j.addTask();
		String jobidComplete3 = aweC.submitJob(j, CLIENT2.getToken());
		
		j = aweC.createJob("list serv err", "some desc err");
		j.addTask();
		j.addErrorTask();
		j.addTask();
		String jobiderr = aweC.submitJob(j, CLIENT2.getToken());
		
		j = aweC.createJobWithNoClient("list serv q", "some desc q");
		j.addTask();
		String jobidq = aweC.submitJob(j, CLIENT2.getToken());
		
		j = aweC.createJob("list results", "list res desc");
		j.addIOTask(null, Arrays.asList("foo", "bar"), Arrays.asList(true, false));
		j.addIOTask(Arrays.asList("bar"), Arrays.asList("baz", "boo"), Arrays.asList(false, true));
		j.addIOTask(Arrays.asList("baz", "boo"), Arrays.asList("wugga"), Arrays.asList(false));
		String jobres = aweC.submitJob(j, CLIENT2.getToken());
		
		System.out.println("Waiting 40s for jobs to run");
		Thread.sleep(40000);
		
	}*/
	
	@Test
	public void unavailableOps() throws Exception {
		TestAweJob j = aweC.createJob("share serv", "share desc");
		j.addTask();
		String jobid = aweC.submitJob(j, CLIENT1.getToken());
		String token = CLIENT1.getToken().toString();
		
		try {
			CLIENT1.completeJob(jobid, token, "stat", null, null);
			fail("shouldn't be able to complete AWE jobs");
		} catch (ServerException se) {
			assertThat("exception correct", se.getMessage(), is(
					"Awe jobs cannot be manually completed."));
		}
		try {
			CLIENT1.forceDeleteJob(token, jobid);
			fail("shouldn't be able to delete AWE jobs");
		} catch (ServerException se) {
			assertThat("exception correct", se.getMessage(), is(
					"Force deleting Awe jobs via the UJS is not allowed."));
		}
		try {
			CLIENT1.deleteJob(jobid);
			fail("shouldn't be able to delete AWE jobs");
		} catch (ServerException se) {
			assertThat("exception correct", se.getMessage(), is(
					"Deleting Awe jobs via the UJS is not yet implemented."));
		}
		try {
			CLIENT1.getJobOwner(jobid);
			fail("shouldn't be able to get AWE job owners");
		} catch (ServerException se) {
			assertThat("exception correct", se.getMessage(), is(
					"It is not currently possible to get the users associated with an Awe job."));
		}
		try {
			CLIENT1.getJobShared(jobid);
			fail("shouldn't be able to get AWE job shared list");
		} catch (ServerException se) {
			assertThat("exception correct", se.getMessage(), is(
					"It is not currently possible to get the users associated with an Awe job."));
		}
		try {
			CLIENT1.startJob(jobid, token, "stat", "desc", new InitProgress().withPtype("none"), null);
			fail("shouldn't be able to start AWE job");
		} catch (ServerException se) {
			assertThat("exception correct", se.getMessage(), is(
					"Awe jobs must be created and / or started from the Awe server."));
		}
		try {
			CLIENT1.startJob(jobid, token, "stat", "desc", new InitProgress().withPtype("task").withMax(3L), null);
			fail("shouldn't be able to start AWE job");
		} catch (ServerException se) {
			assertThat("exception correct", se.getMessage(), is(
					"Awe jobs must be created and / or started from the Awe server."));
		}
		try {
			CLIENT1.startJob(jobid, token, "stat", "desc", new InitProgress().withPtype("percent"), null);
			fail("shouldn't be able to start AWE job");
		} catch (ServerException se) {
			assertThat("exception correct", se.getMessage(), is(
					"Awe jobs must be created and / or started from the Awe server."));
		}
		try {
			CLIENT1.updateJob(jobid, token, "stat", null);
			fail("shouldn't be able to update AWE jobs");
		} catch (ServerException se) {
			assertThat("exception correct", se.getMessage(), is(
					"Awe jobs cannot be manually updated."));
		}
		try {
			CLIENT1.updateJobProgress(jobid, token, "stat", 4L, null);
			fail("shouldn't be able to update AWE jobs");
		} catch (ServerException se) {
			assertThat("exception correct", se.getMessage(), is(
					"Awe jobs cannot be manually updated."));
		}
	}
}
