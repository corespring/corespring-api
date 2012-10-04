package controllers.testplayer

import org.xml.sax.SAXParseException
import qti.{QtiItem, FeedbackElement}
import xml.{Elem, NodeSeq}
import play.api.libs.json.Json
import org.bson.types.ObjectId
import controllers.auth.{Permission, BaseApi}
import models._
import com.mongodb.casbah.Imports._
import api.processors.FeedbackProcessor._
import play.api.{Play, Logger}
import controllers.Log
import play.api.mvc.Action
import common.controllers.ItemResources
import api.processors.FeedbackProcessor
import play.api.cache.Cache
import play.api.Play.current
import scala.Some

case class ExceptionMessage(message:String, lineNumber:Int = -1, columnNumber: Int = -1)

object ItemPlayer extends BaseApi with ItemResources{


  val PATH : String = "/assets/js/corespring/qti/"

  def css( url : String ) : String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)
  def script( url : String ) : String =  """<script type="text/javascript" src="%s"></script>""".format(url)

  def createScripts( name : String, scriptSuffix : String = "" ) : String = {
    Seq( script( PATH + name + scriptSuffix + ".js"), css( PATH + name + scriptSuffix + ".css") ).mkString("\n") }

  val BYTE_BUREAU = css("/assets/stylesheets/bytebureau/styles.css")

  val DEFAULT_CSS = Seq( BYTE_BUREAU).mkString("\n")

  val notFoundJson = Json.toJson(
    Map("error" -> "not found")
  )

  def xmlCacheKey(itemId:String, sessionId: String) = """qti_itemId[%s]_sessionId[%s]""".format(itemId, sessionId)

  /**
   * Very simple QTI Item Renderer
   * @param itemId
   * @return
   */
  def renderItem(itemId: String, printMode: Boolean = false) = ApiAction { request =>
    try {
      getItemXMLByObjectId(itemId,request.ctx.organization) match {
        case Some(xmlData: Elem) =>

          /**
           * Temporary fix - get the ItemPlayer to serve xml with csFeedback ids.
           * Then make this xml available to ItemSessionApi via the cache
           */
          val xmlWithCsFeedbackIds = ItemSessionXmlStore.addCsFeedbackIds(xmlData)
          val itemBody = filterFeedbackContent(addOutcomeIdentifiers(xmlWithCsFeedbackIds \ "itemBody"))

          val scripts: List[String] = getScriptsToInclude(itemBody, printMode)

          val session : ItemSession = ItemSession( itemId = new ObjectId(itemId) )
          ItemSession.save(session, ItemSession.collection.writeConcern)

          //Stash it the cache for the Feedback rendering
          ItemSessionXmlStore.cacheXml(xmlWithCsFeedbackIds, itemId, session.id.toString)

          val qtiXml = <assessmentItem print-mode={ if(printMode) "true" else "false" } cs:itemId={itemId} cs:itemSessionId={session.id.toString} cs:feedbackEnabled="true">{itemBody}</assessmentItem>

          val finalXml = removeNamespaces(qtiXml)

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
        Log.e(e.getMessage)
        e.printStackTrace()
        InternalServerError("ItemPlayer.renderItem: " + itemId + " printMode: " + printMode)
    }

  }

  val NamespaceRegex = """xmlns.*?=".*?"""".r

  /**
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
        if( Content.isCollectionAuthorized(callerOrg,item.collectionId,Permission.All)){
         val dataResource = item.data.get

          dataResource.files.find( _.name == Resource.QtiXml) match {
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


    // suffix to append for loading print-mode scripts
    val scriptSuffix = if (isPrintMode) "-print" else ""


    // base css to include for all QTI items
    //scripts ::= "<link rel=\"stylesheet\" type=\"text/css\" href=\"/assets/js/corespring/qti/qti-base.css\" />"
    // TODO - dropping jquery in for all right now, but this needs to be only dropped in if required by interactions
    scripts ::= script("//ajax.googleapis.com/ajax/libs/jquery/1.8.1/jquery.min.js")
    scripts ::= script("//ajax.googleapis.com/ajax/libs/jqueryui/1.8.23/jquery-ui.min.js")


    // map of elements and the scripts needed to process them
    // can't concatenate string in map value apparently, so using replace()
    val elementScriptsMap = Map (
      "choiceInteraction" -> createScripts("choiceInteraction", scriptSuffix),
      "orderInteraction" -> createScripts("orderInteraction", scriptSuffix),
      "textEntryInteraction" -> createScripts("textEntryInteraction", scriptSuffix),
      "extendedTextInteraction" -> createScripts("extendedTextInteraction", scriptSuffix),
      "tabs" -> createScripts("tabs", scriptSuffix),
      "cs-tabs" -> createScripts("tabs", scriptSuffix),
      "math" -> script("http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML")
    )

    // iterate through the script map
    for ((element, scriptString) <- elementScriptsMap) {
      if ((itemBody \\ element).size > 0) {  // always returns a NodeSeq, check if not empty
        scripts ::= scriptString
      } else {

        //Also check for Attributes (we need to use attriibutes to support ie8
        if( (itemBody \\ ("@" + element)).size > 0 ){
         scripts ::= scriptString
        }
      }
    }

    scripts ::= createScripts("numberedLines")
    scripts ::= DEFAULT_CSS

    // order matters so put them out in the chronological order we put them in
    scripts.reverse
  }

}
