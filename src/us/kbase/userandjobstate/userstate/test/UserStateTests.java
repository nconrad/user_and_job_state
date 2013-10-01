package us.kbase.userandjobstate.userstate.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.userandjobstate.test.UserJobStateTestCommon;
import us.kbase.userandjobstate.userstate.UserState;

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
	public void getAndSetState() throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("bar", "baz");
		us.setState("foo", "serv1", false, "key1",
				data);
		assertThat("got correct data back",
				us.getState("foo", "serv1", false, "key1"),
				is((Object) new BasicDBObject(data)));
		Map<String, Object> data2 = new HashMap<String, Object>();
		data2.put("bar", data);
		us.setState("foo", "authserv", true, "authkey", data2);
		assertThat("got correct data back",
				us.getState("foo", "authserv", true, "authkey"),
				is((Object) new BasicDBObject(data2)));
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
