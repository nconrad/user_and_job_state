package us.kbase.userandjobstate.awe;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenExpiredException;
import us.kbase.common.exceptions.UnimplementedException;
import us.kbase.userandjobstate.awe.client.AweJob;
import us.kbase.userandjobstate.awe.client.AweJobId;
import us.kbase.userandjobstate.awe.client.BasicAweClient;
import us.kbase.userandjobstate.awe.client.exceptions.AweAuthorizationException;
import us.kbase.userandjobstate.awe.client.exceptions.AweHttpException;
import us.kbase.userandjobstate.awe.client.exceptions.AweNoJobException;
import us.kbase.userandjobstate.awe.client.exceptions.InvalidAweUrlException;
import us.kbase.userandjobstate.exceptions.CommunicationException;
import us.kbase.userandjobstate.jobstate.Job;
import us.kbase.userandjobstate.jobstate.JobResults;
import us.kbase.userandjobstate.jobstate.JobState;
import us.kbase.userandjobstate.jobstate.exceptions.NoSuchJobException;

public class AweJobState implements JobState {

	private final BasicAweClient cli;
	
	public AweJobState(final URL url, final AuthToken token)
			throws TokenExpiredException, InvalidAweUrlException,
			AweHttpException, IOException {
		if (token == null || url == null) {
			throw new NullPointerException("Url and token cannot be null");
		}
		cli = new BasicAweClient(url, token);
	}
	
	public static void testURL(final URL url)
			throws InvalidAweUrlException, IOException {
		new BasicAweClient(url);
	}
	
	private String getUserName() {
		return cli.getToken().getUserName();
	}

	@Override
	public String createJob(final String user) throws CommunicationException {
		throw new IllegalAweOperationException(
				"Awe jobs must be created and / " +
				"or started from the awe server.");
	}

	//user is ignored, uses the auth token
	@Override
	public Job getJob(final String user, final String jobID)
			throws CommunicationException, NoSuchJobException {
		final AweJob aj;
		try {
			aj = cli.getJob(new AweJobId(jobID));
		} catch (TokenExpiredException e) {
			throw new CommunicationException("Authorization token for user " +
					getUserName() + "is expired", e);
		} catch (AweAuthorizationException e) {
			throw new NoSuchJobException(String.format(
					"There is no job %s viewable by user %s", jobID,
					getUserName(), e));
		} catch (AweNoJobException e) {
			throw new NoSuchJobException(String.format(
					"There is no job %s viewable by user %s", jobID,
					getUserName(), e));
		} catch (AweHttpException e) {
			throw new CommunicationException(
					"Could not retrieve job from the Awe server: " +
					e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"Invalid Awe job ID: " + jobID, e);
		} catch (IOException e) {
			throw new CommunicationException(
					"Could not retrieve job from the Awe server: " +
					e.getMessage(), e);
		}
		return new UJSAweJob(aj);
	}

	@Override
	public void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, final Date estComplete)
			throws CommunicationException, NoSuchJobException {
		throw new IllegalAweOperationException(
				"Awe jobs must be created and / " +
				"or started from the awe server.");

	}

	@Override
	public void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, final int maxProg,
			final Date estComplete)
			throws CommunicationException, NoSuchJobException {
		throw new IllegalAweOperationException(
				"Awe jobs must be created and / " +
				"or started from the awe server.");
	}

	@Override
	public void startJobWithPercentProg(final String user, final String jobID,
			final String service, final String status,
			final String description, final Date estComplete)
			throws CommunicationException, NoSuchJobException {
		throw new IllegalAweOperationException(
				"Awe jobs must be created and / " +
				"or started from the awe server.");
	}

	@Override
	public String createAndStartJob(final String user, final String service,
			final String status, final String description,
			final Date estComplete) throws CommunicationException {
		throw new IllegalAweOperationException(
				"Awe jobs must be created and / " +
				"or started from the awe server.");
	}

	@Override
	public String createAndStartJob(final String user, final String service,
			final String status, final String description, final int maxProg,
			final Date estComplete)
			throws CommunicationException {
		throw new IllegalAweOperationException(
				"Awe jobs must be created and / " +
				"or started from the awe server.");
	}

	@Override
	public String createAndStartJobWithPercentProg(final String user,
			final String service, final String status,
			final String description, final Date estComplete)
			throws CommunicationException {
		throw new IllegalAweOperationException(
				"Awe jobs must be created and / " +
				"or started from the awe server.");
	}

	@Override
	public void updateJob(String user, String jobID, String service,
			String status, Integer progress, Date estComplete)
			throws CommunicationException, NoSuchJobException {
		throw new IllegalAweOperationException(
				"Awe jobs cannot be manually updated.");
	}

	@Override
	public void completeJob(final String user, final String jobID,
			final String service, final String status, final String error,
			final JobResults results)
			throws CommunicationException, NoSuchJobException {
		throw new IllegalAweOperationException(
				"Awe jobs cannot be manually completed.");

	}

	@Override
	public void deleteJob(final String user, final String jobID)
			throws NoSuchJobException,
			CommunicationException {
		//TODO delete AWE jobs
		throw new UnimplementedException(
				"Deleting Awe jobs via the UJS is not yet implemented.");

	}

	@Override
	public void deleteJob(final String user, final String jobID,
			final String service)
			throws NoSuchJobException, CommunicationException {
		//TODO delete AWE jobs
		throw new UnimplementedException(
				"Deleting Awe jobs via the UJS is not yet implemented.");
	}

	@Override
	public Set<String> listServices(final String user) throws CommunicationException {
		// TODO List services when AWE supports distinct query
		return new HashSet<String>();
	}

	//user is ignored, uses the auth token
	@Override
	public List<Job> listJobs(final String user,
			final List<String> services,
			final boolean running,
			final boolean complete,
			final boolean error,
			final boolean shared) //TODO WAIT deal with filtering on shared 
			throws CommunicationException {
		final List<AweJob> jobs;
		try {
			jobs = cli.getJobs(services); //TODO 1 deal with filters
		} catch (TokenExpiredException e) {
			throw new CommunicationException("Authorization token for user " +
					getUserName() + "is expired", e);
		} catch (AweAuthorizationException e) {
			throw new CommunicationException(String.format(
					"The Awe server could not authorize user %s",
					getUserName(), e));
		} catch (AweHttpException e) {
			throw new CommunicationException(
					"Could not retrieve jobs from the Awe server: " +
					e.getMessage(), e);
		} catch (IOException e) {
			throw new CommunicationException(
					"Could not retrieve jobs from the Awe server: " +
					e.getMessage(), e);
		}
		final List<Job> js = new LinkedList<Job>();
		for (final AweJob j: jobs) {
			js.add(new UJSAweJob(j));
		}
		return js;
	}

	@Override
	public void shareJob(final String owner, final String jobID,
			final List<String> users)
			throws CommunicationException, NoSuchJobException {
		// TODO 1 WAIT share awe job

	}

	@Override
	public void unshareJob(final String user, final String jobID,
			final List<String> users)
			throws CommunicationException, NoSuchJobException {
		// TODO 1 WAIT unshare awe job

	}
	
	@SuppressWarnings("serial")
	class IllegalAweOperationException extends RuntimeException {

		public IllegalAweOperationException(String message) {
			super(message);
		}
	}
	
	
}
