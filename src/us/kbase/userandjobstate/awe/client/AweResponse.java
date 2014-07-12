package us.kbase.userandjobstate.awe.client;

import java.util.List;

import us.kbase.userandjobstate.awe.client.exceptions.AweAuthorizationException;
import us.kbase.userandjobstate.awe.client.exceptions.AweHttpException;
import us.kbase.userandjobstate.awe.client.exceptions.AweNoJobException;


abstract class AweResponse {

	AweResponse() {}

	// per Jared, the error field will either be null or a list with one error
	// string.
	private List<String> error;
	private int status;

	public String getError() {
		return error.get(0);
	}

	public boolean hasError() {
		return error != null;
	}
	
	protected void checkErrors() throws AweHttpException {
		if (hasError()) {
			if (status == 401) {
				throw new AweAuthorizationException(getStatus(), getError());
			} else if (status == 400
					&& getError().contains("job not found")) {
				throw new AweNoJobException(getStatus(), getError());
			} else {
				throw new AweHttpException(getStatus(), getError());
			}
		}
	}

	public int getStatus() {
		return status;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AweResponse [error=");
		builder.append(error);
		builder.append(", status=");
		builder.append(status);
		builder.append("]");
		return builder.toString();
	}

//	@Override
//	public String toString() {
//		return getClass().getName() + " [error=" + error.get(0) +
//				", data=" + data + ", status=" + status + "]";
//	}
}
