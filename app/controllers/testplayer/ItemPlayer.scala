package controllers.testplayer

import org.xml.sax.SAXParseException
import qti.{QtiItem, FeedbackElement}
import xml.{Elem, NodeSeq}
import play.api.libs.json.Json
import org.bson.types.ObjectId
import controllers.auth.{Permission, BaseApi}
import models.{VirtualFile, Content, Item}
import com.mongodb.casbah.Imports._
import api.processors.FeedbackProcessor._
import play.api.Logger


case class ExceptionMessage(message:String, lineNumber:Int = -1, columnNumber: Int = -1)

object ItemPlayer extends BaseApi {

  val notFoundJson = Json.toJson(
    Map("error" -> "not found")
  )

  /**
   * Very simple QTI Item Renderer
   * @param itemId
   * @return
   */
  def renderItem(itemId: String, printMode: Boolean) = ApiAction { request =>
    try {
      getItemXMLByObjectId(itemId,request.ctx.organization) match {
        case Some(xmlData: Elem) =>
          // extract and filter the itemBody element
          val itemBody = filterFeedbackContent(addOutcomeIdentifiers(xmlData \ "itemBody"))
          // Logger.info(itemBody.mkString)

          // parse the itemBody and determine what scripts should be included for the defined interactions
          val scripts: List[String] = getScriptsToInclude(itemBody, printMode)

          val qtiXml = <assessmentItem cs:itemId={itemId} cs:feedbackEnabled="true">{itemBody}</assessmentItem>

          val finalXml = removeNamespaces(qtiXml)

          // angular will render the itemBody client-side
          Ok(views.html.testplayer.itemPlayer(itemId, scripts, finalXml))
        case None =>
          // we found nothing
          NotFound(notFoundJson)
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

  val NamespaceRegex = """xmlns.*?=".*?"""".r

  /**
   *
   * remove the namespaces - Note: this is necessary to support correct rendering in IE8
   * TODO - should we do this with xml processing?
   * @param xml
   * @return
   */
  private def removeNamespaces(xml: Elem): String =  NamespaceRegex.replaceAllIn(xml.mkString, "")

  def getFeedbackInline(itemId: String, responseIdentifier: String, choiceIdentifier: String) = ApiAction { request =>
    getItemXMLByObjectId(itemId, request.ctx.organization) match {
      case Some(rootElement: Elem) => {
        val item = new QtiItem(rootElement)
        val feedback: Seq[FeedbackElement] = item.feedback(responseIdentifier, choiceIdentifier)
        if (feedback.nonEmpty) {
          Ok(Json.toJson(Map("feedback" -> Json.toJson(feedback))))
        } else {
          NotFound(notFoundJson)
        }
      }
      case None => {
        NotFound(notFoundJson)
      }
    }
  }

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
          dataResource.files.find( _.name == "qti.xml") match {
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

  def getScriptsToInclude(itemBody : NodeSeq, isPrintMode: Boolean = false) : List[String] = {
    var scripts = List[String]()

    // TODO - maybe move all of this configuration to an external .json/xml file in conf/ ?
    // e.g. <match element="orderInteraction" priority="500" depends="jquery,jquery-ui">
    //        <web>
    //            <stylesheet src="/assets/js/corespring/qti/orderInteraction.css"/>
    //            <script src="/assets/js/corespring/qti/orderInteraction.js/>
    //        </web>
    //        <print>
    //          ...
    //        <print>
    // (also need to support minifying/obfuscating)

    // base css to include for all QTI items
    scripts ::= "<link rel=\"stylesheet\" type=\"text/css\" href=\"/assets/js/corespring/qti/qti-base.css\" />"


    // suffix to append for loading print-mode scripts
    val scriptSuffix = if (isPrintMode) "-print" else ""


    // TODO - dropping jquery in for all right now, but this needs to be only dropped in if required by interactions
    scripts ::= "<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.8.1/jquery.min.js\"></script>"
    scripts ::= "<script src=\"http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.23/jquery-ui.min.js\"></script>"

    val orderInteractionScripts =  "<script src=\"/assets/js/corespring/qti/orderInteraction" + scriptSuffix + ".js\"></script>\n" +
      "<link rel=\"stylesheet\" type=\"text/css\" href=\"/assets/js/corespring/qti/orderInteraction" + scriptSuffix + ".css\" />"

    val choiceInteractionScripts = "<script src=\"/assets/js/corespring/qti/choiceInteraction" +
      scriptSuffix + ".js\"></script>\n" +
      "<link rel=\"stylesheet\" type=\"text/css\" href=\"/assets/js/corespring/qti/choiceInteraction" + scriptSuffix + ".css\" />"

    val textEntryInteractionScripts = "<script src=\"/assets/js/corespring/qti/textEntryInteraction" +
      scriptSuffix + ".js\"></script>\n" +
      "<link rel=\"stylesheet\" type=\"text/css\" href=\"/assets/js/corespring/qti/textEntryInteraction" + scriptSuffix + ".css\" />"

    val extendedTextInteractionScripts = "<script src=\"/assets/js/corespring/qti/extendedTextInteraction" +
      scriptSuffix + ".js\"></script>\n" +
      "<link rel=\"stylesheet\" type=\"text/css\" href=\"/assets/js/corespring/qti/extendedTextInteraction" + scriptSuffix + ".css\" />"

    val tabScripts = "<script src=\"/assets/js/corespring/qti/tabs" +
      scriptSuffix + ".js\"></script>\n" +
      "<link rel=\"stylesheet\" type=\"text/css\" href=\"/assets/js/corespring/qti/tabs" + scriptSuffix + ".css\" />"

    // map of elements and the scripts needed to process them
    // can't concatenate string in map value apparently, so using replace()
    val elementScriptsMap = Map (
      "choiceInteraction" -> choiceInteractionScripts,
      "orderInteraction" -> orderInteractionScripts,
      "textEntryInteraction" -> textEntryInteractionScripts,
      "extendedTextInteraction" -> extendedTextInteractionScripts,
      "tabs" -> tabScripts,
      "math" -> "<script type=\"text/javascript\" src=\"http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML\"></script>"
    )

    // iterate through the script map
    for ((element, scriptString) <- elementScriptsMap) {
      if ((itemBody \\ element).size > 0) {  // always returns a NodeSeq, check if not empty
        scripts ::= scriptString
      }
    }
    // order matters so put them out in the chronological order we put them in
    scripts.reverse
  }

}
