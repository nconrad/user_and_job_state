package us.kbase.userandjobstate.jobstate.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.common.test.RegexMatcher;
import us.kbase.userandjobstate.jobstate.Job;
import us.kbase.userandjobstate.jobstate.JobState;
import us.kbase.userandjobstate.jobstate.exceptions.NoSuchJobException;
import us.kbase.userandjobstate.test.UserJobStateTestCommon;

public class JobStateTests {
	
	private static JobState js;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		UserJobStateTestCommon.destroyAndSetupDB();
		
		String host = UserJobStateTestCommon.getHost();
		String mUser = UserJobStateTestCommon.getMongoUser();
		String mPwd = UserJobStateTestCommon.getMongoPwd();
		String db = UserJobStateTestCommon.getDB();
		
		if (mUser != null) {
			js = new JobState(host, db, "jobstate", mUser, mPwd);
		} else {
			js = new JobState(host, db, "jobstate");
		}
	}
	
	//TODO JSONRPC layer tests
	
	private static final RegexMatcher OBJ_ID_MATCH = new RegexMatcher("[\\da-f]{24}");
	
	private static String long101;
	private static String long201;
	private static String long1001;
	static {
		for (int i = 0; i < 10; i++) {
			long101 += "aaaaaaaaaabbbbbbbbbb";
			long201 += long101 + long101;
			for (int j = 0; j < 10; j++) {
				long1001 += long201 + long201;
			}
		}
		long101 += "f";
		long201 += "f";
		long1001 += "f";
	}
	
	
	@Test
	public void createJob() throws Exception {
		try {
			js.createJob(null);
			fail("created job with invalid user");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(), 
					is("user cannot be null or the empty string"));
		}
		try {
			js.createJob("");
			fail("created job with invalid user");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(), 
					is("user cannot be null or the empty string"));
		}
		String jobid = js.createJob("foo");
		assertThat("get job id", jobid, OBJ_ID_MATCH);
		Job j = js.getJob("foo", jobid);
		checkJob(j, jobid, "created", "foo", null, null, null, null, null, null, null,
				null, null);
		try {
			js.getJob("foo1", jobid);
			fail("Got a non-existant job");
		} catch (NoSuchJobException nsje) {
			assertThat("correct exception", nsje.getLocalizedMessage(),
					is(String.format("There is no job %s for user foo1", jobid)));
		}
		try {
			js.getJob("foo", "a" + jobid.substring(1));
			fail("Got a non-existant job");
		} catch (NoSuchJobException nsje) {
			assertThat("correct exception", nsje.getLocalizedMessage(),
					is(String.format("There is no job %s for user foo",
					"a" + jobid.substring(1))));
		}
		try {
			js.getJob("foo", "a" + jobid);
			fail("Got a job with a bad id");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is(String.format("Job ID %s is not a legal ID",
					"a" + jobid)));
		}
	}
	
	@Test
	public void startJob() throws Exception {
		String jobid = js.createJob("foo");
		js.startJob("foo", jobid, "serv1", "started job", "job desc");
		Job j = js.getJob("foo", jobid);
		checkJob(j, jobid, "started", "foo", "started job", "serv1", "job desc", "none",
				null, null, false, false, null);
		testStartJobBadArgs("foo", jobid, "serv2", "started job", "job desc",
				new NoSuchJobException(String.format(
						"There is no unstarted job %s for user foo", jobid)));
		testStartJobBadArgs("foo1", jobid, "serv2", "started job", "job desc",
				new NoSuchJobException(String.format(
						"There is no unstarted job %s for user foo1", jobid)));
		testStartJobBadArgs("foo", "a" + jobid.substring(1), "serv2", "started job", "job desc",
				new NoSuchJobException(String.format(
						"There is no unstarted job %s for user foo", "a" + jobid.substring(1))));
		testStartJobBadArgs(null, jobid, "serv2", "started job", "job desc",
				new IllegalArgumentException("user cannot be null or the empty string"));
		testStartJobBadArgs("", jobid, "serv2", "started job", "job desc",
				new IllegalArgumentException("user cannot be null or the empty string"));
		testStartJobBadArgs(long101, jobid, "serv2", "started job", "job desc",
				new IllegalArgumentException("user exceeds the maximum length of 100"));
		testStartJobBadArgs("foo",  null, "serv2", "started job", "job desc",
				new IllegalArgumentException("id cannot be null or the empty string"));
		testStartJobBadArgs("foo", "", "serv2", "started job", "job desc",
				new IllegalArgumentException("id cannot be null or the empty string"));
		testStartJobBadArgs("foo", "afeaefafaefaefafeaf", "serv2", "started job", "job desc",
				new IllegalArgumentException("Job ID afeaefafaefaefafeaf is not a legal ID"));
		testStartJobBadArgs("foo", jobid, null, "started job", "job desc",
				new IllegalArgumentException("service cannot be null or the empty string"));
		testStartJobBadArgs("foo", jobid, "", "started job", "job desc",
				new IllegalArgumentException("service cannot be null or the empty string"));
		testStartJobBadArgs("foo", jobid, long101, "started job", "job desc",
				new IllegalArgumentException("service exceeds the maximum length of 100"));
		testStartJobBadArgs("foo", jobid, "serv2", long201, "job desc",
				new IllegalArgumentException("status exceeds the maximum length of 200"));
		testStartJobBadArgs("foo", jobid, "serv2", "started job", long1001,
				new IllegalArgumentException("description exceeds the maximum length of 1000"));
		try {
			js.startJob("foo", jobid, "serv2", "started job", "job desc", 0);
			fail("Started job with 0 for num tasks");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is("The maximum progress for the job must be > 0"));
		}
		int[] char1 = {11614};
		String uni = new String(char1, 0, 1);
		jobid = js.createJob("unicode");
		js.startJob("unicode", jobid, "serv3", uni, "desc3", 200);
		j = js.getJob("unicode", jobid);
		checkJob(j, jobid, "started", "unicode", uni, "serv3", "desc3",
				"task", 0, 200, false, false, null);
		
		jobid = js.createJob("foo3");
		js.startJob("foo3", jobid, "serv3", "start3", "desc3", 200);
		j = js.getJob("foo3", jobid);
		checkJob(j, jobid, "started", "foo3", "start3", "serv3", "desc3",
				"task", 0, 200, false, false, null);
		jobid = js.createJob("foo4");
		js.startJobWithPercentProg("foo4", jobid, "serv4", "start4", "desc4");
		j = js.getJob("foo4", jobid);
		checkJob(j, jobid, "started", "foo4", "start4", "serv4", "desc4",
				"percent", 0, 100, false, false, null);
		
		jobid = js.createAndStartJob("fooc1", "servc1", "startc1", "desc_c1");
		j = js.getJob("fooc1", jobid);
		checkJob(j, jobid, "started", "fooc1", "startc1", "servc1", "desc_c1", "none",
				null, null, false, false, null);
		
		jobid = js.createAndStartJob("fooc2", "servc2", "startc2", "desc_c2", 50);
		j = js.getJob("fooc2", jobid);
		checkJob(j, jobid, "started", "fooc2", "startc2", "servc2", "desc_c2",
				"task", 0, 50, false, false, null);
		
		jobid = js.createAndStartJobWithPercentProg("fooc3", "servc3",
				"startc3", "desc_c3");
		j = js.getJob("fooc3", jobid);
		checkJob(j, jobid, "started", "fooc3", "startc3", "servc3", "desc_c3",
				"percent", 0, 100, false, false, null);
		
	}
	
	private void testStartJobBadArgs(String user, String jobid, String service,
			String status, String desc, Exception exception) throws Exception {
		try {
			js.startJob(user, jobid, service, status, desc);
			fail("Started job with bad args");
		} catch (Exception e) {
			assertThat("correct exception type", e,
					is(exception.getClass()));
			assertThat("correct exception msg", e.getLocalizedMessage(),
					is(exception.getLocalizedMessage()));
		}
		try {
			js.startJobWithPercentProg(user, jobid, service, status, desc);
			fail("Started job with bad args");
		} catch (Exception e) {
			assertThat("correct exception type", e,
					is(exception.getClass()));
			assertThat("correct exception msg", e.getLocalizedMessage(),
					is(exception.getLocalizedMessage()));
		}
		try {
			js.startJob(user, jobid, service, status, desc, 6);
			fail("Started job with bad args");
		} catch (Exception e) {
			assertThat("correct exception type", e,
					is(exception.getClass()));
			assertThat("correct exception msg", e.getLocalizedMessage(),
					is(exception.getLocalizedMessage()));
		}
		if (!goodID(jobid) || exception instanceof NoSuchJobException) {
			return;
		}
		try {
			js.createAndStartJob(user, service, status, desc);
			fail("Started job with bad args");
		} catch (Exception e) {
			assertThat("correct exception type", e,
					is(exception.getClass()));
			assertThat("correct exception msg", e.getLocalizedMessage(),
					is(exception.getLocalizedMessage()));
		}
		try {
			js.createAndStartJobWithPercentProg(user, service, status, desc);
			fail("Started job with bad args");
		} catch (Exception e) {
			assertThat("correct exception type", e,
					is(exception.getClass()));
			assertThat("correct exception msg", e.getLocalizedMessage(),
					is(exception.getLocalizedMessage()));
		}
		try {
			js.createAndStartJob(user, service, status, desc, 6);
			fail("Started job with bad args");
		} catch (Exception e) {
			assertThat("correct exception type", e,
					is(exception.getClass()));
			assertThat("correct exception msg", e.getLocalizedMessage(),
					is(exception.getLocalizedMessage()));
		}
	}
	
	private static boolean goodID(String jobid) {
		try {
			new ObjectId(jobid);
			return true;
		} catch (IllegalArgumentException iae) {
			return false;
		}
	}
	
	private void checkJob(Job j, String id, String stage, String user,
			String status, String service, String desc, String progtype,
			Integer prog, Integer maxproj, Boolean complete, Boolean error,
			Map<String, Object> results) {
		assertThat("job id ok", j.getID(), is(id));
		assertThat("job stage ok", j.getStage(), is(stage));
		assertThat("job user ok", j.getUser(), is(user));
		assertThat("job service ok", j.getService(), is(service));
		assertThat("job desc ok", j.getDescription(), is(desc));
		assertThat("job progtype ok", j.getProgType(), is(progtype));
		assertThat("job prog ok", j.getProgress(), is(prog));
		assertThat("job maxprog ok", j.getMaxProgress(), is(maxproj));
		assertThat("job status ok", j.getStatus(), is(status));
		assertThat("job updated ok", j.getLastUpdated(), is(Date.class));
		assertThat("job complete ok", j.isComplete(), is(complete));
		assertThat("job error ok", j.hasError(), is(error));
	}
	
	@Test
	public void updateJob() throws Exception {
		//task based progress
		String jobid = js.createAndStartJob("bar", "service1", "st", "de", 33);
		Job j = js.getJob("bar", jobid);
		checkJob(j, jobid, "started", "bar", "st", "service1", "de",
				"task", 0, 33, false, false, null);
		
		js.updateJob("bar", jobid, "service1", "new st", 4);
		j = js.getJob("bar", jobid);
		checkJob(j, jobid, "started", "bar", "new st", "service1", "de",
				"task", 4, 33, false, false, null);
		
		js.updateJob("bar", jobid, "service1", "new st2", 16);
		j = js.getJob("bar", jobid);
		checkJob(j, jobid, "started", "bar", "new st2", "service1", "de",
				"task", 20, 33, false, false, null);
		
		js.updateJob("bar", jobid, "service1", "this really should be done", 16);
		j = js.getJob("bar", jobid);
		checkJob(j, jobid, "started", "bar", "this really should be done",
				"service1", "de", "task", 33, 33, false, false, null);
		
		//no progress tracking
		jobid = js.createAndStartJob("bar2", "service2", "st2", "de2");
		j = js.getJob("bar2", jobid);
		checkJob(j, jobid, "started", "bar2", "st2", "service2", "de2",
				"none", null, null, false, false, null);
		
		js.updateJob("bar2", jobid, "service2", "st2-2", null);
		j = js.getJob("bar2", jobid);
		checkJob(j, jobid, "started", "bar2", "st2-2", "service2", "de2",
				"none", null, null, false, false, null);
		
		js.updateJob("bar2", jobid, "service2", "st2-3", 6);
		j = js.getJob("bar2", jobid);
		checkJob(j, jobid, "started", "bar2", "st2-3", "service2", "de2",
				"none", null, null, false, false, null);
		
		//percentage based tracking
		jobid = js.createAndStartJobWithPercentProg("bar3", "service3", "st3", "de3");
		j = js.getJob("bar3", jobid);
		checkJob(j, jobid, "started", "bar3", "st3", "service3", "de3",
				"percent", 0, 100, false, false, null);
		
		js.updateJob("bar3", jobid, "service3", "st3-2", 30);
		j = js.getJob("bar3", jobid);
		checkJob(j, jobid, "started", "bar3", "st3-2", "service3", "de3",
				"percent", 30, 100, false, false, null);
		
		js.updateJob("bar3", jobid, "service3", "st3-3", 2);
		j = js.getJob("bar3", jobid);
		checkJob(j, jobid, "started", "bar3", "st3-3", "service3", "de3",
				"percent", 32, 100, false, false, null);
		
		js.updateJob("bar3", jobid, "service3", "st3-4", 80);
		j = js.getJob("bar3", jobid);
		checkJob(j, jobid, "started", "bar3", "st3-4", "service3", "de3",
				"percent", 100, 100, false, false, null);
		
		testUpdateJobBadArgs("bar3", jobid, "service2", "stat", null,
				new NoSuchJobException(String.format(
						"There is no job %s for user bar3 started by service service2",
						jobid)));
		testUpdateJobBadArgs("bar2", jobid, "service3", "stat", null,
				new NoSuchJobException(String.format(
						"There is no job %s for user bar2 started by service service3",
						jobid)));
		testUpdateJobBadArgs("bar2", "a" + jobid.substring(1), "service2", "stat", null,
				new NoSuchJobException(String.format(
						"There is no job %s for user bar2 started by service service2",
						"a" + jobid.substring(1))));
		testUpdateJobBadArgs(null, jobid, "serv2", "started job", 1,
				new IllegalArgumentException("user cannot be null or the empty string"));
		testUpdateJobBadArgs("", jobid, "serv2", "started job", 1,
				new IllegalArgumentException("user cannot be null or the empty string"));
		testUpdateJobBadArgs(long101, jobid, "serv2", "started job", 1,
				new IllegalArgumentException("user exceeds the maximum length of 100"));
		testUpdateJobBadArgs("foo",  null, "serv2", "started job", 1,
				new IllegalArgumentException("id cannot be null or the empty string"));
		testUpdateJobBadArgs("foo", "", "serv2", "started job", 1,
				new IllegalArgumentException("id cannot be null or the empty string"));
		testUpdateJobBadArgs("foo", "afeaefafaefaefafeaf", "serv2", "started job", 1,
				new IllegalArgumentException("Job ID afeaefafaefaefafeaf is not a legal ID"));
		testUpdateJobBadArgs("foo", jobid, null, "started job", 1,
				new IllegalArgumentException("service cannot be null or the empty string"));
		testUpdateJobBadArgs("foo", jobid, "", "started job", 1,
				new IllegalArgumentException("service cannot be null or the empty string"));
		testUpdateJobBadArgs("foo", jobid, long101, "started job", 1,
				new IllegalArgumentException("service exceeds the maximum length of 100"));
		testUpdateJobBadArgs("foo", jobid, "serv2", long201, 1,
				new IllegalArgumentException("status exceeds the maximum length of 200"));
		testUpdateJobBadArgs("foo", jobid, "serv2", "started job", -1,
				new IllegalArgumentException("progress cannot be negative"));
		//TODO finish update job tests
	}
	
	private void testUpdateJobBadArgs(String user, String jobid, String service,
			String status, Integer progress, Exception exception) throws Exception {
		try {
			js.updateJob(user, jobid, service, status, progress);
			fail("updated job with bad args");
		} catch (Exception e) {
			assertThat("correct exception type", e,
					is(exception.getClass()));
			assertThat("correct exception msg", e.getLocalizedMessage(),
					is(exception.getLocalizedMessage()));
			
		}
	}
}
