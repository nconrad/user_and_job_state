package us.kbase.userandjobstate.awe.client;

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
	
	//TODO service, description
	
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
}	
