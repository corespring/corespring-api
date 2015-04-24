package org.corespring.platform.core.models.item.index

import play.api.libs.json._

case class ItemIndexSearchResult(total: Int, hits: Seq[ItemIndexHit])

object ItemIndexSearchResult {

  object Format extends Format[ItemIndexSearchResult] {

    implicit val ItemIndexHitFormat = ItemIndexHit.Format

    def reads(json: JsValue): JsResult[ItemIndexSearchResult] = {
      JsSuccess(ItemIndexSearchResult(
        total = (json \ "hits" \ "total").asOpt[Int].getOrElse(0),
        hits = (json \ "hits" \ "hits").asOpt[Seq[ItemIndexHit]].getOrElse(Seq.empty)
      ))
    }

    def writes(itemIndexSearchResult: ItemIndexSearchResult): JsValue = Json.obj(
      "total" -> itemIndexSearchResult.total,
      "hits" -> Json.toJson(itemIndexSearchResult.hits)
    )

  }

  def empty = new ItemIndexSearchResult(0, Seq.empty)

}
