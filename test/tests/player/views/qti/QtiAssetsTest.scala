package tests.player.views.qti

import org.specs2.mutable.Specification
import play.api.LoggerLike
import player.views.qti.QtiAssets
import player.views.qti.models.{QtiJsAsset, QtiAssetsConfig}
import qti.models.RenderingMode

class QtiAssetsTest extends Specification {


  private val logger: LoggerLike = {
    play.api.Logger(this.getClass.getCanonicalName)
  }


  val xml = <root>
    <choiceInteraction></choiceInteraction>
    <textEntryInteraction></textEntryInteraction>
  </root>
  val config = QtiAssetsConfig(Seq())

  "qti assets" should {

    "return the correct paths " in {
      val qtiAssets = new QtiAssets("js", "css", config)
      qtiAssets.getLocalJsPaths(xml, RenderingMode.Aggregate) === List("js/aggregate/choiceInteraction.js", "js/aggregate/textEntryInteraction.js")
      qtiAssets.getLocalCssPaths(xml, RenderingMode.Aggregate) === List("css/aggregate/choiceInteraction.css", "css/aggregate/textEntryInteraction.css")
    }

    "adds local dependents" in {
      val qtiAssets = new QtiAssets("js", "css",
        QtiAssetsConfig(
          Seq(
            QtiJsAsset(
              name = "ChoiceInteraction",
              localDependents = Seq("other")
            )
          )
        )
      )
      qtiAssets.getLocalJsPaths(xml, RenderingMode.Web) === List("js/web/choiceInteraction.js", "js/web/other.js", "js/web/textEntryInteraction.js")
      qtiAssets.getLocalCssPaths(xml, RenderingMode.Web) === List("css/web/choiceInteraction.css", "css/web/other.css", "css/web/textEntryInteraction.css")
    }

    "finds numbered lines" in {
      val xml = <root>
        <node class="numbered-lines"></node>
      </root>
      val qtiAssets = new QtiAssets("js", "css", QtiAssetsConfig(Seq(QtiJsAsset("numberedLines"))))
      qtiAssets.getLocalJsPaths(xml, RenderingMode.Web) === List("js/web/numberedLines.js")
      qtiAssets.getLocalCssPaths(xml, RenderingMode.Web) === List("css/web/numberedLines.css")
    }

    "find cs-tabs" in {
      val xml = <root>
        <node cs-tabs="true"></node>
      </root>
      val qtiAssets = new QtiAssets("js", "css", QtiAssetsConfig(Seq(QtiJsAsset("tabs"))))
      qtiAssets.getLocalJsPaths(xml, RenderingMode.Web) === List("js/web/tabs.js")
      qtiAssets.getLocalCssPaths(xml, RenderingMode.Web) === List("css/web/tabs.css")
    }

    "find tabs and math" in {
      val xml = <root>
        <math></math>
      </root>
      val qtiAssets = new QtiAssets("js", "css", QtiAssetsConfig(Seq(QtiJsAsset("math", hasJsFile = false))))
      val out = qtiAssets.getLocalJsPaths(xml, RenderingMode.Web)
      logger.debug("out: " + out)
      out === List()
    }

  }

}
