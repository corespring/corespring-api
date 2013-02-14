package testplayer.views.utils

import xml.{Node, NodeSeq}
import common.controllers.DefaultCss
import qti.models.{RenderingMode, QtiItem}
import qti.models.RenderingMode._
import qti.models.interactions.{InteractionCompanion, Interaction}
import controllers.Utils

/**
 * Load js and css depending on the contents of the Qti xml.
 */
object QtiScriptLoader {

  val RENDER_MODE : String = "@mode"

  val jsPathMapping:Map[RenderingMode, String] = Map(
    Printing -> "js/corespring/qti/directives/print/",
    Web -> "js/corespring/qti/directives/web/",
    Instructor -> "js/corespring/qti/directives/instructor/"
  )

  val cssPathMapping:Map[RenderingMode, String] = Map(
    Printing -> "stylesheets/qti/directives/print/",
    Web -> "stylesheets/qti/directives/web/",
    Instructor -> "stylesheets/qti/directives/instructor/"
  )


  def load( itemBody : String) : String = {
    val xml = scala.xml.XML.loadString(itemBody)
    val mode:RenderingMode = RenderingMode.withName((xml \ RENDER_MODE).text)
    getScriptsToInclude(xml, mode).mkString("\n")
  }

  def css(url: String): String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)

  def script(url: String): String = """<script type="text/javascript" src="%s"></script>""".format(url)

  private def createScripts(name: String, renderMode: RenderingMode = Web): String = {
    val jspath = "/assets/" + jsPathMapping(renderMode)
    val csspath = "/assets/" + cssPathMapping(renderMode)

    def jsAndCss(name:String) = Seq(script(jspath + name + ".js"), css(csspath + name + ".css")).mkString("\n")

    name.split(",").toList.map( jsAndCss ).mkString("\n")
  }

  private def getScriptsToInclude(itemBody: Node, renderMode: RenderingMode = Web): List[String] = {
    var scripts = Seq[String]()

    val interactionModels:Seq[InteractionCompanion[_ <: Interaction]] = Utils.traverseElements[InteractionCompanion[_ <: Interaction]](itemBody){elem =>
      QtiItem.interactionModels.find(_.interactionMatch(elem)) match {
        case Some(im) => {
          Some(Seq(im))
        }
        case None => None
      }
    }.distinct
    scripts = interactionModels.map(im => im.getHeadHtml(renderMode))
    val elementScriptsMap = Map(
      "tabs" -> createScripts("tabs", renderMode),
      "cs-tabs" -> createScripts("tabs", renderMode),
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
