package player.controllers

import common.controllers.QtiResource
import controllers.auth.{TokenizedRequestActionBuilder, BaseApi}
import models.itemSession.{DefaultItemSession, ItemSession}
import models.quiz.basic.Quiz
import org.bson.types.ObjectId
import org.xml.sax.SAXParseException
import play.api.mvc.Action
import play.api.templates.Html
import player.accessControl.auth.{CheckSessionAccess, CheckSession}
import player.accessControl.cookies.PlayerCookieWriter
import player.accessControl.models.RequestedAccess
import player.views.models.{ExceptionMessage, PlayerParams}
import qti.models.RenderingMode._
import scala.xml.Elem
import models.item.service.{ItemServiceImpl, ItemService, ItemServiceClient}
import org.corespring.platform.data.mongo.models.VersionedId


class Views(auth: TokenizedRequestActionBuilder[RequestedAccess], val itemService : ItemService)
  extends BaseApi
  with QtiResource
  with ItemServiceClient
  with QtiRenderer
  with PlayerCookieWriter {


  private object PlayerTemplates {
    def default(p: PlayerParams): play.api.templates.Html = player.views.html.Player(p)
  }

  def preview(itemId: VersionedId[ObjectId]) = {
    val p = RenderParams(itemId, sessionMode = RequestedAccess.Mode.Preview)
    renderItem(p)
  }

  def render(sessionId: ObjectId) = {
    DefaultItemSession.get(sessionId) match {
      case Some(session) => {
        val p = RenderParams(itemId = session.itemId, sessionId = Some(sessionId), sessionMode = RequestedAccess.Mode.Render)
        renderItem(p)
      }
      case None => Action(NotFound("not found"))
    }
  }

  def administerItem(itemId: VersionedId[ObjectId] ) = {
    val p = RenderParams(itemId = itemId, sessionMode = RequestedAccess.Mode.Administer )
    renderItem(p)
  }

  def administerSession(sessionId: ObjectId) = {
    DefaultItemSession.get(sessionId) match {
      case Some(session) => {
        val p = RenderParams(itemId = session.itemId, sessionId = Some(sessionId), sessionMode = RequestedAccess.Mode.Administer)
        renderItem(p)
      }
      case None => Action(request => NotFound("not found"))
    }
  }

  def aggregate(assessmentId: ObjectId, itemId: VersionedId[ObjectId]) = {

    Quiz.findOneById(assessmentId) match {
      case Some(id) => {
        def renderAggregatePlayer(assessmentId: ObjectId)(p: PlayerParams) = player.views.html.aggregatePlayer(p, assessmentId.toString)
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

  def profile(itemId: VersionedId[ObjectId], tab: String) = {

    def isPrintMode : Boolean = tab != ""

    val p = RenderParams(
      itemId = itemId,
      sessionMode = RequestedAccess.Mode.Preview,
      renderingMode = if(isPrintMode) Printing else Web,
      templateFn = player.views.html.Profile(isPrintMode, tab)
    )

    renderItem(p)
  }

  /** An internal model of the rendering parameters
    * TODO: renderingMode + sessionMode - can we conflate these to one concept?
    */
  protected case class RenderParams(itemId: VersionedId[ObjectId],
                          sessionMode: RequestedAccess.Mode.Mode,
                          renderingMode: RenderingMode = Web,
                          sessionId: Option[ObjectId] = None,
                          assessmentId: Option[ObjectId] = None,
                          templateFn: PlayerParams => Html = defaultTemplate) {

    def toRequestedAccess: RequestedAccess = RequestedAccess.asRead(
      itemId = Some(itemId),
      sessionId = sessionId,
      assessmentId = assessmentId,
      mode = Some(sessionMode)
    )

    def toPlayerParams(xml: String): PlayerParams = {
      import models.versioning.VersionedIdImplicits.Binders._
      PlayerParams(xml, Some(versionedIdToString(itemId)), sessionId.map(_.toString), enablePreview)
    }

    def enablePreview: Boolean = sessionMode == RequestedAccess.Mode.Preview

  }

  protected def renderItem(params: RenderParams) = auth.ValidatedAction(params.toRequestedAccess) {
    tokenRequest =>
      ApiAction {
        implicit request =>
          try {
            getItemXMLByObjectId(params.itemId, request.ctx.organization) match {
              case Some(xmlData: Elem) => {
                val finalXml = prepareQti(xmlData, params.renderingMode)
                val playerParams = params.toPlayerParams(finalXml)
                Ok(params.templateFn(playerParams)).withSession(request.session + activeModeCookie(params.sessionMode))
              }
              case None => NotFound("not found")
            }
          } catch {
            case e: SAXParseException => {
              val errorInfo = ExceptionMessage(e.getMessage, e.getLineNumber, e.getColumnNumber)
              Ok(player.views.html.PlayerError(errorInfo))
            }
          }
      }(tokenRequest)
  }

  /** Allow the default player to be overriden */
  protected def defaultTemplate: (PlayerParams => Html) = PlayerTemplates.default

}

object Views extends Views(CheckSessionAccess, ItemServiceImpl)
