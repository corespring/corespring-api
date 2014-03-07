package index

import org.corespring.search.elasticsearch.indexing.{IndexerElastic4s, IndexerWs}

class IndexerTask extends Task {

  def run {
    IndexerWs.initialize()

    //The elastic4s based indexer fails with a exception at the moment:
    //org.elasticsearch.env.FailedToResolveConfigException: Failed to resolve config path [names.txt],
    //tried file path [names.txt], path file [C:\Users\rbokel\Documents\workspace\github\corespring-api\
    // config\names.txt], and classpath at org.elasticsearch.env.Environment.resolveConfig(Environment.java:207)

    //IndexerElastic4s.initialize()
  }

}

