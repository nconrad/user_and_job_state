package us.kbase.userandjobstate.jobstate.exceptions;

/** 
 * Thrown when a requested job doesn't exist.
 * @author gaprice@lbl.gov
 *
 */
public class NoSuchJobException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public NoSuchJobException() { super(); }
	public NoSuchJobException(String message) { super(message); }
	public NoSuchJobException(String message, Throwable cause) { super(message, cause); }
	public NoSuchJobException(Throwable cause) { super(cause); }
}
