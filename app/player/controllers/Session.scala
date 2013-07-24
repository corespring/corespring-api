package player.controllers

import api.v1.ItemSessionApi
import common.controllers.SimpleJsRoutes
import controllers.auth.TokenizedRequestActionBuilder
import models.itemSession.PreviewItemSessionCompanion
import org.bson.types.ObjectId
import play.api.mvc._
import player.accessControl.auth.CheckSessionAccess
import player.accessControl.cookies.PlayerCookieReader
import player.accessControl.models.RequestedAccess
import player.accessControl.models.RequestedAccess.Mode._
import scala.Some
import models.item.service.ItemServiceImpl
import org.corespring.platform.data.mongo.models.VersionedId


class Session(auth: TokenizedRequestActionBuilder[RequestedAccess]) extends Controller with SimpleJsRoutes with PlayerCookieReader {

  val DefaultApi = ItemSessionApi
  val PreviewApi = new ItemSessionApi(PreviewItemSessionCompanion, ItemServiceImpl)

  /** If we are running in preview mode - return the PreviewApi which will store the sessions in a different collection */
  def api(implicit request: Request[AnyContent]): ItemSessionApi = {
    activeMode(request) match {
      case Some(Preview) => PreviewApi
      case _ => DefaultApi
    }
  }

  def create(itemId: VersionedId[ObjectId]) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId))
  )(implicit request => api.create(itemId)(request))

  def createAndReturn(itemId: VersionedId[ObjectId]) = auth.ValidatedAction(RequestedAccess.asRead(Some(itemId))) {
    implicit request => {
      println("test")
      api.create(itemId)(request)
    }
  }

  def read(itemId: VersionedId[ObjectId], sessionId: ObjectId) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId), Some(sessionId))
  )(implicit request => api.get(itemId, sessionId)(request))

  def update(itemId: VersionedId[ObjectId], sessionId: ObjectId, action: Option[String] = None) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId), Some(sessionId))
  )(implicit request => api.update(itemId, sessionId, action)(request))

  def aggregate(quizId: ObjectId, itemId: VersionedId[ObjectId]) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId), assessmentId = Some(quizId))
  )(implicit request => api.aggregate(quizId, itemId)(request))

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

object Session extends Session(CheckSessionAccess)
