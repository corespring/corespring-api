#!/bin/sh

rm -fr aws.zip

cd docker/cs-api-docker-util
sbt assembly
cd ../..

mkdir docker/.ivy2
cp -rv ~/.ivy2/.credentials docker/.ivy2/.credentials

play stage

UTIL=$(find docker/cs-api-docker-util/target -name "*.jar")
zip -r aws.zip \
./Dockerfile \
docker/.ivy2 \
$UTIL \
docker/scripts \
target/universal/stage \
corespring-components/components \
conf
