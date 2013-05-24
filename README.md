Corespring API
==============

# Set up (we assume you already have Java JDK >= 1.6)

1. Install mongodb
2. Install play 2.0.4
3. For running tests install phantomjs

## Mac users
It is recommended that you use (homebrew)[http://mxcl.github.io/homebrew/] for all your installations.

4. Install source

    cd ~/dev/github/corespring
    git clone git@github.com:corespring/corespring-api.git

# Running

    cd corepsring-api
    play

# Application configuration

The application will run without any configuration by using a set of default values.
These values essentially run the app in development mode, by using the local db
and reseeding the data.

When deploying the application to heroku we override some of these variables.

This is done using the (heroku-helper)[https://github.com/corespring/heroku-helper].
See that project for more documentation. In an nutshell the helper uses 2 files:

* .heroku-helper.conf - allows you to configure scripts to run as part of deployment
* .heroku-helper-env.conf - set up env vars for a given heroku server (not under source control).
