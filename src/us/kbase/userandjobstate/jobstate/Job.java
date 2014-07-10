package us.kbase.userandjobstate.jobstate;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface Job {

	public String getID();

	public String getStage();

	public String getUser();

	public String getService();

	public String getDescription();

	public String getProgType();

	public Integer getProgress();

	public Integer getMaxProgress();

	public String getStatus();

	public Date getStarted();

	public Date getEstimatedCompletion();

	public Date getLastUpdated();

	public Boolean isComplete();

	public Boolean hasError();

	public String getErrorMsg();

	public Map<String, Object> getResults();

	public List<String> getShared();

	public String toString();
	
	public String getSource();

}