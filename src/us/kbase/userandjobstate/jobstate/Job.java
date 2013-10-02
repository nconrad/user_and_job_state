package us.kbase.userandjobstate.jobstate;

import static us.kbase.userandjobstate.jobstate.JobState.PROG_NONE;

import java.util.Date;
import java.util.Map;

import org.bson.types.ObjectId;

public class Job {
	
	private ObjectId _id;
	private String user;
	private String service;
	private String desc;
	private String progtype;
	private Integer prog;
	private Integer maxprog;
	private String status;
	private Date updated;
	private Boolean complete;
	private Boolean error;
	private Map<String, Object> results;
	
	private static final String CREATED = "created";
	private static final String STARTED = "started"; 
	private static final String COMPLETE = "complete";
	private static final String ERROR = "error";
	
	private Job() {}

	public String getID() {
		return _id.toString();
	}
	
	public String getStage() {
		if (service == null) {
			return CREATED;
		}
		if (!complete) {
			return STARTED;
		}
		if (!error) {
			return COMPLETE;
		}
		return ERROR;
	}
	
	public String getUser() {
		return user;
	}

	public String getService() {
		return service;
	}

	public String getDescription() {
		return desc;
	}

	public String getProgType() {
		return progtype;
	}

	public Integer getProgress() {
		if (getProgType() == null || getProgType().equals(PROG_NONE)) {
			return null;
		}
		if (isComplete() || getMaxProgress() < prog) {
			return getMaxProgress();
		}
		return prog;
	}

	public Integer getMaxProgress() {
		if (getProgType() == null || getProgType().equals(PROG_NONE)) {
			return null;
		}
		return maxprog;
	}

	public String getStatus() {
		return status;
	}

	public Date getLastUpdated() {
		return updated;
	}

	public Boolean isComplete() {
		return complete;
	}

	public Boolean hasError() {
		return error;
	}

	public Map<String, Object> getResults() {
		return results;
	}

	@Override
	public String toString() {
		return "Job [_id=" + _id + ", user=" + user +
				", service=" + service + ", desc=" + desc +
				", progtype=" + progtype + ", prog=" + getProgress() +
				", maxprog=" + getMaxProgress() + ", status=" + getStatus() +
				", updated=" + updated + ", complete=" + complete +
				", error=" + error + ", results=" + results + "]";
	}

}