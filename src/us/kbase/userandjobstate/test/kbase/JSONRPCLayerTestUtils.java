package us.kbase.userandjobstate.test.kbase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.LoggerFactory;

import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple14;
import us.kbase.common.service.Tuple5;
import us.kbase.common.service.Tuple7;
import us.kbase.userandjobstate.Result;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;
import us.kbase.userandjobstate.UserAndJobStateServer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class JSONRPCLayerTestUtils {

	protected static String CHAR101 = "";
	protected static String CHAR1001 = "";
	static {
		String hundred = "";
		for (int i = 0; i < 10; i++) {
			hundred += "0123456789";
		}
		CHAR101 = hundred + "a";
		String thousand = "";
		for (int i = 0; i < 10; i++) {
			thousand += hundred;
		}
		CHAR1001 = thousand + "a";
	}
	
	static {
		//stfu Jetty
		((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME))
				.setLevel(Level.OFF);
	}
	
	protected static class ServerThread extends Thread {

		private final UserAndJobStateServer server;
		
		public ServerThread(UserAndJobStateServer server) {
			this.server = server;
		}
		
		public void run() {
			try {
				server.startupServer();
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
	
	private SimpleDateFormat getDateFormat() {
		SimpleDateFormat dateform =
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		dateform.setLenient(false);
		return dateform;
	}
	
	protected void checkJob(UserAndJobStateClient cli, String id, String stage, String status,
			String service, String desc, String progtype, Long prog,
			Long maxprog, String estCompl, Long complete, Long error,
			String errormsg, Results results)
			throws Exception {
		SimpleDateFormat dateform = getDateFormat();
		Tuple14<String, String, String, String, String, String,
				Long, Long, String, String, Long, Long, String,
				Results> ret = cli.getJobInfo(id);
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
		
		Tuple5<String, String, Long, String, String> jobdesc =
				cli.getJobDescription(id);
		assertThat("job service ok", jobdesc.getE1(), is(service));
		assertThat("job progtype ok", jobdesc.getE2(), is(progtype));
		assertThat("job maxprog ok", jobdesc.getE3(), is(maxprog));
		assertThat("job desc ok", jobdesc.getE4(), is(desc));
		if (jobdesc.getE5() != null) {
			dateform.parse(jobdesc.getE5()); //should throw error if bad format
		}
		
		Tuple7<String, String, String, Long, String, Long, Long> 
				jobstat = cli.getJobStatus(id);
		dateform.parse(jobstat.getE1()); //should throw error if bad format
		assertThat("job stage ok", jobstat.getE2(), is(stage));
		assertThat("job status ok", jobstat.getE3(), is(status));
		assertThat("job progress ok", jobstat.getE4(), is(prog));
		assertThat("job est compl ok", jobstat.getE5(), is(estCompl));
		assertThat("job complete ok", jobstat.getE6(), is(complete));
		assertThat("job error ok", jobstat.getE7(), is(error));
		
		checkResults(cli.getResults(id), results);
		
		assertThat("job error msg ok", cli.getDetailedError(id),
				is(errormsg));
	}
	
	protected void checkResults(Results got, Results expected) throws Exception {
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
		if (got.getResults() == null ^ expected.getResults() == null) {
			fail("got null for results.getResults() when expected real results or vice versa: " 
					+ got + " " + expected);
		}
		if (got.getResults() == null) {return;}
		if (got.getResults().size() != expected.getResults().size()) {
			fail("results lists not same size");
		}
		Iterator<Result> gr = got.getResults().iterator();
		Iterator<Result> er = expected.getResults().iterator();
		while (gr.hasNext()) {
			Result gres = gr.next();
			Result eres = er.next();
			assertThat("server type same", gres.getServerType(), is(eres.getServerType()));
			assertThat("url same", gres.getUrl(), is(eres.getUrl()));
			assertThat("id same", gres.getId(), is(eres.getId()));
			assertThat("description same", gres.getDescription(), is(eres.getDescription()));
		}
	}
}
