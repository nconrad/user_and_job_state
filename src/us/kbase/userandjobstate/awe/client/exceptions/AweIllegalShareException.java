package us.kbase.userandjobstate.awe.client.exceptions;

@SuppressWarnings("serial")
public class AweIllegalShareException extends AweHttpException {

	public AweIllegalShareException(int code) {
		super(code);
	}

	public AweIllegalShareException(int code, String message) {
		super(code, message);
	}

	public AweIllegalShareException(int code, String message, Throwable cause) {
		super(code, message, cause);
	}

	public AweIllegalShareException(int code, Throwable cause) {
		super(code, cause);
	}

}
