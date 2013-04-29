package player.controllers

import common.controllers.SimpleJsRoutes
import controllers.auth.TokenizedRequestActionBuilder
import org.bson.types.ObjectId
import play.api.mvc._
import player.accessControl.auth.{AccessGranterChecker, CheckPlayerSession}
import player.accessControl.models.RequestedAccess
import scala.Some


class Session(auth: TokenizedRequestActionBuilder[RequestedAccess]) extends Controller with SimpleJsRoutes {

  import api.v1.{ItemSessionApi => Api}

  def create(itemId: ObjectId) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId))
  )(Api.create(itemId))

  def read(itemId: ObjectId, sessionId: ObjectId) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId), Some(sessionId))
  )(Api.get(itemId, sessionId))

  def update(itemId: ObjectId, sessionId: ObjectId, action: Option[String] = None) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId), Some(sessionId))
  )(Api.update(itemId, sessionId, action))

  def aggregate(quizId: ObjectId, itemId: ObjectId) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId), assessmentId = Some(quizId))
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

//object Session extends Session(AccessGranterChecker)
object Session extends Session(CheckPlayerSession)
