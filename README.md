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
* Install [play 2.1.3](http://www.playframework.com/download)
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

https://corespring.signin.aws.amazon.com/console

Ask evan for a user account.

