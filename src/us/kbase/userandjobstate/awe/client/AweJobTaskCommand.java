package us.kbase.userandjobstate.awe.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AweJobTaskCommand {

	private String args;
	private String description;
	private String name;
	private AweJobTaskEnvironment environ;
	
	private AweJobTaskCommand() {};
	
	public String getArgs() {
		return args;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getName() {
		return name;
	}
	
	public AweJobTaskEnvironment getEnviron() {
		return environ;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AweTaskCommand [args=");
		builder.append(args);
		builder.append(", description=");
		builder.append(description);
		builder.append(", name=");
		builder.append(name);
		builder.append(", environ=");
		builder.append(environ);
		builder.append("]");
		return builder.toString();
	}
}
