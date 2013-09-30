package us.kbase.userandjobstate;

import java.util.List;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonServerMethod;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.Tuple11;
import us.kbase.common.service.Tuple4;
import us.kbase.common.service.Tuple5;
import us.kbase.common.service.UObject;

//BEGIN_HEADER
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import us.kbase.auth.AuthService;
import us.kbase.auth.TokenExpiredException;
import us.kbase.auth.TokenFormatException;
import us.kbase.common.mongo.exceptions.InvalidHostException;
import us.kbase.common.mongo.exceptions.MongoAuthException;
import us.kbase.userandjobstate.userstate.UserState;
//END_HEADER

/**
 * <p>Original spec-file module name: UserAndJobState</p>
 * <pre>
 * Service for storing arbitrary key/object pairs on a per user per service basis
 * and storing job status so that a) long JSON RPC calls can report status and
 * UI elements can receive updates, and b) there's a centralized location for 
 * job status reporting.
 * The service assumes other services are capable of simple math and does not
 * throw errors if a progress bar overflows.
 * Setting objects are limited to 1Mb.
 * There are two modes of operation for setting key values for a user: 
 * 1) no service authentication - an authorization token for a service is not 
 *         required, and any service with the user token can write to any other
 *         service's unauthed values for that user.
 * 2) service authentication required - the service must pass a Globus Online
 *         token that identifies the service in the argument list. Values can only be
 *         set by services with possession of a valid token. The service name in
 *         method returns will be set to the username of the token.
 * The sets of key/value pairs for the two types of method calls are entirely
 * separate - for example, the workspace service could have a key called 'default'
 * that is writable by all other services (no auth) and the same key that was 
 * set with auth to which only the workspace service can write (or any other
 * service that has access to a workspace service account token, so keep your
 * service credentials safe).
 * All job writes require service authentication. No reads, either for key/value
 * pairs or jobs, require service authentication.
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
 *         auth: (bool)
 *         key: (unique index on user/service/auth/key)
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
public class UserAndJobStateServer extends JsonServerServlet {
    private static final long serialVersionUID = 1L;

    //BEGIN_CLASS_HEADER
	//required deploy parameters:
	private static final String HOST = "mongodb-host";
	private static final String DB = "mongodb-database";
	//auth params:
	private static final String USER = "mongodb-user";
	private static final String PWD = "mongodb-pwd";
	
	private static Map<String, String> ujConfig = null;
	
	private static final String USER_COLLECTION = "userstate";
	
	private final UserState us;
	
	private UserState getUserState(final String host, final String dbs,
			final String user, final String pwd) {
		try {
			if (user != null) {
				return new UserState(host, dbs, USER_COLLECTION, user, pwd);
			} else {
				return new UserState(host, dbs, USER_COLLECTION);
			}
		} catch (UnknownHostException uhe) {
			fail("Couldn't find mongo host " + host + ": " +
					uhe.getLocalizedMessage());
		} catch (IOException io) {
			fail("Couldn't connect to mongo host " + host + ": " +
					io.getLocalizedMessage());
		} catch (MongoAuthException ae) {
			fail("Not authorized: " + ae.getLocalizedMessage());
		} catch (InvalidHostException ihe) {
			fail(host + " is an invalid database host: "  +
					ihe.getLocalizedMessage());
		}
		return null;
	}
	
	private void fail(final String error) {
		logErr(error);
		System.err.println(error);
		startupFailed();
	}
	
	private String getServiceName(String serviceToken)
			throws TokenFormatException, TokenExpiredException, IOException {
		if (serviceToken == null || serviceToken.isEmpty()) {
			throw new IllegalArgumentException(
					"Service token cannot be null or the empty string");
		}
		final AuthToken t = new AuthToken(serviceToken);
		if (!AuthService.validateToken(t)) {
			throw new IllegalArgumentException("Service token is invalid");
		}
		return t.getUserName();
	}
    //END_CLASS_HEADER

    public UserAndJobStateServer() throws Exception {
        super("UserAndJobState");
        //BEGIN_CONSTRUCTOR
		//assign config once per jvm, otherwise you could wind up with
		//different threads talking to different mongo instances
		//E.g. first thread's config applies to all threads.
		if (ujConfig == null) {
			ujConfig = new HashMap<String, String>();
			ujConfig.putAll(super.config);
		}
		boolean failed = false;
		if (!ujConfig.containsKey(HOST)) {
			fail("Must provide param " + HOST + " in config file");
			failed = true;
		}
		final String host = ujConfig.get(HOST);
		if (!ujConfig.containsKey(DB)) {
			fail("Must provide param " + DB + " in config file");
			failed = true;
		}
		final String dbs = ujConfig.get(DB);
		if (ujConfig.containsKey(USER) ^ ujConfig.containsKey(PWD)) {
			fail(String.format("Must provide both %s and %s ",
					USER, PWD) + "params in config file if authentication " + 
					"is to be used");
			failed = true;
		}
		if (failed) {
			fail("Server startup failed - all calls will error out.");
			us = null;
		} else {
			final String user = ujConfig.get(USER);
			final String pwd = ujConfig.get(PWD);
			String params = "";
			for (String s: Arrays.asList(HOST, DB, USER)) {
				if (ujConfig.containsKey(s)) {
					params += s + "=" + ujConfig.get(s) + "\n";
				}
			}
			if (pwd != null) {
				params += PWD + "=[redacted for your safety and comfort]\n";
			}
			System.out.println("Using connection parameters:\n" + params);
			logInfo("Using connection parameters:\n" + params);
			us = getUserState(host, dbs, user, pwd);
		}
        //END_CONSTRUCTOR
    }

    /**
     * <p>Original spec-file function name: set_state</p>
     * <pre>
     * Set the state of a key for a service without service authentication.
     * </pre>
     * @param   service   Original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.set_state")
    public void setState(String service, String key, UObject value, AuthToken authPart) throws Exception {
        //BEGIN set_state
		us.setState(authPart.getUserName(), service, false, key,
				value == null ? null : value.asClassInstance(Object.class));
        //END set_state
    }

    /**
     * <p>Original spec-file function name: set_state_auth</p>
     * <pre>
     * Set the state of a key for a service with service authentication.
     * </pre>
     * @param   token   Original type "service_token" (A globus ID token that validates that the service really is said service.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.set_state_auth")
    public void setStateAuth(String token, String key, UObject value, AuthToken authPart) throws Exception {
        //BEGIN set_state_auth
		us.setState(authPart.getUserName(), getServiceName(token), true, key,
				value == null ? null : value.asClassInstance(Object.class));
        //END set_state_auth
    }

    /**
     * <p>Original spec-file function name: get_state</p>
     * <pre>
     * Get the state of a key for a service.
     * </pre>
     * @param   service   Original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @param   auth   Original type "authed" (Specifies whether results returned should be from key/value pairs set with service authentication (true) or without (false).) &rarr; Original type "boolean" (A boolean. 0 = false, other = true.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.get_state")
    public UObject getState(String service, String key, Integer auth, AuthToken authPart) throws Exception {
        UObject returnVal = null;
        //BEGIN get_state
		returnVal = new UObject(us.getState(authPart.getUserName(), service,
				auth != 0, key));
        //END get_state
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: remove_state</p>
     * <pre>
     * Remove a key value pair without service authentication.
     * </pre>
     * @param   service   Original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.remove_state")
    public void removeState(String service, String key, AuthToken authPart) throws Exception {
        //BEGIN remove_state
		us.removeState(authPart.getUserName(), service, false, key);
        //END remove_state
    }

    /**
     * <p>Original spec-file function name: remove_state_auth</p>
     * <pre>
     * Remove a key value pair with service authentication.
     * </pre>
     * @param   token   Original type "service_token" (A globus ID token that validates that the service really is said service.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.remove_state_auth")
    public void removeStateAuth(String token, String key, AuthToken authPart) throws Exception {
        //BEGIN remove_state_auth
		us.removeState(authPart.getUserName(), getServiceName(token), true,
				key);	
        //END remove_state_auth
    }

    /**
     * <p>Original spec-file function name: list_state</p>
     * <pre>
     * List all keys.
     * </pre>
     * @param   service   Original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @param   auth   Original type "authed" (Specifies whether results returned should be from key/value pairs set with service authentication (true) or without (false).) &rarr; Original type "boolean" (A boolean. 0 = false, other = true.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.list_state")
    public List<String> listState(String service, Integer auth, AuthToken authPart) throws Exception {
        List<String> returnVal = null;
        //BEGIN list_state
		returnVal = us.listState(authPart.getUserName(), service, auth != 0);
        //END list_state
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: list_state_services</p>
     * <pre>
     * List all state services.
     * </pre>
     * @param   auth   Original type "authed" (Specifies whether results returned should be from key/value pairs set with service authentication (true) or without (false).) &rarr; Original type "boolean" (A boolean. 0 = false, other = true.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.list_state_services")
    public List<String> listStateServices(Integer auth, AuthToken authPart) throws Exception {
        List<String> returnVal = null;
        //BEGIN list_state_services
		returnVal = us.listServices(authPart.getUserName(), auth != 0);
        //END list_state_services
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
     * @param   token   Original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   desc   Original type "job_description" (A job description string supplied by the reporting service. No more than 1000 characters.)
     * @param   progress   Original type "InitProgress" (see {@link us.kbase.userandjobstate.InitProgress InitProgress} for details)
     */
    @JsonServerMethod(rpc = "UserAndJobState.start_job")
    public void startJob(String job, String token, String status, String desc, InitProgress progress, AuthToken authPart) throws Exception {
        //BEGIN start_job
        //END start_job
    }

    /**
     * <p>Original spec-file function name: create_and_start_job</p>
     * <pre>
     * Create and start a job.
     * </pre>
     * @param   token   Original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   desc   Original type "job_description" (A job description string supplied by the reporting service. No more than 1000 characters.)
     * @param   progress   Original type "InitProgress" (see {@link us.kbase.userandjobstate.InitProgress InitProgress} for details)
     * @return   Original type "job_id" (A job id.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.create_and_start_job")
    public String createAndStartJob(String token, String status, String desc, InitProgress progress, AuthToken authPart) throws Exception {
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
     * @param   token   Original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   prog   Original type "progress" (The amount of progress the job has made since the last update, summed to the total progress so far.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.update_job_progress")
    public void updateJobProgress(String job, String token, String status, Integer prog, AuthToken authPart) throws Exception {
        //BEGIN update_job_progress
        //END update_job_progress
    }

    /**
     * <p>Original spec-file function name: update_job</p>
     * <pre>
     * Update the status for a job.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     * @param   token   Original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.update_job")
    public void updateJob(String job, String token, String status, AuthToken authPart) throws Exception {
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
     * @param   token   Original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   status   Original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   error   Original type "boolean" (A boolean. 0 = false, other = true.)
     * @param   res   Original type "Results" (see {@link us.kbase.userandjobstate.Results Results} for details)
     */
    @JsonServerMethod(rpc = "UserAndJobState.complete_job")
    public void completeJob(String job, String token, String status, Integer error, Results res, AuthToken authPart) throws Exception {
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
     * @param   service   Original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @param   options   Original type "ListJobsOptions" (see {@link us.kbase.userandjobstate.ListJobsOptions ListJobsOptions} for details)
     */
    @JsonServerMethod(rpc = "UserAndJobState.list_jobs")
    public List<Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results>> listJobs(String service, ListJobsOptions options, AuthToken authPart) throws Exception {
        List<Tuple11<String, String, String, String, Integer, Integer, String, Integer, Integer, String, Results>> returnVal = null;
        //BEGIN list_jobs
        //END list_jobs
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: list_job_services</p>
     * <pre>
     * List all job services.
     * </pre>
     * @param   auth   Original type "authed" (Specifies whether results returned should be from key/value pairs set with service authentication (true) or without (false).) &rarr; Original type "boolean" (A boolean. 0 = false, other = true.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.list_job_services")
    public List<String> listJobServices(Integer auth, AuthToken authPart) throws Exception {
        List<String> returnVal = null;
        //BEGIN list_job_services
        //END list_job_services
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: delete_job</p>
     * <pre>
     * Delete a job. Will error out if the job is not complete.
     * </pre>
     * @param   job   Original type "job_id" (A job id.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.delete_job")
    public void deleteJob(String job, AuthToken authPart) throws Exception {
        //BEGIN delete_job
        //END delete_job
    }

    /**
     * <p>Original spec-file function name: force_delete_job</p>
     * <pre>
     * Force delete a job - will always succeed, regardless of job state.
     * </pre>
     * @param   token   Original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   job   Original type "job_id" (A job id.)
     */
    @JsonServerMethod(rpc = "UserAndJobState.force_delete_job")
    public void forceDeleteJob(String token, String job, AuthToken authPart) throws Exception {
        //BEGIN force_delete_job
        //END force_delete_job
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: <program> <server_port>");
            return;
        }
        new UserAndJobStateServer().startupServer(Integer.parseInt(args[0]));
    }
}
