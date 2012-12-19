package testplayer.views.utils

import xml.{Node, NodeSeq}
import common.controllers.DefaultCss
import qti.models.QtiItem
import qti.models.interactions.{InteractionCompanion, Interaction}
import controllers.Utils

/**
 * Load js and css depending on the contents of the Qti xml.
 */
object QtiScriptLoader {

  val PRINT_MODE : String = "@print-mode"

  val JS_PATH: String = "/assets/js/corespring/qti/directives/web/"
  val JS_PRINT_PATH: String = "/assets/js/corespring/qti/directives/print/"

  val CSS_PATH: String = "/assets/stylesheets/qti/directives/web/"
  val CSS_PRINT_PATH: String = "/assets/stylesheets/qti/directives/print/"


  def load( itemBody : String) : String = {
    val xml = scala.xml.XML.loadString(itemBody)
    val isPrintMode = (xml \ PRINT_MODE).text == "true"
    getScriptsToInclude(xml, isPrintMode).mkString("\n")
  }

  private def css(url: String): String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)

  private def script(url: String): String = """<script type="text/javascript" src="%s"></script>""".format(url)

  private def createScripts(name: String, toPrint: Boolean = false): String = {
    val jspath = if (toPrint) JS_PRINT_PATH else JS_PATH
    val csspath = if (toPrint) CSS_PRINT_PATH else CSS_PATH

    def jsAndCss(name:String) = Seq(script(jspath + name + ".js"), css(csspath + name + ".css")).mkString("\n")

    name.split(",").toList.map( jsAndCss ).mkString("\n")
  }

  private def getScriptsToInclude(itemBody: Node, isPrintMode: Boolean = false): List[String] = {
    var scripts = Seq[String]()

    val interactionModels:Seq[InteractionCompanion[_ <: Interaction]] = Utils.traverseElements[InteractionCompanion[_ <: Interaction]](itemBody){elem =>
      QtiItem.interactionModels.find(_.interactionMatch(elem)) match {
        case Some(im) => {
          Some(Seq(im))
        }
        case None => None
      }
    }.distinct
    scripts = interactionModels.map(im => im.getHeadHtml(isPrintMode))
    val elementScriptsMap = Map(
//      "choiceInteraction" -> createScripts("choiceInteraction,simpleChoice", isPrintMode),
//      "inlineChoiceInteraction" -> createScripts("inlineChoiceInteraction", isPrintMode),
//      "orderInteraction" -> createScripts("orderInteraction", isPrintMode),
//      "textEntryInteraction" -> createScripts("textEntryInteraction", isPrintMode),
//      "extendedTextInteraction" -> createScripts("extendedTextInteraction", isPrintMode),
//      "selectTextInteraction" -> createScripts("selectTextInteraction", isPrintMode),
      "tabs" -> createScripts("tabs", isPrintMode),
      "cs-tabs" -> createScripts("tabs", isPrintMode),
      "math" -> script("http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML")
    )
    for ((element, scriptString) <- elementScriptsMap) {
      if ((itemBody \\ element).size > 0) {
        scripts = scriptString +: scripts
      } else {

        if ((itemBody \\ ("@" + element)).size > 0) {
          scripts = scriptString +: scripts
        }
      }
    }

    scripts = createScripts("numberedLines") +: scripts
    scripts = DefaultCss.DEFAULT_CSS +: scripts

    // order matters so put them out in the chronological order we put them in
    scripts.reverse.toList
  }

}
