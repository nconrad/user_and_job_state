
package us.kbase.userandjobstate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;


/**
 * <p>Original spec-file type: Results</p>
 * <pre>
 * A pointer to job results. All arguments are optional. Applications
 * should use the default shock and workspace urls if omitted.
 * list<string> shocknodes - the shocknode(s) where the results can be
 *         found.
 * string shockurl - the url of the shock service where the data was
 *         saved.
 * list<string> workspaceids - the workspace ids where the results can be
 *         found.
 * string workspaceurl - the url of the workspace service where the data
 *         was saved.
 * </pre>
 * 
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "shocknodes",
    "shockurl",
    "workspaceids",
    "workspaceurl"
})
public class Results {

    @JsonProperty("shocknodes")
    private List<String> shocknodes = new ArrayList<String>();
    @JsonProperty("shockurl")
    private String shockurl;
    @JsonProperty("workspaceids")
    private List<String> workspaceids = new ArrayList<String>();
    @JsonProperty("workspaceurl")
    private String workspaceurl;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("shocknodes")
    public List<String> getShocknodes() {
        return shocknodes;
    }

    @JsonProperty("shocknodes")
    public void setShocknodes(List<String> shocknodes) {
        this.shocknodes = shocknodes;
    }

    public Results withShocknodes(List<String> shocknodes) {
        this.shocknodes = shocknodes;
        return this;
    }

    @JsonProperty("shockurl")
    public String getShockurl() {
        return shockurl;
    }

    @JsonProperty("shockurl")
    public void setShockurl(String shockurl) {
        this.shockurl = shockurl;
    }

    public Results withShockurl(String shockurl) {
        this.shockurl = shockurl;
        return this;
    }

    @JsonProperty("workspaceids")
    public List<String> getWorkspaceids() {
        return workspaceids;
    }

    @JsonProperty("workspaceids")
    public void setWorkspaceids(List<String> workspaceids) {
        this.workspaceids = workspaceids;
    }

    public Results withWorkspaceids(List<String> workspaceids) {
        this.workspaceids = workspaceids;
        return this;
    }

    @JsonProperty("workspaceurl")
    public String getWorkspaceurl() {
        return workspaceurl;
    }

    @JsonProperty("workspaceurl")
    public void setWorkspaceurl(String workspaceurl) {
        this.workspaceurl = workspaceurl;
    }

    public Results withWorkspaceurl(String workspaceurl) {
        this.workspaceurl = workspaceurl;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
