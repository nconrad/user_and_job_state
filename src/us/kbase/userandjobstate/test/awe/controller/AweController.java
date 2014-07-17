package us.kbase.userandjobstate.test.awe.controller;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;


/** Q&D Utility to run an AWE server & client for the purposes of testing from
 * Java.
 * @author gaprice@lbl.gov
 *
 */
public class AweController {
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

	public AweController(
			final URL shockURL,
			final String mongohost,
			final String aweMongoDBname,
			final String mongouser,
			final String mongopwd)
					throws Exception {
		this.shockURL = shockURL;
		tempDir = Files.createTempDirectory("AweController-");
		for(String p: tempDirectories) {
			Files.createDirectories(tempDir.resolve(p));
		}
		final int port = findFreePort();
		
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
}
