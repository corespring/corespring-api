# Corespring Api Deployment

This folder contains scripts that are executed as part of a deployment to a given target.
These scripts are called by the heroku-helper: https://github.com/corespring/heroku-helper.
The configuration of these scripts is done in the ../.heroku-helper.conf file.

# Running

    cd corespring-api
    # launches the interactive console
    java -jar deployment/libs/heroku-helper_2.9.2-0.1-one-jar.jar

# System requirements

* ruby 1.9.3
* java >= 1.6
* mongo-db-utils (gem install mongo-db-utils)
* mongo shell
* heroku toolbelt
* git
* Access to the heroku servers (you'll need to be declared as a collaborator)

# Environment Variables

There are 2 sets of environment variables that you'll require (they are not in the code repo for a reason!).

* heroku-helper-env.conf - this file contains mongo uris and twitter/google id/secrets
* env vars - some environment properties that these script use to do its work.

## Main scripts

### run_migrations.rb
Runs the https://github.com/corespring/mongo-migrator - as a push script it is passed as its first argument a path to a json file that contains the heroku configuration for the given server.

### backup_db.rb
backs up the db to Amazon S3, uses the mongo-db-utils gem - as above receives a path to a json config file, also depends on the following env vars:
CORESPRING_AMAZON_BUCKET
CORESPRING_AMAZON_ACCESS_KEY
CORESPRING_AMAZON_SECRET_KEY

### run_rollback.rb
Runs 1. a backup of the db, 2. A rollback using the https://github.com/corespring/mongo-migrator

### copy_live_db.rb
Copies the live db to the target db as specified in the config json file passed in by the heroku helper

### validate_environment.rb
Just validates that this environment is ready to run - the heroku helper will exit if not.

# Migrations
This folder contains data base migration files that are used by the mongo-migrator
