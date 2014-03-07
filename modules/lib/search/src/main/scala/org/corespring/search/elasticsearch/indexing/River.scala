package org.corespring.search.elasticsearch.indexing

case class River(name: String, typ: String, collection: String, script: Option[String] = None)
