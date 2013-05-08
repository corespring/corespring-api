package qti.models.interactions.utils

import common.utils.string
import java.io.File
import qti.models.RenderingMode
import qti.models.RenderingMode._

trait QtiAssetsLoader {
  def localJsPaths: Seq[String]

  def remoteJsPaths: Seq[String]

  def localCssPaths: Seq[String]
}

object FileExistChecker {
  def exists(path: String): Boolean = new File(string.filePath("public", path)).exists()
}

class ScriptLoader(xmlString: String) extends QtiAssetsLoader {

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
