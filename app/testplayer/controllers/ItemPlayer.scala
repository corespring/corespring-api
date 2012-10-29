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
import models.FeedbackIdMapEntry
import scala.Some
import models.Content


object ItemPlayer extends BaseApi with ItemResources {

  val NamespaceRegex = """xmlns.*?=".*?"""".r

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
      case _ =>  Action( NotFound("sessionId: " + sessionId) )
    }
  }

  def previewItem(itemId: String, printMode: Boolean = false, sessionSettings : String = "") =
    _renderItem(itemId, printMode, previewEnabled = false, sessionSettings = sessionSettings )

  def renderItem(itemId: String, printMode: Boolean = false, sessionSettings: String = "") =
    _renderItem(itemId, printMode, previewEnabled = false, sessionSettings = sessionSettings)


  private def createItemSession(itemId: String, mapping: Seq[FeedbackIdMapEntry], sessionSettings : String ): String = {
    val settings = parseSettings(sessionSettings)
    val session: ItemSession = ItemSession(
      itemId = new ObjectId(itemId),
      feedbackIdLookup = mapping,
      settings = if (settings.isDefined) settings else None )
    ItemSession.save(session, ItemSession.collection.writeConcern)
    session.id.toString
  }


  private def _renderItem(itemId: String, printMode: Boolean = false, previewEnabled: Boolean = false, sessionSettings: String = "") = ApiAction {
    request =>
      try {
        getItemXMLByObjectId(itemId, request.ctx.organization) match {
          case Some(xmlData: Elem) =>

            val (xmlWithCsFeedbackIds, mapping) = FeedbackProcessor.addFeedbackIds(xmlData)

            val itemBody = filterFeedbackContent(addOutcomeIdentifiers(xmlWithCsFeedbackIds) \ "itemBody")

            val itemSessionId = if (printMode) "" else createItemSession(itemId, mapping, sessionSettings)

            val qtiXml = <assessmentItem
            print-mode={if (printMode) "true" else "false"}
            cs:itemId={itemId}
            cs:itemSessionId={itemSessionId}
            cs:feedbackEnabled="true"
            cs:noResponseAllowed="true">
              {itemBody}
            </assessmentItem>

            val finalXml = removeNamespaces(qtiXml)

            Ok(testplayer.views.html.itemPlayer(itemId, finalXml, previewEnabled))
          case None =>
            NotFound("not found")
        }
      } catch {
        case e: SAXParseException => {
          // xml processing error - inform the user
          val errorInfo = ExceptionMessage(e.getMessage, e.getLineNumber, e.getColumnNumber)
          Ok(testplayer.views.html.itemPlayerError(errorInfo))
        }
        case e: Exception => throw new RuntimeException("ItemPlayer.renderItem: " + e.getMessage, e)
      }

  }


  private def parseSettings(s: String): Option[ItemSessionSettings] = {
    if (s.isEmpty)
      None
    else {
      try {
        val settings = Json.parse(s).as[ItemSessionSettings]
        Some(settings)
      }
      catch {
        case e: Exception => None
      }
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
