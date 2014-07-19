package us.kbase.userandjobstate.awe.client.exceptions;

@SuppressWarnings("serial")
public class AweIllegalSharingException extends AweHttpException {

	public AweIllegalSharingException(int code) {
		super(code);
	}

	public AweIllegalSharingException(int code, String message) {
		super(code, message);
	}

	public AweIllegalSharingException(int code, String message, Throwable cause) {
		super(code, message, cause);
	}

	public AweIllegalSharingException(int code, Throwable cause) {
		super(code, cause);
	}

}
