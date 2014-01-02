package org.corespring.player.v1.views.qti

import org.specs2.mutable.Specification
import org.corespring.player.v1.views.qti.models.{QtiJsAsset, QtiAssetsConfig}
import org.corespring.qti.models.RenderingMode


class QtiAssetsTest extends Specification {


  val keys = Seq("choiceInteraction", "textEntryInteraction")
  val config = QtiAssetsConfig(Seq())

  "qti assets" should {

    "return the correct paths " in {
      val qtiAssets = new QtiAssets("js", "css", config)
      qtiAssets.getLocalJsPaths(keys, RenderingMode.Aggregate).sorted === List("js/aggregate/choiceInteraction.js", "js/aggregate/textEntryInteraction.js")
      qtiAssets.getLocalCssPaths(keys, RenderingMode.Aggregate).sorted === List("css/aggregate/choiceInteraction.css", "css/aggregate/textEntryInteraction.css")
    }

    "adds local dependents" in {
      val qtiAssets = new QtiAssets("js", "css",
        QtiAssetsConfig(
          Seq(
            QtiJsAsset(
              name = "choiceInteraction",
              localDependents = Seq("other")
            )
          )
        )
      )
      qtiAssets.getLocalJsPaths(keys, RenderingMode.Web).sorted === List("js/web/choiceInteraction.js", "js/web/other.js", "js/web/textEntryInteraction.js")
      qtiAssets.getLocalCssPaths(keys, RenderingMode.Web).sorted === List("css/web/choiceInteraction.css", "css/web/other.css", "css/web/textEntryInteraction.css")
    }

    "finds numbered lines" in {
      val keys = Seq("numberedLines")
      val qtiAssets = new QtiAssets("js", "css", QtiAssetsConfig(Seq(QtiJsAsset("numberedLines"))))
      qtiAssets.getLocalJsPaths(keys, RenderingMode.Web) === List("js/web/numberedLines.js")
      qtiAssets.getLocalCssPaths(keys, RenderingMode.Web) === List("css/web/numberedLines.css")
    }

    "find cs-tabs" in {
      val keys = Seq("tabs")
      val qtiAssets = new QtiAssets("js", "css", QtiAssetsConfig(Seq(QtiJsAsset("tabs"))))
      qtiAssets.getLocalJsPaths(keys, RenderingMode.Web) === List("js/web/tabs.js")
      qtiAssets.getLocalCssPaths(keys, RenderingMode.Web) === List("css/web/tabs.css")
    }

    "find tabs and math" in {
      val keys = Seq("math")
      val qtiAssets = new QtiAssets("js", "css",
        QtiAssetsConfig(
          Seq(
            QtiJsAsset("math", hasJsFile = false, remoteDependents = Seq("remote-math.js")))
        )
      )
      qtiAssets.getLocalJsPaths(keys, RenderingMode.Web) === List()
      qtiAssets.getRemoteJsPaths(keys, RenderingMode.Web) === List("remote-math.js")
    }

  }

}
