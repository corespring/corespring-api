package org.corespring.itemSearch

import org.corespring.models.item.ComponentType

trait ItemSearchModule {

  import com.softwaremill.macwire.MacwireMacros._

  def componentTypes: Seq[ComponentType]
  def elasticSearchExecutionContext: ElasticSearchExecutionContext
  def elasticSearchConfig : ElasticSearchConfig

  lazy val itemIndexService: ItemIndexService = wire[ElasticSearchItemIndexService]
  lazy val itemType : ItemType =  wire[ItemType]
  lazy val widgetType : WidgetType =  wire[WidgetType]
}
