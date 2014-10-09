package us.kbase.userandjobstate.test;


import us.kbase.common.test.TestException;

public class UserJobStateTestCommon {
	
	public static final String SHOCKEXE = "test.shock.exe";
	public static final String MONGOEXE = "test.mongo.exe";
	
	public static final String TEST_TEMP_DIR = "test.temp.dir";
	public static final String KEEP_TEMP_DIR = "test.temp.dir.keep";
	
	public static final String AWE_EXE = "test.awe.server.exe";
	public static final String AWEC_EXE = "test.awe.client.exe";
			
	public static void printJava() {
		System.out.println("Java: " + System.getProperty("java.runtime.version"));
	}
	
	private static String getProp(String prop) {
		String p = System.getProperty(prop);
		if (p == null || p.isEmpty()) {
			throw new TestException("Property " + prop +
					" cannot be null or the empty string.");
		}
		return p;
	}
	
	public static String getTempDir() {
		return getProp(TEST_TEMP_DIR);
	}
	
	public static String getMongoExe() {
		return getProp(MONGOEXE);
	}
	
	public static String getShockExe() {
		return getProp(SHOCKEXE);
	}
	
	public static String getAweExe() {
		return getProp(AWE_EXE);
	}
	
	public static String getAweClientExe() {
		return getProp(AWEC_EXE);
	}
	
	public static boolean getDeleteTempFiles() {
		return !"true".equals(System.getProperty(KEEP_TEMP_DIR));
	}
}
