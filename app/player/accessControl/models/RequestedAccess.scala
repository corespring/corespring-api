package player.accessControl.models

import controllers.auth.Permission
import org.bson.types.ObjectId

case class RequestedAccess(
                       itemId: Option[ContentRequest] = None,
                       sessionId: Option[ContentRequest] = None,
                       assessmentId: Option[ContentRequest] = None,
                       mode: Option[RequestedAccess.Mode.Mode] = Some(RequestedAccess.Mode.All))

object RequestedAccess {
  object Mode extends Enumeration {
    type Mode = Value
    val Preview = Value("preview")
    val Administer = Value("administer")
    val Render = Value("render")
    val Aggregate = Value("aggregate")
    val All = Value("*")
  }


  def asRead(itemId: Option[ObjectId] = None, sessionId: Option[ObjectId] = None, assessmentId: Option[ObjectId] = None, mode: Option[RequestedAccess.Mode.Mode] = None): RequestedAccess = {
    def toContentRequest(optid: Option[ObjectId]): Option[ContentRequest] = optid.map(id => ContentRequest(id, Permission.Read))
    new RequestedAccess(toContentRequest(itemId), toContentRequest(sessionId), toContentRequest(assessmentId), mode)
  }
}
