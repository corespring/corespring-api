FROM phusion/baseimage:0.9.16

RUN apt-get update && \
 apt-get upgrade -y && \
 apt-get install -y \
 wget \
 openjdk-7-jdk 

# Mongo:
# Import MongoDB public GPG key AND create a MongoDB list file
RUN apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv 7F0CEB10
RUN echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | tee /etc/apt/sources.list.d/10gen.list
RUN apt-get update && apt-get install -y mongodb-org
# Create the MongoDB data directory
RUN mkdir -p /var/lib/mongodb 

# Ruby 
RUN apt-get update && \
  apt-get install -y ruby ruby-dev ruby-bundler 

### fakes3
RUN gem install fakes3
RUN mkdir /opt/fake-s3-root
ENV ENV_AMAZON_ENDPOINT="http://localhost:4567"

# elasticsearch
RUN wget -qO - https://packages.elastic.co/GPG-KEY-elasticsearch | sudo apt-key add -
RUN echo "deb http://packages.elastic.co/elasticsearch/1.5/debian stable main" | tee -a /etc/apt/sources.list
RUN apt-get update && sudo apt-get install elasticsearch
# boot at start up
RUN update-rc.d elasticsearch defaults 95 10

# Prevent elasticsearch calling `ulimit`.
RUN sed -i 's/MAX_OPEN_FILES=/# MAX_OPEN_FILES=/g' /etc/init.d/elasticsearch

#ivy 2
ADD docker/.ivy2/.credentials /root/.ivy2/.credentials

# docker sbt util (so we can run mongo-db-seeder + indexer)
ADD docker/cs-api-docker-util/target/scala-2.10/cs-api-docker-util-assembly-*.jar /opt/cs-api-docker-util/run.jar
RUN chmod +x /opt/cs-api-docker-util/run.jar
ADD target/universal/stage/conf/seed-data /opt/cs-api-docker-util/conf/seed-data
ADD target/universal/stage/conf/qti /opt/cs-api-docker-util/conf/qti
ADD target/universal/stage/conf/qti-templates /opt/cs-api-docker-util/conf/qti-templates

ADD corespring-components/components /opt/components
ENV CONTAINER_COMPONENTS_PATH="/opt/components"

ENV DEPLOY_ASSET_LOADER_JS=false

RUN mkdir /data
ADD docker/scripts/main /data/main 
RUN chmod +x /data/main

EXPOSE 9000

ADD target/universal/stage/ /opt/corespring-api

CMD ["/data/main" ] 

# Clean up APT when done.
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

