package us.kbase.userandjobstate.awe.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenExpiredException;
import us.kbase.userandjobstate.awe.client.exceptions.InvalidAweUrlException;
import us.kbase.userandjobstate.awe.client.exceptions.AweHttpException;

/**
 * A basic client for shock. Creating nodes, deleting nodes,
 * getting a subset of node data, and altering read acls is currently supported.
 * 
 * Currently limited to 1000 connections.
 * 
 * @author gaprice@lbl.gov
 *
 */
public class BasicAweClient {
	
	//TODO this needs massive refactoring and redocumentation
	
	public final static String STATE_INIT = "init";
	public final static String STATE_QUEUED = "queued";
	public final static String STATE_INPROG = "in-progress";
	public final static String STATE_COMPL = "completed";
	public final static String STATE_SUSP = "suspend";
	public final static String STATE_DEL = "deleted";
	
	private final URI baseurl;
	private final URI joburl;
	private final static PoolingHttpClientConnectionManager connmgr =
			new PoolingHttpClientConnectionManager();
	static {
		connmgr.setMaxTotal(1000); //perhaps these should be configurable
		connmgr.setDefaultMaxPerRoute(1000);
	}
	//TODO set timeouts for the client for 1/2m for conn req timeout and std timeout
	private final static CloseableHttpClient client =
			HttpClients.custom().setConnectionManager(connmgr).build();
	private final ObjectMapper mapper = new ObjectMapper();
	private AuthToken token = null;
	
	private static final String AUTH = "Authorization";
	private static final String OAUTH = "OAuth ";
	private static final AweACLType ACL_READ = new AweACLType("read");
	
	/** Get the size of the upload / download chunk size.
	 * @return the size of the file chunks sent/received from the Shock server.
	 */
	
	/**
	 * Create a new shock client authorized to act as a shock user.
	 * @param url the location of the shock server.
	 * @param token the authorization token to present to shock.
	 * @throws IOException if an IO problem occurs.
	 * @throws InvalidAweUrlException if the <code>url</code> does not reference
	 * a shock server.
	 * @throws TokenExpiredException if the <code>token</code> is expired.
	 * @throws AweHttpException if the connection to shock fails.
	 */
	public BasicAweClient(final URL url, final AuthToken token)
			throws IOException, InvalidAweUrlException,
			TokenExpiredException, AweHttpException {
		this(url);
		updateToken(token);
	}
	
	/**
	 * Create a new shock client.
	 * @param url the location of the shock server.
	 * @throws IOException if an IO problem occurs.
	 * @throws InvalidAweUrlException if the <code>url</code> does not reference
	 * a shock server.
	 */
	@SuppressWarnings("unchecked")
	public BasicAweClient(final URL url) throws IOException, 
			InvalidAweUrlException {

		mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
		
		String turl = url.getProtocol() + "://" + url.getAuthority()
				+ url.getPath();
		if (turl.charAt(turl.length() - 1) != '/') {
			turl = turl + "/";
		}
		if (!(url.getProtocol().equals("http") ||
				url.getProtocol().equals("https"))) {
			throw new InvalidAweUrlException(turl.toString());
			
		}
		final CloseableHttpResponse response;
			try {
				response = client.execute(new HttpGet(turl));
			} catch (IllegalArgumentException iae) {
				if (iae.getMessage().contains("port out of range")) {
					throw new InvalidAweUrlException(
							"Awe port is out of range: " +
									url.toExternalForm(), iae);
				}
				throw iae;
			}
		final Map<String, Object> shockresp;
		try {
			final String resp = EntityUtils.toString(response.getEntity());
			shockresp = mapper.readValue(resp, Map.class);
		} catch (JsonParseException jpe) {
			throw new InvalidAweUrlException(turl.toString(), jpe);
		} finally {
			response.close();
		}
		if (!shockresp.containsKey("id")) {
			throw new InvalidAweUrlException(turl.toString());
		}
		if (!shockresp.get("id").equals("AWE")) {
			throw new InvalidAweUrlException(turl.toString());
		}
		URL shockurl = new URL(shockresp.get("url").toString());
		//https->http is caused by the router, not shock, per Jared
		if (url.getProtocol().equals("https")) {
			shockurl = new URL("https", shockurl.getAuthority(),
					shockurl.getPort(), shockurl.getFile());
		}
		try {
			baseurl = shockurl.toURI();
		} catch (URISyntaxException use) {
			throw new Error(use); //something went badly wrong 
		}
		joburl = baseurl.resolve("job/");
	}
	
	/**
	 * Replace the token this client presents to the shock server.
	 * @param token the new token
	 * @throws TokenExpiredException if the <code>token</code> is expired.
	 */
	public void updateToken(final AuthToken token)
			throws TokenExpiredException {
		if (token == null) {
			this.token = null;
			return;
		}
		if (token.isExpired()) {
			throw new TokenExpiredException(token.getTokenId());
		}
		this.token = token;
	}
	
	/**
	 * Check the token's validity.
	 * @return <code>true</code> if the client has no auth token or the token
	 * is expired, <code>false</code> otherwise.
	 */
	public boolean isTokenExpired() {
		if (token == null || token.isExpired()) {
			return true;
		}
		return false;
	}
	
	public AuthToken getToken() {
		return token;
	}
	
	/** 
	 * Get the url of the shock server this client communicates with.
	 * @return the shock url.
	 */
	public URL getAweUrl() {
		return uriToUrl(baseurl);
	}
	
	private <T extends AweObjectResponse> AweData
			processRequest(final HttpRequestBase httpreq, final Class<T> clazz)
			throws IOException, AweHttpException, TokenExpiredException {
		authorize(httpreq);
		final CloseableHttpResponse response = client.execute(httpreq);
		try {
			return getAweData(response, clazz);
		} finally {
			response.close();
		}
	}
	
	private <T extends AweObjectResponse> AweData
			getAweData(final HttpResponse response, final Class<T> clazz) 
			throws IOException, AweHttpException {
		final String resp = EntityUtils.toString(response.getEntity());
		try {
			return mapper.readValue(resp, clazz).getAweData();
		} catch (JsonParseException jpe) {
			throw new AweHttpException(
					response.getStatusLine().getStatusCode(),
					"Invalid Awe response. Server said " +
					response.getStatusLine().getStatusCode() + " " +
					response.getStatusLine().getReasonPhrase() + 
					". JSON parser said " + jpe.getLocalizedMessage(), jpe);
		}
	}
	
	//TODO rethink this later, should be a way of unifying list and object methods
	private <T extends AweJobListResponse> List<AweJob> processListRequest(
			final HttpRequestBase httpreq,
			final Class<T> clazz)
			throws IOException, AweHttpException, TokenExpiredException {
		authorize(httpreq);
		final CloseableHttpResponse response = client.execute(httpreq);
		try {
			return getAweListData(response, clazz);
		} finally {
			response.close();
		}
	}

	private <T extends AweJobListResponse> List<AweJob> getAweListData(
			final HttpResponse response,
			final Class<T> clazz) 
			throws IOException, AweHttpException {
		final String resp = EntityUtils.toString(response.getEntity());
		try {
			return mapper.readValue(resp, clazz).getAweData();
		} catch (JsonParseException jpe) {
			throw new AweHttpException(
					response.getStatusLine().getStatusCode(),
					"Invalid Awe response. Server said " +
					response.getStatusLine().getStatusCode() + " " +
					response.getStatusLine().getReasonPhrase() + 
					". JSON parser said " + jpe.getLocalizedMessage(), jpe);
		}
	}
	
	private void authorize(final HttpRequestBase httpreq) throws
			TokenExpiredException {
		if (token != null) {
			if (token.isExpired()) {
				throw new TokenExpiredException(token.getTokenId());
			}
			httpreq.setHeader(AUTH, OAUTH + token);
		}
	}

	/** 
	 * Gets a node from the shock server. Note the object returned 
	 * represents the shock node's state at the time getNode() was called
	 * and does not update further.
	 * @param id the ID of the shock node.
	 * @return a shock node object.
	 * @throws IOException if an IO problem occurs.
	 * @throws AweHttpException if the node could not be fetched from shock.
	 * @throws TokenExpiredException if the client authorization token has
	 * expired.
	 */
	public AweJob getJob(final AweJobId id) throws IOException,
			AweHttpException, TokenExpiredException {
		if (id == null) {
			throw new IllegalArgumentException("id may not be null");
		}
		final URI targeturl = joburl.resolve(id.getId());
		final HttpGet htg = new HttpGet(targeturl);
		return (AweJob)processRequest(htg, AweJobResponse.class);
	}

	public List<AweJob> getJobs(
			final List<String> services,
			final boolean init,
			final boolean queued,
			final boolean in_progress,
			final boolean complete,
			final boolean suspended,
			final boolean deleted)
			throws IOException, AweHttpException, TokenExpiredException {
		String url = joburl.toString() + "?limit=1000"; //TODO need to think about this limit
		final boolean hasServices = services != null && !services.isEmpty();
		final boolean nofilters = init && queued && in_progress && complete &&
				suspended && deleted;
		if (hasServices || !nofilters) {
			url += "&query";
		}
		if (hasServices) {
			url += "&info.service=" + StringUtils.join(services, ",");
		}
		if (!nofilters) {
			final List<String> states = new LinkedList<String>();
			if (init) {states.add(STATE_INIT);}
			if (queued) {states.add(STATE_QUEUED);}
			if (in_progress) {states.add(STATE_INPROG);}
			if (complete) {states.add(STATE_COMPL);}
			if (suspended) {states.add(STATE_SUSP);}
			if (deleted) {states.add(STATE_DEL);}
			url += "&state=" + StringUtils.join(states, ",");
		}
		final URI targeturl;
		try {
			targeturl = new URI(url);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Known good url is somehow bad", e);
		}
		final HttpGet htg = new HttpGet(targeturl);
		return (List<AweJob>) processListRequest(htg, AweJobListResponse.class);
	}

	/**
	 * Deletes a node on the shock server.
	 * @param id the node to delete.
	 * @throws IOException if an IO problem occurs.
	 * @throws AweHttpException if the node could not be deleted.
	 * @throws TokenExpiredException if the client authorization token has
	 * expired.
	 */
	public void deleteJob(final AweJobId id) throws IOException, 
			AweHttpException, TokenExpiredException {
		final URI targeturl = joburl.resolve(id.getId());
		final HttpDelete htd = new HttpDelete(targeturl);
		processRequest(htd, AweJobResponse.class); //triggers throwing errors
	}
	
	/**
	 * Adds a user to a shock node's read access control list (ACL).
	 * @param id the node to modify.
	 * @param user the user to be added to the read ACL.
	 * @throws IOException if an IO problem occurs.
	 * @throws AweHttpException if the node ACL could not be altered.
	 * @throws TokenExpiredException if the client authorization token has
	 * expired.
	 */
	public void setJobReadable(final AweJobId id, final List<String> users)
			throws IOException, AweHttpException, TokenExpiredException {
		final URI targeturl = checkACLArgsAndGenURI(id, users);
		final HttpPut htp = new HttpPut(targeturl);
		//TODO check errors are ok when Shock changes to ACLs for editing ACLs
		processRequest(htp, AweACLResponse.class); //triggers throwing errors
	}
	
	private URI checkACLArgsAndGenURI(final AweJobId id,
			final List<String> users) {
		if (id == null) {
			throw new IllegalArgumentException("id cannot be null");
		}
		if (users == null || users.isEmpty()) {
			throw new IllegalArgumentException(
					"user list cannot be null or empty");
		}
		for (final String user: users) {
			if (user == null || user.equals("")) {
				throw new IllegalArgumentException(
						"user cannot be null or the empty string");
			}
		}
		final URI targeturl = joburl.resolve(id.getId() + ACL_READ.acl + 
				"?users=" + StringUtils.join(users, ","));
		return targeturl;
	}
	
	public void removeJobReadable(final AweJobId id, final List<String> users)
			throws IOException, AweHttpException, TokenExpiredException {
		final URI targeturl = checkACLArgsAndGenURI(id, users);
		final HttpDelete htd = new HttpDelete(targeturl);
		processRequest(htd, AweACLResponse.class); //triggers throwing errors
	}
	
	/**
	 * Retrieves all the access control lists (ACLs) from the shock server for
	 * a node. Note the object returned represents the shock node's state at
	 * the time getACLs() was called and does not update further.
	 * @param id the node to query.
	 * @return the ACLs for the node.
	 * @throws IOException if an IO problem occurs.
	 * @throws AweHttpException if the node's access control lists could not be
	 * retrieved.
	 * @throws TokenExpiredException if the client authorization token has
	 * expired.
	 */
	public AweACL getACLs(final AweJobId id) throws IOException,
			AweHttpException, TokenExpiredException {
		return getACLs(id, new AweACLType());
	}
	
	/**
	 * Retrieves a specific access control list (ACL) from the shock server for
	 * a node. Note the object returned represents the shock node's state at
	 * the time getACLs() was called and does not update further.
	 * @param id the node to query.
	 * @param acl the type of ACL to retrieve.
	 * @return the ACL for the node.
	 * @throws IOException if an IO problem occurs.
	 * @throws AweHttpException if the node's access control list could not be
	 * retrieved.
	 * @throws TokenExpiredException if the client authorization token has
	 * expired.
	 */
	public AweACL getACLs(final AweJobId id, final AweACLType acl) 
			throws IOException, AweHttpException, TokenExpiredException {
		final URI targeturl = joburl.resolve(id.getId() + acl.acl);
		final HttpGet htg = new HttpGet(targeturl);
		return (AweACL)processRequest(htg, AweACLResponse.class);
	}
	
	//for known good uris ONLY
	private URL uriToUrl(final URI uri) {
		try {
			return uri.toURL();
		} catch (MalformedURLException mue) {
			throw new RuntimeException(mue); //something is seriously fuxxored
		}
	}
}
