package us.kbase.userandjobstate.awe;

import static us.kbase.userandjobstate.awe.client.BasicAweClient.STATE_COMPL;
import static us.kbase.userandjobstate.awe.client.BasicAweClient.STATE_SUSP;
import static us.kbase.userandjobstate.awe.client.BasicAweClient.STATE_DEL;
import static us.kbase.userandjobstate.awe.client.BasicAweClient.STATE_INPROG;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import us.kbase.common.exceptions.UnimplementedException;
import us.kbase.userandjobstate.awe.client.AweJobIO;
import us.kbase.userandjobstate.awe.client.AweJob;
import us.kbase.userandjobstate.awe.client.AweJobTask;
import us.kbase.userandjobstate.jobstate.Job;
import us.kbase.userandjobstate.jobstate.JobResult;
import us.kbase.userandjobstate.jobstate.JobResults;
import us.kbase.userandjobstate.jobstate.JobState;

public class UJSAweJob implements Job {
	
	private static final String SOURCE = "Awe";
	
	
	private final static Map<String, String> STATE_MAP =
			new HashMap<String, String>(3);
	static {
		STATE_MAP.put(STATE_SUSP, ERROR);
		STATE_MAP.put(STATE_COMPL, COMPLETE);
		STATE_MAP.put(STATE_DEL, DELETED);
		STATE_MAP.put(STATE_INPROG, STARTED);
	}
	
	private final AweJob job;
	
	private final static DateTimeFormatter DATE_PARSER =
			ISODateTimeFormat.dateTimeParser();
	
	public UJSAweJob(final AweJob job) {
		if (job == null) {
			throw new NullPointerException("job cannot be null");
		}
		this.job = job;
	}
	
	private final static String translateState(final String state) {
		if (!STATE_MAP.containsKey(state)) {
			return CREATED;
		}
		return STATE_MAP.get(state);
	}

	private static Date parseDate(final String date) {
		if (date == null) {
			return null;
		}
		try {
			return DATE_PARSER.parseDateTime(date).toDate();
		} catch (IllegalArgumentException iae) {
			throw new IllegalArgumentException("Unparseable date: " +
					iae.getMessage(), iae);
		}
	}
	
	@Override
	public String getID() {
		return job.getId().getId();
	}

	@Override
	public String getStage() {
		return translateState(job.getState());
	}

	@Override
	public String getUser() {
		//TODO implement when awe has human readable ACLs
		throw new UnimplementedException(
				"It is not currently possible to get the owner of an Awe job.");
	}

	@Override
	public String getService() {
		return job.getInfo().getService();
	}

	@Override
	public String getDescription() {
		return job.getInfo().getDescription();
	}

	@Override
	public String getProgType() {
		return JobState.PROG_TASK;
	}

	@Override
	public Integer getProgress() {
		return job.getTasks().size() - job.getRemaintasks();
	}

	@Override
	public Integer getMaxProgress() {
		return job.getTasks().size();
	}

	@Override
	public String getStatus() {
		return job.getNotes();
	}

	@Override
	public Date getStarted() {
		return parseDate(job.getInfo().getStartedtime());
	}

	@Override
	public Date getEstimatedCompletion() {
		return null;
	}

	@Override
	public Date getLastUpdated() {
		return parseDate(job.getUpdatetime());
	}

	@Override
	public Boolean isComplete() {
		return STATE_COMPL.equals(job.getState());
	}

	@Override
	public Boolean hasError() {
		return STATE_SUSP.equals(job.getState());
	}

	@Override
	public String getErrorMsg() {
		if (!hasError()) {
			return null;
		}
		if (job.getNotes() == null || job.getNotes().isEmpty()) {
			return "Job was manually suspended.";
		}
		//TODO get stdout/err from client
		return job.getNotes();
	}

	@Override
	public JobResults getResults() {
		final List<JobResult> res = new LinkedList<JobResult>();
		for (final AweJobTask t: job.getTasks()) {
			if (t.getOutputs() != null) {
				for (final Entry<String, AweJobIO> output:
						t.getOutputs().entrySet()) {
					final AweJobIO io = output.getValue();
					if (io.isTemporary() == null || !io.isTemporary()) {
						res.add(new JobResult(
								"Shock",
								output.getValue().getHost(),
								output.getValue().getNode(),
								output.getKey()));
					}
				}
			}
		}
		
		return new JobResults(res, null, null, null, null);
	}

	@Override
	public List<String> getShared() {
		//TODO implement when awe has human readable ACLs
		throw new UnimplementedException(
				"It is not currently possible to get the list of users that can view an Awe job.");
	}

	@Override
	public String getSource() {
		return SOURCE;
	}

}
