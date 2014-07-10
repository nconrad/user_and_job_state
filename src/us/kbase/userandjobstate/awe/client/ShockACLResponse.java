package us.kbase.userandjobstate.awe.client;


import us.kbase.userandjobstate.awe.client.exceptions.AweHttpException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


class ShockACLResponse extends AweResponse {
	
	private ShockACLResponse(){}
	
	@JsonProperty("data")
	private ShockACL data;
	
	@JsonIgnore
	ShockACL getAweData() throws AweHttpException {
		checkErrors();
		return data;
	}
}
