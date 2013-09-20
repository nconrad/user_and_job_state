package us.kbase.userandjobstate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.type.TypeReference;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.Tuple11;
import us.kbase.common.service.Tuple4;
import us.kbase.common.service.Tuple5;
import us.kbase.common.service.UObject;

/**
 * <p>Original spec-file module name: UserAndJobState</p>
 * <pre>
 * Service for storing arbitrary key/object pairs on a per user per service basis
 * and storing job status so that a) long JSON RPC calls can report status and
 * UI elements can receive updates, and b) there's a centralized location for 
 * job status reporting.
 * The service assumes other services are capable of simple math and does not
 * throw errors if a progress bar overflows.
 * Currently devs are on the honor system not to clobber each other's settings and
 * jobs. If necessary, per Steve Chan we could set up service authentication by
 * passing a token in the arguments.
 * Setting objects are limited to 1Mb.
 * Potential process flows:
 * Asysnc:
 * UI calls service function which returns with job id
 * service call [spawns thread/subprocess to run job that] periodically updates
 *         the job status of the job id on the job status server
 * meanwhile, the UI periodically polls the job status server to get progress
 *         updates
 * service call finishes, completes job
 * UI pulls pointers to results from the job status server
 * Sync:
 * UI creates job, gets job id
 * UI starts thread that calls service, providing job id
 * service call runs, periodically updating the job status of the job id on the
 *         job status server
 * meanwhile, the UI periodically polls the job status server to get progress
 *         updates
 * service call finishes, completes job, returns results
 * UI thread joins
 * mongodb structures:
 * State collection:
 * {
 *         _id:
 *         user:
 *         service:
 *         key: (unique index on user/service/key)
 *         value:
 * }
 * Job collection:
 * {
 *         _id:
 *         user:
 *         service:
 *         desc:
 *         progtype: ('percent', 'task', 'none')
 *         prog: (int)
 *         maxprog: (int, 100 for percent, user specified for task)
 *         status: (user supplied string)
 *         updated: (date)
 *         complete: (bool) (index on user/service/complete)
 *         error: (bool)
 *         result: {
 *                 shocknodes: (list of strings)
 *                 shockurl:
 *                 workspaceids: (list of strings)
 *                 workspaceurl:
 *         }
 * }
 * </pre>
 */
public class UserAndJobStateClient {
    private JsonClientCaller caller;
    private static URL DEFAULT_URL = null;
    static {
        try {
            DEFAULT_URL = new URL("http://kbase.us/services/userandjobstate/");
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Compile error in client - bad url compiled");
        }
    }

    public UserAndJobStateClient() {
       caller = new JsonClientCaller(DEFAULT_URL);
    }

    public UserAndJobStateClient(URL url) {
        caller = new JsonClientCaller(url);
    }

    public UserAndJobStateClient(URL url, AuthToken token) {
        caller = new JsonClientCaller(url, token);
    }

    public UserAndJobStateClient(URL url, String user, String password) {
        caller = new JsonClientCaller(url, user, password);
    }

    public UserAndJobStateClient(AuthToken token) {
        caller = new JsonClientCaller(DEFAULT_URL, token);
    }

    public UserAndJobStateClient(String user, String password) {
        caller = new JsonClientCaller(DEFAULT_URL, user, password);
    }

    public boolean isAuthAllowedForHttp() {
        return caller.isAuthAllowedForHttp();
    }

    public void setAuthAllowedForHttp(boolean isAuthAllowedForHttp) {
        caller.setAuthAllowedForHttp(isAuthAllowedForHttp);
    }

    /**
     * <p>Original spec-file function name: set_state</p>
     * <pre>
     * Set the state of a key for a service.
     * </pre>
     * @param   service   Original type "service_name" (A service name.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void setState(String service, String key, UObject value) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        args.add(key);
        args.add(value);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.set_state", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: get_state</p>
     * <pre>
     * Get the state of a key for a service.
     * </pre>
     * @param   service   Original type "service_name" (A service name.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public UObject getState(String service, String key) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        args.add(key);
        TypeReference<List<UObject>> retType = new TypeReference<List<UObject>>() {};
        List<UObject> res = caller.jsonrpcCall("UserAndJobState.get_state", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: remove_state</p>
     * <pre>
     * Remove a key value pair.
     * </pre>
     * @param   service   Original type "service_name" (A service name.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void removeState(String service, String key) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        args.add(key);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.remove_state", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: list_state</p>
     * <pre>
     * List all keys.
     * </pre>
     * @param   service   Original type "service_name" (A service name.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public List<String> listState(String service) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        TypeReference<List<List<String>>> retType = new TypeReference<List<List<String>>>() {};
        List<List<String>> res = caller.jsonrpcCall("UserAndJobState.list_state", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: create_job</p>
     * <pre>
     * Create a new job status report.
     * </pre>
     * @return   Original type "job_id" (A job id.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String createJob() throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("UserAndJobState.create_job", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: start_job</p>
     * <pre>
     * Start a job and specify the job parameters.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @param   service   Original type "service_name" (A service name.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   desc   Original type "job_description" (A job description string supplied by the reporting service. No more than 1000 characters.)
     * @param   progress   Original type "InitProgress" (see {@link us.kbase.userandjobstate.InitProgress InitProgress} for details)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void startJob(String job, String service, String status, String desc, InitProgress progress) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        args.add(service);
        args.add(status);
        args.add(desc);
        args.add(progress);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.start_job", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: create_and_start_job</p>
     * <pre>
     * Create and start a job.
     * </pre>
     * @param   service   Original type "service_name" (A service name.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   desc   Original type "job_description" (A job description string supplied by the reporting service. No more than 1000 characters.)
     * @param   progress   Original type "InitProgress" (see {@link us.kbase.userandjobstate.InitProgress InitProgress} for details)
     * @return   Original type "job_id" (A job id.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String createAndStartJob(String service, String status, String desc, InitProgress progress) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        args.add(status);
        args.add(desc);
        args.add(progress);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("UserAndJobState.create_and_start_job", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: update_job_progress</p>
     * <pre>
     * Update the status and progress for a job.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   prog   Original type "progress" (The amount of progress the job has made since the last update, summed to the total progress so far.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void updateJobProgress(String job, String status, Integer prog) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        args.add(status);
        args.add(prog);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.update_job_progress", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: update_job</p>
     * <pre>
     * Update the status for a job.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void updateJob(String job, String status) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        args.add(status);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.update_job", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: get_job_description</p>
     * <pre>
     * Get the description of a job.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Tuple4<String, String, Integer, String> getJobDescription(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<Tuple4<String, String, Integer, String>> retType = new TypeReference<Tuple4<String, String, Integer, String>>() {};
        Tuple4<String, String, Integer, String> res = caller.jsonrpcCall("UserAndJobState.get_job_description", args, retType, true, true);
        return res;
    }

    /**
     * <p>Original spec-file function name: get_job_status</p>
     * <pre>
     * Get the status of a job. 
     * If the progress type is 'none' total_progress will always be 0.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Tuple5<String, String, Integer, Integer, Integer> getJobStatus(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<Tuple5<String, String, Integer, Integer, Integer>> retType = new TypeReference<Tuple5<String, String, Integer, Integer, Integer>>() {};
        Tuple5<String, String, Integer, Integer, Integer> res = caller.jsonrpcCall("UserAndJobState.get_job_status", args, retType, true, true);
        return res;
    }

    /**
     * <p>Original spec-file function name: complete_job</p>
     * <pre>
     * Complete the job. After the job is completed, total_progress always
     * equals max_progress.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   error   Original type "boolean" (A boolean. 0 = false, other = true.)
     * @param   res   Original type "Results" (see {@link us.kbase.userandjobstate.Results Results} for details)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void completeJob(String job, String status, Integer error, Results res) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        args.add(status);
        args.add(error);
        args.add(res);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.complete_job", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: get_results</p>
     * <pre>
     * Get the job results.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @return   Original type "Results" (see {@link us.kbase.userandjobstate.Results Results} for details)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Results getResults(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<List<Results>> retType = new TypeReference<List<Results>>() {};
        List<Results> res = caller.jsonrpcCall("UserAndJobState.get_results", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: get_services</p>
     * <pre>
     * List service names.
     * </pre>
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public List<String> getServices() throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<List<String>>> retType = new TypeReference<List<List<String>>>() {};
        List<List<String>> res = caller.jsonrpcCall("UserAndJobState.get_services", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: get_job_info</p>
     * <pre>
     * Get information about a job.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @return   Original type "job_info" (Information about a job. Note calls returning this structure will probably be slower than the more targeted calls.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results> getJobInfo(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<List<Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results>>> retType = new TypeReference<List<Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results>>>() {};
        List<Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results>> res = caller.jsonrpcCall("UserAndJobState.get_job_info", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: list_jobs</p>
     * <pre>
     * List jobs.
     * </pre>
     * @param   service   Original type "service_name" (A service name.)
     * @param   options   Original type "ListJobsOptions" (see {@link us.kbase.userandjobstate.ListJobsOptions ListJobsOptions} for details)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public List<Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results>> listJobs(String service, ListJobsOptions options) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        args.add(options);
        TypeReference<List<List<Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results>>>> retType = new TypeReference<List<List<Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results>>>>() {};
        List<List<Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results>>> res = caller.jsonrpcCall("UserAndJobState.list_jobs", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: delete_job</p>
     * <pre>
     * Delete a job. Will error out if the job is not complete.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void deleteJob(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.delete_job", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: force_delete_job</p>
     * <pre>
     * Force delete a job - will always succeed, regardless of job state.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void forceDeleteJob(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.force_delete_job", args, retType, false, true);
    }
}
