package us.kbase.userandjobstate.awe;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	
	private static final String AWE_SUSPENDED = "suspend";
	private static final String AWE_COMPLETED = "completed";
	private static final String AWE_DELETED = "deleted";
	private static final String AWE_STARTED = "in-progress";
	
	
	private final static Map<String, String> STATE_MAP =
			new HashMap<String, String>(3);
	static {
		STATE_MAP.put(AWE_SUSPENDED, ERROR);
		STATE_MAP.put(AWE_COMPLETED, COMPLETE);
		STATE_MAP.put(AWE_DELETED, DELETED);
		STATE_MAP.put(AWE_STARTED, STARTED);
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
		//TODO 1 test getservice
		return job.getInfo().getService();
	}

	@Override
	public String getDescription() {
		//TODO 1 test getdescriptoin
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
		return AWE_COMPLETED.equals(job.getState());
	}

	@Override
	public Boolean hasError() {
		return AWE_SUSPENDED.equals(job.getState());
	}

	@Override
	public String getErrorMsg() {
		if (!hasError()) {
			return null;
		}
		if (job.getNotes() == null || job.getNotes().isEmpty()) {
			return "Job was manually suspended.";
			//TODO 1 test this path
		}
		return job.getNotes();
	}

	@Override
	public JobResults getResults() {
		//TODO 1 get results
		final List<JobResult> res = new LinkedList<JobResult>();
		for (final AweJobTask t: job.getTasks()) {
			if (t.getOutputs() != null) {
				for (final AweJobIO output: t.getOutputs().values()) {
					
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
