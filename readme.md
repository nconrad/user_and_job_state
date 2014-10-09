User and Job State service
==========================

Service to maintain global user state and job status

Also has an option to wrap an Awe server and report job state

Tested against https://github.com/kbase/awe_service  
244e569c35375e638d39fc0f2667c976070da92f

RUNTIME REQUIREMENTS
--------------------

mongo 2.4.3+ required.

SETUP
-----

1. make
2. if you want to run tests:  
    1. MongoDB must be installed, but not necessarily running.  
    2. Shock must be installed, but not necessarily running.  
    3. AWE must be installed, but not necessarily running.  
    4. fill in the the test.cfg config file in ./test  
    5. make test  
3. A mongodb instance must be up and running.
4. If using AWE, Shock and AWE must be up and running.
5. fill in deploy.cfg
6. make deploy
7. optionally, set KB_DEPLOYMENT_CONFIG appropriately
8. /kb/deployment/services/user_and_job_state/start_service

If the server doesn't start up correctly, check /var/log/syslog and
/kb/deployment/services/user_and_job_state/glassfish_domain/UserAndJobState/
  logs/server.log for debugging information, assuming the deploy is in the
default location.
