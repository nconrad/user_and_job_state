package us.kbase.userandjobstate.jobstate.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import us.kbase.common.service.Tuple12;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.jobstate.Job;

public class FakeJob {
	
	//TODO move to common
	
	private final String id;
	private final String user;
	private final String service;
	private final String stage;
	private final String desc;
	private final String progtype;
	private final Integer prog;
	private final Integer maxprog;
	private final String status;
	private final Boolean complete;
	private final Boolean error;
	private final Map<String, Object> results;
	
	public FakeJob(final Job j) {
		id = j.getID();
		user = j.getUser();
		service = j.getService();
		stage = j.getStage();
		desc = j.getDescription();
		progtype = j.getProgType();
		prog = j.getProgress();
		maxprog = j.getMaxProgress();
		status = j.getStatus();
		assertThat("updated is date", j.getLastUpdated(), is(Date.class));
		complete = j.isComplete();
		error = j.hasError();
		results = j.getResults();
	}

	public FakeJob(final String id, final String user, final String service,
			final String stage, final String desc, final String progtype,
			final Integer prog, final Integer maxprog, final String status,
			final Boolean complete, final Boolean error,
			final Map<String, Object> results) {
		this.id = id;
		this.user = user;
		this.service = service;
		this.stage = stage;
		this.desc = desc;
		this.progtype = progtype;
		this.prog = prog;
		this.maxprog = maxprog;
		this.status = status;
		this.complete = complete;
		this.error = error;
		this.results = results;
	}

	public FakeJob(Tuple12<String, String, String, String, String, Integer,
			Integer, String, Integer, Integer, String, Results> ji) {
		this.user = null;
		this.id = ji.getE1();
		this.service = ji.getE2();
		this.stage = ji.getE3();
		this.status = ji.getE4();
		this.prog = ji.getE6();
		this.maxprog = ji.getE7();
		this.progtype = ji.getE8();
		this.complete = ji.getE9() != 0;
		this.error = ji.getE10() != 0;
		this.desc = ji.getE11();
		if (ji.getE12() == null) {
			this.results = null;
		} else {
			Results r = ji.getE12();
			Map<String, Object> res = new HashMap<String, Object>();
			res.put("shocknodes", r.getShocknodes());
			res.put("shockurl", r.getShockurl());
			res.put("workspaceids", r.getWorkspaceids());
			res.put("workspaceurl", r.getWorkspaceurl());
			this.results = res;
		}
	}
	
	public String getID() {
		return id;
	}

	@Override
	public String toString() {
		return "FakeJob [id=" + id + ", user=" + user + ", service=" + service
				+ ", stage=" + stage + ", desc=" + desc + ", progtype="
				+ progtype + ", prog=" + prog + ", maxprog=" + maxprog
				+ ", status=" + status + ", complete=" + complete + ", error="
				+ error + ", results=" + results + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((complete == null) ? 0 : complete.hashCode());
		result = prime * result + ((desc == null) ? 0 : desc.hashCode());
		result = prime * result + ((error == null) ? 0 : error.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((maxprog == null) ? 0 : maxprog.hashCode());
		result = prime * result + ((prog == null) ? 0 : prog.hashCode());
		result = prime * result
				+ ((progtype == null) ? 0 : progtype.hashCode());
		result = prime * result + ((results == null) ? 0 : results.hashCode());
		result = prime * result + ((service == null) ? 0 : service.hashCode());
		result = prime * result + ((stage == null) ? 0 : stage.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FakeJob)) {
			return false;
		}
		FakeJob other = (FakeJob) obj;
		if (complete == null) {
			if (other.complete != null) {
				return false;
			}
		} else if (!complete.equals(other.complete)) {
			return false;
		}
		if (desc == null) {
			if (other.desc != null) {
				return false;
			}
		} else if (!desc.equals(other.desc)) {
			return false;
		}
		if (error == null) {
			if (other.error != null) {
				return false;
			}
		} else if (!error.equals(other.error)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (maxprog == null) {
			if (other.maxprog != null) {
				return false;
			}
		} else if (!maxprog.equals(other.maxprog)) {
			return false;
		}
		if (prog == null) {
			if (other.prog != null) {
				return false;
			}
		} else if (!prog.equals(other.prog)) {
			return false;
		}
		if (progtype == null) {
			if (other.progtype != null) {
				return false;
			}
		} else if (!progtype.equals(other.progtype)) {
			return false;
		}
		if (results == null) {
			if (other.results != null) {
				return false;
			}
		} else if (!results.equals(other.results)) {
			return false;
		}
		if (service == null) {
			if (other.service != null) {
				return false;
			}
		} else if (!service.equals(other.service)) {
			return false;
		}
		if (stage == null) {
			if (other.stage != null) {
				return false;
			}
		} else if (!stage.equals(other.stage)) {
			return false;
		}
		if (status == null) {
			if (other.status != null) {
				return false;
			}
		} else if (!status.equals(other.status)) {
			return false;
		}
		if (user == null) {
			if (other.user != null) {
				return false;
			}
		} else if (!user.equals(other.user)) {
			return false;
		}
		return true;
	}
}
