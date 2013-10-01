Service to maintain global user state and job status

*WARNING*: DO NOT RUN TESTS ON A PRODUCTION DATABASE! THE TEST SCRIPTS WILL
  WIPE THE DB.

RUNTIME REQUIREMENTS:
mongo 2.4.3+ required. KBase-v22 has 2.0.something

COMPILATION REQUIREMENTS:
typecomp dev-prototypes branch
java_type_generator dev branch

For now, all compiled files are checked in.

SETUP

1) A mongodb instance must be up and running.
2) make
3) if you want to run tests:
3a) fill in the the test.cfg config file in ./test
3b) make test
4) make deploy
5) /kb/deployment/services/user_and_job_state/start_service