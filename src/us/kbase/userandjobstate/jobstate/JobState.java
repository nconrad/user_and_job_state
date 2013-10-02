package us.kbase.userandjobstate.jobstate;

import static us.kbase.common.utils.StringUtils.checkString;
import static us.kbase.common.utils.StringUtils.checkMaxLen;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;

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

public class JobState {

	private final static int JOB_EXP = 30 * 24 * 60 * 60; // 30 days
	
	private final static int MAX_LEN_USER = 100;
	private final static int MAX_LEN_SERVICE = 100;
	private final static int MAX_LEN_STATUS = 200;
	private final static int MAX_LEN_DESC = 1000;
	
	
	private final static String CREATED = "created";
	private final static String USER = "user";
	private final static String SERVICE = "service";
	private final static String UPDATED = "updated";
	private final static String COMPLETE = "complete";
	private final static String ERROR = "error";
	private final static String DESCRIPTION = "desc";
	private final static String PROG_TYPE = "progtype";
	private final static String PROG = "prog";
	private final static String MAXPROG = "maxprog";
	private final static String STATUS = "status";
	private final static String RESULT = "result";
	
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
		final DBObject opts = new BasicDBObject("expireAfterSeconds", JOB_EXP);
		jobcol.ensureIndex(ttlidx, opts);
		
	}
	
	public String createJob(final String user) throws CommunicationException {
		checkString(user, "user", MAX_LEN_USER);
		final DBObject job = new BasicDBObject(USER, user);
		final Date date = new Date();
		job.put(CREATED, date);
		job.put(UPDATED, date);
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
			final String description) throws CommunicationException,
			NoSuchJobException {
		startJob(user, jobID, service, status, description, PROG_NONE, null);
	}
	
	public void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, int maxProg) throws
		CommunicationException, NoSuchJobException {
		startJob(user, jobID, service, status, description, PROG_TASK, maxProg);
	}
	
	public void startJobWithPercentProg(final String user, final String jobID,
			final String service, final String status,
			final String description)
			throws CommunicationException, NoSuchJobException {
		startJob(user, jobID, service, status, description, PROG_PERC, null);
	}
	
	private void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, final String progType,
			final Integer maxProg)
			throws CommunicationException, NoSuchJobException {
		checkString(user, "user", MAX_LEN_USER);
		final ObjectId oi = checkJobID(jobID);
		//this is coming from an auth token so doesn't need much checking
		//although if this is every really used as a lib (unlikely) will need better QA
		checkString(service, "service", MAX_LEN_SERVICE);
		checkMaxLen(status, "status", MAX_LEN_STATUS);
		checkMaxLen(description, "description", MAX_LEN_DESC);
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
		update.put(UPDATED, new Date());
		update.put(COMPLETE, false);
		update.put(ERROR, false);
		update.put(RESULT, false);
		
		final Integer prog;
		final Integer maxprog;
		if (progType == PROG_TASK) {
			prog = 0;
			maxprog = maxProg;
		} else if (progType == PROG_PERC) {
			prog = 0;
			maxprog = 100;
		} else if (progType != PROG_NONE) {
			throw new IllegalArgumentException("Illegal progress type: " +
					progType);
		} else {
			prog = null;
			maxprog = null;
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
		if(!(Boolean) wr.getField("updatedExisting")) { //seriously 10gen? Seriously?
			throw new NoSuchJobException(String.format(
					"There is no unstarted job %s for user %s", jobID, user));
		}
	}
}