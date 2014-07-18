package us.kbase.userandjobstate.awe.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AweJob extends AweData {

	//TODO lastfailed

	private AweJobId id;
//	private String jid;
	private AweJobInfo info;
	private List<AweJobTask> tasks;
	private String state;
//	private Boolean registered;
	private Integer remaintasks;
	private String updatetime;
	private String notes;
//	private Integer resumed;
	
	private AweJob() {};

	public AweJobId getId() {
		return id;
	}

//	public String getJid() {
//		return jid;
//	}

	public AweJobInfo getInfo() {
		return info;
	}

	public List<AweJobTask> getTasks() {
		return tasks;
	}

	public String getState() {
		return state;
	}

//	public Boolean getRegistered() {
//		return registered;
//	}


	public Integer getRemaintasks() {
		return remaintasks;
	}

	public String getUpdatetime() {
		return updatetime;
	}

	public String getNotes() {
		return notes;
	}

//	public Integer getResumed() {
//		return resumed;
//	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AweJob [id=");
		builder.append(id);
//		builder.append(", jid=");
//		builder.append(jid);
		builder.append(", info=");
		builder.append(info);
		builder.append(", tasks=");
		builder.append(tasks);
		builder.append(", state=");
		builder.append(state);
//		builder.append(", registered=");
//		builder.append(registered);
		builder.append(", remaintasks=");
		builder.append(remaintasks);
		builder.append(", updatetime=");
		builder.append(updatetime);
		builder.append(", notes=");
		builder.append(notes);
//		builder.append(", resumed=");
//		builder.append(resumed);
		builder.append("]");
		return builder.toString();
	}
}
