Service to maintain global user state and job status

*WARNING*: DO NOT RUN TESTS ON A PRODUCTION DATABASE! THE TEST SCRIPTS WILL
  WIPE THE DB.

RUNTIME REQUIREMENTS:
mongo 2.4.3+ required. KBase-v22 has 2.0.something

SETUP

1) A mongodb instance must be up and running.
2) make
3) if you want to run tests:
3a) fill in the the test.cfg config file in ./test
3b) make test
4) make deploy
5) Fill in deploy.cfg and set KB_DEPLOYMENT_CONFIG appropriately
6) /kb/deployment/services/user_and_job_state/start_service

If the server doesn't start up correctly, check /var/log/syslog and
/kb/runtime/glassfish3/glassfish/domains/domain1/logs/server.log 
for debugging information.

KNOWN BUGS: 
In some cases, the server can start up, apparently normally, without
establishing contact with MongoDB. In this case all calls will fail, and the
server should be restarted. This *appears* to only occur once per magellean
instance, which makes it difficult to debug.

To check whether this has occurred:

# cd /kb/deployment/lib/
# python
Python 2.7.3 (default, Sep 26 2013, 20:03:06) 
[GCC 4.6.3] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> from biokbase.userandjobstate.client import UserAndJobState
>>> ujs = UserAndJobState('http://localhost:7083', user_id='[username]',
	password='[pwd]')
>>> ujs.create_job()
u'525c7bb0e4b0ade4bb6a6593'

The above call will fail if the server has started incorrectly.