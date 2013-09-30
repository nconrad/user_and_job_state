
package us.kbase.userandjobstate;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * <p>Original spec-file type: ListJobsOptions</p>
 * <pre>
 * Options for list_jobs command. 
 * boolean oldest_first - return jobs with an ascending sort based on the
 *         creation date.
 * int limit - limit the results to X jobs.
 * int offset - skip the first X jobs.
 * boolean completed - true to return only completed jobs, false to
 *         return only incomplete jobs.
 * boolean error_only - true to return only jobs that errored out. 
 *         Overrides the completed option.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "oldest_first",
    "limit",
    "offset",
    "completed",
    "error_only"
})
public class ListJobsOptions {

    @JsonProperty("oldest_first")
    private Integer oldestFirst;
    @JsonProperty("limit")
    private Integer limit;
    @JsonProperty("offset")
    private Integer offset;
    @JsonProperty("completed")
    private Integer completed;
    @JsonProperty("error_only")
    private Integer errorOnly;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("oldest_first")
    public Integer getOldestFirst() {
        return oldestFirst;
    }

    @JsonProperty("oldest_first")
    public void setOldestFirst(Integer oldestFirst) {
        this.oldestFirst = oldestFirst;
    }

    public ListJobsOptions withOldestFirst(Integer oldestFirst) {
        this.oldestFirst = oldestFirst;
        return this;
    }

    @JsonProperty("limit")
    public Integer getLimit() {
        return limit;
    }

    @JsonProperty("limit")
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public ListJobsOptions withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    @JsonProperty("offset")
    public Integer getOffset() {
        return offset;
    }

    @JsonProperty("offset")
    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public ListJobsOptions withOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    @JsonProperty("completed")
    public Integer getCompleted() {
        return completed;
    }

    @JsonProperty("completed")
    public void setCompleted(Integer completed) {
        this.completed = completed;
    }

    public ListJobsOptions withCompleted(Integer completed) {
        this.completed = completed;
        return this;
    }

    @JsonProperty("error_only")
    public Integer getErrorOnly() {
        return errorOnly;
    }

    @JsonProperty("error_only")
    public void setErrorOnly(Integer errorOnly) {
        this.errorOnly = errorOnly;
    }

    public ListJobsOptions withErrorOnly(Integer errorOnly) {
        this.errorOnly = errorOnly;
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
