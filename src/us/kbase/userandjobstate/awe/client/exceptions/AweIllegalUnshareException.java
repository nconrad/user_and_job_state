package us.kbase.userandjobstate.awe.client.exceptions;

@SuppressWarnings("serial")
public class AweIllegalUnshareException extends AweHttpException {

	public AweIllegalUnshareException(int code) {
		super(code);
	}

	public AweIllegalUnshareException(int code, String message) {
		super(code, message);
	}

	public AweIllegalUnshareException(int code, String message, Throwable cause) {
		super(code, message, cause);
	}

	public AweIllegalUnshareException(int code, Throwable cause) {
		super(code, cause);
	}

}
