package us.kbase.userandjobstate.test.awe.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.auth.AuthException;
import us.kbase.auth.AuthService;
import us.kbase.auth.AuthUser;
import us.kbase.common.test.TestException;
import us.kbase.common.test.controllers.mongo.MongoController;
import us.kbase.common.test.controllers.shock.ShockController;
import us.kbase.userandjobstate.awe.client.AweJob;
import us.kbase.userandjobstate.awe.client.AweJobId;
import us.kbase.userandjobstate.awe.client.BasicAweClient;
import us.kbase.userandjobstate.awe.client.exceptions.AweAuthorizationException;
import us.kbase.userandjobstate.awe.client.exceptions.AweHttpException;
import us.kbase.userandjobstate.awe.client.exceptions.AweNoJobException;
import us.kbase.userandjobstate.test.UserJobStateTestCommon;
import us.kbase.userandjobstate.test.awe.controller.AweController;
import us.kbase.userandjobstate.test.awe.controller.AweController.TestAweJob;

public class AweClientTests {
	
	//TODO expand these tests for more coverage of API, only covers minimal use for now
	
	private static BasicAweClient BAC1;
	private static BasicAweClient BAC2;
	private static String USER1;
	private static String USER2;
	
	private static MongoController mongo;
	private static ShockController shock;
	private static AweController aweC;

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
		
		aweC = new AweController(
				new URL("http://localhost:" + shock.getServerPort()),
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
		String p1 = System.getProperty("test.pwd1");
		String p2 = System.getProperty("test.pwd2");

		System.out.println("Logging in users");
		AuthUser user1;
		try {
			user1 = AuthService.login(USER1, p1);
		} catch (AuthException ae) {
			throw new TestException("Unable to login with test.user1: " + USER1 +
					"\nPlease check the credentials in the test configuration.", ae);
		}
		System.out.println("Logged in user1");
		AuthUser otherguy;
		try {
			otherguy = AuthService.login(USER2, p2);
		} catch (AuthException ae) {
			throw new TestException("Unable to login with test.user2: " + USER2 +
					"\nPlease check the credentials in the test configuration.", ae);
		}
		System.out.println("Logged in user2");
		if (user1.getUserId().equals(otherguy.getUserId())) {
			throw new TestException("The user IDs of test.user1 and " + 
					"test.user2 are the same. Please provide different test users.");
		}
		URL aweurl = new URL("http://localhost:" + aweC.getServerPort());
		
		System.out.println("Testing awe clients pointed at: " + aweurl);
		try {
			BAC1 = new BasicAweClient(aweurl, user1.getToken());
			BAC2 = new BasicAweClient(aweurl, otherguy.getToken());
		} catch (IOException ioe) {
			throw new TestException("Couldn't set up shock client: " +
					ioe.getLocalizedMessage());
		}
		System.out.println("Set up awe clients");
	}
	
	@AfterClass
	public static void tearDownClass() throws IOException {
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
		String jobid = aweC.submitJob(j, BAC1.getToken());
		System.out.println("Waiting 20s for job to enqueue");
		Thread.sleep(20000); //wait for job to enqueue
		checkJob(jobid, "myserv", "some desc", 0, 1, "completed", "");
		
		failGetJob(BAC1, "fdcafcec-f66c-4d37-be5c-8bfbf7cd268d",
				new AweNoJobException(404, "Not Found"));
		failGetJob(BAC2, jobid, new AweAuthorizationException(401, "User Unauthorized"));
		
	}
	
	@Test
	public void shareJob() throws Exception {
		TestAweJob j = aweC.createJob("shareserv", "share desc");
		j.addTask();
		String jobid = aweC.submitJob(j, BAC1.getToken());
		System.out.println("Waiting 10s for job to enqueue");
		Thread.sleep(10000); //wait for job to enqueue
		BAC1.getJob(new AweJobId(jobid)); //should work
		failGetJob(BAC2, jobid, new AweAuthorizationException(401, "User Unauthorized"));
		BAC1.setJobReadable(new AweJobId(jobid), Arrays.asList(USER2));
		BAC2.getJob(new AweJobId(jobid)); //should work
		BAC1.removeJobReadable(new AweJobId(jobid), Arrays.asList(USER2));
		failGetJob(BAC2, jobid, new AweAuthorizationException(401, "User Unauthorized"));
	}
	
	private void failGetJob(BasicAweClient cli, String jobid, Exception e)
			throws Exception {
		try {
			cli.getJob(new AweJobId(jobid));
			fail("got job sucessfully but expected fail");
		} catch (Exception exp) {
			assertThat("correct exception", exp.getLocalizedMessage(),
					is(e.getLocalizedMessage()));
			assertThat("correct exception type", exp, is(e.getClass()));
			if (exp instanceof AweHttpException) {
				assertThat("correct http code", ((AweHttpException)exp).getHttpCode(),
						is(((AweHttpException)e).getHttpCode()));
			}
		}
	}
	
	private void checkJob(String jobid, String service, String description,
			int remaining, int total, String state, String notes)
			throws Exception {
		AweJob aj = BAC1.getJob(new AweJobId(jobid));
		assertThat("service correct", aj.getInfo().getService(), is(service));
		assertThat("desc correct", aj.getInfo().getDescription(), is(description));
		assertThat("remaining correct", aj.getRemaintasks(), is(remaining));
		assertThat("total correct", aj.getTasks().size(), is(total));
		assertThat("state correct", aj.getState(), is(state));
		assertThat("notes correct", aj.getNotes(), is(notes));
	}
	
//	@Test
//	public void listJobs() throws Exception {
//		List<AweJob> lj = bac1.getJobs(null, true, true, true, true, true, true);
//		System.out.println("list:" + lj);
//	}

}
