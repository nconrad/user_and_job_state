package us.kbase.userandjobstate.jobstate;

import static us.kbase.common.utils.StringUtils.isNonEmptyString;

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

public class JobState {
	
	private final static String USER = "user";
	private final static String SERVICE = "service";
	private final static String UPDATED = "updated";
	private final static String COMPLETE = "complete";
	
	private final static String PROG_NONE = "none";
	private final static String PROG_TASK = "task";
	private final static String PROG_PERC = "percent";
	
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
		
	}
	
	public String createJob(final String user) throws CommunicationException {
		isNonEmptyString(user, "user");
		final DBObject job = new BasicDBObject(USER, user);
		job.put(UPDATED, new Date());
		try {
			jobcol.insert(job);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		return ((ObjectId) job.get("_id")).toString();
	}
	
	private static ObjectId checkJobID(final String id) {
		isNonEmptyString(id, "service");
		final ObjectId oi;
		try {
			oi = new ObjectId(id);
		} catch (IllegalArgumentException iae) {
			throw new IllegalArgumentException(String.format(
					"Job ID %s is not a legal ID", id));
		}
	}
	
	public Job getJob(final String user, final String jobID)
			throws CommunicationException, NoSuchJobException {
		isNonEmptyString(user, "user");
		ObjectId oi = checkJobID(jobID);
		final Job j;
		try {
			j = jobjong.findOne("{_id: #, user: #}",
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
			final String description) {
		startJob(user, jobID, service, status, description, PROG_NONE, null);
	}
	
	public void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, int maxProg) {
		startJob(user, jobID, service, status, description, PROG_TASK, maxProg);
	}
	
	public void startJobWithPercentProg(final String user, final String jobID,
			final String service, final String status,
			final String description) {
		startJob(user, jobID, service, status, description, PROG_PERC, null);
	}
	
	private void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, final String progType,
			final Integer maxProg) {
		
	}
}
