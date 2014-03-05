package index

import org.corespring.search.indexing.Indexer

class IndexerTask extends Task {

  def run {
    Indexer.initialize()
  }

}

