### Docker deployment

To test feature branches in isolation, we have defined a set of Dockerfiles that allows cs-api to be run in a different environments.

### Dockerfile types

> We don't have a docker registry so there's a little bit of duplication amongst the following files.

* Dockerfile - the main file that sets up a local mongo, s3 and elastic search
* Dockerfile_remote_mongo_search - sets up only the app and requires that you set the env vars to point the app at a remote mongo, s3 and search
* DockerfileWithDynamo - the app with dynamo running


## Dockerfile

This is a completely sandboxed instance. It seeds a local mongo, sets up a fake s3 and a local elastic search.

> There is more work to do on this instances - like seeding the db with a backup of staging data etc.


If you want to run this docker image or deploy it using [docker-deployer](github.com/corespring/docker-deployer), you'll need to build the following project once: 

```shell
    cd docker/cs-api-docker-util
    sbt assembly
```


## Dockerfile_remote_mongo_search 

This is much thinner - it only sets up the play app and requires that you have the env vars set to point to mongo, elastic search and s3.

This means that you'll need to have your own s3, mongo and elastic set up.

When running docker deployer you'll want to set the env vars you can do this by using `-v X=Y` for `deploy` or run `update-env-vars`. run `docker-deployer --help` for more info.


## DockerfileWithDynamo 

An instance with Dynamo set up


# To deploy your local instance to Elastic Beanstalk on AWS.

1. Request AWS IAM credentials (AWS admin on the team can provide these for you)
2. run `play stage` to build the jars needed for the app to run.
3. deploy to AWS with [docker-deployer](github.com/corespring/docker-deployer) (see below)

# Deploying with docker-deployer

```shell
    docker-deployer deploy --deploy-name $NAME --docker-filename $DOCKER_NAME
```

# Testing the docker file locally 

> You need to have docker installed on your machine to do this.

To create and run a docker image with default Dockerfile:** 

````shell
    docker build -t="corespring-api" .
    docker run -p 9000:9000 -t="corespring-api" #run main script
```

**To create and run a docker image with custom docker file named DockerfileWithDynamo**

````shell
    docker build --file=./DockerfileWithDynamo -t="corespring-api-with-dynamo" .
    docker run -p 9000:9000 -t="corespring-api-with-dynamo" #run main script
```


