package player.views.qti

import player.views.models.QtiKeys
import player.views.qti.models.{QtiJsAsset, QtiAssetsConfig}
import qti.models.RenderingMode._

class QtiAssetsLoaderImpl(qtiKeys:QtiKeys, mode : RenderingMode) extends QtiAssetsLoader {

  val config = QtiAssetsConfig(
    Seq(
      QtiJsAsset("choiceInteraction", localDependents = Seq("simpleChoice")),
      QtiJsAsset("math", hasJsFile = false, remoteDependents = Seq("//cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"))
    )
  )

  private val assets = new QtiAssets("js/corespring/qti/directives", "stylesheets/qti/directives", config)

  def localJsPaths: Seq[String] = assets.getLocalJsPaths(qtiKeys.keys, mode)

  def remoteJsPaths: Seq[String] = assets.getRemoteJsPaths(qtiKeys.keys, mode)

  def localCssPaths: Seq[String] = assets.getLocalCssPaths(qtiKeys.keys, mode)
}
