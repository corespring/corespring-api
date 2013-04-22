package controllers.auth

import org.bson.types.ObjectId

case class ContentRequest(id:ObjectId,p:Permission)
class RequestedAccess( val itemId:Option[ContentRequest] = None, val sessionId:Option[ContentRequest] = None, val assessmentId:Option[ContentRequest] = None)
object RequestedAccess{
  def apply(itemId:Option[ObjectId] = None, sessionId:Option[ObjectId] = None, assessmentId:Option[ObjectId] = None):RequestedAccess = {
    def toContentRequest(optid:Option[ObjectId]):Option[ContentRequest] = optid.map(id => ContentRequest(id,Permission.Read))
    new RequestedAccess(toContentRequest(itemId), toContentRequest(sessionId), toContentRequest(assessmentId))
  }
}
/*trait Authenticate extends Results with BodyParsers{
  def ApiAction[A](p: BodyParser[A])(ra:RequestedAccess)(block: ApiRequest[A] => Result): Action[A]

  def ApiAction(ra:RequestedAccess)(block: ApiRequest[AnyContent] => Result): Action[AnyContent] = ApiAction(parse.anyContent)(ra)(block)
}*/

