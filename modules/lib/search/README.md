# Corespring Search

## Indexing

Indexing is managed through [elasticsearch](http://www.elasticsearch.org/).

### Local Setup

The following steps are used specifically to configure elasticsearch on your local machine.

#### Setting up MongoDB Locally

You must use Replica Sets on your local MongoDB instance if you want to use elasticsearch locally. If you've installed mongodb via Homebrew, you can do this by using

    $ mongo
    > rs.initiate({ _id: "rs0", members: [{ _id: 0, host: "localhost:27017"}]}
    {
    	"info" : "Config now saved locally.  Should come online in about a minute.",
    	"ok" : 1
    }

You can check that this worked:

    > rs.status()
    {
    	"set" : "rs0",
    	"date" : ISODate("2014-02-25T21:29:18Z"),
    	"myState" : 1,
    	"members" : [
    		{
    			"_id" : 0,
    			"name" : "localhost:27017",
    			"health" : 1,
    			"state" : 1,
    			"stateStr" : "PRIMARY",
    			"uptime" : 296,
    			"optime" : Timestamp(1393363696, 1),
    			"optimeDate" : ISODate("2014-02-25T21:28:16Z"),
    			"self" : true
    		}
	    ],
    	"ok" : 1
    }
  

import play.api.Play
import play.api.test.FakeApplication
import index.Indexer
Play.start(FakeApplication())
Indexer.initialize()

#### Install ElasticSearch

If you're on a Mac using OS X, you can install elasticsearch from Homebrew:

    $ brew install elasticsearch

It's recommended to follow the post-installation to set up a daemon to run elasticsearch on boot/when it goes down.

You also need to set the cluster name in your `elasticsearch.yml` file. Using Homebrew, this file will be in
`/usr/local/Cellar/elasticsearch/1.0.0/config/elasticsearch.yml`. You should set the `cluster.name` value to
`corespring_elasticsearch`:

  cluster.name: corespring_elasticsearch


### Install the Javascript Plugin

    $ plugin -install elasticsearch/elasticsearch-lang-javascript/2.0.0.RC1


### Install MongoDB River Plugin

Install the Corespring-specific version of the plugin from our [Github repository](https://github.com/corespring/elasticsearch-river-monogdb):

    $ plugin --url "https://github.com/corespring/elasticsearch-river-mongodb/raw/releases/bin/elasticsearch-river-mongodb-corespring-2.0.0.zip" --install elasticsearch-river-mongodb
    -> Installing com.github.richardwilly98.elasticsearch/elasticsearch-river-mongodb/2.0.0...
    Downloading ...... DONE
    Installed com.github.richardwilly98.elasticsearch/elasticsearch-river-mongodb/2.0.0 into /usr/local/var/lib/elasticsearch/plugins/river-mongodb

After you have installed the plugin you will have to restart elasticsearch (if you have a daemon running, you can `kill -9` the process and it will restart itself).


### Install the Marvel Plugin

In order to get a better look at what's going on with your Elasticsearch instance, it will be useful to install Elasticsearch's [Marvel plugin](http://www.elasticsearch.org/guide/en/marvel/current/):

    $ plugin -i elasticsearch/marvel/latest
    -> Installing elasticsearch/marvel/latest...
    Trying http://download.elasticsearch.org/elasticsearch/marvel/marvel-latest.zip...
    Downloading ...... DONE
    Installed elasticsearch/marvel/latest into /usr/local/var/lib/elasticsearch/plugins/marvel

After you have installed the plugin, and restarted elasticsearch, you can visit (http://localhost:9200/_plugin/marvel/) to view the Marvel dashboard.


### Configuration

Note that these configuration examples are for demonstration purposes only. The [ElasticSearch](Indexer.scala) object's `initialize` method should generate all the necessary rivers and indexes.

#### Creating a MongoDB River

You have to configure the River plugin to point to your local MongoDB instance:

    $ curl -XPUT 'http://localhost:9200/_river/content/_meta' -d '{
      "type": "mongodb", 
      "mongodb": { 
        "db": "api", 
        "collection": "content", 
        "gridfs": false
      }, 
      "index": { 
        "name": "content", 
        "type": "documents" 
      }
    }'
    
    {"_index":"_river","_type":"content","_id":"_meta","_version":1,"created":true}


### Troubleshooting

Stream Corruption Error: Make sure you are talking to the right port. Web access goes through port 9200. Port 9300 is
for binary communication. See http://stackoverflow.com/questions/17854910/elasticsearch-0-90-2-streamcorruptedexception-on-asking-port-9300
