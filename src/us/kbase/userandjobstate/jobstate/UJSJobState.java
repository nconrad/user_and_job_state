package us.kbase.userandjobstate.jobstate;

import static us.kbase.common.utils.StringUtils.checkString;
import static us.kbase.common.utils.StringUtils.checkMaxLen;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.jongo.Jongo;
import org.jongo.MongoCollection;

import us.kbase.common.mongo.GetMongoDB;
import us.kbase.common.mongo.exceptions.InvalidHostException;
import us.kbase.common.mongo.exceptions.MongoAuthException;
import us.kbase.userandjobstate.exceptions.CommunicationException;
import us.kbase.userandjobstate.jobstate.exceptions.NoSuchJobException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class UJSJobState implements JobState {

//	private final static int JOB_EXPIRES = 180 * 24 * 60 * 60; // 180 days
	
	private final static int MAX_LEN_USER = 100;
	private final static int MAX_LEN_SERVICE = 100;
	private final static int MAX_LEN_STATUS = 200;
	private final static int MAX_LEN_DESC = 1000;
	private final static int MAX_LEN_ERR = 100000;
	
	private final static String CREATED = "created";
	private final static String USER = "user";
	private final static String SERVICE = "service";
	private final static String STARTED = "started";
	private final static String UPDATED = "updated";
	private final static String EST_COMP = "estcompl";
	private final static String COMPLETE = "complete";
	private final static String ERROR = "error";
	private final static String ERROR_MSG = "errormsg";
	private final static String DESCRIPTION = "desc";
	private final static String PROG_TYPE = "progtype";
	private final static String PROG = "prog";
	private final static String MAXPROG = "maxprog";
	private final static String STATUS = "status";
	private final static String RESULT = "results";
	private final static String SHARED = "shared";
	
	private final static String MONGO_ID = "_id";
	
	private final DBCollection jobcol;
	private final MongoCollection jobjong;
	
	public UJSJobState(final String host, final String database,
			final String collection, final int mongoReconnectRetry)
			throws UnknownHostException, IOException, InvalidHostException,
			InterruptedException {
		final DB m = GetMongoDB.getDB(host, database, mongoReconnectRetry, 10);
		jobcol = m.getCollection(collection);
		jobjong = new Jongo(m).getCollection(collection);
		ensureIndexes();
	}

	public UJSJobState(final String host, final String database,
			final String collection, final String user, final String password,
			final int mongoReconnectRetry)
			throws UnknownHostException, IOException, InvalidHostException,
			MongoAuthException, InterruptedException {
		final DB m = GetMongoDB.getDB(host, database, user, password,
				mongoReconnectRetry, 10);
		jobcol = m.getCollection(collection);
		jobjong = new Jongo(m).getCollection(collection);
		ensureIndexes();
	}

	private void ensureIndexes() {
		ensureUserIndex(USER);
		ensureUserIndex(SHARED);
//		final DBObject ttlidx = new BasicDBObject(CREATED, 1);
//		final DBObject opts = new BasicDBObject("expireAfterSeconds",
//				JOB_EXPIRES);
//		jobcol.ensureIndex(ttlidx, opts);
	}

	private void ensureUserIndex(final String userField) {
		final DBObject idx = new BasicDBObject();
		idx.put(userField, 1);
		idx.put(SERVICE, 1);
		idx.put(COMPLETE, 1);
		jobcol.ensureIndex(idx);
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#createJob(java.lang.String)
	 */
	@Override
	public String createJob(final String user) throws CommunicationException {
		checkString(user, "user", MAX_LEN_USER);
		final DBObject job = new BasicDBObject(USER, user);
		final Date date = new Date();
		job.put(CREATED, date);
		job.put(UPDATED, date);
		job.put(EST_COMP, null);
		job.put(SERVICE, null);
		try {
			jobcol.insert(job);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		return ((ObjectId) job.get(MONGO_ID)).toString();
	}
	
	private static ObjectId checkJobID(final String id) {
		checkString(id, "id");
		final ObjectId oi;
		try {
			oi = new ObjectId(id);
		} catch (IllegalArgumentException iae) {
			throw new IllegalArgumentException(String.format(
					"Job ID %s is not a legal ID", id));
		}
		return oi;
	}
	
	private final static String QRY_FIND_JOB_BY_OWNER = String.format(
			"{%s: #, %s: #}", MONGO_ID, USER);
	private final static String QRY_FIND_JOB_BY_USER = String.format(
			"{%s: #, $or: [{%s: #}, {%s: #}]}", MONGO_ID, USER, SHARED);
	private final static String QRY_FIND_JOB_NO_USER = String.format(
			"{%s: #}", MONGO_ID);
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#getJob(java.lang.String, java.lang.String)
	 */
	@Override
	public Job getJob(final String user, final String jobID)
			throws CommunicationException, NoSuchJobException {
		checkString(user, "user", MAX_LEN_USER);
		final ObjectId oi = checkJobID(jobID);
		return getJob(user, oi);
	}
		
	private Job getJob(final String user, final ObjectId jobID)
			throws CommunicationException, NoSuchJobException {
		final Job j;
		try {
			if (user == null) {
				j = jobjong.findOne(QRY_FIND_JOB_NO_USER, jobID).as(UJSJob.class);
			} else {
				j = jobjong.findOne(QRY_FIND_JOB_BY_USER, jobID, user, user)
						.as(UJSJob.class);
			}
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		if (j == null) {
			throw new NoSuchJobException(String.format(
					"There is no job %s viewable by user %s", jobID, user));
		}
		return j;
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#startJob(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Date)
	 */
	@Override
	public void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, final Date estComplete)
			throws CommunicationException, NoSuchJobException {
		startJob(user, jobID, service, status, description, PROG_NONE, null,
				estComplete);
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#startJob(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, java.util.Date)
	 */
	@Override
	public void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, final int maxProg,
			final Date estComplete)
			throws CommunicationException, NoSuchJobException {
		startJob(user, jobID, service, status, description, PROG_TASK, maxProg,
				estComplete);
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#startJobWithPercentProg(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Date)
	 */
	@Override
	public void startJobWithPercentProg(final String user, final String jobID,
			final String service, final String status,
			final String description, final Date estComplete)
			throws CommunicationException, NoSuchJobException {
		startJob(user, jobID, service, status, description, PROG_PERC, null,
				estComplete);
	}
	
	private void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, final String progType,
			final Integer maxProg, final Date estComplete)
			throws CommunicationException, NoSuchJobException {
		checkString(user, "user", MAX_LEN_USER);
		final ObjectId oi = checkJobID(jobID);
		//this is coming from an auth token so doesn't need much checking
		//although if this is every really used as a lib (unlikely) will need better QA
		checkString(service, "service", MAX_LEN_SERVICE);
		checkMaxLen(status, "status", MAX_LEN_STATUS);
		checkMaxLen(description, "description", MAX_LEN_DESC);
		checkEstComplete(estComplete);
		if (maxProg != null && maxProg < 1) {
			throw new IllegalArgumentException(
					"The maximum progress for the job must be > 0"); 
		}
		final DBObject query = new BasicDBObject(USER, user);
		query.put(MONGO_ID, oi);
		query.put(SERVICE, null);
		final DBObject update = new BasicDBObject(SERVICE, service);
		update.put(STATUS, status);
		update.put(DESCRIPTION, description);
		update.put(PROG_TYPE, progType);
		final Date now = new Date();
		update.put(STARTED, now);
		update.put(UPDATED, now);
		update.put(EST_COMP, estComplete);
		update.put(COMPLETE, false);
		update.put(ERROR, false);
		update.put(ERROR_MSG, null);
		update.put(RESULT, null);
		
		final Integer prog;
		final Integer maxprog;
		if (progType == PROG_TASK) {
			prog = 0;
			maxprog = maxProg;
		} else if (progType == PROG_PERC) {
			prog = 0;
			maxprog = 100;
		} else if (progType == PROG_NONE) {
			prog = 0;
			maxprog = null;
		} else {
			throw new IllegalArgumentException("Illegal progress type: " +
					progType);
		}
		update.put(PROG, prog);
		update.put(MAXPROG, maxprog);
		
		final WriteResult wr;
		try {
			wr = jobcol.update(query, new BasicDBObject("$set", update));
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		if (wr.getN() != 1) {
			throw new NoSuchJobException(String.format(
					"There is no unstarted job %s for user %s", jobID, user));
		}
	}
	
	private void checkEstComplete(final Date estComplete) {
		if (estComplete == null) {
			return;
		}
		if (estComplete.compareTo(new Date()) < 1) {
			throw new IllegalArgumentException(
					"The estimated completion date must be in the future");
		}
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#createAndStartJob(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Date)
	 */
	@Override
	public String createAndStartJob(final String user, final String service,
			final String status, final String description,
			final Date estComplete)
			throws CommunicationException {
		return createAndStartJob(user, service, status, description, PROG_NONE,
				null, estComplete);
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#createAndStartJob(java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, java.util.Date)
	 */
	@Override
	public String createAndStartJob(final String user, final String service,
			final String status, final String description, final int maxProg,
			final Date estComplete)
			throws CommunicationException {
		return createAndStartJob(user, service, status, description, PROG_TASK,
				maxProg, estComplete);
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#createAndStartJobWithPercentProg(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Date)
	 */
	@Override
	public String createAndStartJobWithPercentProg(final String user,
			final String service, final String status,
			final String description, final Date estComplete)
			throws CommunicationException {
		return createAndStartJob(user, service, status, description, PROG_PERC,
				null, estComplete);
	}
	
	private String createAndStartJob(final String user, final String service,
			final String status, final String description,
			final String progType, final Integer maxProg,
			final Date estComplete)
			throws CommunicationException {
		final String jobid = createJob(user);
		try {
			startJob(user, jobid, service, status, description, progType,
					maxProg, estComplete);
		} catch (NoSuchJobException nsje) {
			throw new RuntimeException(
					"Just created a job and it's already deleted", nsje);
		}
		return jobid;
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#updateJob(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Integer, java.util.Date)
	 */
	@Override
	public void updateJob(final String user, final String jobID,
			final String service, final String status, final Integer progress,
			final Date estComplete)
			throws CommunicationException, NoSuchJobException {
		checkMaxLen(status, "status", MAX_LEN_STATUS);
		final DBObject query = buildStartedJobQuery(user, jobID, service);
		final DBObject set = new BasicDBObject(STATUS, status);
		set.put(UPDATED, new Date());
		if (estComplete != null) {
			checkEstComplete(estComplete);
			set.put(EST_COMP, estComplete);
		}
		final DBObject update = new BasicDBObject("$set", set);
		if (progress != null) {
			if (progress < 0) {
				throw new IllegalArgumentException(
						"progress cannot be negative");
			}
			update.put("$inc", new BasicDBObject(PROG, progress));
		}
		
		final WriteResult wr;
		try {
			wr = jobcol.update(query, update);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		if (wr.getN() != 1) {
			throw new NoSuchJobException(String.format(
					"There is no uncompleted job %s for user %s started by service %s",
					jobID, user, service));
		}
	}
	
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#completeJob(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Map)
	 */
	@Override
	public void completeJob(final String user, final String jobID,
			final String service, final String status, final String error,
			final JobResults results)
			throws CommunicationException, NoSuchJobException {
		checkMaxLen(status, "status", MAX_LEN_STATUS);
		checkMaxLen(error, "error", MAX_LEN_ERR);
		final DBObject query = buildStartedJobQuery(user, jobID, service);
		final DBObject set = new BasicDBObject(UPDATED, new Date());
		set.put(COMPLETE, true);
		set.put(ERROR, error != null);
		set.put(ERROR_MSG, error);
		set.put(STATUS, status);
		//if anyone is stupid enough to store 16mb of results will need to
		//check size first, or at least catch error and report.
		set.put(RESULT, resultsToDBObject(results));
		
		final WriteResult wr;
		try {
			wr = jobcol.update(query, new BasicDBObject("$set", set));
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		if (wr.getN() != 1) {
			throw new NoSuchJobException(String.format(
					"There is no uncompleted job %s for user %s started by service %s",
					jobID, user, service));
		}
	}
	
	/* DO NOT change this to use Jongo. This enforces the continuing use of
	 *  the same field names, which is needed for backwards compatibility.
	 */
	private static DBObject resultsToDBObject(final JobResults res) {
		if (res == null) {
			return null;
		}
		final DBObject ret = new BasicDBObject();
		ret.put("shocknodes", res.getShocknodes());
		ret.put("shockurl", res.getShockurl());
		ret.put("workspaceids", res.getWorkspaceids());
		ret.put("workspaceurl", res.getWorkspaceurl());
		if (res.getResults() != null) {
			final List<DBObject> results = new LinkedList<DBObject>();
			ret.put("results", results);
			for (final JobResult jr: res.getResults()) {
				final DBObject oneres = new BasicDBObject();
				results.add(oneres);
				oneres.put("servtype", jr.getServtype());
				oneres.put("url", jr.getUrl());
				oneres.put("id", jr.getId());
				oneres.put("desc", jr.getDesc());
			}
		}
		return ret;
	}

	private DBObject buildStartedJobQuery(final String user, final String jobID,
			final String service) {
		checkString(user, "user", MAX_LEN_USER);
		final ObjectId id = checkJobID(jobID);
		checkString(service, "service", MAX_LEN_SERVICE);
		final DBObject query = new BasicDBObject(USER, user);
		query.put(MONGO_ID, id);
		query.put(SERVICE, service);
		query.put(COMPLETE, false);
		return query;
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#deleteJob(java.lang.String, java.lang.String)
	 */
	@Override
	public void deleteJob(final String user, final String jobID)
			throws NoSuchJobException, CommunicationException {
		deleteJob(user, jobID, null);
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#deleteJob(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void deleteJob(final String user, final String jobID,
			final String service)
			throws NoSuchJobException, CommunicationException {
		checkString(user, "user", MAX_LEN_USER);
		final ObjectId id = checkJobID(jobID);
		final DBObject query = new BasicDBObject(USER, user);
		query.put(MONGO_ID, id);
		if (service == null) {
			query.put(COMPLETE, true);
		} else {
			query.put(SERVICE, service);
		}
		final WriteResult wr;
		try {
			wr = jobcol.remove(query);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		if (wr.getN() != 1) {
			throw new NoSuchJobException(String.format(
					"There is no %sjob %s for user %s",
					service == null ? "completed " : "", jobID, user +
					(service == null ? "" : " and service " + service)));
		}
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#listServices(java.lang.String)
	 */
	@Override
	public Set<String> listServices(final String user)
			throws CommunicationException {
		checkString(user, "user");
		final DBObject query = new BasicDBObject("$or", Arrays.asList(
				new BasicDBObject(USER, user),
				new BasicDBObject(SHARED, user)));
		query.put(SERVICE, new BasicDBObject("$ne", null));
		final Set<String> services = new HashSet<String>();
		try {
			@SuppressWarnings("unchecked")
			final List<String> servs = jobcol.distinct(SERVICE, query);
			services.addAll(servs);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		return services;
	}
	
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#listJobs(java.lang.String, java.util.List, boolean, boolean, boolean, boolean)
	 */
	@Override
	public List<Job> listJobs(final String user, final List<String> services,
			final boolean queued, final boolean running,
			final boolean complete, final boolean error, final boolean shared)
			throws CommunicationException {
		//queued is ignored
		checkString(user, "user");
		String query;
		if (shared) {
			query = String.format("{$or: [{%s: '%s'}, {%s: '%s'}]",
					USER, user, SHARED, user);
		} else {
			query = String.format("{%s: '%s'", USER, user);
		}
		if (services != null && !services.isEmpty()) {
			for (final String s: services) {
				checkString(s, "service", MAX_LEN_SERVICE);
			}
			query += String.format(", %s: {$in: ['%s']}", SERVICE,
					StringUtils.join(services, "', '"));
		} else {
			query += String.format(", %s: {$ne: null}", SERVICE);
		}
		//this seems dumb.
		if (running && !complete && !error) {
			query += ", " + COMPLETE + ": false}";
		} else if (!running && complete && !error) {
			query += ", " + COMPLETE + ": true, " + ERROR + ": false}";
		} else if (!running && !complete && error) {
			query += ", " + ERROR + ": true}";
		} else if (running && complete && !error) {
			query += ", " + ERROR + ": false}";
		} else if (!running && complete && error) {
			query += ", " + COMPLETE + ": true}";
		} else if (running && !complete && error) {
			query += ", $or: [{" + COMPLETE + ": false}, {" + ERROR + ": true}]}";
		} else {
			query += "}";
		}
		final List<Job> jobs = new LinkedList<Job>();
		try {
			final Iterable<UJSJob> j  = jobjong.find(query).as(UJSJob.class);
			for (final UJSJob job: j) {
				jobs.add(job);
			}
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		return jobs;
	}
	
	//note sharing with an already shared user or sharing with the owner has
	//no effect
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#shareJob(java.lang.String, java.lang.String, java.util.List)
	 */
	@Override
	public void shareJob(final String owner, final String jobID,
			final List<String> users)
			throws CommunicationException, NoSuchJobException {
		final ObjectId id = checkShareParams(owner, jobID, users, "owner");
		final List<String> us = new LinkedList<String>();
		for (final String u: users) {
			if (u != owner) {
				us.add(u);
			}
		}
		final WriteResult wr;
		try {
			wr = jobjong.update(QRY_FIND_JOB_BY_OWNER, id, owner)
					.with("{$addToSet: {" + SHARED + ": {$each: #}}}", us);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		if (wr.getN() != 1) {
			throw new NoSuchJobException(String.format(
					"There is no job %s owned by user %s", jobID, owner));
		}
	}

	private ObjectId checkShareParams(final String user, final String jobID,
			final List<String> users, final String userType) {
		checkString(user, userType);
		if (users == null) {
			throw new IllegalArgumentException("The users list cannot be null");
		}
		if (users.isEmpty()) {
			throw new IllegalArgumentException("The users list is empty");
		}
		for (final String u: users) {
			checkString(u, "user");
		}
		final ObjectId id = checkJobID(jobID);
		return id;
	}
	
	//removing the owner or an unshared user has no effect
	/* (non-Javadoc)
	 * @see us.kbase.userandjobstate.jobstate.JobStateInter#unshareJob(java.lang.String, java.lang.String, java.util.List)
	 */
	@Override
	public void unshareJob(final String user, final String jobID,
			final List<String> users) throws CommunicationException,
			NoSuchJobException {
		final NoSuchJobException e = new NoSuchJobException(String.format(
				"There is no job %s visible to user %s", jobID, user));
		final ObjectId id = checkShareParams(user, jobID, users, "user");
		final Job j;
		try {
			j= getJob(null, id);
		} catch (NoSuchJobException nsje) {
			throw e;
		}
		if (j.getUser().equals(user)) {
			//it's the owner, can do whatever
		} else if (j.getShared().contains(user)) {
			if (!users.equals(Arrays.asList(user))) {
				throw new IllegalArgumentException(String.format(
						"User %s may only stop sharing job %s for themselves",
						user, jobID));
			}
			//shared user removing themselves, no prob
		} else {
			throw e;
		}
		try {
			jobjong.update(QRY_FIND_JOB_BY_OWNER, id, j.getUser())
					.with("{$pullAll: {" + SHARED + ": #}}", users);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
	}
}
