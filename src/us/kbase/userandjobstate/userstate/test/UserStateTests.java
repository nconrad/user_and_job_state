package us.kbase.userandjobstate.userstate.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.userandjobstate.test.UserJobStateTestCommon;
import us.kbase.userandjobstate.userstate.UserState;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

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
		DBObject expected = new BasicDBObject("bar", "baz");
		assertThat("got correct data back", us.getState("foo", "serv1", false, "key1"),
				is((Object) expected));
	}
}
