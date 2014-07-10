package us.kbase.userandjobstate.awe.client;

import us.kbase.userandjobstate.awe.client.exceptions.AweHttpException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

class AweJobResponse extends AweObjectResponse {
	
	private AweJobResponse(){}
	
	@JsonProperty("data")
	private AweJob data;
	
	@JsonIgnore
	@Override
	AweJob getAweData() throws AweHttpException {
		checkErrors();
		return data;
	}
}
