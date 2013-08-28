package player.views.qti

import player.views.qti.models.QtiAssetsConfig
import org.corespring.common.utils.string

//TODO: make this private - we can't do so at the moment because all our tests are within a 'tests' package.
class ScriptResolver(assetsFolder: String, suffix: String = ".js", config: QtiAssetsConfig) {

  def getLocalPaths(interactionKey: String): Seq[String] = {
    config.assets.find(_.name == interactionKey).map { c =>
      val base = if (c.hasJsFile) {
        Seq(string.filePath(assetsFolder, string.lowercaseFirstChar(c.name) + suffix))
      } else {
        Seq()
      }
      base ++ addDependents(c.localDependents)
    }.getOrElse(Seq())
  }

  def getRemotePaths(interactionKey: String): Seq[String] = {
    config.assets.find(_.name == interactionKey).map { c =>
      c.remoteDependents
    }.getOrElse(Seq())
  }

  private def addDependents(deps: Seq[String]): Seq[String] = deps.map(d => string.filePath(assetsFolder, d + suffix))

}
