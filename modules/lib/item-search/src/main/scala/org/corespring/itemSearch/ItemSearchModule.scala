package org.corespring.itemSearch

import java.net.URL

import com.mongodb.casbah.MongoDB
import org.corespring.elasticsearch.{ ContentDenormalizer, ContentIndex, Indices, WSClient }
import org.corespring.itemSearch.AggregateType._
import org.corespring.models.item.ComponentType

case class ItemSearchConfig(url: URL)

trait ItemSearchModule {

  import com.softwaremill.macwire.MacwireMacros._

  def componentTypes: Seq[ComponentType]
  def elasticSearchExecutionContext: ElasticSearchExecutionContext
  def elasticSearchConfig: ElasticSearchConfig
  def db: MongoDB

  private lazy val indices = Indices(elasticSearchConfig.url)(elasticSearchExecutionContext.context)
  private lazy val contentIndex: ContentIndex = new ContentIndex(indices)(elasticSearchExecutionContext.context)
  private lazy val contentDenormalizer = new ContentDenormalizer(db, elasticSearchConfig.componentPath)(elasticSearchExecutionContext.context)

  lazy val itemIndexService: ItemIndexService = wire[ElasticSearchItemIndexService]
  lazy val itemType: ItemType = wire[ItemType]
  lazy val widgetType: WidgetType = wire[WidgetType]
}
