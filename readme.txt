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