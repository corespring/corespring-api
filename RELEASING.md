# Releasing corespring-api

## Intro

This is a proposal to change how we release corespring api.
The motivations for this are: 

- The sbt project and git versions have no relationship to each other, this is messy
- The application is now defined as a set of reusable modules and some of these modules may be of use in other contexts, so the ability to release these as jars will be useful.
- Currently releasing has many manual steps to it that are easy to get wrong, this proposal automates this to remove potential for error.

## Releasing
Releasing corespring-api should perform the following 2 build steps: 

### publish jars

The JARs for the modules should be built and hosted on repository.corespring.org with the release version set.

### build the webapp
The webapp should be ready to run by running the `stage` command.

> Note: a developer can either publish jars or build the app when they see fit. This doesn't change.

## Other constraints
- The project version should match the git tag. So on release of a project with a version of `0.0.1-SNAPSHOT`, the tag for this release would be `v0.0.1` and the jars would be named `name-0.0.1.jar`

