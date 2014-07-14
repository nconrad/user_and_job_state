package us.kbase.userandjobstate.awe.client;

/**
 * Represents a shock user ID.
 * @author gaprice@lbl.gov
 *
 */
public class AweUserId extends AweJobId {

	/**
	 * Construct a user ID.
	 * @param id the ID to create.
	 * @throws IllegalArgumentException if the id is not a valid shock user ID.
	 */
	public AweUserId(String id) throws IllegalArgumentException {
		super(id);
	}
}
