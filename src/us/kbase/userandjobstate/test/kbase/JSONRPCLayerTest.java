package us.kbase.userandjobstate.test.kbase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.auth.AuthException;
import us.kbase.auth.AuthService;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple14;
import us.kbase.common.service.Tuple5;
import us.kbase.common.service.Tuple7;
import us.kbase.common.service.UObject;
import us.kbase.common.test.TestException;
import us.kbase.userandjobstate.InitProgress;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;
import us.kbase.userandjobstate.UserAndJobStateServer;
import us.kbase.userandjobstate.test.FakeJob;
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
	private static String TOKEN1;
	private static String TOKEN2;

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
		try {
			TOKEN1 = AuthService.login(USER1, p1).getTokenString();
		} catch (AuthException ae) {
			throw new TestException("Unable to login with test.user1: " + USER1 +
					"\nPlease check the credentials in the test configuration.", ae);
		}
		try {
			TOKEN2 = AuthService.login(USER2, p2).getTokenString();
		} catch (AuthException ae) {
			throw new TestException("Unable to login with test.user2: " + USER2 +
					"\nPlease check the credentials in the test configuration.", ae);
		}
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
		CLIENT1.setStateAuth(TOKEN2, "akey1", new UObject(data));
		CLIENT2.setStateAuth(TOKEN1, "2akey1", new UObject(data2));
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
		CLIENT1.setStateAuth(TOKEN2, "akey2", new UObject(data));
		CLIENT1.setStateAuth(TOKEN1, "akey", new UObject(data));
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
		CLIENT1.removeStateAuth(TOKEN2, "akey1");
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
	
	private String[] getNearbyTimes() {
		SimpleDateFormat dateform = getDateFormat();
		SimpleDateFormat dateformutc =
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		dateformutc.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date nf = new Date(new Date().getTime() + 10000);
		Date np =  new Date(new Date().getTime() - 10);
		String[] nearfuture = new String[3];
		nearfuture[0] = dateform.format(nf);
		nearfuture[1] = dateformutc.format(nf);
		nearfuture[2] = dateform.format(np);
		return nearfuture;
	}
	
	@Test
	public void createAndStartJob() throws Exception {
		String[] nearfuture = getNearbyTimes();
		
		String jobid = CLIENT1.createJob();
		checkJob(jobid, "created",null, null, null, null,
				null, null, null, null, null, null);
		CLIENT1.startJob(jobid, TOKEN2, "new stat", "ne desc",
				new InitProgress().withPtype("none"), null);
		checkJob(jobid, "started", "new stat", USER2,
				"ne desc", "none", null, null, null, 0, 0, null);
		
		jobid = CLIENT1.createJob();
		CLIENT1.startJob(jobid, TOKEN2, "new stat2", "ne desc2",
				new InitProgress().withPtype("percent"), nearfuture[0]);
		checkJob(jobid, "started", "new stat2", USER2,
				"ne desc2", "percent", 0, 100, nearfuture[1], 0, 0, null);
		
		jobid = CLIENT1.createJob();
		CLIENT1.startJob(jobid, TOKEN2, "new stat3", "ne desc3",
				new InitProgress().withPtype("task").withMax(5), null);
		checkJob(jobid, "started", "new stat3", USER2,
				"ne desc3", "task", 0, 5, null, 0, 0, null);
		
		startJobBadArgs(null, TOKEN2, "s", "d", new InitProgress().withPtype("none"),
				null, "id cannot be null or the empty string", true);
		startJobBadArgs("", TOKEN2, "s", "d", new InitProgress().withPtype("none"),
				null, "id cannot be null or the empty string", true);
		startJobBadArgs("aaaaaaaaaaaaaaaaaaaa", TOKEN2, "s", "d",
				new InitProgress().withPtype("none"),
				null, "Job ID aaaaaaaaaaaaaaaaaaaa is not a legal ID", true);
		
		jobid = CLIENT1.createJob();
		startJobBadArgs(jobid, null, "s", "d", new InitProgress().withPtype("none"),
				null, "Service token cannot be null or the empty string");
		startJobBadArgs(jobid, "foo", "s", "d", new InitProgress().withPtype("none"),
				null, "Auth token is in the incorrect format, near 'foo'");
		//TODO restore when auth service is fixed
//		testStartJob(jobid, token2 + "a", "s", "d", new InitProgress().withPtype("none"),
//				"id cannot be null or the empty string");
		
		startJobBadArgs(jobid, TOKEN2, "s", "d",
				new InitProgress().withPtype("none"),
				nearfuture[2], "The estimated completion date must be in the future", true);
		startJobBadArgs(jobid, TOKEN2, "s", "d",
				new InitProgress().withPtype("none"),
				"2200-12-30T23:30:54-8000", "Unparseable date: \"2200-12-30T23:30:54-8000\"", true);
		startJobBadArgs(jobid, TOKEN2, "s", "d",
				new InitProgress().withPtype("none"),
				"2200-12-30T123:30:54-0800", "Unparseable date: \"2200-12-30T123:30:54-0800\"", true);
		
		startJobBadArgs(jobid, TOKEN2, "s", "d", null,
				null, "InitProgress cannot be null");
		InitProgress ip = new InitProgress().withPtype("none");
		ip.setAdditionalProperties("foo", "bar");
		startJobBadArgs(jobid, TOKEN2, "s", "d", ip,
				null, "Unexpected arguments in InitProgress: foo");
		startJobBadArgs(jobid, TOKEN2, "s", "d", new InitProgress().withPtype(null),
				null, "Progress type cannot be null");
		startJobBadArgs(jobid, TOKEN2, "s", "d", new InitProgress().withPtype("foo"),
				null, "No such progress type: foo");
		startJobBadArgs(jobid, TOKEN2, "s", "d", new InitProgress().withPtype("task")
				.withMax(null),
				null, "Max progress cannot be null for task based progress");
		
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "cs stat", "cs desc",
				new InitProgress().withPtype("none"), nearfuture[0]);
		checkJob(jobid, "started", "cs stat", USER2,
				"cs desc", "none", null, null, nearfuture[1], 0, 0, null);
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "cs stat2", "cs desc2",
				new InitProgress().withPtype("percent"), null);
		checkJob( jobid, "started", "cs stat2", USER2,
				"cs desc2", "percent", 0, 100, null, 0, 0, null);
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "cs stat3", "cs desc3",
				new InitProgress().withPtype("task").withMax(5), null);
		checkJob(jobid, "started", "cs stat3", USER2,
				"cs desc3", "task", 0, 5, null, 0, 0, null);
	}
	
	private void startJobBadArgs(String jobid, String token, String stat, String desc,
			InitProgress prog, String estCompl, String exception) throws Exception {
		startJobBadArgs(jobid, token, stat, desc, prog, estCompl, exception, false);
	}
	
	private void startJobBadArgs(String jobid, String token, String stat, String desc,
			InitProgress prog, String estCompl, String exception, boolean badid)
			throws Exception {
		try {
			CLIENT1.startJob(jobid, token, stat, desc, prog, estCompl);
			fail("started job w/ bad args");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
		if (badid) {
			return;
		}
		try {
			CLIENT1.createAndStartJob(token, stat, desc, prog, estCompl);
			fail("started job w/ bad args");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
	}
	
	private SimpleDateFormat getDateFormat() {
		SimpleDateFormat dateform =
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		dateform.setLenient(false);
		return dateform;
	}
	
	private void checkJob(String id, String stage, String status,
			String service, String desc, String progtype, Integer prog,
			Integer maxprog, String estCompl, Integer complete, Integer error,
			Results results)
			throws Exception {
		SimpleDateFormat dateform = getDateFormat();
		Tuple14<String, String, String, String, String, String,
				Integer, Integer, String, String, Integer, Integer, String,
				Results> ret = CLIENT1.getJobInfo(id);
		assertThat("job id ok", ret.getE1(), is(id));
		assertThat("job stage ok", ret.getE3(), is(stage));
		if (ret.getE4() != null) {
			dateform.parse(ret.getE4()); //should throw error if bad format
		}
		assertThat("job est compl ok", ret.getE10(), is(estCompl));
		assertThat("job service ok", ret.getE2(), is(service));
		assertThat("job desc ok", ret.getE13(), is(desc));
		assertThat("job progtype ok", ret.getE9(), is(progtype));
		assertThat("job prog ok", ret.getE7(), is(prog));
		assertThat("job maxprog ok", ret.getE8(), is(maxprog));
		assertThat("job status ok", ret.getE5(), is(status));
		dateform.parse(ret.getE6()); //should throw error if bad format
		assertThat("job complete ok", ret.getE11(), is(complete));
		assertThat("job error ok", ret.getE12(), is(error));
		checkResults(ret.getE14(), results);
		
		Tuple5<String, String, Integer, String, String> jobdesc =
				CLIENT1.getJobDescription(id);
		assertThat("job service ok", jobdesc.getE1(), is(service));
		assertThat("job progtype ok", jobdesc.getE2(), is(progtype));
		assertThat("job maxprog ok", jobdesc.getE3(), is(maxprog));
		assertThat("job desc ok", jobdesc.getE4(), is(desc));
		if (jobdesc.getE5() != null) {
			dateform.parse(jobdesc.getE5()); //should throw error if bad format
		}
		
		Tuple7<String, String, String, Integer, String, Integer, Integer> 
				jobstat = CLIENT1.getJobStatus(id);
		dateform.parse(jobstat.getE1()); //should throw error if bad format
		assertThat("job stage ok", jobstat.getE2(), is(stage));
		assertThat("job status ok", jobstat.getE3(), is(status));
		assertThat("job progress ok", jobstat.getE4(), is(prog));
		assertThat("job est compl ok", jobstat.getE5(), is(estCompl));
		assertThat("job complete ok", jobstat.getE6(), is(complete));
		assertThat("job error ok", jobstat.getE7(), is(error));
		
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
		String[] nearfuture = getNearbyTimes();
		String jobid = CLIENT1.createAndStartJob(TOKEN2, "up stat", "up desc",
				new InitProgress().withPtype("none"), null);
		CLIENT1.updateJob(jobid, TOKEN2, "up stat2", null);
		checkJob(jobid, "started", "up stat2", USER2,
				"up desc", "none", null, null, null, 0, 0, null);
		CLIENT1.updateJobProgress(jobid, TOKEN2, "up stat3", 40, null);
		checkJob(jobid, "started", "up stat3", USER2,
				"up desc", "none", null, null, null, 0, 0, null);
		CLIENT1.updateJobProgress(jobid, TOKEN2, "up stat3", null, nearfuture[0]);
		checkJob(jobid, "started", "up stat3", USER2,
				"up desc", "none", null, null, nearfuture[1], 0, 0, null);
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "up2 stat", "up2 desc",
				new InitProgress().withPtype("percent"), null);
		CLIENT1.updateJobProgress(jobid, TOKEN2, "up2 stat2", 40, null);
		checkJob(jobid, "started", "up2 stat2", USER2,
				"up2 desc", "percent", 40, 100, null, 0, 0, null);
		CLIENT1.updateJob(jobid, TOKEN2, "up2 stat3", nearfuture[0]);
		checkJob(jobid, "started", "up2 stat3", USER2,
				"up2 desc", "percent", 40, 100, nearfuture[1], 0, 0, null);
		CLIENT1.updateJobProgress(jobid, TOKEN2, "up2 stat4", 70, null);
		checkJob(jobid, "started", "up2 stat4", USER2,
				"up2 desc", "percent", 100, 100, nearfuture[1], 0, 0, null);
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "up3 stat", "up3 desc",
				new InitProgress().withPtype("task").withMax(42), null);
		CLIENT1.updateJobProgress(jobid, TOKEN2, "up3 stat2", 30, nearfuture[0]);
		checkJob(jobid, "started", "up3 stat2", USER2,
				"up3 desc", "task", 30, 42, nearfuture[1], 0, 0, null);
		CLIENT1.updateJob(jobid, TOKEN2, "up3 stat3", null);
		checkJob( jobid, "started", "up3 stat3", USER2,
				"up3 desc", "task", 30, 42, nearfuture[1], 0, 0, null);
		CLIENT1.updateJobProgress(jobid, TOKEN2, "up3 stat4", 15, null);
		checkJob(jobid, "started", "up3 stat4", USER2,
				"up3 desc", "task", 42, 42, nearfuture[1], 0, 0, null);
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "up4 stat", "up4 desc",
				new InitProgress().withPtype("none"), null);
		updateJobBadArgs(jobid, TOKEN2, "up4 stat2", -1, null, "progress cannot be negative");
		
		updateJobBadArgs(null, TOKEN2, "s", null,
				"id cannot be null or the empty string");
		updateJobBadArgs("", TOKEN2, "s", null,
				"id cannot be null or the empty string");
		updateJobBadArgs("aaaaaaaaaaaaaaaaaaaa", TOKEN2, "s", null,
				"Job ID aaaaaaaaaaaaaaaaaaaa is not a legal ID");
		
		updateJobBadArgs(jobid, TOKEN2, "s", nearfuture[2],
				"The estimated completion date must be in the future");
		updateJobBadArgs(jobid, TOKEN2, "s", "2200-12-30T23:30:54-8000",
				"Unparseable date: \"2200-12-30T23:30:54-8000\"");
		updateJobBadArgs(jobid, TOKEN2, "s", "2200-12-30T123:30:54-0800",
				"Unparseable date: \"2200-12-30T123:30:54-0800\"");
		
		updateJobBadArgs(jobid, null, "s", null,
				"Service token cannot be null or the empty string");
		updateJobBadArgs(jobid, "foo", "s", null,
				"Auth token is in the incorrect format, near 'foo'");
		updateJobBadArgs(jobid, TOKEN1, "s", null, String.format(
				"There is no uncompleted job %s for user kbasetest started by service kbasetest",
				jobid, USER1, USER1));
		
		
		//TODO restore when auth service is fixed
//		testStartJob(jobid, token2 + "a", "s", "d", new InitProgress().withPtype("none"),
//				"id cannot be null or the empty string");
	}
	
	private void updateJobBadArgs(String jobid, String token, String status,
			Integer prog, String estCompl, String exception) throws Exception {
		try {
			CLIENT1.updateJobProgress(jobid, token, status, prog, estCompl);
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
	}
	
	private void updateJobBadArgs(String jobid, String token, String status,
			String estCompl, String exception) throws Exception {
		try {
			CLIENT1.updateJob(jobid, token, status, estCompl);
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
		try {
			CLIENT1.updateJobProgress(jobid, token, status, 1, estCompl);
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
	}
	
	@Test
	public void completeJob() throws Exception {
		String[] nearfuture = getNearbyTimes();
		String jobid = CLIENT1.createAndStartJob(TOKEN2, "c stat", "c desc",
				new InitProgress().withPtype("none"), null);
		CLIENT1.completeJob(jobid, TOKEN2, "c stat2", 0, null);
		checkJob(jobid, "complete", "c stat2", USER2, "c desc", "none", null,
				null, null, 1, 0, null);
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "c2 stat", "c2 desc",
				new InitProgress().withPtype("percent"), null);
		CLIENT1.updateJobProgress(jobid, TOKEN2, "c2 stat2", 40, null);
		CLIENT1.completeJob(jobid, TOKEN2, "c2 stat3", 1, new Results());
		checkJob(jobid, "error", "c2 stat3", USER2, "c2 desc", "percent",
				100, 100, null, 1, 1, new Results());
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "c3 stat", "c3 desc",
				new InitProgress().withPtype("task").withMax(37), null);
		CLIENT1.updateJobProgress(jobid, TOKEN2, "c3 stat2", 15, nearfuture[0]);
		Results res = new Results()
						.withShocknodes(Arrays.asList("node1", "node3"))
						.withShockurl("surl")
						.withWorkspaceids(Arrays.asList("ws1", "ws3"))
						.withWorkspaceurl("wurl");
		CLIENT1.completeJob(jobid, TOKEN2, "c3 stat3", 0, res);
		checkJob(jobid, "complete", "c3 stat3", USER2, "c3 desc", "task",
				37, 37, nearfuture[1], 1, 0, res);
		
		testCompleteJob(null, TOKEN2, "s", 0, null,
				"id cannot be null or the empty string");
		testCompleteJob("", TOKEN2, "s", 0, null,
				"id cannot be null or the empty string");
		testCompleteJob("aaaaaaaaaaaaaaaaaaaa", TOKEN2, "s", 0, null,
				"Job ID aaaaaaaaaaaaaaaaaaaa is not a legal ID");
		
		testCompleteJob(jobid, null, "s", 0, null,
				"Service token cannot be null or the empty string");
		testCompleteJob(jobid, "foo", "s", 0, null,
				"Auth token is in the incorrect format, near 'foo'");
		testCompleteJob(jobid, TOKEN1, "s", 0, null, String.format(
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
	
	@Test
	public void deleteJob() throws Exception {
		InitProgress noprog = new InitProgress().withPtype("none");
		String jobid = CLIENT1.createAndStartJob(TOKEN2, "d stat", "d desc", noprog, null);
		CLIENT1.completeJob(jobid, TOKEN2, "d stat2", 0, null);
		CLIENT1.deleteJob(jobid);
		testGetJobBadArgs(jobid, String.format("There is no job %s for user %s",
				jobid, USER1));
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "e stat", "e desc", noprog, null);
		CLIENT1.completeJob(jobid, TOKEN2, "e stat2", 1, null);
		CLIENT1.deleteJob(jobid);
		testGetJobBadArgs(jobid, String.format("There is no job %s for user %s",
				jobid, USER1));
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "d2 stat", "d2 desc", noprog, null);
		CLIENT1.forceDeleteJob(TOKEN2, jobid);
		testGetJobBadArgs(jobid, String.format("There is no job %s for user %s",
				jobid, USER1));
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "d3 stat", "d3 desc", noprog, null);
		CLIENT1.updateJobProgress(jobid, TOKEN2, "d3 stat2", 3, null);
		CLIENT1.forceDeleteJob(TOKEN2, jobid);
		testGetJobBadArgs(jobid, String.format("There is no job %s for user %s",
				jobid, USER1));
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "d4 stat", "d4 desc", noprog, null);
		CLIENT1.completeJob(jobid, TOKEN2, "d4 stat2", 0, null);
		CLIENT1.forceDeleteJob(TOKEN2, jobid);
		testGetJobBadArgs(jobid, String.format("There is no job %s for user %s",
				jobid, USER1));
		
		jobid = CLIENT1.createAndStartJob(TOKEN2, "d5 stat", "d5 desc", noprog, null);
		CLIENT1.completeJob(jobid, TOKEN2, "d5 stat2", 1, null);
		CLIENT1.forceDeleteJob(TOKEN2, jobid);
		testGetJobBadArgs(jobid, String.format("There is no job %s for user %s",
				jobid, USER1));
		
		jobid = CLIENT1.createJob();
		failToDeleteJob(jobid, String.format(
				"There is no completed job %s for user %s", jobid, USER1));
		failToDeleteJob(jobid, TOKEN2, String.format(
				"There is no job %s for user %s and service %s",
				jobid, USER1, USER2));
		CLIENT1.startJob(jobid, TOKEN2, "d6 stat", "d6 desc", noprog, null);
		failToDeleteJob(jobid, String.format(
				"There is no completed job %s for user %s", jobid, USER1));
		CLIENT1.updateJobProgress(jobid, TOKEN2, "d6 stat2", 3, null);
		failToDeleteJob(jobid, String.format(
				"There is no completed job %s for user %s", jobid, USER1));
		
		failToDeleteJob(null, "id cannot be null or the empty string");
		failToDeleteJob("", "id cannot be null or the empty string");
		failToDeleteJob("aaaaaaaaaaaaaaaaaaaa",
				"Job ID aaaaaaaaaaaaaaaaaaaa is not a legal ID");
		
		failToDeleteJob(null, TOKEN2,
				"id cannot be null or the empty string");
		failToDeleteJob("", TOKEN2,
				"id cannot be null or the empty string");
		failToDeleteJob("aaaaaaaaaaaaaaaaaaaa", TOKEN2,
				"Job ID aaaaaaaaaaaaaaaaaaaa is not a legal ID");
		
		failToDeleteJob(jobid, null,
				"Service token cannot be null or the empty string", true);
		failToDeleteJob(jobid, "foo",
				"Auth token is in the incorrect format, near 'foo'");
		failToDeleteJob(jobid, TOKEN1, String.format(
				"There is no job %s for user kbasetest and service kbasetest",
				jobid, USER1, USER1));
	}
	
	private void failToDeleteJob(String jobid, String exception)
			throws Exception {
		failToDeleteJob(jobid, null, exception, false);
	}
	
	private void failToDeleteJob(String jobid, String token, String exception)
			throws Exception {
		failToDeleteJob(jobid, token, exception, false);
	}
	
	private void failToDeleteJob(String jobid, String token, String exception,
			boolean usenulltoken)
			throws Exception {
		try {
			if (!usenulltoken && token == null) {
				CLIENT1.deleteJob(jobid);
			} else {
				CLIENT1.forceDeleteJob(token, jobid);
			}
			fail("deleted job with bad args");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
	}
	
	@Test
	public void listServices() throws Exception {
		checkListServices(CLIENT2, new HashSet<String>());
		InitProgress noprog = new InitProgress().withPtype("none");
		CLIENT1.createAndStartJob(TOKEN2, "ls stat", "ls desc", noprog, null);
		checkListServices(CLIENT1, new HashSet<String>(Arrays.asList(USER2)));
		String jobid = CLIENT1.createAndStartJob(TOKEN1, "ls2 stat",
				"ls2 desc", noprog, null);
		checkListServices(CLIENT1, new HashSet<String>(Arrays.asList(USER1, USER2)));
		CLIENT1.forceDeleteJob(TOKEN1, jobid);
	}
	
	private void checkListServices(UserAndJobStateClient client,
			Set<String> service) throws Exception {
		assertThat("got correct services", new HashSet<String>(client.listJobServices()),
				is(service));
	}
	
	@Test
	public void listJobs() throws Exception {
		//NOTE: all jobs must be deleted from CLIENT2 or other tests may fail
		InitProgress noprog = new InitProgress().withPtype("none");
		Set<FakeJob> empty = new HashSet<FakeJob>();
		
		checkListJobs(USER1, null, empty);
		checkListJobs(USER1, "", empty);
		checkListJobs(USER1, "R", empty);
		checkListJobs(USER1, "C", empty);
		checkListJobs(USER1, "E", empty);
		checkListJobs(USER1, "RC", empty);
		checkListJobs(USER1, "CE", empty);
		checkListJobs(USER1, "RE", empty);
		checkListJobs(USER1, "RCE", empty);
		checkListJobs(USER1, "RCEX", empty);
		
		String jobid = CLIENT2.createJob();
		checkListJobs(USER1, null, empty);
		checkListJobs(USER1, "", empty);
		checkListJobs(USER1, "R", empty);
		checkListJobs(USER1, "C", empty);
		checkListJobs(USER1, "E", empty);
		checkListJobs(USER1, "RC", empty);
		checkListJobs(USER1, "CE", empty);
		checkListJobs(USER1, "RE", empty);
		checkListJobs(USER1, "RCE", empty);
		checkListJobs(USER1, "RXCE", empty);
		
		CLIENT2.startJob(jobid, TOKEN1, "lj stat", "lj desc", noprog, null);
		FakeJob started = new FakeJob(jobid, null, USER1, "started", null, "lj desc",
				"none", null, null, "lj stat", false, false, null);
		Set<FakeJob> setstarted = new HashSet<FakeJob>(Arrays.asList(started));
		checkListJobs(USER1, null, setstarted);
		checkListJobs(USER1, "", setstarted);
		checkListJobs(USER1, "R", setstarted);
		checkListJobs(USER1, "C", empty);
		checkListJobs(USER1, "E", empty);
		checkListJobs(USER1, "RC", setstarted);
		checkListJobs(USER1, "CE", empty);
		checkListJobs(USER1, "RE", setstarted);
		checkListJobs(USER1, "RCE", setstarted);
		checkListJobs(USER1, "!RCE", setstarted);
		
		jobid = CLIENT2.createAndStartJob(TOKEN1, "lj2 stat", "lj2 desc",
				new InitProgress().withPtype("percent"), null);
		CLIENT2.updateJobProgress(jobid, TOKEN1, "lj2 stat2", 42, null);
		FakeJob started2 = new FakeJob(jobid, null, USER1, "started", null,
				"lj2 desc", "percent", 42, 100, "lj2 stat2", false, false, null);
		setstarted.add(started2);
		checkListJobs(USER1, null, setstarted);
		checkListJobs(USER1, "", setstarted);
		checkListJobs(USER1, "R", setstarted);
		checkListJobs(USER1, "C", empty);
		checkListJobs(USER1, "E", empty);
		checkListJobs(USER1, "RC", setstarted);
		checkListJobs(USER1, "CE", empty);
		checkListJobs(USER1, "RE", setstarted);
		checkListJobs(USER1, "RCE", setstarted);
		checkListJobs(USER1, "RCwE", setstarted);
		
		CLIENT2.completeJob(jobid, TOKEN1, "lj2 stat3", 0, 
				new Results().withShocknodes(Arrays.asList("node1", "node2")));
		setstarted.remove(started2);
		started2 = null;
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("shocknodes", Arrays.asList("node1", "node2"));
		res.put("shockurl", null);
		res.put("workspaceids", new ArrayList<String>());
		res.put("workspaceurl", null);
		FakeJob complete = new FakeJob(jobid, null, USER1, "complete", null,
				"lj2 desc", "percent", 100, 100, "lj2 stat3", true, false, res);
		Set<FakeJob> setcomplete = new HashSet<FakeJob>(Arrays.asList(complete));
		Set<FakeJob> setstartcomp = new HashSet<FakeJob>();
		setstartcomp.addAll(setstarted);
		setstartcomp.addAll(setcomplete);
		checkListJobs(USER1, null, setstartcomp);
		checkListJobs(USER1, "", setstartcomp);
		checkListJobs(USER1, "R", setstarted);
		checkListJobs(USER1, "C", setcomplete);
		checkListJobs(USER1, "C0", setcomplete);
		checkListJobs(USER1, "E", empty);
		checkListJobs(USER1, "RC", setstartcomp);
		checkListJobs(USER1, "CE", setcomplete);
		checkListJobs(USER1, "RE", setstarted);
		checkListJobs(USER1, "RCE", setstartcomp);
		
		jobid = CLIENT2.createAndStartJob(TOKEN1, "lj3 stat", "lj3 desc",
				new InitProgress().withPtype("task").withMax(55), null);
		CLIENT2.updateJobProgress(jobid, TOKEN1, "lj3 stat2", 40, null);
		started2 = new FakeJob(jobid, null, USER1, "started", null,
				"lj3 desc", "task", 40, 55, "lj3 stat2", false, false, null);
		setstarted.add(started2);
		setstartcomp.add(started2);
		checkListJobs(USER1, null, setstartcomp);
		checkListJobs(USER1, "", setstartcomp);
		checkListJobs(USER1, "R", setstarted);
		checkListJobs(USER1, "C", setcomplete);
		checkListJobs(USER1, "E", empty);
		checkListJobs(USER1, "RC", setstartcomp);
		checkListJobs(USER1, "CE", setcomplete);
		checkListJobs(USER1, "C#E", setcomplete);
		checkListJobs(USER1, "RE", setstarted);
		checkListJobs(USER1, "RCE", setstartcomp);
		
		CLIENT2.completeJob(jobid, TOKEN1, "lj3 stat3", 1, 
				new Results().withWorkspaceids(Arrays.asList("wss1", "wss2")));
		setstarted.remove(started2);
		setstartcomp.remove(started2);
		started2 = null;
		Map<String, Object> res2 = new HashMap<String, Object>();
		res2.put("shocknodes", new ArrayList<String>());
		res2.put("shockurl", null);
		res2.put("workspaceids", Arrays.asList("wss1", "wss2"));
		res2.put("workspaceurl", null);
		FakeJob error = new FakeJob(jobid, null, USER1, "error", null,
				"lj3 desc", "task", 55, 55, "lj3 stat3", true, true, res2);
		Set<FakeJob> seterr = new HashSet<FakeJob>(Arrays.asList(error));
		Set<FakeJob> all = new HashSet<FakeJob>(
				Arrays.asList(started, complete, error));
		checkListJobs(USER1, null, all);
		checkListJobs(USER1, "", all);
		checkListJobs(USER1, "x", all);
		checkListJobs(USER1, "R", setstarted);
		checkListJobs(USER1, "C", setcomplete);
		checkListJobs(USER1, "E", seterr);
		checkListJobs(USER1, "RC", setstartcomp);
		checkListJobs(USER1, "CE", new HashSet<FakeJob>(
				Arrays.asList(complete, error)));
		checkListJobs(USER1, "RE", new HashSet<FakeJob>(
				Arrays.asList(started, error)));
		checkListJobs(USER1, "RCE", all);
		
		CLIENT2.forceDeleteJob(TOKEN1, started.getID());
		all.remove(started);
		checkListJobs(USER1, null, all);
		checkListJobs(USER1, "", all);
		checkListJobs(USER1, "goodness this is odd input", all);
		checkListJobs(USER1, "R", empty);
		checkListJobs(USER1, "C", setcomplete);
		checkListJobs(USER1, "E", seterr);
		checkListJobs(USER1, "cE", seterr);
		checkListJobs(USER1, "RC", setcomplete);
		checkListJobs(USER1, "CE", new HashSet<FakeJob>(
				Arrays.asList(complete, error)));
		checkListJobs(USER1, "RE", seterr);
		checkListJobs(USER1, "RCE", all);
		
		CLIENT2.deleteJob(complete.getID());
		checkListJobs(USER1, null, seterr);
		checkListJobs(USER1, "", seterr);
		checkListJobs(USER1, "R", empty);
		checkListJobs(USER1, "C", empty);
		checkListJobs(USER1, "E", seterr);
		checkListJobs(USER1, "e", seterr);
		checkListJobs(USER1, "RC", empty);
		checkListJobs(USER1, "CE", seterr);
		checkListJobs(USER1, "RE", seterr);
		checkListJobs(USER1, "RCE", seterr);
		
		CLIENT2.deleteJob(error.getID());
		checkListJobs(USER1, null, empty);
		checkListJobs(USER1, "", empty);
		checkListJobs(USER1, "R", empty);
		checkListJobs(USER1, "C", empty);
		checkListJobs(USER1, "E", empty);
		checkListJobs(USER1, "RC", empty);
		checkListJobs(USER1, "CE", empty);
		checkListJobs(USER1, "RE", empty);
		checkListJobs(USER1, "RCE", empty);
		
		testListJobsWithBadArgs(null,
				"service cannot be null or the empty string");
		testListJobsWithBadArgs("",
				"service cannot be null or the empty string");
		testListJobsWithBadArgs("abcdefghijklmnopqrst" + "abcdefghijklmnopqrst"
				+ "abcdefghijklmnopqrst" + "abcdefghijklmnopqrst" +
				"abcdefghijklmnopqrst" + "a",
				"service exceeds the maximum length of 100");
	}
	
	private void checkListJobs(String service, String filter,
			Set<FakeJob> expected) throws Exception {
		Set<FakeJob> got = new HashSet<FakeJob>();
		for (Tuple14<String, String, String, String, String, String, Integer,
				Integer, String, String, Integer, Integer, String, Results> ji: 
					CLIENT2.listJobs(service, filter)) {
			got.add(new FakeJob(ji));
		}
		assertThat("got the correct jobs", got, is(expected));
	}
	
	public void testListJobsWithBadArgs(String service, String exception)
			throws Exception{
		try {
			CLIENT2.listJobs(service, "RCE");
			fail("list jobs worked w/ bad service");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is(exception));
		}
	}
}
