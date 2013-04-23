package player.controllers

import common.controllers.SimpleJsRoutes
import controllers.auth.{BaseRender}
import org.bson.types.ObjectId
import play.api.mvc._
import auth.{RequestedAccess, Authenticate}


class Session(auth: Authenticate[AnyContent]) extends Controller with SimpleJsRoutes {

  import api.v1.{ItemSessionApi => Api}

  def create(itemId: ObjectId) = auth.OrgAction(
    RequestedAccess(Some(itemId))
  )(Api.create(itemId))

  def read(itemId: ObjectId, sessionId: ObjectId) = auth.OrgAction(
    RequestedAccess(Some(itemId), Some(sessionId))
  )(Api.get(itemId, sessionId))

  def update(itemId: ObjectId, sessionId: ObjectId, action: Option[String] = None) = auth.OrgAction(
    RequestedAccess(Some(itemId), Some(sessionId))
  )(Api.update(itemId, sessionId, action))

  def aggregate(quizId: ObjectId, itemId: ObjectId) = auth.OrgAction(
    RequestedAccess(Some(itemId))
  )(Api.aggregate(quizId, itemId))

  def jsRoutes = Action {
    implicit request =>
      import routes.javascript.{Session => JsSession}
      val jsRoutes = List(
        JsSession.create,
        JsSession.read,
        JsSession.aggregate,
        JsSession.update
      )
      Ok(createSimpleRoutes("PlayerRoutes", jsRoutes: _*))
        .as("text/javascript")
  }
}

//object Session extends Session(AllowEverything)
object Session extends Session(BaseRender)
