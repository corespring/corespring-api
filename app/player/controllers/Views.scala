package player.controllers

import common.controllers.QtiResource
import controllers.auth.{BaseRender, RequestedAccess, BaseApi}
import org.bson.types.ObjectId
import org.xml.sax.SAXParseException
import play.api.mvc.{AnyContent, Action}
import play.api.templates.Html
import player.controllers.auth.Authenticate
import player.models.PlayerParams
import qti.models.RenderingMode._
import scala.xml.Elem
import testplayer.controllers.QtiRenderer
import testplayer.models.ExceptionMessage
import models.itemSession.ItemSession

class Views(auth: Authenticate[AnyContent]) extends BaseApi with QtiResource with QtiRenderer {


  private object PlayerTemplates {
    def default(p: PlayerParams): play.api.templates.Html = player.views.html.Player(p)

    def iframed(p: PlayerParams): play.api.templates.Html = player.views.html.IframedPlayer(p)

    def instructor(p: PlayerParams): play.api.templates.Html = player.views.html.Player(p)
  }

  def preview(itemId: ObjectId) = renderItem(itemId.toString, previewEnabled = true)

  def render(sessionId: ObjectId) = {
    ItemSession.get(sessionId) match {
      case Some(session) => renderItem(itemId = ItemSession.get(sessionId).get.itemId.toString, sessionId = Some(sessionId.toString))
      case None => Action( request => NotFound("not found") )
    }
  }

  def administerItem(itemId: ObjectId) = Action {
    request => Ok("todo..")
  }

  def administerSession(sessionId: ObjectId) = Action {
    request => Ok("todo..")
  }

  def aggregate(assessmentId: ObjectId, itemId: ObjectId) = Action {
    request => Ok("todo..")
  }


  private def renderItem(itemId: String,
                          renderMode: RenderingMode = Web,
                          previewEnabled: Boolean = false,
                          sessionId: Option[String] = None,
                          template: PlayerParams => Html = PlayerTemplates.default) = auth.OrgAction(
    RequestedAccess(Some(new ObjectId(itemId)))
  ) {
    tokenRequest =>
      ApiAction {
        request =>
          try {
            getItemXMLByObjectId(itemId, request.ctx.organization) match {
              case Some(xmlData: Elem) => {
                val finalXml = prepareQti(xmlData, renderMode)
                val params = PlayerParams(finalXml, Some(itemId), sessionId, previewEnabled)
                Ok(template(params))
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
}

/*

}
 */
object Views extends Views(BaseRender)
