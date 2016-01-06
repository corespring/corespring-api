package org.corespring.passage.search

import org.corespring.itemSearch.{ElasticSearchExecutionContext, ElasticSearchConfig}

trait PassageSearchModule {

  import com.softwaremill.macwire.MacwireMacros._

  def elasticSearchExecutionContext: ElasticSearchExecutionContext
  def elasticSearchConfig: ElasticSearchConfig

  lazy val passageIndexService: PassageIndexService = wire[ElasticSearchPassageIndexService]


}
