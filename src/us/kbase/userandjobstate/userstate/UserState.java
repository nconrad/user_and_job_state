package us.kbase.userandjobstate.userstate;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jongo.Jongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

import us.kbase.common.mongo.GetMongoDB;
import us.kbase.common.mongo.exceptions.InvalidHostException;
import us.kbase.common.mongo.exceptions.MongoAuthException;
import us.kbase.userandjobstate.mongo.exceptions.CommunicationException;

public class UserState {
	
	private final static int MAX_VALUE_SIZE = 1000000;
	
	private final static String SERVICE = "service";
	private final static String USER = "user";
	private final static String KEY = "key";
	private final static String AUTH = "auth";
	private final static String VALUE = "value";
	
	private final static String IDX_UNIQ = "unique";
	
	private final DBCollection uscol;
	private final Jongo usjongo;
	
	private final static ObjectMapper MAPPER = new ObjectMapper();
	private final static Pattern INVALID_SERV_NAMES = 
			Pattern.compile("[^\\w]");
	
	public UserState(final String host, final String database,
			final String collection)
			throws UnknownHostException, IOException, InvalidHostException {
		final DB m = GetMongoDB.getDB(host, database);
		usjongo = new Jongo(m);
		uscol = m.getCollection(collection);
		ensureIndexes();
	}

	public UserState(final String host, final String database,
			final String collection, final String user, final String password)
			throws UnknownHostException, IOException, InvalidHostException,
			MongoAuthException {
		final DB m = GetMongoDB.getDB(host, database, user, password);
		usjongo = new Jongo(m);
		uscol = m.getCollection(collection);
		ensureIndexes();
	}

	private void ensureIndexes() {
		final DBObject idx = new BasicDBObject();
		idx.put(USER, 1);
		idx.put(SERVICE, 1);
		idx.put(AUTH, 1);
		idx.put(KEY, 1);
		final DBObject unique = new BasicDBObject();
		unique.put(IDX_UNIQ, 1);
		unique.put("unique", 1);
		uscol.ensureIndex(idx, unique);
	}

	private static final String VAL_ERR = String.format(
			"Value cannot be > %s bytes when serialized", MAX_VALUE_SIZE);
	
	public void setState(final String user, final String service,
			final boolean auth, final String key, final Object value)
			throws CommunicationException {
		//TODO tests
		if (user == null || user.isEmpty()) {
			throw new IllegalArgumentException(
					"User cannot be null or the empty string");
		}
		checkServiceName(service);
		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException(
					"Key cannot be null or the empty string");
		}
		if (value != null) {
			final String valueStr;
			try {
				valueStr = MAPPER.writeValueAsString(value);
			} catch (JsonProcessingException jpe) {
				throw new IllegalArgumentException(
						"Unable to serialize value", jpe);
			}
			if (valueStr.length() > MAX_VALUE_SIZE) {
				throw new IllegalArgumentException(VAL_ERR);
			}
		}
		final DBObject query = new BasicDBObject();
		query.put(USER, user);
		query.put(SERVICE, service);
		query.put(AUTH, auth);
		query.put(KEY, key);
		final DBObject set = new BasicDBObject();
		final DBObject val = new BasicDBObject();
		val.put(VALUE, value);
		set.put("$set", val);
		try {
			uscol.update(query, set, true, false);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
	}
	
	private static void checkServiceName(String name) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException(
					"A service name cannot be null or the empty string");
		}
		final Matcher m = INVALID_SERV_NAMES.matcher(name);
		if (m.find()) {
			throw new IllegalArgumentException(String.format(
					"Illegal character in service name %s: %s", name, m.group()));
		}
	}
}
