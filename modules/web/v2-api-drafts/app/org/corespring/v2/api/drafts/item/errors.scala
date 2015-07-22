package org.corespring.v2.api.drafts.item

import org.corespring.drafts.item.models.{ Conflict, ItemDraft }
import org.corespring.models.item.Item
import play.api.http.Status._
import play.api.libs.json.{ Json, JsValue }

sealed abstract class DraftApiResult(val msg: String, val statusCode: Int) {
  def json: JsValue
}

sealed abstract class DraftApiError(
  override val msg: String,
  override val statusCode: Int = BAD_REQUEST)
  extends DraftApiResult(msg, statusCode) {
  def json = Json.obj("error" -> msg)
}

case object AuthenticationFailed extends DraftApiError("Auth failed", UNAUTHORIZED)

case class cantParseItemId(id: String) extends DraftApiError(s"Can't parse: $id")

case class draftCreationFailed(id: String) extends DraftApiError(s"Draft creation failed for item $id")

case class cantLoadDraft(id: String) extends DraftApiError(s"Cant load draft: $id")

case class draftIsOutOfDate(d: ItemDraft, item: Item, toJson: Conflict => JsValue) extends DraftApiError(s"The draft is out of date", CONFLICT) {
  override def json = Json.obj(
    "error" -> msg,
    "details" -> toJson(Conflict(d, item)))
}

case class nothingToCommit(id: String) extends DraftApiResult(s"nothing to commit for id: $id", ACCEPTED) {
  def json = Json.obj("note" -> msg)
}

case class generalDraftApiError(override val msg: String) extends DraftApiError(msg)
