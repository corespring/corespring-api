package index

import org.corespring.search.indexing.IndexerWs

class IndexerTask extends Task {

  def run {
    IndexerWs.initialize()
  }

}

