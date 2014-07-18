package us.kbase.userandjobstate.test.awe.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;



/** Q&D Utility to run an AWE server & client for the purposes of testing from
 * Java.
 * @author gaprice@lbl.gov
 *
 */
public class AweController {
	
	private final static String AWE_CONFIG_FN = "awe.cfg";
	private final static String AWEC_CONFIG_FN = "awec.cfg";
	
	private final static String AWE_CONFIG =
			"us/kbase/userandjobstate/test/awe/controller/conf/" +
					AWE_CONFIG_FN;
	private final static String AWEC_CONFIG =
			"us/kbase/userandjobstate/test/awe/controller/conf/" +
					AWEC_CONFIG_FN;
	
	private final static List<String> tempDirectories =
			new LinkedList<String>();
	static {
		tempDirectories.add("awe/site");
		tempDirectories.add("awe/data");
		tempDirectories.add("awe/logs");
		tempDirectories.add("awe/awfs");
		tempDirectories.add("awec/data");
		tempDirectories.add("awec/logs");
		tempDirectories.add("awec/work");
	}
	
	private final Path tempDir;
	
	private final URL shockURL;
	
	private final Process awe;
	private final Process awec;
	private final int port;
	private final boolean deleteTempDirOnExit;

	public AweController(
			final URL shockURL,
			final String aweExe,
			final String aweClientExe,
			final String mongohost,
			final String aweMongoDBname,
			final String mongouser,
			final String mongopwd,
			final boolean deleteTempDirOnExit)
					throws Exception {
		this.shockURL = shockURL;
		tempDir = makeTempDirs();
		port = findFreePort();
		this.deleteTempDirOnExit = deleteTempDirOnExit;
		
		checkExe(aweExe, "awe server");
		checkExe(aweExe, "awe client");
		
		Velocity.init();
		VelocityContext context = new VelocityContext();
		context.put("port", port);
		context.put("tempdir", tempDir.toString());
		context.put("mongohost", mongohost);
		context.put("mongodbname", aweMongoDBname);
		context.put("mongouser", mongouser == null ? "" : mongouser);
		context.put("mongopwd", mongopwd == null ? "" : mongopwd);
		
		File awecfg = tempDir.resolve(AWE_CONFIG_FN).toFile();
		File aweclicfg = tempDir.resolve(AWEC_CONFIG_FN).toFile();
		
		generateConfig(AWE_CONFIG, context, awecfg);
		generateConfig(AWEC_CONFIG, context, aweclicfg);

		awe = new ProcessBuilder(aweExe, "--conf", awecfg.toString())
				.redirectErrorStream(true)
				.redirectOutput(tempDir.resolve("awe_server.log").toFile())
				.start();
		awec = new ProcessBuilder(aweClientExe, "--conf", aweclicfg.toString())
				.redirectErrorStream(true)
				.redirectOutput(tempDir.resolve("awe_client.log").toFile())
				.start();
	}
	
	public int getServerPort() {
		return port;
	}
	
	public void destroy() throws IOException {
		if (awe != null) {
			awe.destroy();
		}
		if (awec != null) {
			awec.destroy();
		}
		if (tempDir != null && deleteTempDirOnExit) {
			FileUtils.deleteDirectory(tempDir.toFile());
		}
	}

	private void checkExe(String aweExe, String exeType) {
		File e = new File(aweExe);
		if (!e.exists()) {
			throw new IllegalArgumentException("The provided " + exeType +
					" executable does not exist:" + aweExe);
		}
		if (!e.isFile()) {
			throw new IllegalArgumentException("The provided " + exeType +
					" executable is not a file:" + aweExe);
		}
		if (!e.canExecute()) {
			throw new IllegalArgumentException("The provided " + exeType +
					" executable is not executable:" + aweExe);
		}
		
	}


	private Path makeTempDirs() throws IOException {
		Set<PosixFilePermission> perms =
				PosixFilePermissions.fromString("rwx------");
		FileAttribute<Set<PosixFilePermission>> attr =
				PosixFilePermissions.asFileAttribute(perms);
		Path tempDir = Files.createTempDirectory("AweController-", attr);
		for(String p: tempDirectories) {
			Files.createDirectories(tempDir.resolve(p));
		}
		return tempDir;
	}

	private void generateConfig(final String configName,
			final VelocityContext context, File file)
			throws IOException {
		String template = IOUtils.toString(new BufferedReader(
				new InputStreamReader(
						getClass().getClassLoader()
						.getResourceAsStream(configName))));
		
		StringWriter sw = new StringWriter();
		Velocity.evaluate(context, sw, "aweconfig", template);
		PrintWriter pw = new PrintWriter(file);
		pw.write(sw.toString());
		pw.close();
	}

	/** See https://gist.github.com/vorburger/3429822
	 * Returns a free port number on localhost.
	 *
	 * Heavily inspired from org.eclipse.jdt.launching.SocketUtil (to avoid a
	 * dependency to JDT just because of this).
	 * Slightly improved with close() missing in JDT. And throws exception
	 * instead of returning -1.
	 *
	 * @return a free port number on localhost
	 * @throws IllegalStateException if unable to find a free port
	 */
	private static int findFreePort() {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0);
			socket.setReuseAddress(true);
			int port = socket.getLocalPort();
			try {
				socket.close();
			} catch (IOException e) {
				// Ignore IOException on close()
			}
			return port;
		} catch (IOException e) {
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
		throw new IllegalStateException(
				"Could not find a free TCP/IP port");
	}
	
	public static void main(String[] args) throws Exception {
		AweController ac = new AweController(
				new URL("http://localhost:7044"),
				"/kb/deployment/bin/awe-server",
				"/kb/deployment/bin/awe-client",
				"localhost",
				"delete_awe_db",
				"foo", "foo", false);
		Scanner reader = new Scanner(System.in);
		System.out.println("any char to shut down");
		//get user input for a
		String a=reader.next();
		ac.destroy();
	}
}
