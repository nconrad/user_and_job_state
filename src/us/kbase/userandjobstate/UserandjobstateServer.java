package us.kbase.userandjobstate;

import java.util.List;
import java.util.Map;
import us.kbase.JsonServerMethod;
import us.kbase.JsonServerServlet;
import us.kbase.Tuple11;
import us.kbase.Tuple4;
import us.kbase.Tuple5;
import us.kbase.auth.AuthToken;

//BEGIN_HEADER
//END_HEADER

/**
 * <p>Original spec-file module name: UserAndJobState</p>
 * <pre>
 * Service for storing arbitrary key value pairs on a per user per service basis
 * and storing job status so that a) long JSON RPC calls can report status and
 * UI elements can receive updates, and b) there's a centralized location for 
 * job status reporting.
 * The service assumes other services are capable of simple math and does not
 * throw errors if a progress bar overflows.
 * Since there is no way to authenticate as a service, devs are on the honor
 * system not to clobber each other's settings and jobs.
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
public class UserandjobstateServer extends JsonServerServlet {
    private static final long serialVersionUID = 1L;

    //BEGIN_CLASS_HEADER
    //END_CLASS_HEADER

    public UserandjobstateServer() throws Exception {
        super("UserAndJobState");
        //BEGIN_CONSTRUCTOR
        //END_CONSTRUCTOR
    }

    /**
     * <p>Original spec-file function name: set_state</p>
     * <pre>
     * Set the state of a key for a service.
     * </pre>
     * @param   service   Original type "service_name" (A service name.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.set_state")
    public void setState(String service, String key, String value, AuthToken authPart) throws Exception {
        //BEGIN set_state
        //END set_state
    }

    /**
     * <p>Original spec-file function name: get_state</p>
     * <pre>
     * Get the state of a key for a service.
     * </pre>
     * @param   service   Original type "service_name" (A service name.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.get_state")
    public String getState(String service, String key, AuthToken authPart) throws Exception {
        String returnVal = null;
        //BEGIN get_state
        //END get_state
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: remove_state</p>
     * <pre>
     * Remove a key value pair.
     * </pre>
     * @param   service   Original type "service_name" (A service name.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.remove_state")
    public void removeState(String service, String key, AuthToken authPart) throws Exception {
        //BEGIN remove_state
        //END remove_state
    }

    /**
     * <p>Original spec-file function name: list_state</p>
     * <pre>
     * List all key value pairs.
     * </pre>
     * @param   service   Original type "service_name" (A service name.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.list_state")
    public Map<String,String> listState(String service, AuthToken authPart) throws Exception {
        Map<String,String> returnVal = null;
        //BEGIN list_state
        //END list_state
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: create_job</p>
     * <pre>
     * Create a new job status report.
     * </pre>
     * @return   Original type "job_id" (A job id.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.create_job")
    public String createJob(AuthToken authPart) throws Exception {
        String returnVal = null;
        //BEGIN create_job
        //END create_job
        return returnVal;
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
     */
    @JsonServerMethod(rpc = "UserAndJobState.start_job")
    public void startJob(String job, String service, String status, String desc, InitProgress progress, AuthToken authPart) throws Exception {
        //BEGIN start_job
        //END start_job
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
     */
    @JsonServerMethod(rpc = "UserAndJobState.create_and_start_job")
    public String createAndStartJob(String service, String status, String desc, InitProgress progress, AuthToken authPart) throws Exception {
        String returnVal = null;
        //BEGIN create_and_start_job
        //END create_and_start_job
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: update_job_progress</p>
     * <pre>
     * Update the status and progress for a job.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   prog   Original type "progress" (The amount of progress the job has made since the last update, summed to the total progress so far.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.update_job_progress")
    public void updateJobProgress(String job, String status, Integer prog, AuthToken authPart) throws Exception {
        //BEGIN update_job_progress
        //END update_job_progress
    }

    /**
     * <p>Original spec-file function name: update_job</p>
     * <pre>
     * Update the status for a job.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.update_job")
    public void updateJob(String job, String status, AuthToken authPart) throws Exception {
        //BEGIN update_job
        //END update_job
    }

    /**
     * <p>Original spec-file function name: get_job_description</p>
     * <pre>
     * Get the description of a job.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.get_job_description", tuple = true)
    public Tuple4<String, String, Integer, String> getJobDescription(String job, AuthToken authPart) throws Exception {
        String return1 = null;
        String return2 = null;
        Integer return3 = null;
        String return4 = null;
        //BEGIN get_job_description
        //END get_job_description
        Tuple4<String, String, Integer, String> returnVal = new Tuple4<String, String, Integer, String>();
        returnVal.setE1(return1);
        returnVal.setE2(return2);
        returnVal.setE3(return3);
        returnVal.setE4(return4);
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: get_job_status</p>
     * <pre>
     * Get the status of a job. 
     * If the progress type is 'none' total_progress will always be 0.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.get_job_status", tuple = true)
    public Tuple5<String, String, Integer, Integer, Integer> getJobStatus(String job, AuthToken authPart) throws Exception {
        String return1 = null;
        String return2 = null;
        Integer return3 = null;
        Integer return4 = null;
        Integer return5 = null;
        //BEGIN get_job_status
        //END get_job_status
        Tuple5<String, String, Integer, Integer, Integer> returnVal = new Tuple5<String, String, Integer, Integer, Integer>();
        returnVal.setE1(return1);
        returnVal.setE2(return2);
        returnVal.setE3(return3);
        returnVal.setE4(return4);
        returnVal.setE5(return5);
        return returnVal;
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
     */
    @JsonServerMethod(rpc = "UserAndJobState.complete_job")
    public void completeJob(String job, String status, Integer error, Results res, AuthToken authPart) throws Exception {
        //BEGIN complete_job
        //END complete_job
    }

    /**
     * <p>Original spec-file function name: get_results</p>
     * <pre>
     * Get the job results.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @return   Original type "Results" (see {@link us.kbase.userandjobstate.Results Results} for details)
     */
    @JsonServerMethod(rpc = "UserAndJobState.get_results")
    public Results getResults(String job, AuthToken authPart) throws Exception {
        Results returnVal = null;
        //BEGIN get_results
        //END get_results
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: get_services</p>
     * <pre>
     * List service names.
     * </pre>
     */
    @JsonServerMethod(rpc = "UserAndJobState.get_services")
    public List<String> getServices(AuthToken authPart) throws Exception {
        List<String> returnVal = null;
        //BEGIN get_services
        //END get_services
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: get_job_info</p>
     * <pre>
     * Get information about a job.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @return   Original type "job_info" (Information about a job. Note calls returning this structure will probably be slower than the more targeted calls.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.get_job_info")
    public Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results> getJobInfo(String job, AuthToken authPart) throws Exception {
        Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results> returnVal = null;
        //BEGIN get_job_info
        //END get_job_info
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: list_jobs</p>
     * <pre>
     * List jobs.
     * </pre>
     * @param   service   Original type "service_name" (A service name.)
     * @param   options   Original type "ListJobsOptions" (see {@link us.kbase.userandjobstate.ListJobsOptions ListJobsOptions} for details)
     */
    @JsonServerMethod(rpc = "UserAndJobState.list_jobs")
    public List<Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results>> listJobs(String service, ListJobsOptions options, AuthToken authPart) throws Exception {
        List<Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results>> returnVal = null;
        //BEGIN list_jobs
        //END list_jobs
        return returnVal;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: <program> <server_port>");
            return;
        }
        new UserandjobstateServer().startupServer(Integer.parseInt(args[0]));
    }
}
