package player.views.qti

import common.utils.string
import play.api.{LoggerLike, Logger}
import player.views.qti.models.{QtiJsAsset, QtiAssetsConfig}
import qti.models.QtiItem
import qti.models.RenderingMode._
import scala.xml.Node


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

  /** Build a new Config that adds configs for any key that isn't in the seed config.
    * Use the default settings for these keys - aka use the key name as the js file name.
    * @param assetKeys - the keys found in the Qti xml that need assets delivered
    * @return the new config
    */
  def buildConfig(assetKeys:Seq[String], seedConfig:QtiAssetsConfig) : QtiAssetsConfig ={
    val keysNotInSeedConfig = assetKeys.filterNot(k => seedConfig.assets.exists(_.name == k))
    val defaultAssetDefinitions = keysNotInSeedConfig.map(QtiJsAsset(_))
    seedConfig.copy(assets = defaultAssetDefinitions ++ seedConfig.assets)
  }

  private def withKeysAndResolver(root:String,qti: Node, mode: RenderingMode, makeResolver:(String,QtiAssetsConfig)=>ScriptResolver, fn: (Seq[String], ScriptResolver) => Seq[String]): Seq[String] = {
    val rootPath = string.filePath(root, mode.toString.toLowerCase)
    logger.debug("rootPath: " + rootPath)
    val keys: Seq[String] = findAssetKeys(qti)
    val generatedConfig = buildConfig(keys,seedConfig)
    val resolver: ScriptResolver = makeResolver(rootPath, generatedConfig)
    fn(keys, resolver)
  }


  /** For the given Qti - find asset keys - which means that this key requires an asset.
   * @param qti - the qti to use to find asset keys
   * @return - a Seq of found keys
   */
  protected def findAssetKeys(qti: Node): Seq[String]
}

/** This implementation does a look up for keys in 2 ways:
 * 1. It looks for interaction names
 * 2. It looks for a set of named items that are not interactions but need assets: eg: tabs
 * @param jsRootPath
 * @param cssRootPath
 * @param config
 */
class QtiAssets(jsRootPath: String, cssRootPath:String, config: QtiAssetsConfig) extends BaseQtiAssets(jsRootPath, cssRootPath, config) {

  protected def findAssetKeys(qti: Node): Seq[String] = {

    object matches {
      def attrAndValue(name: String, value: String): Boolean =  (qti \\ ("@" + name)).find(_.text == value).isDefined
      def node(key: String): Boolean = (qti \\ key).size > 0
      def attr(key: String): Boolean = (qti \\ ("@" + key)).size > 0
      def nodeOrAttr(key:String) : Boolean = node(key) || attr(key)
    }

    def interactionKeys = QtiItem.interactionModels.filter( i => matches.node(i.tagName)).map(_.tagName).distinct

    def otherKeys = {
      val nodes = Seq(
        matches.nodeOrAttr("tabs") -> "tabs",
        matches.nodeOrAttr("cs-tabs") -> "tabs",
        matches.nodeOrAttr("math") -> "math",
        matches.attrAndValue("class", "numbered-lines") -> "numberedLines"
      )
      nodes.filter(_._1).map(_._2).distinct
    }

    interactionKeys ++ otherKeys
  }

}
