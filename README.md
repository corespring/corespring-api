This project contains the api rest layer and web ui for administering corespring items.

### Installation
*we assume you already have Java JDK >= 1.6*

*Mac users - It is recommended that you use [homebrew](http://mxcl.github.io/homebrew/) for all your installations.*

    cd ~/dev/github/corespring
    git clone git@github.com:corespring/corespring-api.git

* Install mongodb
* Install play 2.0.4
* For running tests install phantomjs



### Running/Testing

    cd corepsring-api
    play run
    play test

### Application configuration

The application will run without any configuration by using a set of default values.
These values essentially run the app in development mode, by using the local db
and reseeding the data.

When deploying the application to heroku we override some of these variables.

This is done using the [heroku-helper](https://github.com/corespring/heroku-helper).
See that project for more documentation. In an nutshell the helper uses 2 files:

* .heroku-helper.conf - allows you to configure scripts to run as part of deployment
* .heroku-helper-env.conf - set up env vars for a given heroku server (not under source control).

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

To configure the logger when running locally add a system property to your run command:

    play -Dlogger.file=path/to/logger.xml

#### Amazon S3

We use Amazon S3 for deploying files - the management console is here:

https://corespring.signin.aws.amazon.com/console

Ask evan for a user account.

