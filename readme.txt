Service to maintain global user state and job status

*WARNING*: DO NOT RUN TESTS ON A PRODUCTION DATABASE! THE TEST SCRIPTS WILL
  WIPE THE DB.

RUNTIME REQUIREMENTS:
mongo 2.4.3+ required. KBase-v22 has 2.0.something

SETUP

1) A mongodb instance must be up and running.
2) Fill in deploy.cfg appropriately.
3) make
4) if you want to run tests:
4a) fill in the the test.cfg config file in ./test
4b) make test
5) make deploy
6) /kb/deployment/services/user_and_job_state/start_service