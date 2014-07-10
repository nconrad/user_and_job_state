package us.kbase.userandjobstate.awe.client;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AweJobTask {
	private AweTaskCommand cmd;
	private List<String> dependsOn;
	private String taskid;
	private int skip;
	private int totalwork;
	private Map<String, AweIO> inputs;
	private Map<String, AweIO> outputs;
	private Map<String, AweIO> predata;
	private Integer maxworksize;
	private Integer remainwork;
	private String state;
	private String createddate;
	private String starteddate;
	private String completeddate;
	private Integer computetime;
	
	private AweJobTask() {};
	
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
	
	public Map<String, AweIO> getInputs() {
		return inputs;
	}
	
	public Map<String, AweIO> getOutputs() {
		return outputs;
	}
	
	public Map<String, AweIO> getPredata() {
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AweJobTask [cmd=");
		builder.append(cmd);
		builder.append(", dependsOn=");
		builder.append(dependsOn);
		builder.append(", taskid=");
		builder.append(taskid);
		builder.append(", skip=");
		builder.append(skip);
		builder.append(", totalwork=");
		builder.append(totalwork);
		builder.append(", inputs=");
		builder.append(inputs);
		builder.append(", outputs=");
		builder.append(outputs);
		builder.append(", predata=");
		builder.append(predata);
		builder.append(", maxworksize=");
		builder.append(maxworksize);
		builder.append(", remainwork=");
		builder.append(remainwork);
		builder.append(", state=");
		builder.append(state);
		builder.append(", createddate=");
		builder.append(createddate);
		builder.append(", starteddate=");
		builder.append(starteddate);
		builder.append(", completeddate=");
		builder.append(completeddate);
		builder.append(", computetime=");
		builder.append(computetime);
		builder.append("]");
		return builder.toString();
	}
}
