package us.kbase.userandjobstate.awe.client;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import us.kbase.auth.TokenExpiredException;
import us.kbase.userandjobstate.awe.client.exceptions.AweHttpException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>Represents a shock node <b>at the time the node was retrieved from shock</b>.
 * Later updates to the node, including updates made via this class' methods,
 * will not be reflected in the instance. To update the local representation
 * of the node {@link us.kbase.BasicAweClient.client.BasicShockClient#getNode(ShockNodeId)
 * getNode()} must be called again.</p>
 * 
 * <p>This class is never instantiated manually.</p>
 * 
 * Note that mutating the return value of the {@link #getAttributes()} method
 * will alter this object's internal representation of the attributes.
 * 
 * @author gaprice@lbl.gov
 *
 */
// Don't need these and they're undocumented so ignore for now.
// last modified is a particular pain since null is represented as '-' so you
// can't just deserialize to a Date
@JsonIgnoreProperties({"relatives", "type", "indexes", "tags", "linkages",
	"created_on", "last_modified", "public"})
public class ShockNode extends AweData {

	private Map<String, Object> attributes;
	@JsonProperty("file")
	private ShockFileInformation file;
	private ShockNodeId id;
	private ShockVersionStamp version;
	@JsonIgnore
	private BasicAweClient client;
	@JsonIgnore
	private boolean deleted = false;
	
	private ShockNode(){}
	
	//MUST add a client after object deserialization or many of the methods
	//below will fail
	void addClient(final BasicAweClient client) {
		this.client = client;
	}
	
	private void checkDeleted() {
		if (deleted) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Get the shock node's attributes. Note mutating the attributes will
	 * mutate the internal representation of the attributes in this object.
	 * @return the shock node's attributes.
	 */
	public Map<String, Object> getAttributes() {
		checkDeleted();
		return attributes;
	}

	/** 
	 * Proxy for {@link BasicAweClient#deleteNode(ShockNodeId) deleteNode()}. 
	 * Deletes this node on the server. All methods will throw an
	 * exception after this method is called.
	 * @throws AweHttpException if the shock server couldn't delete the node.
	 * @throws IOException if an IO problem occurs.
	 * @throws TokenExpiredException if the client's token has expired.
	 */
	public void delete() throws AweHttpException, IOException,
			TokenExpiredException {
		client.deleteNode(getId());
		client = null; //remove ref to client
		deleted = true;
	}
	
	/**
	 * Proxy for {@link BasicAweClient#getACLs(ShockNodeId) getACLs()}.
	 * Returns all the access control lists (ACLs) for the node.
	 * @return the ACLs.
	 * @throws AweHttpException if the shock server cannot retrieve the ACLs.
	 * @throws IOException if an IO problem occurs.
	 * @throws TokenExpiredException if the client's token has expired.
	 */
	@JsonIgnore
	public ShockACL getACLs() throws AweHttpException, IOException,
			TokenExpiredException {
		return client.getACLs(getId());
	}
	
	/**
	 * Proxy for {@link BasicAweClient#getACLs(ShockNodeId, ShockACLType)
	 * getACLs()}.
	 * Returns the requested access control list (ACL) for the node.
	 * @param acl the type of ACL to retrive from shock.
	 * @return an ACL.
	 * @throws AweHttpException if the shock server cannot retrieve the ACL.
	 * @throws IOException if an IO problem occurs.
	 * @throws TokenExpiredException if the client's token has expired.
	 */
	@JsonIgnore
	public ShockACL getACLs(final ShockACLType acl) throws AweHttpException,
			IOException, TokenExpiredException {
		return client.getACLs(getId(), acl);
	}
	
	/**
	 * Proxy for {@link BasicAweClient#setNodeReadable(ShockNodeId, String)
	 * setNodeReadable()}.
	 * Adds the user to the node's read access control list (ACL).
	 * @param user the user to which read permissions shall be granted.
	 * @throws AweHttpException if the read ACL could not be modified.
	 * @throws IOException if an IO problem occurs.
	 * @throws TokenExpiredException if the client's token has expired.
	 * @throws UnvalidatedEmailException if the <code>user</code>'s email
	 * address is unvalidated.
	 */
	@JsonIgnore
	public void setReadable(final String user) throws AweHttpException,
			IOException, TokenExpiredException {
		client.setNodeReadable(getId(), user);
	}
	
	/**
	 * Get information about the file stored at this node.
	 * @return file information.
	 */
	@JsonIgnore
	public ShockFileInformation getFileInformation() {
		checkDeleted();
		return file;
	}
	
	/**
	 * Get the id of this node.
	 * @return this node's id.
	 */
	public ShockNodeId getId() {
		checkDeleted();
		return id;
	}
	
	/**
	 * Get the version of this node.
	 * @return this node's current version.
	 */
	public ShockVersionStamp getVersion() {
		checkDeleted();
		return version;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ShockNode [attributes=" + attributes + ", file=" + file
				+ ", id=" + id + ", version=" + version + ", deleted=" +
				deleted + "]";
	}
}
