package player.controllers

import common.controllers.QtiResource
import controllers.auth.{BaseRender, BaseApi}
import org.bson.types.ObjectId
import org.xml.sax.SAXParseException
import play.api.mvc.{AnyContent, Action}
import play.api.templates.Html
import player.controllers.auth.{CheckPlayerSession, RequestedAccess, Authenticate}
import player.models.PlayerParams
import qti.models.RenderingMode._
import scala.xml.Elem
import testplayer.controllers.QtiRenderer
import testplayer.models.ExceptionMessage
import models.itemSession.ItemSession
import models.quiz.basic.Quiz
import player.rendering.PlayerCookieWriter

class Views(auth: Authenticate[AnyContent]) extends BaseApi with QtiResource with QtiRenderer with PlayerCookieWriter {


  private object PlayerTemplates {
    def default(p: PlayerParams): play.api.templates.Html = player.views.html.Player(p)

    def iframed(p: PlayerParams): play.api.templates.Html = player.views.html.IframedPlayer(p)

    def instructor(p: PlayerParams): play.api.templates.Html = player.views.html.Player(p)

    def profile(p: PlayerParams): play.api.templates.Html = player.views.html.Profile(p)
  }

  def preview(itemId: ObjectId) = renderItem(itemId.toString, previewEnabled = true,mode = RequestedAccess.PREVIEW_MODE)

  def render(sessionId: ObjectId) = {
    ItemSession.get(sessionId) match {
      case Some(session) => renderItem(itemId = ItemSession.get(sessionId).get.itemId.toString, sessionId = Some(sessionId.toString),mode = RequestedAccess.RENDER_MODE)
      case None => Action( request => NotFound("not found") )
    }
  }

  def administerItem(itemId: ObjectId) = renderItem(itemId.toString, previewEnabled = false, mode = RequestedAccess.ADMINISTER_MODE)

  def administerSession(sessionId: ObjectId) = render(sessionId)

  def aggregate(assessmentId: ObjectId, itemId: ObjectId) = renderQuizAsAggregate(assessmentId, itemId)

  def profile(itemId:ObjectId) = renderItem(itemId.toString, previewEnabled = true, template = PlayerTemplates.profile, mode = RequestedAccess.PREVIEW_MODE)


  protected def renderItem(itemId: String,
                          renderMode: RenderingMode = Web,
                          previewEnabled: Boolean = false,
                          sessionId: Option[String] = None,
                          mode:String,
                          template: PlayerParams => Html = PlayerTemplates.default) = auth.OrgAction(
    RequestedAccess(Some(new ObjectId(itemId)),sessionId.map(new ObjectId(_)),mode = Some(mode))
  ) {
    tokenRequest =>
      ApiAction {
        implicit request =>
          try {
            getItemXMLByObjectId(itemId, request.ctx.organization) match {
              case Some(xmlData: Elem) => {
                val finalXml = prepareQti(xmlData, renderMode)
                val params = PlayerParams(finalXml, Some(itemId), sessionId, previewEnabled)
                Ok(template(params)).withSession( request.session + activeModeCookie(mode))
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


  def renderQuizAsAggregate(quizId: ObjectId, itemId: ObjectId) = auth.OrgAction(
    RequestedAccess(itemId = Some(itemId), assessmentId = Some(quizId), mode = Some(RequestedAccess.AGGREGATE_MODE))
  ) {
    tokenRequest =>
      ApiAction {
        request =>
          Quiz.findOneById(quizId) match {
            case Some(q) =>
              try {
                getItemXMLByObjectId(itemId.toString, request.ctx.organization) match {
                  case Some(xmlData: Elem) =>
                    val finalXml = prepareQti(xmlData, Aggregate)
                    Ok(player.views.html.aggregatePlayer(itemId.toString, finalXml, quizId.toString))
                  case None =>
                    NotFound("not found")
                }
              } catch {
                case e: SAXParseException => {
                  val errorInfo = ExceptionMessage(e.getMessage, e.getLineNumber, e.getColumnNumber)
                  Ok(testplayer.views.html.itemPlayerError(errorInfo))
                }
              }
            case _ => NotFound
          }
      }(tokenRequest)

  }

}

/*

}
 */
object Views extends Views(CheckPlayerSession)
