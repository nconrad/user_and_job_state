package us.kbase.userandjobstate.awe.client;


import us.kbase.userandjobstate.awe.client.exceptions.AweHttpException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


class AweACLResponse extends AweObjectResponse {
	
	private AweACLResponse(){}
	
	@JsonProperty("data")
	private AweACL data;
	
	@JsonIgnore
	AweACL getAweData() throws AweHttpException {
		checkErrors();
		return data;
	}
}
