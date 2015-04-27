package org.corespring.platform.core.services.item

import org.corespring.platform.core.models.JsonUtil
import play.api.libs.json._

/**
 * Contains fields used for querying the item index
 */
case class ItemIndexAggregation(name: String = "aggregation", field: String)

object ItemIndexAggregation {

  object Writes extends Writes[ItemIndexAggregation] {
    override def writes(itemIndexAggregation: ItemIndexAggregation) = {
      import itemIndexAggregation._
      Json.obj(
        "aggs" -> Json.obj(
          name -> Json.obj(
            "terms" -> Json.obj(
              "field" -> field,
              "size" -> 100
            )
          )
        )
      )
    }
  }

}