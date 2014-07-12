package us.kbase.userandjobstate.awe.client;

import us.kbase.userandjobstate.awe.client.exceptions.AweHttpException;


abstract class AweObjectResponse extends AweResponse {

	AweObjectResponse() {}

	private AweData data;

	abstract AweData getAweData() throws AweHttpException;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AweObjectResponse [error=");
		builder.append(getError());
		builder.append(", data=");
		builder.append(data);
		builder.append(", status=");
		builder.append(getStatus());
		builder.append("]");
		return builder.toString();
	}

//	@Override
//	public String toString() {
//		return getClass().getName() + " [error=" + error.get(0) +
//				", data=" + data + ", status=" + status + "]";
//	}
}
