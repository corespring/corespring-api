package qti.models.interactions.utils

import common.utils.string

class ScriptResolver(assetsFolder:String, suffix : String = ".js", config : QtiAssetsConfig) {

  def getLocalPaths(interactionKey:String) : Seq[String] = {
    config.assets.find(_.name == interactionKey).map{ c =>
      val base = if(c.hasJsFile){
        Seq(string.filePath(assetsFolder, string.lowercaseFirstChar(c.name) + suffix))
      } else {
        Seq()
      }
      base ++ addDependents(c.localDependents)
    }.getOrElse(Seq())
  }

  def getRemotePaths(interactionKey:String) : Seq[String] = {
    config.assets.find(_.name == interactionKey).map{ c =>
      c.remoteDependents
    }.getOrElse(Seq())
  }

  private def addDependents(deps : Seq[String]) : Seq[String] = deps.map( d => string.filePath(assetsFolder, d + suffix) )

}
