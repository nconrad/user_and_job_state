package us.kbase.userandjobstate.awe.client;

import java.util.List;
import java.util.Map;

public class AweJobTask {
	private AweTaskCommand cmd;
	private List<String> dependsOn;
	private String taskid;
	private int skip;
	private int totalwork;
	//TODO IO class
	private Map<String, Map<String, Object>> inputs;
	private Map<String, Map<String, Object>> outputs;
	private Map<String, Map<String, Object>> predata;
	private Integer maxworksize;
	private Integer remainwork;
	private String state;
	private String createddate;
	private String starteddate;
	private String completeddate;
	private Integer computetime;
	
	public AweTaskCommand getCmd() {
		return cmd;
	}
	
	public List<String> getDependsOn() {
		return dependsOn;
	}
	
	public String getTaskid() {
		return taskid;
	}
	
	public int getSkip() {
		return skip;
	}
	
	public int getTotalwork() {
		return totalwork;
	}
	
	public Map<String, Map<String, Object>> getInputs() {
		return inputs;
	}
	
	public Map<String, Map<String, Object>> getOutputs() {
		return outputs;
	}
	
	public Map<String, Map<String, Object>> getPredata() {
		return predata;
	}
	
	public Integer getMaxworksize() {
		return maxworksize;
	}
	
	public Integer getRemainwork() {
		return remainwork;
	}
	
	public String getState() {
		return state;
	}
	
	public String getCreateddate() {
		return createddate;
	}
	
	public String getStarteddate() {
		return starteddate;
	}
	
	public String getCompleteddate() {
		return completeddate;
	}
	
	public Integer getComputetime() {
		return computetime;
	}
}
