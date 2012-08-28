package controllers.testplayer


import org.xml.sax.SAXParseException
import qti.{QtiItem, FeedbackElement}
import xml.{Elem, NodeSeq}
import play.api.libs.json.Json
import qti.FeedbackProcessor._
import org.bson.types.ObjectId
import controllers.auth.BaseApi
import models.Item
import api.v1.ItemApi
import com.mongodb.casbah.Imports._
import scala.Some

case class ExceptionMessage(message:String, lineNumber:Int = -1, columnNumber: Int = -1)

object ItemPlayer extends BaseApi {

  /**
   * Very simple QTI Item Renderer
   * @param itemId
   * @return
   */
  def renderItem(itemId: String) = ApiAction { request =>

    try {
      getItemXMLByObjectId(itemId,request.ctx.organization) match {
        case Some(xmlData: Elem) =>
          // extract and filter the itemBody element
          val itemBody = filterFeedbackContent(xmlData \ "itemBody")
          // Logger.info(itemBody.mkString)

          // parse the itemBody and determine what scripts should be included for the defined interactions
          val scripts: List[String] = getScriptsToInclude(itemBody)

          // angular will render the itemBody client-side
          Ok(views.html.testplayer.itemPlayer(itemId, scripts, itemBody.mkString))
        case None =>
          // we found nothing
          NotFound
      }
    } catch {
      case e: SAXParseException =>
        // xml processing error - inform the user
        // TODO - only respond with xml error info if in 'preview'/editing mode
        val saxError = e // only doing this so intellij debugger will let me inspect
        val errorInfo = ExceptionMessage(e.getMessage, e.getLineNumber, e.getColumnNumber)
        Ok(views.html.testplayer.itemPlayerError(errorInfo))
      case e: Exception =>
        // db or other problem?
        //Log.e(e)
        InternalServerError
    }

  }

  def getFeedbackInline(itemId: String, responseIdentifier: String, choiceIdentifier: String) = ApiAction { request =>
    getItemXMLByObjectId(itemId, request.ctx.organization) match {
      case Some(rootElement: Elem) => {
        val item = new QtiItem(rootElement)
        val feedback: Seq[FeedbackElement] = item.feedback(responseIdentifier, choiceIdentifier)
        if (feedback.nonEmpty) {
          Ok("{\"feedback\":" + Json.toJson(feedback).toString + "}")
        } else {
          NotFound("{\"error\": \"not found\"}")
        }
      }
      case None => {
        NotFound("{\"error\": \"not found\"}")
      }
    }
  }

  /**
   * Provides the item XML body for an item with a provided item id.
   * @param itemId
   * @return
   */
  private def getItemXMLByObjectId(itemId: String, callerOrg: ObjectId): Option[Elem] = {
    val xmlDataField = MongoDBObject(Item.xmlData -> 1, Item.collectionId -> 1)

    Item.collection.findOneByID(new ObjectId(itemId), xmlDataField) match {
      case Some(o) =>
        if ( ItemApi.canUpdateOrDelete(callerOrg, o.get(Item.collectionId).asInstanceOf[String])) {
          val xmlDataString = o.get(Item.xmlData).toString
          Some(scala.xml.XML.loadString(xmlDataString))
        } else {
          None
        }
      case _ => None
    }

  }

  def getScriptsToInclude(itemBody : NodeSeq, isWebMode: Boolean = true) : List[String] = {
    var scripts = List[String]()
    // suffix to append for loading print-mode scripts
    val scriptSuffix = if (isWebMode) "" else "-print"


    val choiceInteractionScripts = "<script src=\"/assets/js/corespring/qti/choiceInteraction" +
      scriptSuffix + ".js\"></script>\n" +
      "<link rel=\"stylesheet\" type=\"text/css\" href=\"/assets/js/corespring/qti/choiceInteraction.css\" />"

    // map of elements and the scripts needed to process them
    // can't concatenate string in map value apparently, so using replace()
    val elementScriptsMap = Map (
      "choiceInteraction" -> choiceInteractionScripts,
      "textEntryInteraction" -> "<script src=\"/assets/js/corespring/qti/textEntryInteraction{S}.js\"></script>".replace("{S}", scriptSuffix),
      "extendedTextInteraction" -> "<script src=\"/assets//js/corespring/qti/extendedTextInteraction{S}.js\"></script>".replace("{S}", scriptSuffix),
      "math" -> "<script type=\"text/javascript\" src=\"http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML\"></script>"
    )

    // iterate through the script map
    for ((element, scriptString) <- elementScriptsMap) {
      if ((itemBody \\ element).size > 0) {  // always returns a NodeSeq, check if not empty
        scripts ::= scriptString
      }
    }

    scripts
  }

}
