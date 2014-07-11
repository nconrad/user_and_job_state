package us.kbase.userandjobstate.awe.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AweJobInfo {
	private String pipeline;
	private String name;
	private String project;
	private String user;
	private String clientgroups;
	private String xref;
	private String submittime;
	private String startedtime;
	private String completedtime;
	private Boolean auth;
	private Boolean noretry;
	private String service;
	private String description;
	
	private AweJobInfo() {};
	
	public String getPipeline() {
		return pipeline;
	}
	
	public String getName() {
		return name;
	}
	
	public String getProject() {
		return project;
	}
	
	public String getUser() {
		return user;
	}
	
	public String getClientgroups() {
		return clientgroups;
	}
	
	public String getXref() {
		return xref;
	}

	public String getSubmittime() {
		return submittime;
	}
	
	public String getStartedtime() {
		return startedtime;
	}
	
	public String getCompletedtime() {
		return completedtime;
	}
	
	public Boolean getAuth() {
		return auth;
	}
	
	public Boolean getNoretry() {
		return noretry;
	}
	
	public String getService() {
		return service;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AweJobInfo [pipeline=");
		builder.append(pipeline);
		builder.append(", name=");
		builder.append(name);
		builder.append(", project=");
		builder.append(project);
		builder.append(", user=");
		builder.append(user);
		builder.append(", clientgroups=");
		builder.append(clientgroups);
		builder.append(", xref=");
		builder.append(xref);
		builder.append(", submittime=");
		builder.append(submittime);
		builder.append(", startedtime=");
		builder.append(startedtime);
		builder.append(", completedtime=");
		builder.append(completedtime);
		builder.append(", auth=");
		builder.append(auth);
		builder.append(", noretry=");
		builder.append(noretry);
		builder.append("]");
		return builder.toString();
	}
}	
