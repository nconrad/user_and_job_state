package us.kbase.userandjobstate.awe.client.exceptions;

/**
 * Thrown on an attempt to get a node from shock that doesn't exist.
 * @author gaprice@lbl.gov
 *
 */
public class AweNoJobException extends AweHttpException {

	private static final long serialVersionUID = 1L;
	
	public AweNoJobException(int code) {
		super(code);
	}
	public AweNoJobException(int code, String message) {
		super(code, message);
	}
	public AweNoJobException(int code, String message,
			Throwable cause) {
		super(code, message, cause);
	}
	public AweNoJobException(int code, Throwable cause) {
		super(code, cause);
	}
}
