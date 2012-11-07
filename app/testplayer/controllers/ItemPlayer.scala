package testplayer.controllers

import org.xml.sax.SAXParseException
import xml.Elem
import play.api.libs.json.Json
import org.bson.types.ObjectId
import controllers.auth.{Permission, BaseApi}
import models._
import qti.processors.FeedbackProcessor._
import common.controllers.ItemResources
import qti.processors.FeedbackProcessor
import play.api.mvc._
import testplayer.models.ExceptionMessage
import scala.Some
import models.Content
import play.api.Routes
import play.api._
import play.api.Play.current


object ItemPlayer extends BaseApi with ItemResources {

  val MOCK_ACCESS_TOKEN = "34dj45a769j4e1c0h4wb"

  val NamespaceRegex = """xmlns.*?=".*?"""".r

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

  def previewItemBySessionId(sessionId: String, printMode: Boolean = false) = {

    ItemSession.findOneById(new ObjectId(sessionId)) match {
      case Some(session) => {
        _renderItem(session.itemId.toString, printMode)
      }
      case _ => throw new RuntimeException("Can't find item session: " + sessionId)
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


  private def _renderItem(itemId: String, printMode: Boolean = false, previewEnabled: Boolean = false, sessionSettings: String = "") = ApiAction {
    request =>
      try {
        getItemXMLByObjectId(itemId, request.ctx.organization) match {
          case Some(xmlData: Elem) =>

            val (xmlWithCsFeedbackIds, _) = FeedbackProcessor.addFeedbackIds(xmlData)

            val itemBody = filterFeedbackContent(addOutcomeIdentifiers(xmlWithCsFeedbackIds) \ "itemBody")

            val qtiXml = <assessmentItem print-mode={printMode.toString}>{itemBody}</assessmentItem>

            val finalXml = removeNamespaces(qtiXml)

            if(Play.isDev(play.api.Play.current)){
              Ok(testplayer.views.html.itemPlayer(itemId, finalXml, previewEnabled))
               .withSession("access_token" -> MOCK_ACCESS_TOKEN)
            }
            else {
              Ok(testplayer.views.html.itemPlayer(itemId, finalXml, previewEnabled))
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

  /**
   * remove the namespaces - Note: this is necessary to support correct rendering in IE8
   * TODO - should we do this with xml processing?
   * @param xml
   * @return
   */
  private def removeNamespaces(xml: Elem): String = NamespaceRegex.replaceAllIn(xml.mkString, "")

  /**
   * Provides the item XML body for an item with a provided item id.
   * @param itemId
   * @return
   */
  private def getItemXMLByObjectId(itemId: String, callerOrg: ObjectId): Option[Elem] = {
    Item.findOneById(new ObjectId(itemId)) match {
      case Some(item) => {
        if (Content.isCollectionAuthorized(callerOrg, item.collectionId, Permission.All)) {
          val dataResource = item.data.get

          dataResource.files.find(_.name == Resource.QtiXml) match {
            case Some(qtiXml) => {
              Some(scala.xml.XML.loadString(qtiXml.asInstanceOf[VirtualFile].content))
            }
            case _ => None
          }
        } else None
      }
      case _ => None
    }
  }


}
