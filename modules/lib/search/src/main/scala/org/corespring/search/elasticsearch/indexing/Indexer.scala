package org.corespring.search.elasticsearch.indexing


trait Indexer {

  def initialize() = {
    dropAll()
    importMapping()
    createRivers()
  }

  protected def dropAll()

  protected def importMapping()

  protected def createRivers()

}
