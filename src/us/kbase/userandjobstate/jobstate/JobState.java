package us.kbase.userandjobstate.jobstate;

import static us.kbase.common.utils.StringUtils.checkString;
import static us.kbase.common.utils.StringUtils.checkMaxLen;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class JobState {

	private final static int JOB_EXPIRES = 30 * 24 * 60 * 60; // 30 days
	
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
	
	private final static String MONGO_ID = "_id";
	
	public final static String PROG_NONE = "none";
	public final static String PROG_TASK = "task";
	public final static String PROG_PERC = "percent";
	
	private final DBCollection jobcol;
	private final MongoCollection jobjong;
	
	public JobState(final String host, final String database,
			final String collection)
			throws UnknownHostException, IOException, InvalidHostException {
		final DB m = GetMongoDB.getDB(host, database);
		jobcol = m.getCollection(collection);
		jobjong = new Jongo(m).getCollection(collection);
		ensureIndexes();
	}

	public JobState(final String host, final String database,
			final String collection, final String user, final String password)
			throws UnknownHostException, IOException, InvalidHostException,
			MongoAuthException {
		final DB m = GetMongoDB.getDB(host, database, user, password);
		jobcol = m.getCollection(collection);
		jobjong = new Jongo(m).getCollection(collection);
		ensureIndexes();
	}

	private void ensureIndexes() {
		final DBObject idx = new BasicDBObject();
		idx.put(USER, 1);
		idx.put(SERVICE, 1);
		idx.put(COMPLETE, 1);
		jobcol.ensureIndex(idx);
		final DBObject ttlidx = new BasicDBObject(CREATED, 1);
		final DBObject opts = new BasicDBObject("expireAfterSeconds",
				JOB_EXPIRES);
		jobcol.ensureIndex(ttlidx, opts);
	}
	
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
	
	private final static String QRY_FIND_JOB = String.format(
			"{%s: #, %s: #}", MONGO_ID, USER);
	
	public Job getJob(final String user, final String jobID)
			throws CommunicationException, NoSuchJobException {
		checkString(user, "user", MAX_LEN_USER);
		final ObjectId oi = checkJobID(jobID);
		final Job j;
		try {
			j = jobjong.findOne(QRY_FIND_JOB,
					oi, user).as(Job.class);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		if (j == null) {
			throw new NoSuchJobException(String.format(
					"There is no job %s for user %s", jobID, user));
		}
		return j;
	}
	
	public void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, final Date estComplete)
			throws CommunicationException, NoSuchJobException {
		startJob(user, jobID, service, status, description, PROG_NONE, null,
				estComplete);
	}
	
	public void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, final int maxProg,
			final Date estComplete)
			throws CommunicationException, NoSuchJobException {
		startJob(user, jobID, service, status, description, PROG_TASK, maxProg,
				estComplete);
	}
	
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
	
	public String createAndStartJob(final String user, final String service,
			final String status, final String description,
			final Date estComplete)
			throws CommunicationException {
		return createAndStartJob(user, service, status, description, PROG_NONE,
				null, estComplete);
	}
	
	public String createAndStartJob(final String user, final String service,
			final String status, final String description, final int maxProg,
			final Date estComplete)
			throws CommunicationException {
		return createAndStartJob(user, service, status, description, PROG_TASK,
				maxProg, estComplete);
	}
	
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
	
	
	public void completeJob(final String user, final String jobID,
			final String service, final String status, final String error,
			final Map<String, Object> results)
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
		set.put(RESULT, results);
		
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
	
	public void deleteJob(final String user, final String jobID)
			throws NoSuchJobException, CommunicationException {
		deleteJob(user, jobID, null);
	}
	
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
	
	final static DBObject SERV_GROUP;
	static {
		final DBObject pfields = new BasicDBObject(SERVICE, 1);
		pfields.put(MONGO_ID, 0);
		SERV_GROUP = new BasicDBObject("$group",
				new BasicDBObject(MONGO_ID, "$" + SERVICE));
	}
	
	public Set<String> listServices(final String user)
			throws CommunicationException {
		checkString(user, "user");
		final DBObject query = new BasicDBObject(USER, user);
		query.put(SERVICE, new BasicDBObject("$ne", null));
		final DBObject match = new BasicDBObject("$match", query);
		
		final AggregationOutput mret;
		try {
			mret = jobcol.aggregate(match, SERV_GROUP);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		final Set<String> services = new HashSet<String>();
		for (DBObject o: mret.results()) {
			services.add((String) o.get(MONGO_ID));
		}
		return services;
	}
	
	public List<Job> listJobs(final String user, final List<String> services,
			final boolean running, final boolean complete,
			final boolean error)
			throws CommunicationException {
		checkString(user, "user");
		String query = String.format("{%s: '%s'", USER, user);
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
		final Iterable<Job> j;
		try {
			j = jobjong.find(query).as(Job.class);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		final List<Job> jobs = new LinkedList<Job>();
		for (final Job job: j) {
			jobs.add(job);
		}
		return jobs;
	}
}
