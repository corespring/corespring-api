# Releasing corespring-api

## Intro

This is a proposal to change how we release corespring api.
The motivations for this are:

- The project version (has always been 1.0) has no relationship with the actual release versions that we deploy and tag in git.
- The application is now defined as a set of reusable modules and some of these modules may be of use in other contexts, so the ability to release versions of these as libraries will be useful.
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


## Proposal

Make use of [sbt-release](https://github.com/sbt/sbt-release) and and a few extra utilities via a new plugin: [sbt-release-extras](https://bitbucket.org/corespring/sbt-release-extras).
Then we can adjust the release steps to suit the apps needs. The end result will be that there'll be a single command to run to create a release: 

```bash
    
    $ play release
```

The custom release steps will be: 
```bash

    checkBranchVersion # check that the git branch 'release/X.X.X' matches the project version X.X.X-SNAPSHOT
    checkSnapshotDependencies # check that there are no snapshot dependencies for the project
    runClean # clean
    runTest # test
    runIntegrationTest # integration test
    prepareReleaseVersion # prepare the release version X.X.X-SNAPSHOT -> X.X.X
    setReleaseVersion # set the version in the project and write it to version.sbt
    commitReleaseVersion # commit the changes
    tagRelease # tag
    mergeReleaseTagTo("master") # merge tag to master
    publishArtifacts # publish libs
    runStage # run stage

```

As well as this provide some utilities for creating release and hotfix branches: 

`create-release-branch` - will create a release branch from the nominated release parent (aka: 'develop'), it'll bump the version in 'develop' so that it has a new version. Eg if we are on 1.0.0-SNAPSHOT, release will create release/1.0.0 with a project version of '1.0.0-SNAPSHOT' and will update the version in 'develop' to be '1.2.0-SNAPSHOT'.

`create-hotfix-branch` - will create a hotfix branch, you have to create it from the nominated hotfix parent (aka: 'master'), you'll be asked from which tag you want to create the hotfix. If you choose 1.0.0, the hotfix will be hotfix/1.0.1.

For both commands the working tree must be clean.

