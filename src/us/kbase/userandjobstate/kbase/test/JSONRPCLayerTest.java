package us.kbase.userandjobstate.kbase.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.auth.AuthService;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple12;
import us.kbase.common.service.Tuple4;
import us.kbase.common.service.Tuple6;
import us.kbase.common.service.UObject;
import us.kbase.userandjobstate.InitProgress;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;
import us.kbase.userandjobstate.UserAndJobStateServer;
import us.kbase.userandjobstate.test.UserJobStateTestCommon;

/*
 * These tests are specifically for testing the JSON-RPC communications between
 * the client, up to the invocation of the {@link us.kbase.userandjobstate.userstate.UserState}
 * and {@link us.kbase.userandjobstate.jobstate.JobState}
 * methods. As such they do not test the full functionality of the methods;
 * {@link us.kbase.userandjobstate.userstate.test.UserStateTests} and
 * {@link us.kbase.userandjobstate.jobstate.test.JobStateTests} handles that.
 */
public class JSONRPCLayerTest {
	
	private static UserAndJobStateServer SERVER = null;
	private static UserAndJobStateClient CLIENT1 = null;
	private static String USER1 = null;
	private static UserAndJobStateClient CLIENT2 = null;
	private static String USER2 = null;
	private static String token1;
	private static String token2;

	private static class ServerThread extends Thread {
		
		public void run() {
			try {
				SERVER.startupServer();
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
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		//TODO catch exceptions and print nice errors - next deploy
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
		ini.store(iniFile);
		
		//set up env
		Map<String, String> env = getenv();
		env.put("KB_DEPLOYMENT_CONFIG", iniFile.getAbsolutePath());
		env.put("KB_SERVICE_NAME", "UserAndJobState");

		SERVER = new UserAndJobStateServer();
		new ServerThread().start();
		System.out.println("Main thread waiting for server to start up");
		while(SERVER.getServerPort() == null) {
			Thread.sleep(1000);
		}
		int port = SERVER.getServerPort();
		System.out.println("Started test server on port " + port);
		System.out.println("Starting tests");
		CLIENT1 = new UserAndJobStateClient(new URL("http://localhost:" + port), USER1, p1);
		CLIENT2 = new UserAndJobStateClient(new URL("http://localhost:" + port), USER2, p2);
		CLIENT1.setAuthAllowedForHttp(true);
		CLIENT2.setAuthAllowedForHttp(true);
		token1 = AuthService.login(USER1, p1).getTokenString();
		token2 = AuthService.login(USER2, p2).getTokenString();
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		if (SERVER != null) {
			System.out.print("Killing server... ");
			SERVER.stopServer();
			System.out.println("Done");
		}
	}
	
	@Test
	public void unicode() throws Exception {
		int[] char1 = {11614};
		String uni = new String(char1, 0, 1);
		CLIENT1.setState("uni", "key", new UObject(uni));
		assertThat("get unicode back",
				CLIENT1.getState("uni", "key", 0)
				.asClassInstance(Object.class),
				is((Object) uni));
		CLIENT1.removeState("uni", "key");
	}
	
	@Test
	public void getSetListStateService() throws Exception {
		List<Integer> data = Arrays.asList(1, 2, 3, 5, 8, 13);
		List<Integer> data2 = Arrays.asList(42);
		CLIENT1.setState("serv1", "key1", new UObject(data));
		CLIENT2.setState("serv1", "2key1", new UObject(data2));
		CLIENT1.setStateAuth(token2, "akey1", new UObject(data));
		CLIENT2.setStateAuth(token1, "2akey1", new UObject(data2));
		assertThat("get correct data back",
				CLIENT1.getState("serv1", "key1", 0)
				.asClassInstance(Object.class),
				is((Object) data));
		assertThat("get correct data back",
				CLIENT2.getState("serv1", "2key1", 0)
				.asClassInstance(Object.class),
				is((Object) data2));
		assertThat("get correct data back",
				CLIENT1.getState(USER2, "akey1", 1)
				.asClassInstance(Object.class),
				is((Object) data));
		assertThat("get correct data back",
				CLIENT2.getState(USER1, "2akey1", 1)
				.asClassInstance(Object.class),
				is((Object) data2));
		CLIENT1.setState("serv1", "key2", new UObject(data));
		CLIENT1.setState("serv2", "key", new UObject(data));
		CLIENT1.setStateAuth(token2, "akey2", new UObject(data));
		CLIENT1.setStateAuth(token1, "akey", new UObject(data));
		assertThat("get correct keys",
				new HashSet<String>(CLIENT1.listState("serv1", 0)),
				is(new HashSet<String>(Arrays.asList("key1", "key2"))));
		assertThat("get correct keys",
				new HashSet<String>(CLIENT1.listState("serv2", 0)),
				is(new HashSet<String>(Arrays.asList("key"))));
		assertThat("get correct keys",
				new HashSet<String>(CLIENT1.listState(USER2, 1)),
				is(new HashSet<String>(Arrays.asList("akey1", "akey2"))));
		assertThat("get correct keys",
				new HashSet<String>(CLIENT1.listState(USER1, 1)),
				is(new HashSet<String>(Arrays.asList("akey"))));
		assertThat("get correct services",
				new HashSet<String>(CLIENT1.listStateServices(0)),
				is(new HashSet<String>(Arrays.asList("serv1", "serv2"))));
		assertThat("get correct services",
				new HashSet<String>(CLIENT1.listStateServices(1)),
				is(new HashSet<String>(Arrays.asList(USER1, USER2))));
		CLIENT1.removeState("serv1", "key1");
		CLIENT1.removeStateAuth(token2, "akey1");
		try {
			CLIENT1.getState("serv1", "key1", 0);
			fail("got deleted state");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is("There is no key key1 for the unauthorized service serv1"));
		}
		try {
			CLIENT1.getState(USER2, "akey1", 1);
			fail("got deleted state");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is("There is no key akey1 for the authorized service " + USER2));
		}
	}
	
	@Test
	public void badToken() throws Exception {
		try {
			CLIENT1.setStateAuth(null, "key", new UObject("foo"));
			fail("set state w/ bad token");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is("Service token cannot be null or the empty string"));
		}
		try {
			CLIENT1.setStateAuth("", "key", new UObject("foo"));
			fail("set state w/ bad token");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is("Service token cannot be null or the empty string"));
		}
		try {
			CLIENT1.setStateAuth("boogabooga", "key", new UObject("foo"));
			fail("set state w/ bad token");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is("Auth token is in the incorrect format, near 'boogabooga'"));
		}
		/* TODO restore when auth service is fixed
		try {
			CLIENT1.setStateAuth(token2 + "a", "key", new UObject("foo"));
			fail("set state w/ bad token");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is("Service token is invalid"));
		}*/
	}
	
	@Test
	public void createAndStartJob() throws Exception {
		String jobid = CLIENT1.createJob();
		checkJob(jobid, "created", null, null, null,
				null, null, null, null, null, null);
		CLIENT1.startJob(jobid, token2, "new stat", "ne desc",
				new InitProgress().withPtype("none"));
		checkJob(jobid, "started", "new stat", USER2,
				"ne desc", "none", null, null, 0, 0, null);
		
		jobid = CLIENT1.createJob();
		CLIENT1.startJob(jobid, token2, "new stat2", "ne desc2",
				new InitProgress().withPtype("percent"));
		checkJob(jobid, "started", "new stat2", USER2,
				"ne desc2", "percent", 0, 100, 0, 0, null);
		
		jobid = CLIENT1.createJob();
		CLIENT1.startJob(jobid, token2, "new stat3", "ne desc3",
				new InitProgress().withPtype("task").withMax(5));
		checkJob(jobid, "started", "new stat3", USER2,
				"ne desc3", "task", 0, 5, 0, 0, null);
		
		testStartJob(null, token2, "s", "d", new InitProgress().withPtype("none"),
				"id cannot be null or the empty string", true);
		testStartJob("", token2, "s", "d", new InitProgress().withPtype("none"),
				"id cannot be null or the empty string", true);
		testStartJob("aaaaaaaaaaaaaaaaaaaa", token2, "s", "d",
				new InitProgress().withPtype("none"),
				"Job ID aaaaaaaaaaaaaaaaaaaa is not a legal ID", true);
		
		jobid = CLIENT1.createJob();
		testStartJob(jobid, null, "s", "d", new InitProgress().withPtype("none"),
				"Service token cannot be null or the empty string");
		testStartJob(jobid, "foo", "s", "d", new InitProgress().withPtype("none"),
				"Auth token is in the incorrect format, near 'foo'");
		//TODO restore when auth service is fixed
//		testStartJob(jobid, token2 + "a", "s", "d", new InitProgress().withPtype("none"),
//				"id cannot be null or the empty string");
		
		testStartJob(jobid, token2, "s", "d", null,
				"InitProgress cannot be null");
		InitProgress ip = new InitProgress().withPtype("none");
		ip.setAdditionalProperties("foo", "bar");
		testStartJob(jobid, token2, "s", "d", ip,
				"Unexpected arguments in InitProgress: foo");
		testStartJob(jobid, token2, "s", "d", new InitProgress().withPtype(null),
				"Progress type cannot be null");
		testStartJob(jobid, token2, "s", "d", new InitProgress().withPtype("foo"),
				"No such progress type: foo");
		testStartJob(jobid, token2, "s", "d", new InitProgress().withPtype("task")
				.withMax(null),
				"Max progress cannot be null for task based progress");
		
		
		jobid = CLIENT1.createAndStartJob(token2, "cs stat", "cs desc",
				new InitProgress().withPtype("none"));
		checkJob(jobid, "started", "cs stat", USER2,
				"cs desc", "none", null, null, 0, 0, null);
		
		jobid = CLIENT1.createAndStartJob(token2, "cs stat2", "cs desc2",
				new InitProgress().withPtype("percent"));
		checkJob( jobid, "started", "cs stat2", USER2,
				"cs desc2", "percent", 0, 100, 0, 0, null);
		
		jobid = CLIENT1.createAndStartJob(token2, "cs stat3", "cs desc3",
				new InitProgress().withPtype("task").withMax(5));
		checkJob(jobid, "started", "cs stat3", USER2,
				"cs desc3", "task", 0, 5, 0, 0, null);
	}
	
	private void testStartJob(String jobid, String token, String stat, String desc,
			InitProgress prog, String exception) throws Exception {
		testStartJob(jobid, token, stat, desc, prog, exception, false);
	}
	
	private void testStartJob(String jobid, String token, String stat, String desc,
			InitProgress prog, String exception, boolean badid) throws Exception {
		try {
			CLIENT1.startJob(jobid, token, stat, desc, prog);
			fail("started job w/ bad args");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
		if (badid) {
			return;
		}
		try {
			CLIENT1.createAndStartJob(token, stat, desc, prog);
			fail("started job w/ bad args");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
	}
	
	private static SimpleDateFormat DATE_FORMAT =
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	
	private void checkJob(String id, String stage, String status,
			String service, String desc, String progtype, Integer prog,
			Integer maxprog, Integer complete, Integer error,
			Results results)
			throws Exception {
		Tuple12<String, String, String, String, String,
				Integer, Integer, String, Integer, Integer, String, Results> ret 
				= CLIENT1.getJobInfo(id);
		assertThat("job id ok", ret.getE1(), is(id));
		assertThat("job stage ok", ret.getE3(), is(stage));
		assertThat("job service ok", ret.getE2(), is(service));
		assertThat("job desc ok", ret.getE11(), is(desc));
		assertThat("job progtype ok", ret.getE8(), is(progtype));
		assertThat("job prog ok", ret.getE6(), is(prog));
		assertThat("job maxprog ok", ret.getE7(), is(maxprog));
		assertThat("job status ok", ret.getE4(), is(status));
		DATE_FORMAT.parse(ret.getE5()); //should throw error if bad format
		assertThat("job complete ok", ret.getE9(), is(complete));
		assertThat("job error ok", ret.getE10(), is(error));
		checkResults(ret.getE12(), results);
		
		Tuple4<String, String, Integer, String> jobdesc =
				CLIENT1.getJobDescription(id);
		assertThat("job service ok", jobdesc.getE1(), is(service));
		assertThat("job progtype ok", jobdesc.getE2(), is(progtype));
		assertThat("job maxprog ok", jobdesc.getE3(), is(maxprog));
		assertThat("job desc ok", jobdesc.getE4(), is(desc));
		
		Tuple6<String, String, String, Integer, Integer, Integer> jobstat =
				CLIENT1.getJobStatus(id);
		DATE_FORMAT.parse(jobstat.getE1()); //should throw error if bad format
		assertThat("job stage ok", jobstat.getE2(), is(stage));
		assertThat("job status ok", jobstat.getE3(), is(status));
		assertThat("job progress ok", jobstat.getE4(), is(prog));
		assertThat("job complete ok", jobstat.getE5(), is(complete));
		assertThat("job error ok", jobstat.getE6(), is(error));
		
		checkResults(CLIENT1.getResults(id), results);
	}
	
	private void checkResults(Results got, Results expected) throws Exception {
		if (got == null & expected == null) {
			return;
		}
		if (got == null ^ expected == null) {
			fail("got null for results when expected real results or vice versa: " 
					+ got + " " + expected);
		}
		assertThat("shock ids same", got.getShocknodes(), is(expected.getShocknodes()));
		assertThat("shock url same", got.getShockurl(), is(expected.getShockurl()));
		assertThat("ws ids same", got.getWorkspaceids(), is(expected.getWorkspaceids()));
		assertThat("ws url same", got.getWorkspaceurl(), is(expected.getWorkspaceurl()));
	}
	
	@Test
	public void getJobInfoBadArgs() throws Exception {
		testGetJobBadArgs(null, "id cannot be null or the empty string");
		testGetJobBadArgs("", "id cannot be null or the empty string");
		testGetJobBadArgs("foo", "Job ID foo is not a legal ID");
		
		String jobid = CLIENT1.createJob();
		if (jobid.charAt(0) == 'a') {
			jobid = "b" + jobid.substring(1);
		} else {
			jobid = "a" + jobid.substring(1);
		}
		testGetJobBadArgs(jobid, String.format(
				"There is no job %s for user kbasetest", jobid));
	}
	
	private void testGetJobBadArgs(String jobid, String exception)
			throws Exception {
		try {
			CLIENT1.getJobInfo(jobid);
			fail("got job with bad id");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
		try {
			CLIENT1.getJobDescription(jobid);
			fail("got job with bad id");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
		try {
			CLIENT1.getJobStatus(jobid);
			fail("got job with bad id");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
		try {
			CLIENT1.getResults(jobid);
			fail("got job with bad id");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
	}
	
	@Test
	public void updateJob() throws Exception {
		String jobid = CLIENT1.createAndStartJob(token2, "up stat", "up desc",
				new InitProgress().withPtype("none"));
		CLIENT1.updateJob(jobid, token2, "up stat2");
		checkJob(jobid, "started", "up stat2", USER2,
				"up desc", "none", null, null, 0, 0, null);
		CLIENT1.updateJobProgress(jobid, token2, "up stat3", 40);
		checkJob(jobid, "started", "up stat3", USER2,
				"up desc", "none", null, null, 0, 0, null);
		CLIENT1.updateJobProgress(jobid, token2, "up stat3", null);
		checkJob(jobid, "started", "up stat3", USER2,
				"up desc", "none", null, null, 0, 0, null);
		
		jobid = CLIENT1.createAndStartJob(token2, "up2 stat", "up2 desc",
				new InitProgress().withPtype("percent"));
		CLIENT1.updateJobProgress(jobid, token2, "up2 stat2", 40);
		checkJob(jobid, "started", "up2 stat2", USER2,
				"up2 desc", "percent", 40, 100, 0, 0, null);
		CLIENT1.updateJob(jobid, token2, "up2 stat3");
		checkJob(jobid, "started", "up2 stat3", USER2,
				"up2 desc", "percent", 40, 100, 0, 0, null);
		CLIENT1.updateJobProgress(jobid, token2, "up2 stat4", 70);
		checkJob(jobid, "started", "up2 stat4", USER2,
				"up2 desc", "percent", 100, 100, 0, 0, null);
		
		jobid = CLIENT1.createAndStartJob(token2, "up3 stat", "up3 desc",
				new InitProgress().withPtype("task").withMax(42));
		CLIENT1.updateJobProgress(jobid, token2, "up3 stat2", 30);
		checkJob(jobid, "started", "up3 stat2", USER2,
				"up3 desc", "task", 30, 42, 0, 0, null);
		CLIENT1.updateJob(jobid, token2, "up3 stat3");
		checkJob( jobid, "started", "up3 stat3", USER2,
				"up3 desc", "task", 30, 42, 0, 0, null);
		CLIENT1.updateJobProgress(jobid, token2, "up3 stat4", 15);
		checkJob(jobid, "started", "up3 stat4", USER2,
				"up3 desc", "task", 42, 42, 0, 0, null);
		
		jobid = CLIENT1.createAndStartJob(token2, "up4 stat", "up4 desc",
				new InitProgress().withPtype("none"));
		testUpdateJob(jobid, token2, "up4 stat2", -1, "progress cannot be negative");
		
		testUpdateJob(null, token2, "s",
				"id cannot be null or the empty string");
		testUpdateJob("", token2, "s",
				"id cannot be null or the empty string");
		testUpdateJob("aaaaaaaaaaaaaaaaaaaa", token2, "s",
				"Job ID aaaaaaaaaaaaaaaaaaaa is not a legal ID");
		
		testUpdateJob(jobid, null, "s",
				"Service token cannot be null or the empty string");
		testUpdateJob(jobid, "foo", "s",
				"Auth token is in the incorrect format, near 'foo'");
		testUpdateJob(jobid, token1, "s", String.format(
				"There is no uncompleted job %s for user kbasetest started by service kbasetest",
				jobid, USER1, USER1));
		
		
		//TODO restore when auth service is fixed
//		testStartJob(jobid, token2 + "a", "s", "d", new InitProgress().withPtype("none"),
//				"id cannot be null or the empty string");
	}
	
	private void testUpdateJob(String jobid, String token, String status,
			Integer prog, String exception) throws Exception {
		try {
			CLIENT1.updateJobProgress(jobid, token, status, prog);
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
	}
	
	private void testUpdateJob(String jobid, String token, String status,
			String exception) throws Exception {
		try {
			CLIENT1.updateJob(jobid, token, status);
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
		try {
			CLIENT1.updateJobProgress(jobid, token, status, 1);
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
	}
	
	@Test
	public void completeJob() throws Exception {
		String jobid = CLIENT1.createAndStartJob(token2, "c stat", "c desc",
				new InitProgress().withPtype("none"));
		CLIENT1.completeJob(jobid, token2, "c stat2", 0, null);
		checkJob(jobid, "complete", "c stat2", USER2, "c desc", "none", null,
				null, 1, 0, null);
		
		jobid = CLIENT1.createAndStartJob(token2, "c2 stat", "c2 desc",
				new InitProgress().withPtype("percent"));
		CLIENT1.updateJobProgress(jobid, token2, "c2 stat2", 40);
		CLIENT1.completeJob(jobid, token2, "c2 stat3", 1, new Results());
		checkJob(jobid, "error", "c2 stat3", USER2, "c2 desc", "percent",
				100, 100, 1, 1, new Results());
		
		jobid = CLIENT1.createAndStartJob(token2, "c3 stat", "c3 desc",
				new InitProgress().withPtype("task").withMax(37));
		CLIENT1.updateJobProgress(jobid, token2, "c3 stat2", 15);
		Results res = new Results()
						.withShocknodes(Arrays.asList("node1", "node3"))
						.withShockurl("surl")
						.withWorkspaceids(Arrays.asList("ws1", "ws3"))
						.withWorkspaceurl("wurl");
		CLIENT1.completeJob(jobid, token2, "c3 stat3", 0, res);
		checkJob(jobid, "complete", "c3 stat3", USER2, "c3 desc", "task",
				37, 37, 1, 0, res);
		
		testCompleteJob(null, token2, "s", 0, null,
				"id cannot be null or the empty string");
		testCompleteJob("", token2, "s", 0, null,
				"id cannot be null or the empty string");
		testCompleteJob("aaaaaaaaaaaaaaaaaaaa", token2, "s", 0, null,
				"Job ID aaaaaaaaaaaaaaaaaaaa is not a legal ID");
		
		testCompleteJob(jobid, null, "s", 0, null,
				"Service token cannot be null or the empty string");
		testCompleteJob(jobid, "foo", "s", 0, null,
				"Auth token is in the incorrect format, near 'foo'");
		testCompleteJob(jobid, token1, "s", 0, null, String.format(
				"There is no uncompleted job %s for user kbasetest started by service kbasetest",
				jobid, USER1, USER1));
	}
	
	private void testCompleteJob(String jobid, String token, String status,
			Integer error, Results res, String exception) throws Exception {
		try {
			CLIENT1.completeJob(jobid, token, status, error, res);
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
	}
	
	//TODO delete job tests
}
