package Bio::KBase::userandjobstate::Client;

use JSON::RPC::Client;
use strict;
use Data::Dumper;
use URI;
use Bio::KBase::Exceptions;
use Bio::KBase::AuthToken;

# Client version should match Impl version
# This is a Semantic Version number,
# http://semver.org
our $VERSION = "0.1.0";

=head1 NAME

Bio::KBase::userandjobstate::Client

=head1 DESCRIPTION


Service for storing arbitrary key value pairs on a per user per service basis
and storing job status so that a) long JSON RPC calls can report status and
UI elements can receive updates, and b) there's a centralized location for 
job status reporting.

The service assumes other services are capable of simple math and does not
throw errors if a progress bar overflows.

Since there is no way to authenticate as a service, devs are on the honor
system not to clobber each other's settings and jobs.

Potential process flows:

Asysnc:
UI calls service function which returns with job id
service call [spawns thread/subprocess to run job that] periodically updates
        the job status of the job id on the job status server
meanwhile, the UI periodically polls the job status server to get progress
        updates
service call finishes, completes job
UI pulls pointers to results from the job status server

Sync:
UI creates job, gets job id
UI starts thread that calls service, providing job id
service call runs, periodically updating the job status of the job id on the
        job status server
meanwhile, the UI periodically polls the job status server to get progress
        updates
service call finishes, completes job, returns results
UI thread joins

mongodb structures:

State collection:
{
        _id:
        user:
        service:
        key: (unique index on user/service/key)
        value:
}

Job collection:
{
        _id:
        user:
        service:
        desc:
        progtype: ('percent', 'task', 'none')
        prog: (int)
        maxprog: (int, 100 for percent, user specified for task)
        status: (user supplied string)
        updated: (date)
        complete: (bool) (index on user/service/complete)
        error: (bool)
        result: {
                shocknodes: (list of strings)
                shockurl:
                workspaceids: (list of strings)
                workspaceurl:
        }
}


=cut

sub new
{
    my($class, $url, @args) = @_;
    
    if (!defined($url))
    {
	$url = 'http://kbase.us/services/userandjobstate/';
    }

    my $self = {
	client => Bio::KBase::userandjobstate::Client::RpcClient->new,
	url => $url,
    };

    #
    # This module requires authentication.
    #
    # We create an auth token, passing through the arguments that we were (hopefully) given.

    {
	my $token = Bio::KBase::AuthToken->new(@args);
	
	if (!$token->error_message)
	{
	    $self->{token} = $token->token;
	    $self->{client}->{token} = $token->token;
	}
        else
        {
	    #
	    # All methods in this module require authentication. In this case, if we
	    # don't have a token, we can't continue.
	    #
	    die "Authentication failed: " . $token->error_message;
	}
    }

    my $ua = $self->{client}->ua;	 
    my $timeout = $ENV{CDMI_TIMEOUT} || (30 * 60);	 
    $ua->timeout($timeout);
    bless $self, $class;
    #    $self->_validate_version();
    return $self;
}




=head2 set_state

  $obj->set_state($service, $key, $value)

=over 4

=item Parameter and return types

=begin html

<pre>
$service is a UserAndJobState.service_name
$key is a string
$value is a string
service_name is a string

</pre>

=end html

=begin text

$service is a UserAndJobState.service_name
$key is a string
$value is a string
service_name is a string


=end text

=item Description

Set the state of a key for a service.

=back

=cut

sub set_state
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 3)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function set_state (received $n, expecting 3)");
    }
    {
	my($service, $key, $value) = @args;

	my @_bad_arguments;
        (!ref($service)) or push(@_bad_arguments, "Invalid type for argument 1 \"service\" (value was \"$service\")");
        (!ref($key)) or push(@_bad_arguments, "Invalid type for argument 2 \"key\" (value was \"$key\")");
        (!ref($value)) or push(@_bad_arguments, "Invalid type for argument 3 \"value\" (value was \"$value\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to set_state:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'set_state');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.set_state",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'set_state',
					      );
	} else {
	    return;
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method set_state",
					    status_line => $self->{client}->status_line,
					    method_name => 'set_state',
				       );
    }
}



=head2 get_state

  $value = $obj->get_state($service, $key)

=over 4

=item Parameter and return types

=begin html

<pre>
$service is a UserAndJobState.service_name
$key is a string
$value is a string
service_name is a string

</pre>

=end html

=begin text

$service is a UserAndJobState.service_name
$key is a string
$value is a string
service_name is a string


=end text

=item Description

Get the state of a key for a service.

=back

=cut

sub get_state
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 2)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function get_state (received $n, expecting 2)");
    }
    {
	my($service, $key) = @args;

	my @_bad_arguments;
        (!ref($service)) or push(@_bad_arguments, "Invalid type for argument 1 \"service\" (value was \"$service\")");
        (!ref($key)) or push(@_bad_arguments, "Invalid type for argument 2 \"key\" (value was \"$key\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to get_state:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'get_state');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.get_state",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'get_state',
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method get_state",
					    status_line => $self->{client}->status_line,
					    method_name => 'get_state',
				       );
    }
}



=head2 remove_state

  $obj->remove_state($service, $key)

=over 4

=item Parameter and return types

=begin html

<pre>
$service is a UserAndJobState.service_name
$key is a string
service_name is a string

</pre>

=end html

=begin text

$service is a UserAndJobState.service_name
$key is a string
service_name is a string


=end text

=item Description

Remove a key value pair.

=back

=cut

sub remove_state
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 2)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function remove_state (received $n, expecting 2)");
    }
    {
	my($service, $key) = @args;

	my @_bad_arguments;
        (!ref($service)) or push(@_bad_arguments, "Invalid type for argument 1 \"service\" (value was \"$service\")");
        (!ref($key)) or push(@_bad_arguments, "Invalid type for argument 2 \"key\" (value was \"$key\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to remove_state:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'remove_state');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.remove_state",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'remove_state',
					      );
	} else {
	    return;
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method remove_state",
					    status_line => $self->{client}->status_line,
					    method_name => 'remove_state',
				       );
    }
}



=head2 list_state

  $key_value_pairs = $obj->list_state($service)

=over 4

=item Parameter and return types

=begin html

<pre>
$service is a UserAndJobState.service_name
$key_value_pairs is a reference to a hash where the key is a string and the value is a string
service_name is a string

</pre>

=end html

=begin text

$service is a UserAndJobState.service_name
$key_value_pairs is a reference to a hash where the key is a string and the value is a string
service_name is a string


=end text

=item Description

List all key value pairs.

=back

=cut

sub list_state
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function list_state (received $n, expecting 1)");
    }
    {
	my($service) = @args;

	my @_bad_arguments;
        (!ref($service)) or push(@_bad_arguments, "Invalid type for argument 1 \"service\" (value was \"$service\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to list_state:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'list_state');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.list_state",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'list_state',
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method list_state",
					    status_line => $self->{client}->status_line,
					    method_name => 'list_state',
				       );
    }
}



=head2 create_job

  $job = $obj->create_job()

=over 4

=item Parameter and return types

=begin html

<pre>
$job is a UserAndJobState.job_id
job_id is a string

</pre>

=end html

=begin text

$job is a UserAndJobState.job_id
job_id is a string


=end text

=item Description

Create a new job status report.

=back

=cut

sub create_job
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 0)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function create_job (received $n, expecting 0)");
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.create_job",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'create_job',
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method create_job",
					    status_line => $self->{client}->status_line,
					    method_name => 'create_job',
				       );
    }
}



=head2 start_job

  $obj->start_job($job, $service, $status, $desc, $progress)

=over 4

=item Parameter and return types

=begin html

<pre>
$job is a UserAndJobState.job_id
$service is a UserAndJobState.service_name
$status is a UserAndJobState.job_status
$desc is a UserAndJobState.job_description
$progress is a UserAndJobState.InitProgress
job_id is a string
service_name is a string
job_status is a string
job_description is a string
InitProgress is a reference to a hash where the following keys are defined:
	ptype has a value which is a UserAndJobState.progress_type
	max has a value which is a UserAndJobState.max_progress
progress_type is a string
max_progress is an int

</pre>

=end html

=begin text

$job is a UserAndJobState.job_id
$service is a UserAndJobState.service_name
$status is a UserAndJobState.job_status
$desc is a UserAndJobState.job_description
$progress is a UserAndJobState.InitProgress
job_id is a string
service_name is a string
job_status is a string
job_description is a string
InitProgress is a reference to a hash where the following keys are defined:
	ptype has a value which is a UserAndJobState.progress_type
	max has a value which is a UserAndJobState.max_progress
progress_type is a string
max_progress is an int


=end text

=item Description

Start a job and specify the job parameters.

=back

=cut

sub start_job
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 5)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function start_job (received $n, expecting 5)");
    }
    {
	my($job, $service, $status, $desc, $progress) = @args;

	my @_bad_arguments;
        (!ref($job)) or push(@_bad_arguments, "Invalid type for argument 1 \"job\" (value was \"$job\")");
        (!ref($service)) or push(@_bad_arguments, "Invalid type for argument 2 \"service\" (value was \"$service\")");
        (!ref($status)) or push(@_bad_arguments, "Invalid type for argument 3 \"status\" (value was \"$status\")");
        (!ref($desc)) or push(@_bad_arguments, "Invalid type for argument 4 \"desc\" (value was \"$desc\")");
        (ref($progress) eq 'HASH') or push(@_bad_arguments, "Invalid type for argument 5 \"progress\" (value was \"$progress\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to start_job:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'start_job');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.start_job",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'start_job',
					      );
	} else {
	    return;
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method start_job",
					    status_line => $self->{client}->status_line,
					    method_name => 'start_job',
				       );
    }
}



=head2 create_and_start_job

  $job = $obj->create_and_start_job($service, $status, $desc, $progress)

=over 4

=item Parameter and return types

=begin html

<pre>
$service is a UserAndJobState.service_name
$status is a UserAndJobState.job_status
$desc is a UserAndJobState.job_description
$progress is a UserAndJobState.InitProgress
$job is a UserAndJobState.job_id
service_name is a string
job_status is a string
job_description is a string
InitProgress is a reference to a hash where the following keys are defined:
	ptype has a value which is a UserAndJobState.progress_type
	max has a value which is a UserAndJobState.max_progress
progress_type is a string
max_progress is an int
job_id is a string

</pre>

=end html

=begin text

$service is a UserAndJobState.service_name
$status is a UserAndJobState.job_status
$desc is a UserAndJobState.job_description
$progress is a UserAndJobState.InitProgress
$job is a UserAndJobState.job_id
service_name is a string
job_status is a string
job_description is a string
InitProgress is a reference to a hash where the following keys are defined:
	ptype has a value which is a UserAndJobState.progress_type
	max has a value which is a UserAndJobState.max_progress
progress_type is a string
max_progress is an int
job_id is a string


=end text

=item Description

Create and start a job.

=back

=cut

sub create_and_start_job
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 4)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function create_and_start_job (received $n, expecting 4)");
    }
    {
	my($service, $status, $desc, $progress) = @args;

	my @_bad_arguments;
        (!ref($service)) or push(@_bad_arguments, "Invalid type for argument 1 \"service\" (value was \"$service\")");
        (!ref($status)) or push(@_bad_arguments, "Invalid type for argument 2 \"status\" (value was \"$status\")");
        (!ref($desc)) or push(@_bad_arguments, "Invalid type for argument 3 \"desc\" (value was \"$desc\")");
        (ref($progress) eq 'HASH') or push(@_bad_arguments, "Invalid type for argument 4 \"progress\" (value was \"$progress\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to create_and_start_job:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'create_and_start_job');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.create_and_start_job",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'create_and_start_job',
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method create_and_start_job",
					    status_line => $self->{client}->status_line,
					    method_name => 'create_and_start_job',
				       );
    }
}



=head2 update_job_progress

  $obj->update_job_progress($job, $status, $prog)

=over 4

=item Parameter and return types

=begin html

<pre>
$job is a UserAndJobState.job_id
$status is a UserAndJobState.job_status
$prog is a UserAndJobState.progress
job_id is a string
job_status is a string
progress is an int

</pre>

=end html

=begin text

$job is a UserAndJobState.job_id
$status is a UserAndJobState.job_status
$prog is a UserAndJobState.progress
job_id is a string
job_status is a string
progress is an int


=end text

=item Description

Update the status and progress for a job.

=back

=cut

sub update_job_progress
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 3)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function update_job_progress (received $n, expecting 3)");
    }
    {
	my($job, $status, $prog) = @args;

	my @_bad_arguments;
        (!ref($job)) or push(@_bad_arguments, "Invalid type for argument 1 \"job\" (value was \"$job\")");
        (!ref($status)) or push(@_bad_arguments, "Invalid type for argument 2 \"status\" (value was \"$status\")");
        (!ref($prog)) or push(@_bad_arguments, "Invalid type for argument 3 \"prog\" (value was \"$prog\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to update_job_progress:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'update_job_progress');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.update_job_progress",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'update_job_progress',
					      );
	} else {
	    return;
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method update_job_progress",
					    status_line => $self->{client}->status_line,
					    method_name => 'update_job_progress',
				       );
    }
}



=head2 update_job

  $obj->update_job($job, $status)

=over 4

=item Parameter and return types

=begin html

<pre>
$job is a UserAndJobState.job_id
$status is a UserAndJobState.job_status
job_id is a string
job_status is a string

</pre>

=end html

=begin text

$job is a UserAndJobState.job_id
$status is a UserAndJobState.job_status
job_id is a string
job_status is a string


=end text

=item Description

Update the status for a job.

=back

=cut

sub update_job
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 2)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function update_job (received $n, expecting 2)");
    }
    {
	my($job, $status) = @args;

	my @_bad_arguments;
        (!ref($job)) or push(@_bad_arguments, "Invalid type for argument 1 \"job\" (value was \"$job\")");
        (!ref($status)) or push(@_bad_arguments, "Invalid type for argument 2 \"status\" (value was \"$status\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to update_job:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'update_job');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.update_job",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'update_job',
					      );
	} else {
	    return;
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method update_job",
					    status_line => $self->{client}->status_line,
					    method_name => 'update_job',
				       );
    }
}



=head2 get_job_description

  $service, $ptype, $max, $desc = $obj->get_job_description($job)

=over 4

=item Parameter and return types

=begin html

<pre>
$job is a UserAndJobState.job_id
$service is a UserAndJobState.service_name
$ptype is a UserAndJobState.progress_type
$max is a UserAndJobState.max_progress
$desc is a UserAndJobState.job_description
job_id is a string
service_name is a string
progress_type is a string
max_progress is an int
job_description is a string

</pre>

=end html

=begin text

$job is a UserAndJobState.job_id
$service is a UserAndJobState.service_name
$ptype is a UserAndJobState.progress_type
$max is a UserAndJobState.max_progress
$desc is a UserAndJobState.job_description
job_id is a string
service_name is a string
progress_type is a string
max_progress is an int
job_description is a string


=end text

=item Description

Get the description of a job.

=back

=cut

sub get_job_description
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function get_job_description (received $n, expecting 1)");
    }
    {
	my($job) = @args;

	my @_bad_arguments;
        (!ref($job)) or push(@_bad_arguments, "Invalid type for argument 1 \"job\" (value was \"$job\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to get_job_description:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'get_job_description');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.get_job_description",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'get_job_description',
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method get_job_description",
					    status_line => $self->{client}->status_line,
					    method_name => 'get_job_description',
				       );
    }
}



=head2 get_job_status

  $last_update, $status, $progress, $complete, $error = $obj->get_job_status($job)

=over 4

=item Parameter and return types

=begin html

<pre>
$job is a UserAndJobState.job_id
$last_update is a UserAndJobState.timestamp
$status is a UserAndJobState.job_status
$progress is a UserAndJobState.total_progress
$complete is a UserAndJobState.boolean
$error is a UserAndJobState.boolean
job_id is a string
timestamp is a string
job_status is a string
total_progress is an int
boolean is an int

</pre>

=end html

=begin text

$job is a UserAndJobState.job_id
$last_update is a UserAndJobState.timestamp
$status is a UserAndJobState.job_status
$progress is a UserAndJobState.total_progress
$complete is a UserAndJobState.boolean
$error is a UserAndJobState.boolean
job_id is a string
timestamp is a string
job_status is a string
total_progress is an int
boolean is an int


=end text

=item Description

Get the status of a job. 
If the progress type is 'none' total_progress will always be 0.

=back

=cut

sub get_job_status
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function get_job_status (received $n, expecting 1)");
    }
    {
	my($job) = @args;

	my @_bad_arguments;
        (!ref($job)) or push(@_bad_arguments, "Invalid type for argument 1 \"job\" (value was \"$job\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to get_job_status:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'get_job_status');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.get_job_status",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'get_job_status',
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method get_job_status",
					    status_line => $self->{client}->status_line,
					    method_name => 'get_job_status',
				       );
    }
}



=head2 complete_job

  $obj->complete_job($job, $status, $error, $res)

=over 4

=item Parameter and return types

=begin html

<pre>
$job is a UserAndJobState.job_id
$status is a UserAndJobState.job_status
$error is a UserAndJobState.boolean
$res is a UserAndJobState.Results
job_id is a string
job_status is a string
boolean is an int
Results is a reference to a hash where the following keys are defined:
	shocknodes has a value which is a reference to a list where each element is a string
	shockurl has a value which is a string
	workspaceids has a value which is a reference to a list where each element is a string
	workspaceurl has a value which is a string

</pre>

=end html

=begin text

$job is a UserAndJobState.job_id
$status is a UserAndJobState.job_status
$error is a UserAndJobState.boolean
$res is a UserAndJobState.Results
job_id is a string
job_status is a string
boolean is an int
Results is a reference to a hash where the following keys are defined:
	shocknodes has a value which is a reference to a list where each element is a string
	shockurl has a value which is a string
	workspaceids has a value which is a reference to a list where each element is a string
	workspaceurl has a value which is a string


=end text

=item Description

Complete the job. After the job is completed, total_progress always
equals max_progress.

=back

=cut

sub complete_job
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 4)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function complete_job (received $n, expecting 4)");
    }
    {
	my($job, $status, $error, $res) = @args;

	my @_bad_arguments;
        (!ref($job)) or push(@_bad_arguments, "Invalid type for argument 1 \"job\" (value was \"$job\")");
        (!ref($status)) or push(@_bad_arguments, "Invalid type for argument 2 \"status\" (value was \"$status\")");
        (!ref($error)) or push(@_bad_arguments, "Invalid type for argument 3 \"error\" (value was \"$error\")");
        (ref($res) eq 'HASH') or push(@_bad_arguments, "Invalid type for argument 4 \"res\" (value was \"$res\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to complete_job:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'complete_job');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.complete_job",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'complete_job',
					      );
	} else {
	    return;
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method complete_job",
					    status_line => $self->{client}->status_line,
					    method_name => 'complete_job',
				       );
    }
}



=head2 get_results

  $res = $obj->get_results($job)

=over 4

=item Parameter and return types

=begin html

<pre>
$job is a UserAndJobState.job_id
$res is a UserAndJobState.Results
job_id is a string
Results is a reference to a hash where the following keys are defined:
	shocknodes has a value which is a reference to a list where each element is a string
	shockurl has a value which is a string
	workspaceids has a value which is a reference to a list where each element is a string
	workspaceurl has a value which is a string

</pre>

=end html

=begin text

$job is a UserAndJobState.job_id
$res is a UserAndJobState.Results
job_id is a string
Results is a reference to a hash where the following keys are defined:
	shocknodes has a value which is a reference to a list where each element is a string
	shockurl has a value which is a string
	workspaceids has a value which is a reference to a list where each element is a string
	workspaceurl has a value which is a string


=end text

=item Description

Get the job results.

=back

=cut

sub get_results
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function get_results (received $n, expecting 1)");
    }
    {
	my($job) = @args;

	my @_bad_arguments;
        (!ref($job)) or push(@_bad_arguments, "Invalid type for argument 1 \"job\" (value was \"$job\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to get_results:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'get_results');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.get_results",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'get_results',
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method get_results",
					    status_line => $self->{client}->status_line,
					    method_name => 'get_results',
				       );
    }
}



=head2 get_services

  $services = $obj->get_services()

=over 4

=item Parameter and return types

=begin html

<pre>
$services is a reference to a list where each element is a UserAndJobState.service_name
service_name is a string

</pre>

=end html

=begin text

$services is a reference to a list where each element is a UserAndJobState.service_name
service_name is a string


=end text

=item Description

List service names.

=back

=cut

sub get_services
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 0)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function get_services (received $n, expecting 0)");
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.get_services",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'get_services',
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method get_services",
					    status_line => $self->{client}->status_line,
					    method_name => 'get_services',
				       );
    }
}



=head2 get_job_info

  $info = $obj->get_job_info($job)

=over 4

=item Parameter and return types

=begin html

<pre>
$job is a UserAndJobState.job_id
$info is a UserAndJobState.job_info
job_id is a string
job_info is a reference to a list containing 11 items:
	0: (job) a UserAndJobState.job_id
	1: (service) a UserAndJobState.service_name
	2: (status) a UserAndJobState.job_status
	3: (last_update) a UserAndJobState.timestamp
	4: (prog) a UserAndJobState.total_progress
	5: (max) a UserAndJobState.max_progress
	6: (ptype) a UserAndJobState.progress_type
	7: (complete) a UserAndJobState.boolean
	8: (error) a UserAndJobState.boolean
	9: (desc) a UserAndJobState.job_description
	10: (res) a UserAndJobState.Results
service_name is a string
job_status is a string
timestamp is a string
total_progress is an int
max_progress is an int
progress_type is a string
boolean is an int
job_description is a string
Results is a reference to a hash where the following keys are defined:
	shocknodes has a value which is a reference to a list where each element is a string
	shockurl has a value which is a string
	workspaceids has a value which is a reference to a list where each element is a string
	workspaceurl has a value which is a string

</pre>

=end html

=begin text

$job is a UserAndJobState.job_id
$info is a UserAndJobState.job_info
job_id is a string
job_info is a reference to a list containing 11 items:
	0: (job) a UserAndJobState.job_id
	1: (service) a UserAndJobState.service_name
	2: (status) a UserAndJobState.job_status
	3: (last_update) a UserAndJobState.timestamp
	4: (prog) a UserAndJobState.total_progress
	5: (max) a UserAndJobState.max_progress
	6: (ptype) a UserAndJobState.progress_type
	7: (complete) a UserAndJobState.boolean
	8: (error) a UserAndJobState.boolean
	9: (desc) a UserAndJobState.job_description
	10: (res) a UserAndJobState.Results
service_name is a string
job_status is a string
timestamp is a string
total_progress is an int
max_progress is an int
progress_type is a string
boolean is an int
job_description is a string
Results is a reference to a hash where the following keys are defined:
	shocknodes has a value which is a reference to a list where each element is a string
	shockurl has a value which is a string
	workspaceids has a value which is a reference to a list where each element is a string
	workspaceurl has a value which is a string


=end text

=item Description

Get information about a job.

=back

=cut

sub get_job_info
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function get_job_info (received $n, expecting 1)");
    }
    {
	my($job) = @args;

	my @_bad_arguments;
        (!ref($job)) or push(@_bad_arguments, "Invalid type for argument 1 \"job\" (value was \"$job\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to get_job_info:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'get_job_info');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.get_job_info",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'get_job_info',
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method get_job_info",
					    status_line => $self->{client}->status_line,
					    method_name => 'get_job_info',
				       );
    }
}



=head2 list_jobs

  $jobs = $obj->list_jobs($service, $options)

=over 4

=item Parameter and return types

=begin html

<pre>
$service is a UserAndJobState.service_name
$options is a UserAndJobState.ListJobsOptions
$jobs is a reference to a list where each element is a UserAndJobState.job_info
service_name is a string
ListJobsOptions is a reference to a hash where the following keys are defined:
	oldest_first has a value which is a UserAndJobState.boolean
	limit has a value which is an int
	offset has a value which is an int
	completed has a value which is a UserAndJobState.boolean
	error_only has a value which is a UserAndJobState.boolean
boolean is an int
job_info is a reference to a list containing 11 items:
	0: (job) a UserAndJobState.job_id
	1: (service) a UserAndJobState.service_name
	2: (status) a UserAndJobState.job_status
	3: (last_update) a UserAndJobState.timestamp
	4: (prog) a UserAndJobState.total_progress
	5: (max) a UserAndJobState.max_progress
	6: (ptype) a UserAndJobState.progress_type
	7: (complete) a UserAndJobState.boolean
	8: (error) a UserAndJobState.boolean
	9: (desc) a UserAndJobState.job_description
	10: (res) a UserAndJobState.Results
job_id is a string
job_status is a string
timestamp is a string
total_progress is an int
max_progress is an int
progress_type is a string
job_description is a string
Results is a reference to a hash where the following keys are defined:
	shocknodes has a value which is a reference to a list where each element is a string
	shockurl has a value which is a string
	workspaceids has a value which is a reference to a list where each element is a string
	workspaceurl has a value which is a string

</pre>

=end html

=begin text

$service is a UserAndJobState.service_name
$options is a UserAndJobState.ListJobsOptions
$jobs is a reference to a list where each element is a UserAndJobState.job_info
service_name is a string
ListJobsOptions is a reference to a hash where the following keys are defined:
	oldest_first has a value which is a UserAndJobState.boolean
	limit has a value which is an int
	offset has a value which is an int
	completed has a value which is a UserAndJobState.boolean
	error_only has a value which is a UserAndJobState.boolean
boolean is an int
job_info is a reference to a list containing 11 items:
	0: (job) a UserAndJobState.job_id
	1: (service) a UserAndJobState.service_name
	2: (status) a UserAndJobState.job_status
	3: (last_update) a UserAndJobState.timestamp
	4: (prog) a UserAndJobState.total_progress
	5: (max) a UserAndJobState.max_progress
	6: (ptype) a UserAndJobState.progress_type
	7: (complete) a UserAndJobState.boolean
	8: (error) a UserAndJobState.boolean
	9: (desc) a UserAndJobState.job_description
	10: (res) a UserAndJobState.Results
job_id is a string
job_status is a string
timestamp is a string
total_progress is an int
max_progress is an int
progress_type is a string
job_description is a string
Results is a reference to a hash where the following keys are defined:
	shocknodes has a value which is a reference to a list where each element is a string
	shockurl has a value which is a string
	workspaceids has a value which is a reference to a list where each element is a string
	workspaceurl has a value which is a string


=end text

=item Description

List jobs.

=back

=cut

sub list_jobs
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 2)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function list_jobs (received $n, expecting 2)");
    }
    {
	my($service, $options) = @args;

	my @_bad_arguments;
        (!ref($service)) or push(@_bad_arguments, "Invalid type for argument 1 \"service\" (value was \"$service\")");
        (ref($options) eq 'HASH') or push(@_bad_arguments, "Invalid type for argument 2 \"options\" (value was \"$options\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to list_jobs:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'list_jobs');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.list_jobs",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'list_jobs',
					      );
	} else {
	    return wantarray ? @{$result->result} : $result->result->[0];
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method list_jobs",
					    status_line => $self->{client}->status_line,
					    method_name => 'list_jobs',
				       );
    }
}



=head2 delete_job

  $obj->delete_job($job)

=over 4

=item Parameter and return types

=begin html

<pre>
$job is a UserAndJobState.job_id
job_id is a string

</pre>

=end html

=begin text

$job is a UserAndJobState.job_id
job_id is a string


=end text

=item Description

Delete a job. Will error out if the job is not complete.

=back

=cut

sub delete_job
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function delete_job (received $n, expecting 1)");
    }
    {
	my($job) = @args;

	my @_bad_arguments;
        (!ref($job)) or push(@_bad_arguments, "Invalid type for argument 1 \"job\" (value was \"$job\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to delete_job:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'delete_job');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.delete_job",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'delete_job',
					      );
	} else {
	    return;
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method delete_job",
					    status_line => $self->{client}->status_line,
					    method_name => 'delete_job',
				       );
    }
}



=head2 force_delete_job

  $obj->force_delete_job($job)

=over 4

=item Parameter and return types

=begin html

<pre>
$job is a UserAndJobState.job_id
job_id is a string

</pre>

=end html

=begin text

$job is a UserAndJobState.job_id
job_id is a string


=end text

=item Description

Force delete a job - will always succeed, regardless of job state.

=back

=cut

sub force_delete_job
{
    my($self, @args) = @_;

# Authentication: required

    if ((my $n = @args) != 1)
    {
	Bio::KBase::Exceptions::ArgumentValidationError->throw(error =>
							       "Invalid argument count for function force_delete_job (received $n, expecting 1)");
    }
    {
	my($job) = @args;

	my @_bad_arguments;
        (!ref($job)) or push(@_bad_arguments, "Invalid type for argument 1 \"job\" (value was \"$job\")");
        if (@_bad_arguments) {
	    my $msg = "Invalid arguments passed to force_delete_job:\n" . join("", map { "\t$_\n" } @_bad_arguments);
	    Bio::KBase::Exceptions::ArgumentValidationError->throw(error => $msg,
								   method_name => 'force_delete_job');
	}
    }

    my $result = $self->{client}->call($self->{url}, {
	method => "UserAndJobState.force_delete_job",
	params => \@args,
    });
    if ($result) {
	if ($result->is_error) {
	    Bio::KBase::Exceptions::JSONRPC->throw(error => $result->error_message,
					       code => $result->content->{code},
					       method_name => 'force_delete_job',
					      );
	} else {
	    return;
	}
    } else {
        Bio::KBase::Exceptions::HTTP->throw(error => "Error invoking method force_delete_job",
					    status_line => $self->{client}->status_line,
					    method_name => 'force_delete_job',
				       );
    }
}



sub version {
    my ($self) = @_;
    my $result = $self->{client}->call($self->{url}, {
        method => "UserAndJobState.version",
        params => [],
    });
    if ($result) {
        if ($result->is_error) {
            Bio::KBase::Exceptions::JSONRPC->throw(
                error => $result->error_message,
                code => $result->content->{code},
                method_name => 'force_delete_job',
            );
        } else {
            return wantarray ? @{$result->result} : $result->result->[0];
        }
    } else {
        Bio::KBase::Exceptions::HTTP->throw(
            error => "Error invoking method force_delete_job",
            status_line => $self->{client}->status_line,
            method_name => 'force_delete_job',
        );
    }
}

sub _validate_version {
    my ($self) = @_;
    my $svr_version = $self->version();
    my $client_version = $VERSION;
    my ($cMajor, $cMinor) = split(/\./, $client_version);
    my ($sMajor, $sMinor) = split(/\./, $svr_version);
    if ($sMajor != $cMajor) {
        Bio::KBase::Exceptions::ClientServerIncompatible->throw(
            error => "Major version numbers differ.",
            server_version => $svr_version,
            client_version => $client_version
        );
    }
    if ($sMinor < $cMinor) {
        Bio::KBase::Exceptions::ClientServerIncompatible->throw(
            error => "Client minor version greater than Server minor version.",
            server_version => $svr_version,
            client_version => $client_version
        );
    }
    if ($sMinor > $cMinor) {
        warn "New client version available for Bio::KBase::userandjobstate::Client\n";
    }
    if ($sMajor == 0) {
        warn "Bio::KBase::userandjobstate::Client version is $svr_version. API subject to change.\n";
    }
}

=head1 TYPES



=head2 service_name

=over 4



=item Description

A service name.


=item Definition

=begin html

<pre>
a string
</pre>

=end html

=begin text

a string

=end text

=back



=head2 boolean

=over 4



=item Description

A boolean. 0 = false, other = true.


=item Definition

=begin html

<pre>
an int
</pre>

=end html

=begin text

an int

=end text

=back



=head2 timestamp

=over 4



=item Description

A time, e.g. 2012-12-17T23:24:06.


=item Definition

=begin html

<pre>
a string
</pre>

=end html

=begin text

a string

=end text

=back



=head2 job_id

=over 4



=item Description

A job id.


=item Definition

=begin html

<pre>
a string
</pre>

=end html

=begin text

a string

=end text

=back



=head2 job_status

=over 4



=item Description

A job status string supplied by the reporting service. No more than
200 characters.


=item Definition

=begin html

<pre>
a string
</pre>

=end html

=begin text

a string

=end text

=back



=head2 job_description

=over 4



=item Description

A job description string supplied by the reporting service. No more than
1000 characters.


=item Definition

=begin html

<pre>
a string
</pre>

=end html

=begin text

a string

=end text

=back



=head2 progress

=over 4



=item Description

The amount of progress the job has made since the last update, summed
to the total progress so far.


=item Definition

=begin html

<pre>
an int
</pre>

=end html

=begin text

an int

=end text

=back



=head2 total_progress

=over 4



=item Description

The total progress of a job.


=item Definition

=begin html

<pre>
an int
</pre>

=end html

=begin text

an int

=end text

=back



=head2 max_progress

=over 4



=item Description

The maximum possible progress of a job.


=item Definition

=begin html

<pre>
an int
</pre>

=end html

=begin text

an int

=end text

=back



=head2 progress_type

=over 4



=item Description

The type of progress that is being tracked. One of:
'none' - no numerical progress tracking
'task' - Task based tracking, e.g. 3/24
'percent' - percentage based tracking, e.g. 5/100%


=item Definition

=begin html

<pre>
a string
</pre>

=end html

=begin text

a string

=end text

=back



=head2 InitProgress

=over 4



=item Description

Initialization information for progress tracking. Currently 3 choices:

progress_type ptype - one of 'none', 'percent', or 'task'
max_progress max- required only for task based tracking. The 
        total number of tasks until the job is complete.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
ptype has a value which is a UserAndJobState.progress_type
max has a value which is a UserAndJobState.max_progress

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
ptype has a value which is a UserAndJobState.progress_type
max has a value which is a UserAndJobState.max_progress


=end text

=back



=head2 Results

=over 4



=item Description

A pointer to job results. All arguments are optional. Applications
should use the default shock and workspace urls if omitted.
list<string> shocknodes - the shocknode(s) where the results can be
        found.
string shockurl - the url of the shock service where the data was
        saved.
list<string> workspaceids - the workspace ids where the results can be
        found.
string workspaceurl - the url of the workspace service where the data
        was saved.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
shocknodes has a value which is a reference to a list where each element is a string
shockurl has a value which is a string
workspaceids has a value which is a reference to a list where each element is a string
workspaceurl has a value which is a string

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
shocknodes has a value which is a reference to a list where each element is a string
shockurl has a value which is a string
workspaceids has a value which is a reference to a list where each element is a string
workspaceurl has a value which is a string


=end text

=back



=head2 job_info

=over 4



=item Description

Information about a job. Note calls returning this structure will
probably be slower than the more targeted calls.


=item Definition

=begin html

<pre>
a reference to a list containing 11 items:
0: (job) a UserAndJobState.job_id
1: (service) a UserAndJobState.service_name
2: (status) a UserAndJobState.job_status
3: (last_update) a UserAndJobState.timestamp
4: (prog) a UserAndJobState.total_progress
5: (max) a UserAndJobState.max_progress
6: (ptype) a UserAndJobState.progress_type
7: (complete) a UserAndJobState.boolean
8: (error) a UserAndJobState.boolean
9: (desc) a UserAndJobState.job_description
10: (res) a UserAndJobState.Results

</pre>

=end html

=begin text

a reference to a list containing 11 items:
0: (job) a UserAndJobState.job_id
1: (service) a UserAndJobState.service_name
2: (status) a UserAndJobState.job_status
3: (last_update) a UserAndJobState.timestamp
4: (prog) a UserAndJobState.total_progress
5: (max) a UserAndJobState.max_progress
6: (ptype) a UserAndJobState.progress_type
7: (complete) a UserAndJobState.boolean
8: (error) a UserAndJobState.boolean
9: (desc) a UserAndJobState.job_description
10: (res) a UserAndJobState.Results


=end text

=back



=head2 ListJobsOptions

=over 4



=item Description

Options for list_jobs command. 

boolean oldest_first - return jobs with an ascending sort based on the
        creation date.
int limit - limit the results to X jobs.
int offset - skip the first X jobs.
boolean completed - true to return only completed jobs, false to
        return only incomplete jobs.
boolean error_only - true to return only jobs that errored out. 
        Overrides the completed option.


=item Definition

=begin html

<pre>
a reference to a hash where the following keys are defined:
oldest_first has a value which is a UserAndJobState.boolean
limit has a value which is an int
offset has a value which is an int
completed has a value which is a UserAndJobState.boolean
error_only has a value which is a UserAndJobState.boolean

</pre>

=end html

=begin text

a reference to a hash where the following keys are defined:
oldest_first has a value which is a UserAndJobState.boolean
limit has a value which is an int
offset has a value which is an int
completed has a value which is a UserAndJobState.boolean
error_only has a value which is a UserAndJobState.boolean


=end text

=back



=cut

package Bio::KBase::userandjobstate::Client::RpcClient;
use base 'JSON::RPC::Client';

#
# Override JSON::RPC::Client::call because it doesn't handle error returns properly.
#

sub call {
    my ($self, $uri, $obj) = @_;
    my $result;

    if ($uri =~ /\?/) {
       $result = $self->_get($uri);
    }
    else {
        Carp::croak "not hashref." unless (ref $obj eq 'HASH');
        $result = $self->_post($uri, $obj);
    }

    my $service = $obj->{method} =~ /^system\./ if ( $obj );

    $self->status_line($result->status_line);

    if ($result->is_success) {

        return unless($result->content); # notification?

        if ($service) {
            return JSON::RPC::ServiceObject->new($result, $self->json);
        }

        return JSON::RPC::ReturnObject->new($result, $self->json);
    }
    elsif ($result->content_type eq 'application/json')
    {
        return JSON::RPC::ReturnObject->new($result, $self->json);
    }
    else {
        return;
    }
}


sub _post {
    my ($self, $uri, $obj) = @_;
    my $json = $self->json;

    $obj->{version} ||= $self->{version} || '1.1';

    if ($obj->{version} eq '1.0') {
        delete $obj->{version};
        if (exists $obj->{id}) {
            $self->id($obj->{id}) if ($obj->{id}); # if undef, it is notification.
        }
        else {
            $obj->{id} = $self->id || ($self->id('JSON::RPC::Client'));
        }
    }
    else {
        # $obj->{id} = $self->id if (defined $self->id);
	# Assign a random number to the id if one hasn't been set
	$obj->{id} = (defined $self->id) ? $self->id : substr(rand(),2);
    }

    my $content = $json->encode($obj);

    $self->ua->post(
        $uri,
        Content_Type   => $self->{content_type},
        Content        => $content,
        Accept         => 'application/json',
	($self->{token} ? (Authorization => $self->{token}) : ()),
    );
}



1;
