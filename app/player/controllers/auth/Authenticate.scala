package player.controllers.auth

import play.api.mvc.{Result, BodyParser, Action}
import player.models.TokenizedRequest
import controllers.auth.{Permission}
import org.bson.types.ObjectId

trait Authenticate[A] {
  //def OrgAction(p: BodyParser[A])(block: TokenizedRequest[A] => Result): Action[A]
  //def OrgAction(block: TokenizedRequest[A] => Result): Action[A]

  def OrgAction(access:RequestedAccess)(block: TokenizedRequest[A] => Result) : Action[A]
  def OrgAction(p:BodyParser[A])(access:RequestedAccess)(block: TokenizedRequest[A] => Result) : Action[A]
}
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