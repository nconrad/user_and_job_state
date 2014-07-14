package us.kbase.userandjobstate.awe.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AweJobIO {
	
	private String name;
	private String host;
	private String node;
	private String url;
	private long size;
	private String origin;
	private Boolean nonzero;
	private String shockfilename;
	private String shockindex;
	private String attrfile;
//	private Boolean nofile;
//	private Boolean delete;
//	private String type;
	private Boolean temporary;
	
	private AweJobIO() {}

	public String getName() {
		return name;
	}
	
	public String getHost() {
		return host;
	}
	
	public String getNode() {
		return node;
	}
	
	public String getUrl() {
		return url;
	}
	
	public long getSize() {
		return size;
	}
	
	public String getOrigin() {
		return origin;
	}
	
	public boolean isNonzero() {
		return nonzero;
	}
	
	public String getShockfilename() {
		return shockfilename;
	}
	
	public String getShockindex() {
		return shockindex;
	}
	
	public String getAttrfile() {
		return attrfile;
	}
	
//	public boolean isNofile() {
//		return nofile;
//	}
//	
//	public boolean isDelete() {
//		return delete;
//	}
//	
//	public String getType() {
//		return type;
//	}
	
	public Boolean isTemporary() {
		return temporary;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AweJobIO [name=");
		builder.append(name);
		builder.append(", host=");
		builder.append(host);
		builder.append(", node=");
		builder.append(node);
		builder.append(", url=");
		builder.append(url);
		builder.append(", size=");
		builder.append(size);
		builder.append(", origin=");
		builder.append(origin);
		builder.append(", nonzero=");
		builder.append(nonzero);
		builder.append(", shockfilename=");
		builder.append(shockfilename);
		builder.append(", shockindex=");
		builder.append(shockindex);
		builder.append(", attrfile=");
		builder.append(attrfile);
//		builder.append(", nofile=");
//		builder.append(nofile);
//		builder.append(", delete=");
//		builder.append(delete);
//		builder.append(", type=");
//		builder.append(type);
		builder.append(", temporary=");
		builder.append(temporary);
		builder.append("]");
		return builder.toString();
	}
}
