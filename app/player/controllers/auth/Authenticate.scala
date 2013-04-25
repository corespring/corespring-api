package player.controllers.auth

import controllers.auth.Permission
import org.bson.types.ObjectId
import play.api.mvc._
import player.models.TokenizedRequest
import scala.Some

trait Authenticate[ACCESS_DESCRIPTION, CONTENT <: AnyContent, REQUEST <: Request[CONTENT]] {
  def OrgAction(access:ACCESS_DESCRIPTION)(block: REQUEST => Result) : Action[CONTENT]
  def OrgAction(p:BodyParser[CONTENT])(access:ACCESS_DESCRIPTION)(block: REQUEST => Result) : Action[CONTENT]
}


trait AuthenticateAndUseToken[ACCESS] extends Authenticate[ACCESS,AnyContent, TokenizedRequest[AnyContent]]

/** If you need to proxy an Api call - use this trait */
trait PlayerAuthenticate extends AuthenticateAndUseToken[RequestedAccess]

case class ContentRequest(id:ObjectId,p:Permission)
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