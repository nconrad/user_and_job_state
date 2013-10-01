package us.kbase.userandjobstate.jobstate;

import static us.kbase.common.utils.StringUtils.isNonEmptyString;

import java.io.IOException;
import java.net.UnknownHostException;

import org.bson.types.ObjectId;

import us.kbase.common.mongo.GetMongoDB;
import us.kbase.common.mongo.exceptions.InvalidHostException;
import us.kbase.common.mongo.exceptions.MongoAuthException;
import us.kbase.userandjobstate.exceptions.CommunicationException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

public class JobState {
	
	private final static String USER = "user";
	
	private final DBCollection jobcol;
	
	public JobState(final String host, final String database,
			final String collection)
			throws UnknownHostException, IOException, InvalidHostException {
		final DB m = GetMongoDB.getDB(host, database);
		jobcol = m.getCollection(collection);
		ensureIndexes();
	}

	public JobState(final String host, final String database,
			final String collection, final String user, final String password)
			throws UnknownHostException, IOException, InvalidHostException,
			MongoAuthException {
		final DB m = GetMongoDB.getDB(host, database, user, password);
		jobcol = m.getCollection(collection);
		ensureIndexes();
	}

	private void ensureIndexes() {
		// TODO Auto-generated method stub
		
	}
	
	public String createJob(String user) throws CommunicationException {
		isNonEmptyString(user, "user");
		final DBObject job = new BasicDBObject(USER, user);
		try {
			jobcol.insert(job);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		return ((ObjectId) job.get("_id")).toString();
	}
}
