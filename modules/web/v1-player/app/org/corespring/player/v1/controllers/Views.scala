package org.corespring.player.v1.controllers

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.QtiResource
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.platform.core.models.itemSession.DefaultItemSession
import org.corespring.platform.core.services.assessment.basic.AssessmentService
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.core.services.item.{ ItemServiceClient, ItemService }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.auth.{ CheckSessionAccess, TokenizedRequestActionBuilder }
import org.corespring.player.accessControl.cookies.PlayerCookieWriter
import org.corespring.player.accessControl.models.RequestedAccess
import org.corespring.player.v1.views.models.{ QtiKeys, PlayerParams, ExceptionMessage }
import org.corespring.qti.models.RenderingMode._
import org.corespring.web.common.controllers.deployment.{ AssetsLoader, AssetsLoaderImpl }
import org.xml.sax.SAXParseException
import play.api.mvc.{ SimpleResult, Action }
import play.api.templates.Html
import scala.Some
import scala.concurrent.Future
import scala.xml.Elem

class Views(auth: TokenizedRequestActionBuilder[RequestedAccess], val itemService: ItemService, assessmentService: AssessmentService)
  extends BaseApi
  with QtiResource
  with ItemServiceClient
  with QtiRenderer
  with PlayerCookieWriter {

  private object PlayerTemplates {
    def default(p: PlayerParams): play.api.templates.Html = org.corespring.player.v1.views.html.Player(p)

  }

  def preview(itemId: VersionedId[ObjectId]) = {
    val p = RenderParams(itemId, sessionMode = RequestedAccess.Mode.Preview)
    renderItem(p)
  }

  def render(sessionId: ObjectId, role: String) = {
    DefaultItemSession.get(sessionId)(false) match {
      case Some(session) => {
        val p = RenderParams(itemId = session.itemId, sessionId = Some(sessionId), sessionMode = RequestedAccess.Mode.Render, role = role)
        renderItem(p)
      }
      case None => Action(NotFound("not found"))
    }
  }

  def administerItem(itemId: VersionedId[ObjectId]) = {
    val p = RenderParams(itemId = itemId, sessionMode = RequestedAccess.Mode.Administer)
    renderItem(p)
  }

  def administerSession(sessionId: ObjectId) = {
    DefaultItemSession.get(sessionId)(false) match {
      case Some(session) => {
        val p = RenderParams(itemId = session.itemId, sessionId = Some(sessionId), sessionMode = RequestedAccess.Mode.Administer)
        renderItem(p)
      }
      case None => Action(request => NotFound("not found"))
    }
  }

  def aggregate(assessmentId: ObjectId, itemId: VersionedId[ObjectId]) = {

    assessmentService.findOneById(assessmentId) match {
      case Some(id) => {
        def renderAggregatePlayer(assessmentId: ObjectId)(p: PlayerParams) = org.corespring.player.v1.views.html.aggregatePlayer(p, assessmentId.toString)
        val p = RenderParams(
          itemId = itemId,
          assessmentId = Some(assessmentId),
          renderingMode = Aggregate,
          sessionMode = RequestedAccess.Mode.Aggregate,
          templateFn = renderAggregatePlayer(assessmentId))
        renderItem(p)
      }
      case _ => Action(NotFound("Can't find assessment with id: " + assessmentId))
    }
  }

  def profile(itemId: VersionedId[ObjectId], tab: String, selectedTab: String) = {

    def isPrintMode: Boolean = tab != ""

    val p = RenderParams(
      itemId = itemId,
      sessionMode = RequestedAccess.Mode.Preview,
      renderingMode = if (isPrintMode) Printing else Web,
      templateFn = org.corespring.player.v1.views.html.Profile(isPrintMode, tab, selectedTab))

    renderItem(p)
  }

  /**
   * An internal model of the rendering parameters
   * TODO: renderingMode + sessionMode - can we conflate these to one concept?
   */
  protected case class RenderParams(itemId: VersionedId[ObjectId],
    sessionMode: RequestedAccess.Mode.Mode,
    renderingMode: RenderingMode = Web,
    sessionId: Option[ObjectId] = None,
    assessmentId: Option[ObjectId] = None,
    role: String = "student",
    templateFn: PlayerParams => Html = defaultTemplate,
    assetsLoader: AssetsLoader = AssetsLoaderImpl) {

    def toRequestedAccess: RequestedAccess = RequestedAccess.asRead(
      itemId = Some(itemId),
      sessionId = sessionId,
      assessmentId = assessmentId,
      mode = Some(sessionMode),
      role = Some(role))

    def toPlayerParams(xml: String, qtiKeys: QtiKeys): PlayerParams = {

      PlayerParams(
        xml,
        Some(itemId.toString),
        sessionId.map(_.toString),
        role,
        enablePreview,
        qtiKeys,
        renderingMode,
        assetsLoader)
    }

    def enablePreview: Boolean = sessionMode == RequestedAccess.Mode.Preview

  }

  protected def renderItem(params: RenderParams) = auth.ValidatedAction(params.toRequestedAccess) {
    tokenRequest =>
      ApiAction { implicit request =>
        prepareHtml(params, request.ctx.organization) match {
          case Some(html: Html) => Ok(html).withSession(request.session + activeModeCookie(params.sessionMode))
          case None => NotFound("not found")
        }
      }(tokenRequest)
  }

  protected def prepareHtml(params: RenderParams, orgId: ObjectId): Option[Html] =
    try {
      getItemXMLByObjectId(params.itemId, orgId) match {
        case Some(xmlData: Elem) => {
          val qtiKeys = QtiKeys((xmlData)(0))
          val finalXml = prepareQti(xmlData, params.renderingMode)
          val playerParams = params.toPlayerParams(finalXml, qtiKeys)
          Some(params.templateFn(playerParams))
        }
        case None => None
      }
    } catch {
      case e: SAXParseException => {
        val errorInfo = ExceptionMessage(e.getMessage, e.getLineNumber, e.getColumnNumber)
        Some(org.corespring.player.v1.views.html.PlayerError(errorInfo))
      }
    }

  /** Allow the default player to be overriden */
  protected def defaultTemplate: (PlayerParams => Html) = PlayerTemplates.default

}

object Views extends Views(CheckSessionAccess, ItemServiceWired, AssessmentService)