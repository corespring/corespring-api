package org.corespring.itemSearch

import org.corespring.models.item.ComponentType

trait ItemSearchModule {

  import com.softwaremill.macwire.MacwireMacros._

  def elasticSearchUrl: ElasticSearchUrl
  def componentTypes: Seq[ComponentType]
  def elasticSearchExecutionContext: ElasticSearchExecutionContext

  lazy val itemIndexService: ItemIndexService = wire[ElasticSearchItemIndexService]
}
