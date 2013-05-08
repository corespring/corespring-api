package player.views.qti

import qti.models.RenderingMode
import qti.models.RenderingMode._
import player.views.qti.models.{QtiJsAsset, QtiAssetsConfig}

class QtiAssetsLoaderImpl(xmlString: String) extends QtiAssetsLoader {

  val config = QtiAssetsConfig(
    Seq(
      QtiJsAsset("ChoiceInteraction", localDependents = Seq("simpleChoice")),
      QtiJsAsset("math", hasJsFile = false, remoteDependents = Seq("//cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"))
    )
  )

  private val qti = scala.xml.XML.loadString(xmlString)
  private val mode: RenderingMode = RenderingMode.withName((qti \ "@mode").text)
  private val assets = new QtiAssets("js/corespring/qti/directives", "stylesheets/qti/directives", config)

  def localJsPaths: Seq[String] = assets.getLocalJsPaths(qti, mode)

  def remoteJsPaths: Seq[String] = assets.getRemoteJsPaths(qti, mode)

  def localCssPaths: Seq[String] = assets.getLocalCssPaths(qti, mode)
}
