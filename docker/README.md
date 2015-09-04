### Docker deployment

To test feature branches in isolation, we have defined a `Dockerfile` that allows cs-api to be run in a sandboxed environment.

If you want to run this docker image or deploy it using [docker-deployer](github.com/corespring/docker-deployer), you'll need to build the following project once: 

```shell
    cd docker/cs-api-docker-util
    sbt assembly
```

**To deploy your local instance to Elastic Beanstalk on AWS.**

1. Request AWS IAM credentials (AWS admin on the team can provide these for you)
2. run `play stage` to build a zip with the current state of your local environment
3. delpoy to AWS with [docker-deployer](github.com/corespring/docker-deployer) (see below)

Deploying with docker-deployer:

```shell
    docker-deployer deploy --deploy-name $NAME 
```

**To create and run a docker image with default Dockerfile:** 

````shell
    docker build -t="corespring-api" .
    docker run -p 9000:9000 -t="corespring-api" #run main script
```

**To create and run a docker image with custom docker file named DockerfileWithDynamo**

````shell
    docker build --file=./DockerfileWithDynamo -t="corespring-api-with-dynamo" .
    docker run -p 9000:9000 -t="corespring-api-with-dynamo" #run main script
```


### Dockerfile types

> We don't have a docker registry so there's a little bit of duplication amongst the following files.

* Dockerfile - the main file that sets up a local mongo, s3 and elastic search
* Dockerfile_remote_mongo_search - sets up only the app and requires that you set the env vars to point the app at a remote mongo, s3 and search
* DockerfileWithDynamo - the app with dynamo running