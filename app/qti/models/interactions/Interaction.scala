package qti.models.interactions

import xml.{Elem, NodeSeq, Node}
import models.{ItemResponseOutcome, ItemResponse}
import qti.models.{QtiItem, ResponseDeclaration}
import testplayer.views.utils.QtiScriptLoader
import qti.models.RenderingMode._
import play.api.Play
import play.api.Play.current
import QtiScriptLoader.{jsPathMapping, cssPathMapping, css, script}

trait Interaction {
  val responseIdentifier: String

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse): Option[ItemResponseOutcome]

  def validate(qtiItem: QtiItem): (Boolean, String) = {
    val isValid = !(qtiItem.responseDeclarations.find(_.identifier == responseIdentifier).isEmpty)
    val msg = if (isValid) "Ok" else "Missing response declartaion for " + responseIdentifier
    (isValid, msg)
  }
}

trait InteractionCompanion[T <: Interaction] {
  def tagName: String
  def apply(interaction: Node, itemBody: Option[Node]): T
  def parse(itemBody: Node): Seq[Interaction]
  def interactionMatch(e: Elem): Boolean = e.label == tagName
  def preProcessXml(interactionXml: Elem): NodeSeq = interactionXml
  def getHeadHtml(mode:RenderingMode):String = InteractionHelper.getHeadHtml(tagName, mode)
}

object Interaction {
  def responseIdentifier(n: Node) = (n \ "@responseIdentifier").text
}

object InteractionHelper {
  def getHeadHtml(interactionName: String, mode: RenderingMode) = {
    val jspath = jsPathMapping(mode)
    val csspath = cssPathMapping(mode)

    def revertJsIfDoesntExist(name:String) = {
      Play.getExistingFile("public/" + jspath + name + ".js") match {
        case Some(file) => "/assets/" + jspath + name + ".js"
        case None => "/assets/" + jsPathMapping(Web) + name + ".js"
      }
    }
    def revertCssIfDoesntExist(name:String) = {
      Play.getExistingFile("public/" + csspath + name + ".css") match {
        case Some(file) => "/assets/" + csspath + name + ".css"
        case None => "/assets/" + cssPathMapping(Web) + name + ".css"
      }
    }
    def jsAndCss(name: String) = Seq(script(revertJsIfDoesntExist(name)), css(revertCssIfDoesntExist(name))).mkString("\n")
    jsAndCss(interactionName) + "\n"
  }
}
