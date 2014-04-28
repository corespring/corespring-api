package org.corespring.player.v1.views.qti

import org.corespring.player.v1.views.models.QtiKeys
import org.corespring.qti.models.RenderingMode.RenderingMode
import org.corespring.player.v1.views.qti.models.{QtiJsAsset, QtiAssetsConfig}


class QtiAssetsLoaderImpl(qtiKeys: QtiKeys, mode: RenderingMode) extends QtiAssetsLoader {

  val config = QtiAssetsConfig(
    Seq(
      QtiJsAsset("choiceInteraction", localDependents = Seq("simpleChoice")),
      QtiJsAsset("math", hasJsFile = false, remoteDependents = Seq("//cdn.mathjax.org/mathjax/2.2-latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML")),
      QtiJsAsset("tex", localDependents = Seq("tex"), remoteDependents = Seq("//cdn.mathjax.org/mathjax/2.2-latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML")),
      QtiJsAsset("lineInteraction", localDependents = Seq("jsxgraph"), remoteDependents = Seq("/assets/js/vendor/jsxgraph/jsxgraphcore.min.js")),
      QtiJsAsset("pointInteraction", localDependents = Seq("jsxgraph"), remoteDependents = Seq("/assets/js/vendor/jsxgraph/jsxgraphcore.min.js"))
    )
  )

  private val assets = new QtiAssets("js/corespring/qti/directives", "stylesheets/qti/directives", config)

  def localJsPaths: Seq[String] = assets.getLocalJsPaths(qtiKeys.keys, mode)

  def remoteJsPaths: Seq[String] = assets.getRemoteJsPaths(qtiKeys.keys, mode)

  def localCssPaths: Seq[String] = assets.getLocalCssPaths(qtiKeys.keys, mode)
}
