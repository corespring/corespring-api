package testplayer.views.utils

import xml.NodeSeq
import common.controllers.DefaultCss

/**
 * Load js and css depending on the contents of the Qti xml.
 */
object QtiScriptLoader {

  val JS_PATH: String = "/assets/js/corespring/qti/directives/web/"
  val JS_PRINT_PATH: String = "/assets/js/corespring/qti/directives/print/"

  val CSS_PATH: String = "/assets/stylesheets/qti/directives/web/"
  val CSS_PRINT_PATH: String = "/assets/stylesheets/qti/directives/print/"


  def load( itemBody : String) : String = {
    val xml = scala.xml.XML.loadString(itemBody)
    val isPrintMode = (xml \ "@printMode").text == "true"
    getScriptsToInclude(scala.xml.XML.loadString(itemBody), isPrintMode).mkString("\n")
  }

  private def css(url: String): String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)

  private def script(url: String): String = """<script type="text/javascript" src="%s"></script>""".format(url)

  private def createScripts(name: String, toPrint: Boolean = false): String = {
    val jspath = if (toPrint) JS_PRINT_PATH else JS_PATH
    val csspath = if (toPrint) CSS_PRINT_PATH else CSS_PATH
    Seq(script(jspath + name + ".js"), css(csspath + name + ".css")).mkString("\n")
  }

  private def getScriptsToInclude(itemBody: NodeSeq, isPrintMode: Boolean = false): List[String] = {
    var scripts = List[String]()


    val elementScriptsMap = Map(
      "choiceInteraction" -> createScripts("choiceInteraction", isPrintMode),
      "inlineChoiceInteraction" -> createScripts("inlineChoiceInteraction", isPrintMode),
      "orderInteraction" -> createScripts("orderInteraction", isPrintMode),
      "textEntryInteraction" -> createScripts("textEntryInteraction", isPrintMode),
      "extendedTextInteraction" -> createScripts("extendedTextInteraction", isPrintMode),
      "tabs" -> createScripts("tabs", isPrintMode),
      "cs-tabs" -> createScripts("tabs", isPrintMode),
      "math" -> script("http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML")
    )

    for ((element, scriptString) <- elementScriptsMap) {
      if ((itemBody \\ element).size > 0) {
        scripts ::= scriptString
      } else {

        if ((itemBody \\ ("@" + element)).size > 0) {
          scripts ::= scriptString
        }
      }
    }

    scripts ::= createScripts("numberedLines")
    scripts ::= DefaultCss.DEFAULT_CSS

    // order matters so put them out in the chronological order we put them in
    scripts.reverse
  }

}
