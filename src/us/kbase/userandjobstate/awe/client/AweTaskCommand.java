package us.kbase.userandjobstate.awe.client;

public class AweTaskCommand {
	private String args;
	private String description;
	private String name;
	private AweTaskEnvironment environ;
	
	public String getArgs() {
		return args;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getName() {
		return name;
	}
	
	public AweTaskEnvironment getEnviron() {
		return environ;
	}
}
