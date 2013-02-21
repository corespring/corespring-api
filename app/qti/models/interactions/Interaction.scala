package qti.models.interactions

import xml.{Elem, NodeSeq, Node}
import qti.models.{QtiItem, ResponseDeclaration}
import testplayer.views.utils.QtiScriptLoader
import models.itemSession.{ItemResponseOutcome, ItemResponse}

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
  def getHeadHtml(toPrint:Boolean):String = InteractionHelper.getHeadHtml(tagName, toPrint)
}

object Interaction {
  def responseIdentifier(n: Node) = (n \ "@responseIdentifier").text
}

object InteractionHelper {
  def getHeadHtml(interactionName: String, toPrint: Boolean) = {
    def css(url: String): String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)
    def script(url: String): String = """<script type="text/javascript" src="%s"></script>""".format(url)
    val jspath = if (toPrint) QtiScriptLoader.JS_PRINT_PATH else QtiScriptLoader.JS_PATH
    val csspath = if (toPrint) QtiScriptLoader.CSS_PRINT_PATH else QtiScriptLoader.CSS_PATH
    def jsAndCss(name: String) = Seq(script(jspath + name + ".js"), css(csspath + name + ".css")).mkString("\n")
    jsAndCss(interactionName) + "\n"
  }
}
