# cs-api-docker-util

## What is it?

A project that builds an executable jar. The jar runs 2 commands that are part of the corespring-api play project, without the need to install sbt.

## Why?

Because the docker image will only be built once, we want the image build to be quick, this jar avoids having to install sbt + download all the dependencies. Instead everything is prebuilt and ready to go.

### Build

    sbt assembly

### See

The `Dockerfile`.
