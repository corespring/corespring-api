package org.corespring.passage.search

import play.api.libs.json._

case class PassageIndexSearchResult(total: Int, hits: Seq[PassageIndexHit])

object PassageIndexSearchResult {

  object Format extends Format[PassageIndexSearchResult] {

    implicit val PassageIndexHitFormat = PassageIndexHit.Format

    def reads(json: JsValue): JsResult[PassageIndexSearchResult] = {
      JsSuccess(PassageIndexSearchResult(
        total = (json \ "hits" \ "total").asOpt[Int].getOrElse(0),
        hits = (json \ "hits" \ "hits").asOpt[Seq[PassageIndexHit]].getOrElse(Seq.empty)))
    }

    def writes(itemIndexSearchResult: PassageIndexSearchResult): JsValue = Json.obj(
      "total" -> itemIndexSearchResult.total,
      "hits" -> Json.toJson(itemIndexSearchResult.hits))

  }

  def empty = new PassageIndexSearchResult(0, Seq.empty)

}
