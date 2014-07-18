package us.kbase.userandjobstate.test.awe.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.auth.AuthException;
import us.kbase.auth.AuthService;
import us.kbase.auth.AuthUser;
import us.kbase.common.test.TestException;
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
	
	private final static boolean deleteTempFilesOnExit = true;

	private static BasicAweClient bac1;
	private static BasicAweClient bac2;
	
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
				deleteTempFilesOnExit);
		System.out.println("Awe temp dir is " + aweC.getTempDir());
		String u1 = System.getProperty("test.user1");
		String u2 = System.getProperty("test.user2");
		String p1 = System.getProperty("test.pwd1");
		String p2 = System.getProperty("test.pwd2");

		System.out.println("Logging in users");
		AuthUser user1;
		try {
			user1 = AuthService.login(u1, p1);
		} catch (AuthException ae) {
			throw new TestException("Unable to login with test.user1: " + u1 +
					"\nPlease check the credentials in the test configuration.", ae);
		}
		System.out.println("Logged in user1");
		AuthUser otherguy;
		try {
			otherguy = AuthService.login(u2, p2);
		} catch (AuthException ae) {
			throw new TestException("Unable to login with test.user2: " + u2 +
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
			bac1 = new BasicAweClient(aweurl, user1.getToken());
			bac2 = new BasicAweClient(aweurl, otherguy.getToken());
		} catch (IOException ioe) {
			throw new TestException("Couldn't set up shock client: " +
					ioe.getLocalizedMessage());
		}
		System.out.println("Set up awe clients");
	}
	
	@AfterClass
	public static void tearDownClass() throws IOException {
		if (aweC != null) {
			aweC.destroy();
		}
	}
	
	@Test
	public void getJob() throws Exception {
		@SuppressWarnings("unused")
		int breakpoint = 0;
		TestAweJob j = aweC.createJob("myserv", "some desc");
		j.addTask();
		String jobid = aweC.submitJob(j, bac1.getToken());
		System.out.println("Waiting 10s for job to enqueue");
		Thread.sleep(10000); //wait for job to enqueue
		checkJob(jobid, "myserv", "some desc", 0, 1, "completed", "");
		
		failGetJob(bac1, "fdcafcec-f66c-4d37-be5c-8bfbf7cd268d",
				new AweNoJobException(400, "job not found:fdcafcec-f66c-4d37-be5c-8bfbf7cd268d"));
		failGetJob(bac2, jobid, new AweAuthorizationException(401, "User Unauthorized"));
		
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
				System.out.println(exp);
				assertThat("correct http code", ((AweHttpException)exp).getHttpCode(),
						is(((AweHttpException)e).getHttpCode()));
			}
		}
	}
	
	private void checkJob(String jobid, String service, String description,
			int remaining, int total, String state, String notes)
			throws Exception {
		AweJob aj = bac1.getJob(new AweJobId(jobid));
		assertThat("service correct", aj.getInfo().getService(), is(service));
		assertThat("desc correct", aj.getInfo().getDescription(), is(description));
		assertThat("remaining correct", aj.getRemaintasks(), is(remaining));
		assertThat("total correct", aj.getTasks().size(), is(total));
		assertThat("state correct", aj.getState(), is(state));
		assertThat("notes correct", aj.getNotes(), is(notes));
	}
	
	@Test
	public void listJobs() throws Exception {
		List<AweJob> lj = bac1.getJobs(null, true, true, true, true, true, true);
		System.out.println("list:" + lj);
	}

}
