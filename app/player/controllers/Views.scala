package player.controllers

import common.controllers.QtiResource
import controllers.auth.{TokenizedRequestActionBuilder, BaseApi}
import org.bson.types.ObjectId
import org.xml.sax.SAXParseException
import play.api.mvc.Action
import play.api.templates.Html
import player.accessControl.auth.CheckSessionAccess
import player.accessControl.cookies.PlayerCookieWriter
import player.accessControl.models.RequestedAccess
import player.views.models.{QtiKeys, ExceptionMessage, PlayerParams}
import org.corespring.qti.models.RenderingMode
import RenderingMode._
import scala.xml.Elem
import org.corespring.platform.core.models.item.service.{ItemServiceImpl, ItemService, ItemServiceClient}
import org.corespring.platform.data.mongo.models.VersionedId
import common.controllers.deployment.{AssetsLoaderImpl, AssetsLoader}
import RenderingMode.RenderingMode
import org.corespring.platform.core.models.itemSession.DefaultItemSession
import org.corespring.platform.core.models.quiz.basic.Quiz
import org.corespring.platform.core.models.versioning.VersionedIdImplicits


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

    def isPrintMode: Boolean = tab != ""

    val p = RenderParams(
      itemId = itemId,
      sessionMode = RequestedAccess.Mode.Preview,
      renderingMode = if (isPrintMode) Printing else Web,
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
                                    templateFn: PlayerParams => Html = defaultTemplate,
                                    assetsLoader: AssetsLoader = AssetsLoaderImpl) {

    def toRequestedAccess: RequestedAccess = RequestedAccess.asRead(
      itemId = Some(itemId),
      sessionId = sessionId,
      assessmentId = assessmentId,
      mode = Some(sessionMode)
    )

    def toPlayerParams(xml: String, qtiKeys: QtiKeys): PlayerParams = {
      import VersionedIdImplicits.Binders._
      PlayerParams(
        xml,
        Some(versionedIdToString(itemId)),
        sessionId.map(_.toString),
        enablePreview,
        qtiKeys,
        renderingMode,
        assetsLoader  )
    }

    def enablePreview: Boolean = sessionMode == RequestedAccess.Mode.Preview

  }

  protected def renderItem(params: RenderParams) = auth.ValidatedAction(params.toRequestedAccess) {
    tokenRequest =>
      ApiAction {
        implicit request => prepareHtml(params, request.ctx.organization) match {
          case Some(html: Html) => Ok(html).withSession(request.session + activeModeCookie(params.sessionMode))
          case None => NotFound("not found")
        }
      }(tokenRequest)
  }

  protected def prepareHtml(params: RenderParams, orgId: ObjectId): Option[Html] =
    try {
      getItemXMLByObjectId(params.itemId, orgId) match {
        case Some(xmlData: Elem) => {
          val qtiKeys = QtiKeys((xmlData \ "itemBody")(0))
          val finalXml = prepareQti(xmlData, params.renderingMode)
          val playerParams = params.toPlayerParams(finalXml, qtiKeys)
          Some(params.templateFn(playerParams))
        }
        case None => None
      }
    } catch {
      case e: SAXParseException => {
        val errorInfo = ExceptionMessage(e.getMessage, e.getLineNumber, e.getColumnNumber)
        Some(player.views.html.PlayerError(errorInfo))
      }
    }


  /** Allow the default player to be overriden */
  protected def defaultTemplate: (PlayerParams => Html) = PlayerTemplates.default

}

object Views extends Views(CheckSessionAccess, ItemServiceImpl)
