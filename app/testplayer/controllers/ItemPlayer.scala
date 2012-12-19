package testplayer.controllers

import org.xml.sax.SAXParseException
import xml.Elem
import org.bson.types.ObjectId
import controllers.auth.BaseApi
import models._
import common.controllers.ItemResources
import play.api.mvc._
import testplayer.models.ExceptionMessage
import scala.Some
import play.api._


object ItemPlayer extends BaseApi with ItemResources with QtiRenderer{


  def javascriptRoutes = Action { implicit request =>

    import api.v1.routes.javascript._

    Ok(
      Routes.javascriptRouter("TestPlayerRoutes")(
        ItemSessionApi.update,
        ItemSessionApi.get,
        ItemSessionApi.create
      )
    ).as("text/javascript")
  }

  def renderItemBySessionId(sessionId:String, printMode : Boolean = false) = {
    callRenderBySessionId(
      _renderItem(_,_, previewEnabled = false, sessionSettings = "", sessionId = sessionId),
      sessionId,
      printMode)
  }

  def previewItemBySessionId(sessionId: String, printMode: Boolean = false) = {
    callRenderBySessionId(
      _renderItem(_,_, previewEnabled = true, sessionSettings = "", sessionId = sessionId),
      sessionId,
      printMode)
  }

  private def callRenderBySessionId(renderFn : (String,Boolean) => Action[AnyContent], id : String, printMode : Boolean ) = {
    ItemSession.findOneById(new ObjectId(id)) match {
      case Some(session) => {
        renderFn(session.itemId.toString, printMode)
      }
      case _ => throw new RuntimeException("Can't find item session: " + id)
    }
  }

  def getDataFileBySessionId(sessionId: String, filename: String) = {

    ItemSession.findOneById(new ObjectId(sessionId)) match {
      case Some(session) => getDataFile(session.itemId.toString, filename)
      case _ => Action(NotFound("sessionId: " + sessionId))
    }
  }

  def previewItem(itemId: String, printMode: Boolean = false, sessionSettings: String = "") =
    _renderItem(itemId, printMode, previewEnabled = !printMode, sessionSettings = sessionSettings)

  def renderItem(itemId: String, printMode: Boolean = false, sessionSettings: String = "") =
    _renderItem(itemId, printMode, previewEnabled = false, sessionSettings = sessionSettings)

  def renderSelectTextItem() = ApiAction {
    request =>
      try {
             getItemXMLByObjectId("5f6c9fbdc01de23b24788176", request.ctx.organization) match {
               case Some(xmlData: Elem) =>
                 println("Rendering QTI")
                 val finalXml = prepareQti(xmlData, false)
                Ok(testplayer.views.html.itemPlayer("5f6c9fbdc01de23b24788176", finalXml, true, "", common.mock.MockToken))
             }
      }catch {
              case e: SAXParseException => {
                val errorInfo = ExceptionMessage(e.getMessage, e.getLineNumber, e.getColumnNumber)
                Ok(testplayer.views.html.itemPlayerError(errorInfo))
              }
              case e: Exception => throw new RuntimeException("ItemPlayer.renderItem: " + e.getMessage, e)
        }

  }


  private def _renderItem(itemId: String, printMode: Boolean = false, previewEnabled: Boolean = false, sessionSettings: String = "", sessionId:String = "") = ApiAction {
    request =>
      try {
        getItemXMLByObjectId(itemId, request.ctx.organization) match {
          case Some(xmlData: Elem) =>

            val finalXml = prepareQti(xmlData, printMode)

            if(Play.isDev(play.api.Play.current) && request.token == null){
              println("Mock Token Session")
              Ok(testplayer.views.html.itemPlayer(itemId, finalXml, previewEnabled, sessionId, common.mock.MockToken))
            }
            else {
              Ok(testplayer.views.html.itemPlayer(itemId, finalXml, previewEnabled, sessionId, request.token))
            } 

          case None =>
            NotFound("not found")
        }
      } catch {
        case e: SAXParseException => {
          val errorInfo = ExceptionMessage(e.getMessage, e.getLineNumber, e.getColumnNumber)
          Ok(testplayer.views.html.itemPlayerError(errorInfo))
        }
        case e: Exception => throw new RuntimeException("ItemPlayer.renderItem: " + e.getMessage, e)
      }
  }

}
