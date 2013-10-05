package us.kbase.userandjobstate.kbase.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
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
import us.kbase.common.service.UObject;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;
import us.kbase.userandjobstate.UserAndJobStateServer;
import us.kbase.userandjobstate.jobstate.Job;
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
		ws.add("backend-secret", "");
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
		/* TODO restore when the auth service is fixed
		try {
			CLIENT1.setStateAuth(token2 + "a", "key", new UObject("foo"));
			fail("set state w/ bad token");
		} catch (ServerException se) {
			assertThat("correct exception", se.getLocalizedMessage(),
					is("Service token is invalid"));
		}*/
	}
	
	@Test
	public void createJob() throws Exception {
		String jobid = CLIENT1.createJob();
		Tuple12<String, String, String, String, String, Integer, Integer,
				String, Integer, Integer, String, Results> ret =
				CLIENT1.getJobInfo(jobid);
		checkJob(ret, jobid, "created", null, null, null, null, null,
				null, null, null, null);
	}
	
	private void checkJob(Tuple12<String, String, String, String, String,
			Integer, Integer, String, Integer, Integer, String, Results> ret,
			String id, String stage, String status,
			String service, String desc, String progtype, Integer prog,
			Integer maxproj, Integer complete, Integer error,
			Results results)
			throws Exception {
		assertThat("job id ok", ret.getE1(), is(id));
		assertThat("job stage ok", ret.getE3(), is(stage));
		assertThat("job service ok", ret.getE2(), is(service));
		assertThat("job desc ok", ret.getE11(), is(desc));
		assertThat("job progtype ok", ret.getE8(), is(progtype));
		assertThat("job prog ok", ret.getE6(), is(prog));
		assertThat("job maxprog ok", ret.getE7(), is(maxproj));
		assertThat("job status ok", ret.getE4(), is(status));
		assertThat("job updated ok", ret.getE5(), is(String.class)); //TODO parse date
		assertThat("job complete ok", ret.getE9(), is(complete));
		assertThat("job error ok", ret.getE10(), is(error));
		assertThat("job results ok", ret.getE12(), is(results));
		checkResults(ret.getE12(), results);
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
	
	//TODO ServerError toString doesn't seem to work check this
}
