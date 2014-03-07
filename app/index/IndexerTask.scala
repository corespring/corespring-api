package index

import org.corespring.search.elasticsearch.indexing.IndexerElastic4s

class IndexerTask extends Task {

  def run {
    IndexerElastic4s.initialize()
  }

}

