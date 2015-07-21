package org.corespring.itemSearch

import org.corespring.models.json.JsonUtil
import play.api.libs.json._

/**
 * Contains fields used for querying the item index
 */
case class ItemIndexAggregation(name: String = "aggregation", field: String,
                                collectionIds: Seq[String] = Seq.empty[String])

object ItemIndexAggregation {

  object Writes extends Writes[ItemIndexAggregation] with JsonUtil {
    override def writes(itemIndexAggregation: ItemIndexAggregation) = {
      import itemIndexAggregation._
      partialObj(
        "query" -> (collectionIds.nonEmpty match {
          case true => Some(Json.obj(
            "terms" -> Json.obj(
              "collectionId" -> collectionIds
            )
          ))
          case _ => None
        }),
        "aggs" -> Some(Json.obj(
          name -> Json.obj(
            "terms" -> Json.obj(
              "field" -> field,
              "size" -> 100
            )
          )
        )
      ))
    }
  }

}