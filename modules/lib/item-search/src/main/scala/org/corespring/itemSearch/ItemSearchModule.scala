package org.corespring.itemSearch

import java.net.URL

import org.corespring.itemSearch.AggregateType._
import org.corespring.models.item.ComponentType

case class ItemSearchConfig(url:URL)

trait ItemSearchModule {

  import com.softwaremill.macwire.MacwireMacros._

  def componentTypes: Seq[ComponentType]
  def elasticSearchExecutionContext: ElasticSearchExecutionContext
  def elasticSearchConfig: ElasticSearchConfig

  lazy val itemIndexService: ItemIndexService = wire[ElasticSearchItemIndexService]
  lazy val itemType: ItemType = wire[ItemType]
  lazy val widgetType: WidgetType = wire[WidgetType]
}
