package us.kbase.userandjobstate.awe.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AweTaskCommand {

	private String args;
	private String description;
	private String name;
	private AweTaskEnvironment environ;
	
	private AweTaskCommand() {};
	
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
