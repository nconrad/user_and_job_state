package us.kbase.userandjobstate.userstate.exceptions;

/** 
 * Thrown when a requested key doesn't exist.
 * @author gaprice@lbl.gov
 *
 */
public class NoSuchKeyException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public NoSuchKeyException() { super(); }
	public NoSuchKeyException(String message) { super(message); }
	public NoSuchKeyException(String message, Throwable cause) { super(message, cause); }
	public NoSuchKeyException(Throwable cause) { super(cause); }
}
