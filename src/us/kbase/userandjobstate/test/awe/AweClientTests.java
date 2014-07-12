package us.kbase.userandjobstate.test.awe;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.auth.AuthException;
import us.kbase.auth.AuthService;
import us.kbase.auth.AuthUser;
import us.kbase.common.test.TestException;
import us.kbase.userandjobstate.awe.client.AweJob;
import us.kbase.userandjobstate.awe.client.AweJobId;
import us.kbase.userandjobstate.awe.client.BasicAweClient;
import us.kbase.userandjobstate.awe.client.exceptions.AweNoJobException;

public class AweClientTests {

	private static BasicAweClient bac1;
//	private static BasicAweClient bac2;
//	private static AuthUser otherguy;

	@BeforeClass
	public static void setUpClass() throws Exception {
		//TODO set up the runner to work with these
		System.out.println("Java: " + System.getProperty("java.runtime.version"));
		URL url = new URL(System.getProperty("test.awe.url"));
		System.out.println("Testing awe clients pointed at: " + url);
		String u1 = System.getProperty("test.user1");
//		String u2 = System.getProperty("test.user2");
		String p1 = System.getProperty("test.pwd1");
//		String p2 = System.getProperty("test.pwd2");

		System.out.println("Logging in users");
		AuthUser user1;
		try {
			user1 = AuthService.login(u1, p1);
		} catch (AuthException ae) {
			throw new TestException("Unable to login with test.user1: " + u1 +
					"\nPlease check the credentials in the test configuration.", ae);
		}
		System.out.println("Logged in user1");
/*		try {
			otherguy = AuthService.login(u2, p2);
		} catch (AuthException ae) {
			throw new TestException("Unable to login with test.user2: " + u2 +
					"\nPlease check the credentials in the test configuration.", ae);
		}
		System.out.println("Logged in user2");
		if (user1.getUserId().equals(otherguy.getUserId())) {
			throw new TestException("The user IDs of test.user1 and " + 
					"test.user2 are the same. Please provide test users with different email addresses.");
		}
*/		try {
			bac1 = new BasicAweClient(url, user1.getToken());
//			bac2 = new BasicAweClient(url, otherguy.getToken());
		} catch (IOException ioe) {
			throw new TestException("Couldn't set up shock client: " +
					ioe.getLocalizedMessage());
		}
		System.out.println("Set up shock clients");
	}
	
	//TODO real tests against a local server, need to load with jobs
	@Test
	public void printJob() throws Exception {
		String jobid = "fdcafcec-f66c-4d37-be5c-8bfbf7cd268f";
		AweJob aj = bac1.getJob(new AweJobId(jobid));
		System.out.println(aj);
		
		jobid = "fdcafcec-f66c-4d37-be5c-8bfbf7cd268d";
		try {
			aj = bac1.getJob(new AweJobId(jobid));
		} catch (AweNoJobException anje) {
			assertThat("correct exception", anje.getMessage(), is(
					"job not found:fdcafcec-f66c-4d37-be5c-8bfbf7cd268d"));
		}
		System.out.println(aj);
	}
	
	@Test
	public void listJobs() throws Exception {
		List<AweJob> lj = bac1.getJobs(null, true, true, true, true, true, true);
		System.out.println(lj);
	}

}
