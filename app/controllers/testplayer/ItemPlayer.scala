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
import models.bleezmo.{FeedbackInline, ChoiceInteraction, OrderInteraction}

case class ExceptionMessage(message:String, lineNumber:Int = -1, columnNumber: Int = -1)

object ItemPlayer extends BaseApi with ItemResources{


  val JS_PATH : String = "/assets/js/corespring/qti/directives/web/"
  val JS_PRINT_PATH : String = "/assets/js/corespring/qti/directives/print/"

  val CSS_PATH : String = "/assets/stylesheets/qti/directives/web/"
  val CSS_PRINT_PATH : String = "/assets/stylesheets/qti/directives/print/"

  def css( url : String ) : String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)
  def script( url : String ) : String =  """<script type="text/javascript" src="%s"></script>""".format(url)

  def createScripts( name : String, toPrint : Boolean = false) : String = {
    val jspath = if (toPrint) JS_PRINT_PATH else JS_PATH
    val csspath = if (toPrint) CSS_PRINT_PATH else CSS_PATH
    Seq( script( jspath + name + ".js"), css( csspath + name  + ".css") ).mkString("\n") }

  val BYTE_BUREAU = css("/assets/stylesheets/bytebureau/styles.css")

  val DEFAULT_CSS = Seq(BYTE_BUREAU).mkString("\n")

  val notFoundJson = Json.toJson(
    Map("error" -> "not found")
  )


  def previewItem(itemId:String, printMode:Boolean = false) =
    _renderItem(itemId, printMode, previewEnabled = !printMode )

  /**
   * Very simple QTI Item Renderer
   * @param itemId
   * @return
   */
  def renderItem(itemId: String, printMode: Boolean = false) =
    _renderItem(itemId, printMode, previewEnabled  = false)


  private def createItemSession(itemId:String, mapping:Map[String,String]) : String = {
    val session : ItemSession = ItemSession( itemId = new ObjectId(itemId),feedbackIdLookup = mapping )
    ItemSession.save(session, ItemSession.collection.writeConcern)
    session.id.toString
  }

  private def _renderItem(itemId:String, printMode : Boolean = false, previewEnabled : Boolean = false)  = ApiAction { request =>
    try {
      getItemXMLByObjectId(itemId,request.ctx.organization) match {
        case Some(xmlData: Elem) =>

          val (xmlWithCsFeedbackIds,mapping) = FeedbackProcessor.addFeedbackIds(xmlData)

          val itemBody = filterFeedbackContent(addOutcomeIdentifiers(xmlWithCsFeedbackIds) \ "itemBody")

          val scripts: List[String] = getScriptsToInclude(itemBody, printMode)

          val itemSessionId = if (printMode) "" else createItemSession(itemId, mapping)

          val qtiXml = <assessmentItem
                           print-mode={ if(printMode) "true" else "false" }
                           cs:itemId={itemId}
                           cs:itemSessionId={itemSessionId}
                           cs:feedbackEnabled="true"
                           cs:noResponseAllowed="true">{itemBody}</assessmentItem>

          val finalXml = removeNamespaces(qtiXml)

          Ok(views.html.testplayer.itemPlayer(itemId, scripts, finalXml, previewEnabled))
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
        val choiceIdentifiers:Seq[String] = if (choiceIdentifier.contains(",")) choiceIdentifier.split(",") else Seq(choiceIdentifier)
        val item = bleezmo.QtiItem(rootElement)
        val feedback: Seq[FeedbackInline] = item.itemBody.interactions.find(_.responseIdentifier == responseIdentifier) match {
          case Some(interaction) =>
            interaction match {
            case ChoiceInteraction(_,choices) => choices.filter(sc => choiceIdentifiers.contains(sc.identifier)).flatMap(_.feedbackInline)
            case OrderInteraction(_,choices) => choices.filter(sc => choiceIdentifiers.contains(sc.identifier)).flatMap(_.feedbackInline)
            case _ => Seq()
          }
          case None => Seq()
        }
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



    // base css to include for all QTI items
    //scripts ::= "<link rel=\"stylesheet\" type=\"text/css\" href=\"/assets/js/corespring/qti/qti-base.css\" />"
    // TODO - dropping jquery in for all right now, but this needs to be only dropped in if required by interactions
    scripts ::= script("//ajax.googleapis.com/ajax/libs/jquery/1.8.1/jquery.min.js")
    scripts ::= script("//ajax.googleapis.com/ajax/libs/jqueryui/1.8.23/jquery-ui.min.js")


    // map of elements and the scripts needed to process them
    // can't concatenate string in map value apparently, so using replace()
    val elementScriptsMap = Map (
      "choiceInteraction" -> createScripts("choiceInteraction", isPrintMode),
      "orderInteraction" -> createScripts("orderInteraction", isPrintMode),
      "textEntryInteraction" -> createScripts("textEntryInteraction", isPrintMode),
      "extendedTextInteraction" -> createScripts("extendedTextInteraction", isPrintMode),
      "tabs" -> createScripts("tabs", isPrintMode),
      "cs-tabs" -> createScripts("tabs", isPrintMode),
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
