package us.kbase.userandjobstate.awe.client;

import java.util.List;

import us.kbase.userandjobstate.awe.client.exceptions.AweHttpException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

class AweJobListResponse extends AweResponse {
	
	private AweJobListResponse(){}
	
	@SuppressWarnings("unused")
	@JsonProperty
	private int limit;
	@SuppressWarnings("unused")
	@JsonProperty
	private int offset;
	@SuppressWarnings("unused")
	@JsonProperty
	private int total_count;
	
	@JsonProperty("data")
	private List<AweJob> data;
	
	@JsonIgnore
	List<AweJob> getAweData() throws AweHttpException {
		checkErrors();
		return data;
	}
}
