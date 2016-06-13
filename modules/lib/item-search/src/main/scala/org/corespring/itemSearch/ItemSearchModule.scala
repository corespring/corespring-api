package org.corespring.itemSearch

import java.net.URL

import org.corespring.elasticsearch.{ ContentIndex, Indices, ServerClient }
import org.corespring.itemSearch.AggregateType._
import org.corespring.models.item.ComponentType

case class ItemSearchConfig(url: URL)

trait ItemSearchModule {

  import com.softwaremill.macwire.MacwireMacros._

  def componentTypes: Seq[ComponentType]
  def elasticSearchExecutionContext: ElasticSearchExecutionContext
  def elasticSearchConfig: ElasticSearchConfig

  private lazy val serverClient = ServerClient(elasticSearchConfig.url)
  private lazy val indices = new Indices(serverClient)(elasticSearchExecutionContext.context)
  private lazy val contentIndex: ContentIndex = new ContentIndex(indices.index("content", "content"), indices)(elasticSearchExecutionContext.context)
  lazy val itemIndexService: ItemIndexService = wire[ElasticSearchItemIndexService]
  lazy val itemType: ItemType = wire[ItemType]
  lazy val widgetType: WidgetType = wire[WidgetType]
}
