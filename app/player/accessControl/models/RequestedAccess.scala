package player.accessControl.models

import controllers.auth.Permission
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId

case class RequestedAccess(
                       itemId: Option[VersionedContentRequest] = None,
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

    def valueOf(string: String) = {
      values.collectFirst{ case i if (i.toString().equals(string)) => i }
    }
  }

  implicit def toContentRequest(id:Option[ObjectId]):Option[ContentRequest] = id.map(ContentRequest(_, Permission.Read))
  implicit def toVersionedContentRequest(id:Option[VersionedId[ObjectId]]):Option[VersionedContentRequest] = id.map(VersionedContentRequest(_, Permission.Read))

  def asRead(itemId: Option[VersionedId[ObjectId]] = None,
             sessionId: Option[ObjectId] = None,
             assessmentId: Option[ObjectId] = None,
             mode: Option[RequestedAccess.Mode.Mode] = None): RequestedAccess = {
    new RequestedAccess(itemId, sessionId, assessmentId, mode)
  }
}
