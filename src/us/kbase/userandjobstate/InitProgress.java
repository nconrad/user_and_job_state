
package us.kbase.userandjobstate;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;


/**
 * <p>Original spec-file type: InitProgress</p>
 * <pre>
 * Initialization information for progress tracking. Currently 3 choices:
 * progress_type ptype - one of 'none', 'percent', or 'task'
 * max_progress max- required only for task based tracking. The 
 *         total number of tasks until the job is complete.
 * </pre>
 * 
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "ptype",
    "max"
})
public class InitProgress {

    @JsonProperty("ptype")
    private String ptype;
    @JsonProperty("max")
    private Integer max;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("ptype")
    public String getPtype() {
        return ptype;
    }

    @JsonProperty("ptype")
    public void setPtype(String ptype) {
        this.ptype = ptype;
    }

    public InitProgress withPtype(String ptype) {
        this.ptype = ptype;
        return this;
    }

    @JsonProperty("max")
    public Integer getMax() {
        return max;
    }

    @JsonProperty("max")
    public void setMax(Integer max) {
        this.max = max;
    }

    public InitProgress withMax(Integer max) {
        this.max = max;
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
