![corespring](public/images/logo.png)

This project contains the api rest layer and web ui for administering corespring items.

### Prerequisites

* A working ssh key for github: [more info](https://help.github.com/articles/generating-ssh-keys)

### Installation

Before you do *anything* please run the following to install the git pre-commit hook:

    ln -s $(pwd)/hooks/pre-commit .git/hooks/pre-commit
    
For more information, please see our git commit hooks [documentation](hooks/README.md).

**corespring-components submodule**

After downloading the corespring-api repository, run the following commands to init the submodule.

`git submodule init`

`git submodule update`

*we assume you already have Java JDK >= 1.6*

*Mac users - It is recommended that you use [homebrew](http://mxcl.github.io/homebrew/) for all your installations.*

    cd ~/dev/github/corespring
    git clone git@github.com:corespring/corespring-api.git

* Install mongodb
* Install [play 2.2.1](http://downloads.typesafe.com/play/2.2.1/play-2.2.1.zip)
* Install elasticsearch
* Install sbt (using brew or download zip)
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


### Unit Tests

    play test

### Integration tests

These tests are slower than the unit tests, as they set up a db + s3 etc, but they exercise the web apps endpoints and other parts of the system.

* They run sequentially
* They boot the play app at the start of the test run
* They seed and destroy the data they need

    play it:test

To test a single example in a given test run (works for unit tests and integration tests):

    it:test-only *ItemSessionApiTest* -- -ex "1: return 200 and 100% - for multiple choice"

#### Logging in integration tests

Add a logger to `it-resources/application-logger.xml` - it's ignored by git so do what you want there


### Regression Testing

see: [corespring-container-regression-tests](https://github.com/corespring/corespring-container-regression-tests)

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

by default in Dev mode the logger in conf/application-logger.xml is used.

#### Amazon S3

We use Amazon S3 for deploying files - the management console is here:

Each deployment uses its own s3 bucket: corespring-assets-${deployment}.
The developer machines point to corespring-assets-dev for example.

You may find the following tool useful: [aws cli](https://aws.amazon.com/cli/).

The web ui is here: [aws console](https://corespring.signin.aws.amazon.com/console).

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

### Releasing

The release flow is very similar to that in [corespring-container](https://github.com/corespring/corespring-container/blob/develop/README.md#creating-a-release).

The main difference being that this app runs `stage` as well as `publish`.

### Dev Tools

see: [cs-dev-tools](https://github.com/corespring/cs-dev-tools)

## Container Configuration

The V2 Production player can load assets from a CDN. To enable this set the following env-var: `CONTAINER_CDN_DOMAIN`.
If it's not set the assets will be retrieved locally. Note that this domain needs to use the corespring-api server as it's origin.

## Cloudfront 

We are using cloudfront for the CDN. see: https://console.aws.amazon.com/cloudfront/home

### Using cloudfront for item assets in the player 

#### Deployment steps: 
see [Amazon Docs] (http://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/PrivateContent.html)

**Note 1:** The Cloudfront console is slow. Changing any of the settings in there easily can take 5 minutes before it is applied. Better to do this in quiet hours on prod.  
**Note 2:** The settings for restricted access are tied to the cloudfront distribution. If you set the distribution to require signed urls, all items in there will need to be signed.  
  

1. Create CloudFront Key Pairs    
see [Amazon howto] (http://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-trusted-signers.html#private-content-creating-cloudfront-key-pairs)

2. Create CloudFront distribution for the s3-assets folder of the deployment target 

3. Restrict bucket access   
In the Cloudfront Origin tab for your distribution choose restrict bucket access = yes with a new or existing identity. Choose "Yes, Update Bucket Policy" to automatically update the bucket policy. If you don't do that, you will see "Forbidden" answers to your requests 

4. Restrict viewer access    
In the Cloudfront Behaviour tab for your distribution choose restrict viewer access = yes. Choose "self" as the trused signer 

5. Heroku settings  
(IAR is short for Item Asset Resolver)  
ENV_IAR_ENABLED - set it to true or false to enable the resolver. When it is disabled, the code will behave like it doesn't exist - default is false  
ENV_IAR_SIGN_URLS - set it to true or false to activate/deactivate signing of urls - default is false  
ENV_IAR_CDN_DOMAIN - set it to the the cloudfront domain with two leading slashes - no default    
ENV_IAR_KEY_PAIR_ID - set it to the name of the key pair that you created in step 1 - no default    
ENV_IAR_PRIVATE_KEY - set it to the content of the private key file - no default   
ENV_IAR_URL_VALID_IN_HOURS - set it to the number of hours a url should remain valid - default is 24    
ENV_IAR_ADD_VERSION_AS_QUERY_PARAM - set it to true to add the app version to the url - default is true
ENV_IAR_HTTP_PROTOCOL_FOR_SIGNED_URLS - the signed url will get this protocol - default is https: 

**Note:** Setting the Private Key via the heroku webapp doesn't seem to work. In a shell you can use 

    heroku config:add ENV_IAR_PRIVATE_KEY="[paste the key here]" --app [the app name]
    
**Note 2:** In corespring-api/bin/shell.scripts/add-item-asset-resolver-vars you can find a script to setup all vars at once. Remember not to commit any secrets.
      
#### Testing
* Create a new item, add an image to it and save it. 
* Take note of the itemId
* Open the item in [your server]/items/[your itemId]/sample-launch-code
 
#### Troubleshooting 
Let's say you have everything set-up to sign urls   
If the image is not shown or you are forbidden to access it
* Set ENV_IAR_SIGN_URLS to false  
* Set restrict viewer access = no in the behaviour tab of your distribution  
* When the distribution is finished with deployment, you can test again     

If you still cannot access the image, your distribution might not have the right to access the s3 bucket.      
* Open the origin tab and edit your distribution.   
* Select the correct Origin Access Identity in the "Your Identities" drop down.  
* Select "Yes, Update Bucket Policy" for Grant Read Permissions on Bucket  
* When the distribution is finished with deployment, you can test again  

Still cannot access?     
* Open the origin tab and have a look at the column named Origin Access Identity, eg. origin-access-identity/cloudfront/E3V4529M09EC7F  
* Go to the s3 bucket and open the properties/permissions/edit bucket policy   
* Verify that the origin access identity in there has the same id
* Verify that the protocol of the url matches the protocol that the url has been signed with (https: by default)
  
Works now with signUrl=false
* Go back to deployment step 4 "Restrict viewer access"
* Set ENV_IAR_SIGN_URLS to true          
* When the distribution is finished with deployment, you can test again
  

#### Deactivation 
If you cannot get it to work or you want to disable the resolver for other reasons:
2. Set enabled to false ENV_IAR_ENABLED = false

#### Don't sign 
If you want to use the CDN for item assets but don't want to restrict access, set signUrls to false, ENV_IAR_SIGN_URLS = false. Make sure that the in the Cloudfront Behaviour tab "restrict viewer access = no" is choosen. 


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

[Docker Information](docker/README.md)


### Running Locally

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

