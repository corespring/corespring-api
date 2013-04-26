package player.accessControl.models

import controllers.auth.Permission
import org.bson.types.ObjectId

class RequestedAccess( val itemId:Option[ContentRequest] = None, val sessionId:Option[ContentRequest] = None, val assessmentId:Option[ContentRequest] = None, val mode:Option[String] = Some("*"))

object RequestedAccess{
  val PREVIEW_MODE = "preview"
  val ADMINISTER_MODE = "administer"
  val RENDER_MODE = "render"
  val AGGREGATE_MODE = "aggregate"
  def apply(itemId:Option[ObjectId] = None, sessionId:Option[ObjectId] = None, assessmentId:Option[ObjectId] = None, mode:Option[String] = None):RequestedAccess = {
    def toContentRequest(optid:Option[ObjectId]):Option[ContentRequest] = optid.map(id => ContentRequest(id,Permission.Read))
    new RequestedAccess(toContentRequest(itemId), toContentRequest(sessionId), toContentRequest(assessmentId), mode)
  }
}
