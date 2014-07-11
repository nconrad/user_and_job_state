package us.kbase.userandjobstate.awe.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AweIO {
	
	private String name;
	private String host;
	private String node;
	private String url;
	private int size;
	private String origin;
	private boolean nonzero;
	private String shockfilename;
	private String shockindex;
	private String attrfile;
	private boolean nofile;
	private boolean delete;
	private String type;
	private boolean formoptions;
	private Boolean temporary; //TODO 1 test temporary w/ results object
	
	private AweIO() {}

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
	
	public int getSize() {
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
	
	public boolean isNofile() {
		return nofile;
	}
	
	public boolean isDelete() {
		return delete;
	}
	
	public String getType() {
		return type;
	}
	
	public boolean isFormoptions() {
		return formoptions;
	}
	
	public Boolean isTemporary() {
		return temporary;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AweIO [name=");
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
		builder.append(", nofile=");
		builder.append(nofile);
		builder.append(", delete=");
		builder.append(delete);
		builder.append(", type=");
		builder.append(type);
		builder.append(", formoptions=");
		builder.append(formoptions);
		builder.append("]");
		return builder.toString();
	}
}
