package us.kbase.userandjobstate.awe.client;

import java.util.Map;

public class AweTaskEnvironment {
	private Map<String, String> public_;
	private Map<String, String> private_ ;
	
	public Map<String, String> getPublic() {
		return public_;
	}
	
	public void setPublic(Map<String, String> public_) {
		this.public_ = public_;
	}
	
	public Map<String, String> getPrivate() {
		return private_;
	}
	
	public void setPrivate(Map<String, String> private_) {
		this.private_ = private_;
	}
}
