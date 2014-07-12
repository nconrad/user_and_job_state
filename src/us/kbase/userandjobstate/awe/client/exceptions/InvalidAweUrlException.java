package us.kbase.userandjobstate.awe.client.exceptions;

/** 
 * Thrown when the shock url provided to the client doesn't point to a 
 * shock server.
 * @author gaprice@lbl.gov
 *
 */
public class InvalidAweUrlException extends AweException {

	private static final long serialVersionUID = 1L;
	
	public InvalidAweUrlException() { super(); }
	public InvalidAweUrlException(String message) { super(message); }
	public InvalidAweUrlException(String message, Throwable cause) { super(message, cause); }
	public InvalidAweUrlException(Throwable cause) { super(cause); }
}
