package player.controllers

import api.v1.ItemSessionApi
import common.controllers.SimpleJsRoutes
import org.bson.types.ObjectId
import org.corespring.platform.core.models.itemSession.PreviewItemSessionCompanion
import org.corespring.platform.core.services.assessment.basic.AssessmentService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.auth.{ CheckSessionAccess, TokenizedRequestActionBuilder }
import org.corespring.player.accessControl.cookies.PlayerCookieReader
import org.corespring.player.accessControl.models.RequestedAccess
import org.corespring.player.accessControl.models.RequestedAccess.Mode._
import play.api.mvc._
import scala.Some
import org.corespring.platform.core.services.item.ItemServiceImpl

class Session(auth: TokenizedRequestActionBuilder[RequestedAccess]) extends Controller with SimpleJsRoutes with PlayerCookieReader {

  val DefaultApi = ItemSessionApi
  val PreviewApi = new ItemSessionApi(PreviewItemSessionCompanion, ItemServiceImpl, AssessmentService)

  /** If we are running in preview mode - return the PreviewApi which will store the sessions in a different collection */
  def api(implicit request: Request[AnyContent]): ItemSessionApi = {
    activeMode(request) match {
      case Some(Preview) => PreviewApi
      case _ => DefaultApi
    }
  }

  def create(itemId: VersionedId[ObjectId]) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId)))(implicit request => api.create(itemId)(request))

  def read(itemId: VersionedId[ObjectId], sessionId: ObjectId, role:String) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId), Some(sessionId), role = Some(role)))(implicit request =>
    api.get(itemId, sessionId, role)(request))

  def update(itemId: VersionedId[ObjectId], sessionId: ObjectId, role: String, action: Option[String] = None) = {
    auth.ValidatedAction(
      RequestedAccess.asRead(Some(itemId), Some(sessionId), role = Some(role)))(implicit request =>
      api.update(itemId, sessionId, role, action)(request)
    )
  }

  def aggregate(assessmentId: ObjectId, itemId: VersionedId[ObjectId]) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId), assessmentId = Some(assessmentId)))(implicit request => api.aggregate(assessmentId, itemId)(request))

  def jsRoutes = Action {
    implicit request =>
      import routes.javascript.{ Session => JsSession }
      val jsRoutes = List(
        JsSession.create,
        JsSession.read,
        JsSession.aggregate,
        JsSession.update)
      Ok(createSimpleRoutes("PlayerRoutes", jsRoutes: _*))
        .as("text/javascript")
  }
}

object Session extends Session(CheckSessionAccess)
