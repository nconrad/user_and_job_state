package us.kbase.userandjobstate.awe.client.exceptions;

/** 
 * Thrown when the person represented by the client token does not have
 * authorization for the requested action on the shock server.
 * @author gaprice@lbl.gov
 *
 */
public class AweAuthorizationException extends AweHttpException {

	private static final long serialVersionUID = 1L;
	
	public AweAuthorizationException(int code) {
		super(code);
	}
	public AweAuthorizationException(int code, String message) {
		super(code, message);
	}
	public AweAuthorizationException(int code, String message,
			Throwable cause) {
		super(code, message, cause);
	}
	public AweAuthorizationException(int code, Throwable cause) {
		super(code, cause);
	}
}
