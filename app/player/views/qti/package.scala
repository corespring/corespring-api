package player.views

import java.io.File
import org.corespring.common.utils.string

package object qti {

  def fileExists(path: String): Boolean = { new File(string.filePath("public", path)).exists() }

  object models {
    //TODO - move tests to base package - see: https://trello.com/c/C6VdkAqj
    /*private[qti]*/ case class QtiJsAsset(name: String, hasJsFile: Boolean = true, localDependents: Seq[String] = Seq(), remoteDependents: Seq[String] = Seq())
    /*private[qti]*/ case class QtiAssetsConfig(assets: Seq[QtiJsAsset])
  }
}
