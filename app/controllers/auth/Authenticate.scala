package controllers.auth

import play.api.mvc._
import scala.Some
import models.Organization
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId

case class ContentRequest(id:ObjectId,p:Permission)
class RequestedAccess( val itemId:Option[ContentRequest] = None, val sessionId:Option[ContentRequest] = None, val assessmentId:Option[ContentRequest] = None)
object RequestedAccess{
  def apply(itemId:Option[ObjectId] = None, sessionId:Option[ObjectId] = None, assessmentId:Option[ObjectId] = None):RequestedAccess = {
    def toContentRequest(optid:Option[ObjectId]):Option[ContentRequest] = optid.map(id => ContentRequest(id,Permission.Read))
    new RequestedAccess(toContentRequest(itemId), toContentRequest(sessionId), toContentRequest(assessmentId))
  }
}
trait Authenticate extends Results with BodyParsers{
  def ApiAction[A](p: BodyParser[A])(ra:RequestedAccess)(block: ApiRequest[A] => Result): Action[A]

  def ApiAction(ra:RequestedAccess)(block: ApiRequest[AnyContent] => Result): Action[AnyContent] = ApiAction(parse.anyContent)(ra)(block)
}

object NullAuth extends Authenticate {

  def ApiAction[A](p: BodyParser[A])(ra:RequestedAccess)(block: ApiRequest[A] => Result): Action[A] = {
    Action(p) {
      request =>
        Organization.findOne(MongoDBObject("name" -> "Corespring Organization")) match {
          case Some(org) => {
            val context = AuthorizationContext(org.id, None, isSSLogin = false)
            block(ApiRequest(context, request, "token"))
          }
          case _ => throw new RuntimeException("Can't find Corespring Organization")
        }
    }
  }
}
