package org.corespring.v2.api.drafts.item

import play.api.http.Status._
import play.api.libs.json.Json
sealed abstract class DraftApiError(val msg: String, val statusCode: Int = BAD_REQUEST) {
  def json = Json.obj("error" -> msg)
}

case object AuthenticationFailed extends DraftApiError("Auth failed", UNAUTHORIZED)

case class cantParseItemId(id: String) extends DraftApiError(s"Can't parse: $id")

case class draftCreationFailed(id: String) extends DraftApiError(s"Draft creation failed for item $id")

case object UnknownDraftApiError extends DraftApiError("???")