package us.kbase.userandjobstate.awe.client.exceptions;

/** 
 * Parent class for all Shock exceptions.
 * @author gaprice@lbl.gov
 *
 */
public class AweException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public AweException() { super(); }
	public AweException(String message) { super(message); }
	public AweException(String message, Throwable cause) { super(message, cause); }
	public AweException(Throwable cause) { super(cause); }
}
