package us.kbase.userandjobstate.jobstate;

import static us.kbase.userandjobstate.jobstate.JobState.PROG_NONE;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
	private Date started;
	private Date updated;
	private Date estcompl;
	private Boolean complete;
	private Boolean error;
	private String errormsg;
	private Map<String, Object> results;
	private List<String> shared;
	
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

	public Date getStarted() {
		return started;
	}
	
	public Date getEstimatedCompletion() {
		return estcompl;
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
	
	public String getErrorMsg() {
		return errormsg;
	}

	public Map<String, Object> getResults() {
		return results;
	}
	
	public List<String> getShared() {
		if (shared == null) {
			return new LinkedList<String>();
		}
		return new LinkedList<String>(shared);
	}

	@Override
	public String toString() {
		return "Job [_id=" + _id + ", user=" + user +
				", service=" + service + ", desc=" + desc +
				", progtype=" + progtype + ", prog=" + getProgress() +
				", maxprog=" + getMaxProgress() + ", status=" + getStatus() +
				", started=" + started + ", estcompl=" + estcompl +
				", updated=" + updated + ", complete=" + complete +
				", error=" + error + ", errormsg=" + errormsg +
				", results=" + results + ", shared=" + shared + "]";
	}

}
