package us.kbase.userandjobstate.userstate;

import static us.kbase.common.utils.StringUtils.isNonEmptyString;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

import us.kbase.common.mongo.GetMongoDB;
import us.kbase.common.mongo.exceptions.InvalidHostException;
import us.kbase.common.mongo.exceptions.MongoAuthException;
import us.kbase.userandjobstate.exceptions.CommunicationException;
import us.kbase.userandjobstate.userstate.exceptions.NoSuchKeyException;

public class UserState { //TODO tests for all this
	//TODO fix glassfish start scripts so they fail if no contact
	
	private final static int MAX_VALUE_SIZE = 1000000;
	
	private final static String SERVICE = "service";
	private final static String USER = "user";
	private final static String KEY = "key";
	private final static String AUTH = "auth";
	private final static String VALUE = "value";
	private final static String MONGO_ID = "_id";
	
	private final static String IDX_UNIQ = "unique";
	
	private final DBCollection uscol;
	
	private final static ObjectMapper MAPPER = new ObjectMapper();
	private final static Pattern INVALID_SERV_NAMES = 
			Pattern.compile("[^\\w]");
	
	public UserState(final String host, final String database,
			final String collection)
			throws UnknownHostException, IOException, InvalidHostException {
		final DB m = GetMongoDB.getDB(host, database);
		uscol = m.getCollection(collection);
		ensureIndexes();
	}

	public UserState(final String host, final String database,
			final String collection, final String user, final String password)
			throws UnknownHostException, IOException, InvalidHostException,
			MongoAuthException {
		final DB m = GetMongoDB.getDB(host, database, user, password);
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
		final DBObject query = generateQuery(user, service, auth, key);
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

	private DBObject generateQuery(final String user, final String service,
			final boolean auth, final String key) {
		isNonEmptyString(user, "user");
		checkServiceName(service);
		isNonEmptyString(key, "key");
		final DBObject query = new BasicDBObject();
		query.put(USER, user);
		query.put(SERVICE, service);
		query.put(AUTH, auth);
		query.put(KEY, key);
		return query;
	}
	
	private static void checkServiceName(final String name) {
		isNonEmptyString(name, "service");
		final Matcher m = INVALID_SERV_NAMES.matcher(name);
		if (m.find()) {
			throw new IllegalArgumentException(String.format(
					"Illegal character in service name %s: %s", name, m.group()));
		}
	}

	public Object getState(final String user, final String service, 
			final boolean auth, final String key)
			throws CommunicationException, NoSuchKeyException {
		final DBObject query = generateQuery(user, service, auth, key);
		final DBObject projection = new BasicDBObject();
		projection.put(VALUE, 1);
		final DBObject mret;
		try {
			mret = uscol.findOne(query, projection);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		if (mret == null) {
			throw new NoSuchKeyException(String.format(
					"There is no key %s for the %sauthorized service %s", key,
					auth ? "" : "un", service));
		}
		return mret.get(VALUE);
	}

	public void removeState(final String user, final String service, 
			final boolean auth, final String key)
			throws CommunicationException {
		final DBObject query = generateQuery(user, service, auth, key);
		try {
			uscol.remove(query);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
	}

	public List<String> listState(final String user, final String service,
			final boolean auth)
			throws CommunicationException, NoSuchKeyException {
		isNonEmptyString(user, "user");
		checkServiceName(service);
		final DBObject query = new BasicDBObject();
		query.put(USER, user);
		query.put(SERVICE, service);
		query.put(AUTH, auth);
		final DBObject projection = new BasicDBObject();
		projection.put(KEY, 1);
		final DBCursor mret;
		try {
			mret = uscol.find(query, projection);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		if (mret == null) {
			throw new NoSuchKeyException(String.format(
					"There are no keys for the %sauthorized service %s",
					auth ? "" : "un", service));
		}
		final List<String> keys = new LinkedList<String>();
		for (DBObject o: mret) {
			keys.add((String) o.get(KEY));
		}
		return keys;
	}
	
	final static DBObject SERV_GROUP;
	final static DBObject SERV_PROJ;
	static {
		final DBObject pfields = new BasicDBObject(SERVICE, 1);
		pfields.put(MONGO_ID, 0);
		SERV_PROJ = new BasicDBObject("$project", pfields);
		SERV_GROUP = new BasicDBObject("$group",
				new BasicDBObject(MONGO_ID, "$" + SERVICE));
	}

	public List<String> listServices(final String user, final boolean auth)
			throws CommunicationException {
		isNonEmptyString(user, "user");
		final DBObject mfields = new BasicDBObject(USER, user);
		mfields.put(AUTH, auth);
		final DBObject match = new BasicDBObject("$match", mfields);
		
		final AggregationOutput mret;
		try {
			mret = uscol.aggregate(match, SERV_PROJ, SERV_GROUP);
		} catch (MongoException me) {
			throw new CommunicationException(
					"There was a problem communicating with the database", me);
		}
		final List<String> services = new LinkedList<String>();
		for (DBObject o: mret.results()) {
			services.add((String) o.get(MONGO_ID));
		}
		return services;
	}
}
