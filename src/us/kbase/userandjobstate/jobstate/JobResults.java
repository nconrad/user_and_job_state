package us.kbase.userandjobstate.jobstate;

import static us.kbase.common.utils.StringUtils.checkMaxLen;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class JobResults {
	
	//TODO unit tests for this and JobResult
	
	private List<JobResult> results = null;
	private String workspaceurl = null;
	private List<String> workspaceids = null;
	private String shockurl = null;
	private List<String> shocknodes = null;
	
	public final static int MAX_LEN_URL = 1000;
	public final static int MAX_LEN_ID = 1000;
	
	@SuppressWarnings("unused")
	private JobResults() {} //for jongo
	
	public JobResults(
			final List<JobResult> results,
			final String workspaceurl,
			final List<String> workspaceids,
			final String shockurl,
			final List<String> shocknodes) {
		super();
		checkMaxLen(workspaceurl, "workspaceurl", MAX_LEN_URL);
		checkMaxLen(shockurl, "shockurl", MAX_LEN_URL);
		if (workspaceids != null) {
			for (final String id: workspaceids) {
				checkMaxLen(id, "workspaceid", MAX_LEN_ID);
			}
		}
		if (shocknodes != null) {
			for (final String id: shocknodes) {
				checkMaxLen(id, "shocknode", MAX_LEN_ID);
			}
		}
		this.results = makeImmutable(results);
		this.workspaceurl = workspaceurl;
		this.workspaceids = makeImmutable(workspaceids);
		this.shockurl = shockurl;
		this.shocknodes = makeImmutable(shocknodes);
	}
	
	private static <T> List<T> makeImmutable(final List<T> l) {
		if (l == null) {
			return null;
		}
		return Collections.unmodifiableList(
				new LinkedList<T>(l));
	}

	public List<JobResult> getResults() {
		return results;
	}

	public String getWorkspaceurl() {
		return workspaceurl;
	}

	public List<String> getWorkspaceids() {
		return workspaceids;
	}

	public String getShockurl() {
		return shockurl;
	}

	public List<String> getShocknodes() {
		return shocknodes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((results == null) ? 0 : results.hashCode());
		result = prime * result
				+ ((shocknodes == null) ? 0 : shocknodes.hashCode());
		result = prime * result
				+ ((shockurl == null) ? 0 : shockurl.hashCode());
		result = prime * result
				+ ((workspaceids == null) ? 0 : workspaceids.hashCode());
		result = prime * result
				+ ((workspaceurl == null) ? 0 : workspaceurl.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JobResults other = (JobResults) obj;
		if (results == null) {
			if (other.results != null)
				return false;
		} else if (!results.equals(other.results))
			return false;
		if (shocknodes == null) {
			if (other.shocknodes != null)
				return false;
		} else if (!shocknodes.equals(other.shocknodes))
			return false;
		if (shockurl == null) {
			if (other.shockurl != null)
				return false;
		} else if (!shockurl.equals(other.shockurl))
			return false;
		if (workspaceids == null) {
			if (other.workspaceids != null)
				return false;
		} else if (!workspaceids.equals(other.workspaceids))
			return false;
		if (workspaceurl == null) {
			if (other.workspaceurl != null)
				return false;
		} else if (!workspaceurl.equals(other.workspaceurl))
			return false;
		return true;
	}

}
