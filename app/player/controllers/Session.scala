package player.controllers

import common.controllers.SimpleJsRoutes
import org.bson.types.ObjectId
import play.api.mvc._
import player.controllers.auth.{AllowEverything, Authenticate}


class Session(auth: Authenticate[AnyContent]) extends Controller with SimpleJsRoutes {

  import api.v1.ItemSessionApi

  def create(itemId: ObjectId) = auth.OrgAction(ItemSessionApi.create(itemId))

  def read(itemId: ObjectId, sessionId: ObjectId) = auth.OrgAction(ItemSessionApi.get(itemId, sessionId))

  def update(itemId: ObjectId, sessionId: ObjectId, action: Option[String] = None) = auth.OrgAction(ItemSessionApi.update(itemId, sessionId, action))

  def aggregate(quizId: ObjectId, itemId: ObjectId) = auth.OrgAction(ItemSessionApi.aggregate(quizId, itemId))

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
  }
}

object Session extends Session(AllowEverything)
