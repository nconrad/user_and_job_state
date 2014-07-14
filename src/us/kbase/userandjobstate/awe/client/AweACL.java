package us.kbase.userandjobstate.awe.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one or more of the access control lists (ACLs) for a shock
 * object <b>at the time the ACL(s) were retrieved from shock</b>. 
 * Later updates to the ACLs will not be reflected in the instance.
 * To update the local representation of the ACLs
 * {@link us.kbase.BasicAweClient.client.BasicShockClient#getACLs(AweJobId)
 * getACLs()} must be called again.</p>
 *
 * This class is never instantiated manually.
 * 
 * @author gaprice@lbl.gov
 *
 */
public class AweACL extends AweData {

	private AweUserId owner;
	private List<AweUserId> read;
	private List<AweUserId> write;
	private List<AweUserId> delete;
	
	private AweACL(){}

	/**
	 * Get the user ID of the node's owner.
	 * @return the owner ID.
	 */
	public AweUserId getOwner() {
		return owner;
	}

	/**
	 * Get the list of user IDs that can read the node.
	 * @return the list of IDs or <code>null</code> if the list was not 
	 * included in the server response.
	 */
	public List<AweUserId> getRead() {
		if (read == null) {return null;}
		return new ArrayList<AweUserId>(read);
	}

	/**
	 * Get the list of user IDs that can write to the node.
	 * @return the list of IDs or <code>null</code> if the list was not 
	 * included in the server response.
	 */
	public List<AweUserId> getWrite() {
		if (write == null) {return null;}
		return new ArrayList<AweUserId>(write);
	}

	/**
	 * Get the list of user IDs that can delete the node.
	 * @return the list of IDs or <code>null</code> if the list was not 
	 * included in the server response.
	 */
	public List<AweUserId> getDelete() {
		if (delete == null) {return null;}
		return new ArrayList<AweUserId>(delete);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		//this is repulsive. Rethink this later.
		if (this == obj) {return true;}
		if (!(obj instanceof AweACL)) {return false;}
		AweACL acl = (AweACL)obj;
		if ((this.owner == null ^ acl.owner == null) ||
			(this.read == null ^ acl.read == null) ||
			(this.write == null ^ acl.write == null) ||
			(this.delete == null ^ acl.delete == null)) {
			return false;
		}
		return ((this.owner == null && acl.owner == null) || this.owner.equals(acl.owner)) &&
				((this.read == null && acl.read == null) || this.read.equals(acl.read)) &&
				((this.write == null && acl.write == null) || this.write.equals(acl.write)) &&
				((this.delete == null && acl.delete == null) || this.delete.equals(acl.delete));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AweACL [owner=" + owner + ", read=" + read + ", write="
				+ write + ", delete=" + delete + "]";
	}
}
