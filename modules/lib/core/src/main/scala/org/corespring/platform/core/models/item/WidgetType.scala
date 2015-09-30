package org.corespring.platform.core.models.item

import org.corespring.platform.core.services.item.{ ElasticSearchItemIndexService, ItemIndexService }
import play.api.libs.json.{ Json, JsArray }

import scala.concurrent.Await

class WidgetType(itemIndexService: ItemIndexService) {

  val all = JsArray({
    import scala.concurrent.duration._
    Await.result(itemIndexService.widgetTypes, Duration(10, SECONDS))
      .getOrElse(throw new Exception("Could not run aggregate query on ElasticSearch node"))
  }.map { case (value, key) => Json.obj("key" -> key, "value" -> value) }.toSeq)

}

object WidgetType extends WidgetType(ElasticSearchItemIndexService)