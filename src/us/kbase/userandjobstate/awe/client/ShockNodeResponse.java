package us.kbase.userandjobstate.awe.client;


import us.kbase.userandjobstate.awe.client.exceptions.AweHttpException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


class ShockNodeResponse extends AweResponse {
	
	private ShockNodeResponse(){}
	
	@JsonProperty("data")
	private ShockNode data;
	
	@JsonIgnore
	ShockNode getAweData() throws AweHttpException {
		checkErrors();
		return data;
	}
}
