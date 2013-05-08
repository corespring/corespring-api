package qti.models.interactions.utils

import common.utils.string
import controllers.Utils
import play.api.{LoggerLike, Logger}
import qti.models.QtiItem
import qti.models.RenderingMode._
import qti.models.interactions.{Interaction, InteractionCompanion}
import scala.xml.Node

case class QtiAssetsConfig(assets: Seq[QtiJsAsset])

abstract class BaseQtiAssets(jsRootPath: String, cssRootPath:String, seedConfig: QtiAssetsConfig) {

  def logger: LoggerLike = Logger(this.getClass.getCanonicalName)

  def getLocalJsPaths(qti: Node, mode: RenderingMode): Seq[String] = {
    withKeysAndResolver(jsRootPath, qti, mode, new ScriptResolver(_, ".js", _), (keys, resolver) => keys.map(resolver.getLocalPaths(_)).flatten.distinct)
  }

  def getRemoteJsPaths(qti: Node, mode: RenderingMode): Seq[String] = {
    withKeysAndResolver(jsRootPath, qti, mode, new ScriptResolver(_,".js", _), (keys, resolver) => keys.map(resolver.getRemotePaths(_)).flatten.distinct)
  }

  def getLocalCssPaths(qti:Node, mode : RenderingMode) : Seq[String] = {
    withKeysAndResolver(cssRootPath, qti, mode, new ScriptResolver(_,".css", _), (keys, resolver) => keys.map(resolver.getLocalPaths(_)).flatten.distinct)
  }

  private def withKeysAndResolver(root:String,qti: Node, mode: RenderingMode, makeResolver:(String,QtiAssetsConfig)=>ScriptResolver, fn: (Seq[String], ScriptResolver) => Seq[String]): Seq[String] = {
    val rootPath = string.filePath(root, mode.toString.toLowerCase)
    logger.debug("rootPath: " + rootPath)
    val keys: Seq[String] = getKeysThatNeedAssets(qti)
    val generatedConfig = addDefaultsForOtherKeys(keys)
    val resolver: ScriptResolver = makeResolver(rootPath, generatedConfig)
    fn(keys, resolver)
  }

  private def addDefaultsForOtherKeys(keys: Seq[String]): QtiAssetsConfig = {
    val keysNotInSeedConfig = keys.filterNot(k => seedConfig.assets.exists(_.name == k))
    val assets = keysNotInSeedConfig.map(QtiJsAsset(_))
    seedConfig.copy(assets = assets ++ seedConfig.assets)
  }

  protected def getKeysThatNeedAssets(qti: Node): Seq[String]
}

class QtiAssets(jsRootPath: String, cssRootPath:String, config: QtiAssetsConfig) extends BaseQtiAssets(jsRootPath, cssRootPath, config) {

  protected def getKeysThatNeedAssets(qti: Node): Seq[String] = {

    def interactionKeys = Utils.traverseElements[InteractionCompanion[_ <: Interaction]](qti) {
      elem =>
        QtiItem.interactionModels.find(_.interactionMatch(elem)) match {
          case Some(im) => Some(Seq(im))
          case None => None
        }
    }.distinct.map(_.getClass.getSimpleName.replace("$", ""))

    def otherKeys = {
      def xmlContainsNodeOrAttribute(key: String): Boolean = (qti \\ key).size > 0 || (qti \\ ("@" + key)).size > 0
      def containsAttributeWithValue(name: String, value: String): Boolean =  (qti \\ ("@" + name)).find(_.text == value).isDefined

      val nodes = Seq(
        xmlContainsNodeOrAttribute("tabs") -> "tabs",
        xmlContainsNodeOrAttribute("cs-tabs") -> "tabs",
        xmlContainsNodeOrAttribute("math") -> "math",
        containsAttributeWithValue("class", "numbered-lines") -> "numberedLines"
      )
      nodes.filter(_._1).map(_._2).distinct
    }

    interactionKeys ++ otherKeys
  }

}
