package us.kbase.userandjobstate.awe.client;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AweJobTaskEnvironment {
	private Map<String, String> public_;
	
	private AweJobTaskEnvironment() {};
	
	public Map<String, String> getPublic() {
		return public_;
	}
	
	public void setPublic(Map<String, String> public_) {
		this.public_ = public_;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AweTaskEnvironment [public_=");
		builder.append(public_);
		builder.append("]");
		return builder.toString();
	}
}
