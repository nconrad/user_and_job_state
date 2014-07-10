package us.kbase.userandjobstate.awe.client;

import java.util.List;

public class AweJob {
	
	
	//TODO lastfailed
	
		private AweJobId id;
		private String jid;
		private AweJobInfo info;
		private List<AweJobTask> tasks;
		private String state;
		private Boolean registered;
		private Integer remaintasks;
		private String updatetime;
		private String notes;
		private Integer resumed;
		
		public AweJobId getId() {
			return id;
		}
	
		
		public String getJid() {
			return jid;
		}
		
		public AweJobInfo getInfo() {
			return info;
		}
		
		public List<AweJobTask> getTasks() {
			return tasks;
		}
		
		public String getState() {
			return state;
		}
		
		public Boolean getRegistered() {
			return registered;
		}
		
		
		public Integer getRemaintasks() {
			return remaintasks;
		}
		
		public String getUpdatetime() {
			return updatetime;
		}
		
		public String getNotes() {
			return notes;
		}
		
		public Integer getResumed() {
			return resumed;
		}
}
