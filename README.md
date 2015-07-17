![corespring](public/images/logo.png)

This project contains the api rest layer and web ui for administering corespring items.

### Prerequisites

* A working ssh key for github: [more info](https://help.github.com/articles/generating-ssh-keys)

### Installation

Before you do *anything* please run the following to install the git pre-commit hook:

    ln -s hooks/pre-commit .git/hooks/pre-commit
    
For more information, please see our git commit hooks [documentation](hooks/README.md).

*we assume you already have Java JDK >= 1.6*

*Mac users - It is recommended that you use [homebrew](http://mxcl.github.io/homebrew/) for all your installations.*

    cd ~/dev/github/corespring
    git clone git@github.com:corespring/corespring-api.git

* Install mongodb
* Install [play 2.2.1](http://downloads.typesafe.com/play/2.2.1/play-2.2.1.zip)
* Install elasticsearch
* For running tests install phantomjs

### SBT Configuration

We use a private repo for our assets. Because of this you will need to define your credentials in a file located at:

    ~/.ivy2/.credentials

This file will look like so:

    realm=Artifactory Realm
    host=repository.corespring.org
    user=XXXXXXXXXX
    password=XXXXXXXXXX

Ask someone to provide you with the user password


### Testing

Currently our testing is a hybrid of unit and integration tests.
We are looking to move out the integration tests to the 'it' folder.
And we plan to remove the db-seeding that is done for the entire test suite.
Instead each tests will hoist and destroy their own data.


    play test

### Integration tests

These tests are more expensive than the unit tests.

* They run sequentially
* They boot the play app for each test
* They seed and destroy the data they need

    play it:test



To test a single example in a given test run:

    it:test-only *ItemSessionApiTest* -- -ex "1: return 200 and 100% - for multiple choice"


### Application configuration

The application will run without any configuration by using a set of default values.
These values essentially run the app in development mode, by using the local db
and reseeding the data.

When deploying the application to heroku we override some of these variables using env vars.

If you want the search API to work locally, you must run

    sbt index
    
before running the application. Note that there is a bug where the sbt task will not terminate after indexing is 
complete, so watch for the message and kill the process manually. Work is currently in progress to streamline indexing.


### IntelliJ Configuration

You'll need to install the Scala and Play 2.0 plugins. These can be found in Settings by navigating to Plugins and clicking the 'Install JetBrains plugin...' button.

After that, you'll need to generate the IntelliJ project files. Do this with the following command:

    play idea

### Logging configuration

For information on how to configure the xml see: [play docs](http://www.playframework.com/documentation/2.1.1/SettingsLogger)
and [logback docs](http://logback.qos.ch/manual/configuration.html)

#### Heroku

There are some logging configurations in conf/logging. When `foreman` starts `play` it uses the logger
defined by `ENV_LOGGER` which defaults to `conf/logger.xml` (this is defined in the .env file).

To change this add an environment variable:

    heroku config:set ENV_LOGGER=conf/logging/debug.xml --app your_server_name_here

Note that you can point to a file that is not on the class path if you want:

    heroku config:set ENV_LOGGER=/home/test/some-log-config.xml --app your_server_name_here

* Note: It would have been preferable to set the logger up in the conf file - but thats not possible with the current
version of play.*



#### Localhost

by default in Dev mode the logger in conf/logger.xml is used.

#### Amazon S3

We use Amazon S3 for deploying files - the management console is here:

Each deployment uses its own s3 bucket: corespring-assets-${deployment}.
The developer machines point to corespring-assets-dev for example.

There are some useful utilities for working with the s3 assets: 

* [corespring-assets gem](https://github.com/corespring/corespring-api-assets) - some commands for pulling/pushing buckets and cleaning them.
* [s3cmd](https://github.com/pearltrees/s3cmd-modification) - a command line utility for working with s3 (modded for parallel speed).

[aws console](https://corespring.signin.aws.amazon.com/console)

Ask evan for a user account.

#### Amazon DynamoDb (experimental) 

By default we are using MongoDb for session storage. So normally you don't have to worry about this. 
As a new feature we are currently trying out Amazon's DynamoDb for the sessionDbService. If you want to play 
with it or do some work in this area, you can enable it in application.conf       

For development you should run a local instance. 
You can get it here: [local dynamodb](http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tools.DynamoDBLocal.html)

Check application.conf for dynamo configuration properties.
    
[aws console](https://corespring.signin.aws.amazon.com/console)
Ask evan for a user account or use an account from passpack 


### Dev Tools

Some quick shortcuts for running certain things:

DEV_TOOLS_ENABLED needs to be set to true on the env for this to work:


   GET /dev/tools/v2-player/:itemId -> run the item in the new v2 player.

## Container Configuration

The V2 Production player can load assets from a CDN. To enable this set the following env-var: `CONTAINER_CDN_DOMAIN`.
If it's not set the assets will be retrieved locally. Note that this domain needs to use the corespring-api server as it's origin.

## Cloudfront 

We are using cloudfront for the CDN. see: https://console.aws.amazon.com/cloudfront/home

## New Relic

New Relic is included as a dependency. It is not our intention yet (as of 9/9) to use this in production, but as an option we can
turn on in devt/staging to analyze performance.

It is configured with the file `newrelic.yml`

It will only be running if the new relic agent is included in the environment as NEW_RELIC_CONF

    heroku config:set NEW_RELIC_CONF="-J-javaagent:target/universal/stage/lib/com.newrelic.agent.java.newrelic-agent-3.10.0.jar -J-Dnewrelic.config.file=conf/newrelic.yml"

Heroku instances with the new relic add-on installed override the New Relic license information specified in the newrelic.yml file.

See:

https://devcenter.heroku.com/articles/newrelic#add-on-installation

RUM (Real User Monitoring) features are not enabled as yet.

https://docs.newrelic.com/docs/agents/java-agent/instrumentation/page-load-timing-java#manual_instrumentation


### Docker deployment

To test feature branches in isolation, we have defined a `Dockerfile` that allows cs-api to be run in a sandboxed environment.

If you want to run this docker image or deploy it using [docker-deployer](github.com/corespring/docker-deployer), you'll need to build the following project once: 

```shell
    cd docker/cs-api-docker-util
    sbt assembly
```

To create and run a docker image with default Dockerfile: 

````shell
    docker build -t="corespring-api" .
    docker run -p 9000:9000 -t="corespring-api" #run main script
```

To create and run a docker image with custom docker file named DockerfileWithDynamo 

````shell
    docker build --file=./DockerfileWithDynamo -t="corespring-api-with-dynamo" .
    docker run -p 9000:9000 -t="corespring-api-with-dynamo" #run main script
```

To deploy with docker-deployer:

```shell
    docker-deployer deploy --deploy-name $NAME 
```

### Running Localy

If you want to run the API on local environment for the first time start the **play console**:

    play
    
then in the play console run:

    seed-dev
    
when thats done run:

    index
    
if indexing finishes run the API:

    run
    
Once it's running you can access the CMS in a browser on `localhost:9000`

#### Slow updates

All corespring dependencies are configured to use only repository.corespring.org as their resolver.

So you should see resolution times of:

* `clean update` -> ~200s
* `update` -> 10s

> Once sbt have [improved consolidated resolution](https://github.com/sbt/sbt/issues/2105) our resolve times should improve again.

If your updates are slow due to snapshot updates, you may set the following in your console:

```sh
  set updateOptions := updateOptions.value.withLatestSnapshots(false)

```

This will disable snapshots updating themselves.