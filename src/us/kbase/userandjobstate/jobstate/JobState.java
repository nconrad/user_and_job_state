package us.kbase.userandjobstate.jobstate;

import java.io.IOException;
import java.net.UnknownHostException;

import us.kbase.common.mongo.GetMongoDB;
import us.kbase.common.mongo.exceptions.InvalidHostException;
import us.kbase.common.mongo.exceptions.MongoAuthException;

import com.mongodb.DB;
import com.mongodb.DBCollection;

public class JobState {
	
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
}
