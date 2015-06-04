# cs-api-docker-util

## What is it?

A project that builds an executable jar. The jar runs 2 commands that are part of the corespring-api play project, but without the need to install sbt.

## Why?

Because the docker image will only be built once we want the image build to be quick, this jar will avoid having to install sbt + dependencies only for it to be run once. 