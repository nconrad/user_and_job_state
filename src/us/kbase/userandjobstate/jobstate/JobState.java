package us.kbase.userandjobstate.jobstate;

import java.util.Date;
import java.util.List;
import java.util.Set;

import us.kbase.userandjobstate.exceptions.CommunicationException;
import us.kbase.userandjobstate.jobstate.exceptions.NoSuchJobException;

public interface JobState {

	public final static String PROG_NONE = "none";
	public final static String PROG_TASK = "task";
	public final static String PROG_PERC = "percent";

	public String createJob(final String user) throws CommunicationException;

	public Job getJob(final String user, final String jobID)
			throws CommunicationException, NoSuchJobException;

	public void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, final Date estComplete)
			throws CommunicationException, NoSuchJobException;

	public void startJob(final String user, final String jobID,
			final String service, final String status,
			final String description, final int maxProg, final Date estComplete)
			throws CommunicationException, NoSuchJobException;

	public void startJobWithPercentProg(final String user, final String jobID,
			final String service, final String status,
			final String description, final Date estComplete)
			throws CommunicationException, NoSuchJobException;

	public String createAndStartJob(final String user, final String service,
			final String status, final String description,
			final Date estComplete) throws CommunicationException;

	public String createAndStartJob(final String user, final String service,
			final String status, final String description, final int maxProg,
			final Date estComplete) throws CommunicationException;

	public String createAndStartJobWithPercentProg(final String user,
			final String service, final String status,
			final String description, final Date estComplete)
			throws CommunicationException;

	public void updateJob(final String user, final String jobID,
			final String service, final String status, final Integer progress,
			final Date estComplete) throws CommunicationException,
			NoSuchJobException;

	public void completeJob(final String user, final String jobID,
			final String service, final String status, final String error,
			final JobResults results) throws CommunicationException,
			NoSuchJobException;

	public void deleteJob(final String user, final String jobID)
			throws NoSuchJobException, CommunicationException;

	public void deleteJob(final String user, final String jobID,
			final String service) throws NoSuchJobException,
			CommunicationException;

	public Set<String> listServices(final String user)
			throws CommunicationException;

	public List<Job> listJobs(final String user, final List<String> services,
			final boolean queued, final boolean running,
			final boolean complete, final boolean error, final boolean shared)
			throws CommunicationException;

	//note sharing with an already shared user or sharing with the owner has
	//no effect
	public void shareJob(final String owner, final String jobID,
			final List<String> users) throws CommunicationException,
			NoSuchJobException;

	//removing the owner or an unshared user has no effect
	public void unshareJob(final String user, final String jobID,
			final List<String> users) throws CommunicationException,
			NoSuchJobException;

}