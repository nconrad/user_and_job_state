package us.kbase.userandjobstate.userstate.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.userandjobstate.test.UserJobStateTestCommon;
import us.kbase.userandjobstate.userstate.UserState;
import us.kbase.userandjobstate.userstate.exceptions.NoSuchKeyException;

import com.mongodb.BasicDBObject;

public class UserStateTests {
	
	private static UserState us;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		UserJobStateTestCommon.destroyAndSetupDB();
		
		String host = UserJobStateTestCommon.getHost();
		String mUser = UserJobStateTestCommon.getMongoUser();
		String mPwd = UserJobStateTestCommon.getMongoPwd();
		String db = UserJobStateTestCommon.getDB();
		
		if (mUser != null) {
			us = new UserState(host, db, "userstate", mUser, mPwd);
		} else {
			us = new UserState(host, db, "userstate");
		}
	}
	
	@Test
	public void illegalArgs() throws Exception {
		checkArgs(null, "foo", true, "key", "user cannot be null or the empty string");
		checkArgs(null, "foo", false, "key", "user cannot be null or the empty string");
		checkArgs("", "foo", true, "key", "user cannot be null or the empty string");
		checkArgs("", "foo", false, "key", "user cannot be null or the empty string");
		checkArgs("u", null, true, "key", "service cannot be null or the empty string");
		checkArgs("u", null, false, "key", "service cannot be null or the empty string");
		checkArgs("u", "", true, "key", "service cannot be null or the empty string");
		checkArgs("u", "", false, "key", "service cannot be null or the empty string");
		checkArgs("u", "afa)aafe", true, "key", "Illegal character in service name afa)aafe: )");
		checkArgs("u", "afae-afa", false, "key", "Illegal character in service name afae-afa: -");
		checkArgs("u", "foo", true, null, "key cannot be null or the empty string");
		checkArgs("u", "foo", false, null, "key cannot be null or the empty string");
		checkArgs("u", "foo", true, "", "key cannot be null or the empty string");
		checkArgs("u", "foo", false, "", "key cannot be null or the empty string");
		checkListState(null, "foo", true, "user cannot be null or the empty string");
		checkListState(null, "foo", false, "user cannot be null or the empty string");
		checkListState("", "foo", true, "user cannot be null or the empty string");
		checkListState("", "foo", false, "user cannot be null or the empty string");
		checkListState("u", null, true, "service cannot be null or the empty string");
		checkListState("u", null, false, "service cannot be null or the empty string");
		checkListState("u", "", true, "service cannot be null or the empty string");
		checkListState("u", "", false, "service cannot be null or the empty string");
		checkListState("u", "afa)aafe", true, "Illegal character in service name afa)aafe: )");
		checkListState("u", "afae-afa", false, "Illegal character in service name afae-afa: -");
		checkListServices(null, true, "user cannot be null or the empty string");
		checkListServices(null, false, "user cannot be null or the empty string");
		checkListServices("", true, "user cannot be null or the empty string");
		checkListServices("", false, "user cannot be null or the empty string");
	}
	
	private void checkListState(String user, String service, boolean auth,
			String exception) throws Exception {
		try {
			us.listState(user, service, auth);
			fail("list state with bad args");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is(exception));
		}
	}
	
	private void checkListServices(String user, boolean auth,
			String exception) throws Exception {
		try {
			us.listServices(user, auth);
			fail("list services with bad args");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is(exception));
		}
	}
	
	private void checkArgs(String user, String service, boolean auth,
			String key, String exception) throws Exception {
		Object data = "foo";
		try {
			us.setState(user, service, auth, key, data);
			fail("set state w/ bad args");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is(exception));
		}
		try {
			us.getState(user, service, auth, key);
			fail("get state w/ bad args");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is(exception));
		}
		try {
			us.removeState(user, service, auth, key);
			fail("remove state w/ bad args");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is(exception));
		}
	}
	
	@Test
	public void getNonExistantState() throws Exception {
		us.setState("u", "nostate", false, "thing1", "foo");
		us.setState("u", "nostate", true, "thing1", "foo");
		try {
			us.getState("u", "nostate", false, "thing");
			fail("got non-existant state");
		} catch (NoSuchKeyException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is("There is no key thing for the unauthorized service nostate"));
		}
		try {
			us.getState("u", "nostate", true, "thing");
			fail("got non-existant state");
		} catch (NoSuchKeyException nske) {
			assertThat("correct exception", nske.getLocalizedMessage(),
					is("There is no key thing for the authorized service nostate"));
		}
	}
	
	@Test
	public void removeState() throws Exception {
		us.setState("u", "remstate", false, "thing1", "foo");
		us.setState("u", "remstate", true, "thing1", "foo");
		assertThat("get works prior to remove",
				us.getState("u", "remstate", false, "thing1"),
				is((Object) "foo"));
		assertThat("get works prior to remove",
				us.getState("u", "remstate", true, "thing1"),
				is((Object) "foo"));
		us.removeState("u", "remstate", false, "thing1");
		assertThat("get works after unauthed remove",
				us.getState("u", "remstate", true, "thing1"),
				is((Object) "foo"));
		try {
			us.getState("u", "remstate", false, "thing1");
			fail("got removed state");
		} catch (NoSuchKeyException nske) {
			assertThat("correct exception", nske.getLocalizedMessage(),
					is("There is no key thing1 for the unauthorized service remstate"));
		}
		us.setState("u", "remstate", false, "thing1", "foo");
		us.removeState("u", "remstate", true, "thing1");
		assertThat("get works after authed remove",
				us.getState("u", "remstate", false, "thing1"),
				is((Object) "foo"));
		try {
			us.getState("u", "remstate", true, "thing1");
			fail("got removed state");
		} catch (NoSuchKeyException nske) {
			assertThat("correct exception", nske.getLocalizedMessage(),
					is("There is no key thing1 for the authorized service remstate"));
		}
	}

	@Test
	public void getAndSetState() throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("bar", "baz");
		us.setState("foo", "serv1", false, "key1", data);
		assertThat("got correct data back",
				us.getState("foo", "serv1", false, "key1"),
				is((Object) new BasicDBObject(data)));
		Map<String, Object> data2 = new HashMap<String, Object>();
		data2.put("bar", data);
		us.setState("foo", "authserv1", true, "authkey1", data2);
		assertThat("got correct data back",
				us.getState("foo", "authserv1", true, "authkey1"),
				is((Object) new BasicDBObject(data2)));
		us.setState("foo", "serv1", false, "key2", data);
		us.setState("foo", "serv2", false, "key", data);
		us.setState("foo", "authserv1", true, "authkey2", data2);
		us.setState("foo", "authserv2", true, "authkey", data2);
		//TODO finish basic ops testing
	}
	
	@Test
	public void setUnserializable() throws Exception {
		try {
			us.setState("foo", "unserializable", false, "a", new JFrame());
			fail("saved unserializable object");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is("Unable to serialize value"));
		}
	}
	
	private static final String BIG_STR = 
			"afbiekdiekaotkjaialeotialeicitlaielfialefotaleofleif" +
			"afbiekdiekaotkjaialeotialeicitlaielfialefotaleofleif";
	@Test
	public void failOnBigState() throws Exception {
		Map<Integer, String> data = new HashMap<Integer, String>();
		for (int i = 0; i < 10000; i++) {
			data.put(i, BIG_STR);
		}
		try {
			us.setState("foo", "servBig", false, "key1", data);
			fail("set state with way too big data");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is("Value cannot be > 1000000 bytes when serialized"));
		}
	}
}
