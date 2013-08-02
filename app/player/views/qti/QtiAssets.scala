package player.views.qti

import common.utils.string
import play.api.{LoggerLike, Logger}
import player.views.qti.models.{QtiJsAsset, QtiAssetsConfig}
import org.corespring.qti.models.{RenderingMode, QtiItem}
import RenderingMode._
import scala.xml.Node
import common.log.PackageLogging
import org.corespring.qti.models.QtiItem


class QtiAssets(jsRootPath: String, cssRootPath:String, seedConfig: QtiAssetsConfig) {

  def logger: LoggerLike = Logger(this.getClass.getCanonicalName)

  def getLocalJsPaths(keys:Seq[String], mode: RenderingMode): Seq[String] = {
    withKeysAndResolver(jsRootPath, keys, mode, new ScriptResolver(_, ".js", _), (keys, resolver) => keys.map(resolver.getLocalPaths(_)).flatten.distinct)
  }

  def getRemoteJsPaths(keys:Seq[String], mode: RenderingMode): Seq[String] = {
    withKeysAndResolver(jsRootPath, keys, mode, new ScriptResolver(_,".js", _), (keys, resolver) => keys.map(resolver.getRemotePaths(_)).flatten.distinct)
  }

  def getLocalCssPaths(keys:Seq[String], mode : RenderingMode) : Seq[String] = {
    withKeysAndResolver(cssRootPath, keys, mode, new ScriptResolver(_,".css", _), (keys, resolver) => keys.map(resolver.getLocalPaths(_)).flatten.distinct)
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

  private def withKeysAndResolver(root:String,keys:Seq[String], mode: RenderingMode, makeResolver:(String,QtiAssetsConfig)=>ScriptResolver, fn: (Seq[String], ScriptResolver) => Seq[String]): Seq[String] = {
    val rootPath = string.filePath(root, mode.toString.toLowerCase)
    logger.debug("rootPath: " + rootPath)
    val generatedConfig = buildConfig(keys,seedConfig)
    val resolver: ScriptResolver = makeResolver(rootPath, generatedConfig)
    fn(keys, resolver)
  }
}

